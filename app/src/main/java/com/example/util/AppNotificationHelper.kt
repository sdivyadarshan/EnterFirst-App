package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.data.AppAnnouncement
import kotlin.math.abs

/**
 * Manages system push and local notifications for EnterFirst app updates,
 * admin broadcasts, and notice announcements.
 *
 * Implements strict deduplication logic to ensure only ONE notification
 * is posted per announcement content/identity, completely eliminating multi-duplicate notifications.
 */
object AppNotificationHelper {
    private const val TAG = "AppNotificationHelper"
    const val CHANNEL_ID = "enterfirst_app_updates_channel"
    const val CHANNEL_NAME = "App Updates & Announcements"
    private const val PREFS_NAME = "enterfirst_notification_dedup_v2"
    private const val DEFAULT_NOTIFICATION_ID = 5001

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for updates, features, and admin broadcasts"
                enableVibration(true)
                setShowBadge(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    /**
     * Generates a deterministic content-based key to prevent duplicate notifications
     * across different triggers (FCM, WorkManager, AlarmManager, Realtime Listener).
     */
    fun getAnnouncementKey(announcement: AppAnnouncement): String {
        val cleanTitle = announcement.title.trim().lowercase()
        val cleanMsg = announcement.message.trim().lowercase().take(40)
        return "ann_${abs(cleanTitle.hashCode())}_${abs(cleanMsg.hashCode())}"
    }

    /**
     * Checks if this announcement has already been posted as a system notification.
     */
    fun isAlreadyNotified(context: Context, announcement: AppAnnouncement): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = getAnnouncementKey(announcement)
        if (prefs.getBoolean("key_$key", false)) return true
        if (announcement.id > 0 && prefs.getBoolean("id_${announcement.id}", false)) return true
        return false
    }

    /**
     * Legacy helper supporting ID lookup.
     */
    fun isAlreadyNotified(context: Context, announcementId: Long): Boolean {
        if (announcementId <= 0) return false
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean("id_$announcementId", false)
    }

    /**
     * Atomically marks an announcement as notified.
     * Returns true if newly marked, false if previously marked.
     */
    @Synchronized
    fun markAsNotified(context: Context, announcement: AppAnnouncement): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = getAnnouncementKey(announcement)
        val alreadyByKey = prefs.getBoolean("key_$key", false)
        val alreadyById = if (announcement.id > 0) prefs.getBoolean("id_${announcement.id}", false) else false

        if (alreadyByKey || alreadyById) {
            return false // Already notified!
        }

