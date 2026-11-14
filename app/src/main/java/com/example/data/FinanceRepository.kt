package com.example.data

import kotlinx.coroutines.flow.Flow

class FinanceRepository(
    private val transactionDao: TransactionDao,
    private val preferenceDao: PreferenceDao,
    private val announcementDao: AnnouncementDao
) {
    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()
    val allAnnouncements: Flow<List<AppAnnouncement>> = announcementDao.getAllAnnouncements()
    val unreadAnnouncementsCount: Flow<Int> = announcementDao.getUnreadCount()
    val allUsers: Flow<List<AppUser>> = announcementDao.getAllUsers()

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

    suspend fun getAllAnnouncementsList(): List<AppAnnouncement> {
        return announcementDao.getAllAnnouncementsList()
    }

    suspend fun postAnnouncement(announcement: AppAnnouncement): Long {
        return announcementDao.insertAnnouncement(announcement)
    }

    suspend fun markAnnouncementRead(id: Long) {
        announcementDao.markAsRead(id)
    }

    suspend fun markAllAnnouncementsRead() {
        announcementDao.markAllAsRead()
    }

    suspend fun deleteAnnouncement(id: Long) {
        announcementDao.deleteAnnouncement(id)
    }

    suspend fun clearAllAnnouncementsExceptWelcome() {
        announcementDao.clearAllExceptWelcome()
    }

    suspend fun clearAllAnnouncements() {
        announcementDao.clearAllAnnouncements()
    }

    suspend fun getAnnouncementCount(): Int {
        return announcementDao.getAnnouncementCount()
    }

    suspend fun registerOrUpdateUser(user: AppUser) {
        announcementDao.insertOrUpdateUser(user)
    }
}
