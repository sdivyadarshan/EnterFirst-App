package com.example.ocr

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.io.File

/**
 * Manages the internal configuration framework for Gemini OCR System Prompt variables.
 * - Stores active prompt persistently in `ocr_config.json` in internal storage.
 * - Auto-initializes on app boot with the default Financial OCR System Prompt if empty or missing.
 * - Supports live hot-fixing via FCM payloads or Admin broadcast without requiring app restart.
 * - Emits hotFixTriggerEvent to trigger the full-screen dynamic UI optimization overlay.
 */
object OcrConfigManager {
    private const val TAG = "OcrConfigManager"
    const val CONFIG_FILE_NAME = "ocr_config.json"

    val DEFAULT_OCR_SYSTEM_PROMPT = """
You are the structured data extraction layer of a financial OCR pipeline. The incoming text has been pre-processed using a Spatial Row Reconstruction algorithm that groups bounding boxes into continuous horizontal rows and orders text left-to-right.

Your sole objective is to output a clean JSON object containing the finalized payment details by executing the following protocols:

1. RECEIPT TOTAL EXTRACTION PROTOCOL:
- Scan the reconstructed text rows from the bottom upward. 
- Look for structural row keywords: "Total Payment", "Total", "Grand Total", "Amount Paid", "Net Payable", "Amount Due", or "Balance".
- Extract the exact numeric value printed on that specific row or directly below it.
- FALLBACK: If no explicit structural keyword is found, isolate and extract the absolute bottom-most boundary number on the receipt. Never default to top-row line items.

2. ABSOLUTE MATHEMATICAL GUARDRAIL:
- DO NOT perform any math, addition, or manual calculations. 
- Extract only the literal, pre-printed total value identified by the bottom-up protocol.

3. MERCHANT DETECTION PROTOCOL:
- Identify the merchant or shop name from the top rows.
- Skip generic structural headers or transaction types (e.g., completely ignore "CASH RECEIPT", "TAX INVOICE", or "WELCOME").
- Capture the literal structural brand or business identity name.

OUTPUT FORMAT:
Return exclusively a valid JSON object matching the requested schema. Do not wrap the output in markdown fences (```json). If a field is completely missing, set it to null.
    """.trimIndent()

    data class HotFixEvent(
        val prompt: String,
        val version: Int = 1,
        val timestamp: Long = System.currentTimeMillis()
    )

    data class PendingHotFix(
        val prompt: String,
        val version: Int = 1,
        val timestamp: Long = System.currentTimeMillis()
    )

    private const val PREFS_HOT_FIX = "enterfirst_ocr_hot_fix_prefs"
    private const val KEY_PENDING_PROMPT = "pending_prompt"
    private const val KEY_PENDING_VERSION = "pending_version"

    private val _currentSystemPrompt = MutableStateFlow(DEFAULT_OCR_SYSTEM_PROMPT)
    val currentSystemPrompt: StateFlow<String> = _currentSystemPrompt.asStateFlow()

    private val _hotFixTriggerEvent = MutableStateFlow<HotFixEvent?>(null)
    val hotFixTriggerEvent: StateFlow<HotFixEvent?> = _hotFixTriggerEvent.asStateFlow()

    private val _pendingHotFix = MutableStateFlow<PendingHotFix?>(null)
    val pendingHotFix: StateFlow<PendingHotFix?> = _pendingHotFix.asStateFlow()

    private var currentVersion: Int = 1
    private var isInitialized = false