        prefs.edit()
            .putBoolean("key_$key", true)
            .apply {
                if (announcement.id > 0) {
                    putBoolean("id_${announcement.id}", true)
                }
            }
            .apply()
        return true
    }

    @Synchronized
    fun markAsNotified(context: Context, announcementId: Long): Boolean {
        if (announcementId <= 0) return true
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = "id_$announcementId"
        if (prefs.getBoolean(key, false)) return false
        prefs.edit().putBoolean(key, true).apply()
        return true
    }

    /**
     * Clears all deduplication history when resetting announcements.
     */
    fun clearAllNotifiedKeys(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.cancelAll()
        } catch (e: Exception) {
            // Ignore
        }
    }

    /**
     * Cancels an active system notification when an announcement is deleted.
     */
    fun cancelNotification(context: Context, announcementId: Long) {
        if (announcementId <= 0) return
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().remove("id_$announcementId").apply()

            val notificationManager = NotificationManagerCompat.from(context)
            val notificationId = abs(announcementId.hashCode() % 10000) + 5000
            notificationManager.cancel(notificationId)
            notificationManager.cancel(announcementId.toInt())
        } catch (e: Exception) {
            // Ignore
        }
    }

    /**
     * Cancels an active system notification by announcement object.
     */
    fun cancelNotification(context: Context, announcement: AppAnnouncement) {
        try {
            val key = getAnnouncementKey(announcement)
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .remove("key_$key")
                .remove("id_${announcement.id}")
                .apply()

            val notificationManager = NotificationManagerCompat.from(context)
            val notificationId = abs(key.hashCode() % 10000) + 5000
            val tag = "enterfirst_$key"
            notificationManager.cancel(tag, notificationId)
            notificationManager.cancel(announcement.id.toInt())
        } catch (e: Exception) {
            // Ignore
        }
    }

    /**
     * Posts an announcement notification with strict deduplication and schedule awareness.
     */
    fun showAnnouncementNotification(
        context: Context,
        announcement: AppAnnouncement,
        forceShow: Boolean = false
    ) {
        // Enforce scheduling time window
        val now = System.currentTimeMillis()
        if (!announcement.isCurrentlyActive(now)) {
            Log.d(TAG, "Suppressed notification for '${announcement.title}' - not active in schedule window.")
            return
        }

        // Deduplication check
        if (!forceShow) {
            val isNew = markAsNotified(context, announcement)
            if (!isNew) {
                Log.d(TAG, "Suppressed duplicate notification for '${announcement.title}' (key: ${getAnnouncementKey(announcement)})")
                return
            }
        }

        if (announcement.category == "HOT_FIX") {
            val prompt = com.example.ocr.OcrConfigManager.getSystemPrompt(context)
            val ver = com.example.ocr.OcrConfigManager.getCurrentVersion()
            com.example.ocr.OcrConfigManager.setPendingHotFix(context, prompt, ver)
            showOcrBugFixNotification(context, prompt, ver)
            return
        }

        showUpdateNotificationInternal(
            context = context,
            title = announcement.title,
            message = announcement.message,
            apkUrl = announcement.apkDownloadUrl,
            versionCode = announcement.versionCode,
            announcementKey = getAnnouncementKey(announcement),
            announcementId = announcement.id
        )
    }

    /**
     * Displays an update / announcement notification with deterministic tags and IDs
     * so Android automatically updates/collapses duplicates instead of stacking them.
     */
    fun showUpdateNotification(
        context: Context,
        title: String,
        message: String,
        apkUrl: String? = null,
        versionCode: String? = null,
        announcementId: Long = 0L,
        forceShow: Boolean = false
    ) {
        val dummyAnnouncement = AppAnnouncement(
            id = announcementId,
            title = title,
            message = message,
            apkDownloadUrl = apkUrl,
            versionCode = versionCode
        )
        showAnnouncementNotification(context, dummyAnnouncement, forceShow)
    }

    private fun showUpdateNotificationInternal(
        context: Context,
        title: String,
        message: String,
        apkUrl: String? = null,
        versionCode: String? = null,
        announcementKey: String,
        announcementId: Long = 0L
    ) {
        createNotificationChannel(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_ANNOUNCEMENTS", true)
            putExtra("ANNOUNCEMENT_ID", announcementId)
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            abs(announcementKey.hashCode() % 10000),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        // Clean title formatting
        val cleanTitle = title.trim().removePrefix("📢").removePrefix("🚀").trim()
        val formattedTitle = if (!versionCode.isNullOrBlank()) {
            "🚀 Update ($versionCode): $cleanTitle"
        } else {
            "📢 $cleanTitle"
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(formattedTitle)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openAppPendingIntent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)

        if (!apkUrl.isNullOrBlank()) {
            try {
                val downloadIntent = Intent(Intent.ACTION_VIEW, Uri.parse(apkUrl)).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                val downloadPendingIntent = PendingIntent.getActivity(
                    context,
                    abs((announcementKey + "_apk").hashCode() % 10000),
                    downloadIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
                )
                builder.addAction(
                    android.R.drawable.stat_sys_download,
                    "Download APK",
                    downloadPendingIntent
                )
            } catch (e: Exception) {
                // Ignore uri parsing failure
            }
        }

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            // Deterministic notification tag and ID prevents multiple stacked duplicates
            val notificationId = abs(announcementKey.hashCode() % 10000) + 5000
            val tag = "enterfirst_$announcementKey"

            notificationManager.notify(tag, notificationId, builder.build())
            Log.d(TAG, "Notification displayed for tag=$tag, id=$notificationId, title='$title'")
        } catch (e: SecurityException) {
            Log.w(TAG, "Notification permission missing: ${e.message}")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    const val OCR_BUG_FIX_NOTIFICATION_ID = 5999
    const val OCR_BUG_FIX_TAG = "enterfirst_ocr_fix"

    /**
     * Displays a system notification informing the user that an OCR bug fix is available.
     * Contains an explicit 'Fix Now' action that triggers the optimization test UI and patches the prompt.
     */
    fun showOcrBugFixNotification(context: Context, newPrompt: String, version: Int) {
        createNotificationChannel(context)

        val fixIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("ACTION_APPLY_HOT_FIX", true)
            putExtra("HOT_FIX_PROMPT", newPrompt)
            putExtra("HOT_FIX_VERSION", version)
        }

        val fixPendingIntent = PendingIntent.getActivity(
            context,
            OCR_BUG_FIX_NOTIFICATION_ID,
            fixIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Fix OCR Bug")
            .setContentText("Admin released an updated OCR prompt. Tap 'Fix' to apply.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Admin released an updated Gemini AI OCR system prompt (v$version). Tap 'Fix' to patch your app logic and run the optimization pipeline.")
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentIntent(fixPendingIntent)
            .setAutoCancel(true)
            .addAction(
                android.R.drawable.ic_menu_rotate,
                "Fix",
                fixPendingIntent
            )

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(OCR_BUG_FIX_TAG, OCR_BUG_FIX_NOTIFICATION_ID, builder.build())
            Log.d(TAG, "Displayed OCR Bug Fix notification with 'Fix Now' action (v$version)")
        } catch (e: SecurityException) {
            Log.w(TAG, "Notification permission missing for OCR fix: ${e.message}")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun cancelOcrBugFixNotification(context: Context) {
        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.cancel(OCR_BUG_FIX_TAG, OCR_BUG_FIX_NOTIFICATION_ID)
        } catch (e: Exception) {
            // Ignore
        }
    }
}
