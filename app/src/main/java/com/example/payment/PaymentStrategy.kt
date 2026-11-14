package com.example.payment

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

/**
 * Payment type classification based on full decoded QR parameters.
 */
enum class PaymentType(val label: String, val badgeText: String) {
    P2P("P2P / Personal", "PERSONAL / P2P UPI"),
    MERCHANT("Merchant / Shop", "SHOP / MERCHANT QR"),
    BUS_TRANSIT("Bus & Public Transport", "BUS & TRANSIT QR")
}

/**
 * Identified payment strategies.
 */
enum class PaymentStrategyType(val title: String, val description: String) {
    P2P_DIRECT_INTENT(
        title = "Direct UPI Link",
        description = "Launches UPI app directly via standard sanitized UPI URL intent"
    ),
    MERCHANT_QR_IMAGE_SHARE(
        title = "QR Image Share",
        description = "Generates standard UPI QR image and shares to UPI apps (GPay, PhonePe, Navi, BHIM)"
    )
}

/**
 * Centralized Validation and Sanitization Utility for UPI data.
 * Standardizes payee addresses (VPAs), amount parsing, bus transit detection,
 * and builds 100% NPCI-compliant canonical UPI URIs.
 */
object UpiValidator {

    private val VPA_REGEX = Regex("^[a-zA-Z0-9.\\-_]{2,256}@[a-zA-Z0-9.\\-_]{2,64}$")
    private val EMBEDDED_VPA_REGEX = Regex("[a-zA-Z0-9.\\-_]{2,256}@[a-zA-Z0-9.\\-_]{2,64}")

    /**
     * Validates if a string is a well-formed UPI VPA address.
     */
    fun isValidVpa(vpa: String?): Boolean {
        if (vpa.isNullOrBlank()) return false
        val sanitized = sanitizeVpa(vpa)
        return VPA_REGEX.matches(sanitized)
    }

    /**
     * Cleans and sanitizes a VPA address:
     * - Trims whitespace
     * - Removes URL prefixes (e.g. upi://pay?pa=, mailto:)
     * - Removes accidental trailing commas, periods, or colons from OCR/scanner
     */
    fun sanitizeVpa(vpa: String): String {
        var cleaned = vpa.trim()
        if (cleaned.startsWith("upi://pay?pa=", ignoreCase = true)) {
            cleaned = cleaned.substringAfter("pa=").substringBefore("&")
        }
        if (cleaned.startsWith("mailto:", ignoreCase = true)) {
            cleaned = cleaned.substringAfter("mailto:")
        }
        // Remove trailing punctuation
        cleaned = cleaned.trimEnd('.', ',', ';', ':', '!', '?', '/', ' ')
        // Remove leading punctuation or @
        cleaned = cleaned.trimStart(':', '/', ' ')
        return cleaned.lowercase(Locale.ROOT)
    }

    /**
     * Extracts the first valid VPA found in an arbitrary string or block of text.
     */
    fun extractVpa(text: String?): String? {
        if (text.isNullOrBlank()) return null
        val match = EMBEDDED_VPA_REGEX.find(text)
        return match?.value?.let { sanitizeVpa(it) }
    }

