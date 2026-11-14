package com.example.util

import android.util.Log
import com.example.data.AppAnnouncement
import com.example.data.AppDatabase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Handles incoming realtime Firebase Cloud Messaging (FCM) push notifications
 * pushed from Firebase console or cloud function even when the app is completely closed.
 */
class EnterFirstFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM Token received: $token")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "FCM message received from: ${remoteMessage.from}")

        val data = remoteMessage.data

        // ------------------------------------------------------------------------------------
        // PROTOCOL: HOT-FIX DATA PAYLOAD INTERCEPTION & PATCHING
        // Intercepts data key "hot_fix_prompt", overwrites ocr_config.json, and applies update live
        // ------------------------------------------------------------------------------------
        val hotFixPrompt = data["hot_fix_prompt"]
        if (!hotFixPrompt.isNullOrBlank()) {
            val version = data["version"]?.toIntOrNull() ?: (com.example.ocr.OcrConfigManager.getCurrentVersion() + 1)
            Log.i(TAG, "INTERCEPTED FCM hot_fix_prompt (len=${hotFixPrompt.length}, ver=$version). Setting pending fix...")
            val isNew = com.example.ocr.OcrConfigManager.setPendingHotFix(
                context = applicationContext,
                newPrompt = hotFixPrompt,
                version = version
            )
            if (isNew) {
                AppNotificationHelper.showOcrBugFixNotification(
                    context = applicationContext,
                    newPrompt = hotFixPrompt,
                    version = version
                )
            }
            // If the push message is solely a hot-fix payload with no separate announcement, return
            if (data["title"] == null && remoteMessage.notification == null) {
                Log.d(TAG, "OCR bug fix notification posted with 'Fix Now' action.")
                return
            }
        }

        val title = data["title"] ?: remoteMessage.notification?.title ?: "EnterFirst Notice"
        val message = data["message"] ?: data["body"] ?: remoteMessage.notification?.body ?: ""
        val apkUrl = data["apkDownloadUrl"] ?: data["apkUrl"]
        val versionCode = data["versionCode"]
        val announcementId = data["id"]?.toLongOrNull() ?: data["timestamp"]?.toLongOrNull() ?: System.currentTimeMillis()
        val category = data["category"] ?: "NOTICE"
        val priority = data["priority"] ?: "HIGH"
        val scheduledAppearTime = data["scheduledAppearTime"]?.toLongOrNull()
        val scheduledDisappearTime = data["scheduledDisappearTime"]?.toLongOrNull()

        val announcement = AppAnnouncement(
            id = announcementId,
            title = title,
            message = message,
            category = category,
            timestamp = announcementId,
            priority = priority,
            apkDownloadUrl = apkUrl,
            versionCode = versionCode,
            scheduledAppearTime = scheduledAppearTime,
            scheduledDisappearTime = scheduledDisappearTime
        )

        // Save announcement to local DB
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(applicationContext)
                db.announcementDao().insertAnnouncement(announcement)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to save FCM announcement to DB: ${e.message}")
            }
        }

        // Post deduplicated notification if active
        if (announcement.isCurrentlyActive()) {
            AppNotificationHelper.showAnnouncementNotification(
                context = applicationContext,
                announcement = announcement
            )
        }
    }

    companion object {
        const val TAG = "EnterFirstFCMService"
    }
}
