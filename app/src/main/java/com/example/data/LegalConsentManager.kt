package com.example.data

import android.content.Context
import android.os.Build
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.Locale
import java.util.UUID

/**
 * Manages legally compliant "Terms and Conditions" and "Privacy Policy" acceptance flows.
 * Stores audit proofs in Cloud Firestore collection 'legal_consent_proofs'.
 */
object LegalConsentManager {
    private const val TAG = "LegalConsentManager"
    private const val COLLECTION_LEGAL_CONSENT = "legal_consent_proofs"
    private const val PROJECT_ID = "enterfirst-d93c1"
    const val TERMS_VERSION = "v2026.09.26"
    const val TERMS_URL_SNAPSHOT = "https://enterfirst.app/terms-and-conditions-v2"
    const val PRIVACY_URL_SNAPSHOT = "https://enterfirst.app/privacy-policy-v2"

    private const val PREFS_NAME = "legal_consent_prefs"
    private const val KEY_UID = "consent_uid"
    private const val KEY_CONSENT_ACCEPTED = "consent_accepted"
    private const val KEY_CONSENT_TIMESTAMP = "consent_timestamp"

    /**
     * Gets or creates a persistent unique UID for this installation/user.
     */
    fun getOrCreateUid(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var uid = prefs.getString(KEY_UID, null)
        if (uid.isNullOrBlank()) {
            uid = "user_" + UUID.randomUUID().toString().replace("-", "").take(16)
            prefs.edit().putString(KEY_UID, uid).apply()
        }
        return uid
    }

    /**
     * Checks if terms and conditions have been previously accepted locally.
     */
    fun isTermsAccepted(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_CONSENT_ACCEPTED, false)
    }

    /**
     * Records the user's explicit legal consent in Cloud Firestore and local preferences.
     *
     * JSON METADATA PAYLOAD:
     * - uid: The authenticated Firebase User ID / persistent UID.
     * - email: The user's email address.
     * - action: "terms_and_conditions_accepted"
     * - document_details: { version, url_snapshot }
     * - device_metadata: { platform, model, manufacturer, os_version, sdk_int, app_version }
     * - timestamp: FieldValue.serverTimestamp()
     *
     * Written to Firestore collection: 'legal_consent_proofs', document ID = uid.
     */
    suspend fun recordConsentProof(
        context: Context,
        userEmail: String? = null,
        authenticatedUid: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val uid = authenticatedUid ?: getOrCreateUid(context)
        val email = if (!userEmail.isNullOrBlank()) userEmail.trim() else "unlinked@enterfirst.local"

        // Mark local storage immediately
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_CONSENT_ACCEPTED, true)
            .putLong(KEY_CONSENT_TIMESTAMP, System.currentTimeMillis())
            .apply()

        // 1. Write via Firebase Firestore SDK
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            val firestore = FirebaseFirestore.getInstance()

            val documentDetails = hashMapOf<String, Any>(
                "version" to TERMS_VERSION,
                "url_snapshot" to TERMS_URL_SNAPSHOT,
                "privacy_url_snapshot" to PRIVACY_URL_SNAPSHOT,
                "summary_title" to "EnterFirst — Terms & Conditions Summary"
            )

            val deviceMetadata = hashMapOf<String, Any>(
                "platform" to "Android",
                "manufacturer" to Build.MANUFACTURER,
                "model" to Build.MODEL,
                "os_version" to Build.VERSION.RELEASE,
                "sdk_int" to Build.VERSION.SDK_INT,
                "app_version" to "2.0.0",
                "locale" to Locale.getDefault().toString()
            )

            val payload = hashMapOf<String, Any>(
                "uid" to uid,
                "email" to email,
                "action" to "terms_and_conditions_accepted",
                "document_details" to documentDetails,
                "device_metadata" to deviceMetadata,
                "timestamp" to FieldValue.serverTimestamp()
            )

            firestore.collection(COLLECTION_LEGAL_CONSENT)
                .document(uid)
                .set(payload, SetOptions.merge())
                .await()

            Log.d(TAG, "Legal consent proof recorded in Firestore for UID: $uid")
            return@withContext true
        } catch (e: Exception) {
            Log.w(TAG, "Firestore SDK legal consent error: ${e.message}. Trying REST fallback...")
        }

        // 2. REST Fallback
        try {
            val client = OkHttpClient.Builder()
                .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .build()

            val fields = JSONObject().apply {
                put("uid", JSONObject().put("stringValue", uid))
                put("email", JSONObject().put("stringValue", email))
                put("action", JSONObject().put("stringValue", "terms_and_conditions_accepted"))
                put("document_details", JSONObject().put("mapValue", JSONObject().put("fields", JSONObject().apply {
                    put("version", JSONObject().put("stringValue", TERMS_VERSION))
                    put("url_snapshot", JSONObject().put("stringValue", TERMS_URL_SNAPSHOT))
                })))
                put("device_metadata", JSONObject().put("mapValue", JSONObject().put("fields", JSONObject().apply {
                    put("platform", JSONObject().put("stringValue", "Android"))
                    put("manufacturer", JSONObject().put("stringValue", Build.MANUFACTURER))
                    put("model", JSONObject().put("stringValue", Build.MODEL))
                    put("os_version", JSONObject().put("stringValue", Build.VERSION.RELEASE))
                    put("app_version", JSONObject().put("stringValue", "2.0.0"))
                })))
                // ISO timestamp for REST
                put("timestamp", JSONObject().put("timestampValue", java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                }.format(java.util.Date())))
            }

            val bodyJson = JSONObject().put("fields", fields)
            val body = bodyJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("https://firestore.googleapis.com/v1/projects/$PROJECT_ID/databases/(default)/documents/$COLLECTION_LEGAL_CONSENT/$uid")
                .patch(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Log.d(TAG, "Legal consent proof recorded via Firestore REST for UID: $uid")
                    return@withContext true
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Firestore REST legal consent error: ${e.message}")
        }

        return@withContext false
    }
}
