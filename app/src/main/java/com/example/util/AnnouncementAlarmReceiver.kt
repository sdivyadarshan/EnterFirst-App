package com.example.util

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.AppDatabase
import com.example.data.FirebaseUserManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * High-reliability BroadcastReceiver triggered by AlarmManager
 * to poll for announcements and notifications in the background even if the app process is closed.
 */
class AnnouncementAlarmReceiver : android.content.BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        Log.d(TAG, "AnnouncementAlarmReceiver received wakeup alarm")

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                FirebaseUserManager.init(context)
                val cloudAnnouncements = FirebaseUserManager.fetchAnnouncementsFromFirebase(context)
                val cloudIds = cloudAnnouncements.map { it.id }.toSet() + cloudAnnouncements.map { it.timestamp }.toSet()
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
                    dao.insertAnnouncement(announcement)
                    if (announcement.isCurrentlyActive(now) && !AppNotificationHelper.isAlreadyNotified(context, announcement)) {
                        Log.d(TAG, "AlarmReceiver: Triggering notification for ${announcement.title}")
                        AppNotificationHelper.showAnnouncementNotification(
                            context = context,
                            announcement = announcement
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "AlarmReceiver background poll error: ${e.message}")
            } finally {
                // Re-arm next alarm heartbeat to ensure ongoing background delivery
                AnnouncementSyncScheduler.scheduleNextAlarm(context)
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val TAG = "AnnouncementAlarmReceiver"
        const val ACTION_POLL_ANNOUNCEMENTS = "com.example.ACTION_POLL_ANNOUNCEMENTS"
    }
}
