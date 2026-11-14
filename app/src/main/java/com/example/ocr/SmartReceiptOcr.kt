package com.example.ocr

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.ui.screens.TransactionResult
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.Locale
import java.util.regex.Pattern

/**
 * Enterprise-Grade OCR & Financial Receipt Intelligence Engine:
 * 1. Smart Multimodal Gemini AI Vision (Primary for nuanced layouts, handwriting, complex invoices)
 * 2. High-Precision On-Device Google ML Kit Text Recognition (Instant, 100% offline fallback)
 * 3. Deep Entity Extraction: Filters UTR/Ref numbers, GSTIN, Phone numbers, and accurately calculates totals.
 * 4. Automatic Merchant, Category, and Transaction Type (Income vs Expense) classification.
 */
object SmartReceiptOcr {
    private const val TAG = "SmartReceiptOcr"

    private val KNOWN_MERCHANTS = listOf(
        "Swiggy", "Zomato", "Starbucks", "Uber", "Ola", "Rapido",
        "Amazon", "Flipkart", "Blinkit", "Zepto", "Dunzo", "Myntra",
        "BookMyShow", "MakeMyTrip", "BigBasket", "D-Mart", "DMart", "Reliance Fresh",
        "Reliance Digital", "Reliance Retail", "Airtel", "Jio", "Vodafone", "VI", "BESCOM",
        "Tata Power", "Shell", "HPCL", "BPCL", "IOCL", "Indian Oil",
        "Bharat Petroleum", "Hindustan Petroleum", "IRCTC", "KSRTC", "BMTC",
        "MSRTC", "TSRTC", "APSRTC", "Chalo", "KFC", "McDonalds", "Burger King",
        "Domino's", "Pizza Hut", "Subway", "Cult.fit", "Decathlon", "Apollo Pharmacy",
        "Netmeds", "1mg", "Tata 1mg", "PharmEasy", "Medplus", "Croma", "Vijay Sales",
        "Netflix", "Spotify", "Disney+ Hotstar", "Apple", "Google Play",
        "Nayara Energy", "Paytm", "PhonePe", "Google Pay", "CRED", "BHIM", "Amazon Pay",
        "HDFC Bank", "SBI", "ICICI Bank", "Axis Bank", "Kotak Bank", "IndusInd Bank",
        "Zara", "H&M", "Trends", "Max Fashion", "Pantaloons", "Westside", "Lenskart",
        "More Supermarket", "Spencer's", "Nature's Basket", "Haldiram's", "Haldirams",
        "Bikanervala", "Chai Point", "Chaayos", "Cafe Coffee Day", "CCD"
    )

    data class MlKitExtraction(
        val reconstructedLines: List<String>,
        val rawText: String,
        val result: TransactionResult?
    )

    /**
     * Primary entry point for OCR extraction.
     * Pre-processes spatial rows with ML Kit, then executes Gemini AI with the updated System Instructions,
     * seamlessly falling back to on-device ML Kit heuristic pipeline.
     */
    suspend fun processReceipt(bitmap: Bitmap): TransactionResult? = withContext(Dispatchers.Default) {
        val scaledBitmap = scaleBitmapForOcr(bitmap)

        // Step 1: Pre-process with on-device ML Kit to get spatial reconstructed rows
        val mlKitData = extractMlKitData(bitmap)
        val reconstructedLines = mlKitData?.reconstructedLines ?: emptyList()
        val mlKitResult = mlKitData?.result

        // Step 2: Primary Cloud Engine: High-Precision Multimodal Gemini AI Vision
        // System Prompt is loaded dynamically from local ocr_config.json (supporting live hot-fixing)
        val geminiKey = BuildConfig.GEMINI_API_KEY
        if (!geminiKey.isNullOrBlank() && geminiKey != "null" && geminiKey.length > 5) {
            try {
                val aiResult = processWithGemini(scaledBitmap, reconstructedLines, geminiKey)
                if (aiResult != null && (aiResult.amount > 0 || aiResult.party.isNotBlank())) {
                    val finalAmount = if (aiResult.amount > 0) aiResult.amount else (mlKitResult?.amount ?: 0.0)
                    val finalParty = if (aiResult.party.isNotBlank()) aiResult.party else (mlKitResult?.party ?: "")
                    val finalCategory = if (aiResult.category.isNotBlank() && aiResult.category != "Others") {
                        aiResult.category
                    } else {
                        mlKitResult?.category ?: aiResult.category
                    }
                    val finalResult = aiResult.copy(amount = finalAmount, party = finalParty, category = finalCategory)
                    Log.d(TAG, "Successfully extracted receipt data via Gemini AI (configured by ocr_config.json): $finalResult")
                    return@withContext finalResult
                }
            } catch (e: Exception) {
                Log.w(TAG, "Gemini AI processing failed, checking secondary fallbacks: ${e.message}")
            }
        }

        // Secondary Optional Fallback: Mindee API if configured
        val mindeeKey = BuildConfig.MINDEE_API_KEY
        if (!mindeeKey.isNullOrBlank() && mindeeKey != "null" && mindeeKey != "MY_MINDEE_API_KEY" && mindeeKey.length > 5) {
            try {
                val mindeeResult = processWithMindee(scaledBitmap, mindeeKey)
                if (mindeeResult != null && (mindeeResult.amount > 0 || mindeeResult.party.isNotBlank())) {
                    val finalAmount = if (mindeeResult.amount > 0) mindeeResult.amount else (mlKitResult?.amount ?: 0.0)
                    val finalParty = if (mindeeResult.party.isNotBlank()) mindeeResult.party else (mlKitResult?.party ?: "")
                    val finalCategory = if (mindeeResult.category.isNotBlank() && mindeeResult.category != "Others") {
                        mindeeResult.category
                    } else {
                        mlKitResult?.category ?: mindeeResult.category
                    }
                    val finalResult = mindeeResult.copy(amount = finalAmount, party = finalParty, category = finalCategory)
                    Log.d(TAG, "Extracted receipt data via secondary Mindee fallback: $finalResult")
                    return@withContext finalResult
                }
            } catch (e: Exception) {
                Log.w(TAG, "Mindee OCR fallback failed: ${e.message}")
            }
        }

        // Step 3: On-Device ML Kit Text Recognition with Spatial Row Reconstruction fallback
        if (mlKitResult != null && (mlKitResult.amount > 0 || mlKitResult.party.isNotBlank())) {
            Log.d(TAG, "Successfully extracted receipt data via ML Kit: $mlKitResult")
            return@withContext mlKitResult
        }

        // Step 4: Fallback result if neither could parse specific fields
        return@withContext mlKitResult ?: TransactionResult(
            type = "EXPENSE",
            amount = 0.0,
            party = "Receipt / Bill",
            category = "Others",
            note = "Scanned Receipt"
        )
    }

