package com.example.util

import android.accounts.Account
import android.content.Context
import android.util.Log
import com.example.data.AppDatabase
import com.example.data.Preference
import com.example.data.Transaction
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

object GoogleDriveBackupHelper {

    private const val TAG = "GoogleDriveBackupHelper"
    private const val DRIVE_FILE_SCOPE = "oauth2:https://www.googleapis.com/auth/drive.file"

    /**
     * Checks if auto-backup is enabled, due, and performs the upload to Google Drive silently.
     * Can be invoked from UI (ViewModel), on app startup, or from background WorkManager.
     */
    suspend fun performSilentAutoBackup(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            val database = AppDatabase.getDatabase(context)
            val prefDao = database.preferenceDao()

            val autoFreq = prefDao.getPreferenceValue("auto_backup_frequency") ?: "OFF"
            if (autoFreq == "OFF") {
                Log.d(TAG, "Auto-backup is disabled (OFF)")
                return@withContext false
            }

            val lastBackup = prefDao.getPreferenceValue("last_auto_backup_time")?.toLongOrNull() ?: 0L
            val now = System.currentTimeMillis()

            val intervalMs = when (autoFreq) {
                "DAILY" -> 18 * 60 * 60 * 1000L      // 18 hours buffer for daily
                "WEEKLY" -> 6 * 24 * 60 * 60 * 1000L  // 6 days buffer for weekly
                "MONTHLY" -> 28 * 24 * 60 * 60 * 1000L // 28 days buffer for monthly
                else -> return@withContext false
            }

            val isDue = (now - lastBackup >= intervalMs) || (lastBackup == 0L)
            if (!isDue) {
                Log.d(TAG, "Auto-backup ($autoFreq) is not due yet. Last backup: $lastBackup, now: $now")
                return@withContext false
            }

            val lastAccount = GoogleSignIn.getLastSignedInAccount(context)
            if (lastAccount == null || lastAccount.email.isNullOrBlank()) {
                Log.d(TAG, "No Google account linked. Skipping auto-backup.")
                return@withContext false
            }

            Log.d(TAG, "Auto-backup ($autoFreq) is DUE. Requesting OAuth token for ${lastAccount.email}...")
            val gAccount = Account(lastAccount.email ?: "", "com.google")
            val token = GoogleAuthUtil.getToken(context, gAccount, DRIVE_FILE_SCOPE)

            if (token.isNullOrEmpty()) {
                Log.w(TAG, "Failed to retrieve OAuth token for silent auto-backup")
                return@withContext false
            }

            val client = OkHttpClient()

            // 1. Back up preferences
            backupPreferencesToDrive(client, token, database)

            // 2. Back up transactions CSV
            val txList = database.transactionDao().getAllTransactionsList()
            val backupSuccess = backupTransactionsCsvToDrive(client, token, txList, context)

            if (backupSuccess) {
                prefDao.insertPreference(Preference("last_auto_backup_time", now.toString()))
                Log.i(TAG, "Silent auto-backup ($autoFreq) completed successfully!")
                return@withContext true
            } else {
                Log.e(TAG, "Silent auto-backup CSV upload failed.")
                return@withContext false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error performing silent auto-backup: ${e.message}", e)
            return@withContext false
        }
    }