    /**
     * Initializes the OCR configuration on app boot.
     * Reads system instructions from `ocr_config.json`. If missing or empty, initializes with DEFAULT_OCR_SYSTEM_PROMPT.
     */
    @Synchronized
    fun init(context: Context) {
        val appContext = context.applicationContext
        try {
            val configFile = File(appContext.filesDir, CONFIG_FILE_NAME)
            if (!configFile.exists() || configFile.length() == 0L) {
                Log.d(TAG, "ocr_config.json not found or empty. Initializing with default OCR System Prompt...")
                writeConfigToFile(configFile, DEFAULT_OCR_SYSTEM_PROMPT, version = 1)
                _currentSystemPrompt.value = DEFAULT_OCR_SYSTEM_PROMPT
                currentVersion = 1
            } else {
                val content = configFile.readText(Charsets.UTF_8).trim()
                if (content.isEmpty()) {
                    writeConfigToFile(configFile, DEFAULT_OCR_SYSTEM_PROMPT, version = 1)
                    _currentSystemPrompt.value = DEFAULT_OCR_SYSTEM_PROMPT
                    currentVersion = 1
                } else {
                    val (prompt, ver) = parseConfigFileContent(content)
                    if (prompt.isNotBlank()) {
                        _currentSystemPrompt.value = prompt
                        currentVersion = ver
                        Log.d(TAG, "Successfully loaded system instructions from ocr_config.json (v$ver, length=${prompt.length})")
                    } else {
                        writeConfigToFile(configFile, DEFAULT_OCR_SYSTEM_PROMPT, version = 1)
                        _currentSystemPrompt.value = DEFAULT_OCR_SYSTEM_PROMPT
                        currentVersion = 1
                    }
                }
            }

            // Check if there is an unapplied pending hot-fix saved in preferences
            val prefs = appContext.getSharedPreferences(PREFS_HOT_FIX, Context.MODE_PRIVATE)
            val pendingStr = prefs.getString(KEY_PENDING_PROMPT, null)
            val pendingVer = prefs.getInt(KEY_PENDING_VERSION, 1)
            if (!pendingStr.isNullOrBlank() && pendingStr.trim() != _currentSystemPrompt.value.trim()) {
                _pendingHotFix.value = PendingHotFix(pendingStr, pendingVer)
                Log.d(TAG, "Loaded unapplied pending hot-fix (v$pendingVer) from prefs")
            } else {
                _pendingHotFix.value = null
            }

            isInitialized = true
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing ocr_config.json: ${e.message}", e)
            _currentSystemPrompt.value = DEFAULT_OCR_SYSTEM_PROMPT
        }
    }

    /**
     * Registers a pending OCR bug fix payload received via FCM or cloud sync.
     * Does NOT auto-patch or trigger animation yet — waits until the user taps 'Fix Now'.
     * Returns true if newly queued as pending, false if already applied.
     */
    @Synchronized
    fun setPendingHotFix(context: Context, newPrompt: String, version: Int): Boolean {
        val trimmed = newPrompt.trim()
        if (trimmed.isBlank() || trimmed == _currentSystemPrompt.value.trim()) {
            return false
        }
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS_HOT_FIX, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_PENDING_PROMPT, trimmed)
            .putInt(KEY_PENDING_VERSION, version)
            .apply()

