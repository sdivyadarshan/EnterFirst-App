package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

data class ManualSection(
    val id: Int,
    val title: String,
    val content: String
)

object UserManualPdfHelper {

    const val USER_MANUAL_PDF_LINK = "https://drive.google.com/file/d/13o5_uCBUAiw0ADhfILUDGLHrZGyi_drk/view?usp=drivesdk"

    fun isOnline(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
            val network = cm?.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            false
        }
    }

    val SECTIONS: List<ManualSection> = listOf(
        ManualSection(
            1,
            "1. Introduction & Core Philosophy",
            """EnterFirst is a modern, privacy-first expense tracking application designed to help users log and categorize expenditures before they complete digital UPI payments, instead of trying to remember them after the fact. By combining the logging step and the payment transaction workflow into one fluid interface, the app eliminates the typical friction associated with manual bookkeeping, while ensuring complete data confidentiality and offline autonomy.

The Core Problem:
• The Forgetfulness Tax — manual post-payment logging suffers from high abandonment rates because users simply forget to open the app after paying.
• The Privacy Trap — automated tracking apps scrape private SMS messages or require direct, high-risk access to online banking passwords and servers.

The Golden Rule:
“Privacy first, Track next.”
EnterFirst breaks this paradigm by operating under one strict rule: it captures transaction metadata first, during the scanning process itself — not after the money has already moved.

What EnterFirst is NOT:
✗ It is NOT a bank
✗ It is NOT a digital wallet
✗ It is NOT a payment processor
It never stores credit credentials, and it never touches or processes your money."""
        ),
        ManualSection(
            2,
            "2. High-Level Technical Architecture",
            """EnterFirst runs as an offline-first sandbox layer on top of your device's existing UPI setup. Every payment you scan flows through a short, auditable pipeline before control is handed to your banking app.

The Refined Payment Workflow:
1. Scan QR Code: When you scan a merchant's UPI QR code, the app decodes the image into a standard UPI deep-link URI payload (e.g. upi://pay?pa=merchant@upi&pn=MerchantName&am=100.00&cu=INR).
2. Cryptographic Parsing: The internal parser extracts the recipient's UPI ID (pa), Payee/Merchant Name (pn), and pre-filled transaction amount (am).
3. Local Logging: Once verified and categorized by the user, EnterFirst commits the log to your local device database.
4. Handoff to UPI App: The payment data is handed off via URL push or generated QR share intent to your preferred certified UPI application (Google Pay, PhonePe, Paytm, Navi, BHIM) to finalize the transfer.

Payment Handoff Modes:
• URL Push (Default Mode): Pushes a standard upi:// deep-link directly to your device's chosen UPI handler via URL intent.
• P2P Support Scope: True peer-to-peer (P2P) URL push works best with Navi or BHIM.
• Automatic Fallback: For other apps (Google Pay, PhonePe, Paytm), automatically falls back to Generated QR Share Intent."""
        ),
        ManualSection(
            3,
            "3. Data Model & Local Storage Schema",
            """Every entry in EnterFirst is normalized into a single local transaction record before it is written to the on-device database:
• Transaction ID: Unique local identifier auto-generated per entry.
• Type: Enum INCOME or EXPENSE.
• Account Type: Enum BANK or CASH, indicating which baseline is affected.
• Amount: Decimal transaction value in INR.
• Category: One of the supported spending categories (Food, Travel, Shopping, Education, Entertainment, Bills, Health, Others).
• Merchant / Description: Payee name (from QR/OCR) or user-entered description.
• Note: Freeform note added by the user.
• Source: SCAN_AND_PAY, MANUAL, OCR_DETECTED, or DUPLICATED.
• Timestamp: Local device time captured at the moment of saving.

Derived, not duplicated:
The Combined Available Balance shown on the dashboard is calculated on demand from your onboarding baselines plus every INCOME and EXPENSE record in the local database."""
        ),
        ManualSection(
            4,
            "4. The Secure Privacy Advantage & Permissions",
            """To maintain complete, uncompromised security, EnterFirst does NOT:
• Request SMS reading or scraping permissions.
• Link to external bank accounts or credentials.
• Require OTP verifications or financial passwords.
• Upload financial data to third-party cloud analytics databases.

Permissions Checklist:
• Camera: Requested for scanning UPI QR codes and photographing receipts for OCR detection.
• SMS Read: NEVER requested.
• Bank Account Link: NEVER requested.
• Contacts: Not required.
• Internet: Optional — used only if you link an email for Cloud Backup or update notifications."""
        ),
        ManualSection(
            5,
            "5. Terms & Conditions (Setup Agreement)",
            """EnterFirst — Terms & Conditions Summary:
1. EnterFirst is not a bank, digital wallet, or payment processor. It does not hold, transmit, or process your money.
2. EnterFirst acts only as a redirector and local logging layer. It hands off your payment details to a UPI-certified app of your choice (e.g. Google Pay, PhonePe, Paytm, Navi, BHIM) to complete the actual transaction.
3. Your transaction data is generated and stored locally on your device. It is uploaded only if you explicitly opt in to Cloud Backup.
4. EnterFirst is not responsible for any loss of money, failed or delayed transactions, incorrect UPI IDs, or errors that occur inside the third-party UPI app used to complete a payment.
5. You are responsible for verifying the payee, amount, and UPI ID shown inside your UPI app before authorizing any payment with your bank PIN.
6. Optional features — Cloud Backup and email notifications — require linking an email address, and can be unlinked at any time from Settings.
7. Continued use of the app after an update constitutes acceptance of any revised terms shown at that time.

“I have read and agree to the Terms & Conditions.” — this checkbox must be ticked before the Agree & Continue button becomes active on the onboarding screen."""
        ),
        ManualSection(
            6,
            "6. First-Time Setup & Onboarding",
            """Upon launching EnterFirst for the first time, an onboarding assistant guides you through:
1. Terms & Conditions: Read the summary and tick “I agree” to continue.
2. Bank Balance Calibrator: Input your current digital bank account value.
3. Cash Balance Calibrator: Input the physical cash you have on hand.
4. Optional Email Link: Link an email address for Cloud Backup and update notifications (can be skipped).
5. Database Initialization: Pressing Continue allocates the local database schema and launches your personalized Dashboard."""
        ),
        ManualSection(
            7,
            "7. The Home Screen Dashboard",
            """The primary Home Screen provides an immediate, high-fidelity summary:
• Top Tagline: Displays “Privacy first, Track next” as a reminder of data sovereignty.
• Combined Available Balance: Real-time sum of Bank and Cash baselines.
• Today & Monthly Spending Cards: Live numerical tallies of cumulative expenses.
• Category Spending Breakdown: Progress rows showing exact proportions spent per category.
• Bottom Quick-Action Buttons: Floating action buttons for Scan & Pay and Add Transaction.
• Zero Balance Celebration: Celebratory animation when balance lands exactly on zero."""
        ),
        ManualSection(
            8,
            "8. Category Spending & Visual Analytics",
            """Expenditures are organized into eight distinct categories with visual indicators:
• Food: Restaurants, cafes, deliveries, and groceries.
• Travel: Fuel, cab rides, tickets, and public transit.
• Shopping: Clothes, gear, accessories, and shopping malls.
• Education: Books, tutorials, and courses.
• Entertainment: Movies, streaming subscriptions, and games.
• Bills: Cell recharges, electricity, internet, and utilities.
• Health: Clinical visits, gym memberships, and medicines.
• Others: Miscellaneous cash payments or custom local logs."""
        ),
        ManualSection(
            9,
            "9. The Monthly Budgeting Engine",
            """Target spending limits monitor progress rows dynamically color-coded:
• Safe Zone (Below 85%): Strong cushion remaining this month.
• Warning Zone (85% – 100%): Slow down — approaching limit.
• Danger Zone (Over budget): Overdrawn — spending exceeded target.
Tap the budget card on the dashboard to adjust your target numeric limit."""
        ),
        ManualSection(
            10,
            "10. Scan & Pay Workflow (Step-by-Step)",
            """1. Tap the floating Scan & Pay button on the dashboard.
2. Grant camera permissions if prompted.
3. Align any merchant UPI QR code in the camera frame.
4. EnterFirst extracts merchant name and UPI ID. Enter amount and category.
5. Tap Pay Now. EnterFirst logs the entry and hands off to your selected UPI app."""
        ),
        ManualSection(
            11,
            "11. Manual Transactions & Cash Entries",
            """To track physical cash deposits, withdrawals, or offline spends:
1. Tap Add Transaction button.
2. Toggle between INCOME and EXPENSE.
3. Toggle Account Type between BANK or CASH.
4. Input amount with built-in calculator keypad (e.g. 120+45+30=).
5. Use OCR Bill Scan to auto-detect amounts from paper receipts."""
        ),
        ManualSection(
            12,
            "12. Advanced History, Filters & Deletions",
            """• Chromatic Colors: Green for income, Red for expenses.
• Real-Time Search: Filter by merchant name or description.
• Time Filters: Segment by Today, This Week, or This Month.
• Double-Tap to Duplicate: Double-tap any transaction card to prefill and duplicate recurring spends.
• Long-Press to Edit or Delete: Edit details or delete entries with immediate balance reversal."""
        ),
        ManualSection(
            13,
            "13. App Settings & Customization",
            """• Default UPI App Selector (Google Pay, PhonePe, Paytm, Navi, BHIM).
• Theme Mode (Dark Slate, Light, System).
• Re-calibrate Baselines (Bank and Cash).
• Reset Monthly Budget & Reset All Data.
• Cloud Backup & Email Linking (Optional, strictly opt-in).
• Terms & Conditions accessible anytime."""
        ),
        ManualSection(
            14,
            "14. System Tips & Companion Rules",
            """• Use Scan & Pay daily for contactless merchant purchases so tracking happens automatically.
• Update cash balances whenever you withdraw notes from an ATM.
• Double-tap recurring transactions (like daily tea or parking) to duplicate quickly.
• Link an email address if you want history protected against device loss."""
        ),
        ManualSection(
            15,
            "15. Difference From Traditional Expense Apps",
            """Traditional Apps vs EnterFirst:
• Track after payment vs Tracks before payment
• Requires discipline vs Natural payment workflow
• Often uses SMS access vs Privacy-first, no SMS access
• Separate payment & tracking vs Combined single-scan flow
• Users forget entries vs Real-time in-the-moment tracking"""
        ),
        ManualSection(
            16,
            "16. Frequently Asked Questions",
            """Q: Does EnterFirst need internet?
A: No, scanning, logging, and OCR work fully offline. Internet is used only by the redirected UPI app or optional Cloud Backup.

Q: Can EnterFirst see my bank balance or PIN?
A: No, EnterFirst never links to your bank and never sees your PIN.

Q: Is EnterFirst responsible if a payment fails?
A: No, EnterFirst is a redirector and logging layer only."""
        ),
        ManualSection(
            17,
            "17. Version History",
            """Current Version 2.0:
• Separate Bank and Cash baselines
• Monthly Budgeting Engine with Safe/Warning/Danger zones
• Calculator keypad and AI-assisted OCR bill scan
• URL Push default with QR Share Intent fallback
• Real-time search, double-tap duplicate, long-press actions
• Mandatory Terms & Conditions setup agreement"""
        ),
        ManualSection(
            18,
            "18. Future Roadmap",
            """• Smart Categorization using merchant names.
• Group Expense Splitting with friends before payment.
• Advanced Analytics Dashboard with savings patterns.
• Expanded Cloud Sync across multiple devices."""
        ),
        ManualSection(
            19,
            "19. Glossary of Terms",
            """• UPI: Unified Payments Interface — real-time bank transfer system.
• VPA / UPI ID: Virtual Payment Address (e.g. merchant@upi).
• QR Code: Quick Response code encoding UPI deep-link.
• Baseline: Starting balance entered during onboarding.
• OCR: Optical Character Recognition for receipts."""
        ),
        ManualSection(
            20,
            "20. Creator Information & Support",
            """S Divya Darshan
Electronics and Communication Engineering (ECE), 1st Year Student
Passionate about solving daily life struggles through creative technology and product innovation. EnterFirst was designed, built, and polished from the ground up as an independent product project.

Social Profiles & Portfolios:
• Email: sddarshan2007@gmail.com
• GitHub: github.com/sdivyadarshan
• LinkedIn: linkedin.com/in/s-divyadarshan-796076376
• Instagram: @_.daxrshnxz

“A privacy-first expense tracking layer built on top of UPI payments.” — App Philosophy, EnterFirst"""
        )
    )

    const val TERMS_SUMMARY = """EnterFirst — Terms & Conditions Summary
1. EnterFirst is not a bank, digital wallet, or payment processor. It does not hold, transmit, or process your money.
2. EnterFirst acts only as a redirector and local logging layer. It hands off your payment details to a UPI-certified app of your choice (e.g. Google Pay, PhonePe, Paytm, Navi, BHIM) to complete the actual transaction.
3. Your transaction data is generated and stored locally on your device. It is uploaded only if you explicitly opt in to Cloud Backup.
4. EnterFirst is not responsible for any loss of money, failed or delayed transactions, incorrect UPI IDs, or errors that occur inside the third-party UPI app used to complete a payment.
5. You are responsible for verifying the payee, amount, and UPI ID shown inside your UPI app before authorizing any payment with your bank PIN.
6. Optional features — Cloud Backup and email notifications — require linking an email address, and can be unlinked at any time from Settings.
7. Continued use of the app after an update constitutes acceptance of any revised terms shown at that time.

“I have read and agree to the Terms & Conditions.” — this checkbox must be ticked before the Agree & Continue button becomes active on the onboarding screen."""

    const val PRIVACY_POLICY_SUMMARY = """EnterFirst — Privacy Policy Summary
1. Zero Data Collection by Default: All transaction records, balances, budgets, and custom notes are stored locally and encrypted on your device.
2. No Financial Credentials: We never request, capture, or store bank account numbers, passwords, debit/credit cards, or UPI PINs.
3. No SMS Scraping: EnterFirst does not request READ_SMS permission and never reads your private messages.
4. Opt-in Cloud Backup: Cloud backups to Google Drive are strictly opt-in and can be disconnected at any time in Settings.
5. Hardware Access: Camera permission is used solely in real-time to scan UPI QR codes and capture receipt images for offline OCR."""

    /**
     * Generates a physical PDF document in app files and returns its FileProvider Uri.
     */
    fun generateUserManualPdfFile(context: Context): File {
        val pdfFile = File(context.filesDir, "EnterFirst_user_manual_v2.pdf")
        if (pdfFile.exists() && pdfFile.length() > 1024) {
            return pdfFile
        }

        val pdfDoc = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 40f

        val titlePaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val subtitlePaint = Paint().apply {
            color = Color.parseColor("#10B981")
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val sectionHeaderPaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val bodyPaint = Paint().apply {
            color = Color.parseColor("#334155")
            textSize = 9.5f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }

        val footerPaint = Paint().apply {
            color = Color.parseColor("#94A3B8")
            textSize = 8.5f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = pdfDoc.startPage(pageInfo)
        var canvas = page.canvas
        var yPos = margin + 20f

        // Cover / Header on Page 1
        canvas.drawText("EnterFirst — Complete User Manual v2.0", margin, yPos, titlePaint)
        yPos += 22f
        canvas.drawText("“Privacy First, Track Next” • Pre-Payment Expense Tracker", margin, yPos, subtitlePaint)
        yPos += 18f
        canvas.drawText("Author: S Divya Darshan | ECE 1st Year Student", margin, margin + 40f, footerPaint)
        yPos += 24f

        fun splitText(text: String, maxWidth: Float, paint: Paint): List<String> {
            val lines = mutableListOf<String>()
            text.split("\n").forEach { rawLine ->
                if (rawLine.isBlank()) {
                    lines.add("")
                    return@forEach
                }
                val words = rawLine.split(" ")
                var currentLine = ""
                for (word in words) {
                    val candidate = if (currentLine.isEmpty()) word else "$currentLine $word"
                    if (paint.measureText(candidate) <= maxWidth) {
                        currentLine = candidate
                    } else {
                        if (currentLine.isNotEmpty()) lines.add(currentLine)
                        currentLine = word
                    }
                }
                if (currentLine.isNotEmpty()) lines.add(currentLine)
            }
            return lines
        }

        for (section in SECTIONS) {
            val sectionLines = splitText(section.content, pageWidth - 2 * margin, bodyPaint)
            val neededHeight = 24f + (sectionLines.size * 13f) + 16f

            if (yPos + neededHeight > pageHeight - margin) {
                // Draw footer
                canvas.drawText("Page $pageNumber — EnterFirst v2.0 User Manual", margin, pageHeight - 20f, footerPaint)
                pdfDoc.finishPage(page)

                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = pdfDoc.startPage(pageInfo)
                canvas = page.canvas
                yPos = margin + 20f
            }

            canvas.drawText(section.title, margin, yPos, sectionHeaderPaint)
            yPos += 18f

            for (line in sectionLines) {
                if (yPos > pageHeight - margin - 20f) {
                    canvas.drawText("Page $pageNumber — EnterFirst v2.0 User Manual", margin, pageHeight - 20f, footerPaint)
                    pdfDoc.finishPage(page)

                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    page = pdfDoc.startPage(pageInfo)
                    canvas = page.canvas
                    yPos = margin + 20f
                }
                canvas.drawText(line, margin, yPos, bodyPaint)
                yPos += 13f
            }
            yPos += 14f
        }

        canvas.drawText("Page $pageNumber — EnterFirst v2.0 User Manual", margin, pageHeight - 20f, footerPaint)
        pdfDoc.finishPage(page)

        try {
            FileOutputStream(pdfFile).use { out ->
                pdfDoc.writeTo(out)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            pdfDoc.close()
        }

        return pdfFile
    }

    fun openOrShareUserManualPdf(context: Context) {
        if (isOnline(context)) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(USER_MANUAL_PDF_LINK)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Offline or external intent fallback: open local PDF
        try {
            val file = generateUserManualPdfFile(context)
            val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(viewIntent, "Open EnterFirst User Manual (v2.0)").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Opening in-app manual reader...", Toast.LENGTH_SHORT).show()
        }
    }
}
