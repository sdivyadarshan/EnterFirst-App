package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.FinanceRepository
import com.example.data.Transaction
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
import java.io.FileOutputStream
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

    private val _recoveryIntent = MutableStateFlow<android.content.Intent?>(null)
    val recoveryIntent: StateFlow<android.content.Intent?> = _recoveryIntent.asStateFlow()

    fun clearRecoveryIntent() {
        _recoveryIntent.value = null
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
            }
            _isPreferencesLoaded.value = true
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
        receiptUri: String? = null
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
                timestamp = System.currentTimeMillis()
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

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
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

    // CSV Operations
    fun exportToCsv(context: Context, onResult: (Uri?) -> Unit) {
        viewModelScope.launch {
            try {
                val txs = transactions.value
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
        var curVal = StringBuilder()
        var inQuotes = false
        for (i in 0 until line.length) {
            val ch = line[i]
            if (inQuotes) {
                if (ch == '\"') {
                    if (i + 1 < line.length && line[i + 1] == '\"') {
                        curVal.append('\"')
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
                    curVal = StringBuilder()
                } else {
                    curVal.append(ch)
                }
            }
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
        viewModelScope.launch {
            try {
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    val txs = transactions.value
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

    fun setGoogleAccount(email: String?, name: String?) {
        viewModelScope.launch {
            _googleEmail.value = email
            _googleName.value = name
            repository.savePreference("google_email", email ?: "")
            repository.savePreference("google_name", name ?: "")
            if (email == null) {
                _driveBackupStatus.value = "Idle"
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
                    val success = restoreFromGoogleDriveWithToken(token)
                    if (success) {
                        _driveBackupStatus.value = "Success: Restored data and settings from Google Drive!"
                    } else {
                        _driveBackupStatus.value = "Failed to restore backup from Google Drive."
                    }
                } else {
                    _driveBackupStatus.value = "No existing backup found. Creating first backup..."
                    backupToGoogleDriveWithToken(token)
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
                    backupToGoogleDriveWithToken(token)
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
                    val success = restoreFromGoogleDriveWithToken(token)
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

    private suspend fun restoreFromGoogleDriveWithToken(token: String): Boolean {
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

        // 2. Search and Restore Transactions CSV
        try {
            val queryUrl = "https://www.googleapis.com/drive/v3/files?q=" + 
                    java.net.URLEncoder.encode("name contains 'enterfirst_backup_' and name contains '.csv' and trashed=false", "UTF-8")
            val queryRequest = Request.Builder()
                .url(queryUrl)
                .header("Authorization", "Bearer $token")
                .get()
                .build()

            val filesList = mutableListOf<Pair<String, String>>()
            client.newCall(queryRequest).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    val json = JSONObject(bodyStr)
                    val filesArray = json.optJSONArray("files")
                    if (filesArray != null) {
                        for (i in 0 until filesArray.length()) {
                            val f = filesArray.getJSONObject(i)
                            filesList.add(f.optString("name") to f.optString("id"))
                        }
                    }
                }
            }

            filesList.sortBy { it.first }

            if (filesList.isNotEmpty()) {
                var importedCountTotal = 0
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
                                                merchantUpiId = merchantUpiId
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
                    if (!_isSetupCompleted.value) {
                        repository.savePreference("is_setup_completed", "true")
                        _isSetupCompleted.value = true
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return restoredAny
    }

    private fun backupPreferencesToGoogleDriveWithToken(token: String, client: okhttp3.OkHttpClient) {
        try {
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
            val queryUrl = "https://www.googleapis.com/drive/v3/files?q=" + 
                    java.net.URLEncoder.encode("name='$fileName' and trashed=false", "UTF-8")
            
            val queryRequest = Request.Builder()
                .url(queryUrl)
                .header("Authorization", "Bearer $token")
                .get()
                .build()

            var existingFileId: String? = null
            client.newCall(queryRequest).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    val json = JSONObject(bodyStr)
                    val filesArray = json.optJSONArray("files")
                    if (filesArray != null && filesArray.length() > 0) {
                        existingFileId = filesArray.getJSONObject(0).optString("id")
                    }
                }
            }

            if (existingFileId != null) {
                val uploadUrl = "https://www.googleapis.com/upload/drive/v3/files/$existingFileId?uploadType=media"
                val requestBody = settingsJson.toString().toRequestBody("application/json".toMediaTypeOrNull())
                val updateRequest = Request.Builder()
                    .url(uploadUrl)
                    .header("Authorization", "Bearer $token")
                    .patch(requestBody)
                    .build()

                client.newCall(updateRequest).execute()
            } else {
                val uploadUrl = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"
                val boundary = "settings_boundary"
                val bodyBuilder = StringBuilder()
                bodyBuilder.append("--").append(boundary).append("\r\n")
                bodyBuilder.append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
                bodyBuilder.append("{\"name\": \"").append(fileName).append("\", \"mimeType\": \"application/json\"}\r\n")
                bodyBuilder.append("--").append(boundary).append("\r\n")
                bodyBuilder.append("Content-Type: application/json\r\n\r\n")
                bodyBuilder.append(settingsJson.toString()).append("\r\n")
                bodyBuilder.append("--").append(boundary).append("--\r\n")

                val requestBody = bodyBuilder.toString().toByteArray(Charsets.UTF_8).toRequestBody("multipart/related; boundary=$boundary".toMediaTypeOrNull())

                val createRequest = Request.Builder()
                    .url(uploadUrl)
                    .header("Authorization", "Bearer $token")
                    .post(requestBody)
                    .build()

                client.newCall(createRequest).execute()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun backupToGoogleDriveWithToken(token: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                _driveBackupStatus.value = "Backing up..."
                val client = okhttp3.OkHttpClient()
                
                // Back up preferences first
                backupPreferencesToGoogleDriveWithToken(token, client)

                val txs = transactions.value
                val csvContent = StringBuilder()
                csvContent.append("ID,Type,Amount,Party,Category,Note,Timestamp,PaymentStatus,PaymentMethod,MerchantUpiId\n")
                for (tx in txs) {
                    csvContent.append("${tx.id},${tx.type},${tx.amount},\"${tx.party.replace("\"", "\"\"")}\",${tx.category},\"${tx.note.replace("\"", "\"\"")}\",${tx.timestamp},${tx.paymentStatus},${tx.paymentMethod},${tx.merchantUpiId ?: ""}\n")
                }

                val sdf = SimpleDateFormat("yyyy_MM", Locale.getDefault())
                val currentMonthStr = sdf.format(System.currentTimeMillis())
                val fileName = "enterfirst_backup_$currentMonthStr.csv"

                val queryUrl = "https://www.googleapis.com/drive/v3/files?q=" + 
                        java.net.URLEncoder.encode("name='$fileName' and trashed=false", "UTF-8")
                
                val queryRequest = Request.Builder()
                    .url(queryUrl)
                    .header("Authorization", "Bearer $token")
                    .get()
                    .build()

                var existingFileId: String? = null
                client.newCall(queryRequest).execute().use { response ->
                    if (response.isSuccessful) {
                        val bodyStr = response.body?.string() ?: ""
                        val json = JSONObject(bodyStr)
                        val filesArray = json.optJSONArray("files")
                        if (filesArray != null && filesArray.length() > 0) {
                            existingFileId = filesArray.getJSONObject(0).optString("id")
                        }
                    }
                }

                if (existingFileId != null) {
                    val uploadUrl = "https://www.googleapis.com/upload/drive/v3/files/$existingFileId?uploadType=media"
                    val requestBody = csvContent.toString().toRequestBody("text/csv".toMediaTypeOrNull())
                    val updateRequest = Request.Builder()
                        .url(uploadUrl)
                        .header("Authorization", "Bearer $token")
                        .patch(requestBody)
                        .build()

                    client.newCall(updateRequest).execute().use { response ->
                        if (response.isSuccessful) {
                            _driveBackupStatus.value = "Success: $fileName updated on Drive!"
                        } else {
                            val errBody = response.body?.string() ?: ""
                            _driveBackupStatus.value = "Failed to update: HTTP ${response.code} $errBody"
                        }
                    }
                } else {
                    val uploadUrl = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"
                    val boundary = "foo_bar_baz"
                    val bodyBuilder = StringBuilder()
                    bodyBuilder.append("--").append(boundary).append("\r\n")
                    bodyBuilder.append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
                    bodyBuilder.append("{\"name\": \"").append(fileName).append("\", \"mimeType\": \"text/csv\"}\r\n")
                    bodyBuilder.append("--").append(boundary).append("\r\n")
                    bodyBuilder.append("Content-Type: text/csv\r\n\r\n")
                    bodyBuilder.append(csvContent).append("\r\n")
                    bodyBuilder.append("--").append(boundary).append("--\r\n")

                    val requestBody = bodyBuilder.toString().toByteArray(Charsets.UTF_8).toRequestBody("multipart/related; boundary=$boundary".toMediaTypeOrNull())

                    val createRequest = Request.Builder()
                        .url(uploadUrl)
                        .header("Authorization", "Bearer $token")
                        .post(requestBody)
                        .build()

                    client.newCall(createRequest).execute().use { response ->
                        if (response.isSuccessful) {
                            _driveBackupStatus.value = "Success: $fileName uploaded to Drive!"
                        } else {
                            val errBody = response.body?.string() ?: ""
                            _driveBackupStatus.value = "Failed to upload: HTTP ${response.code} $errBody"
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _driveBackupStatus.value = "Failed: ${e.localizedMessage}"
            }
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
