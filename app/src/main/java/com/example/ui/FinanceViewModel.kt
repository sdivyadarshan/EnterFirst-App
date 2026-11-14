package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.FinanceRepository
import com.example.data.Transaction
import com.example.data.AppAnnouncement
import com.example.data.AppUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class FinanceViewModel(private val repository: FinanceRepository) : ViewModel() {

    // Preferences and Settings States
    private val _isPreferencesLoaded = MutableStateFlow(false)
    val isPreferencesLoaded: StateFlow<Boolean> = _isPreferencesLoaded.asStateFlow()

    private val _isSetupCompleted = MutableStateFlow(false)
    val isSetupCompleted: StateFlow<Boolean> = _isSetupCompleted.asStateFlow()

    private val _initialBankBalance = MutableStateFlow(0.0)
    val initialBankBalance: StateFlow<Double> = _initialBankBalance.asStateFlow()

    private val _initialCashBalance = MutableStateFlow(0.0)
    val initialCashBalance: StateFlow<Double> = _initialCashBalance.asStateFlow()

    private val _monthlyBudgetLimit = MutableStateFlow(0.0)
    val monthlyBudgetLimit: StateFlow<Double> = _monthlyBudgetLimit.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _balanceColorName = MutableStateFlow("Default")
    val balanceColorName: StateFlow<String> = _balanceColorName.asStateFlow()

    private val _customCategories = MutableStateFlow<Map<String, String>>(emptyMap())
    val customCategories: StateFlow<Map<String, String>> = _customCategories.asStateFlow()

    private val _categoryOrder = MutableStateFlow<List<String>>(emptyList())
    val categoryOrder: StateFlow<List<String>> = _categoryOrder.asStateFlow()

    private val _googleEmail = MutableStateFlow<String?>(null)
    val googleEmail: StateFlow<String?> = _googleEmail.asStateFlow()

    private val _googleName = MutableStateFlow<String?>(null)
    val googleName: StateFlow<String?> = _googleName.asStateFlow()

    private val _driveBackupStatus = MutableStateFlow<String>("Idle")
    val driveBackupStatus: StateFlow<String> = _driveBackupStatus.asStateFlow()

    private val _autoBackupFrequency = MutableStateFlow("OFF")
    val autoBackupFrequency: StateFlow<String> = _autoBackupFrequency.asStateFlow()

    private val _lastAutoBackupTime = MutableStateFlow(0L)
    val lastAutoBackupTime: StateFlow<Long> = _lastAutoBackupTime.asStateFlow()

    private val _recoveryIntent = MutableStateFlow<android.content.Intent?>(null)
    val recoveryIntent: StateFlow<android.content.Intent?> = _recoveryIntent.asStateFlow()

    private val _isZeroBalanceVideoEnabled = MutableStateFlow(true)
    val isZeroBalanceVideoEnabled: StateFlow<Boolean> = _isZeroBalanceVideoEnabled.asStateFlow()

    private val _customZeroBalanceVideoPath = MutableStateFlow<String?>(null)
    val customZeroBalanceVideoPath: StateFlow<String?> = _customZeroBalanceVideoPath.asStateFlow()

    private val _incomingUpiPayload = MutableStateFlow<String?>(null)
    val incomingUpiPayload: StateFlow<String?> = _incomingUpiPayload.asStateFlow()

    companion object {
        val ADMIN_EMAILS = listOf("actionsendhanaigal@gmail.com", "sddarshan2007@gmail.com")
        const val ADMIN_EMAIL = "actionsendhanaigal@gmail.com"
        const val ADMIN_EMAIL_ALT = "sddarshan2007@gmail.com"

        fun isUserAdmin(email: String?): Boolean {
            if (email.isNullOrBlank()) return false
            return ADMIN_EMAILS.any { it.equals(email.trim(), ignoreCase = true) }
        }

        private val _deepLinkPayload = MutableStateFlow<String?>(null)
        val deepLinkPayload: StateFlow<String?> = _deepLinkPayload.asStateFlow()
        fun setDeepLinkPayload(payload: String?) {
            _deepLinkPayload.value = payload
        }
    }

    private val _isAdmin = MutableStateFlow<Boolean>(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    private val _isBatteryDialogShown = MutableStateFlow<Boolean>(false)
    val isBatteryDialogShown: StateFlow<Boolean> = _isBatteryDialogShown.asStateFlow()

    private val _isUpiQrSetupDone = MutableStateFlow<Boolean>(false)
    val isUpiQrSetupDone: StateFlow<Boolean> = _isUpiQrSetupDone.asStateFlow()

    private val _userUpiQrs = MutableStateFlow<List<UpiQrCode>>(emptyList())
    val userUpiQrs: StateFlow<List<UpiQrCode>> = _userUpiQrs.asStateFlow()

    // Announcements & Admin Broadcasts Flow
    val announcements: StateFlow<List<AppAnnouncement>> = repository.allAnnouncements
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val unreadAnnouncementsCount: StateFlow<Int> = repository.unreadAnnouncementsCount
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    // Registered / Logged-in App Users Flow
    val allUsers: StateFlow<List<AppUser>> = repository.allUsers
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun clearRecoveryIntent() {
        _recoveryIntent.value = null
    }

    fun setIncomingUpiPayload(payload: String?) {
        _incomingUpiPayload.value = payload
    }

    fun setDriveBackupStatus(status: String) {
        _driveBackupStatus.value = status
    }

    // Transactions Flow
    val transactions: StateFlow<List<Transaction>> = repository.allTransactions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Derived States
    val currentBankBalance: StateFlow<Double> = combine(
        _initialBankBalance,
        transactions
    ) { initial, txs ->
        val incomes = txs.filter { it.type == "INCOME" && it.paymentMethod == "BANK" && it.paymentStatus == "PAID" }.sumOf { it.amount }
        val expenses = txs.filter { it.type == "EXPENSE" && it.paymentMethod == "BANK" && it.paymentStatus == "PAID" }.sumOf { it.amount }
        initial + incomes - expenses
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val currentCashBalance: StateFlow<Double> = combine(
        _initialCashBalance,
        transactions
    ) { initial, txs ->
        val incomes = txs.filter { it.type == "INCOME" && it.paymentMethod == "CASH" && it.paymentStatus == "PAID" }.sumOf { it.amount }
        val expenses = txs.filter { it.type == "EXPENSE" && it.paymentMethod == "CASH" && it.paymentStatus == "PAID" }.sumOf { it.amount }
        initial + incomes - expenses
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val currentTotalBalance: StateFlow<Double> = combine(
        currentBankBalance,
        currentCashBalance
    ) { bank, cash ->
        bank + cash
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val todaySpending: StateFlow<Double> = transactions.combine(_isSetupCompleted) { txs, _ ->
        val today = System.currentTimeMillis()
        txs.filter {
            it.type == "EXPENSE" && it.paymentStatus == "PAID" && isSameDay(it.timestamp, today)
        }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val monthlySpending: StateFlow<Double> = transactions.combine(_isSetupCompleted) { txs, _ ->
        val today = System.currentTimeMillis()
        txs.filter {
            it.type == "EXPENSE" && it.paymentStatus == "PAID" && isSameMonth(it.timestamp, today)
        }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    init {
        loadPreferences()
    }

    private fun loadPreferences() {
        viewModelScope.launch {
            _isSetupCompleted.value = repository.getPreferenceValue("is_setup_completed")?.toBoolean() ?: false
            _initialBankBalance.value = repository.getPreferenceValue("initial_bank_balance")?.toDoubleOrNull() ?: 0.0
            _initialCashBalance.value = repository.getPreferenceValue("initial_cash_balance")?.toDoubleOrNull() ?: 0.0
            _monthlyBudgetLimit.value = repository.getPreferenceValue("monthly_budget_limit")?.toDoubleOrNull() ?: 0.0
            _isDarkTheme.value = repository.getPreferenceValue("is_dark_theme")?.toBoolean() ?: true
            _balanceColorName.value = repository.getPreferenceValue("balance_color_name") ?: "Default"

            val customStr = repository.getPreferenceValue("custom_categories") ?: ""
            val customMap = if (customStr.isNotEmpty()) {
                customStr.split(",").mapNotNull {
                    val parts = it.split(":")
                    if (parts.size == 2) parts[0] to parts[1] else null
                }.toMap()
            } else {
                emptyMap()
            }
            _customCategories.value = customMap

            val orderStr = repository.getPreferenceValue("category_order") ?: ""
            val defaultList = listOf("Food", "Travel", "Shopping", "Bills", "Health", "Education", "Entertainment", "Others")
            val orderList = if (orderStr.isNotEmpty()) {
                val list = orderStr.split(",").filter { it.isNotEmpty() }
                (list + defaultList).distinct()
            } else {
                defaultList
            }
            _categoryOrder.value = orderList

            val savedEmail = repository.getPreferenceValue("google_email")
            val savedName = repository.getPreferenceValue("google_name")
            if (!savedEmail.isNullOrEmpty()) {
                _googleEmail.value = savedEmail
                _googleName.value = savedName
                val userIsAdmin = isUserAdmin(savedEmail)
                _isAdmin.value = userIsAdmin
                val user = AppUser(
                    email = savedEmail,
                    displayName = savedName ?: "",
                    lastLoginTime = System.currentTimeMillis(),
                    isAdmin = userIsAdmin
                )
                repository.registerOrUpdateUser(user)
            }
            try {
                val cleanedKey = "has_cleaned_legacy_notifications_v4"
                val hasCleaned = repository.getPreferenceValue(cleanedKey)?.toBoolean() ?: false
                if (!hasCleaned) {
                    repository.clearAllAnnouncementsExceptWelcome()
                    repository.savePreference(cleanedKey, "true")
                }

                if (repository.getAnnouncementCount() == 0) {
                    repository.postAnnouncement(
                        AppAnnouncement(
                            title = "Welcome to EnterFirst Finance & UPI",
                            message = "Official updates and APK releases from the Administrator will be broadcasted here. UPI payments, automatic offline sync, and receipt sharing are active",
                            category = "FEATURE",
                            senderEmail = "Admin",
                            priority = "NORMAL"
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            _autoBackupFrequency.value = repository.getPreferenceValue("auto_backup_frequency") ?: "OFF"
            _lastAutoBackupTime.value = repository.getPreferenceValue("last_auto_backup_time")?.toLongOrNull() ?: 0L
            _isZeroBalanceVideoEnabled.value = repository.getPreferenceValue("zero_balance_video_enabled")?.toBoolean() ?: true
            _customZeroBalanceVideoPath.value = repository.getPreferenceValue("custom_zero_balance_video_path").takeIf { !it.isNullOrBlank() }

            _isBatteryDialogShown.value = repository.getPreferenceValue("is_battery_dialog_shown")?.toBoolean() ?: false
            _isUpiQrSetupDone.value = repository.getPreferenceValue("is_upi_qr_setup_done")?.toBoolean() ?: false

            val qrsJsonStr = repository.getPreferenceValue("user_upi_qrs_json") ?: "[]"
            val qrsList = mutableListOf<UpiQrCode>()
            try {
                val arr = org.json.JSONArray(qrsJsonStr)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    qrsList.add(
                        UpiQrCode(
                            id = obj.optString("id"),
                            appName = obj.optString("appName"),
                            imagePath = obj.optString("imagePath")
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            _userUpiQrs.value = qrsList

            _isPreferencesLoaded.value = true
        }
    }

    fun setBatteryDialogShown() {
        _isBatteryDialogShown.value = true
        viewModelScope.launch {
            repository.savePreference("is_battery_dialog_shown", "true")
        }
    }

    fun setUpiQrSetupDone() {
        _isUpiQrSetupDone.value = true
        viewModelScope.launch {
            repository.savePreference("is_upi_qr_setup_done", "true")
        }
    }

    fun saveUpiQrCodes(qrs: List<UpiQrCode>) {
        _userUpiQrs.value = qrs
        _isUpiQrSetupDone.value = true
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val jsonArray = org.json.JSONArray()
            for (qr in qrs) {
                val obj = org.json.JSONObject().apply {
                    put("id", qr.id)
                    put("appName", qr.appName)
                    put("imagePath", qr.imagePath)
                }
                jsonArray.put(obj)
            }
            repository.savePreference("user_upi_qrs_json", jsonArray.toString())
            repository.savePreference("is_upi_qr_setup_done", "true")
        }
    }

    fun deleteUpiQrCode(qrId: String) {
        val updated = _userUpiQrs.value.filter { it.id != qrId }
        saveUpiQrCodes(updated)
    }

    fun moveUpiQrUp(qrId: String) {
        val list = _userUpiQrs.value.toMutableList()
        val index = list.indexOfFirst { it.id == qrId }
        if (index > 0) {
            val item = list.removeAt(index)
            list.add(index - 1, item)
            saveUpiQrCodes(list)
        }
    }

    fun moveUpiQrDown(qrId: String) {
        val list = _userUpiQrs.value.toMutableList()
        val index = list.indexOfFirst { it.id == qrId }
        if (index >= 0 && index < list.size - 1) {
            val item = list.removeAt(index)
            list.add(index + 1, item)
            saveUpiQrCodes(list)
        }
    }

    fun updateUpiQr(qrId: String, newAppName: String, newImagePath: String? = null) {
        val list = _userUpiQrs.value.map { item ->
            if (item.id == qrId) {
                item.copy(
                    appName = newAppName,
                    imagePath = newImagePath ?: item.imagePath
                )
            } else item
        }
        saveUpiQrCodes(list)
    }

    fun setZeroBalanceVideoEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.savePreference("zero_balance_video_enabled", enabled.toString())
            _isZeroBalanceVideoEnabled.value = enabled
        }
    }

    fun setCustomZeroBalanceVideoPath(path: String?) {
        viewModelScope.launch {
            if (path.isNullOrBlank()) {
                repository.savePreference("custom_zero_balance_video_path", "")
                _customZeroBalanceVideoPath.value = null
            } else {
                repository.savePreference("custom_zero_balance_video_path", path)
                _customZeroBalanceVideoPath.value = path
            }
        }
    }

    fun setBalanceColorName(name: String) {
        viewModelScope.launch {
            repository.savePreference("balance_color_name", name)
            _balanceColorName.value = name
        }
    }

    fun setMonthlyBudgetLimit(limit: Double) {
        viewModelScope.launch {
            repository.savePreference("monthly_budget_limit", limit.toString())
            _monthlyBudgetLimit.value = limit
        }
    }

    fun addCustomCategory(name: String, emoji: String) {
        viewModelScope.launch {
            val current = _customCategories.value.toMutableMap()
            current[name] = emoji
            _customCategories.value = current
            
            val customStr = current.map { "${it.key}:${it.value}" }.joinToString(",")
            repository.savePreference("custom_categories", customStr)
            
            trackCategorySelection(name)
        }
    }

    fun trackCategorySelection(name: String) {
        viewModelScope.launch {
            val currentOrder = _categoryOrder.value.toMutableList()
            currentOrder.remove(name)
            currentOrder.add(0, name)
            _categoryOrder.value = currentOrder
            
            val orderStr = currentOrder.joinToString(",")
            repository.savePreference("category_order", orderStr)
        }
    }

    fun completeSetup(bank: Double, cash: Double) {
        viewModelScope.launch {
            repository.savePreference("initial_bank_balance", bank.toString())
            repository.savePreference("initial_cash_balance", cash.toString())
            repository.savePreference("is_setup_completed", "true")
            _initialBankBalance.value = bank
            _initialCashBalance.value = cash
            _isSetupCompleted.value = true
        }
    }

    fun toggleTheme() {
        viewModelScope.launch {
            val nextTheme = !_isDarkTheme.value
            repository.savePreference("is_dark_theme", nextTheme.toString())
            _isDarkTheme.value = nextTheme
        }
    }

    fun updateBankBalance(newBalance: Double) {
        viewModelScope.launch {
            // Adjust the starting balance so that the calculated current balance matches newBalance
            val txs = transactions.value
            val incomes = txs.filter { it.type == "INCOME" && it.paymentMethod == "BANK" && it.paymentStatus == "PAID" }.sumOf { it.amount }
            val expenses = txs.filter { it.type == "EXPENSE" && it.paymentMethod == "BANK" && it.paymentStatus == "PAID" }.sumOf { it.amount }
            val adjustedInitial = newBalance - incomes + expenses
            repository.savePreference("initial_bank_balance", adjustedInitial.toString())
            _initialBankBalance.value = adjustedInitial
        }
    }

    fun updateCashBalance(newBalance: Double) {
        viewModelScope.launch {
            // Adjust the starting balance so that the calculated current balance matches newBalance
            val txs = transactions.value
            val incomes = txs.filter { it.type == "INCOME" && it.paymentMethod == "CASH" && it.paymentStatus == "PAID" }.sumOf { it.amount }
            val expenses = txs.filter { it.type == "EXPENSE" && it.paymentMethod == "CASH" && it.paymentStatus == "PAID" }.sumOf { it.amount }
            val adjustedInitial = newBalance - incomes + expenses
            repository.savePreference("initial_cash_balance", adjustedInitial.toString())
            _initialCashBalance.value = adjustedInitial
        }
    }

    fun addTransaction(
        type: String,
        amount: Double,
        party: String,
        category: String,
        note: String,
        paymentMethod: String,
        paymentStatus: String,
        merchantUpiId: String? = null,
        receiptUri: String? = null,
        timestamp: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            val tx = Transaction(
                type = type,
                amount = amount,
                party = party,
                category = category,
                note = note,
                paymentMethod = paymentMethod,
                paymentStatus = paymentStatus,
                merchantUpiId = merchantUpiId,
                receiptUri = receiptUri,
                timestamp = timestamp
            )
            repository.insertTransaction(tx)
        }
    }

    fun updateTransactionStatus(transactionId: Int, status: String) {
        viewModelScope.launch {
            val tx = repository.getTransactionById(transactionId)
            if (tx != null) {
                repository.insertTransaction(tx.copy(paymentStatus = status))
            }
        }
    }

    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.insertTransaction(transaction)
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun setAutoBackupFrequency(frequency: String) {
        viewModelScope.launch {
            _autoBackupFrequency.value = frequency
            repository.savePreference("auto_backup_frequency", frequency)
        }
    }

    fun updateLastBackupTimeAndSave() {
        val now = System.currentTimeMillis()
        _lastAutoBackupTime.value = now
        viewModelScope.launch {
            repository.savePreference("last_auto_backup_time", now.toString())
        }
    }

    fun resetAllData() {
        viewModelScope.launch {
            repository.resetAllData()
            _isSetupCompleted.value = false
            _initialBankBalance.value = 0.0
            _initialCashBalance.value = 0.0
            _isDarkTheme.value = true
        }
    }

    // Data Export & Import Operations
    fun exportToCsv(context: Context, onResult: (Uri?) -> Unit) {
        exportToCsv(context, null, onResult)
    }

    fun exportToCsv(context: Context, customList: List<Transaction>?, onResult: (Uri?) -> Unit) {
        viewModelScope.launch {
            try {
                val txs = customList ?: transactions.value
                val csvContent = StringBuilder()
                csvContent.append("ID,Type,Amount,Party,Category,Note,Timestamp,PaymentStatus,PaymentMethod,MerchantUpiId\n")
                for (tx in txs) {
                    csvContent.append("${tx.id},${tx.type},${tx.amount},\"${tx.party.replace("\"", "\"\"")}\",${tx.category},\"${tx.note.replace("\"", "\"\"")}\",${tx.timestamp},${tx.paymentStatus},${tx.paymentMethod},${tx.merchantUpiId ?: ""}\n")
                }

                val cacheFile = File(context.cacheDir, "enterfirst_backup.csv")
                FileOutputStream(cacheFile).use { fos ->
                    fos.write(csvContent.toString().toByteArray())
                }

                val authority = "${context.packageName}.fileprovider"
                val uri = androidx.core.content.FileProvider.getUriForFile(context, authority, cacheFile)
                onResult(uri)
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(null)
            }
        }
    }

    fun exportToJson(context: Context, customList: List<Transaction>? = null, onResult: (Uri?) -> Unit) {
        viewModelScope.launch {
            try {
                val txs = customList ?: transactions.value
                val array = org.json.JSONArray()
                for (tx in txs) {
                    val obj = JSONObject().apply {
                        put("id", tx.id)
                        put("type", tx.type)
                        put("amount", tx.amount)
                        put("party", tx.party)
                        put("category", tx.category)
                        put("note", tx.note)
                        put("timestamp", tx.timestamp)
                        put("paymentStatus", tx.paymentStatus)
                        put("paymentMethod", tx.paymentMethod)
                        put("merchantUpiId", tx.merchantUpiId)
                    }
                    array.put(obj)
                }

                val cacheFile = File(context.cacheDir, "enterfirst_backup.json")
                FileOutputStream(cacheFile).use { fos ->
                    fos.write(array.toString(2).toByteArray())
                }

                val authority = "${context.packageName}.fileprovider"
                val uri = androidx.core.content.FileProvider.getUriForFile(context, authority, cacheFile)
                onResult(uri)
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(null)
            }
        }
    }

    fun importFromJson(context: Context, uri: Uri, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val text = inputStream?.bufferedReader()?.use { it.readText() } ?: ""
                val array = org.json.JSONArray(text)
                var importedCount = 0
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val tx = Transaction(
                        type = obj.optString("type", "EXPENSE"),
                        amount = obj.optDouble("amount", 0.0),
                        party = obj.optString("party", "Unknown"),
                        category = obj.optString("category", "Others"),
                        note = obj.optString("note", ""),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        paymentStatus = obj.optString("paymentStatus", "SUCCESS"),
                        paymentMethod = obj.optString("paymentMethod", "UPI"),
                        merchantUpiId = obj.optString("merchantUpiId").takeIf { !it.isNullOrBlank() }
                    )
                    repository.insertTransaction(tx)
                    importedCount++
                }
                onResult(true, "Successfully imported $importedCount transactions")
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false, "Import failed: ${e.localizedMessage}")
            }
        }
    }

    fun importFromCsv(context: Context, uri: Uri, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val reader = BufferedReader(InputStreamReader(inputStream))
                val header = reader.readLine() // Header
                if (header == null || !header.contains("Type") || !header.contains("Amount")) {
                    onResult(false, "Invalid CSV format")
                    return@launch
                }

                var importedCount = 0
                var line: String? = reader.readLine()
                while (line != null) {
                    if (line.trim().isEmpty()) {
                        line = reader.readLine()
                        continue
                    }

                    val parts = parseCsvLine(line)
                    if (parts.size >= 10) {
                        val type = parts[1]
                        val amount = parts[2].toDoubleOrNull() ?: 0.0
                        val party = parts[3]
                        val category = parts[4]
                        val note = parts[5]
                        val timestamp = parts[6].toLongOrNull() ?: System.currentTimeMillis()
                        val paymentStatus = parts[7]
                        val paymentMethod = parts[8]
                        val merchantUpiId = parts[9].ifEmpty { null }

                        val tx = Transaction(
                            type = type,
                            amount = amount,
                            party = party,
                            category = category,
                            note = note,
                            timestamp = timestamp,
                            paymentStatus = paymentStatus,
                            paymentMethod = paymentMethod,
                            merchantUpiId = merchantUpiId
                        )
                        repository.insertTransaction(tx)
                        importedCount++
                    }
                    line = reader.readLine()
                }
                reader.close()
                onResult(true, "Successfully imported $importedCount transactions")
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false, "Import failed: ${e.localizedMessage}")
            }
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val curVal = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            if (inQuotes) {
                if (ch == '\"') {
                    if (i + 1 < line.length && line[i + 1] == '\"') {
                        curVal.append('\"')
                        i++ // Skip the escaped second quote
                    } else {
                        inQuotes = false
                    }
                } else {
                    curVal.append(ch)
                }
            } else {
                if (ch == '\"') {
                    inQuotes = true
                } else if (ch == ',') {
                    result.add(curVal.toString())
                    curVal.clear()
                } else {
                    curVal.append(ch)
                }
            }
            i++
        }
        result.add(curVal.toString())
        return result
    }

    // Helper utilities
    private fun isSameDay(t1: Long, t2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = t1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = t2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun isSameMonth(t1: Long, t2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = t1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = t2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH)
    }

    fun writeCsvToUri(context: Context, uri: Uri, onResult: (Boolean) -> Unit) {
        writeCsvToUri(context, uri, null, onResult)
    }

    fun writeCsvToUri(context: Context, uri: Uri, customList: List<Transaction>?, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    val txs = customList ?: transactions.value
                    val csvContent = StringBuilder()
                    csvContent.append("ID,Type,Amount,Party,Category,Note,Timestamp,PaymentStatus,PaymentMethod,MerchantUpiId\n")
                    for (tx in txs) {
                        csvContent.append("${tx.id},${tx.type},${tx.amount},\"${tx.party.replace("\"", "\"\"")}\",${tx.category},\"${tx.note.replace("\"", "\"\"")}\",${tx.timestamp},${tx.paymentStatus},${tx.paymentMethod},${tx.merchantUpiId ?: ""}\n")
                    }
                    os.write(csvContent.toString().toByteArray())
                }
                onResult(true)
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false)
            }
        }
    }

    fun writeJsonToUri(context: Context, uri: Uri, onResult: (Boolean) -> Unit) {
        writeJsonToUri(context, uri, null, onResult)
    }

    fun writeJsonToUri(context: Context, uri: Uri, customList: List<Transaction>?, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    val txs = customList ?: transactions.value
                    val array = org.json.JSONArray()
                    for (tx in txs) {
                        val obj = JSONObject().apply {
                            put("id", tx.id)
                            put("type", tx.type)
                            put("amount", tx.amount)
                            put("party", tx.party)
                            put("category", tx.category)
                            put("note", tx.note)
                            put("timestamp", tx.timestamp)
                            put("paymentStatus", tx.paymentStatus)
                            put("paymentMethod", tx.paymentMethod)
                            put("merchantUpiId", tx.merchantUpiId)
                        }
                        array.put(obj)
                    }
                    os.write(array.toString(2).toByteArray())
                }
                onResult(true)
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false)
            }
        }
    }

    fun setGoogleAccount(email: String?, name: String?) {
        viewModelScope.launch {
            _googleEmail.value = email
            _googleName.value = name
            val userIsAdmin = isUserAdmin(email)
            _isAdmin.value = userIsAdmin
            repository.savePreference("google_email", email ?: "")
            repository.savePreference("google_name", name ?: "")
            if (email != null) {
                val appUser = AppUser(
                    email = email,
                    displayName = name ?: "",
                    lastLoginTime = System.currentTimeMillis(),
                    isAdmin = userIsAdmin
                )
                repository.registerOrUpdateUser(appUser)
                try {
                    com.example.data.FirebaseUserManager.registerUserInFirebase(appUser)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                // If auto-backup frequency is OFF or unset, set default to DAILY so auto backup works seamlessly
                if (_autoBackupFrequency.value == "OFF") {
                    _autoBackupFrequency.value = "DAILY"
                    repository.savePreference("auto_backup_frequency", "DAILY")
                }
            }
            if (email == null) {
                _driveBackupStatus.value = "Idle"
            }
        }
    }

    fun syncFirebaseUsers(context: android.content.Context? = null, onComplete: ((Int) -> Unit)? = null) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val cloudUsers = com.example.data.FirebaseUserManager.fetchAllUsersFromFirebase(context)
                for (user in cloudUsers) {
                    repository.registerOrUpdateUser(user)
                }
                val cloudAnnouncements = com.example.data.FirebaseUserManager.fetchAnnouncementsFromFirebase(context)
                val cloudIds = cloudAnnouncements.map { it.id }.toSet() + cloudAnnouncements.map { it.timestamp }.toSet()

                // Purge local announcements that were removed from Firebase by Admin
                val localList = repository.getAllAnnouncementsList()
                for (localItem in localList) {
                    if (localItem.title != "Welcome to EnterFirst Finance & UPI") {
                        if (localItem.id !in cloudIds && localItem.timestamp !in cloudIds) {
                            repository.deleteAnnouncement(localItem.id)
                            if (context != null) {
                                com.example.util.AppNotificationHelper.cancelNotification(context, localItem.id)
                            }
                        }
                    }
                }

                for (announcement in cloudAnnouncements) {
                    repository.postAnnouncement(announcement)
                    if (context != null && announcement.isCurrentlyActive() && !com.example.util.AppNotificationHelper.isAlreadyNotified(context, announcement)) {
                        com.example.util.AppNotificationHelper.showAnnouncementNotification(
                            context = context,
                            announcement = announcement
                        )
                    }
                }
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onComplete?.invoke(cloudUsers.size)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onComplete?.invoke(0)
                }
            }
        }
    }

    fun initAppOpenSyncAndAutoBackup(context: android.content.Context) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val appCtx = context.applicationContext

            // 0. Ensure all background sync workers & heartbeat are active
            com.example.util.AnnouncementSyncScheduler.startAllBackgroundSync(appCtx)

            // 1. Start real-time Firestore announcement listener with instant deletion & cloud-id reconciliation
            com.example.data.FirebaseUserManager.startRealtimeAnnouncementListener(
                context = appCtx,
                onAnnouncementReceived = { announcement ->
                    viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        repository.postAnnouncement(announcement)
                        if (announcement.isCurrentlyActive() && !com.example.util.AppNotificationHelper.isAlreadyNotified(appCtx, announcement)) {
                            com.example.util.AppNotificationHelper.showAnnouncementNotification(
                                context = appCtx,
                                announcement = announcement
                            )
                        }
                    }
                },
                onAnnouncementRemoved = { removedId ->
                    viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        repository.deleteAnnouncement(removedId)
                        com.example.util.AppNotificationHelper.cancelNotification(appCtx, removedId)
                    }
                },
                onActiveCloudIdsSynced = { activeCloudIds ->
                    viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        val localList = repository.getAllAnnouncementsList()
                        for (localItem in localList) {
                            if (localItem.title != "Welcome to EnterFirst Finance & UPI") {
                                if (localItem.id !in activeCloudIds && localItem.timestamp !in activeCloudIds) {
                                    repository.deleteAnnouncement(localItem.id)
                                    com.example.util.AppNotificationHelper.cancelNotification(appCtx, localItem.id)
                                }
                            }
                        }
                    }
                }
            )

            // 2. Fetch all cloud announcements immediately on app open to catch up & purge deleted ones
            try {
                val cloudAnnouncements = com.example.data.FirebaseUserManager.fetchAnnouncementsFromFirebase(appCtx)
                val cloudIds = cloudAnnouncements.map { it.id }.toSet() + cloudAnnouncements.map { it.timestamp }.toSet()

                val localList = repository.getAllAnnouncementsList()
                for (localItem in localList) {
                    if (localItem.title != "Welcome to EnterFirst Finance & UPI") {
                        if (localItem.id !in cloudIds && localItem.timestamp !in cloudIds) {
                            repository.deleteAnnouncement(localItem.id)
                            com.example.util.AppNotificationHelper.cancelNotification(appCtx, localItem.id)
                        }
                    }
                }

                for (announcement in cloudAnnouncements) {
                    repository.postAnnouncement(announcement)
                    if (announcement.isCurrentlyActive() && !com.example.util.AppNotificationHelper.isAlreadyNotified(appCtx, announcement)) {
                        com.example.util.AppNotificationHelper.showAnnouncementNotification(
                            context = appCtx,
                            announcement = announcement
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 3. Register current logged-in user to Firebase
            val email = _googleEmail.value
            if (!email.isNullOrBlank()) {
                val user = AppUser(
                    email = email,
                    displayName = _googleName.value ?: "",
                    lastLoginTime = System.currentTimeMillis(),
                    isAdmin = _isAdmin.value
                )
                com.example.data.FirebaseUserManager.registerUserInFirebase(user, appCtx)
            }

            // 4. Auto Daily Backup if enabled and internet is active
            if (com.example.data.FirebaseUserManager.isNetworkAvailable(appCtx)) {
                val autoFreq = _autoBackupFrequency.value
                val lastBackup = _lastAutoBackupTime.value
                val now = System.currentTimeMillis()
                val isBackupDue = when (autoFreq) {
                    "DAILY" -> (now - lastBackup) > 18 * 60 * 60 * 1000L || lastBackup == 0L
                    "WEEKLY" -> (now - lastBackup) > 6 * 24 * 60 * 60 * 1000L || lastBackup == 0L
                    "MONTHLY" -> (now - lastBackup) > 28 * 24 * 60 * 60 * 1000L || lastBackup == 0L
                    else -> false
                }
                if (isBackupDue && !_googleEmail.value.isNullOrBlank()) {
                    performSilentAutoBackup(appCtx)
                }
            }
        }
    }

    private fun performSilentAutoBackup(context: android.content.Context) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val success = com.example.util.GoogleDriveBackupHelper.performSilentAutoBackup(context)
            if (success) {
                val now = System.currentTimeMillis()
                _lastAutoBackupTime.value = now
            }
        }
    }

    fun postAdminUpdate(
        title: String,
        message: String,
        category: String = "UPDATE",
        priority: String = "NORMAL",
        apkDownloadUrl: String? = null,
        versionCode: String? = null,
        scheduledAppearTime: Long? = null,
        scheduledDisappearTime: Long? = null,
        context: android.content.Context? = null
    ) {
        viewModelScope.launch {
            val cleanApkUrl = apkDownloadUrl?.trim().takeIf { !it.isNullOrBlank() }
            val cleanVersion = versionCode?.trim().takeIf { !it.isNullOrBlank() }
            val announcement = AppAnnouncement(
                title = title,
                message = message,
                category = category,
                senderEmail = "Admin",
                priority = priority,
                timestamp = System.currentTimeMillis(),
                apkDownloadUrl = cleanApkUrl,
                versionCode = cleanVersion,
                scheduledAppearTime = scheduledAppearTime,
                scheduledDisappearTime = scheduledDisappearTime
            )
            val savedId = repository.postAnnouncement(announcement)
            val finalAnnouncement = announcement.copy(id = if (savedId > 0) savedId else announcement.timestamp)
            if (context != null) {
                com.example.util.AppNotificationHelper.markAsNotified(context, finalAnnouncement)
            }
            try {
                com.example.data.FirebaseUserManager.broadcastAnnouncementToFirebase(finalAnnouncement, context)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            if (context != null && finalAnnouncement.isCurrentlyActive()) {
                com.example.util.AppNotificationHelper.showAnnouncementNotification(
                    context = context,
                    announcement = finalAnnouncement,
                    forceShow = true
                )
            }
        }
    }

    fun markAnnouncementRead(id: Long) {
        viewModelScope.launch {
            repository.markAnnouncementRead(id)
        }
    }

    fun markAllAnnouncementsRead() {
        viewModelScope.launch {
            repository.markAllAnnouncementsRead()
        }
    }

    fun deleteAnnouncement(id: Long, context: android.content.Context? = null) {
        viewModelScope.launch {
            repository.deleteAnnouncement(id)
            try {
                com.example.data.FirebaseUserManager.deleteAnnouncementFromFirebase(id, context)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun clearAllAnnouncementsExceptWelcome(context: android.content.Context? = null) {
        viewModelScope.launch {
            repository.clearAllAnnouncementsExceptWelcome()
            // Guarantee Welcome notice is retained/created
            if (repository.getAnnouncementCount() == 0) {
                repository.postAnnouncement(
                    AppAnnouncement(
                        title = "Welcome to EnterFirst Finance & UPI",
                        message = "Official updates and APK releases from the Administrator will be broadcasted here. UPI payments, automatic offline sync, and receipt sharing are active",
                        category = "FEATURE",
                        senderEmail = "Admin",
                        priority = "NORMAL"
                    )
                )
            }
            try {
                com.example.data.FirebaseUserManager.clearAllCloudAnnouncementsExceptWelcome(context)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            if (context != null) {
                com.example.util.AppNotificationHelper.clearAllNotifiedKeys(context)
            }
        }
    }

    fun fetchGoogleDriveToken(context: Context, account: com.google.android.gms.auth.api.signin.GoogleSignInAccount) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                _driveBackupStatus.value = "Retrieving OAuth token..."
                val gAccount = android.accounts.Account(account.email ?: "", "com.google")
                val token = com.google.android.gms.auth.GoogleAuthUtil.getToken(
                    context,
                    gAccount,
                    "oauth2:https://www.googleapis.com/auth/drive.file"
                )
                
                _driveBackupStatus.value = "Token retrieved. Checking for backups..."
                val client = okhttp3.OkHttpClient()
                val queryUrl = "https://www.googleapis.com/drive/v3/files?q=" + 
                        java.net.URLEncoder.encode("(name='enterfirst_settings.json' or name contains 'enterfirst_backup_') and trashed=false", "UTF-8")
                
                val queryRequest = Request.Builder()
                    .url(queryUrl)
                    .header("Authorization", "Bearer $token")
                    .get()
                    .build()

                var backupExists = false
                client.newCall(queryRequest).execute().use { response ->
                    if (response.isSuccessful) {
                        val bodyStr = response.body?.string() ?: ""
                        val json = JSONObject(bodyStr)
                        val filesArray = json.optJSONArray("files")
                        if (filesArray != null && filesArray.length() > 0) {
                            backupExists = true
                        }
                    }
                }

                if (backupExists) {
                    _driveBackupStatus.value = "Restoring data from Google Drive..."
                    val success = restoreFromGoogleDriveWithToken(token, context.applicationContext)
                    if (success) {
                        _driveBackupStatus.value = "Success: Restored data and settings from Google Drive!"
                    } else {
                        _driveBackupStatus.value = "Failed to restore backup from Google Drive."
                    }
                } else {
                    _driveBackupStatus.value = "No existing backup found. Creating first backup..."
                    backupToGoogleDriveWithToken(token, context.applicationContext)
                }
            } catch (e: com.google.android.gms.auth.UserRecoverableAuthException) {
                e.printStackTrace()
                _driveBackupStatus.value = "Consent required. Directing..."
                _recoveryIntent.value = e.intent
            } catch (e: Exception) {
                e.printStackTrace()
                _driveBackupStatus.value = "Failed: Token retrieval: ${e.localizedMessage}"
            }
        }
    }

    fun backupToGoogleDrive(context: Context) {
        val lastAccount = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(context)
        if (lastAccount != null) {
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    _driveBackupStatus.value = "Retrieving OAuth token..."
                    val gAccount = android.accounts.Account(lastAccount.email ?: "", "com.google")
                    val token = com.google.android.gms.auth.GoogleAuthUtil.getToken(
                        context,
                        gAccount,
                        "oauth2:https://www.googleapis.com/auth/drive.file"
                    )
                    backupToGoogleDriveWithToken(token, context.applicationContext)
                } catch (e: Exception) {
                    e.printStackTrace()
                    _driveBackupStatus.value = "Failed: ${e.localizedMessage}"
                }
            }
        } else {
            _driveBackupStatus.value = "Failed: Not signed in."
        }
    }

    fun restoreFromGoogleDrive(context: Context) {
        val lastAccount = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(context)
        if (lastAccount != null) {
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    _driveBackupStatus.value = "Retrieving OAuth token..."
                    val gAccount = android.accounts.Account(lastAccount.email ?: "", "com.google")
                    val token = com.google.android.gms.auth.GoogleAuthUtil.getToken(
                        context,
                        gAccount,
                        "oauth2:https://www.googleapis.com/auth/drive.file"
                    )
                    _driveBackupStatus.value = "Restoring data from Google Drive..."
                    val success = restoreFromGoogleDriveWithToken(token, context.applicationContext)
                    if (success) {
                        _driveBackupStatus.value = "Success: Restored data and settings from Google Drive!"
                    } else {
                        _driveBackupStatus.value = "Failed: No backup found on Google Drive."
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    _driveBackupStatus.value = "Failed: ${e.localizedMessage}"
                }
            }
        } else {
            _driveBackupStatus.value = "Failed: Not signed in."
        }
    }

    private suspend fun restoreFromGoogleDriveWithToken(token: String, context: Context? = null): Boolean {
        val client = okhttp3.OkHttpClient()
        var restoredAny = false

        // 1. Search and Restore Settings
        try {
            val queryUrl = "https://www.googleapis.com/drive/v3/files?q=" + 
                    java.net.URLEncoder.encode("name='enterfirst_settings.json' and trashed=false", "UTF-8")
            val queryRequest = Request.Builder()
                .url(queryUrl)
                .header("Authorization", "Bearer $token")
                .get()
                .build()

            var settingsFileId: String? = null
            client.newCall(queryRequest).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    val json = JSONObject(bodyStr)
                    val filesArray = json.optJSONArray("files")
                    if (filesArray != null && filesArray.length() > 0) {
                        settingsFileId = filesArray.getJSONObject(0).optString("id")
                    }
                }
            }

            if (settingsFileId != null) {
                val downloadUrl = "https://www.googleapis.com/drive/v3/files/$settingsFileId?alt=media"
                val downloadRequest = Request.Builder()
                    .url(downloadUrl)
                    .header("Authorization", "Bearer $token")
                    .get()
                    .build()

                client.newCall(downloadRequest).execute().use { response ->
                    if (response.isSuccessful) {
                        val content = response.body?.string() ?: ""
                        if (content.isNotEmpty()) {
                            val settingsJson = JSONObject(content)
                            
                            val bank = settingsJson.optString("initial_bank_balance")
                            if (bank.isNotEmpty()) {
                                repository.savePreference("initial_bank_balance", bank)
                                _initialBankBalance.value = bank.toDoubleOrNull() ?: 0.0
                            }
                            
                            val cash = settingsJson.optString("initial_cash_balance")
                            if (cash.isNotEmpty()) {
                                repository.savePreference("initial_cash_balance", cash)
                                _initialCashBalance.value = cash.toDoubleOrNull() ?: 0.0
                            }

                            val completed = settingsJson.optString("is_setup_completed")
                            if (completed.isNotEmpty()) {
                                repository.savePreference("is_setup_completed", completed)
                                _isSetupCompleted.value = completed.toBoolean()
                            }

                            val budgetLimit = settingsJson.optString("monthly_budget_limit")
                            if (budgetLimit.isNotEmpty()) {
                                repository.savePreference("monthly_budget_limit", budgetLimit)
                                _monthlyBudgetLimit.value = budgetLimit.toDoubleOrNull() ?: 0.0
                            }

                            val theme = settingsJson.optString("is_dark_theme")
                            if (theme.isNotEmpty()) {
                                repository.savePreference("is_dark_theme", theme)
                                _isDarkTheme.value = theme.toBoolean()
                            }

                            val colorName = settingsJson.optString("balance_color_name")
                            if (colorName.isNotEmpty()) {
                                repository.savePreference("balance_color_name", colorName)
                                _balanceColorName.value = colorName
                            }

                            val custom = settingsJson.optString("custom_categories")
                            if (custom.isNotEmpty()) {
                                repository.savePreference("custom_categories", custom)
                                val customMap = custom.split(",").mapNotNull {
                                    val parts = it.split(":")
                                    if (parts.size == 2) parts[0] to parts[1] else null
                                }.toMap()
                                _customCategories.value = customMap
                            }

                            val order = settingsJson.optString("category_order")
                            if (order.isNotEmpty()) {
                                repository.savePreference("category_order", order)
                                val defaultList = listOf("Food", "Travel", "Shopping", "Bills", "Health", "Education", "Entertainment", "Others")
                                val list = order.split(",").filter { it.isNotEmpty() }
                                _categoryOrder.value = (list + defaultList).distinct()
                            }
                            
                            restoredAny = true
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Search and Restore Transactions & Receipts from CSV and individual image files
        try {
            val queryUrl = "https://www.googleapis.com/drive/v3/files?q=" + 
                    java.net.URLEncoder.encode("name contains 'enterfirst_backup_' and name contains '.csv' and trashed=false", "UTF-8") +
                    "&orderBy=modifiedTime desc&fields=files(id,name,modifiedTime)"
            val queryRequest = Request.Builder()
                .url(queryUrl)
                .header("Authorization", "Bearer $token")
                .get()
                .build()

            val filesList = mutableListOf<Pair<String, String>>()
            val seenFileNames = mutableSetOf<String>()
            client.newCall(queryRequest).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    val json = JSONObject(bodyStr)
                    val filesArray = json.optJSONArray("files")
                    if (filesArray != null) {
                        for (i in 0 until filesArray.length()) {
                            val f = filesArray.getJSONObject(i)
                            val name = f.optString("name")
                            val id = f.optString("id")
                            if (name.isNotEmpty() && id.isNotEmpty() && !seenFileNames.contains(name)) {
                                seenFileNames.add(name)
                                filesList.add(name to id)
                            }
                        }
                    }
                }
            }

            filesList.sortByDescending { it.first }

            if (filesList.isNotEmpty()) {
                val receiptsDir = if (context != null) File(context.filesDir, "receipts").apply { mkdirs() } else null
                var importedCountTotal = 0
                var downloadedReceiptsCount = 0

                // Map of Drive image file names to their Drive file IDs for fast receipt lookup
                val driveImageCache = mutableMapOf<String, String>()
                try {
                    val imgQueryUrl = "https://www.googleapis.com/drive/v3/files?q=" +
                            java.net.URLEncoder.encode("name contains 'receipt' and trashed=false", "UTF-8")
                    val imgRequest = Request.Builder()
                        .url(imgQueryUrl)
                        .header("Authorization", "Bearer $token")
                        .get()
                        .build()
                    client.newCall(imgRequest).execute().use { imgResponse ->
                        if (imgResponse.isSuccessful) {
                            val imgBody = imgResponse.body?.string() ?: ""
                            val imgJson = JSONObject(imgBody)
                            val imgArray = imgJson.optJSONArray("files")
                            if (imgArray != null) {
                                for (k in 0 until imgArray.length()) {
                                    val item = imgArray.getJSONObject(k)
                                    val imgName = item.optString("name")
                                    val imgId = item.optString("id")
                                    if (imgName.isNotEmpty() && imgId.isNotEmpty()) {
                                        driveImageCache[imgName] = imgId
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                for ((name, fileId) in filesList) {
                    val downloadUrl = "https://www.googleapis.com/drive/v3/files/$fileId?alt=media"
                    val downloadRequest = Request.Builder()
                        .url(downloadUrl)
                        .header("Authorization", "Bearer $token")
                        .get()
                        .build()

                    client.newCall(downloadRequest).execute().use { response ->
                        if (response.isSuccessful) {
                            val csvContent = response.body?.string() ?: ""
                            if (csvContent.isNotEmpty()) {
                                val reader = BufferedReader(InputStreamReader(csvContent.byteInputStream()))
                                val header = reader.readLine()
                                if (header != null && header.contains("Type") && header.contains("Amount")) {
                                    var line: String? = reader.readLine()
                                    while (line != null) {
                                        if (line.trim().isEmpty()) {
                                            line = reader.readLine()
                                            continue
                                        }

                                        val parts = parseCsvLine(line)
                                        if (parts.size >= 10) {
                                            val id = parts[0].toIntOrNull() ?: 0
                                            val type = parts[1]
                                            val amount = parts[2].toDoubleOrNull() ?: 0.0
                                            val party = parts[3]
                                            val category = parts[4]
                                            val note = parts[5]
                                            val timestamp = parts[6].toLongOrNull() ?: System.currentTimeMillis()
                                            val paymentStatus = parts[7]
                                            val paymentMethod = parts[8]
                                            val merchantUpiId = parts[9].ifEmpty { null }
                                            val receiptFileName = if (parts.size >= 11) parts[10].ifEmpty { null } else null

                                            var localReceiptPath: String? = null
                                            if (receiptFileName != null && receiptsDir != null) {
                                                val localFile = File(receiptsDir, receiptFileName)
                                                if (localFile.exists() && localFile.length() > 0) {
                                                    localReceiptPath = localFile.absolutePath
                                                } else {
                                                    // Download receipt image separately from Google Drive
                                                    val driveImgId = driveImageCache[receiptFileName]
                                                    if (driveImgId != null) {
                                                        try {
                                                            val imgDlUrl = "https://www.googleapis.com/drive/v3/files/$driveImgId?alt=media"
                                                            val imgDlReq = Request.Builder()
                                                                .url(imgDlUrl)
                                                                .header("Authorization", "Bearer $token")
                                                                .get()
                                                                .build()
                                                            client.newCall(imgDlReq).execute().use { imgRes ->
                                                                if (imgRes.isSuccessful) {
                                                                    val imgBytes = imgRes.body?.bytes()
                                                                    if (imgBytes != null && imgBytes.isNotEmpty()) {
                                                                        FileOutputStream(localFile).use { fos ->
                                                                            fos.write(imgBytes)
                                                                        }
                                                                        localReceiptPath = localFile.absolutePath
                                                                        downloadedReceiptsCount++
                                                                    }
                                                                }
                                                            }
                                                        } catch (e: Exception) {
                                                            e.printStackTrace()
                                                        }
                                                    }
                                                }
                                            }

                                            val tx = Transaction(
                                                id = id,
                                                type = type,
                                                amount = amount,
                                                party = party,
                                                category = category,
                                                note = note,
                                                timestamp = timestamp,
                                                paymentStatus = paymentStatus,
                                                paymentMethod = paymentMethod,
                                                merchantUpiId = merchantUpiId,
                                                receiptUri = localReceiptPath
                                            )
                                            repository.insertTransaction(tx)
                                            importedCountTotal++
                                        }
                                        line = reader.readLine()
                                    }
                                }
                                reader.close()
                            }
                        }
                    }
                }
                if (importedCountTotal > 0) {
                    restoredAny = true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (restoredAny && !_isSetupCompleted.value) {
            repository.savePreference("is_setup_completed", "true")
            _isSetupCompleted.value = true
        }

        return restoredAny
    }

    private fun backupPreferencesToGoogleDriveWithToken(token: String, client: okhttp3.OkHttpClient): Boolean {
        return try {
            val settingsJson = JSONObject()
            settingsJson.put("initial_bank_balance", _initialBankBalance.value.toString())
            settingsJson.put("initial_cash_balance", _initialCashBalance.value.toString())
            settingsJson.put("is_setup_completed", _isSetupCompleted.value.toString())
            settingsJson.put("monthly_budget_limit", _monthlyBudgetLimit.value.toString())
            settingsJson.put("is_dark_theme", _isDarkTheme.value.toString())
            settingsJson.put("balance_color_name", _balanceColorName.value)
            
            val customStr = _customCategories.value.map { "${it.key}:${it.value}" }.joinToString(",")
            settingsJson.put("custom_categories", customStr)
            
            val orderStr = _categoryOrder.value.joinToString(",")
            settingsJson.put("category_order", orderStr)

            val fileName = "enterfirst_settings.json"
            uploadFileToDrive(
                client = client,
                token = token,
                fileName = fileName,
                fileBytes = settingsJson.toString().toByteArray(Charsets.UTF_8),
                mimeType = "application/json"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun backupToGoogleDriveWithToken(token: String, context: Context? = null) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                _driveBackupStatus.value = "Backing up..."
                val client = okhttp3.OkHttpClient()
                
                // Back up preferences first
                backupPreferencesToGoogleDriveWithToken(token, client)

                val txs = transactions.value
                val csvContent = StringBuilder()
                csvContent.append("ID,Type,Amount,Party,Category,Note,Timestamp,PaymentStatus,PaymentMethod,MerchantUpiId,ReceiptFileName\n")
                
                var uploadedImagesCount = 0
                for (tx in txs) {
                    var receiptDriveFileName = ""
                    if (!tx.receiptUri.isNullOrEmpty()) {
                        val file = File(tx.receiptUri)
                        var validFile: File? = if (file.exists() && file.isFile) file else null
                        if (validFile == null && context != null) {
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

                            // Upload image file separately to Google Drive
                            val imgUploaded = uploadFileToDrive(
                                client = client,
                                token = token,
                                fileName = receiptDriveFileName,
                                fileBytes = validFile.readBytes(),
                                mimeType = mimeType
                            )
                            if (imgUploaded) {
                                uploadedImagesCount++
                            }
                        }
                    }
                    csvContent.append("${tx.id},${tx.type},${tx.amount},\"${tx.party.replace("\"", "\"\"")}\",${tx.category},\"${tx.note.replace("\"", "\"\"")}\",${tx.timestamp},${tx.paymentStatus},${tx.paymentMethod},${tx.merchantUpiId ?: ""},\"${receiptDriveFileName}\"\n")
                }

                val sdf = SimpleDateFormat("yyyy_MM", Locale.getDefault())
                val currentMonthStr = sdf.format(System.currentTimeMillis())
                val baseFileName = "enterfirst_backup_$currentMonthStr"

                // Upload the CSV file (guaranteed clean overwrite)
                val csvFileName = "${baseFileName}.csv"
                val csvUploaded = uploadFileToDrive(
                    client = client,
                    token = token,
                    fileName = csvFileName,
                    fileBytes = csvContent.toString().toByteArray(Charsets.UTF_8),
                    mimeType = "text/csv"
                )

                if (!csvUploaded) {
                    _driveBackupStatus.value = "Failed: Could not upload CSV backup to Google Drive."
                    return@launch
                }

                val receiptMsg = if (uploadedImagesCount > 0) " and $uploadedImagesCount receipt image(s)" else ""
                _driveBackupStatus.value = "Success: Backed up CSV$receiptMsg to Google Drive!"
                updateLastBackupTimeAndSave()
            } catch (e: Exception) {
                e.printStackTrace()
                _driveBackupStatus.value = "Failed: ${e.localizedMessage}"
            }
        }
    }

    private fun uploadFileToDrive(
        client: okhttp3.OkHttpClient,
        token: String,
        fileName: String,
        fileBytes: ByteArray,
        mimeType: String
    ): Boolean {
        try {
            // 1. Search for any existing copies of this file in Google Drive
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
                Log.w("FinanceViewModel", "Warning querying existing file $fileName: ${e.message}")
            }

            // 2. Fix overwrite issue: delete any existing versions of this file from Drive
            // so there are never stale, duplicate, or conflicting copies
            for (oldFileId in existingFileIds) {
                try {
                    val deleteRequest = Request.Builder()
                        .url("https://www.googleapis.com/drive/v3/files/$oldFileId")
                        .header("Authorization", "Bearer $token")
                        .delete()
                        .build()
                    client.newCall(deleteRequest).execute().use { delRes ->
                        Log.d("FinanceViewModel", "Deleted old Drive file $oldFileId for $fileName (status=${delRes.code})")
                    }
                } catch (e: Exception) {
                    Log.w("FinanceViewModel", "Warning deleting old Drive file $oldFileId: ${e.message}")
                }
            }

            // 3. Upload fresh file using multipart/related upload to guarantee clean overwrite
            val uploadUrl = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"
            val boundary = "enterfirst_boundary_${System.currentTimeMillis()}"
            val metaJson = "{\"name\": \"$fileName\", \"mimeType\": \"$mimeType\"}"
            
            val byteBuffer = java.io.ByteArrayOutputStream()
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
                    Log.e("FinanceViewModel", "Drive upload failed for $fileName: code=${response.code}, err=$errBody")
                    return false
                }
                Log.d("FinanceViewModel", "Successfully backed up and overwrote $fileName in Google Drive")
                return true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("FinanceViewModel", "Exception uploading $fileName to Drive: ${e.message}")
            return false
        }
    }
}

class FinanceViewModelFactory(private val repository: FinanceRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FinanceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FinanceViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

data class UpiQrCode(
    val id: String = java.util.UUID.randomUUID().toString(),
    val appName: String = "GPay",
    val imagePath: String = ""
)