    /**
     * Preserves high visual sharpness up to 2400px so small printed numbers remain clearly legible.
     */
    private fun scaleBitmapForOcr(bitmap: Bitmap): Bitmap {
        val maxDimension = 2400
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDimension && height <= maxDimension) {
            return bitmap
        }
        val ratio = width.toFloat() / height.toFloat()
        val newWidth: Int
        val newHeight: Int
        if (width > height) {
            newWidth = maxDimension
            newHeight = (maxDimension / ratio).toInt()
        } else {
            newHeight = maxDimension
            newWidth = (maxDimension * ratio).toInt()
        }
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * On-Device ML Kit Text Recognition with Spatial Row Reconstruction.
     */
    private suspend fun extractMlKitData(bitmap: Bitmap): MlKitExtraction? = withContext(Dispatchers.IO) {
        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val visionText = recognizer.process(image).await()
            val rawText = visionText.text
            if (rawText.isBlank()) return@withContext null

            val allLines = mutableListOf<String>()
            val lineObjects = mutableListOf<com.google.mlkit.vision.text.Text.Line>()
            for (block in visionText.textBlocks) {
                for (line in block.lines) {
                    val lineText = line.text.trim()
                    if (lineText.isNotEmpty()) {
                        allLines.add(lineText)
                        lineObjects.add(line)
                    }
                }
            }

            // Spatial row reconstruction: merge columnar lines that visually occupy the same row
            val reconstructedLines = reconstructSpatialLines(lineObjects)
            val effectiveLines = if (reconstructedLines.isNotEmpty()) reconstructedLines else allLines
            val combinedText = effectiveLines.joinToString("\n") + "\n\n" + rawText

            val parsed = parseTextToTransaction(combinedText, effectiveLines)
            MlKitExtraction(reconstructedLines = effectiveLines, rawText = rawText, result = parsed)
        } catch (e: Exception) {
            Log.e(TAG, "ML Kit OCR processing error: ${e.message}", e)
            null
        }
    }

    /**
     * Merges columnar text elements into single horizontal lines based on vertical bounding-box alignment.
     */
    private fun reconstructSpatialLines(lineObjects: List<com.google.mlkit.vision.text.Text.Line>): List<String> {
        if (lineObjects.isEmpty()) return emptyList()

        val validLines = lineObjects.filter { it.boundingBox != null }
        if (validLines.isEmpty()) {
            return lineObjects.map { it.text.trim() }.filter { it.isNotEmpty() }
        }

        val sortedByY = validLines.sortedBy { it.boundingBox?.top ?: 0 }
        val rows = mutableListOf<MutableList<com.google.mlkit.vision.text.Text.Line>>()

        for (line in sortedByY) {
            val box = line.boundingBox ?: continue
            val centerY = (box.top + box.bottom) / 2
            val height = (box.bottom - box.top).coerceAtLeast(10)
            val tolerance = (height * 0.65f).coerceIn(12f, 35f).toInt()

            var matchedRow: MutableList<com.google.mlkit.vision.text.Text.Line>? = null
            for (row in rows) {
                val firstBox = row[0].boundingBox ?: continue
                val rowCenterY = (firstBox.top + firstBox.bottom) / 2
                if (Math.abs(rowCenterY - centerY) <= tolerance) {
                    matchedRow = row
                    break
                }
            }

            if (matchedRow != null) {
                matchedRow.add(line)
            } else {
                rows.add(mutableListOf(line))
            }
        }

        val result = mutableListOf<String>()
        for (row in rows) {
            row.sortBy { it.boundingBox?.left ?: 0 }
            val joined = row.joinToString(" ") { it.text.trim() }.trim()
            if (joined.isNotEmpty()) {
                result.add(joined)
            }
        }
        return result
    }

    /**
     * Parses raw extracted text lines to extract Amount, Merchant, Category, and Type.
     */
    fun parseTextToTransaction(rawText: String, lines: List<String>): TransactionResult {
        val lowerText = rawText.lowercase(Locale.ROOT)

        // --- 1. AMOUNT EXTRACTION ---
        var detectedAmount = 0.0

        // Explicit structural total keywords ranked by specificity
        val totalKeywords = listOf(
            "total payment", "grand total", "net payable", "final total",
            "amount paid", "total payable", "amount due", "balance due",
            "net total", "invoice total", "bill total", "order total",
            "total amt", "net amt", "bill amt", "gross amt", "to pay", "total"
        )

        val amountCurrencyRegex = Pattern.compile("(?:₹|rs\\.?|inr)?\\s*([0-9,]+(?:\\.[0-9]{1,2}))", Pattern.CASE_INSENSITIVE)

        // Strategy 1: Scan lines from bottom up for structural Total keyword
        val bottomLinesFirst = lines.asReversed()
        for (keyword in totalKeywords) {
            val kwPattern = Pattern.compile("\\b" + Pattern.quote(keyword) + "\\b", Pattern.CASE_INSENSITIVE)
            for (i in bottomLinesFirst.indices) {
                val line = bottomLinesFirst[i]
                if (kwPattern.matcher(line).find()) {
                    // Check if numeric amount is on this exact line
                    val matcher = amountCurrencyRegex.matcher(line)
                    var lineAmount = 0.0
                    while (matcher.find()) {
                        val numStr = matcher.group(1)?.replace(",", "")?.trim() ?: ""
                        val parsed = numStr.toDoubleOrNull() ?: 0.0
                        if (parsed in 0.5..9999999.0 && !isLikelyNoiseNumber(parsed, numStr)) {
                            lineAmount = parsed
                        }
                    }
                    if (lineAmount > 0.0) {
                        detectedAmount = lineAmount
                        break
                    }

                    // If keyword is standalone on line, check the next line below it in physical order
                    if (i > 0) {
                        val lineBelow = bottomLinesFirst[i - 1]
                        val matcherBelow = amountCurrencyRegex.matcher(lineBelow)
                        var belowAmount = 0.0
                        while (matcherBelow.find()) {
                            val numStr = matcherBelow.group(1)?.replace(",", "")?.trim() ?: ""
                            val parsed = numStr.toDoubleOrNull() ?: 0.0
                            if (parsed in 0.5..9999999.0 && !isLikelyNoiseNumber(parsed, numStr)) {
                                belowAmount = parsed
                            }
                        }
                        if (belowAmount > 0.0) {
                            detectedAmount = belowAmount
                            break
                        }
                    }
                }
            }
            if (detectedAmount > 0.0) break
        }

        // Strategy 2: Search in rawText for keyword followed closely by amount
        if (detectedAmount <= 0.0) {
            for (keyword in totalKeywords) {
                val p = Pattern.compile("\\b" + Pattern.quote(keyword) + "\\b[^0-9\\n]{0,40}?(?:₹|rs\\.?|inr)?\\s*([0-9,]+(?:\\.[0-9]{1,2}))", Pattern.CASE_INSENSITIVE)
                val m = p.matcher(rawText)
                var lastMatch = 0.0
                while (m.find()) {
                    val numStr = m.group(1)?.replace(",", "")?.trim() ?: ""
                    val parsed = numStr.toDoubleOrNull() ?: 0.0
                    if (parsed in 0.5..9999999.0 && !isLikelyNoiseNumber(parsed, numStr)) {
                        lastMatch = parsed
                    }
                }
                if (lastMatch > 0.0) {
                    detectedAmount = lastMatch
                    break
                }
            }
        }

        // Strategy 2.5: Multi-item tabular invoice row detection (when no explicit Total row is printed)
        if (detectedAmount <= 0.0) {
            val headerIdx = lines.indexOfFirst { l ->
                val upper = l.uppercase(Locale.ROOT)
                (upper.contains("ITEM") || upper.contains("DESCRIPTION") || upper.contains("PARTICULARS")) &&
                        (upper.contains("AMOUNT") || upper.contains("QTY") || upper.contains("RATE") || upper.contains("PRICE"))
            }
            if (headerIdx != -1) {
                var itemSum = 0.0
                var itemCount = 0
                val rowAmountRegex = Pattern.compile("(?:₹|rs\\.?|inr)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)\\s*$", Pattern.CASE_INSENSITIVE)
                for (j in (headerIdx + 1) until lines.size) {
                    val line = lines[j].trim()
                    if (line.isEmpty()) continue
                    val m = rowAmountRegex.matcher(line)
                    if (m.find()) {
                        val numStr = m.group(1)?.replace(",", "")?.trim() ?: ""
                        val valDouble = numStr.toDoubleOrNull() ?: 0.0
                        if (valDouble > 0.0 && !isLikelyNoiseNumber(valDouble, numStr)) {
                            itemSum += valDouble
                            itemCount++
                        }
                    }
                }
                if (itemCount >= 2 && itemSum > 0.0) {
                    detectedAmount = itemSum
                }
            }
        }

        // Strategy 3: Check explicit UPI payment patterns
        if (detectedAmount <= 0.0) {
            val upiPatterns = listOf(
                Pattern.compile("\\b(?:paid|payment of|sent|transfer of|received|debited|credited)\\b\\s*[:=-]?\\s*(?:₹|rs\\.?|inr)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)", Pattern.CASE_INSENSITIVE),
                Pattern.compile("(?:₹|rs\\.?|inr)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)", Pattern.CASE_INSENSITIVE)
            )
            for (p in upiPatterns) {
                val m = p.matcher(rawText)
                var lastMatch = 0.0
                while (m.find()) {
                    val numStr = m.group(1)?.replace(",", "")?.trim() ?: ""
                    val parsed = numStr.toDoubleOrNull() ?: 0.0
                    if (parsed in 0.5..9999999.0 && !isLikelyNoiseNumber(parsed, numStr)) {
                        lastMatch = parsed
                    }
                }
                if (lastMatch > 0.0) {
                    detectedAmount = lastMatch
                    break
                }
            }
        }

        // Strategy 4: Final visual boundary extraction - The bottom-most monetary decimal printed on receipt
        if (detectedAmount <= 0.0) {
            val decimalPattern = Pattern.compile("\\b([0-9]{1,6}\\.[0-9]{2})\\b")
            val candidateAmounts = mutableListOf<Double>()
            for (line in lines) {
                val m = decimalPattern.matcher(line)
                while (m.find()) {
                    val numStr = m.group(1)?.trim() ?: ""
                    val parsed = numStr.toDoubleOrNull() ?: 0.0
                    if (parsed in 0.5..9999999.0 && !isLikelyNoiseNumber(parsed, numStr)) {
                        candidateAmounts.add(parsed)
                    }
                }
            }
            if (candidateAmounts.isNotEmpty()) {
                // In receipts, the bottom-most number is the final total (e.g. 523.80 at the bottom of the column)
                detectedAmount = candidateAmounts.last()
            }
        }

        // --- 2. MERCHANT / PARTY EXTRACTION ---
        var detectedParty = ""

        // Check for known merchants first
        for (merchant in KNOWN_MERCHANTS) {
            if (rawText.contains(merchant, ignoreCase = true)) {
                detectedParty = merchant
                break
            }
        }

        // Check for explicit "Paid to", "Bill From", "Sold By", "Merchant", "Store"
        // Strict word boundary ensures words like "Total" or "Payment" are not matched as prefixes
        if (detectedParty.isBlank()) {
            val billFromRegex = Pattern.compile(
                "\\b(?:paid to|payment to|bill from|sold by|vendor|store|merchant|biller|sent to|transfer to)\\b\\s*[:=-]?\\s*([a-zA-Z0-9 &._'-]{3,45})",
                Pattern.CASE_INSENSITIVE
            )
            val matcher = billFromRegex.matcher(rawText)
            while (matcher.find()) {
                val candidate = matcher.group(1)?.trim() ?: ""
                val lowerCandidate = candidate.lowercase(Locale.ROOT)
                val isInvalid = lowerCandidate.contains("total") ||
                        lowerCandidate.contains("payment") ||
                        lowerCandidate.contains("amount") ||
                        lowerCandidate.contains("receipt") ||
                        lowerCandidate.contains("invoice") ||
                        lowerCandidate.contains("cashier") ||
                        lowerCandidate.contains("account") ||
                        lowerCandidate.contains("successful") ||
                        candidate.contains("₹") ||
                        candidate.contains("rs", ignoreCase = true)
                if (candidate.isNotBlank() && !isInvalid) {
                    detectedParty = candidate
                    break
                }
            }
        }

        // Check for UPI VPA ID (e.g. merchant@okaxis, store@upi)
        if (detectedParty.isBlank()) {
            val vpaPattern = Pattern.compile("([a-zA-Z0-9.\\-_]{2,64}@[a-zA-Z0-9.\\-_]{2,32})")
            val matcher = vpaPattern.matcher(rawText)
            if (matcher.find()) {
                val vpa = matcher.group(1)?.trim() ?: ""
                if (vpa.isNotBlank() && !vpa.contains("example") && !vpa.contains("schema")) {
                    detectedParty = vpa
                }
            }
        }

        // Fallback to top non-empty business header line (e.g. "SHOP NAME" below "CASH RECEIPT")
        if (detectedParty.isBlank()) {
            val firstMeaningfulLine = lines.firstOrNull { line ->
                val trimmed = line.trim()
                val lower = trimmed.lowercase(Locale.ROOT)
                trimmed.length in 3..40 &&
                        !trimmed.contains("₹") &&
                        !lower.contains("cash receipt") &&
                        !lower.contains("tax invoice") &&
                        !lower.contains("invoice") &&
                        !lower.contains("cash memo") &&
                        !lower.contains("bill of supply") &&
                        !lower.contains("receipt") &&
                        !lower.contains("total") &&
                        !lower.contains("payment") &&
                        !lower.contains("welcome") &&
                        !lower.contains("thank you") &&
                        !lower.contains("address") &&
                        !lower.contains("date") &&
                        !lower.contains("cashier") &&
                        !trimmed.all { it.isDigit() || it == '.' || it == ',' || it == '-' || it == ':' || it == '/' }
            }
            detectedParty = firstMeaningfulLine?.trim() ?: "Receipt / Bill"
        }

        // Clean up party name from trailing noise
        detectedParty = detectedParty
            .replace(Regex("(?i)\\b(pvt ltd|private limited|ltd|llp|inc|co)\\b"), "")
            .replace(Regex("[:=-]+"), "")
            .trim()
        if (detectedParty.isBlank() || detectedParty.equals("Total Payment", ignoreCase = true) || detectedParty.equals("Payment", ignoreCase = true)) {
            detectedParty = "Merchant / Store"
        }

        // --- 3. CATEGORY CLASSIFICATION ---
        val category = when {
            lowerText.contains("swiggy") || lowerText.contains("zomato") || lowerText.contains("restaurant") ||
                    lowerText.contains("food") || lowerText.contains("cafe") || lowerText.contains("dine") ||
                    lowerText.contains("pizza") || lowerText.contains("burger") || lowerText.contains("bakery") ||
                    lowerText.contains("coffee") || lowerText.contains("tea") || lowerText.contains("kitchen") ||
                    lowerText.contains("hotel") || lowerText.contains("biryani") || lowerText.contains("sweets") ||
                    lowerText.contains("chai") || lowerText.contains("snack") || lowerText.contains("dining") ||
                    lowerText.contains("kfc") || lowerText.contains("mcdonald") || lowerText.contains("domino") -> "Food"

            lowerText.contains("uber") || lowerText.contains("ola") || lowerText.contains("rapido") ||
                    lowerText.contains("petrol") || lowerText.contains("fuel") || lowerText.contains("diesel") ||
                    lowerText.contains("toll") || lowerText.contains("parking") || lowerText.contains("metro") ||
                    lowerText.contains("bus") || lowerText.contains("irctc") || lowerText.contains("flight") ||
                    lowerText.contains("train") || lowerText.contains("ksrtc") || lowerText.contains("bmtc") ||
                    lowerText.contains("transport") || lowerText.contains("travel") || lowerText.contains("chalo") ||
                    lowerText.contains("fastag") || lowerText.contains("shell") || lowerText.contains("hpcl") ||
                    lowerText.contains("bpcl") || lowerText.contains("iocl") || lowerText.contains("indian oil") -> "Travel"

            lowerText.contains("electricity") || lowerText.contains("bescom") || lowerText.contains("water") ||
                    lowerText.contains("wifi") || lowerText.contains("broadband") || lowerText.contains("airtel") ||
                    lowerText.contains("jio") || lowerText.contains("recharge") || lowerText.contains("gas") ||
                    lowerText.contains("bill") || lowerText.contains("utility") || lowerText.contains("rent") ||
                    lowerText.contains("maintenance") || lowerText.contains("tata power") || lowerText.contains("vodafone") ||
                    lowerText.contains("dth") || lowerText.contains("cylinder") -> "Bills"

            lowerText.contains("pharmacy") || lowerText.contains("hospital") || lowerText.contains("clinic") ||
                    lowerText.contains("medicine") || lowerText.contains("doctor") || lowerText.contains("apollo") ||
                    lowerText.contains("1mg") || lowerText.contains("pharmeasy") || lowerText.contains("lab") ||
                    lowerText.contains("medical") || lowerText.contains("dental") || lowerText.contains("health") ||
                    lowerText.contains("medplus") || lowerText.contains("diagnostic") -> "Health"

            lowerText.contains("school") || lowerText.contains("college") || lowerText.contains("tuition") ||
                    lowerText.contains("fee") || lowerText.contains("course") || lowerText.contains("book") ||
                    lowerText.contains("udemy") || lowerText.contains("education") || lowerText.contains("class") ||
                    lowerText.contains("stationery") -> "Education"

            lowerText.contains("netflix") || lowerText.contains("spotify") || lowerText.contains("hotstar") ||
                    lowerText.contains("prime") || lowerText.contains("movie") || lowerText.contains("cinema") ||
                    lowerText.contains("pvr") || lowerText.contains("inox") || lowerText.contains("game") ||
                    lowerText.contains("entertainment") || lowerText.contains("youtube") || lowerText.contains("bookmyshow") -> "Entertainment"

            lowerText.contains("samsung") || lowerText.contains("boat") || lowerText.contains("speaker") ||
                    lowerText.contains("headphone") || lowerText.contains("phone") || lowerText.contains("electronics") ||
                    lowerText.contains("laptop") || lowerText.contains("gadget") || lowerText.contains("bluetooth") ||
                    lowerText.contains("croma") || lowerText.contains("reliance digital") || lowerText.contains("amazon") ||
                    lowerText.contains("flipkart") || lowerText.contains("myntra") || lowerText.contains("shopping") ||
                    lowerText.contains("retail") || lowerText.contains("store") || lowerText.contains("market") ||
                    lowerText.contains("dmart") || lowerText.contains("d-mart") || lowerText.contains("blinkit") ||
                    lowerText.contains("zepto") || lowerText.contains("bigbasket") || lowerText.contains("zara") ||
                    lowerText.contains("h&m") || lowerText.contains("decathlon") -> "Shopping"

            else -> "Others"
        }

        // --- 4. TRANSACTION TYPE ---
        val type = when {
            lowerText.contains("received from") || lowerText.contains("received ₹") ||
                    lowerText.contains("credited") || lowerText.contains("refund") ||
                    lowerText.contains("cashback") || lowerText.contains("income") ||
                    lowerText.contains("salary") || lowerText.contains("deposit") -> "INCOME"
            else -> "EXPENSE"
        }

        val note = if (detectedParty.isNotBlank() && detectedParty != "Merchant / Store" && detectedParty != "Receipt / Bill") {
            "$category payment to $detectedParty"
        } else {
            "Scanned $category receipt"
        }

        return TransactionResult(
            type = type,
            amount = detectedAmount,
            party = detectedParty,
            category = category,
            note = note
        )
    }

    /**
     * Extracts individual line-item net amounts from item rows in an invoice table.
     */
    private fun extractLineItemAmounts(lines: List<String>): List<Double> {
        val amounts = mutableListOf<Double>()
        val currencyRegex = Pattern.compile("(?:₹|rs\\.?|inr)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)", Pattern.CASE_INSENSITIVE)

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isBlank()) continue

            val lower = trimmed.lowercase(Locale.ROOT)
            if (lower.contains("gstin") || lower.contains("phone") || lower.contains("mobile") ||
                lower.contains("bill date") || lower.contains("due date") || lower.contains("po no") ||
                lower.contains("pin") || lower.contains("vehicle") || (lower.contains("items") && lower.contains("amount")) ||
                lower.contains("total") || lower.contains("subtotal") || lower.contains("net payable") ||
                lower.contains("amount paid") || lower.contains("paid amount") || lower.contains("balance") ||
                lower.contains("round off") || lower.contains("tax") || lower.contains("cgst") || lower.contains("sgst")
            ) {
                continue
            }

            val matcher = currencyRegex.matcher(trimmed)
            val lineAmounts = mutableListOf<Double>()
            while (matcher.find()) {
                val str = matcher.group(1)?.replace(",", "")?.trim() ?: ""
                val parsed = str.toDoubleOrNull() ?: 0.0
                if (parsed in 0.5..9999999.0 && !isLikelyNoiseNumber(parsed, str)) {
                    lineAmounts.add(parsed)
                }
            }

            if (lineAmounts.isNotEmpty()) {
                // The last numeric amount in an invoice row is typically the item line total
                val netItemAmount = lineAmounts.last()
                amounts.add(netItemAmount)
            }
        }

        return amounts
    }

    /**
     * Rejects phone numbers (10 digits starting with 6,7,8,9), UTR/UPI reference numbers (12 digits),
     * postal codes (6 digits), 4-digit years (2024..2030), and GST codes.
     */
    private fun isLikelyNoiseNumber(value: Double, rawStr: String): Boolean {
        val cleanStr = rawStr.replace(".", "").replace(",", "").trim()
        
        // 10-digit Indian Mobile Numbers
        if (cleanStr.length == 10 && (cleanStr.startsWith("9") || cleanStr.startsWith("8") || cleanStr.startsWith("7") || cleanStr.startsWith("6"))) {
            return true
        }

        // 12-digit UPI / UTR transaction reference numbers
        if (cleanStr.length in 11..13) {
            return true
        }

        // 6-digit Postal PIN code
        if (cleanStr.length == 6 && (cleanStr.startsWith("5") || cleanStr.startsWith("6") || cleanStr.startsWith("1") || cleanStr.startsWith("4") || cleanStr.startsWith("7") || cleanStr.startsWith("8") || cleanStr.startsWith("2") || cleanStr.startsWith("3"))) {
            return true
        }

        // Year values (e.g. 2024, 2025, 2026, 2027)
        if (cleanStr.length == 4 && (cleanStr.startsWith("202") || cleanStr.startsWith("203"))) {
            return true
        }

        return false
    }

    /**
     * Drop-in replacement Financial & Receipt OCR using Mindee API.
     * Takes receipt image, posts multipart/form-data to Mindee's Expense Receipts / Financial Document endpoint,
     * and extracts the clean numeric total, merchant / supplier, and categorization.
     */
    private suspend fun processWithMindee(
        bitmap: Bitmap,
        apiKey: String
    ): TransactionResult? = withContext(Dispatchers.IO) {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        val imageBytes = outputStream.toByteArray()

        val endpoints = listOf(
            "https://api.mindee.net/v1/products/mindee/expense_receipts/v5/predict",
            "https://api.mindee.net/v1/products/mindee/financial_document/v1/predict"
        )

        val client = OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                name = "document",
                filename = "receipt.jpg",
                body = imageBytes.toRequestBody("image/jpeg".toMediaType())
            )
            .build()

        for (endpoint in endpoints) {
            try {
                val request = Request.Builder()
                    .url(endpoint)
                    .addHeader("Authorization", "Token $apiKey")
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.w(TAG, "Mindee endpoint $endpoint returned HTTP ${response.code}: ${response.message}")
                        return@use
                    }
                    val bodyString = response.body?.string() ?: return@use
                    val json = JSONObject(bodyString)
                    val document = json.optJSONObject("document") ?: return@use
                    val inference = document.optJSONObject("inference") ?: return@use
                    val prediction = inference.optJSONObject("prediction") ?: return@use

                    // Extract Clean Numeric Total Amount
                    val amountVal = extractMindeeAmount(prediction)

                    // Extract Supplier / Merchant Name
                    val supplierName = extractMindeeSupplier(prediction)

                    // Extract Category
                    val categoryVal = extractMindeeCategory(prediction)

                    // Extract Date or Note
                    val dateVal = prediction.optJSONObject("date")?.optString("value", "") ?: ""
                    val noteVal = if (dateVal.isNotBlank()) "Receipt $dateVal" else "Receipt scan"

                    Log.d(TAG, "Mindee OCR parsed successfully: amount=$amountVal, supplier=$supplierName, category=$categoryVal")
                    return@withContext TransactionResult(
                        type = "EXPENSE",
                        amount = amountVal,
                        party = supplierName,
                        category = categoryVal,
                        note = noteVal
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Mindee request to $endpoint error: ${e.message}")
            }
        }
        return@withContext null
    }

    private fun extractMindeeAmount(prediction: JSONObject): Double {
        // Priority 1: total_amount
        val totalObj = prediction.optJSONObject("total_amount")
        if (totalObj != null && !totalObj.isNull("value")) {
            val v = totalObj.opt("value")
            val parsed = when (v) {
                is Number -> v.toDouble()
                is String -> v.replace(",", "").replace("₹", "").replace("$", "").trim().toDoubleOrNull() ?: 0.0
                else -> 0.0
            }
            if (parsed > 0.0) return parsed
        }

        // Priority 2: total_net
        val netObj = prediction.optJSONObject("total_net")
        if (netObj != null && !netObj.isNull("value")) {
            val v = netObj.opt("value")
            val parsed = when (v) {
                is Number -> v.toDouble()
                is String -> v.replace(",", "").replace("₹", "").replace("$", "").trim().toDoubleOrNull() ?: 0.0
                else -> 0.0
            }
            if (parsed > 0.0) return parsed
        }

        return 0.0
    }

    private fun extractMindeeSupplier(prediction: JSONObject): String {
        val keys = listOf("supplier_name", "supplier", "merchant_name")
        for (key in keys) {
            val obj = prediction.optJSONObject(key)
            if (obj != null && !obj.isNull("value")) {
                val value = obj.optString("value", "").trim()
                if (value.isNotBlank() && !value.equals("null", ignoreCase = true)) {
                    return value
                }
            }
        }
        return ""
    }

    private fun extractMindeeCategory(prediction: JSONObject): String {
        val catObj = prediction.optJSONObject("category")
        val rawCat = if (catObj != null && !catObj.isNull("value")) {
            catObj.optString("value", "").lowercase(Locale.ROOT)
        } else ""

        return when {
            rawCat.contains("food") || rawCat.contains("restaurant") || rawCat.contains("meal") || rawCat.contains("snack") -> "Food"
            rawCat.contains("transport") || rawCat.contains("toll") || rawCat.contains("fuel") || rawCat.contains("gas") || rawCat.contains("travel") -> "Travel"
            rawCat.contains("shopping") || rawCat.contains("supplies") || rawCat.contains("retail") || rawCat.contains("cloth") -> "Shopping"
            rawCat.contains("utilit") || rawCat.contains("bill") || rawCat.contains("telecom") || rawCat.contains("internet") -> "Bills"
            rawCat.contains("health") || rawCat.contains("medical") || rawCat.contains("pharmacy") -> "Health"
            rawCat.contains("education") || rawCat.contains("book") -> "Education"
            rawCat.contains("entertainment") || rawCat.contains("leisure") || rawCat.contains("hotel") -> "Entertainment"
            else -> "Others"
        }
    }

    /**
     * High-Precision Multimodal Gemini AI Vision & Structured Financial OCR Request.
     */
    private suspend fun processWithGemini(
        bitmap: Bitmap,
        reconstructedLines: List<String>,
        apiKey: String
    ): TransactionResult? = withContext(Dispatchers.IO) {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        val base64Data = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)

        // System Prompt retrieved dynamically from local ocr_config.json configuration framework
        val systemInstructionText = OcrConfigManager.getSystemPrompt()

        val rowsFormatted = if (reconstructedLines.isNotEmpty()) {
            buildString {
                appendLine("--- RECONSTRUCTED SPATIAL TEXT ROWS (Ordered Left-to-Right, Top to Bottom) ---")
                reconstructedLines.forEachIndexed { idx, line ->
                    appendLine("Row ${idx + 1}: $line")
                }
                appendLine("--- END OF RECONSTRUCTED ROWS ---")
            }
        } else {
            "Reconstructed spatial text rows unavailable from pre-processing. Scan receipt image directly."
        }

        val userPrompt = """
$rowsFormatted

Analyze the receipt image and the pre-processed reconstructed spatial text rows above.
Strictly adhere to the System Instructions: execute the Receipt Total Extraction Protocol, Absolute Mathematical Guardrail, and Merchant Detection Protocol.

Output exclusively a valid JSON object with the following schema:
{
  "total_amount": <number or null, the literal pre-printed final grand total>,
  "party": <string or null, literal merchant or shop name from top rows>,
  "category": <string, one of 'Food', 'Travel', 'Shopping', 'Bills', 'Health', 'Education', 'Entertainment', 'Others'>,
  "type": <string, 'EXPENSE' or 'INCOME'>,
  "note": <string or null, short 3-5 word description>
}
        """.trimIndent()

        val requestJson = JSONObject().apply {
            put("systemInstruction", JSONObject().apply {
                put("parts", org.json.JSONArray().apply {
                    put(JSONObject().put("text", systemInstructionText))
                })
            })
            put("contents", org.json.JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", org.json.JSONArray().apply {
                        put(JSONObject().put("text", userPrompt))
                        put(JSONObject().apply {
                            put("inlineData", JSONObject().apply {
                                put("mimeType", "image/jpeg")
                                put("data", base64Data)
                            })
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.0)
            })
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = requestJson.toString().toRequestBody(mediaType)

        // Modern Gemini model endpoints supported in Google AI Studio
        val candidateModels = listOf(
            "gemini-3.5-flash",
            "gemini-flash-latest",
            "gemini-3.1-flash-lite-preview",
            "gemini-3.1-pro-preview"
        )

        val client = OkHttpClient.Builder()
            .connectTimeout(45, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(45, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(45, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        for (model in candidateModels) {
            try {
                val request = Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey")
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use
                    val responseString = response.body?.string() ?: return@use
                    val jsonObject = JSONObject(responseString)
                    val candidates = jsonObject.optJSONArray("candidates") ?: return@use
                    val content = candidates.getJSONObject(0).optJSONObject("content") ?: return@use
                    val parts = content.optJSONArray("parts") ?: return@use
                    val textStr = parts.getJSONObject(0).optString("text") ?: return@use

                    val cleanJsonStr = textStr.replace("```json", "").replace("```", "").trim()
                    val resultJson = JSONObject(cleanJsonStr)

                    val typeVal = resultJson.optString("type", "EXPENSE")
                    val amountVal = when {
                        resultJson.has("total_amount") && !resultJson.isNull("total_amount") -> {
                            val v = resultJson.opt("total_amount")
                            when (v) {
                                is Number -> v.toDouble()
                                is String -> v.replace(",", "").replace("₹", "").trim().toDoubleOrNull() ?: 0.0
                                else -> 0.0
                            }
                        }
                        resultJson.has("amount") && !resultJson.isNull("amount") -> {
                            val v = resultJson.opt("amount")
                            when (v) {
                                is Number -> v.toDouble()
                                is String -> v.replace(",", "").replace("₹", "").trim().toDoubleOrNull() ?: 0.0
                                else -> 0.0
                            }
                        }
                        else -> 0.0
                    }
                    val partyRaw = if (resultJson.isNull("party")) "" else resultJson.optString("party", "")
                    val merchantRaw = if (resultJson.isNull("merchant")) "" else resultJson.optString("merchant", "")
                    val storeRaw = if (resultJson.isNull("store_name")) "" else resultJson.optString("store_name", "")
                    val partyVal = listOf(partyRaw, merchantRaw, storeRaw)
                        .firstOrNull { it.isNotBlank() && !it.equals("null", ignoreCase = true) } ?: ""

                    val categoryRaw = if (resultJson.isNull("category")) "Others" else resultJson.optString("category", "Others")
                    val categoryVal = if (categoryRaw.isBlank() || categoryRaw.equals("null", ignoreCase = true)) "Others" else categoryRaw

                    val noteRaw = if (resultJson.isNull("note")) "" else resultJson.optString("note", "")
                    val noteVal = if (noteRaw.equals("null", ignoreCase = true)) "" else noteRaw

                    return@withContext TransactionResult(typeVal, amountVal, partyVal, categoryVal, noteVal)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Gemini endpoint $model attempt failed: ${e.message}")
            }
        }

        return@withContext null
    }
}
