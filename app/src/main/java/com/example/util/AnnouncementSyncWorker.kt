package com.example.util

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.AppDatabase
import com.example.data.FirebaseUserManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Background WorkManager worker that polls for newly broadcasted
 * announcements & app updates from the cloud when the app is closed.
 */
class AnnouncementSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val context = applicationContext
        Log.d(TAG, "AnnouncementSyncWorker executing background sync...")

        try {
            FirebaseUserManager.init(context)
            val cloudAnnouncements = FirebaseUserManager.fetchAnnouncementsFromFirebase(context)
            val cloudIds = cloudAnnouncements.map { it.id }.toSet() + cloudAnnouncements.map { it.timestamp }.toSet()
            Log.d(TAG, "Fetched ${cloudAnnouncements.size} announcements in background")

            val database = AppDatabase.getDatabase(context)
            val dao = database.announcementDao()
            val now = System.currentTimeMillis()

            // Purge local announcements deleted by Admin from Firebase
            val localList = dao.getAllAnnouncementsList()
            for (localItem in localList) {
                if (localItem.title != "Welcome to EnterFirst Finance & UPI") {
                    if (localItem.id !in cloudIds && localItem.timestamp !in cloudIds) {
                        dao.deleteAnnouncement(localItem.id)
                        AppNotificationHelper.cancelNotification(context, localItem.id)
                    }
                }
            }

            for (announcement in cloudAnnouncements) {
                // Save to local database
                dao.insertAnnouncement(announcement)

                // If currently active and not yet notified, post notification to device
                if (announcement.isCurrentlyActive(now) && !AppNotificationHelper.isAlreadyNotified(context, announcement)) {
                    Log.d(TAG, "Posting background notification for announcement: ${announcement.title}")
                    AppNotificationHelper.showAnnouncementNotification(
                        context = context,
                        announcement = announcement
                    )
                }
            }

            // Check & perform scheduled auto backup to Google Drive if due (Daily, Weekly, Monthly)
            try {
                GoogleDriveBackupHelper.performSilentAutoBackup(context)
            } catch (e: Exception) {
                Log.w(TAG, "Background auto backup check skipped: ${e.message}")
            }

            Result.success()
        } catch (e: Exception) {
            Log.w(TAG, "Background announcement sync failed: ${e.message}")
            Result.retry()
        }
    }

    companion object {
        const val TAG = "AnnouncementSyncWorker"
        const val WORK_NAME_PERIODIC = "enterfirst_announcement_periodic_sync"
        const val WORK_NAME_EXPEDITED = "enterfirst_announcement_expedited_sync"
    }
}