    /**
     * Uploads or cleanly overwrites a file in Google Drive.
     * Deletes any existing copies first to prevent duplicate files, then performs multipart upload.
     */
    fun uploadFileToDrive(
        client: OkHttpClient,
        token: String,
        fileName: String,
        fileBytes: ByteArray,
        mimeType: String
    ): Boolean {
        try {
            // 1. Query for any existing copies of this file
            val queryUrl = "https://www.googleapis.com/drive/v3/files?q=" +
                    java.net.URLEncoder.encode("name='$fileName' and trashed=false", "UTF-8") +
                    "&fields=files(id,name)"

            val queryRequest = Request.Builder()
                .url(queryUrl)
                .header("Authorization", "Bearer $token")
                .get()
                .build()

            val existingFileIds = mutableListOf<String>()
            try {
                client.newCall(queryRequest).execute().use { response ->
                    if (response.isSuccessful) {
                        val bodyStr = response.body?.string() ?: ""
                        val json = JSONObject(bodyStr)
                        val filesArray = json.optJSONArray("files")
                        if (filesArray != null) {
                            for (i in 0 until filesArray.length()) {
                                val id = filesArray.getJSONObject(i).optString("id")
                                if (id.isNotEmpty()) {
                                    existingFileIds.add(id)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Warning querying existing file $fileName: ${e.message}")
            }

            // 2. Delete existing copies from Drive so there are never stale/duplicate versions
            for (oldFileId in existingFileIds) {
                try {
                    val deleteRequest = Request.Builder()
                        .url("https://www.googleapis.com/drive/v3/files/$oldFileId")
                        .header("Authorization", "Bearer $token")
                        .delete()
                        .build()
                    client.newCall(deleteRequest).execute().use { delRes ->
                        Log.d(TAG, "Deleted old Drive file $oldFileId for $fileName (status=${delRes.code})")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Warning deleting old Drive file $oldFileId: ${e.message}")
                }
            }

            // 3. Upload fresh file using multipart/related
            val uploadUrl = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"
            val boundary = "enterfirst_boundary_${System.currentTimeMillis()}"
            val metaJson = "{\"name\": \"$fileName\", \"mimeType\": \"$mimeType\"}"

            val byteBuffer = ByteArrayOutputStream()
            byteBuffer.write("--$boundary\r\n".toByteArray(Charsets.UTF_8))
            byteBuffer.write("Content-Type: application/json; charset=UTF-8\r\n\r\n".toByteArray(Charsets.UTF_8))
            byteBuffer.write(metaJson.toByteArray(Charsets.UTF_8))
            byteBuffer.write("\r\n--$boundary\r\n".toByteArray(Charsets.UTF_8))
            byteBuffer.write("Content-Type: $mimeType\r\n\r\n".toByteArray(Charsets.UTF_8))
            byteBuffer.write(fileBytes)
            byteBuffer.write("\r\n--$boundary--\r\n".toByteArray(Charsets.UTF_8))

            val requestBody = byteBuffer.toByteArray().toRequestBody("multipart/related; boundary=$boundary".toMediaTypeOrNull())

            val createRequest = Request.Builder()
                .url(uploadUrl)
                .header("Authorization", "Bearer $token")
                .post(requestBody)
                .build()

            client.newCall(createRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    Log.e(TAG, "Drive upload failed for $fileName: code=${response.code}, err=$errBody")
                    return false
                }
                Log.d(TAG, "Successfully backed up and overwrote $fileName in Google Drive")
                return true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception uploading $fileName to Drive: ${e.message}", e)
            return false
        }
    }

    private suspend fun backupPreferencesToDrive(client: OkHttpClient, token: String, database: AppDatabase): Boolean {
        return try {
            val prefDao = database.preferenceDao()
            val settingsJson = JSONObject().apply {
                put("initial_bank_balance", prefDao.getPreferenceValue("initial_bank_balance") ?: "0.0")
                put("initial_cash_balance", prefDao.getPreferenceValue("initial_cash_balance") ?: "0.0")
                put("is_setup_completed", prefDao.getPreferenceValue("is_setup_completed") ?: "false")
                put("monthly_budget_limit", prefDao.getPreferenceValue("monthly_budget_limit") ?: "0.0")
                put("is_dark_theme", prefDao.getPreferenceValue("is_dark_theme") ?: "false")
                put("balance_color_name", prefDao.getPreferenceValue("balance_color_name") ?: "Default")
                put("custom_categories", prefDao.getPreferenceValue("custom_categories") ?: "")
                put("category_order", prefDao.getPreferenceValue("category_order") ?: "")
            }

            uploadFileToDrive(
                client = client,
                token = token,
                fileName = "enterfirst_settings.json",
                fileBytes = settingsJson.toString().toByteArray(Charsets.UTF_8),
                mimeType = "application/json"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to backup preferences: ${e.message}")
            false
        }
    }

    private fun backupTransactionsCsvToDrive(
        client: OkHttpClient,
        token: String,
        txList: List<Transaction>,
        context: Context
    ): Boolean {
        return try {
            val csvContent = StringBuilder()
            csvContent.append("ID,Type,Amount,Party,Category,Note,Timestamp,PaymentStatus,PaymentMethod,MerchantUpiId,ReceiptFileName\n")

            for (tx in txList) {
                var receiptDriveFileName = ""
                if (!tx.receiptUri.isNullOrEmpty()) {
                    val file = File(tx.receiptUri)
                    var validFile: File? = if (file.exists() && file.isFile) file else null
                    if (validFile == null) {
                        val inFilesDir = File(context.filesDir, "receipts/${file.name}")
                        if (inFilesDir.exists() && inFilesDir.isFile) {
                            validFile = inFilesDir
                        }
                    }

                    if (validFile != null) {
                        val safeParty = tx.party.replace(Regex("[^a-zA-Z0-9_]"), "_").take(20)
                        val ext = validFile.extension.ifEmpty { "jpg" }
                        receiptDriveFileName = "enterfirst_receipt_tx_${tx.id}_${safeParty}_${tx.timestamp}.$ext"

                        val mimeType = when (ext.lowercase(Locale.ROOT)) {
                            "png" -> "image/png"
                            "webp" -> "image/webp"
                            else -> "image/jpeg"
                        }

                        uploadFileToDrive(
                            client = client,
                            token = token,
                            fileName = receiptDriveFileName,
                            fileBytes = validFile.readBytes(),
                            mimeType = mimeType
                        )
                    }
                }

                csvContent.append("${tx.id},${tx.type},${tx.amount},\"${tx.party.replace("\"", "\"\"")}\",${tx.category},\"${tx.note.replace("\"", "\"\"")}\",${tx.timestamp},${tx.paymentStatus},${tx.paymentMethod},${tx.merchantUpiId ?: ""},\"${receiptDriveFileName}\"\n")
            }

            val sdf = SimpleDateFormat("yyyy_MM", Locale.getDefault())
            val currentMonthStr = sdf.format(System.currentTimeMillis())
            val csvFileName = "enterfirst_backup_${currentMonthStr}.csv"

            uploadFileToDrive(
                client = client,
                token = token,
                fileName = csvFileName,
                fileBytes = csvContent.toString().toByteArray(Charsets.UTF_8),
                mimeType = "text/csv"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to backup transactions: ${e.message}")
            false
        }
    }
}
