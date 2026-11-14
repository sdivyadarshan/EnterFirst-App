package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.AppAnnouncement
import com.example.data.AppUser
import com.example.ui.FinanceViewModel
import com.example.ui.theme.EmeraldGreen
import com.example.util.BackgroundRestrictionHelper
import com.example.util.FixBackgroundRestrictionsDialog
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

// ------------------------------------------------------------------------------------
// 1. PAYMENT SENT PROOF OVERLAY (5-SECOND SENDER DISPLAY)
// ------------------------------------------------------------------------------------
@Composable
fun PaymentSentProofOverlay(
    merchantName: String,
    merchantUpiId: String,
    amount: Double,
    pendingTxId: Int,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var secondsRemaining by remember { mutableIntStateOf(5) }
    var isPaused by remember { mutableStateOf(false) }

    LaunchedEffect(isPaused) {
        if (!isPaused) {
            while (secondsRemaining > 0) {
                delay(1000L)
                secondsRemaining--
            }
            onDismiss()
        }
    }

    val displayDate = remember {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
    }
    val txnRef = remember(pendingTxId) {
        "TXN" + SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date()) + (if (pendingTxId > 0) pendingTxId else (1000..9999).random())
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF070B13).copy(alpha = 0.96f))
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(24.dp)
                .testTag("payment_sent_proof_overlay"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0xFF0F172A))
                    .border(2.dp, Color(0xFF10B981).copy(alpha = 0.4f), RoundedCornerShape(28.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Success Badge
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(Color(0xFF10B981).copy(alpha = 0.15f), shape = CircleShape)
                        .border(3.dp, Color(0xFF10B981), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Payment Sent",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(52.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "PAYMENT SENT",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.5.sp,
                        color = Color(0xFF10B981)
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Big amount
                Text(
                    text = "₹${String.format(Locale.getDefault(), "%,.2f", amount)}",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    modifier = Modifier.testTag("proof_amount_text")
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Sent to ${merchantName.ifEmpty { "Recipient / Merchant" }}",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFE2E8F0)
                    ),
                    textAlign = TextAlign.Center
                )

                if (merchantUpiId.isNotEmpty()) {
                    Text(
                        text = merchantUpiId,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF94A3B8)
                        ),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Receipt Breakdown Container
                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Transaction Ref", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                            Text(txnRef, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Date & Time", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                            Text(displayDate, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = Color.White)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Status", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Color(0xFF10B981), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("SUCCESSFUL", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Countdown Timer Display
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val progress = (secondsRemaining / 5f).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Color(0xFF10B981),
                        trackColor = Color(0xFF334155)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (isPaused) {
                            "Screen Held — Tap Resume or Done to close"
                        } else {
                            "Showing payment proof to sender... Auto-closing in ${secondsRemaining}s"
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (isPaused) Color(0xFFF59E0B) else Color(0xFF94A3B8),
                            fontWeight = FontWeight.Medium
                        ),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Action Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Hold / Resume Button
                    OutlinedButton(
                        onClick = { isPaused = !isPaused },
                        modifier = Modifier.weight(1f).testTag("hold_screen_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (isPaused) Color(0xFFF59E0B) else Color(0xFF475569))
                    ) {
                        Text(
                            text = if (isPaused) "Resume" else "Hold Screen",
                            color = if (isPaused) Color(0xFFF59E0B) else Color.White,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    // Done / Return Button
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).testTag("done_proof_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Text(
                            text = "Done",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Share text receipt option
                TextButton(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "✅ Payment of ₹$amount sent to ${merchantName.ifEmpty { "Recipient" }} ($merchantUpiId)\nRef: $txnRef\nDate: $displayDate\nVia EnterFirst UPI"
                            )
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Payment Proof"))
                    },
                    modifier = Modifier.testTag("share_receipt_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = "Share",
                        tint = Color(0xFF60A5FA),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Share Receipt",
                        color = Color(0xFF60A5FA),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------------------------------
// 2. OFFICIAL ADMIN UPDATE BANNER FOR DASHBOARD
// ------------------------------------------------------------------------------------
@Composable
fun OfficialAdminUpdateBanner(
    update: AppAnnouncement,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("admin_update_banner"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1B4B).copy(alpha = 0.85f) // Deep indigo
        ),
        border = BorderStroke(1.dp, Color(0xFF818CF8).copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(Color(0xFF6366F1).copy(alpha = 0.2f), shape = CircleShape)
                    .border(1.dp, Color(0xFF818CF8).copy(alpha = 0.5f), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Campaign,
                    contentDescription = null,
                    tint = Color(0xFFA5B4FC),
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Admin Update",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFA5B4FC)
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "•",
                        color = Color(0xFF6B7280)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (!update.versionCode.isNullOrBlank()) update.versionCode else "Official Release",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = EmeraldGreen,
                            fontWeight = FontWeight.SemiBold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = update.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = update.message,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFFD1D5DB)
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            val isHotFix = update.category == "HOT_FIX"
            Surface(
                color = if (isHotFix) Color(0xFF00FF66) else Color(0xFF6366F1),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.clickable {
                    if (isHotFix) {
                        com.example.ocr.OcrConfigManager.applyPendingHotFix(context)
                    } else {
                        onClick()
                    }
                }
            ) {
                Text(
                    text = if (isHotFix) "Fix" else "View",
                    color = if (isHotFix) Color.Black else Color.White,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

// ------------------------------------------------------------------------------------
// 3. APP UPDATES & ANNOUNCEMENTS SHEET / DIALOG
// ------------------------------------------------------------------------------------
@Composable
fun AppUpdatesSheet(
    announcements: List<AppAnnouncement>,
    isAdmin: Boolean = false,
    viewModel: FinanceViewModel? = null,
    onMarkRead: (Long) -> Unit,
    onMarkAllRead: () -> Unit,
    onDelete: (Long) -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var isCheckingUpdates by remember { mutableStateOf(false) }
    var showFixBackgroundDialog by remember { mutableStateOf(false) }
    var showClearAllConfirm by remember { mutableStateOf(false) }
    var isBatteryRestricted by remember { mutableStateOf(!BackgroundRestrictionHelper.isBatteryOptimizationIgnored(context)) }

    LaunchedEffect(Unit) {
        if (viewModel != null) {
            viewModel.initAppOpenSyncAndAutoBackup(context)
        }
    }

    val displayedAnnouncements = remember(announcements, isAdmin) {
        if (isAdmin) {
            announcements
        } else {
            announcements.filter { it.isCurrentlyActive() }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF070B13).copy(alpha = 0.94f))
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(16.dp)
                .testTag("app_updates_sheet"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF0F172A))
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(Color(0xFF6366F1).copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.NotificationsActive,
                                contentDescription = null,
                                tint = Color(0xFF818CF8),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "App Updates",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = if (isAdmin) "Admin Broadcast Portal & Controls" else "Official Announcements & Releases",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (isAdmin) EmeraldGreen else Color(0xFF94A3B8),
                                    fontWeight = if (isAdmin) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Background & Battery Optimization Fix Banner
                Surface(
                    color = if (isBatteryRestricted) Color(0xFF451A03) else Color(0xFF064E3B).copy(alpha = 0.6f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (isBatteryRestricted) Color(0xFFF59E0B) else EmeraldGreen.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showFixBackgroundDialog = true }
                        .testTag("fix_background_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isBatteryRestricted) Icons.Filled.Warning else Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = if (isBatteryRestricted) Color(0xFFFBBF24) else EmeraldGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isBatteryRestricted) "Fix Background & Battery Restrictions" else "Background Sync: Unrestricted & Active",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isBatteryRestricted) Color(0xFFFDE68A) else EmeraldGreen
                                    )
                                )
                                Text(
                                    text = if (isBatteryRestricted) "Tap to enable background alerts when app is closed" else "Alarms and push notifications delivery are optimal",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFFCBD5E1),
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                        Surface(
                            color = if (isBatteryRestricted) Color(0xFFF59E0B) else EmeraldGreen,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (isBatteryRestricted) "Fix" else "Settings",
                                color = Color.Black,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Sub-actions: Count, Reload button, Admin Clear-All, and Mark All Read
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${displayedAnnouncements.size} Broadcast(s)",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = Color(0xFF94A3B8),
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = {
                                if (viewModel != null && !isCheckingUpdates) {
                                    isCheckingUpdates = true
                                    try {
                                        viewModel.syncFirebaseUsers(context) { _ ->
                                            isCheckingUpdates = false
                                            Toast.makeText(
                                                context,
                                                "☁️ Synced updates from cloud server!",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    } catch (e: Exception) {
                                        isCheckingUpdates = false
                                        e.printStackTrace()
                                    }
                                }
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            if (isCheckingUpdates) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = EmeraldGreen
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = "Reload Updates",
                                    tint = EmeraldGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (isAdmin) {
                            TextButton(
                                onClick = { showClearAllConfirm = true },
                                modifier = Modifier.testTag("admin_clear_all_button")
                            ) {
                                Text(
                                    text = "Clear All (Keep Welcome)",
                                    color = Color(0xFFEF4444),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }

                        TextButton(onClick = onMarkAllRead) {
                            Text(
                                text = "Mark Read",
                                color = EmeraldGreen,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // List of Announcements
                if (displayedAnnouncements.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF475569),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "You're all caught up!",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color(0xFF94A3B8),
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            Text(
                                text = "New updates and announcements will appear here.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF64748B)
                                ),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(displayedAnnouncements, key = { it.id }) { item ->
                            val formattedDate = SimpleDateFormat("dd MMM yyyy • hh:mm a", Locale.getDefault()).format(Date(item.timestamp))
                            val now = System.currentTimeMillis()
                            val isFutureScheduled = item.scheduledAppearTime != null && item.scheduledAppearTime > now
                            val isExpired = item.scheduledDisappearTime != null && item.scheduledDisappearTime > 0 && now > item.scheduledDisappearTime

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onMarkRead(item.id) },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (!item.isRead) Color(0xFF1E293B) else Color(0xFF131D2E)
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (isFutureScheduled) Color(0xFFF59E0B).copy(alpha = 0.5f)
                                    else if (isExpired) Color(0xFFEF4444).copy(alpha = 0.4f)
                                    else if (!item.isRead) Color(0xFF38BDF8).copy(alpha = 0.5f)
                                    else Color(0xFF334155)
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Surface(
                                                color = when (item.category) {
                                                    "FEATURE" -> Color(0xFF10B981).copy(alpha = 0.2f)
                                                    "PAYMENT" -> Color(0xFF6366F1).copy(alpha = 0.2f)
                                                    "SYSTEM" -> Color(0xFFF59E0B).copy(alpha = 0.2f)
                                                    else -> Color(0xFF38BDF8).copy(alpha = 0.2f)
                                                },
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = item.category,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = when (item.category) {
                                                            "FEATURE" -> Color(0xFF34D399)
                                                            "PAYMENT" -> Color(0xFF818CF8)
                                                            "SYSTEM" -> Color(0xFFFBBF24)
                                                            else -> Color(0xFF38BDF8)
                                                        }
                                                    ),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }

                                            // Schedule badges
                                            if (isFutureScheduled) {
                                                Surface(
                                                    color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        text = "⏰ Scheduled",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFFFBBF24)
                                                        ),
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                                    )
                                                }
                                            } else if (isExpired) {
                                                Surface(
                                                    color = Color(0xFFEF4444).copy(alpha = 0.2f),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        text = "⌛ Expired",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFFF87171)
                                                        ),
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                                    )
                                                }
                                            } else if (item.scheduledDisappearTime != null && item.scheduledDisappearTime > 0) {
                                                Surface(
                                                    color = Color(0xFF38BDF8).copy(alpha = 0.15f),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    val expStr = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(item.scheduledDisappearTime))
                                                    Text(
                                                        text = "⏳ Expires $expStr",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF38BDF8)
                                                        ),
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (!item.isRead) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .background(Color(0xFF38BDF8), CircleShape)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "NEW",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = Color(0xFF38BDF8),
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                            }
                                            if (isAdmin) {
                                                IconButton(
                                                    onClick = { onDelete(item.id) },
                                                    modifier = Modifier.size(28.dp).testTag("delete_announcement_${item.id}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Delete,
                                                        contentDescription = "Delete Announcement",
                                                        tint = Color(0xFFEF4444),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = item.message,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = Color(0xFFCBD5E1),
                                            lineHeight = 20.sp
                                        )
                                    )

                                    if (!item.apkDownloadUrl.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Button(
                                            onClick = {
                                                try {
                                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.apkDownloadUrl)).apply {
                                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                    }
                                                    context.startActivity(intent)
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Cannot open download link", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Filled.Download, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (!item.versionCode.isNullOrBlank()) "Download APK (${item.versionCode})" else "Download & Install APK Update",
                                                color = Color.Black,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                    if (item.category == "HOT_FIX") {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Button(
                                            onClick = {
                                                com.example.ocr.OcrConfigManager.applyPendingHotFix(context)
                                                onDismiss()
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF66)),
                                            modifier = Modifier.fillMaxWidth().testTag("sheet_apply_fix_now_button")
                                        ) {
                                            Icon(Icons.Filled.Build, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Fix (Optimize AI Logic)",
                                                color = Color.Black,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = formattedDate,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Color(0xFF64748B)
                                            )
                                        )
                                        Text(
                                            text = if (!item.versionCode.isNullOrBlank()) "Release ${item.versionCode}" else "Official Admin Broadcast",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Color(0xFF94A3B8),
                                                fontWeight = FontWeight.Medium
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                ) {
                    Text("Close", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showFixBackgroundDialog) {
        FixBackgroundRestrictionsDialog(
            onDismiss = {
                showFixBackgroundDialog = false
                isBatteryRestricted = !BackgroundRestrictionHelper.isBatteryOptimizationIgnored(context)
            }
        )
    }

    if (showClearAllConfirm) {
        AlertDialog(
            onDismissRequest = { showClearAllConfirm = false },
            title = {
                Text("Clear All Broadcasts?", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "This will remove all broadcast announcements locally and from the cloud server.\n\nThe official 'Welcome to EnterFirst Finance & UPI' notice will be preserved.",
                    color = Color(0xFFCBD5E1)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel?.clearAllAnnouncementsExceptWelcome(context)
                        showClearAllConfirm = false
                        Toast.makeText(context, "🧹 All non-welcome notifications cleared!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Clear All (Keep Welcome)", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllConfirm = false }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }
}

// ------------------------------------------------------------------------------------
// 4. ADMIN BROADCAST COMPOSER DIALOG
// ------------------------------------------------------------------------------------
@Composable
fun AdminBroadcastDialog(
    viewModel: FinanceViewModel,
    allUsers: List<AppUser>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var versionCode by remember { mutableStateOf("") }
    var apkDownloadUrl by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("UPDATE") }
    var selectedPriority by remember { mutableStateOf("NORMAL") }

    // Optional Scheduling Time Period
    var isScheduleEnabled by remember { mutableStateOf(false) }
    var appearOffsetHours by remember { mutableIntStateOf(0) } // 0 = Immediately, 1 = +1h, 24 = +1d, 72 = +3d
    var disappearOffsetHours by remember { mutableIntStateOf(0) } // 0 = Never, 24 = 1d, 72 = 3d, 168 = 7d, 720 = 30d

    val categories = listOf("UPDATE", "HOT_FIX", "FEATURE", "SYSTEM", "PAYMENT", "NOTICE")
    val priorities = listOf("NORMAL", "HIGH", "URGENT")
    var hotFixPromptInput by remember { mutableStateOf(com.example.ocr.OcrConfigManager.getSystemPrompt(context)) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF070B13).copy(alpha = 0.95f))
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(16.dp)
                .testTag("admin_broadcast_dialog"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.94f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF0F172A))
                    .border(2.dp, EmeraldGreen.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(EmeraldGreen.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Campaign,
                                contentDescription = null,
                                tint = EmeraldGreen,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Admin Broadcast Portal",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = "Verified Administrator Session",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = EmeraldGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Target Audience summary
                var isSyncingUsers by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    viewModel.syncFirebaseUsers(context)
                }

                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.People,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Target Users: ${allUsers.size} registered",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF38BDF8)
                                    )
                                )
                            }
                            IconButton(
                                onClick = {
                                    isSyncingUsers = true
                                    viewModel.syncFirebaseUsers(context) { count ->
                                        isSyncingUsers = false
                                        Toast.makeText(context, "☁️ Synced with Firebase! Found $count users.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                if (isSyncingUsers) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = EmeraldGreen)
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.CloudSync,
                                        contentDescription = "Sync Firebase Users",
                                        tint = EmeraldGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                        if (allUsers.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(allUsers) { user ->
                                    Surface(
                                        color = Color(0xFF334155),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = user.email,
                                            style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFE2E8F0)),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        // Title Input
                        Text(
                            text = "Update Title",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            placeholder = { Text("e.g. UPI Update v2.5: Image Share Live", color = Color(0xFF64748B)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_update_title_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = EmeraldGreen,
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedContainerColor = Color(0xFF1E293B),
                                unfocusedContainerColor = Color(0xFF1E293B)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                        )
                    }

                    item {
                        // Category Selector
                        Text(
                            text = "Category",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            categories.forEach { cat ->
                                val isSelected = selectedCategory == cat
                                Surface(
                                    color = if (isSelected) EmeraldGreen else Color(0xFF1E293B),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, if (isSelected) EmeraldGreen else Color(0xFF334155)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            selectedCategory = cat
                                            if (cat == "HOT_FIX") {
                                                if (title.isBlank() || title.startsWith("UPI Update")) {
                                                    title = "Fix OCR Bug"
                                                }
                                                if (message.isBlank()) {
                                                    message = "Admin released an updated OCR prompt to optimize receipt scanning. Tap 'Fix' to apply."
                                                }
                                                selectedPriority = "URGENT"
                                            }
                                        }
                                ) {
                                    Text(
                                        text = cat,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.Black else Color(0xFF94A3B8)
                                        ),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (selectedCategory == "HOT_FIX") {
                        item {
                            Surface(
                                color = Color(0xFF0F172A),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.5.dp, Color(0xFF00FF66).copy(alpha = 0.6f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Filled.Memory,
                                            contentDescription = null,
                                            tint = Color(0xFF00FF66),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Hot-Fix OCR System Prompt (Payload)",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF00FF66)
                                            )
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "When broadcasted, regular users will receive a 'Fix OCR Bug' notification with a 'Fix' action. When they tap Fix, that time only our test UI will run on their device and patch their ocr_config.json.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF94A3B8),
                                        fontSize = 11.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = hotFixPromptInput,
                                        onValueChange = { hotFixPromptInput = it },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(140.dp)
                                            .testTag("admin_dialog_hot_fix_prompt_input"),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = Color(0xFF00FF66),
                                            unfocusedBorderColor = Color(0xFF334155),
                                            focusedContainerColor = Color(0xFF1E293B),
                                            unfocusedContainerColor = Color(0xFF1E293B)
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                                    )
                                }
                            }
                        }
                    }

                    item {
                        // Priority Selector
                        Text(
                            text = "Priority",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            priorities.forEach { prio ->
                                val isSelected = selectedPriority == prio
                                Surface(
                                    color = if (isSelected) Color(0xFF6366F1) else Color(0xFF1E293B),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, if (isSelected) Color(0xFF818CF8) else Color(0xFF334155)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedPriority = prio }
                                ) {
                                    Text(
                                        text = prio,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else Color(0xFF94A3B8)
                                        ),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }

                    item {
                        // Message Body Input
                        Text(
                            text = "Update Message to All Users",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = message,
                            onValueChange = { message = it },
                            placeholder = { Text("Write the announcement details to be delivered to all logged in users...", color = Color(0xFF64748B)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .testTag("admin_update_message_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = EmeraldGreen,
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedContainerColor = Color(0xFF1E293B),
                                unfocusedContainerColor = Color(0xFF1E293B)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            maxLines = 6
                        )
                    }

                    item {
                        // Optional Scheduling Section (Appear / Disappear Time Period)
                        Surface(
                            color = Color(0xFF1E293B),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, if (isScheduleEnabled) EmeraldGreen.copy(alpha = 0.5f) else Color(0xFF334155)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Filled.Schedule,
                                            contentDescription = null,
                                            tint = if (isScheduleEnabled) EmeraldGreen else Color(0xFF94A3B8),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "Schedule Time Period (Optional)",
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            )
                                            Text(
                                                text = "Configure when notification appears & auto-disappears",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = Color(0xFF94A3B8),
                                                    fontSize = 11.sp
                                                )
                                            )
                                        }
                                    }
                                    Switch(
                                        checked = isScheduleEnabled,
                                        onCheckedChange = { isScheduleEnabled = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = EmeraldGreen,
                                            checkedTrackColor = Color(0xFF064E3B)
                                        ),
                                        modifier = Modifier.testTag("admin_schedule_toggle")
                                    )
                                }

                                if (isScheduleEnabled) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    HorizontalDivider(color = Color(0xFF334155))
                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Appear Time Offset
                                    Text(
                                        text = "1. Appear Date & Time (Start)",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF38BDF8)
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    val appearOptions = listOf(
                                        0 to "Immediately",
                                        1 to "+1 Hour",
                                        24 to "+1 Day",
                                        72 to "+3 Days"
                                    )
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        items(appearOptions) { (hours, label) ->
                                            val isSelected = appearOffsetHours == hours
                                            Surface(
                                                color = if (isSelected) Color(0xFF38BDF8) else Color(0xFF0F172A),
                                                shape = RoundedCornerShape(8.dp),
                                                border = BorderStroke(1.dp, if (isSelected) Color(0xFF38BDF8) else Color(0xFF475569)),
                                                modifier = Modifier.clickable { appearOffsetHours = hours }
                                            ) {
                                                Text(
                                                    text = label,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isSelected) Color.Black else Color(0xFFCBD5E1)
                                                    ),
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Disappear / Expiry Time Offset
                                    Text(
                                        text = "2. Auto-Disappear / Expiry Date & Time (End)",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFBBF24)
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    val disappearOptions = listOf(
                                        0 to "Never Expires",
                                        24 to "1 Day",
                                        72 to "3 Days",
                                        168 to "1 Week",
                                        720 to "1 Month"
                                    )
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        items(disappearOptions) { (hours, label) ->
                                            val isSelected = disappearOffsetHours == hours
                                            Surface(
                                                color = if (isSelected) Color(0xFFFBBF24) else Color(0xFF0F172A),
                                                shape = RoundedCornerShape(8.dp),
                                                border = BorderStroke(1.dp, if (isSelected) Color(0xFFFBBF24) else Color(0xFF475569)),
                                                modifier = Modifier.clickable { disappearOffsetHours = hours }
                                            ) {
                                                Text(
                                                    text = label,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isSelected) Color.Black else Color(0xFFCBD5E1)
                                                    ),
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        // Optional Version Tag
                        Text(
                            text = "Release Version (Optional)",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = versionCode,
                            onValueChange = { versionCode = it },
                            placeholder = { Text("e.g. v2.1.0 or Build 104", color = Color(0xFF64748B)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_update_version_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = EmeraldGreen,
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedContainerColor = Color(0xFF1E293B),
                                unfocusedContainerColor = Color(0xFF1E293B)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }

                    item {
                        // New APK Download URL
                        Text(
                            text = "New APK Update Link (Optional)",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = apkDownloadUrl,
                            onValueChange = { apkDownloadUrl = it },
                            placeholder = { Text("https://example.com/app-release.apk or Drive link", color = Color(0xFF64748B)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_update_apk_url_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = EmeraldGreen,
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedContainerColor = Color(0xFF1E293B),
                                unfocusedContainerColor = Color(0xFF1E293B)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Actions: Broadcast in-app & Email
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            if (title.isBlank() || message.isBlank()) {
                                Toast.makeText(context, "Please enter both Title and Message", Toast.LENGTH_SHORT).show()
                            } else {
                                val now = System.currentTimeMillis()
                                val computedAppearTime = if (isScheduleEnabled && appearOffsetHours > 0) {
                                    now + (appearOffsetHours * 3600000L)
                                } else {
                                    null
                                }
                                val computedDisappearTime = if (isScheduleEnabled && disappearOffsetHours > 0) {
                                    (computedAppearTime ?: now) + (disappearOffsetHours * 3600000L)
                                } else {
                                    null
                                }

                                if (selectedCategory == "HOT_FIX" && hotFixPromptInput.isNotBlank()) {
                                    com.example.ocr.OcrConfigManager.applyHotFix(context, hotFixPromptInput)
                                }

                                viewModel.postAdminUpdate(
                                    title = title.trim(),
                                    message = message.trim(),
                                    category = selectedCategory,
                                    priority = selectedPriority,
                                    apkDownloadUrl = apkDownloadUrl.trim().takeIf { it.isNotBlank() },
                                    versionCode = versionCode.trim().takeIf { it.isNotBlank() },
                                    scheduledAppearTime = computedAppearTime,
                                    scheduledDisappearTime = computedDisappearTime,
                                    context = context
                                )
                                val scheduleNotice = if (computedAppearTime != null) " (Scheduled to appear in $appearOffsetHours hr)" else ""
                                Toast.makeText(
                                    context,
                                    "🚀 Broadcast & push notification sent to all app users!$scheduleNotice",
                                    Toast.LENGTH_LONG
                                ).show()
                                onDismiss()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("broadcast_update_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                    ) {
                        Icon(Icons.Filled.Send, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Broadcast Update to All App Users",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            if (title.isBlank() || message.isBlank()) {
                                Toast.makeText(context, "Please enter Title and Message first", Toast.LENGTH_SHORT).show()
                            } else {
                                val recipientEmails = allUsers.map { it.email }.filter { it.isNotBlank() }.distinct().toTypedArray()
                                val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:")
                                    if (recipientEmails.isNotEmpty()) {
                                        putExtra(Intent.EXTRA_EMAIL, recipientEmails)
                                    }
                                    putExtra(Intent.EXTRA_SUBJECT, "[EnterFirst Official Update] $title")
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "Official Announcement from EnterFirst Administrator:\n\n$message" +
                                        (if (apkDownloadUrl.isNotBlank()) "\n\nDownload New APK Update: $apkDownloadUrl" else "") +
                                        "\n\nCategory: $selectedCategory | Priority: $selectedPriority\nDelivered to registered EnterFirst users."
                                    )
                                }
                                try {
                                    context.startActivity(Intent.createChooser(emailIntent, "Send Email Update to Users"))
                                } catch (e: Exception) {
                                    Toast.makeText(context, "No email client found", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("email_update_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.6f))
                    ) {
                        Icon(Icons.Filled.Email, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Email Broadcast to Registered Users", color = Color(0xFF38BDF8), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
