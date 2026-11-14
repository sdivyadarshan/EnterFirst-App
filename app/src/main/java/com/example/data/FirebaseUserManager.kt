package com.example.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * Manages cloud storage of user accounts and system broadcasts using Firebase Firestore.
 * Features:
 * 1. Auto-registration of user accounts on login / app launch.
 * 2. Real-time announcement listening via Firestore Snapshot Listener.
 * 3. Robust REST fallback for environments where Firestore SDK might need alternative routing.
 * 4. Automatic cloud sync on app launch when internet is active.
 * 5. Admin deletion and scheduling synchronization.
 */
object FirebaseUserManager {
    private const val TAG = "FirebaseUserManager"
    private const val PROJECT_ID = "enterfirst-d93c1"
    private const val COLLECTION_USERS = "users"
    private const val COLLECTION_ANNOUNCEMENTS = "announcements"

    private var isFirebaseInitialized: Boolean = false
    private var announcementListener: ListenerRegistration? = null

    fun isNetworkAvailable(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            if (cm != null) {
                val network = cm.activeNetwork ?: return false
                val capabilities = cm.getNetworkCapabilities(network) ?: return false
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            } else {
                false
            }
        } catch (e: Exception) {
            true
        }
    }

    fun init(context: Context) {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            isFirebaseInitialized = true
            Log.d(TAG, "Firebase initialized successfully")
        } catch (e: Exception) {
            Log.w(TAG, "Firebase initialization error: ${e.message}")
            isFirebaseInitialized = false
        }
    }

    /**
     * Registers or updates a user in Firebase Firestore.
     */
    suspend fun registerUserInFirebase(user: AppUser, context: Context? = null): Boolean = withContext(Dispatchers.IO) {
        if (user.email.isBlank()) return@withContext false

        // Attempt 1: Standard Firebase Firestore SDK
        try {
            if (context != null && !isFirebaseInitialized) {
                init(context)
            }
            val firestore = FirebaseFirestore.getInstance()
            val userData = hashMapOf(
                "email" to user.email.trim(),
                "displayName" to user.displayName.trim(),
                "photoUrl" to (user.photoUrl ?: ""),
                "lastLoginTime" to user.lastLoginTime,
                "isAdmin" to user.isAdmin,
                "deviceInfo" to "${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})",
                "appVersion" to "2.5.0",
                "updatedAt" to System.currentTimeMillis()
            )
            firestore.collection(COLLECTION_USERS)
                .document(user.email.trim().replace(".", "_dot_"))
                .set(userData, SetOptions.merge())
                .await()
            Log.d(TAG, "User ${user.email} registered in Firebase Firestore SDK")
            return@withContext true
        } catch (e: Exception) {
            Log.w(TAG, "Firestore SDK register error: ${e.message}. Attempting REST fallback...")
        }

        // Attempt 2: Direct REST Sync
        try {
            val client = OkHttpClient.Builder()
                .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .build()

            val docId = user.email.trim().replace(".", "_dot_")
            val jsonPayload = JSONObject().apply {
                put("fields", JSONObject().apply {
                    put("email", JSONObject().put("stringValue", user.email.trim()))
                    put("displayName", JSONObject().put("stringValue", user.displayName.trim()))
                    put("lastLoginTime", JSONObject().put("integerValue", user.lastLoginTime.toString()))
                    put("isAdmin", JSONObject().put("booleanValue", user.isAdmin))
                    put("deviceInfo", JSONObject().put("stringValue", "${Build.MANUFACTURER} ${Build.MODEL}"))
                })
            }

            val body = jsonPayload.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("https://firestore.googleapis.com/v1/projects/$PROJECT_ID/databases/(default)/documents/$COLLECTION_USERS/$docId")
                .patch(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Log.d(TAG, "User registered via Firestore REST")
                    return@withContext true
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Firestore REST register: ${e.message}")
        }

        return@withContext false
    }

    /**
     * Fetches all registered users from Firebase Firestore for the Admin.
     */
    suspend fun fetchAllUsersFromFirebase(context: Context? = null): List<AppUser> = withContext(Dispatchers.IO) {
        val userList = mutableListOf<AppUser>()

        try {
            if (context != null && !isFirebaseInitialized) {
                init(context)
            }
            val firestore = FirebaseFirestore.getInstance()
            val snapshot = firestore.collection(COLLECTION_USERS).get().await()
            for (doc in snapshot.documents) {
                val email = doc.getString("email") ?: doc.id.replace("_dot_", ".")
                val displayName = doc.getString("displayName") ?: ""
                val photoUrl = doc.getString("photoUrl")
                val lastLogin = doc.getLong("lastLoginTime") ?: System.currentTimeMillis()
                val isAdmin = doc.getBoolean("isAdmin") ?: false

                if (email.isNotBlank()) {
                    userList.add(
                        AppUser(
                            email = email,
                            displayName = displayName,
                            photoUrl = photoUrl,
                            lastLoginTime = lastLogin,
                            isAdmin = isAdmin
                        )
                    )
                }
            }
            if (userList.isNotEmpty()) {
                Log.d(TAG, "Fetched ${userList.size} users from Firebase Firestore SDK")
                return@withContext userList
            }
        } catch (e: Exception) {
            Log.w(TAG, "Firestore SDK fetch users error: ${e.message}")
        }

        return@withContext userList
    }

    /**
     * Broadcasts an announcement to Firebase Firestore so all user devices receive it.
     */
    suspend fun broadcastAnnouncementToFirebase(announcement: AppAnnouncement, context: Context? = null): Boolean = withContext(Dispatchers.IO) {
        var broadcastSuccess = false

        // Attempt 1: Standard Firebase Firestore SDK
        try {
            if (context != null && !isFirebaseInitialized) {
                init(context)
            }
            val firestore = FirebaseFirestore.getInstance()
            val data = hashMapOf<String, Any>(
                "title" to announcement.title,
                "message" to announcement.message,
                "category" to announcement.category,
                "senderEmail" to announcement.senderEmail,
                "timestamp" to announcement.timestamp,
                "priority" to announcement.priority,
                "apkDownloadUrl" to (announcement.apkDownloadUrl ?: ""),
                "versionCode" to (announcement.versionCode ?: ""),
                "scheduledAppearTime" to (announcement.scheduledAppearTime ?: 0L),
                "scheduledDisappearTime" to (announcement.scheduledDisappearTime ?: 0L)
            )
            if (announcement.category == "HOT_FIX") {
                data["hot_fix_prompt"] = com.example.ocr.OcrConfigManager.getSystemPrompt()
                data["hot_fix_version"] = com.example.ocr.OcrConfigManager.getCurrentVersion()
            }
            firestore.collection(COLLECTION_ANNOUNCEMENTS)
                .document(announcement.timestamp.toString())
                .set(data, SetOptions.merge())
                .await()
            Log.d(TAG, "Announcement broadcasted via Firestore SDK: ${announcement.title}")
            broadcastSuccess = true
        } catch (e: Exception) {
            Log.w(TAG, "Failed to broadcast via Firestore SDK: ${e.message}. Attempting REST fallback...")
        }

        // Attempt 2: Direct REST Sync
        try {
            val client = OkHttpClient.Builder()
                .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .build()

            val docId = announcement.timestamp.toString()
            val jsonPayload = JSONObject().apply {
                put("fields", JSONObject().apply {
                    put("title", JSONObject().put("stringValue", announcement.title))
                    put("message", JSONObject().put("stringValue", announcement.message))
                    put("category", JSONObject().put("stringValue", announcement.category))
                    put("senderEmail", JSONObject().put("stringValue", announcement.senderEmail))
                    put("timestamp", JSONObject().put("integerValue", announcement.timestamp.toString()))
                    put("priority", JSONObject().put("stringValue", announcement.priority))
                    put("apkDownloadUrl", JSONObject().put("stringValue", announcement.apkDownloadUrl ?: ""))
                    put("versionCode", JSONObject().put("stringValue", announcement.versionCode ?: ""))
                    put("scheduledAppearTime", JSONObject().put("integerValue", (announcement.scheduledAppearTime ?: 0L).toString()))
                    put("scheduledDisappearTime", JSONObject().put("integerValue", (announcement.scheduledDisappearTime ?: 0L).toString()))
                    if (announcement.category == "HOT_FIX") {
                        val prompt = com.example.ocr.OcrConfigManager.getSystemPrompt()
                        val ver = com.example.ocr.OcrConfigManager.getCurrentVersion()
                        put("hot_fix_prompt", JSONObject().put("stringValue", prompt))
                        put("hot_fix_version", JSONObject().put("integerValue", ver.toString()))
                    }
                })
            }

            val body = jsonPayload.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("https://firestore.googleapis.com/v1/projects/$PROJECT_ID/databases/(default)/documents/$COLLECTION_ANNOUNCEMENTS/$docId")
                .patch(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Log.d(TAG, "Announcement broadcasted via Firestore REST: ${announcement.title}")
                    broadcastSuccess = true
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Firestore REST announcement broadcast: ${e.message}")
        }

        return@withContext broadcastSuccess
    }

    /**
     * Deletes an announcement from Firebase Firestore (both SDK and REST).
     */
    suspend fun deleteAnnouncementFromFirebase(announcementId: Long, context: Context? = null): Boolean = withContext(Dispatchers.IO) {
        var deleted = false
        val docId = announcementId.toString()

        try {
            if (context != null && !isFirebaseInitialized) {
                init(context)
            }
            val firestore = FirebaseFirestore.getInstance()
            firestore.collection(COLLECTION_ANNOUNCEMENTS).document(docId).delete().await()
            
            // Also delete any document that matches timestamp
            val querySnapshot = firestore.collection(COLLECTION_ANNOUNCEMENTS)
                .whereEqualTo("timestamp", announcementId)
                .get()
                .await()
            for (doc in querySnapshot.documents) {
                doc.reference.delete().await()
            }

            // Track in deleted_announcements for cross-device sync
            firestore.collection("deleted_announcements")
                .document(docId)
                .set(mapOf("deletedAt" to System.currentTimeMillis(), "announcementId" to announcementId))

            Log.d(TAG, "Deleted announcement $docId from Firestore SDK")
            deleted = true
        } catch (e: Exception) {
            Log.w(TAG, "Firestore SDK delete failed: ${e.message}")
        }

        try {
            val client = OkHttpClient.Builder()
                .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .build()

            val request = Request.Builder()
                .url("https://firestore.googleapis.com/v1/projects/$PROJECT_ID/databases/(default)/documents/$COLLECTION_ANNOUNCEMENTS/$docId")
                .delete()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Log.d(TAG, "Deleted announcement $docId via REST")
                    deleted = true
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Firestore REST delete error: ${e.message}")
        }

        return@withContext deleted
    }

    /**
     * Clears all announcements from Firebase Firestore except the official Welcome notice.
     */
    suspend fun clearAllCloudAnnouncementsExceptWelcome(context: Context? = null): Boolean = withContext(Dispatchers.IO) {
        var success = true
        try {
            if (context != null && !isFirebaseInitialized) {
                init(context)
            }
            val cloudList = fetchAnnouncementsFromFirebase(context)
            for (item in cloudList) {
                if (item.title != "Welcome to EnterFirst Finance & UPI") {
                    deleteAnnouncementFromFirebase(item.id, context)
                }
            }
            // Update global sync meta
            try {
                val firestore = FirebaseFirestore.getInstance()
                firestore.collection("system_config")
                    .document("announcements_sync")
                    .set(mapOf("lastClearedTime" to System.currentTimeMillis()), SetOptions.merge())
            } catch (e: Exception) {
                // Ignore
            }
        } catch (e: Exception) {
            Log.w(TAG, "clearAllCloudAnnouncementsExceptWelcome error: ${e.message}")
            success = false
        }
        return@withContext success
    }

    /**
     * Starts listening to the Firestore 'announcements' collection in real-time.
     * Whenever the admin sends, deletes, or clears updates, this listener is immediately triggered on all active devices.
     */
    fun startRealtimeAnnouncementListener(
        context: Context,
        onAnnouncementReceived: (AppAnnouncement) -> Unit,
        onAnnouncementRemoved: ((Long) -> Unit)? = null,
        onActiveCloudIdsSynced: ((Set<Long>) -> Unit)? = null
    ) {
        try {
            if (!isFirebaseInitialized) {
                init(context)
            }
            announcementListener?.remove()

            val firestore = FirebaseFirestore.getInstance()
            announcementListener = firestore.collection(COLLECTION_ANNOUNCEMENTS)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Realtime announcement listener error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val activeIds = mutableSetOf<Long>()
                        for (doc in snapshot.documents) {
                            val id = doc.getLong("timestamp") ?: doc.id.toLongOrNull()
                            if (id != null) activeIds.add(id)
                        }
                        if (activeIds.isNotEmpty() || snapshot.documents.isEmpty()) {
                            onActiveCloudIdsSynced?.invoke(activeIds)
                        }

                        for (change in snapshot.documentChanges) {
                            if (change.type == com.google.firebase.firestore.DocumentChange.Type.ADDED ||
                                change.type == com.google.firebase.firestore.DocumentChange.Type.MODIFIED
                            ) {
                                val doc = change.document
                                val title = doc.getString("title") ?: continue
                                val message = doc.getString("message") ?: continue
                                val category = doc.getString("category") ?: "UPDATE"
                                val sender = doc.getString("senderEmail") ?: "Admin"
                                val timestamp = doc.getLong("timestamp") ?: doc.id.toLongOrNull() ?: System.currentTimeMillis()
                                val priority = doc.getString("priority") ?: "NORMAL"
                                val apkUrl = doc.getString("apkDownloadUrl")
                                val version = doc.getString("versionCode")
                                val appearTime = doc.getLong("scheduledAppearTime")
                                val disappearTime = doc.getLong("scheduledDisappearTime")

                                // Check for attached HOT_FIX prompt payload
                                val hotFixPrompt = doc.getString("hot_fix_prompt")
                                if (category == "HOT_FIX" && !hotFixPrompt.isNullOrBlank()) {
                                    val hotFixVer = doc.getLong("hot_fix_version")?.toInt() ?: 1
                                    val isNew = com.example.ocr.OcrConfigManager.setPendingHotFix(context, hotFixPrompt, hotFixVer)
                                    val prefs = context.getSharedPreferences("enterfirst_prefs", Context.MODE_PRIVATE)
                                    val currentEmail = prefs.getString("google_email", null)
                                    val isSender = currentEmail != null && currentEmail.equals(sender, true)
                                    if (isNew && !isSender) {
                                        com.example.util.AppNotificationHelper.showOcrBugFixNotification(context, hotFixPrompt, hotFixVer)
                                    }
                                }

                                val announcement = AppAnnouncement(
                                    id = timestamp,
                                    title = title,
                                    message = message,
                                    category = category,
                                    senderEmail = sender,
                                    timestamp = timestamp,
                                    priority = priority,
                                    apkDownloadUrl = if (apkUrl.isNullOrBlank()) null else apkUrl,
                                    versionCode = if (version.isNullOrBlank()) null else version,
                                    scheduledAppearTime = if (appearTime != null && appearTime > 0) appearTime else null,
                                    scheduledDisappearTime = if (disappearTime != null && disappearTime > 0) disappearTime else null
                                )
                                onAnnouncementReceived(announcement)
                            } else if (change.type == com.google.firebase.firestore.DocumentChange.Type.REMOVED) {
                                val doc = change.document
                                val timestamp = doc.getLong("timestamp") ?: doc.id.toLongOrNull() ?: 0L
                                if (timestamp > 0) {
                                    Log.d(TAG, "Realtime listener detected removed announcement: $timestamp")
                                    onAnnouncementRemoved?.invoke(timestamp)
                                }
                            }
                        }
                    }
                }
            Log.d(TAG, "Realtime Firestore incremental announcement listener started with deletion sync")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to start realtime announcement listener: ${e.message}")
        }
    }

    /**
     * Fetches announcements from Firebase Firestore via SDK and REST fallback.
     */
    suspend fun fetchAnnouncementsFromFirebase(context: Context? = null): List<AppAnnouncement> = withContext(Dispatchers.IO) {
        val announcements = mutableListOf<AppAnnouncement>()

        // Attempt 1: Firestore SDK
        try {
            if (context != null && !isFirebaseInitialized) {
                init(context)
            }
            val firestore = FirebaseFirestore.getInstance()
            val snapshot = firestore.collection(COLLECTION_ANNOUNCEMENTS).get().await()
            for (doc in snapshot.documents) {
                val title = doc.getString("title") ?: continue
                val message = doc.getString("message") ?: continue
                val category = doc.getString("category") ?: "UPDATE"
                val sender = doc.getString("senderEmail") ?: "Admin"
                val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                val priority = doc.getString("priority") ?: "NORMAL"
                val apkUrl = doc.getString("apkDownloadUrl")
                val version = doc.getString("versionCode")
                val appearTime = doc.getLong("scheduledAppearTime")
                val disappearTime = doc.getLong("scheduledDisappearTime")

                announcements.add(
                    AppAnnouncement(
                        id = timestamp,
                        title = title,
                        message = message,
                        category = category,
                        senderEmail = sender,
                        timestamp = timestamp,
                        priority = priority,
                        apkDownloadUrl = if (apkUrl.isNullOrBlank()) null else apkUrl,
                        versionCode = if (version.isNullOrBlank()) null else version,
                        scheduledAppearTime = if (appearTime != null && appearTime > 0) appearTime else null,
                        scheduledDisappearTime = if (disappearTime != null && disappearTime > 0) disappearTime else null
                    )
                )
            }
            if (announcements.isNotEmpty()) {
                return@withContext announcements
            }
        } catch (e: Exception) {
            Log.w(TAG, "Firestore SDK fetch announcements error: ${e.message}")
        }

        // Attempt 2: REST fallback
        try {
            val client = OkHttpClient.Builder()
                .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .build()

            val request = Request.Builder()
                .url("https://firestore.googleapis.com/v1/projects/$PROJECT_ID/databases/(default)/documents/$COLLECTION_ANNOUNCEMENTS")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    val json = JSONObject(bodyStr)
                    val docs = json.optJSONArray("documents")
                    if (docs != null) {
                        for (i in 0 until docs.length()) {
                            val docObj = docs.getJSONObject(i)
                            val fields = docObj.optJSONObject("fields") ?: continue
                            val title = fields.optJSONObject("title")?.optString("stringValue") ?: continue
                            val message = fields.optJSONObject("message")?.optString("stringValue") ?: continue
                            val category = fields.optJSONObject("category")?.optString("stringValue") ?: "UPDATE"
                            val sender = fields.optJSONObject("senderEmail")?.optString("stringValue") ?: "Admin"
                            val timestampStr = fields.optJSONObject("timestamp")?.optString("integerValue")
                            val timestamp = timestampStr?.toLongOrNull() ?: System.currentTimeMillis()
                            val priority = fields.optJSONObject("priority")?.optString("stringValue") ?: "NORMAL"
                            val apkUrl = fields.optJSONObject("apkDownloadUrl")?.optString("stringValue")
                            val version = fields.optJSONObject("versionCode")?.optString("stringValue")
                            val appearStr = fields.optJSONObject("scheduledAppearTime")?.optString("integerValue")
                            val disappearStr = fields.optJSONObject("scheduledDisappearTime")?.optString("integerValue")

                            announcements.add(
                                AppAnnouncement(
                                    id = timestamp,
                                    title = title,
                                    message = message,
                                    category = category,
                                    senderEmail = sender,
                                    timestamp = timestamp,
                                    priority = priority,
                                    apkDownloadUrl = if (apkUrl.isNullOrBlank()) null else apkUrl,
                                    versionCode = if (version.isNullOrBlank()) null else version,
                                    scheduledAppearTime = appearStr?.toLongOrNull()?.takeIf { it > 0 },
                                    scheduledDisappearTime = disappearStr?.toLongOrNull()?.takeIf { it > 0 }
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Firestore REST fetch announcements error: ${e.message}")
        }

        return@withContext announcements
    }
}