        _pendingHotFix.value = PendingHotFix(trimmed, version)
        Log.i(TAG, "Set pending OCR hot-fix v$version (user must click 'Fix Now' to apply)")
        return true
    }

    /**
     * Called when the user clicks 'Fix Now' (from notification, in-app banner, or update sheet).
     * Simultaneously overwrites ocr_config.json, updates in-memory prompt, cancels notification,
     * and runs the full-screen dynamic UI test animation for 3 seconds.
     */
    @Synchronized
    fun applyPendingHotFix(context: Context): Boolean {
        val pending = _pendingHotFix.value
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS_HOT_FIX, Context.MODE_PRIVATE)
        val prompt = pending?.prompt ?: prefs.getString(KEY_PENDING_PROMPT, null)
        val ver = pending?.version ?: prefs.getInt(KEY_PENDING_VERSION, currentVersion + 1)

        if (prompt.isNullOrBlank()) {
            return false
        }

        // Overwrite ocr_config.json and trigger 3-second test UI
        val applied = applyHotFix(appContext, prompt, ver)
        if (applied) {
            prefs.edit().clear().apply()
            _pendingHotFix.value = null
            com.example.util.AppNotificationHelper.cancelOcrBugFixNotification(appContext)
        }
        return applied
    }

    /**
     * Dismisses the pending hot-fix without applying.
     */
    fun dismissPendingHotFix(context: Context) {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS_HOT_FIX, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        _pendingHotFix.value = null
        com.example.util.AppNotificationHelper.cancelOcrBugFixNotification(appContext)
    }

    /**
     * Retrieves the active Gemini OCR system prompt.
     */
    fun getSystemPrompt(context: Context? = null): String {
        if (!isInitialized && context != null) {
            init(context)
        }
        val prompt = _currentSystemPrompt.value
        return if (prompt.isNotBlank()) prompt else DEFAULT_OCR_SYSTEM_PROMPT
    }

    /**
     * Intercepts and patches new system instructions from FCM payload or admin broadcast.
     * Overwrites `ocr_config.json` with the new text string and applies the update instantly in memory.
     * Emits HotFixEvent to trigger the full-screen dynamic UI optimization animation for 3 seconds.
     */
    @Synchronized
    fun applyHotFix(context: Context, newPrompt: String, version: Int? = null): Boolean {
        val trimmed = newPrompt.trim()
        if (trimmed.isBlank()) {
            Log.w(TAG, "Cannot apply empty hot_fix_prompt")
            return false
        }

        val appContext = context.applicationContext
        try {
            val configFile = File(appContext.filesDir, CONFIG_FILE_NAME)
            val nextVersion = version ?: (currentVersion + 1)
            writeConfigToFile(configFile, trimmed, nextVersion)

            // Instantly apply in memory without hard app restart
            _currentSystemPrompt.value = trimmed
            currentVersion = nextVersion
            isInitialized = true

            Log.i(TAG, "HOT-FIX APPLIED: ocr_config.json updated to v$nextVersion! New prompt length: ${trimmed.length}")

            // Trigger the UI/UX animation overlay
            _hotFixTriggerEvent.value = HotFixEvent(
                prompt = trimmed,
                version = nextVersion,
                timestamp = System.currentTimeMillis()
            )
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply hot_fix_prompt: ${e.message}", e)
            return false
        }
    }

    /**
     * Allows manual or testing trigger of the 3-second optimization UI overlay.
     */
    fun triggerHotFixAnimation(testPrompt: String? = null) {
        val prompt = testPrompt ?: _currentSystemPrompt.value
        _hotFixTriggerEvent.value = HotFixEvent(
            prompt = prompt,
            version = currentVersion,
            timestamp = System.currentTimeMillis()
        )
    }

    /**
     * Clears the hot fix trigger event after the UI overlay completes its 3-second animation.
     */
    fun clearHotFixEvent() {
        _hotFixTriggerEvent.value = null
    }

    /**
     * Reverts to default system instructions.
     */
    fun resetToDefault(context: Context): Boolean {
        return applyHotFix(context, DEFAULT_OCR_SYSTEM_PROMPT, version = 1)
    }

    fun getCurrentVersion(): Int = currentVersion

    private fun writeConfigToFile(file: File, prompt: String, version: Int) {
        val json = JSONObject().apply {
            put("system_prompt", prompt)
            put("version", version)
            put("last_updated", System.currentTimeMillis())
            put("algorithm", "Spatial Row Reconstruction + Bottom-Up Total Extraction")
        }
        file.writeText(json.toString(2), Charsets.UTF_8)
    }

    private fun parseConfigFileContent(content: String): Pair<String, Int> {
        return try {
            if (content.startsWith("{")) {
                val json = JSONObject(content)
                val prompt = json.optString("system_prompt")
                    .takeIf { it.isNotBlank() }
                    ?: json.optString("hot_fix_prompt")
                    .takeIf { it.isNotBlank() }
                    ?: json.optString("system_instructions", "")
                val ver = json.optInt("version", 1)
                Pair(prompt, ver)
            } else {
                // Raw text prompt saved directly
                Pair(content, 1)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse JSON, treating as raw string prompt: ${e.message}")
            Pair(content, 1)
        }
    }
}