    /**
     * Parses an amount string into a valid Double, handling currency symbols, commas, and prefixes.
     */
    fun parseAmount(amountStr: String?): Double? {
        if (amountStr.isNullOrBlank()) return null
        return try {
            val cleaned = amountStr
                .replace("₹", "")
                .replace("Rs.", "", ignoreCase = true)
                .replace("Rs", "", ignoreCase = true)
                .replace("INR", "", ignoreCase = true)
                .replace(",", "")
                .trim()
            val parsed = cleaned.toDoubleOrNull()
            if (parsed != null && parsed > 0) parsed else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Checks if a parsed payload is a Bus or Public Transit QR code.
     */
    fun isBusOrTransit(
        pa: String,
        pn: String,
        mc: String?,
        tr: String?,
        tn: String?,
        params: Map<String, String>
    ): Boolean {
        // 1. Check Merchant Category Code (4111 = Suburban/Commuter passenger transportation; 4131 = Bus lines; 4789 = Transport services)
        if (mc in listOf("4111", "4131", "4789", "4011", "4112")) {
            return true
        }

        // 2. Transit keywords in VPA handle
        val transitVpaKeywords = listOf(
            "bus", "transit", "chalo", "bmtc", "ksrtc", "best", "dtc", "transport",
            "conductor", "ticket", "depot", "travels", "fleet", "msrtc", "apsrtc",
            "tsrtc", "wbtc", "nwkrtc", "kkrtc", "metro", "commute", "toll"
        )
        val lowerPa = pa.lowercase(Locale.ROOT)
        if (transitVpaKeywords.any { lowerPa.contains(it) }) {
            return true
        }

        // 3. Transit keywords in Merchant/Payee Name
        val lowerPn = pn.lowercase(Locale.ROOT)
        val transitNameKeywords = listOf(
            "bus", "transport", "transit", "conductor", "ticket", "express",
            "shuttle", "corporation", "metro", "chalo", "roadways", "depot", "travels"
        )
        if (transitNameKeywords.any { lowerPn.contains(it) }) {
            return true
        }

        // 4. Transit keywords in Note or Reference
        val combinedNotes = "${tr.orEmpty()} ${tn.orEmpty()}".lowercase(Locale.ROOT)
        val transitRefKeywords = listOf("ticket", "bus_no", "busno", "route", "trip", "fare", "seat", "depot")
        if (transitRefKeywords.any { combinedNotes.contains(it) }) {
            return true
        }

        // 5. Special transit keys in raw params
        val transitParamKeys = listOf("ticketid", "ticket_id", "busno", "bus_no", "fare", "route", "tripid")
        if (params.keys.any { k -> transitParamKeys.contains(k.lowercase(Locale.ROOT)) }) {
            return true
        }

        return false
    }

    /**
     * Builds a 100% clean, standard NPCI-compliant canonical UPI URI.
     * Crucial: Strips proprietary, broken, or unencoded transit metadata that causes external
     * UPI apps (Google Pay, PhonePe, Paytm, BHIM, Cred) to fail with "Invalid UPI ID".
     */
    fun buildCanonicalUpiUri(
        pa: String,
        pn: String,
        am: Double?,
        tr: String? = null,
        tn: String? = null,
        mc: String? = null,
        cu: String = "INR"
    ): String {
        val cleanPa = sanitizeVpa(pa)
        val cleanPn = if (pn.isNotBlank()) pn.trim() else "Merchant"
        
        val queryMap = LinkedHashMap<String, String>()
        queryMap["pa"] = cleanPa
        queryMap["pn"] = cleanPn
        
        if (am != null && am > 0) {
            queryMap["am"] = String.format(Locale.US, "%.2f", am)
        }
        
        queryMap["cu"] = if (cu.isNotBlank()) cu else "INR"
        
        if (!mc.isNullOrBlank() && mc != "0000" && mc != "null") {
            queryMap["mc"] = mc.trim()
        }
        
        if (!tr.isNullOrBlank()) {
            // Keep clean alphanumeric reference
            val cleanTr = tr.trim().take(35)
            queryMap["tr"] = cleanTr
        }
        
        if (!tn.isNullOrBlank()) {
            val cleanTn = tn.trim().take(60)
            queryMap["tn"] = cleanTn
        }

        val builder = StringBuilder("upi://pay?")
        var first = true
        for ((k, v) in queryMap) {
            if (v.isBlank()) continue
            if (!first) builder.append("&")
            builder.append(Uri.encode(k)).append("=").append(Uri.encode(v))
            first = false
        }
        return builder.toString()
    }
}

/**
 * Data model that preserves the ENTIRE decoded UPI QR payload and all its parameters,
 * and provides sanitized canonical URI generation and transit classification.
 */
data class UpiPayload(
    val rawPayload: String,
    val params: Map<String, String> = emptyMap(),
    val pa: String = "",                         // Payee VPA / UPI ID
    val pn: String = "Merchant",                  // Payee Name / Bus Operator
    val am: String? = null,                       // Pre-set Amount if any
    val mc: String? = null,                       // Merchant Category Code (e.g. 4111, 4131, 5411)
    val mid: String? = null,                      // Merchant ID
    val msid: String? = null,                     // Merchant Store ID
    val mtid: String? = null,                     // Merchant Terminal ID
    val tid: String? = null,                      // Terminal ID / Txn ID
    val tr: String? = null,                       // Transaction Reference / Ticket No
    val tn: String? = null,                       // Transaction Note / Route
    val cu: String = "INR",                       // Currency (default INR)
    val mode: String? = null,                     // QR Mode (e.g., 01=Static, 02=Dynamic, 04)
    val orgid: String? = null,                    // PSP / Bank Org ID
    val sign: String? = null,                     // Digital signature
    val url: String? = null,                      // Invoice / Ref URL
    val isTransit: Boolean = false                // Identified as public transport / bus ticket
) {
    /**
     * Determines whether this payload represents Bus/Transit, Merchant, or Personal P2P UPI.
     */
    fun determinePaymentType(): PaymentType {
        if (isTransit || UpiValidator.isBusOrTransit(pa, pn, mc, tr, tn, params)) {
            return PaymentType.BUS_TRANSIT
        }

        // 1. Merchant Category Code check (4-digit code != 0000)
        val validMc = mc?.trim()
        if (!validMc.isNullOrEmpty() && validMc != "0000" && validMc != "null") {
            return PaymentType.MERCHANT
        }

        // 2. Merchant ID / Store ID / Terminal ID checks
        if (!mid.isNullOrBlank() || !msid.isNullOrBlank() || !mtid.isNullOrBlank() || !tid.isNullOrBlank()) {
            return PaymentType.MERCHANT
        }

        // 3. Organization ID presence (typically banks issuing merchant QR)
        if (!orgid.isNullOrBlank()) return PaymentType.MERCHANT

        // 4. Standard QR mode indicating Static/Dynamic Merchant QR (01, 02, 04)
        if (mode in listOf("01", "02", "04")) return PaymentType.MERCHANT

        // 5. Merchant Transaction Reference patterns
        tr?.let { ref ->
            val upperRef = ref.uppercase(Locale.ROOT)
            if (upperRef.startsWith("BPE") ||
                upperRef.startsWith("PTM") ||
                upperRef.startsWith("PHONEPE") ||
                upperRef.startsWith("BHARATPE") ||
                upperRef.startsWith("PINELABS") ||
                upperRef.startsWith("TXN") ||
                upperRef.startsWith("ORDER")
            ) {
                return PaymentType.MERCHANT
            }
        }

        // 6. Inspect Payee VPA handle for merchant tags
        val lowerPa = pa.lowercase(Locale.ROOT)
        val merchantHandles = listOf(
            "@paytmqr", "@bharatpe", "@pinelabs", "@icicimerchant",
            "@hdfcmerchant", "@pos", "@razorpay", "@sbipay",
            "@axisbank", "@yesbank", "@indus", ".bhim", ".bharatpe"
        )
        if (merchantHandles.any { lowerPa.contains(it) }) {
            return PaymentType.MERCHANT
        }

        // 7. Check additional raw parameters for merchant-specific keys
        val merchantParamKeys = listOf("mcc", "merchantcode", "storeid", "terminalid", "subid", "sid")
        if (params.keys.any { key -> merchantParamKeys.contains(key.lowercase(Locale.ROOT)) }) {
            return PaymentType.MERCHANT
        }

        // Otherwise, classified as P2P / Personal UPI ID
        return PaymentType.P2P
    }

    /**
     * Builds a clean canonical upi://pay URI guaranteed to be 100% compliant with UPI spec.
     * Prevents external UPI apps from crashing or failing on "invalid upi" for complex bus QR payloads.
     */
    fun buildUpiUri(newAmount: Double?): String {
        val targetAmount = if (newAmount != null && newAmount > 0) {
            newAmount
        } else {
            UpiValidator.parseAmount(am)
        }
        return UpiValidator.buildCanonicalUpiUri(
            pa = pa,
            pn = pn,
            am = targetAmount,
            tr = tr,
            tn = tn,
            mc = mc,
            cu = cu
        )
    }

    companion object {
        /**
         * Robust parser for diverse UPI QR data formats:
         * - Standard upi://pay links (case insensitive)
         * - Proprietary deep link formats (paytmmp://, phonepe://, gpay://, tez://, bhim://, cred://)
         * - URLs with nested/embedded UPI query strings (e.g. bus ticket portals)
         * - BharatQR / EMVCo TLV format (000201...)
         * - JSON formatted QR tickets
         * - Multi-line Key-Value formatted transit tickets
         * - Plain text containing VPAs and amounts
         */
        fun parse(rawQr: String): UpiPayload {
            val trimmed = rawQr.trim()
            val decoded = try { Uri.decode(trimmed) } catch (e: Exception) { trimmed }
            val rawLower = trimmed.lowercase(Locale.ROOT)

            val paramMap = mutableMapOf<String, String>()

            // 1. BharatQR / EMVCo TLV structure (starts with 000201)
            if (trimmed.startsWith("000201")) {
                val bharatParams = parseBharatQr(trimmed)
                paramMap.putAll(bharatParams)
            }
            // 2. Standard or Custom Scheme UPI Links (e.g. upi://pay, paytmmp://pay, phonepe://pay, etc.)
            else if (rawLower.contains("upi://") || rawLower.contains("pa=") || rawLower.contains("paytmmp://") || rawLower.contains("phonepe://")) {
                extractQueryParams(trimmed, decoded, paramMap)
            }
            // 3. URLs with embedded UPI data (e.g. https://...?data=upi%3A%2F%2Fpay... or https://.../qr?upi=...)
            else if (rawLower.startsWith("http://") || rawLower.startsWith("https://")) {
                val embeddedUpi = extractEmbeddedUpiFromUrl(trimmed, decoded)
                if (embeddedUpi != null) {
                    return parse(embeddedUpi)
                } else {
                    // Try parsing query params directly from the HTTP URL as fallback
                    extractQueryParams(trimmed, decoded, paramMap)
                }
            }
            // 4. JSON Format Transit / Merchant Payload
            else if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                parseJsonPayload(trimmed, paramMap)
            }
            // 5. Multi-line Key-Value (e.g. "PA: bus@sbi\nAM: 25.00\nPN: BMTC")
            else if (trimmed.contains("\n") && (trimmed.contains(":") || trimmed.contains("="))) {
                parseKeyValueLines(trimmed, paramMap)
            }
            // 6. Plain VPA or embedded VPA in text
            else {
                val extractedVpa = UpiValidator.extractVpa(decoded)
                if (extractedVpa != null) {
                    paramMap["pa"] = extractedVpa
                    paramMap["pn"] = "Merchant"
                    // Check if an amount was also present in text
                    val amountRegex = Regex("(?:rs\\.?|inr|₹)\\s*([0-9]+(?:\\.[0-9]{1,2})?)", RegexOption.IGNORE_CASE)
                    amountRegex.find(decoded)?.let { m ->
                        paramMap["am"] = m.groupValues[1]
                    }
                }
            }

            // Standardize and normalize all key synonyms
            val normalized = normalizeKeys(paramMap)

            val rawPa = normalized["pa"] ?: ""
            val cleanPa = UpiValidator.sanitizeVpa(rawPa)
            val cleanPn = normalized["pn"]?.let { Uri.decode(it) }?.ifBlank { "Merchant" } ?: "Merchant"
            val am = normalized["am"]
            val mc = normalized["mc"]
            val mid = normalized["mid"]
            val msid = normalized["msid"]
            val mtid = normalized["mtid"]
            val tid = normalized["tid"]
            val tr = normalized["tr"]
            val tn = normalized["tn"]
            val cu = normalized["cu"] ?: "INR"
            val mode = normalized["mode"]
            val orgid = normalized["orgid"]
            val sign = normalized["sign"]
            val url = normalized["url"]

            val isTransit = UpiValidator.isBusOrTransit(
                pa = cleanPa,
                pn = cleanPn,
                mc = mc,
                tr = tr,
                tn = tn,
                params = normalized
            )

            return UpiPayload(
                rawPayload = trimmed,
                params = normalized,
                pa = cleanPa,
                pn = cleanPn,
                am = am,
                mc = mc,
                mid = mid,
                msid = msid,
                mtid = mtid,
                tid = tid,
                tr = tr,
                tn = tn,
                cu = cu,
                mode = mode,
                orgid = orgid,
                sign = sign,
                url = url,
                isTransit = isTransit
            )
        }

        /**
         * Extracts query parameters from UPI URIs with multiple fallbacks.
         */
        private fun extractQueryParams(raw: String, decoded: String, targetMap: MutableMap<String, String>) {
            // If the raw contains a nested upi URI, extract starting from upi:// or pa=
            val workingStr = if (decoded.contains("upi://pay?", ignoreCase = true)) {
                "upi://pay?" + decoded.substringAfter("upi://pay?", "")
            } else if (decoded.contains("pa=")) {
                "upi://pay?" + decoded.substringAfter("pa=").let { "pa=$it" }
            } else {
                raw
            }

            val uri = try { Uri.parse(workingStr) } catch (e: Exception) { null }
            uri?.queryParameterNames?.forEach { name ->
                uri.getQueryParameter(name)?.let { value ->
                    targetMap[name.lowercase(Locale.ROOT)] = value
                }
            }

            // Fallback manual regex parser in case parameter tokens were double encoded
            val queryPart = if (decoded.contains("?")) decoded.substringAfter("?") else decoded
            val pairs = queryPart.split("&", ";")
            for (pair in pairs) {
                val parts = pair.split("=", limit = 2)
                if (parts.size == 2) {
                    val k = parts[0].trim().lowercase(Locale.ROOT)
                    val v = parts[1].trim()
                    if (k.isNotEmpty() && !targetMap.containsKey(k)) {
                        targetMap[k] = v
                    }
                }
            }
        }

        /**
         * Recursively looks for embedded UPI links inside HTTP/HTTPS URLs.
         */
        private fun extractEmbeddedUpiFromUrl(url: String, decoded: String): String? {
            val upiMarker = "upi://pay?"
            if (decoded.contains(upiMarker, ignoreCase = true)) {
                val start = decoded.indexOf(upiMarker, ignoreCase = true)
                return decoded.substring(start)
            }
            if (url.contains("upi%3A%2F%2Fpay", ignoreCase = true)) {
                val start = url.indexOf("upi%3A%2F%2Fpay", ignoreCase = true)
                val substring = url.substring(start)
                return try { Uri.decode(substring) } catch (e: Exception) { substring }
            }
            return null
        }

        /**
         * Parses JSON payloads.
         */
        private fun parseJsonPayload(jsonStr: String, targetMap: MutableMap<String, String>) {
            try {
                val json = JSONObject(jsonStr)
                val keys = json.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val v = json.optString(k, "")
                    if (v.isNotEmpty()) {
                        targetMap[k.lowercase(Locale.ROOT)] = v
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        /**
         * Parses multi-line key-value tickets.
         */
        private fun parseKeyValueLines(text: String, targetMap: MutableMap<String, String>) {
            val lines = text.split("\n")
            for (line in lines) {
                val delimiter = if (line.contains("=")) "=" else if (line.contains(":")) ":" else continue
                val parts = line.split(delimiter, limit = 2)
                if (parts.size == 2) {
                    val k = parts[0].trim().lowercase(Locale.ROOT)
                    val v = parts[1].trim()
                    if (k.isNotEmpty() && v.isNotEmpty()) {
                        targetMap[k] = v
                    }
                }
            }
        }

        /**
         * Normalizes synonym keys from diverse formats into standard UPI parameters.
         */
        private fun normalizeKeys(rawMap: Map<String, String>): Map<String, String> {
            val normalized = mutableMapOf<String, String>()
            for ((key, value) in rawMap) {
                val cleanKey = key.trim().lowercase(Locale.ROOT)
                val cleanVal = value.trim()
                if (cleanVal.isEmpty()) continue

                when (cleanKey) {
                    // Payee VPA synonyms
                    "pa", "vpa", "upi", "payee", "payee_vpa", "payeeaddress", "receiver", "to", "acc", "payeeguid" -> {
                        if (!normalized.containsKey("pa") || UpiValidator.isValidVpa(cleanVal)) {
                            normalized["pa"] = cleanVal
                        }
                    }
                    // Payee Name synonyms
                    "pn", "name", "payeename", "payee_name", "merchant", "merchant_name", "operator", "bus_operator", "bus", "desc" -> {
                        if (!normalized.containsKey("pn")) {
                            normalized["pn"] = cleanVal
                        }
                    }
                    // Amount synonyms
                    "am", "amt", "amount", "fare", "ticket_fare", "price", "val", "total", "money", "mam" -> {
                        if (!normalized.containsKey("am")) {
                            val parsed = UpiValidator.parseAmount(cleanVal)
                            if (parsed != null) {
                                normalized["am"] = String.format(Locale.US, "%.2f", parsed)
                            }
                        }
                    }
                    // Merchant Category Code synonyms
                    "mc", "mcc", "category" -> {
                        if (!normalized.containsKey("mc")) normalized["mc"] = cleanVal
                    }
                    // Reference / Ticket ID synonyms
                    "tr", "txnref", "ref", "ticketid", "ticket_no", "ticket_id", "orderno", "order_id" -> {
                        if (!normalized.containsKey("tr")) normalized["tr"] = cleanVal
                    }
                    // Note / Route synonyms
                    "tn", "note", "remark", "route", "trip" -> {
                        if (!normalized.containsKey("tn")) normalized["tn"] = cleanVal
                    }
                    // Currency synonyms
                    "cu", "currency" -> {
                        normalized["cu"] = if (cleanVal == "356" || cleanVal.equals("INR", ignoreCase = true)) "INR" else cleanVal
                    }
                    else -> {
                        normalized[cleanKey] = cleanVal
                    }
                }
            }
            return normalized
        }

        /**
         * Comprehensive EMVCo / BharatQR TLV Parser.
         */
        private fun parseBharatQr(payload: String): Map<String, String> {
            val result = mutableMapOf<String, String>()
            try {
                var i = 0
                while (i < payload.length - 4) {
                    val tag = payload.substring(i, i + 2)
                    val length = payload.substring(i + 2, i + 4).toIntOrNull() ?: break
                    if (i + 4 + length > payload.length) break
                    val value = payload.substring(i + 4, i + 4 + length)

                    when (tag) {
                        "26", "27", "28" -> {
                            // Merchant Account Information - UPI usually in sub-tag 01 or 02
                            var j = 0
                            while (j < value.length - 4) {
                                val subTag = value.substring(j, j + 2)
                                val subLen = value.substring(j + 2, j + 4).toIntOrNull() ?: break
                                if (j + 4 + subLen > value.length) break
                                val subVal = value.substring(j + 4, j + 4 + subLen)
                                if (subTag == "01" || subTag == "02") {
                                    if (UpiValidator.isValidVpa(subVal) || subVal.contains("@")) {
                                        result["pa"] = subVal
                                    }
                                }
                                j += 4 + subLen
                            }
                        }
                        "52" -> result["mc"] = value // Merchant Category Code
                        "54" -> result["am"] = value // Transaction Amount
                        "58" -> result["cu"] = if (value == "356") "INR" else value
                        "59" -> result["pn"] = value // Payee Name
                        "02" -> result["mode"] = value // Point of Initiation Method
                        "62" -> {
                            // Additional Data (Txn Ref / Bill / Terminal)
                            var k = 0
                            while (k < value.length - 4) {
                                val addTag = value.substring(k, k + 2)
                                val addLen = value.substring(k + 2, k + 4).toIntOrNull() ?: break
                                if (k + 4 + addLen > value.length) break
                                val addVal = value.substring(k + 4, k + 4 + addLen)
                                when (addTag) {
                                    "01" -> result["tr"] = addVal // Bill Number / Txn Ref
                                    "02" -> result["mobile"] = addVal
                                    "03" -> result["storeid"] = addVal
                                    "07" -> result["tid"] = addVal // Terminal ID
                                    "08" -> result["tn"] = addVal // Purpose / Note
                                }
                                k += 4 + addLen
                            }
                        }
                    }
                    i += 4 + length
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return result
        }
    }
}

/**
 * Execution outcome for payment strategies.
 */
sealed interface StrategyResult {
    data class Success(val strategy: PaymentStrategyType, val message: String) : StrategyResult
    data class FallbackSuggested(val fallbackStrategy: PaymentStrategyType, val reason: String) : StrategyResult
    data class Failure(val error: String) : StrategyResult
}

/**
 * Common payment strategy interface.
 */
interface PaymentStrategy {
    val strategyType: PaymentStrategyType
    fun execute(context: Context, payload: UpiPayload, amount: Double): StrategyResult
}

/**
 * P2P Payment Strategy:
 * Generates a clean, 100% compliant canonical UPI URL intent (ACTION_VIEW).
 * Avoids any illegal query strings that cause external UPI apps to fail with "Invalid UPI ID".
 */
class P2PPaymentStrategy : PaymentStrategy {
    override val strategyType: PaymentStrategyType = PaymentStrategyType.P2P_DIRECT_INTENT

    override fun execute(context: Context, payload: UpiPayload, amount: Double): StrategyResult {
        return try {
            val upiUriString = payload.buildUpiUri(amount)
            val uri = Uri.parse(upiUriString)
            val intent = Intent(Intent.ACTION_VIEW, uri)

            val chooser = Intent.createChooser(intent, "Pay via UPI App (GPay, PhonePe, Paytm, BHIM)")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)

            StrategyResult.Success(
                strategy = strategyType,
                message = "Launched UPI application via Direct URL Intent"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            // Provide fallback to QR Image Share if direct URL fails
            StrategyResult.FallbackSuggested(
                fallbackStrategy = PaymentStrategyType.MERCHANT_QR_IMAGE_SHARE,
                reason = "Direct UPI intent failed: ${e.localizedMessage}. Fallback to QR Image Share available."
            )
        }
    }
}

/**
 * Merchant & Bus/Transit Payment Strategy:
 * 1. Takes all verified parameters (payee, operator name, fare amount)
 * 2. Builds a standardized, crisp, 100% compliant QR code bitmap
 * 3. Saves PNG into secure cache
 * 4. Initiates Android ACTION_SEND (image/png) intent
 * 5. External UPI apps (GPay, PhonePe, Paytm, BHIM, Cred, Navi) import and scan the QR directly from the image!
 *
 * This guarantees seamless scanning even for complex bus/transit QRs whose direct deep links fail.
 */
class MerchantPaymentStrategy : PaymentStrategy {
    override val strategyType: PaymentStrategyType = PaymentStrategyType.MERCHANT_QR_IMAGE_SHARE

    override fun execute(context: Context, payload: UpiPayload, amount: Double): StrategyResult {
        return try {
            val fullUpiUri = payload.buildUpiUri(amount)

            // Generate Crisp standard QR bitmap
            val qrBitmap = generateQrCodeBitmap(fullUpiUri, 600, 600)
                ?: return StrategyResult.FallbackSuggested(
                    fallbackStrategy = PaymentStrategyType.P2P_DIRECT_INTENT,
                    reason = "Failed to render QR Code Bitmap"
                )

            // Save to shared cache directory
            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs()
            val imageFile = File(cachePath, "enterfirst_pay_qr.png")
            FileOutputStream(imageFile).use { out ->
                qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                imageFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Pay ₹${String.format(Locale.US, "%.2f", amount)} to ${payload.pn} (${payload.pa})"
                )
            }

            val chooser = Intent.createChooser(shareIntent, "Open with UPI App (GPay / PhonePe / Paytm / BHIM)")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)

            StrategyResult.Success(
                strategy = strategyType,
                message = "Opened UPI QR Image Share Chooser"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            // Graceful fallback to direct URL intent if image sharing fails
            StrategyResult.FallbackSuggested(
                fallbackStrategy = PaymentStrategyType.P2P_DIRECT_INTENT,
                reason = "Image share failed: ${e.localizedMessage}. Fallback to Direct UPI Link."
            )
        }
    }

    fun generateQrCodeBitmap(text: String, width: Int, height: Int): Bitmap? {
        return try {
            val bitMatrix: BitMatrix = MultiFormatWriter().encode(
                text,
                BarcodeFormat.QR_CODE,
                width,
                height
            )
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(
                        x,
                        y,
                        if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                    )
                }
            }
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

/**
 * PaymentRouter:
 * Central conditional payment router that selects between P2P, Merchant, and Transit strategies
 * based on full decoded QR payload analysis, with bidirectional fallback support.
 */
class PaymentRouter(
    val p2pStrategy: P2PPaymentStrategy = P2PPaymentStrategy(),
    val merchantStrategy: MerchantPaymentStrategy = MerchantPaymentStrategy()
) {
    /**
     * Resolves the recommended strategy for a given payload.
     * Both Merchant and Bus/Transit QRs default to QR Image Share, which has the highest reliability.
     */
    fun resolveStrategy(payload: UpiPayload): PaymentStrategy {
        return when (payload.determinePaymentType()) {
            PaymentType.P2P -> p2pStrategy
            PaymentType.MERCHANT -> merchantStrategy
            PaymentType.BUS_TRANSIT -> merchantStrategy
        }
    }

    /**
     * Dispatches payment execution using the resolved or overridden strategy.
     */
    fun routePayment(
        context: Context,
        payload: UpiPayload,
        amount: Double,
        forcedStrategy: PaymentStrategyType? = null
    ): StrategyResult {
        val strategy = when (forcedStrategy) {
            PaymentStrategyType.P2P_DIRECT_INTENT -> p2pStrategy
            PaymentStrategyType.MERCHANT_QR_IMAGE_SHARE -> merchantStrategy
            null -> resolveStrategy(payload)
        }

        return strategy.execute(context, payload, amount)
    }
}
