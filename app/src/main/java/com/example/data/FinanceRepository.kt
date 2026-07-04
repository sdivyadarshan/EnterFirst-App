package com.example.data

import kotlinx.coroutines.flow.Flow

class FinanceRepository(
    private val transactionDao: TransactionDao,
    private val preferenceDao: PreferenceDao
) {
    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()

    fun getPreferenceFlow(key: String): Flow<Preference?> {
        return preferenceDao.getPreferenceFlow(key)
    }

    suspend fun getPreferenceValue(key: String): String? {
        return preferenceDao.getPreferenceValue(key)
    }

    suspend fun savePreference(key: String, value: String) {
        preferenceDao.insertPreference(Preference(key, value))
    }

    suspend fun insertTransaction(transaction: Transaction): Long {
        return transactionDao.insertTransaction(transaction)
    }

    suspend fun getTransactionById(id: Int): Transaction? {
        return transactionDao.getTransactionById(id)
    }

    suspend fun deleteTransaction(transaction: Transaction) {
        transactionDao.deleteTransaction(transaction)
    }

    suspend fun resetAllData() {
        transactionDao.deleteAllTransactions()
        preferenceDao.deleteAllPreferences()
    }
}
