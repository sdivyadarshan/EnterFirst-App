package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import com.example.ui.components.CalculatorKeypad
import com.example.ui.components.CalculatorAmountDisplay
import com.example.ui.components.CalculatorEvaluator
import com.example.ui.components.HotFixOptimizationOverlay
import com.example.ui.components.BatteryOptimizationDialog
import com.example.ui.components.MultiUpiQrSetupDialog
import com.example.ui.components.MultiUpiQrViewerDialog
import com.example.ui.components.ManageUpiQrsDialog
import com.example.ui.components.ElectricBlue
import com.example.ui.UpiQrCode
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import com.example.payment.UpiPayload
import com.example.payment.PaymentType
import com.example.payment.PaymentStrategyType
import com.example.payment.PaymentRouter
import com.example.payment.StrategyResult
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import android.webkit.WebView
import android.webkit.WebViewClient
import android.view.ViewGroup
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import com.example.data.AppAnnouncement
import com.example.data.AppUser
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Transaction
import com.example.ui.FinanceViewModel
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.HighDensitySuccess
import com.example.ui.theme.MyApplicationTheme
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import kotlinx.coroutines.launch
import androidx.compose.foundation.rememberScrollState
import androidx.core.app.NotificationCompat
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.compose.foundation.verticalScroll
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.compose.AsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import android.os.Build
import android.media.MediaPlayer
import android.media.AudioAttributes
import android.media.SoundPool
import android.media.ToneGenerator
import android.media.AudioManager
import com.example.util.UserManualPdfHelper
import com.example.data.LegalConsentManager
import android.util.Base64
import java.io.ByteArrayOutputStream
import org.json.JSONObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Request
import okhttp3.OkHttpClient
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// --- SMART OCR UTILITY (ML KIT ON-DEVICE + GEMINI AI) ---
data class TransactionResult(
    val type: String,
    val amount: Double,
    val party: String,
    val category: String,
    val note: String
)

fun Bitmap.toBase64(): String {
    val outputStream = ByteArrayOutputStream()
    compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
    return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
}

suspend fun extractTransactionFromImage(bitmap: Bitmap): TransactionResult? {
    return com.example.ocr.SmartReceiptOcr.processReceipt(bitmap)
}

// Categories color mapping matching the screenshots exactly
val CategoryColors = mapOf(
    "Food" to Color(0xFFFF5C5C), // Vibrant Coral Red
    "Travel" to Color(0xFF00B4D8), // Vibrant Blue
    "Shopping" to Color(0xFFEC407A), // Pink-Magenta
    "Bills" to Color(0xFFFFA000), // Vibrant Orange-Yellow
    "Health" to Color(0xFF05C492), // Radiant Green
    "Education" to Color(0xFF9C27B0), // Purple
    "Entertainment" to Color(0xFFFF4081), // Pink-Red
    "Others" to Color(0xFF5A6578) // Muted Slate-Gray
)

val CategoryEmojis = mapOf(
    "Food" to "🍔",
    "Travel" to "🚗",
    "Shopping" to "🛍️",
    "Bills" to "💡",
    "Health" to "💊",
    "Education" to "🎓",
    "Entertainment" to "🎬",
    "Others" to "📦"
)

val BalanceColorsMap = mapOf(
    "Default" to Color.Unspecified,
    "Emerald Green" to Color(0xFF05C492),
    "Sky Blue" to Color(0xFF29B6F6),
    "Coral Red" to Color(0xFFFF5252),
    "Orange" to Color(0xFFFF9800),
    "Gold" to Color(0xFFFFD700)
)

fun getCategoryColor(category: String): Color {
    val predefined = CategoryColors[category]
    if (predefined != null) return predefined
    val hash = category.hashCode()
    val colors = listOf(
        Color(0xFFFF5C5C),
        Color(0xFF00B4D8),
        Color(0xFFEC407A),
        Color(0xFFFFA000),
        Color(0xFF05C492),
        Color(0xFF9C27B0),
        Color(0xFFFF4081),
        Color(0xFF5A6578)
    )
    return colors[kotlin.math.abs(hash) % colors.size]
}

fun getCategoryEmoji(category: String, customCategories: Map<String, String>): String {
    return customCategories[category] ?: CategoryEmojis[category] ?: "📦"
}

enum class AppTab {
    Home, History, Analytics, Settings, About
}

@Composable
fun MainAppScreen(viewModel: FinanceViewModel) {
    val isPreferencesLoaded by viewModel.isPreferencesLoaded.collectAsStateWithLifecycle()
    val isSetupCompleted by viewModel.isSetupCompleted.collectAsStateWithLifecycle()
    val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()

    var hasSplashShown by rememberSaveable { mutableStateOf(false) }
    var showSplash by remember { mutableStateOf(!hasSplashShown) }
    var showAssetDownloadScreen by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // Temporary values for payment workflow and UI state
    var scannedMerchantName by remember { mutableStateOf("") }
    var scannedMerchantUpiId by remember { mutableStateOf("") }
    var scannedAmount by remember { mutableStateOf("") }
    var scannedFullUpiUri by remember { mutableStateOf("") }
    var currentScreenState by remember { mutableStateOf("TABS") } // "TABS", "ADD_TRANSACTION", "SCANNER", "PAYMENT_WORKFLOW"
    var currentTab by remember { mutableStateOf(AppTab.Home) }

    val autoBackupFrequency by viewModel.autoBackupFrequency.collectAsStateWithLifecycle()
    val lastAutoBackupTime by viewModel.lastAutoBackupTime.collectAsStateWithLifecycle()
    val googleEmail by viewModel.googleEmail.collectAsStateWithLifecycle()

    LaunchedEffect(isPreferencesLoaded, autoBackupFrequency, lastAutoBackupTime, googleEmail) {
        if (isPreferencesLoaded && !googleEmail.isNullOrEmpty() && autoBackupFrequency != "OFF") {
            val now = System.currentTimeMillis()
            val intervalMs = when (autoBackupFrequency) {
                "DAILY" -> 24 * 60 * 60 * 1000L
                "WEEKLY" -> 7 * 24 * 60 * 60 * 1000L
                "MONTHLY" -> 30 * 24 * 60 * 60 * 1000L
                else -> 24 * 60 * 60 * 1000L
            }
            if (now - lastAutoBackupTime >= intervalMs) {
                viewModel.backupToGoogleDrive(context)
            }
        }
    }

    val isBatteryDialogShown by viewModel.isBatteryDialogShown.collectAsStateWithLifecycle()
    val isUpiQrSetupDone by viewModel.isUpiQrSetupDone.collectAsStateWithLifecycle()
    val userUpiQrs by viewModel.userUpiQrs.collectAsStateWithLifecycle()

    var showBatteryOptimizationDialog by remember { mutableStateOf(false) }
    var showUpiQrSetupDialog by remember { mutableStateOf(false) }
    var showUpiQrViewerDialog by remember { mutableStateOf(false) }

    var showNotificationPermissionDialog by remember { mutableStateOf(false) }
    val notificationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            android.widget.Toast.makeText(context, "🔔 Notifications enabled for updates & alerts", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.initAppOpenSyncAndAutoBackup(context)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                showNotificationPermissionDialog = true
            }
        }
    }

    LaunchedEffect(isPreferencesLoaded, showSplash, showNotificationPermissionDialog, isBatteryDialogShown, isUpiQrSetupDone) {
        if (isPreferencesLoaded && !showSplash && !showNotificationPermissionDialog) {
            if (!isBatteryDialogShown) {
                kotlinx.coroutines.delay(350)
                showBatteryOptimizationDialog = true
            } else if (!isUpiQrSetupDone) {
                kotlinx.coroutines.delay(350)
                showUpiQrSetupDialog = true
            }
        }
    }

    val announcements by viewModel.announcements.collectAsStateWithLifecycle()
    val hotFixEvent by com.example.ocr.OcrConfigManager.hotFixTriggerEvent.collectAsStateWithLifecycle()
    val pendingHotFix by com.example.ocr.OcrConfigManager.pendingHotFix.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        if (showSplash) {
            kotlinx.coroutines.delay(1400)
            showSplash = false
            hasSplashShown = true
        }
    }

    val deepLinkPayload by FinanceViewModel.deepLinkPayload.collectAsStateWithLifecycle()
    val incomingUpiPayload by viewModel.incomingUpiPayload.collectAsStateWithLifecycle()

    LaunchedEffect(deepLinkPayload, incomingUpiPayload, isSetupCompleted) {
        val payload = deepLinkPayload ?: incomingUpiPayload
        if (payload != null && isSetupCompleted && !showSplash) {
            processQrCodeResult(
                rawValue = payload,
                context = context,
                onMerchantParsed = { name, pa, am, raw ->
                    scannedMerchantName = name
                    scannedMerchantUpiId = pa ?: ""
                    scannedAmount = am ?: ""
                    scannedFullUpiUri = raw ?: ""
                    currentScreenState = "PAYMENT_WORKFLOW"
                },
                onShowExternalDialog = { _ -> }
            )
            FinanceViewModel.setDeepLinkPayload(null)
            viewModel.setIncomingUpiPayload(null)
        }
    }

    var previousScreenState by remember { mutableStateOf("TABS") }
    var showZeroBalancePopup by remember { mutableStateOf(false) }

    val totalBalance by viewModel.currentTotalBalance.collectAsStateWithLifecycle()
    val isZeroBalanceVideoEnabled by viewModel.isZeroBalanceVideoEnabled.collectAsStateWithLifecycle()
    val customZeroBalanceVideoPath by viewModel.customZeroBalanceVideoPath.collectAsStateWithLifecycle()

    LaunchedEffect(currentScreenState) {
        if (currentScreenState == "TABS" && (previousScreenState == "ADD_TRANSACTION" || previousScreenState == "PAYMENT_WORKFLOW" || previousScreenState == "PAY_ENTRY" || previousScreenState == "EDIT_TRANSACTION")) {
            kotlinx.coroutines.delay(400)
            if (isZeroBalanceVideoEnabled && totalBalance <= 0.001) {
                showZeroBalancePopup = true
            }
        }
        previousScreenState = currentScreenState
    }

    // Transaction ID for the pending payment
    var pendingTransactionId by remember { mutableStateOf<Int?>(null) }
    var editingTransaction by remember { mutableStateOf<Transaction?>(null) }

    MyApplicationTheme(darkTheme = isDarkTheme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            if (showSplash) {
                SplashScreen()
            } else if (!isSetupCompleted) {
                SetupScreen(onSetupComplete = { bank, cash ->
                    showAssetDownloadScreen = true
                    viewModel.completeSetup(bank, cash)
                })
            } else if (showAssetDownloadScreen) {
                AssetDownloadScreen(onComplete = {
                    showAssetDownloadScreen = false
                })
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    when (currentScreenState) {
                        "TABS" -> {
                            Scaffold(
                                bottomBar = {
                                    NavigationBar(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        tonalElevation = 1.dp,
                                        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                                    ) {
                                        val navBarColors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        NavigationBarItem(
                                            selected = currentTab == AppTab.Home,
                                            onClick = { currentTab = AppTab.Home },
                                            icon = {
                                                Icon(
                                                    if (currentTab == AppTab.Home) Icons.Filled.Home else Icons.Outlined.Home,
                                                    contentDescription = "Home"
                                                )
                                            },
                                            label = { Text("Home", fontWeight = if (currentTab == AppTab.Home) FontWeight.Bold else FontWeight.Normal, fontSize = 11.sp) },
                                            colors = navBarColors
                                        )
                                        NavigationBarItem(
                                            selected = currentTab == AppTab.History,
                                            onClick = { currentTab = AppTab.History },
                                            icon = {
                                                Icon(
                                                    if (currentTab == AppTab.History) Icons.AutoMirrored.Filled.List else Icons.AutoMirrored.Outlined.List,
                                                    contentDescription = "History"
                                                )
                                            },
                                            label = { Text("History", fontWeight = if (currentTab == AppTab.History) FontWeight.Bold else FontWeight.Normal, fontSize = 11.sp) },
                                            colors = navBarColors
                                        )
                                        NavigationBarItem(
                                            selected = currentTab == AppTab.Analytics,
                                            onClick = { currentTab = AppTab.Analytics },
                                            icon = {
                                                Icon(
                                                    if (currentTab == AppTab.Analytics) Icons.Filled.PieChart else Icons.Outlined.PieChart,
                                                    contentDescription = "Analytics"
                                                )
                                            },
                                            label = { Text("Analytics", fontWeight = if (currentTab == AppTab.Analytics) FontWeight.Bold else FontWeight.Normal, fontSize = 11.sp) },
                                            colors = navBarColors
                                        )
                                        NavigationBarItem(
                                            selected = currentTab == AppTab.Settings,
                                            onClick = { currentTab = AppTab.Settings },
                                            icon = {
                                                Icon(
                                                    if (currentTab == AppTab.Settings) Icons.Filled.Settings else Icons.Outlined.Settings,
                                                    contentDescription = "Settings"
                                                )
                                            },
                                            label = { Text("Settings", fontWeight = if (currentTab == AppTab.Settings) FontWeight.Bold else FontWeight.Normal, fontSize = 11.sp) },
                                            colors = navBarColors
                                        )
                                        NavigationBarItem(
                                            selected = currentTab == AppTab.About,
                                            onClick = { currentTab = AppTab.About },
                                            icon = {
                                                Icon(
                                                    if (currentTab == AppTab.About) Icons.Filled.Person else Icons.Outlined.Person,
                                                    contentDescription = "About"
                                                )
                                            },
                                            label = { Text("About", fontWeight = if (currentTab == AppTab.About) FontWeight.Bold else FontWeight.Normal, fontSize = 11.sp) },
                                            colors = navBarColors
                                        )
                                    }
                                }
                            ) { innerPadding ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(innerPadding)
                                 ) {
                                    when (currentTab) {
                                        AppTab.Home -> DashboardScreen(
                                            viewModel = viewModel,
                                            onAddClick = { currentScreenState = "ADD_TRANSACTION" },
                                            onScanClick = { currentScreenState = "SCANNER" },
                                            onSeeAllHistory = { currentTab = AppTab.History },
                                            onSettingsClick = { currentTab = AppTab.Settings },
                                            onAnalyticsClick = { currentTab = AppTab.Analytics },
                                            onEditClick = { tx ->
                                                editingTransaction = tx
                                                currentScreenState = "EDIT_TRANSACTION"
                                            }
                                        )
                                        AppTab.History -> HistoryScreen(
                                            viewModel = viewModel,
                                            onEditClick = { tx ->
                                                editingTransaction = tx
                                                currentScreenState = "EDIT_TRANSACTION"
                                            }
                                        )
                                        AppTab.Analytics -> AnalyticsDashboardScreen(
                                            viewModel = viewModel,
                                            onBackClick = { currentTab = AppTab.Home }
                                        )
                                        AppTab.Settings -> SettingsScreen(
                                            viewModel = viewModel,
                                            onPreviewZeroBalance = { showZeroBalancePopup = true }
                                        )
                                        AppTab.About -> AboutScreen()
                                    }
                                }
                            }
                        }
                        "ADD_TRANSACTION" -> {
                            AddTransactionScreen(
                                viewModel = viewModel,
                                onDismiss = { currentScreenState = "TABS" },
                                prefilledMerchantName = "",
                                prefilledMerchantUpi = "",
                                prefilledAmount = ""
                            )
                        }
                        "SCANNER" -> {
                            ScannerScreen(
                                onDismiss = { currentScreenState = "TABS" },
                                onMerchantParsed = { name, upi, amt, raw ->
                                    scannedMerchantName = name
                                    scannedMerchantUpiId = upi
                                    scannedAmount = amt ?: ""
                                    scannedFullUpiUri = raw ?: ""
                                    currentScreenState = "PAY_ENTRY"
                                }
                            )
                        }
                        "PAY_ENTRY" -> {
                            // User scanned QR, now entering amount + details for the pending transaction
                            AddTransactionScreen(
                                viewModel = viewModel,
                                onDismiss = { currentScreenState = "TABS" },
                                prefilledMerchantName = scannedMerchantName,
                                prefilledMerchantUpi = scannedMerchantUpiId,
                                prefilledAmount = scannedAmount,
                                isUpiPaymentFlow = true,
                                onPendingCreated = { txId, amt ->
                                    pendingTransactionId = txId
                                    scannedAmount = amt
                                    currentScreenState = "PAYMENT_WORKFLOW"
                                }
                            )
                        }
                        "PAYMENT_WORKFLOW" -> {
                            PaymentWorkflowScreen(
                                viewModel = viewModel,
                                pendingTxId = pendingTransactionId ?: 0,
                                merchantName = scannedMerchantName,
                                merchantUpiId = scannedMerchantUpiId,
                                amount = scannedAmount.toDoubleOrNull() ?: 0.0,
                                originalUpiUri = scannedFullUpiUri,
                                onComplete = {
                                    currentScreenState = "TABS"
                                }
                            )
                        }
                        "EDIT_TRANSACTION" -> {
                            editingTransaction?.let { tx ->
                                EditTransactionScreen(
                                    viewModel = viewModel,
                                    transaction = tx,
                                    onDismiss = {
                                        currentScreenState = "TABS"
                                        editingTransaction = null
                                    }
                                )
                            }
                        }
                    }

                    if (showZeroBalancePopup) {
                        ZeroBalanceVideoPopup(
                            customVideoPath = customZeroBalanceVideoPath,
                            onDismiss = { showZeroBalancePopup = false }
                        )
                    }

                    // Full-screen dynamic 3-second UI/UX animation triggered when hot-fix is applied
                    hotFixEvent?.let { event ->
                        HotFixOptimizationOverlay(
                            event = event,
                            onDismiss = {
                                com.example.ocr.OcrConfigManager.clearHotFixEvent()
                            }
                        )
                    }

                    if (showNotificationPermissionDialog) {
                        AlertDialog(
                            onDismissRequest = { showNotificationPermissionDialog = false },
                            icon = {
                                Icon(
                                    imageVector = Icons.Filled.NotificationsActive,
                                    contentDescription = null,
                                    tint = EmeraldGreen,
                                    modifier = Modifier.size(32.dp)
                                )
                            },
                            title = {
                                Text(
                                    text = "Enable Updates & Notifications",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                            },
                            text = {
                                Text(
                                    text = "Allow notification access so you instantly receive official administrator updates, new APK release downloads, and payment receipts.",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFCBD5E1))
                                )
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        showNotificationPermissionDialog = false
                                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                                ) {
                                    Text("Allow Notifications", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showNotificationPermissionDialog = false }) {
                                    Text("Later", color = Color(0xFF94A3B8))
                                }
                            },
                            containerColor = Color(0xFF0F172A)
                        )
                    }

                    if (showBatteryOptimizationDialog) {
                        BatteryOptimizationDialog(
                            onDismiss = {
                                showBatteryOptimizationDialog = false
                                viewModel.setBatteryDialogShown()
                            },
                            onAllowClicked = {
                                showBatteryOptimizationDialog = false
                                viewModel.setBatteryDialogShown()
                            }
                        )
                    }

                    if (showUpiQrSetupDialog) {
                        MultiUpiQrSetupDialog(
                            initialQrs = userUpiQrs,
                            onDismiss = {
                                showUpiQrSetupDialog = false
                                viewModel.setUpiQrSetupDone()
                            },
                            onSaveAndAnimateToLogo = { savedQrs ->
                                showUpiQrSetupDialog = false
                                viewModel.saveUpiQrCodes(savedQrs)
                                if (savedQrs.isNotEmpty()) {
                                    Toast.makeText(
                                        context,
                                        "👉 Touch top-left app logo anytime to view your UPI QRs!",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        )
                    }

                    if (showUpiQrViewerDialog) {
                        MultiUpiQrViewerDialog(
                            upiQrs = userUpiQrs,
                            onDismiss = { showUpiQrViewerDialog = false },
                            onEditClicked = {
                                showUpiQrViewerDialog = false
                                showUpiQrSetupDialog = true
                            }
                        )
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------------------------
// ------------------------------------------------------------------------------------
// 1. SETUP SCREEN (WITH LEGALLY COMPLIANT TERMS ACCEPTANCE & AUDIT PROOFS)
// ------------------------------------------------------------------------------------
@Composable
fun SetupScreen(onSetupComplete: (Double, Double) -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var bankInput by remember { mutableStateOf("") }
    var cashInput by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf("") }
    var termsAccepted by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp)
            .windowInsetsPadding(WindowInsets.statusBars),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Logo Icon Drawing
        AppLogoGraphic(
            modifier = Modifier
                .size(110.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Welcome to EnterFirst",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = "Privacy First, Track Next",
            style = MaterialTheme.typography.bodyMedium,
            color = EmeraldGreen,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Set your starting balances to begin local tracking.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = bankInput,
            onValueChange = { bankInput = it },
            label = { Text("Starting Bank Balance (₹)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("setup_bank_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = EmeraldGreen,
                focusedLabelColor = EmeraldGreen
            )
        )

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = cashInput,
            onValueChange = { cashInput = it },
            label = { Text("Starting Cash Balance (₹)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("setup_cash_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = EmeraldGreen,
                focusedLabelColor = EmeraldGreen
            )
        )

        if (errorText.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = errorText, color = CrimsonRed, style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Legally Compliant Terms & Conditions + Privacy Policy Consent Box
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            border = BorderStroke(
                1.dp,
                if (termsAccepted) EmeraldGreen else MaterialTheme.colorScheme.outlineVariant
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("terms_acceptance_box")
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = termsAccepted,
                        onCheckedChange = { termsAccepted = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = EmeraldGreen,
                            checkmarkColor = Color.Black
                        ),
                        modifier = Modifier.testTag("terms_checkbox")
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "I agree to the ",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            Text(
                                text = "[Terms & Conditions]",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = EmeraldGreen,
                                    fontWeight = FontWeight.Bold,
                                    textDecoration = TextDecoration.Underline
                                ),
                                modifier = Modifier
                                    .clickable { showTermsDialog = true }
                                    .testTag("terms_link")
                            )
                            Text(
                                text = " and ",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            Text(
                                text = "[Privacy Policy]",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF38BDF8),
                                    fontWeight = FontWeight.Bold,
                                    textDecoration = TextDecoration.Underline
                                ),
                                modifier = Modifier
                                    .clickable { showPrivacyDialog = true }
                                    .testTag("privacy_link")
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "“I have read and agree to the Terms & Conditions.” — this checkbox must be ticked before the Agree & Continue button becomes active on the onboarding screen.",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    ),
                    modifier = Modifier.padding(start = 6.dp, end = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Agree & Continue Button (Disabled by default, dynamically enabled ONLY when checkbox ticked)
        Button(
            onClick = {
                val bank = if (bankInput.trim().isEmpty()) 0.0 else bankInput.toDoubleOrNull()
                val cash = if (cashInput.trim().isEmpty()) 0.0 else cashInput.toDoubleOrNull()
                if (bank == null || cash == null || bank < 0 || cash < 0) {
                    errorText = "Please enter valid positive amounts."
                } else if (!termsAccepted) {
                    errorText = "You must agree to the Terms & Conditions to proceed."
                } else {
                    isSubmitting = true
                    coroutineScope.launch {
                        // Persist legal consent proof in Cloud Firestore 'legal_consent_proofs'
                        try {
                            LegalConsentManager.recordConsentProof(context)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        onSetupComplete(bank, cash)
                    }
                }
            },
            enabled = termsAccepted && !isSubmitting,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("setup_complete_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = EmeraldGreen,
                disabledContainerColor = Color(0xFF334155).copy(alpha = 0.45f),
                disabledContentColor = Color(0xFF94A3B8)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.Black,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Registering Consent...",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            } else {
                Text(
                    "Agree & Continue",
                    color = if (termsAccepted) Color.Black else Color(0xFF94A3B8),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showTermsDialog) {
        TermsAndConditionsDialog(
            onDismiss = { showTermsDialog = false },
            onAgree = {
                termsAccepted = true
                showTermsDialog = false
            }
        )
    }

    if (showPrivacyDialog) {
        PrivacyPolicyDialog(
            onDismiss = { showPrivacyDialog = false }
        )
    }
}

// ------------------------------------------------------------------------------------
// TERMS & CONDITIONS SUMMARY POPUP DIALOG
// ------------------------------------------------------------------------------------
@Composable
fun TermsAndConditionsDialog(
    onDismiss: () -> Unit,
    onAgree: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.Gavel,
                contentDescription = null,
                tint = EmeraldGreen,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = "EnterFirst — Terms & Conditions Summary",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Surface(
                    color = EmeraldGreen.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Legal Agreement & Scope of Service",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = EmeraldGreen
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                val termsList = listOf(
                    "1. EnterFirst is not a bank, digital wallet, or payment processor. It does not hold, transmit, or process your money.",
                    "2. EnterFirst acts only as a redirector and local logging layer. It hands off your payment details to a UPI-certified app of your choice (e.g. Google Pay, PhonePe, Paytm, Navi, BHIM) to complete the actual transaction.",
                    "3. Your transaction data is generated and stored locally on your device. It is uploaded only if you explicitly opt in to Cloud Backup.",
                    "4. EnterFirst is not responsible for any loss of money, failed or delayed transactions, incorrect UPI IDs, or errors that occur inside the third-party UPI app used to complete a payment.",
                    "5. You are responsible for verifying the payee, amount, and UPI ID shown inside your UPI app before authorizing any payment with your bank PIN.",
                    "6. Optional features — Cloud Backup and email notifications — require linking an email address, and can be unlinked at any time from Settings.",
                    "7. Continued use of the app after an update constitutes acceptance of any revised terms shown at that time."
                )

                termsList.forEach { term ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = term,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 18.sp
                            ),
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "“I have read and agree to the Terms & Conditions.” — this checkbox must be ticked before the Agree & Continue button becomes active on the onboarding screen.",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        },
        confirmButton = {
            if (onAgree != null) {
                Button(
                    onClick = onAgree,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                ) {
                    Text("I AGREE", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("CLOSE", color = EmeraldGreen, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            if (onAgree != null) {
                TextButton(onClick = onDismiss) {
                    Text("CLOSE")
                }
            }
        }
    )
}

// ------------------------------------------------------------------------------------
// PRIVACY POLICY POPUP DIALOG
// ------------------------------------------------------------------------------------
@Composable
fun PrivacyPolicyDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.Security,
                contentDescription = null,
                tint = Color(0xFF38BDF8),
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = "EnterFirst — Privacy Policy Summary",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = UserManualPdfHelper.PRIVACY_POLICY_SUMMARY,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 18.sp
                    )
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("UNDERSTOOD", color = EmeraldGreen, fontWeight = FontWeight.Bold)
            }
        }
    )
}

// ------------------------------------------------------------------------------------
// FULL IN-APP USER MANUAL DIALOG (v2.0)
// ------------------------------------------------------------------------------------
@Composable
fun UserManualDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }

    val filteredSections = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            UserManualPdfHelper.SECTIONS
        } else {
            UserManualPdfHelper.SECTIONS.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.content.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header with Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "EnterFirst User Manual v2.0",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "“Privacy First, Track Next” • S Divya Darshan",
                            style = MaterialTheme.typography.bodySmall.copy(color = EmeraldGreen, fontWeight = FontWeight.Medium)
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Push / Open PDF Button (Direct Drive Link: https://drive.google.com/file/d/13o5_uCBUAiw0ADhfILUDGLHrZGyi_drk/view?usp=drivesdk)
                Button(
                    onClick = {
                        UserManualPdfHelper.openOrShareUserManualPdf(context)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("push_pdf_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PictureAsPdf,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Push / Open EnterFirst_user_manual_v2 (PDF)",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search 20 manual sections...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldGreen,
                        cursorColor = EmeraldGreen
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Sections List
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredSections) { sec ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = sec.title,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldGreen
                                    )
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = sec.content,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurface,
                                        lineHeight = 18.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
// ------------------------------------------------------------------------------------
// 2. DASHBOARD / HOME SCREEN
// ------------------------------------------------------------------------------------
@Composable
fun DashboardScreen(
    viewModel: FinanceViewModel,
    onAddClick: () -> Unit,
    onScanClick: () -> Unit,
    onSeeAllHistory: () -> Unit,
    onSettingsClick: () -> Unit,
    onAnalyticsClick: () -> Unit = {},
    onEditClick: (Transaction) -> Unit
) {
    val currentBank by viewModel.currentBankBalance.collectAsStateWithLifecycle()
    val currentCash by viewModel.currentCashBalance.collectAsStateWithLifecycle()
    val totalBalance by viewModel.currentTotalBalance.collectAsStateWithLifecycle()
    val todaySpending by viewModel.todaySpending.collectAsStateWithLifecycle()
    val monthlySpending by viewModel.monthlySpending.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val balanceColorName by viewModel.balanceColorName.collectAsStateWithLifecycle()
    val customCategories by viewModel.customCategories.collectAsStateWithLifecycle()
    val monthlyBudgetLimit by viewModel.monthlyBudgetLimit.collectAsStateWithLifecycle()
    var showBudgetDialog by remember { mutableStateOf(false) }

    val announcements by viewModel.announcements.collectAsStateWithLifecycle()
    val unreadAnnouncementsCount by viewModel.unreadAnnouncementsCount.collectAsStateWithLifecycle()
    val pendingHotFix by com.example.ocr.OcrConfigManager.pendingHotFix.collectAsStateWithLifecycle()
    var showUpdatesSheet by remember { mutableStateOf(false) }

    val userUpiQrs by viewModel.userUpiQrs.collectAsStateWithLifecycle()
    var showUpiQrSetupDialog by remember { mutableStateOf(false) }
    var showUpiQrViewerDialog by remember { mutableStateOf(false) }
    var showManageQrsDialogInDashboard by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var animatedBalance by remember { mutableStateOf<Double?>(null) }
    var animationJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    var showDeleteDialogFor by remember { mutableStateOf<Transaction?>(null) }
    var showTransactionOptionsFor by remember { mutableStateOf<Transaction?>(null) }
    var showDuplicateConfirmFor by remember { mutableStateOf<Transaction?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Header matching the screenshots perfectly
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            if (userUpiQrs.isNotEmpty()) {
                                showUpiQrViewerDialog = true
                            } else {
                                showUpiQrSetupDialog = true
                            }
                        }
                        .padding(vertical = 4.dp, horizontal = 4.dp)
                ) {
                    Box {
                        AppLogoGraphic(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        if (userUpiQrs.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 4.dp, y = (-4).dp)
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldGreen),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.QrCode2,
                                    contentDescription = "My QRs",
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Privacy first, Track next",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Normal
                            )
                            if (userUpiQrs.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "• My QRs",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "EnterFirst",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Announcements / Updates Bell Button
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                            .clickable { showUpdatesSheet = true }
                            .testTag("updates_bell_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (unreadAnnouncementsCount > 0) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                            contentDescription = "Updates & Announcements",
                            tint = if (unreadAnnouncementsCount > 0) EmeraldGreen else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(22.dp)
                        )
                        if (unreadAnnouncementsCount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(16.dp)
                                    .background(Color(0xFFEF4444), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$unreadAnnouncementsCount",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Settings Button
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                            .clickable { onSettingsClick() }
                            .testTag("settings_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        // Official Admin Update Banner
        if (announcements.isNotEmpty()) {
            item {
                val latestAnnouncement = announcements.firstOrNull { !it.isRead } ?: announcements.first()
                OfficialAdminUpdateBanner(
                    update = latestAnnouncement,
                    onClick = { showUpdatesSheet = true }
                )
            }
        }

        // In-App OCR Bug Fix Banner (Visible when admin sent a fix and user hasn't clicked 'Fix Now' yet)
        if (pendingHotFix != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .testTag("pending_ocr_bug_fix_banner"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    border = BorderStroke(1.5.dp, Color(0xFF00FF66))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFF00FF66).copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Memory,
                                    contentDescription = null,
                                    tint = Color(0xFF00FF66),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Fix OCR Bug",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Admin released an updated OCR prompt (v${pendingHotFix?.version}). Tap 'Fix' to apply.",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                com.example.ocr.OcrConfigManager.applyPendingHotFix(context)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF66)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("apply_pending_hot_fix_button")
                        ) {
                            Text("Fix", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        item {
            // Main Balance Card matching screenshots exactly with clean layout and emerald gradient
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(totalBalance) {
                        detectTapGestures(
                            onDoubleTap = {
                                animationJob?.cancel()
                                animationJob = coroutineScope.launch {
                                    val target = totalBalance
                                    val durationMs = 2700L
                                    val startTime = System.currentTimeMillis()
                                    playSoundEffect(context)
                                    while (System.currentTimeMillis() - startTime < durationMs) {
                                        val elapsed = System.currentTimeMillis() - startTime
                                        val fraction = elapsed.toDouble() / durationMs
                                        animatedBalance = target * fraction
                                        kotlinx.coroutines.delay(16) // ~60fps
                                    }
                                    animatedBalance = target
                                    kotlinx.coroutines.delay(1000)
                                    animatedBalance = null
                                }
                            }
                        )
                    },
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0xFF05C492), Color(0xFF00966C))
                            )
                        )
                        .padding(24.dp)
                ) {
                    Text(
                        "CURRENT BALANCE",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.75f),
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "₹${formatAmount(animatedBalance ?: totalBalance)}",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        fontFamily = FontFamily.SansSerif,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Bank",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.75f),
                                fontWeight = FontWeight.Normal
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "₹${formatAmount(currentBank)}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Box(
                            modifier = Modifier
                                .height(36.dp)
                                .width(1.dp)
                                .background(Color.White.copy(alpha = 0.25f))
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 24.dp)
                        ) {
                            Text(
                                "Cash",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.75f),
                                fontWeight = FontWeight.Normal
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "₹${formatAmount(currentCash)}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        item {
            // Today's Spend vs Month Spend matching the dark premium theme exactly (no borders, matching surface container)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "TODAY",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "₹${formatAmount(todaySpending)}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = CrimsonRed
                        )
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "THIS MONTH",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "₹${formatAmount(monthlySpending)}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = CrimsonRed
                        )
                    }
                }
            }
        }

        item {
            // Monthly Budget setting and progress card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showBudgetDialog = true }
                    .testTag("monthly_budget_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = "Monthly Budget Icon",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "MONTHLY BUDGET",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                        IconButton(
                            onClick = { showBudgetDialog = true },
                            modifier = Modifier.size(24.dp).testTag("edit_budget_icon")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = "Edit Budget",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (monthlyBudgetLimit == 0.0) {
                        Text(
                            "No budget set for this month. Tap here to set a target spending limit!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    } else {
                        val progressFraction = if (monthlyBudgetLimit > 0) (monthlySpending / monthlyBudgetLimit).coerceIn(0.0, 1.0).toFloat() else 0f
                        val progressPercentage = (progressFraction * 100).toInt()
                        val isOverBudget = monthlySpending > monthlyBudgetLimit

                        val progressColor = when {
                            isOverBudget -> CrimsonRed
                            progressFraction > 0.85f -> Color(0xFFFFB300) // Amber / Warning
                            else -> EmeraldGreen
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                Text(
                                    "Spent: ₹${formatAmount(monthlySpending)} of ₹${formatAmount(monthlyBudgetLimit)}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    if (isOverBudget) "Over budget by ₹${formatAmount(monthlySpending - monthlyBudgetLimit)}!" else "${100 - progressPercentage}% of budget remaining",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isOverBudget) CrimsonRed else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                "$progressPercentage%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = progressColor
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        LinearProgressIndicator(
                            progress = { progressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = progressColor,
                            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                        )
                    }
                }
            }
        }

        // Category Statistics list (with beautiful, clean bar rows and no redundant pie chart)
        item {
            val spendingTxs = transactions.filter { it.type == "EXPENSE" && it.paymentStatus == "PAID" }
            val categorySums = spendingTxs.groupBy { it.category }.mapValues { (_, txs) -> txs.sumOf { it.amount } }
            val maxCategorySum = categorySums.values.maxOrNull() ?: 1.0

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Spending by Category",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        TextButton(
                            onClick = onAnalyticsClick,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Filled.AutoGraph, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Recharts View", color = EmeraldGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (spendingTxs.isEmpty()) {
                        Text(
                            "No expenses recorded yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    } else {
                        // Category progress rows
                        categorySums.entries.sortedByDescending { it.value }.forEach { (cat, amt) ->
                            val progress = (amt / maxCategorySum).toFloat()
                            val color = getCategoryColor(cat)
                            val emoji = getCategoryEmoji(cat, customCategories)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = emoji,
                                    fontSize = 22.sp,
                                    modifier = Modifier.width(36.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = cat,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "₹${formatAmount(amt)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LinearProgressIndicator(
                                        progress = { progress },
                                        color = color,
                                        trackColor = MaterialTheme.colorScheme.background,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(CircleShape)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Recent Transactions Section in High Density Theme
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "RECENT ACTIVITY",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
                TextButton(onClick = onSeeAllHistory) {
                    Text(
                        "View All",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        val recentTxs = transactions.take(5)
        if (recentTxs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No transactions yet.",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            }
        } else {
            items(recentTxs) { tx ->
                TransactionRow(
                    transaction = tx,
                    customCategories = customCategories,
                    onLongClick = { showTransactionOptionsFor = tx },
                    onDoubleTap = { showDuplicateConfirmFor = tx }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp)) // Padding for floating CTA buttons
        }
    }

    // Floating actions
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 16.dp, end = 16.dp, start = 16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onAddClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("add_manual_button"),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Icon(
                    Icons.Filled.Add, 
                    contentDescription = "Manual Entry", 
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "Add Manual", 
                    color = MaterialTheme.colorScheme.onPrimaryContainer, 
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            Button(
                onClick = onScanClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .weight(1.3f)
                    .height(56.dp)
                    .testTag("scan_pay_button"),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Icon(
                    Icons.Filled.QrCodeScanner, 
                    contentDescription = "Scan & Pay", 
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "Scan & Pay", 
                    color = MaterialTheme.colorScheme.onPrimary, 
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }

    // Transaction Options dialog
    showTransactionOptionsFor?.let { tx ->
        AlertDialog(
            onDismissRequest = { showTransactionOptionsFor = null },
            title = { Text("Transaction Options", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Select an action for this transaction:", style = MaterialTheme.typography.bodyMedium)
                    
                    if (tx.paymentStatus == "PENDING" || tx.paymentStatus == "FAILED") {
                        Button(
                            onClick = {
                                viewModel.updateTransactionStatus(tx.id, "PAID")
                                showTransactionOptionsFor = null
                                Toast.makeText(context, "Status updated to PAID (Successful)", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Mark as Successful (PAID)", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = {
                            onEditClick(tx)
                            showTransactionOptionsFor = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Edit Details / Type", color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            showDeleteDialogFor = tx
                            showTransactionOptionsFor = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Delete Transaction", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTransactionOptionsFor = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Confirmation dialog
    showDeleteDialogFor?.let { tx ->
        AlertDialog(
            onDismissRequest = { showDeleteDialogFor = null },
            title = { Text("Delete Transaction") },
            text = { Text("Remove \"${tx.party}\" (₹${formatAmount(tx.amount)})?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTransaction(tx)
                        showDeleteDialogFor = null
                    },
                    modifier = Modifier.testTag("delete_confirm_button")
                ) {
                    Text("DELETE", color = CrimsonRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialogFor = null }) {
                    Text("CANCEL")
                }
            }
        )
    }

    // Duplicate Confirmation dialog
    showDuplicateConfirmFor?.let { tx ->
        AlertDialog(
            onDismissRequest = { showDuplicateConfirmFor = null },
            title = { Text("Duplicate Transaction") },
            text = { Text("Create a new duplicate transaction for \"${tx.party}\" (₹${formatAmount(tx.amount)})?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.addTransaction(
                            type = tx.type,
                            amount = tx.amount,
                            party = tx.party,
                            category = tx.category,
                            note = tx.note,
                            paymentMethod = tx.paymentMethod,
                            paymentStatus = tx.paymentStatus,
                            merchantUpiId = tx.merchantUpiId,
                            receiptUri = tx.receiptUri
                        )
                        showDuplicateConfirmFor = null
                        Toast.makeText(context, "Transaction duplicated successfully!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("duplicate_confirm_button")
                ) {
                    Text("DUPLICATE", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDuplicateConfirmFor = null }) {
                    Text("CANCEL")
                }
            }
        )
    }

    if (showBudgetDialog) {
        var budgetInput by remember { mutableStateOf(if (monthlyBudgetLimit > 0) monthlyBudgetLimit.toInt().toString() else "") }
        var inputError by remember { mutableStateOf<String?>(null) }
        
        AlertDialog(
            onDismissRequest = { showBudgetDialog = false },
            title = { Text("Set Monthly Budget", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Define your target spending limit for the month. We'll show your progress against your total expenses.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = budgetInput,
                        onValueChange = {
                            budgetInput = it
                            inputError = null
                        },
                        label = { Text("Budget Limit (₹)") },
                        placeholder = { Text("e.g. 15000") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                            imeAction = androidx.compose.ui.text.input.ImeAction.Done
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("budget_limit_input"),
                        isError = inputError != null
                    )
                    if (inputError != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = inputError ?: "",
                            color = CrimsonRed,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val limit = budgetInput.toDoubleOrNull()
                        if (limit == null || limit <= 0) {
                            inputError = "Please enter a valid positive number"
                        } else {
                            viewModel.setMonthlyBudgetLimit(limit)
                            showBudgetDialog = false
                        }
                    },
                    modifier = Modifier.testTag("save_budget_button")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBudgetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showUpdatesSheet) {
        val googleEmailState by viewModel.googleEmail.collectAsStateWithLifecycle()
        val isAdminState by viewModel.isAdmin.collectAsStateWithLifecycle()
        AppUpdatesSheet(
            announcements = announcements,
            isAdmin = (FinanceViewModel.isUserAdmin(googleEmailState) || isAdminState),
            viewModel = viewModel,
            onMarkRead = { viewModel.markAnnouncementRead(it) },
            onMarkAllRead = { viewModel.markAllAnnouncementsRead() },
            onDelete = { viewModel.deleteAnnouncement(it, context) },
            onDismiss = { showUpdatesSheet = false }
        )
    }

    if (showUpiQrSetupDialog) {
        MultiUpiQrSetupDialog(
            initialQrs = userUpiQrs,
            onDismiss = {
                showUpiQrSetupDialog = false
                viewModel.setUpiQrSetupDone()
            },
            onSaveAndAnimateToLogo = { savedQrs ->
                showUpiQrSetupDialog = false
                viewModel.saveUpiQrCodes(savedQrs)
                if (savedQrs.isNotEmpty()) {
                    Toast.makeText(
                        context,
                        "👉 Touch top-left app logo anytime to view your UPI QRs!",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        )
    }

    if (showUpiQrViewerDialog) {
        MultiUpiQrViewerDialog(
            upiQrs = userUpiQrs,
            onDismiss = { showUpiQrViewerDialog = false },
            onEditClicked = {
                showUpiQrViewerDialog = false
                showManageQrsDialogInDashboard = true
            }
        )
    }

    if (showManageQrsDialogInDashboard) {
        ManageUpiQrsDialog(
            upiQrs = userUpiQrs,
            onDismiss = { showManageQrsDialogInDashboard = false },
            onMoveUp = { viewModel.moveUpiQrUp(it) },
            onMoveDown = { viewModel.moveUpiQrDown(it) },
            onDelete = { viewModel.deleteUpiQrCode(it) },
            onUpdate = { id, appName, imgPath -> viewModel.updateUpiQr(id, appName, imgPath) },
            onAddNewQr = { appName, imgPath ->
                val newList = userUpiQrs + UpiQrCode(appName = appName, imagePath = imgPath)
                viewModel.saveUpiQrCodes(newList)
            }
        )
    }
}

// ------------------------------------------------------------------------------------
// TRANSACTION ROW
// ------------------------------------------------------------------------------------
@Composable
fun TransactionRow(
    transaction: Transaction,
    customCategories: Map<String, String> = emptyMap(),
    onLongClick: () -> Unit,
    onDoubleTap: () -> Unit
) {
    val dateStr = formatTimestamp(transaction.timestamp)
    val color = getCategoryColor(transaction.category)
    val emoji = getCategoryEmoji(transaction.category, customCategories)
    val context = LocalContext.current

    var showReceiptDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(transaction.id) {
                detectTapGestures(
                    onDoubleTap = {
                        SoundEffectManager.playDoubleTapSound()
                        onDoubleTap()
                    },
                    onLongPress = { onLongClick() },
                    onTap = {
                        if (transaction.receiptUri != null) {
                            showReceiptDialog = true
                        } else {
                            onLongClick()
                        }
                    }
                )
            },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // High Density category circular pastel background
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        transaction.party,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (transaction.receiptUri != null) {
                        Icon(
                            Icons.Filled.Receipt,
                            contentDescription = "Receipt Attached",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(14.dp)
                                .padding(start = 4.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    "${transaction.category} • $dateStr",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                val isExpense = transaction.type == "EXPENSE"
                val prefix = if (isExpense) "-" else "+"
                val valueColor = if (isExpense) MaterialTheme.colorScheme.error else HighDensitySuccess
                Text(
                    "$prefix₹${formatAmount(transaction.amount)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = valueColor
                )
                Text(
                    transaction.paymentStatus.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (transaction.paymentStatus == "PAID") HighDensitySuccess else if (transaction.paymentStatus == "PENDING") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            }
        }
    }

    if (showReceiptDialog && transaction.receiptUri != null) {
        Dialog(onDismissRequest = { showReceiptDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Attached Receipt",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    val bitmap = loadBitmapFromPath(context, transaction.receiptUri)
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Attached Receipt Photo",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    } else {
                        Text("Failed to load receipt photo.")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(onClick = { showReceiptDialog = false }) {
                        Text("Close", color = EmeraldGreen, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------------------------
// 3. HISTORY SCREEN WITH FILTERS & GRAPHS
// ------------------------------------------------------------------------------------
@Composable
fun HistoryScreen(
    viewModel: FinanceViewModel,
    onEditClick: (Transaction) -> Unit
) {
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val categoryOrder by viewModel.categoryOrder.collectAsStateWithLifecycle()
    val customCategories by viewModel.customCategories.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }
    var selectedTimeFilter by remember { mutableStateOf("All") } // "All", "Today", "Week", "Month"
    var selectedCategoryFilter by remember { mutableStateOf("All") } // "All", "Food", "Travel", etc.
    var showDeleteDialogFor by remember { mutableStateOf<Transaction?>(null) }
    var showTransactionOptionsFor by remember { mutableStateOf<Transaction?>(null) }
    var showDuplicateConfirmFor by remember { mutableStateOf<Transaction?>(null) }
    var showCompareDialog by remember { mutableStateOf(false) }

    // Filter transaction logic including search query on party & notes
    val filteredTransactions = transactions.filter { tx ->
        val timeMatch = when (selectedTimeFilter) {
            "Today" -> isSameDay(tx.timestamp, System.currentTimeMillis())
            "Week" -> isWithinDays(tx.timestamp, 7)
            "Month" -> isWithinDays(tx.timestamp, 30)
            else -> true
        }

        val categoryMatch = if (selectedCategoryFilter == "All") {
            true
        } else {
            tx.category == selectedCategoryFilter
        }

        val searchMatch = if (searchQuery.isBlank()) {
            true
        } else {
            tx.party.contains(searchQuery, ignoreCase = true) ||
            tx.note.contains(searchQuery, ignoreCase = true)
        }

        timeMatch && categoryMatch && searchMatch
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppLogoGraphic(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "History",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = { showCompareDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.BarChart,
                    contentDescription = "Compare Parameters",
                    tint = EmeraldGreen,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "Compare",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Search by paid to, received from, notes...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Clear Search")
                    }
                }
            },
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = EmeraldGreen,
                focusedLabelColor = EmeraldGreen,
                cursorColor = EmeraldGreen
            )
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Time Filters row
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val filters = listOf("All", "Today", "Week", "Month")
            items(filters) { f ->
                val selected = selectedTimeFilter == f
                val color = if (selected) EmeraldGreen else MaterialTheme.colorScheme.surface
                val textColor = if (selected) Color.Black else MaterialTheme.colorScheme.onSurface

                Card(
                    onClick = { selectedTimeFilter = f },
                    colors = CardDefaults.cardColors(containerColor = color),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = f,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }
            }
        }

        // Category Filters row
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val categories = listOf("All") + categoryOrder
            items(categories) { c ->
                val selected = selectedCategoryFilter == c
                val color = if (selected) EmeraldGreen else MaterialTheme.colorScheme.surface
                val textColor = if (selected) Color.Black else MaterialTheme.colorScheme.onSurface

                Card(
                    onClick = { selectedCategoryFilter = c },
                    colors = CardDefaults.cardColors(containerColor = color),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = c,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (filteredTransactions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillParentMaxSize()
                            .padding(bottom = 60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No matching transactions found.",
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                    }
                }
            } else {
                items(filteredTransactions) { tx ->
                    TransactionRow(
                        transaction = tx,
                        customCategories = customCategories,
                        onLongClick = { showTransactionOptionsFor = tx },
                        onDoubleTap = { showDuplicateConfirmFor = tx }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    // Transaction Options dialog
    showTransactionOptionsFor?.let { tx ->
        AlertDialog(
            onDismissRequest = { showTransactionOptionsFor = null },
            title = { Text("Transaction Options", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Select an action for this transaction:", style = MaterialTheme.typography.bodyMedium)
                    
                    if (tx.paymentStatus == "PENDING" || tx.paymentStatus == "FAILED") {
                        Button(
                            onClick = {
                                viewModel.updateTransactionStatus(tx.id, "PAID")
                                showTransactionOptionsFor = null
                                Toast.makeText(context, "Status updated to PAID (Successful)", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Mark as Successful (PAID)", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = {
                            onEditClick(tx)
                            showTransactionOptionsFor = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Edit Details / Type", color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            showDeleteDialogFor = tx
                            showTransactionOptionsFor = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Delete Transaction", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTransactionOptionsFor = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Confirmation dialog
    showDeleteDialogFor?.let { tx ->
        AlertDialog(
            onDismissRequest = { showDeleteDialogFor = null },
            title = { Text("Delete Transaction") },
            text = { Text("Remove \"${tx.party}\" (₹${formatAmount(tx.amount)})?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTransaction(tx)
                        showDeleteDialogFor = null
                    }
                ) {
                    Text("DELETE", color = CrimsonRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialogFor = null }) {
                    Text("CANCEL")
                }
            }
        )
    }

    // Duplicate Confirmation dialog
    showDuplicateConfirmFor?.let { tx ->
        AlertDialog(
            onDismissRequest = { showDuplicateConfirmFor = null },
            title = { Text("Duplicate Transaction") },
            text = { Text("Create a new duplicate transaction for \"${tx.party}\" (₹${formatAmount(tx.amount)})?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.addTransaction(
                            type = tx.type,
                            amount = tx.amount,
                            party = tx.party,
                            category = tx.category,
                            note = tx.note,
                            paymentMethod = tx.paymentMethod,
                            paymentStatus = tx.paymentStatus,
                            merchantUpiId = tx.merchantUpiId,
                            receiptUri = tx.receiptUri
                        )
                        showDuplicateConfirmFor = null
                        Toast.makeText(context, "Transaction duplicated successfully!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("duplicate_confirm_button")
                ) {
                    Text("DUPLICATE", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDuplicateConfirmFor = null }) {
                    Text("CANCEL")
                }
            }
        )
    }

    // Compare Parameters dialog
    if (showCompareDialog) {
        Dialog(onDismissRequest = { showCompareDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    FinancialComparisonCard(transactions = transactions)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { showCompareDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                        modifier = Modifier.align(Alignment.End),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Close", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------------------------
// 4. ADD MANUAL / QR DETAIL SCREEN
// ------------------------------------------------------------------------------------
@Composable
fun AddTransactionScreen(
    viewModel: FinanceViewModel,
    onDismiss: () -> Unit,
    prefilledMerchantName: String = "",
    prefilledMerchantUpi: String = "",
    prefilledAmount: String = "",
    isUpiPaymentFlow: Boolean = false,
    onPendingCreated: ((Int, String) -> Unit)? = null
) {
    var type by remember { mutableStateOf(if (isUpiPaymentFlow) "EXPENSE" else "EXPENSE") } // "EXPENSE" or "INCOME"
    var amountInput by remember { mutableStateOf(prefilledAmount) }
    var showCalculatorKeypad by remember { mutableStateOf(false) }
    var transactionTimestamp by remember { mutableStateOf(System.currentTimeMillis()) }
    var partyInput by remember { mutableStateOf(prefilledMerchantName) }
    var categoryInput by remember { mutableStateOf("Food") }
    var noteInput by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf(if (isUpiPaymentFlow) "BANK" else "BANK") } // "BANK" or "CASH"

    val categoryOrder by viewModel.categoryOrder.collectAsStateWithLifecycle()
    val customCategories by viewModel.customCategories.collectAsStateWithLifecycle()

    var showCustomCategoryDialog by remember { mutableStateOf(false) }
    var customCategoryNameInput by remember { mutableStateOf("") }
    var customCategoryEmojiInput by remember { mutableStateOf("🍟") }

    val emojiList = listOf("🍟", "🚕", "📱", "💵", "🏥", "📚", "🎬", "📦", "💻", "🏠", "🐶", "🏋️", "💇", "🎁", "⚽", "🍕", "☕", "🛒", "🛠️", "💅", "💄", "🧺", "🌱")

    // Image note attachment states
    var receiptPath by remember { mutableStateOf<String?>(null) }
    var receiptBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isAnalyzingImage by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val localPath = saveImageLocally(context, it)
            if (localPath != null) {
                receiptPath = localPath
                receiptBitmap = loadBitmapFromPath(context, localPath)
                Toast.makeText(context, "Receipt attached successfully", Toast.LENGTH_SHORT).show()
            }
        }
    }

    var pendingCameraCapturePath by remember { mutableStateOf<String?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && pendingCameraCapturePath != null) {
            val bitmap = loadBitmapFromPath(context, pendingCameraCapturePath!!)
            if (bitmap != null) {
                receiptPath = pendingCameraCapturePath
                receiptBitmap = bitmap
                Toast.makeText(context, "Photo captured successfully", Toast.LENGTH_SHORT).show()
            }
        }
    }

    var pendingOcrCameraCapturePath by remember { mutableStateOf<String?>(null) }
    val ocrCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && pendingOcrCameraCapturePath != null) {
            coroutineScope.launch {
                try {
                    isAnalyzingImage = true
                    val bitmap = loadBitmapFromPath(context, pendingOcrCameraCapturePath!!)
                    if (bitmap != null) {
                        receiptPath = pendingOcrCameraCapturePath
                        receiptBitmap = bitmap
                        val result = extractTransactionFromImage(bitmap)
                        if (result != null) {
                            type = result.type
                            if (result.amount > 0) {
                                amountInput = if (result.amount % 1.0 == 0.0) result.amount.toLong().toString() else result.amount.toString()
                            }
                            if (result.party.isNotBlank()) partyInput = result.party
                            if (result.category.isNotBlank()) categoryInput = result.category
                            if (result.note.isNotBlank()) noteInput = result.note
                            Toast.makeText(context, "📸 AI auto-filled details from Camera Photo!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Photo saved. Enter details manually.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "OCR Error: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    isAnalyzingImage = false
                }
            }
        }
    }

    val ocrImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            coroutineScope.launch {
                try {
                    isAnalyzingImage = true
                    val localPath = saveImageLocally(context, it)
                    if (localPath != null) {
                        receiptPath = localPath
                        val bitmap = loadBitmapFromPath(context, localPath)
                        if (bitmap != null) {
                            receiptBitmap = bitmap
                            val result = extractTransactionFromImage(bitmap)
                            if (result != null) {
                                type = result.type
                                if (result.amount > 0) {
                                    amountInput = if (result.amount % 1.0 == 0.0) result.amount.toLong().toString() else result.amount.toString()
                                }
                                if (result.party.isNotBlank()) partyInput = result.party
                                if (result.category.isNotBlank()) categoryInput = result.category
                                if (result.note.isNotBlank()) noteInput = result.note
                                Toast.makeText(context, "🖼️ AI auto-filled details from Gallery Image!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Image saved. Enter details manually.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    isAnalyzingImage = false
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // Custom Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Filled.Close, contentDescription = "Close")
            }
            Text(
                text = if (isUpiPaymentFlow) "Pay Details" else "Add Transaction",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Button(
                onClick = {
                    if (CalculatorEvaluator.isExpression(amountInput)) {
                        CalculatorEvaluator.evaluate(amountInput)?.let {
                            amountInput = CalculatorEvaluator.formatAmount(it)
                        }
                    }
                    val amt = amountInput.toDoubleOrNull()
                    if (amt == null || amt <= 0) {
                        Toast.makeText(context, "Please enter a valid amount.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val finalParty = if (partyInput.trim().isEmpty()) categoryInput else partyInput.trim()

                    if (isUpiPaymentFlow && onPendingCreated != null) {
                        // Create transaction as PENDING and trigger QR page
                        val tx = com.example.data.Transaction(
                            type = "EXPENSE",
                            amount = amt,
                            party = finalParty,
                            category = categoryInput,
                            note = noteInput,
                            paymentStatus = "PENDING",
                            paymentMethod = "BANK",
                            merchantUpiId = prefilledMerchantUpi,
                            receiptUri = receiptPath,
                            timestamp = transactionTimestamp
                        )
                        viewModel.trackCategorySelection(categoryInput)
                        kotlinx.coroutines.MainScope().launch {
                            val db = com.example.data.AppDatabase.getDatabase(context)
                            val txId = db.transactionDao().insertTransaction(tx).toInt()
                            onPendingCreated(txId, amountInput)
                        }
                    } else {
                        // Regular Manual Add Flow
                        viewModel.addTransaction(
                            type = type,
                            amount = amt,
                            party = finalParty,
                            category = categoryInput,
                            note = noteInput,
                            paymentMethod = paymentMethod,
                            paymentStatus = "PAID",
                            receiptUri = receiptPath,
                            timestamp = transactionTimestamp
                        )
                        viewModel.trackCategorySelection(categoryInput)
                        Toast.makeText(context, "Transaction Saved", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = if (type == "EXPENSE") CrimsonRed else EmeraldGreen),
                modifier = Modifier.testTag("save_transaction_button")
            ) {
                Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (isAnalyzingImage) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = EmeraldGreen)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("AI is extracting transaction details...", color = EmeraldGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            } else if (!isUpiPaymentFlow) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ocr_upload_card"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.AutoAwesome, contentDescription = "OCR Auto-Fill", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Smart Receipt OCR Auto-Fill", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    Text("Scan bill/receipt with Camera or select photo from Gallery", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f))
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val temp = createCameraTempUri(context)
                                        if (temp != null) {
                                            pendingOcrCameraCapturePath = temp.second
                                            ocrCameraLauncher.launch(temp.first)
                                        } else {
                                            Toast.makeText(context, "Unable to create camera file", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f).testTag("ocr_camera_button")
                                ) {
                                    Icon(Icons.Filled.PhotoCamera, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Camera OCR", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }

                                Button(
                                    onClick = { ocrImagePicker.launch("image/*") },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                                    modifier = Modifier.weight(1f).testTag("ocr_gallery_button")
                                ) {
                                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Gallery OCR", color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }

            if (!isUpiPaymentFlow) {
                item {
                    // Segment Selector (Expense vs Income)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        horizontalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        Button(
                            onClick = { type = "EXPENSE" },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("select_expense_tab"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (type == "EXPENSE") CrimsonRed else Color.Transparent
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.ArrowDownward, contentDescription = "Expense", tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Expense", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { type = "INCOME" },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("select_income_tab"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (type == "INCOME") EmeraldGreen else Color.Transparent
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.ArrowUpward, contentDescription = "Income", tint = if (type == "INCOME") Color.Black else Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Income", color = if (type == "INCOME") Color.Black else Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                // Interactive Calculator Amount Display (Opens custom keypad without system soft keyboard)
                CalculatorAmountDisplay(
                    amountText = amountInput,
                    type = type,
                    isActive = showCalculatorKeypad,
                    onActivate = { showCalculatorKeypad = true }
                )
            }

            item {
                // Paid To / Purpose Name
                val label = if (type == "EXPENSE") "PAID TO / PURPOSE (OPTIONAL)" else "RECEIVED FROM (OPTIONAL)"
                val placeholder = if (type == "EXPENSE") "e.g. Swiggy, Rent, Petrol..." else "e.g. Salary, Freelance, Gift..."
                OutlinedTextField(
                    value = partyInput,
                    onValueChange = {
                        if (showCalculatorKeypad) {
                            if (CalculatorEvaluator.isExpression(amountInput)) {
                                CalculatorEvaluator.evaluate(amountInput)?.let {
                                    amountInput = CalculatorEvaluator.formatAmount(it)
                                }
                            }
                            showCalculatorKeypad = false
                        }
                        partyInput = it
                    },
                    label = { Text(label) },
                    placeholder = { Text(placeholder) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("purpose_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (type == "EXPENSE") CrimsonRed else EmeraldGreen,
                        focusedLabelColor = if (type == "EXPENSE") CrimsonRed else EmeraldGreen
                    )
                )
            }

            item {
                // Category list
                Text("CATEGORY", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categoryOrder) { cat ->
                        val selected = categoryInput == cat
                        val borderCol = if (selected) if (type == "EXPENSE") CrimsonRed else EmeraldGreen else Color.Transparent
                        val emoji = getCategoryEmoji(cat, customCategories)

                        Card(
                            onClick = { categoryInput = cat },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selected) borderCol.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            border = BorderStroke(1.5.dp, if (selected) borderCol else MaterialTheme.colorScheme.outlineVariant),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(emoji, fontSize = 20.sp, modifier = Modifier.padding(end = 6.dp))
                                Text(cat, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Special "+ Custom" category button
                    item {
                        Card(
                            onClick = { showCustomCategoryDialog = true },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("➕", fontSize = 20.sp, modifier = Modifier.padding(end = 6.dp))
                                Text("Custom", fontWeight = FontWeight.Bold, color = EmeraldGreen)
                            }
                        }
                    }
                }
            }

            item {
                // Note input
                OutlinedTextField(
                    value = noteInput,
                    onValueChange = {
                        if (showCalculatorKeypad) {
                            if (CalculatorEvaluator.isExpression(amountInput)) {
                                CalculatorEvaluator.evaluate(amountInput)?.let {
                                    amountInput = CalculatorEvaluator.formatAmount(it)
                                }
                            }
                            showCalculatorKeypad = false
                        }
                        noteInput = it
                    },
                    label = { Text("NOTE (OPTIONAL)") },
                    placeholder = { Text("Add a note...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .testTag("note_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (type == "EXPENSE") CrimsonRed else EmeraldGreen,
                        focusedLabelColor = if (type == "EXPENSE") CrimsonRed else EmeraldGreen
                    )
                )
            }

            item {
                // Attachment Button
                Text("ATTACHMENT", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            val temp = createCameraTempUri(context)
                            if (temp != null) {
                                pendingCameraCapturePath = temp.second
                                cameraLauncher.launch(temp.first)
                            } else {
                                Toast.makeText(context, "Unable to create camera file", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.PhotoCamera, contentDescription = "Camera", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Capture", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Button(
                        onClick = { imagePicker.launch("image/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.PhotoLibrary, contentDescription = "Gallery", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Choose", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                receiptBitmap?.let { bitmap ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Attached Receipt Image preview",
                            modifier = Modifier.fillMaxSize()
                        )
                        IconButton(
                            onClick = {
                                receiptPath = null
                                receiptBitmap = null
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = "Remove receipt", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    if (isAnalyzingImage) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = EmeraldGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("AI is extracting transaction details...", color = EmeraldGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    } else {
                        Button(
                            onClick = {
                                isAnalyzingImage = true
                                coroutineScope.launch {
                                    val result = extractTransactionFromImage(bitmap)
                                    isAnalyzingImage = false
                                    if (result != null) {
                                        type = result.type
                                        if (result.amount > 0) {
                                            amountInput = if (result.amount % 1.0 == 0.0) result.amount.toLong().toString() else result.amount.toString()
                                        }
                                        if (result.party.isNotBlank()) partyInput = result.party
                                        if (result.category.isNotBlank()) categoryInput = result.category
                                        if (result.note.isNotBlank()) noteInput = result.note
                                        Toast.makeText(context, "AI detected transaction details!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "AI could not detect details.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = "AI Detect", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Detect Info using Gemini AI", color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (!isUpiPaymentFlow) {
                item {
                    // Payment Method (Bank vs Cash)
                    Text("PAYMENT METHOD", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Card(
                            onClick = { paymentMethod = "BANK" },
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(
                                containerColor = if (paymentMethod == "BANK") EmeraldGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            border = if (paymentMethod == "BANK") BorderStroke(2.dp, EmeraldGreen) else null,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Filled.AccountBalance, contentDescription = "Bank")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Bank (UPI)", fontWeight = FontWeight.Bold)
                            }
                        }

                        Card(
                            onClick = { paymentMethod = "CASH" },
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(
                                containerColor = if (paymentMethod == "CASH") EmeraldGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            border = if (paymentMethod == "CASH") BorderStroke(2.dp, EmeraldGreen) else null,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Filled.Payments, contentDescription = "Cash")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Cash", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }

        // Custom in-app Calculator Keypad docked at bottom
        AnimatedVisibility(
            visible = showCalculatorKeypad,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            CalculatorKeypad(
                amountText = amountInput,
                onAmountChange = { amountInput = it },
                selectedTimestamp = transactionTimestamp,
                onDateSelected = { transactionTimestamp = it },
                onDone = {
                    if (CalculatorEvaluator.isExpression(amountInput)) {
                        CalculatorEvaluator.evaluate(amountInput)?.let {
                            amountInput = CalculatorEvaluator.formatAmount(it)
                        }
                    }
                    showCalculatorKeypad = false
                },
                accentColor = if (type == "EXPENSE") CrimsonRed else EmeraldGreen
            )
        }
    }

    if (showCustomCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showCustomCategoryDialog = false },
            title = { Text("Create Custom Category") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = customCategoryNameInput,
                        onValueChange = { customCategoryNameInput = it },
                        label = { Text("Category Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Select Emoji Icon:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(emojiList) { emo ->
                            val isSelected = emo == customCategoryEmojiInput
                            val borderCol = if (isSelected) EmeraldGreen else Color.Transparent
                            Card(
                                onClick = { customCategoryEmojiInput = emo },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) EmeraldGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                border = BorderStroke(2.dp, borderCol),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = emo,
                                    fontSize = 28.sp,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = customCategoryNameInput.trim()
                        if (name.isEmpty()) {
                            Toast.makeText(context, "Category name cannot be empty.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.addCustomCategory(name, customCategoryEmojiInput)
                        categoryInput = name
                        showCustomCategoryDialog = false
                        customCategoryNameInput = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                ) {
                    Text("Add", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomCategoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ------------------------------------------------------------------------------------
// 4B. EDIT TRANSACTION SCREEN
// ------------------------------------------------------------------------------------
@Composable
fun EditTransactionScreen(
    viewModel: FinanceViewModel,
    transaction: Transaction,
    onDismiss: () -> Unit
) {
    var type by remember { mutableStateOf(transaction.type) } // "EXPENSE" or "INCOME"
    var amountInput by remember { mutableStateOf(if (transaction.amount % 1.0 == 0.0) transaction.amount.toLong().toString() else transaction.amount.toString()) }
    var showCalculatorKeypad by remember { mutableStateOf(false) }
    var transactionTimestamp by remember { mutableStateOf(transaction.timestamp) }
    var partyInput by remember { mutableStateOf(transaction.party) }
    var categoryInput by remember { mutableStateOf(transaction.category) }
    var noteInput by remember { mutableStateOf(transaction.note) }
    var paymentMethod by remember { mutableStateOf(transaction.paymentMethod) } // "BANK" or "CASH"
    var paymentStatus by remember { mutableStateOf(transaction.paymentStatus) } // "PAID", "PENDING", "FAILED"

    val categoryOrder by viewModel.categoryOrder.collectAsStateWithLifecycle()
    val customCategories by viewModel.customCategories.collectAsStateWithLifecycle()

    var showCustomCategoryDialog by remember { mutableStateOf(false) }
    var customCategoryNameInput by remember { mutableStateOf("") }
    var customCategoryEmojiInput by remember { mutableStateOf("🍟") }

    val emojiList = listOf("🍟", "🚕", "📱", "💵", "🏥", "📚", "🎬", "📦", "💻", "🏠", "🐶", "🏋️", "💇", "🎁", "⚽", "🍕", "☕", "🛒", "🛠️", "💅", "💄", "🧺", "🌱")

    // Image note attachment states
    var receiptPath by remember { mutableStateOf<String?>(transaction.receiptUri) }
    var receiptBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isAnalyzingImage by remember { mutableStateOf(false) }

    LaunchedEffect(transaction.receiptUri) {
        transaction.receiptUri?.let { path ->
            receiptBitmap = loadBitmapFromPath(context, path)
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val localPath = saveImageLocally(context, it)
            if (localPath != null) {
                receiptPath = localPath
                receiptBitmap = loadBitmapFromPath(context, localPath)
                Toast.makeText(context, "Receipt attached successfully", Toast.LENGTH_SHORT).show()
            }
        }
    }

    var pendingEditCameraCapturePath by remember { mutableStateOf<String?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && pendingEditCameraCapturePath != null) {
            val bitmap = loadBitmapFromPath(context, pendingEditCameraCapturePath!!)
            if (bitmap != null) {
                receiptPath = pendingEditCameraCapturePath
                receiptBitmap = bitmap
                Toast.makeText(context, "Photo captured successfully", Toast.LENGTH_SHORT).show()
            }
        }
    }

    var pendingEditOcrCameraCapturePath by remember { mutableStateOf<String?>(null) }
    val editOcrCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && pendingEditOcrCameraCapturePath != null) {
            coroutineScope.launch {
                try {
                    isAnalyzingImage = true
                    val bitmap = loadBitmapFromPath(context, pendingEditOcrCameraCapturePath!!)
                    if (bitmap != null) {
                        receiptPath = pendingEditOcrCameraCapturePath
                        receiptBitmap = bitmap
                        val result = extractTransactionFromImage(bitmap)
                        if (result != null) {
                            type = result.type
                            if (result.amount > 0) {
                                amountInput = if (result.amount % 1.0 == 0.0) result.amount.toLong().toString() else result.amount.toString()
                            }
                            if (result.party.isNotBlank()) partyInput = result.party
                            if (result.category.isNotBlank()) categoryInput = result.category
                            if (result.note.isNotBlank()) noteInput = result.note
                            Toast.makeText(context, "📸 AI auto-filled details from Camera Photo!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Photo saved. Enter details manually.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "OCR Error: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    isAnalyzingImage = false
                }
            }
        }
    }

    val editOcrImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            coroutineScope.launch {
                try {
                    isAnalyzingImage = true
                    val localPath = saveImageLocally(context, it)
                    if (localPath != null) {
                        receiptPath = localPath
                        val bitmap = loadBitmapFromPath(context, localPath)
                        if (bitmap != null) {
                            receiptBitmap = bitmap
                            val result = extractTransactionFromImage(bitmap)
                            if (result != null) {
                                type = result.type
                                if (result.amount > 0) {
                                    amountInput = if (result.amount % 1.0 == 0.0) result.amount.toLong().toString() else result.amount.toString()
                                }
                                if (result.party.isNotBlank()) partyInput = result.party
                                if (result.category.isNotBlank()) categoryInput = result.category
                                if (result.note.isNotBlank()) noteInput = result.note
                                Toast.makeText(context, "🖼️ AI auto-filled details from Gallery Image!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Image saved. Enter details manually.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    isAnalyzingImage = false
                }
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            // Custom Toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                }
                Text(
                    text = "Edit Transaction",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = {
                        if (CalculatorEvaluator.isExpression(amountInput)) {
                            CalculatorEvaluator.evaluate(amountInput)?.let {
                                amountInput = CalculatorEvaluator.formatAmount(it)
                            }
                        }
                        val amt = amountInput.toDoubleOrNull()
                        if (amt == null || amt <= 0) {
                            Toast.makeText(context, "Please enter a valid amount.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val finalParty = if (partyInput.trim().isEmpty()) categoryInput else partyInput.trim()

                        val updatedTx = transaction.copy(
                            type = type,
                            amount = amt,
                            party = finalParty,
                            category = categoryInput,
                            note = noteInput,
                            paymentMethod = paymentMethod,
                            paymentStatus = paymentStatus,
                            receiptUri = receiptPath,
                            timestamp = transactionTimestamp
                        )
                        viewModel.updateTransaction(updatedTx)
                        viewModel.trackCategorySelection(categoryInput)
                        Toast.makeText(context, "Transaction updated successfully", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Icon(Icons.Filled.Check, contentDescription = "Save", tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                if (isAnalyzingImage) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = EmeraldGreen)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("AI is extracting transaction details...", color = EmeraldGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                } else {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("edit_ocr_card"),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Filled.AutoAwesome, contentDescription = "OCR Auto-Fill", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Re-Scan Receipt with AI OCR", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                        Text("Snap photo with Camera or pick from Gallery", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f))
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            val temp = createCameraTempUri(context)
                                            if (temp != null) {
                                                pendingEditOcrCameraCapturePath = temp.second
                                                editOcrCameraLauncher.launch(temp.first)
                                            } else {
                                                Toast.makeText(context, "Unable to create camera file", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Filled.PhotoCamera, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Camera OCR", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }

                                    Button(
                                        onClick = { editOcrImagePicker.launch("image/*") },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Filled.PhotoLibrary, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Gallery OCR", color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    // Type selector
                    Text("TRANSACTION TYPE", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Card(
                            onClick = { type = "EXPENSE" },
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(
                                containerColor = if (type == "EXPENSE") CrimsonRed.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            border = if (type == "EXPENSE") BorderStroke(2.dp, CrimsonRed) else null,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Filled.ArrowUpward, contentDescription = "Expense", tint = CrimsonRed)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Expense", fontWeight = FontWeight.Bold, color = if (type == "EXPENSE") CrimsonRed else Color.White)
                            }
                        }

                        Card(
                            onClick = { type = "INCOME" },
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(
                                containerColor = if (type == "INCOME") EmeraldGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            border = if (type == "INCOME") BorderStroke(2.dp, EmeraldGreen) else null,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Filled.ArrowDownward, contentDescription = "Income", tint = EmeraldGreen)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Income", fontWeight = FontWeight.Bold, color = if (type == "INCOME") EmeraldGreen else Color.White)
                            }
                        }
                    }
                }

                item {
                    // Interactive Calculator Amount Display
                    CalculatorAmountDisplay(
                        amountText = amountInput,
                        type = type,
                        isActive = showCalculatorKeypad,
                        onActivate = { showCalculatorKeypad = true }
                    )
                }

                item {
                    // Party Input
                    OutlinedTextField(
                        value = partyInput,
                        onValueChange = {
                            if (showCalculatorKeypad) {
                                if (CalculatorEvaluator.isExpression(amountInput)) {
                                    CalculatorEvaluator.evaluate(amountInput)?.let {
                                        amountInput = CalculatorEvaluator.formatAmount(it)
                                    }
                                }
                                showCalculatorKeypad = false
                            }
                            partyInput = it
                        },
                        label = { Text(if (type == "EXPENSE") "Paid To (Merchant/Person)" else "Received From") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("edit_party_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (type == "EXPENSE") CrimsonRed else EmeraldGreen,
                            focusedLabelColor = if (type == "EXPENSE") CrimsonRed else EmeraldGreen
                        )
                    )
                }

                item {
                    // Notes Input
                    OutlinedTextField(
                        value = noteInput,
                        onValueChange = {
                            if (showCalculatorKeypad) {
                                if (CalculatorEvaluator.isExpression(amountInput)) {
                                    CalculatorEvaluator.evaluate(amountInput)?.let {
                                        amountInput = CalculatorEvaluator.formatAmount(it)
                                    }
                                }
                                showCalculatorKeypad = false
                            }
                            noteInput = it
                        },
                        label = { Text("Note (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("edit_note_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (type == "EXPENSE") CrimsonRed else EmeraldGreen,
                            focusedLabelColor = if (type == "EXPENSE") CrimsonRed else EmeraldGreen
                        )
                    )
                }

                item {
                    // Category list
                    Text("CATEGORY", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(categoryOrder) { cat ->
                            val selected = categoryInput == cat
                            val borderCol = if (selected) if (type == "EXPENSE") CrimsonRed else EmeraldGreen else Color.Transparent
                            val emoji = getCategoryEmoji(cat, customCategories)

                            Card(
                                onClick = { categoryInput = cat },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selected) borderCol.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                border = BorderStroke(1.5.dp, if (selected) borderCol else MaterialTheme.colorScheme.outlineVariant),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(emoji, fontSize = 20.sp, modifier = Modifier.padding(end = 6.dp))
                                    Text(cat, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Custom Category button
                        item {
                            Card(
                                onClick = { showCustomCategoryDialog = true },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("➕", fontSize = 20.sp, modifier = Modifier.padding(end = 6.dp))
                                    Text("Custom", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                item {
                    // Payment Status
                    Text("PAYMENT STATUS", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val statuses = listOf("PAID", "PENDING", "FAILED")
                        statuses.forEach { status ->
                            val isSelected = paymentStatus == status
                            val bgCol = when (status) {
                                "PAID" -> EmeraldGreen
                                "PENDING" -> Color(0xFFFFA000)
                                else -> CrimsonRed
                            }
                            Card(
                                onClick = { paymentStatus = status },
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) bgCol.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                border = if (isSelected) BorderStroke(2.dp, bgCol) else null,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Box(modifier = Modifier.padding(12.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    Text(status, fontWeight = FontWeight.Bold, color = if (isSelected) bgCol else Color.White)
                                }
                            }
                        }
                    }
                }

                item {
                    // Attachment Section
                    Text("RECEIPT OR INVOICE ATTACHMENT", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                val temp = createCameraTempUri(context)
                                if (temp != null) {
                                    pendingEditCameraCapturePath = temp.second
                                    cameraLauncher.launch(temp.first)
                                } else {
                                    Toast.makeText(context, "Unable to create camera file", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.PhotoCamera, contentDescription = "Camera", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Capture", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Button(
                            onClick = { imagePicker.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.PhotoLibrary, contentDescription = "Gallery", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Choose", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    receiptBitmap?.let { bitmap ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Attached Receipt Image preview",
                                modifier = Modifier.fillMaxSize()
                            )
                            IconButton(
                                onClick = {
                                    receiptPath = null
                                    receiptBitmap = null
                                },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = "Remove receipt", tint = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        if (isAnalyzingImage) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = EmeraldGreen)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("AI is extracting transaction details...", color = EmeraldGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        } else {
                            Button(
                                onClick = {
                                    isAnalyzingImage = true
                                    coroutineScope.launch {
                                        val result = extractTransactionFromImage(bitmap)
                                        isAnalyzingImage = false
                                        if (result != null) {
                                            type = result.type
                                            if (result.amount > 0) {
                                                amountInput = if (result.amount % 1.0 == 0.0) result.amount.toLong().toString() else result.amount.toString()
                                            }
                                            if (result.party.isNotBlank()) partyInput = result.party
                                            if (result.category.isNotBlank()) categoryInput = result.category
                                            if (result.note.isNotBlank()) noteInput = result.note
                                            Toast.makeText(context, "AI detected transaction details!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "AI could not detect details.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Filled.AutoAwesome, contentDescription = "AI Detect", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Detect Info using Gemini AI", color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                item {
                    // Payment Method (Bank vs Cash)
                    Text("PAYMENT METHOD", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Card(
                            onClick = { paymentMethod = "BANK" },
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(
                                containerColor = if (paymentMethod == "BANK") EmeraldGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            border = if (paymentMethod == "BANK") BorderStroke(2.dp, EmeraldGreen) else null,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Filled.AccountBalance, contentDescription = "Bank")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Bank (UPI)", fontWeight = FontWeight.Bold)
                            }
                        }

                        Card(
                            onClick = { paymentMethod = "CASH" },
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(
                                containerColor = if (paymentMethod == "CASH") EmeraldGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            border = if (paymentMethod == "CASH") BorderStroke(2.dp, EmeraldGreen) else null,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Filled.Payments, contentDescription = "Cash")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Cash", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }

            // Custom in-app Calculator Keypad docked at bottom
            AnimatedVisibility(
                visible = showCalculatorKeypad,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                CalculatorKeypad(
                    amountText = amountInput,
                    onAmountChange = { amountInput = it },
                    selectedTimestamp = transactionTimestamp,
                    onDateSelected = { transactionTimestamp = it },
                    onDone = {
                        if (CalculatorEvaluator.isExpression(amountInput)) {
                            CalculatorEvaluator.evaluate(amountInput)?.let {
                                amountInput = CalculatorEvaluator.formatAmount(it)
                            }
                        }
                        showCalculatorKeypad = false
                    },
                    accentColor = if (type == "EXPENSE") CrimsonRed else EmeraldGreen
                )
            }
        }
    }

    if (showCustomCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showCustomCategoryDialog = false },
            title = { Text("Create Custom Category") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = customCategoryNameInput,
                        onValueChange = { customCategoryNameInput = it },
                        label = { Text("Category Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Select Emoji Icon:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(emojiList) { emo ->
                            val isSelected = emo == customCategoryEmojiInput
                            val borderCol = if (isSelected) EmeraldGreen else Color.Transparent
                            Card(
                                onClick = { customCategoryEmojiInput = emo },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) EmeraldGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                border = BorderStroke(2.dp, borderCol),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = emo,
                                    fontSize = 28.sp,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = customCategoryNameInput.trim()
                        if (name.isEmpty()) {
                            Toast.makeText(context, "Category name cannot be empty.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.addCustomCategory(name, customCategoryEmojiInput)
                        categoryInput = name
                        showCustomCategoryDialog = false
                        customCategoryNameInput = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                ) {
                    Text("Add", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomCategoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        BitmapFactory.decodeStream(inputStream)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun scanQrCodeFromBitmap(
    bitmap: Bitmap,
    onSuccess: (String) -> Unit,
    onFailure: (Exception) -> Unit
) {
    val image = InputImage.fromBitmap(bitmap, 0)
    val scanner = BarcodeScanning.getClient()
    scanner.process(image)
        .addOnSuccessListener { barcodes ->
            val code = barcodes.firstOrNull()?.rawValue
            if (code != null) {
                onSuccess(code)
            } else {
                // Fallback to OCR if QR scan fails
                scanTextForUpi(bitmap, onSuccess, onFailure)
            }
        }
        .addOnFailureListener {
            // Fallback to OCR on failure
            scanTextForUpi(bitmap, onSuccess, onFailure)
        }
}

fun scanTextForUpi(
    bitmap: Bitmap,
    onSuccess: (String) -> Unit,
    onFailure: (Exception) -> Unit
) {
    val image = InputImage.fromBitmap(bitmap, 0)
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    recognizer.process(image)
        .addOnSuccessListener { visionText ->
            val fullText = visionText.text
            // Pattern for upi://pay links
            val upiLinkRegex = Regex("upi://pay\\?[^\\s]*", RegexOption.IGNORE_CASE)
            // Pattern for standard VPA (UPI ID)
            val vpaRegex = Regex("[a-zA-Z0-9.\\-_]{2,256}@[a-zA-Z0-9.\\-_]{2,64}")
            
            val upiMatch = upiLinkRegex.find(fullText)
            if (upiMatch != null) {
                onSuccess(upiMatch.value)
                return@addOnSuccessListener
            }
            
            val vpaMatch = vpaRegex.find(fullText)
            if (vpaMatch != null) {
                onSuccess(vpaMatch.value)
                return@addOnSuccessListener
            }
            
            onFailure(Exception("No UPI QR or ID detected. Please ensure the QR is clear."))
        }
        .addOnFailureListener {
            onFailure(it)
        }
}

sealed class QrResultType {
    data class Link(val url: String) : QrResultType()
    data class Text(val content: String) : QrResultType()
}

fun updateUpiUriAmount(originalUri: String, amount: Double): String {
    if (originalUri.isEmpty()) return originalUri
    return try {
        val payload = UpiPayload.parse(originalUri)
        if (payload.pa.isNotEmpty()) {
            payload.buildUpiUri(amount)
        } else {
            originalUri
        }
    } catch (e: Exception) {
        e.printStackTrace()
        originalUri
    }
}

fun processQrCodeResult(
    rawValue: String,
    context: Context,
    onMerchantParsed: (String, String, String?, String?) -> Unit,
    onShowExternalDialog: (QrResultType) -> Unit
) {
    val payload = UpiPayload.parse(rawValue)
    
    if (payload.pa.isNotEmpty()) {
        // Full original QR payload preserved in rawValue
        onMerchantParsed(payload.pn, payload.pa, payload.am, rawValue)
    } else {
        // Fallback: Check if it's an external web link or plain text
        if (rawValue.startsWith("http://", ignoreCase = true) || rawValue.startsWith("https://", ignoreCase = true)) {
            onShowExternalDialog(QrResultType.Link(rawValue))
        } else {
            onShowExternalDialog(QrResultType.Text(rawValue))
        }
    }
}

// ------------------------------------------------------------------------------------
// 5. SCANNER SCREEN
// ------------------------------------------------------------------------------------
@Composable
fun ScannerScreen(
    onDismiss: () -> Unit,
    onMerchantParsed: (String, String, String?, String?) -> Unit
) {
    var flashEnabled by remember { mutableStateOf(false) }
    var isManualEntry by remember { mutableStateOf(false) }
    var manualUpiId by remember { mutableStateOf("") }
    var manualMerchantName by remember { mutableStateOf("") }
    var activeExternalResult by remember { mutableStateOf<QrResultType?>(null) }

    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasCameraPermission = isGranted
            if (!isGranted) {
                Toast.makeText(context, "Camera permission is required to scan QR codes", Toast.LENGTH_LONG).show()
            }
        }
    )

    val qrImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val bitmap = loadBitmapFromUri(context, it)
            if (bitmap != null) {
                scanQrCodeFromBitmap(
                    bitmap = bitmap,
                    onSuccess = { rawValue ->
                        processQrCodeResult(
                            rawValue = rawValue,
                            context = context,
                            onMerchantParsed = onMerchantParsed,
                            onShowExternalDialog = { result ->
                                activeExternalResult = result
                            }
                        )
                    },
                    onFailure = { err ->
                        Toast.makeText(context, "No QR code detected: ${err.message}", Toast.LENGTH_LONG).show()
                    }
                )
            } else {
                Toast.makeText(context, "Failed to load image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Request on start
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // Custom Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
            }
            Text(
                "Scan UPI QR",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            IconButton(onClick = { flashEnabled = !flashEnabled }) {
                Icon(
                    if (flashEnabled) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                    contentDescription = "Flash Toggle",
                    tint = Color.White
                )
            }
        }

        if (!isManualEntry) {
            if (hasCameraPermission) {
                // Live Camera scan block
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    // Render real camera view
                    QrCameraPreview(
                        modifier = Modifier.fillMaxSize(),
                        onQrCodeScanned = { rawValue ->
                            if (activeExternalResult == null) {
                                processQrCodeResult(
                                    rawValue = rawValue,
                                    context = context,
                                    onMerchantParsed = onMerchantParsed,
                                    onShowExternalDialog = { result ->
                                        activeExternalResult = result
                                    }
                                )
                            }
                        },
                        flashEnabled = flashEnabled
                    )

                    // Overlay Target guides
                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .border(3.dp, EmeraldGreen, RoundedCornerShape(24.dp))
                    )

                    Text(
                        "Point camera at a UPI QR code",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 32.dp)
                    )
                }
            } else {
                // Friendly request container
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(Color.DarkGray.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Camera Access Required",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "Camera Access Required",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "To scan UPI QR codes directly, please allow EnterFirst to access your device's camera.",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            permissionLauncher.launch(android.Manifest.permission.CAMERA)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Grant Permission", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp)
                    .align(Alignment.CenterHorizontally),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { qrImageLauncher.launch("image/*") },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = "Upload QR", tint = EmeraldGreen)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Upload QR Image", color = Color.White)
                }

                Button(
                    onClick = { isManualEntry = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit UPI", tint = EmeraldGreen)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Manual UPI ID", color = Color.White)
                }
            }
        } else {
            // Manual UPI text entry fields
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    value = manualMerchantName,
                    onValueChange = { manualMerchantName = it },
                    label = { Text("Merchant Name (Optional)", color = Color.White) },
                    placeholder = { Text("e.g. Chai Shop, Grocery") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldGreen,
                        focusedLabelColor = EmeraldGreen,
                        unfocusedTextColor = Color.White,
                        focusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = manualUpiId,
                    onValueChange = { manualUpiId = it },
                    label = { Text("Enter UPI ID (VPA)", color = Color.White) },
                    placeholder = { Text("e.g. merchant@upi, paytm@qr") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldGreen,
                        focusedLabelColor = EmeraldGreen,
                        unfocusedTextColor = Color.White,
                        focusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("manual_upi_input")
                )

                Button(
                    onClick = {
                        if (manualUpiId.trim().isEmpty() || !manualUpiId.contains("@")) {
                            Toast.makeText(context, "Please enter a valid UPI ID (containing @).", Toast.LENGTH_SHORT).show()
                        } else {
                            val name = manualMerchantName.ifEmpty { "Merchant" }
                            onMerchantParsed(name, manualUpiId, null, "upi://pay?pa=$manualUpiId&pn=${Uri.encode(name)}")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                    modifier = Modifier.fillMaxWidth().testTag("manual_upi_submit"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Proceed to Pay", color = Color.Black, fontWeight = FontWeight.Bold)
                }

                TextButton(onClick = { isManualEntry = false }) {
                    Text("Back to scanning", color = EmeraldGreen, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    activeExternalResult?.let { result ->
        AlertDialog(
            onDismissRequest = { activeExternalResult = null },
            title = {
                Text(
                    text = when (result) {
                        is QrResultType.Link -> "External Link Detected"
                        is QrResultType.Text -> "Text Content Scanned"
                    },
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = when (result) {
                            is QrResultType.Link -> "Would you like to open this external link in your browser?"
                            is QrResultType.Text -> "Found the following text in the QR code:"
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = when (result) {
                                is QrResultType.Link -> result.url
                                is QrResultType.Text -> result.content
                            },
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        when (result) {
                            is QrResultType.Link -> {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(result.url))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Cannot open link: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                            is QrResultType.Text -> {
                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("Scanned Text", result.content)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                            }
                        }
                        activeExternalResult = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                ) {
                    Text(
                        text = when (result) {
                            is QrResultType.Link -> "Open Link"
                            is QrResultType.Text -> "Copy Text"
                        },
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { activeExternalResult = null }) {
                    Text("Close")
                }
            }
        )
    }
}

// ------------------------------------------------------------------------------------
// 6. PAYMENT WORKFLOW QR REGENERATOR & ACTION SCREEN
// ------------------------------------------------------------------------------------
@Composable
fun PaymentWorkflowScreen(
    viewModel: FinanceViewModel,
    pendingTxId: Int,
    merchantName: String,
    merchantUpiId: String,
    amount: Double,
    originalUpiUri: String = "",
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val paymentRouter = remember { PaymentRouter() }
    val scrollState = rememberScrollState()

    // Parse the entire original QR payload preserving all parameters
    val payload = remember(originalUpiUri, merchantUpiId, merchantName) {
        if (originalUpiUri.isNotEmpty()) {
            val parsed = UpiPayload.parse(originalUpiUri)
            if (parsed.pa.isNotEmpty()) parsed else UpiPayload(
                rawPayload = originalUpiUri,
                pa = merchantUpiId,
                pn = merchantName
            )
        } else {
            UpiPayload(
                rawPayload = "upi://pay?pa=$merchantUpiId&pn=${Uri.encode(merchantName)}",
                pa = merchantUpiId,
                pn = merchantName
            )
        }
    }

    // Determine classification: Bus/Transit, Merchant, or P2P
    val detectedPaymentType = remember(payload) { payload.determinePaymentType() }

    // Default strategy based on conditional routing:
    // P2P -> P2PPaymentStrategy (Direct UPI Intent)
    // Merchant / Bus -> MerchantPaymentStrategy (QR Image Share Intent)
    val initialStrategy = remember(detectedPaymentType) {
        when (detectedPaymentType) {
            PaymentType.P2P -> PaymentStrategyType.P2P_DIRECT_INTENT
            PaymentType.MERCHANT, PaymentType.BUS_TRANSIT -> PaymentStrategyType.MERCHANT_QR_IMAGE_SHARE
        }
    }

    var activeStrategy by remember { mutableStateOf(initialStrategy) }
    var optionSelected by remember { mutableStateOf(false) }
    var showPaymentSentProof by remember { mutableStateOf(false) }

    // Generate canonical clean QR bitmap for visual display & direct scanning
    val canonicalUpiUri = remember(payload, amount) { payload.buildUpiUri(amount) }
    val qrBitmap = remember(canonicalUpiUri) { generateQrCode(canonicalUpiUri, 400, 400) }

    fun executeActiveStrategy(strategyToUse: PaymentStrategyType) {
        val result = paymentRouter.routePayment(
            context = context,
            payload = payload,
            amount = amount,
            forcedStrategy = strategyToUse
        )
        when (result) {
            is StrategyResult.Success -> {
                // Initiated
            }
            is StrategyResult.FallbackSuggested -> {
                Toast.makeText(context, result.reason, Toast.LENGTH_SHORT).show()
            }
            is StrategyResult.Failure -> {
                Toast.makeText(context, result.error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Auto-launch the resolved strategy on entry
    LaunchedEffect(Unit) {
        executeActiveStrategy(activeStrategy)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0F19)) // Premium dark background
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(scrollState)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Badge indicating payment type
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(
                    when (detectedPaymentType) {
                        PaymentType.BUS_TRANSIT -> Color(0xFF06B6D4).copy(alpha = 0.15f)
                        PaymentType.MERCHANT -> Color(0xFF6366F1).copy(alpha = 0.15f)
                        PaymentType.P2P -> Color(0xFF10B981).copy(alpha = 0.15f)
                    },
                    shape = CircleShape
                )
                .border(
                    2.dp,
                    when (detectedPaymentType) {
                        PaymentType.BUS_TRANSIT -> Color(0xFF06B6D4).copy(alpha = 0.4f)
                        PaymentType.MERCHANT -> Color(0xFF6366F1).copy(alpha = 0.4f)
                        PaymentType.P2P -> Color(0xFF10B981).copy(alpha = 0.4f)
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when (detectedPaymentType) {
                    PaymentType.BUS_TRANSIT -> Icons.Filled.DirectionsBus
                    PaymentType.MERCHANT -> Icons.Outlined.QrCodeScanner
                    PaymentType.P2P -> Icons.Outlined.Smartphone
                },
                contentDescription = null,
                tint = when (detectedPaymentType) {
                    PaymentType.BUS_TRANSIT -> Color(0xFF22D3EE)
                    PaymentType.MERCHANT -> Color(0xFF818CF8)
                    PaymentType.P2P -> Color(0xFF34D399)
                },
                modifier = Modifier.size(30.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Tag indicating detected classification
        Surface(
            color = when (detectedPaymentType) {
                PaymentType.BUS_TRANSIT -> Color(0xFF06B6D4).copy(alpha = 0.18f)
                PaymentType.MERCHANT -> Color(0xFF6366F1).copy(alpha = 0.18f)
                PaymentType.P2P -> Color(0xFF10B981).copy(alpha = 0.18f)
            },
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(
                1.dp,
                when (detectedPaymentType) {
                    PaymentType.BUS_TRANSIT -> Color(0xFF06B6D4).copy(alpha = 0.4f)
                    PaymentType.MERCHANT -> Color(0xFF6366F1).copy(alpha = 0.4f)
                    PaymentType.P2P -> Color(0xFF10B981).copy(alpha = 0.4f)
                }
            )
        ) {
            Text(
                text = when (detectedPaymentType) {
                    PaymentType.BUS_TRANSIT -> "🚌 BUS & PUBLIC TRANSPORT QR"
                    PaymentType.MERCHANT -> "🛍️ SHOP / MERCHANT QR"
                    PaymentType.P2P -> "👤 PERSONAL / P2P UPI"
                },
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = when (detectedPaymentType) {
                        PaymentType.BUS_TRANSIT -> Color(0xFF22D3EE)
                        PaymentType.MERCHANT -> Color(0xFF818CF8)
                        PaymentType.P2P -> Color(0xFF34D399)
                    }
                ),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Heading & Subtitle
        Text(
            text = if (activeStrategy == PaymentStrategyType.MERCHANT_QR_IMAGE_SHARE) {
                "QR Share Ready"
            } else {
                "UPI App Launched"
            },
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = if (detectedPaymentType == PaymentType.BUS_TRANSIT) {
                "Standardized clean QR generated for flawless scanning in GPay, PhonePe, Paytm, and BHIM."
            } else if (activeStrategy == PaymentStrategyType.MERCHANT_QR_IMAGE_SHARE) {
                "EnterFirst shared the generated QR image to your UPI app for instant decoding."
            } else {
                "Authorizing payment in your selected UPI application."
            },
            style = MaterialTheme.typography.bodySmall.copy(
                color = Color(0xFF94A3B8)
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Payee & Fare Details Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161E2E)),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Color(0xFF233044))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = merchantName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = merchantUpiId,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF94A3B8)
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "₹${formatAmount(amount)}",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = EmeraldGreen
                        )
                    )
                }

                if (qrBitmap != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.wrapContentSize()
                        ) {
                            Image(
                                bitmap = qrBitmap.asImageBitmap(),
                                contentDescription = "Standardized UPI QR",
                                modifier = Modifier
                                    .size(190.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )

                            // Trigger button placed directly over QR image
                            Button(
                                onClick = {
                                    activeStrategy = PaymentStrategyType.MERCHANT_QR_IMAGE_SHARE
                                    executeActiveStrategy(PaymentStrategyType.MERCHANT_QR_IMAGE_SHARE)
                                },
                                shape = RoundedCornerShape(24.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF0F172A).copy(alpha = 0.94f)
                                ),
                                border = BorderStroke(1.5.dp, EmeraldGreen),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .testTag("pay_with_qr_share_button")
                                    .align(Alignment.Center)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Share,
                                    contentDescription = null,
                                    tint = EmeraldGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Pay with QR Share",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Disclaimer line
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1E293B).copy(alpha = 0.7f),
                        border = BorderStroke(1.dp, Color(0xFF334155)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("qr_share_disclaimer_container"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "You can pay using QR share method if auto UPI method fails",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                ),
                                modifier = Modifier.testTag("qr_share_disclaimer_text")
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Stacked Buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Option 1: Payment Successful
            PaymentConfirmationOption(
                icon = Icons.Outlined.CheckCircle,
                iconColor = Color(0xFF10B981),
                title = "Payment Successful",
                testTag = "payment_success_button",
                onClick = {
                    if (!optionSelected) {
                        optionSelected = true
                        playSoundEffect(context)
                        viewModel.updateTransactionStatus(pendingTxId, "PAID")
                        Toast.makeText(context, "Payment confirmed. Showing proof for 5 seconds...", Toast.LENGTH_SHORT).show()
                        showPaymentSentProof = true
                    }
                }
            )

            // Option 2: Payment Failed
            PaymentConfirmationOption(
                icon = Icons.Outlined.Cancel,
                iconColor = Color(0xFFEF4444),
                title = "Payment Failed",
                testTag = "payment_failed_button",
                onClick = {
                    if (!optionSelected) {
                        optionSelected = true
                        viewModel.updateTransactionStatus(pendingTxId, "FAILED")
                        Toast.makeText(context, "Transaction saved as Failed", Toast.LENGTH_SHORT).show()
                        onComplete()
                    }
                }
            )

            // Option 3: Payment Submitted
            PaymentConfirmationOption(
                icon = Icons.Outlined.AccessTime,
                iconColor = Color(0xFF94A3B8),
                title = "Payment Submitted",
                testTag = "payment_submitted_button",
                onClick = {
                    if (!optionSelected) {
                        optionSelected = true
                        viewModel.updateTransactionStatus(pendingTxId, "PENDING")
                        Toast.makeText(context, "Transaction marked as Pending/Submitted", Toast.LENGTH_SHORT).show()
                        onComplete()
                    }
                }
            )

            // Quick Action: Show Payment Proof to Sender (5s)
            OutlinedButton(
                onClick = {
                    showPaymentSentProof = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("show_payment_proof_quick_button"),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Show Payment Proof to Sender (5s Screen)",
                    color = Color(0xFF10B981),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Strategy Relaunch & Fallback Switcher
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = { executeActiveStrategy(activeStrategy) }
            ) {
                Text(
                    text = if (activeStrategy == PaymentStrategyType.MERCHANT_QR_IMAGE_SHARE) {
                        "Share Clean QR Image"
                    } else {
                        "Relaunch UPI App"
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF10B981),
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            Text("•", color = Color(0xFF475569), modifier = Modifier.padding(horizontal = 4.dp))

            // Graceful Fallback Switcher
            TextButton(
                onClick = {
                    val nextStrategy = if (activeStrategy == PaymentStrategyType.MERCHANT_QR_IMAGE_SHARE) {
                        PaymentStrategyType.P2P_DIRECT_INTENT
                    } else {
                        PaymentStrategyType.MERCHANT_QR_IMAGE_SHARE
                    }
                    activeStrategy = nextStrategy
                    executeActiveStrategy(nextStrategy)
                }
            ) {
                Text(
                    text = if (activeStrategy == PaymentStrategyType.MERCHANT_QR_IMAGE_SHARE) {
                        "Try Direct Link (Fallback)"
                    } else {
                        "Try QR Share (Fallback)"
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF60A5FA),
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Skip for now / Cancel
        TextButton(
            onClick = onComplete,
            modifier = Modifier.testTag("skip_payment_button")
        ) {
            Text(
                text = "Skip for now",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Medium
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showPaymentSentProof) {
        PaymentSentProofOverlay(
            merchantName = merchantName,
            merchantUpiId = merchantUpiId,
            amount = amount,
            pendingTxId = pendingTxId,
            onDismiss = onComplete
        )
    }
}

@Composable
fun PaymentConfirmationOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    title: String,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .testTag(testTag)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF121824)
        ),
        border = BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
        }
    }
}

fun getAppSignature(context: Context, type: String): String {
    try {
        val packageInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            context.packageManager.getPackageInfo(
                context.packageName,
                android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(
                context.packageName,
                android.content.pm.PackageManager.GET_SIGNATURES
            )
        }
        
        val signatures = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            packageInfo.signingInfo?.apkContentsSigners
        } else {
            @Suppress("DEPRECATION")
            packageInfo.signatures
        }

        if (signatures != null && signatures.isNotEmpty()) {
            val md = java.security.MessageDigest.getInstance(type)
            val publicKey = md.digest(signatures[0].toByteArray())
            val hexString = StringBuilder()
            for (i in publicKey.indices) {
                val appendString = java.lang.Integer.toHexString(0xFF and publicKey[i].toInt())
                if (appendString.length == 1) hexString.append("0")
                hexString.append(appendString.uppercase(java.util.Locale.US))
                if (i < publicKey.size - 1) hexString.append(":")
            }
            return hexString.toString()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return "Not Available"
}

// ------------------------------------------------------------------------------------
// 7. SETTINGS SCREEN
// ------------------------------------------------------------------------------------
@Composable
fun SettingsScreen(
    viewModel: FinanceViewModel,
    onPreviewZeroBalance: () -> Unit = {}
) {
    val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val initialBank by viewModel.initialBankBalance.collectAsStateWithLifecycle()
    val initialCash by viewModel.initialCashBalance.collectAsStateWithLifecycle()
    val googleEmail by viewModel.googleEmail.collectAsStateWithLifecycle()
    val googleName by viewModel.googleName.collectAsStateWithLifecycle()
    val driveBackupStatus by viewModel.driveBackupStatus.collectAsStateWithLifecycle()
    val autoBackupFrequency by viewModel.autoBackupFrequency.collectAsStateWithLifecycle()
    val lastAutoBackupTime by viewModel.lastAutoBackupTime.collectAsStateWithLifecycle()

    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
    val announcements by viewModel.announcements.collectAsStateWithLifecycle()
    var showAdminBroadcastDialog by remember { mutableStateOf(false) }
    var showUpdatesSheet by remember { mutableStateOf(false) }
    var showHotFixPromptDialog by remember { mutableStateOf(false) }
    var customHotFixPromptInput by remember { mutableStateOf("") }

    var showBankEditDialog by remember { mutableStateOf(false) }
    var showCashEditDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var showTroubleshoot by remember { mutableStateOf(false) }
    var showResetBudgetDialog by remember { mutableStateOf(false) }
    var showFixBackgroundDialogInSettings by remember { mutableStateOf(false) }
    var showTermsDialogInSettings by remember { mutableStateOf(false) }

    var editValue by remember { mutableStateOf("") }
    val context = LocalContext.current
    val runningPackageName = context.packageName
    val runningSha1 = getAppSignature(context, "SHA-1")
    val runningSha256 = getAppSignature(context, "SHA-256")

    val csvImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            viewModel.importFromCsv(context, it) { success, msg ->
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            }
        }
    }

    val csvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            viewModel.writeCsvToUri(context, uri) { success ->
                if (success) {
                    Toast.makeText(context, "Exported successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Export failed.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val googleAuthRecoveryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            viewModel.backupToGoogleDrive(context)
        } else {
            viewModel.setDriveBackupStatus("Failed: Consent denied by user")
        }
    }

    val recoveryIntent by viewModel.recoveryIntent.collectAsStateWithLifecycle()
    LaunchedEffect(recoveryIntent) {
        recoveryIntent?.let { intent ->
            googleAuthRecoveryLauncher.launch(intent)
            viewModel.clearRecoveryIntent()
        }
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
            if (account != null) {
                viewModel.setGoogleAccount(account.email, account.displayName)
                viewModel.fetchGoogleDriveToken(context, account)
            }
        } catch (e: com.google.android.gms.common.api.ApiException) {
            e.printStackTrace()
            val pkg = context.packageName
            val explanation = when (e.statusCode) {
                10 -> {
                    showTroubleshoot = true
                    "Developer Error (Code 10: Signature/Package mismatch).\n\nOpened the Troubleshooting panel below with your exact SHA-1 and package details to resolve this!"
                }
                12500 -> {
                    showTroubleshoot = true
                    "Sign-in configuration error (Code 12500).\n\nOpened the Troubleshooting panel below with configurations to assist you."
                }
                7 -> "Network error (Code 7: Please check your internet connection)."
                else -> "Error ${e.statusCode}: ${e.localizedMessage ?: "Unknown API exception"}"
            }
            Toast.makeText(context, explanation, Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Google Sign-In failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            // Save video to app files as cache
            val videoFile = File(context.filesDir, "custom_zero_balance_video.mp4")
            try {
                context.contentResolver.openInputStream(it)?.use { input ->
                    videoFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                viewModel.setCustomZeroBalanceVideoPath(videoFile.absolutePath)
                Toast.makeText(context, "Custom zero balance video saved!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Failed to save video: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val isZeroBalanceVideoEnabled by viewModel.isZeroBalanceVideoEnabled.collectAsStateWithLifecycle()
    val customZeroBalanceVideoPath by viewModel.customZeroBalanceVideoPath.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppLogoGraphic(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text("Settings", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        }

        // Theme Row
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isDarkTheme) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                        contentDescription = "Theme"
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Dark Theme", fontWeight = FontWeight.Bold)
                }
                Switch(
                    checked = isDarkTheme,
                    onCheckedChange = { viewModel.toggleTheme() },
                    colors = SwitchDefaults.colors(checkedThumbColor = EmeraldGreen)
                )
            }
        }

        // Update Bank Balance Row
        Card(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                editValue = ""
                showBankEditDialog = true
            },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.AccountBalance, contentDescription = "Bank Balance")
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Update Bank Balance", fontWeight = FontWeight.Bold)
                }
                Icon(Icons.Filled.ChevronRight, contentDescription = "Navigate")
            }
        }

        // Update Cash Balance Row
        Card(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                editValue = ""
                showCashEditDialog = true
            },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Payments, contentDescription = "Cash Balance")
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Update Cash Balance", fontWeight = FontWeight.Bold)
                }
                Icon(Icons.Filled.ChevronRight, contentDescription = "Navigate")
            }
        }

        // Manage Multi-UPI QRs Card
        val userUpiQrsInSettings by viewModel.userUpiQrs.collectAsStateWithLifecycle()
        var showManageQrsDialogInSettings by remember { mutableStateOf(false) }

        Card(
            modifier = Modifier.fillMaxWidth(),
            onClick = { showManageQrsDialogInSettings = true },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(ElectricBlue.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.QrCode2, contentDescription = "Manage QRs", tint = ElectricBlue)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Manage Multi-UPI QRs", fontWeight = FontWeight.Bold)
                        Text(
                            if (userUpiQrsInSettings.isNotEmpty()) "${userUpiQrsInSettings.size} QR code(s) stored • Tap to edit or reorder" else "Store & organize your UPI QRs",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(Icons.Filled.ChevronRight, contentDescription = "Navigate")
            }
        }

        if (showManageQrsDialogInSettings) {
            ManageUpiQrsDialog(
                upiQrs = userUpiQrsInSettings,
                onDismiss = { showManageQrsDialogInSettings = false },
                onMoveUp = { viewModel.moveUpiQrUp(it) },
                onMoveDown = { viewModel.moveUpiQrDown(it) },
                onDelete = { viewModel.deleteUpiQrCode(it) },
                onUpdate = { id, appName, imgPath -> viewModel.updateUpiQr(id, appName, imgPath) },
                onAddNewQr = { appName, imgPath ->
                    val newList = userUpiQrsInSettings + UpiQrCode(appName = appName, imagePath = imgPath)
                    viewModel.saveUpiQrCodes(newList)
                }
            )
        }

        // Zero Balance Video Settings
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.VideoSettings, contentDescription = null, tint = EmeraldGreen)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Zero Balance Animation", fontWeight = FontWeight.Bold)
                            Text("Play celebration video when balance is zero", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Switch(
                        checked = isZeroBalanceVideoEnabled,
                        onCheckedChange = { viewModel.setZeroBalanceVideoEnabled(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = EmeraldGreen)
                    )
                }

                if (isZeroBalanceVideoEnabled) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Custom Video", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Text(
                                text = if (customZeroBalanceVideoPath != null) "Using custom video file" else "Using default app animation",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (customZeroBalanceVideoPath != null) EmeraldGreen else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Row {
                            if (customZeroBalanceVideoPath != null) {
                                IconButton(onClick = { viewModel.setCustomZeroBalanceVideoPath(null) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Remove custom video", tint = CrimsonRed)
                                }
                            }
                            
                            Button(
                                onClick = { 
                                    videoPickerLauncher.launch(
                                        androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Filled.Movie, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (customZeroBalanceVideoPath != null) "Change" else "Pick Video", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Export/Import CSV and Google Drive Cloud Backup Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Backup & Restore (Local CSV)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            val timeSec = System.currentTimeMillis() / 1000
                            csvExportLauncher.launch("enterfirst_backup_$timeSec.csv")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.CloudUpload, contentDescription = "Export")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export CSV", color = Color.Black)
                    }

                    Button(
                        onClick = { csvImportLauncher.launch("text/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.CloudDownload, contentDescription = "Import", tint = EmeraldGreen)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Import CSV", color = MaterialTheme.colorScheme.onSurface)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)

                Text("Google Drive Cloud Backup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                if (googleEmail == null) {
                    Text(
                        "Link your Google Account to securely backup and sync your transaction history to Google Drive. A monthly CSV backup will be overwritten automatically.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = {
                            val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)
                                .requestEmail()
                                .requestScopes(com.google.android.gms.common.api.Scope("https://www.googleapis.com/auth/drive.file"))
                                .build()
                            val signInClient = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, gso)
                            googleSignInLauncher.launch(signInClient.signInIntent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Filled.Link, contentDescription = "Link Google Account", tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Link Google Account", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Linked Account", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = EmeraldGreen)
                            Text(googleName ?: "Google User", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                            Text(googleEmail ?: "", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(
                            onClick = {
                                val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)
                                    .requestEmail()
                                    .requestScopes(com.google.android.gms.common.api.Scope("https://www.googleapis.com/auth/drive.file"))
                                    .build()
                                val signInClient = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, gso)
                                signInClient.signOut().addOnCompleteListener {
                                    viewModel.setGoogleAccount(null, null)
                                }
                            }
                        ) {
                            Icon(Icons.Filled.Logout, contentDescription = "Sign Out", tint = CrimsonRed)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Unlink", color = CrimsonRed)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Auto Backup Frequency",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("OFF" to "Off", "DAILY" to "Daily", "WEEKLY" to "Weekly", "MONTHLY" to "Monthly").forEach { (freqKey, label) ->
                            val isSelected = autoBackupFrequency == freqKey
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        color = if (isSelected) EmeraldGreen else MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        viewModel.setAutoBackupFrequency(freqKey)
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    if (lastAutoBackupTime > 0L) {
                        Spacer(modifier = Modifier.height(6.dp))
                        val formattedTime = formatTimestamp(lastAutoBackupTime)
                        Text(
                            text = "Last auto backup: $formattedTime",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.backupToGoogleDrive(context)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Filled.CloudSync, contentDescription = "Sync", tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Backup Now", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Button(
                            onClick = {
                                viewModel.restoreFromGoogleDrive(context)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = "Restore", tint = MaterialTheme.colorScheme.onSecondaryContainer)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restore Now", color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    if (driveBackupStatus != "Idle") {
                        val isSuccess = driveBackupStatus.startsWith("Success")
                        val isError = driveBackupStatus.startsWith("Failed")
                        val statusColor = if (isSuccess) EmeraldGreen else if (isError) CrimsonRed else MaterialTheme.colorScheme.onSurfaceVariant
                        
                        Text(
                            text = driveBackupStatus,
                            style = MaterialTheme.typography.bodyMedium,
                            color = statusColor,
                            fontWeight = if (isSuccess || isError) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { showTroubleshoot = !showTroubleshoot },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = if (showTroubleshoot) "Hide Connection Info" else "Troubleshoot Connection (Code 10)",
                                color = EmeraldGreen,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(onClick = { showTroubleshoot = !showTroubleshoot }) {
                            Icon(
                                imageVector = if (showTroubleshoot) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = "Toggle Troubleshooting",
                                tint = EmeraldGreen
                            )
                        }
                    }

                    if (showTroubleshoot) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    "Google Drive integration requires registering this app's package name and SHA-1 in your Google Cloud Console / Firebase project. Copy these details and add them under 'Android App' in your project settings:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                                
                                // Package Name Row
                                Column {
                                    Text("Package Name", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = EmeraldGreen)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(runningPackageName, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                        IconButton(
                                            onClick = {
                                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(runningPackageName))
                                                Toast.makeText(context, "Copied Package Name", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Filled.ContentCopy, contentDescription = "Copy Package Name", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }

                                // SHA-1 Fingerprint Row
                                Column {
                                    Text("Debug SHA-1 Signature", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = EmeraldGreen)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            runningSha1,
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = {
                                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(runningSha1))
                                                Toast.makeText(context, "Copied SHA-1", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Filled.ContentCopy, contentDescription = "Copy SHA-1", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }

                                // SHA-256 Fingerprint Row
                                Column {
                                    Text("Debug SHA-256 Signature", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = EmeraldGreen)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            runningSha256,
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = {
                                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(runningSha256))
                                                Toast.makeText(context, "Copied SHA-256", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Filled.ContentCopy, contentDescription = "Copy SHA-256", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Admin Broadcast Portal
        if (FinanceViewModel.isUserAdmin(googleEmail) || isAdmin) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_portal_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF0F172A)
                ),
                border = BorderStroke(
                    1.dp,
                    EmeraldGreen.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Campaign,
                                contentDescription = null,
                                tint = EmeraldGreen,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Admin Broadcast Portal",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Verified Administrator Session",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Surface(
                            color = EmeraldGreen.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "ACTIVE ADMIN",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldGreen
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Broadcast updates, notices, and release announcements to all registered users of the app.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                showAdminBroadcastDialog = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("open_admin_broadcast_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                        ) {
                            Icon(Icons.Filled.Send, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Send Update", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        
                        Button(
                            onClick = onPreviewZeroBalance,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            Icon(Icons.Filled.Visibility, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Preview UI", color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }

            // Gemini OCR Configuration Framework (ocr_config.json) & Hot-Fix Controls (ADMIN ONLY)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ocr_config_framework_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFF00E5FF).copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Memory,
                                    contentDescription = null,
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Gemini OCR System Prompt",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Config File: ocr_config.json • Active v${com.example.ocr.OcrConfigManager.getCurrentVersion()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF00FF66).copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "ADMIN ONLY",
                                color = Color(0xFF00FF66),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Edit system prompt variables and broadcast hot-fixes to all app users. Other users will receive a notification with a 'Fix Now' action that triggers the optimization animation and live-updates their ocr_config.json.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                customHotFixPromptInput = com.example.ocr.OcrConfigManager.getSystemPrompt(context)
                                showHotFixPromptDialog = true
                            },
                            modifier = Modifier
                                .weight(1.3f)
                                .testTag("edit_ocr_config_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = null, tint = Color.Black, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Edit & Send Fix", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        FilledTonalButton(
                            onClick = {
                                // Trigger the 3-second dynamic optimization overlay for testing
                                com.example.ocr.OcrConfigManager.triggerHotFixAnimation()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("test_hot_fix_animation_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Test UI (3s)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Preview Zero Balance Video
        Card(
            modifier = Modifier.fillMaxWidth().testTag("preview_zero_balance_card"),
            onClick = onPreviewZeroBalance,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.PlayCircleFilled, contentDescription = "Preview Video", tint = EmeraldGreen)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Preview Zero Balance Video", fontWeight = FontWeight.Bold)
                        Text("Watch the animation triggered when balance reaches ₹0", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Icon(Icons.Filled.ChevronRight, contentDescription = "Play", tint = EmeraldGreen)
            }
        }

        // Reset Monthly Budget Row
        Card(
            modifier = Modifier.fillMaxWidth().testTag("reset_budget_card"),
            onClick = { showResetBudgetDialog = true },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Reset Budget")
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Reset Monthly Budget", fontWeight = FontWeight.Bold)
                }
                Icon(Icons.Filled.ChevronRight, contentDescription = "Navigate")
            }
        }

        // Background & Battery Optimization Row
        Card(
            modifier = Modifier.fillMaxWidth().testTag("background_battery_settings_card"),
            onClick = { showFixBackgroundDialogInSettings = true },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.BatteryChargingFull, contentDescription = "Battery Settings", tint = EmeraldGreen)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Background & Battery Optimization", fontWeight = FontWeight.Bold)
                        Text("Fix missed push updates & alarms when app is closed", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Icon(Icons.Filled.ChevronRight, contentDescription = "Open", tint = EmeraldGreen)
            }
        }

        // Terms & Conditions (Section 5: Setup Agreement summary)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_terms_card"),
            onClick = { showTermsDialogInSettings = true },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Gavel, contentDescription = "Terms & Conditions", tint = EmeraldGreen)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Terms & Conditions", fontWeight = FontWeight.Bold)
                        Text("EnterFirst legal summary & scope of service", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Icon(Icons.Filled.ChevronRight, contentDescription = "Open", tint = EmeraldGreen)
            }
        }

        // Reset All Data Row
        Card(
            modifier = Modifier.fillMaxWidth(),
            onClick = { showResetConfirmDialog = true },
            colors = CardDefaults.cardColors(containerColor = CrimsonRed.copy(alpha = 0.15f)),
            border = BorderStroke(1.dp, CrimsonRed)
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.DeleteForever, contentDescription = "Reset All", tint = CrimsonRed)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Reset All Data", color = CrimsonRed, fontWeight = FontWeight.Bold)
                }
                Icon(Icons.Filled.ChevronRight, contentDescription = "Navigate", tint = CrimsonRed)
            }
        }
    }

    // Dialogs
    if (showTermsDialogInSettings) {
        TermsAndConditionsDialog(
            onDismiss = { showTermsDialogInSettings = false }
        )
    }

    if (showFixBackgroundDialogInSettings) {
        com.example.util.FixBackgroundRestrictionsDialog(
            onDismiss = { showFixBackgroundDialogInSettings = false }
        )
    }

    if (showResetBudgetDialog) {
        AlertDialog(
            onDismissRequest = { showResetBudgetDialog = false },
            title = { Text("Reset Monthly Budget") },
            text = { Text("Are you sure you want to reset your monthly budget? This will remove your set spending limit.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.setMonthlyBudgetLimit(0.0)
                        showResetBudgetDialog = false
                        Toast.makeText(context, "Monthly budget has been reset.", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("confirm_reset_budget_button")
                ) {
                    Text("RESET", color = CrimsonRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetBudgetDialog = false }) {
                    Text("CANCEL")
                }
            }
        )
    }

    if (showBankEditDialog) {
        AlertDialog(
            onDismissRequest = { showBankEditDialog = false },
            title = { Text("Update Bank Balance") },
            text = {
                OutlinedTextField(
                    value = editValue,
                    onValueChange = { editValue = it },
                    label = { Text("New Bank Balance (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val amt = editValue.toDoubleOrNull()
                        if (amt != null && amt >= 0) {
                            viewModel.updateBankBalance(amt)
                            showBankEditDialog = false
                        } else {
                            Toast.makeText(context, "Enter a valid positive number.", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) { Text("UPDATE", color = EmeraldGreen, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showBankEditDialog = false }) { Text("CANCEL") } }
        )
    }

    if (showCashEditDialog) {
        AlertDialog(
            onDismissRequest = { showCashEditDialog = false },
            title = { Text("Update Cash Balance") },
            text = {
                OutlinedTextField(
                    value = editValue,
                    onValueChange = { editValue = it },
                    label = { Text("New Cash Balance (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val amt = editValue.toDoubleOrNull()
                        if (amt != null && amt >= 0) {
                            viewModel.updateCashBalance(amt)
                            showCashEditDialog = false
                        } else {
                            Toast.makeText(context, "Enter a valid positive number.", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) { Text("UPDATE", color = EmeraldGreen, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showCashEditDialog = false }) { Text("CANCEL") } }
        )
    }

    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("Confirm Full Reset") },
            text = { Text("This permanently deletes all transaction history, starting balances, and settings. This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetAllData()
                        showResetConfirmDialog = false
                    }
                ) { Text("RESET EVERYTHING", color = CrimsonRed, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showResetConfirmDialog = false }) { Text("CANCEL") } }
        )
    }

    if (showAdminBroadcastDialog) {
        AdminBroadcastDialog(
            viewModel = viewModel,
            allUsers = allUsers,
            onDismiss = { showAdminBroadcastDialog = false }
        )
    }

    if (showUpdatesSheet) {
        AppUpdatesSheet(
            announcements = announcements,
            isAdmin = (FinanceViewModel.isUserAdmin(googleEmail) || isAdmin),
            viewModel = viewModel,
            onMarkRead = { viewModel.markAnnouncementRead(it) },
            onMarkAllRead = { viewModel.markAllAnnouncementsRead() },
            onDelete = { viewModel.deleteAnnouncement(it, context) },
            onDismiss = { showUpdatesSheet = false }
        )
    }

    if (showHotFixPromptDialog && (FinanceViewModel.isUserAdmin(googleEmail) || isAdmin)) {
        AlertDialog(
            onDismissRequest = { showHotFixPromptDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Memory,
                    contentDescription = null,
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "Live OCR System Prompt (ocr_config.json)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp)
                ) {
                    Text(
                        text = "Modify the active system prompt variables for Gemini AI calls. Clicking 'Send Fix to All' broadcasts the fix to all registered users, triggering a 'Fix OCR Bug' notification with a 'Fix' button.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = customHotFixPromptInput,
                        onValueChange = { customHotFixPromptInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .testTag("hot_fix_prompt_input"),
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                        placeholder = { Text("Enter updated system prompt...") },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customHotFixPromptInput.isNotBlank()) {
                            val nextVer = com.example.ocr.OcrConfigManager.getCurrentVersion() + 1
                            // 1. Update Admin's local prompt
                            com.example.ocr.OcrConfigManager.applyHotFix(context, customHotFixPromptInput, nextVer)

                            // 2. Broadcast Fix announcement to all users
                            viewModel.postAdminUpdate(
                                title = "Fix OCR Bug",
                                message = "Admin released an updated OCR prompt (v$nextVer). Tap 'Fix' to optimize app logic.",
                                category = "HOT_FIX",
                                priority = "URGENT",
                                context = context
                            )
                            Toast.makeText(context, "🚀 OCR Bug Fix broadcasted to all users! Notification dispatched.", Toast.LENGTH_LONG).show()
                            showHotFixPromptDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                    modifier = Modifier.testTag("broadcast_hot_fix_button")
                ) {
                    Icon(Icons.Filled.Send, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Send Fix to All", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(
                        onClick = {
                            if (customHotFixPromptInput.isNotBlank()) {
                                com.example.ocr.OcrConfigManager.applyHotFix(context, customHotFixPromptInput)
                                showHotFixPromptDialog = false
                            }
                        }
                    ) {
                        Text("Apply Locally", color = Color(0xFF00E5FF))
                    }
                    TextButton(
                        onClick = {
                            com.example.ocr.OcrConfigManager.resetToDefault(context)
                            customHotFixPromptInput = com.example.ocr.OcrConfigManager.DEFAULT_OCR_SYSTEM_PROMPT
                        }
                    ) {
                        Text("Reset Default", color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(onClick = { showHotFixPromptDialog = false }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }
}

// ------------------------------------------------------------------------------------
// 8. ABOUT SCREEN
// ------------------------------------------------------------------------------------
@Composable
fun InstagramLogoIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Canvas(modifier = modifier) {
        val sizePx = size.width
        val strokeWidthPx = sizePx * 0.08f
        
        // 1. Draw outer rounded rectangle
        drawRoundRect(
            color = tint,
            topLeft = Offset(strokeWidthPx / 2f, strokeWidthPx / 2f),
            size = Size(sizePx - strokeWidthPx, sizePx - strokeWidthPx),
            cornerRadius = CornerRadius(sizePx * 0.28f),
            style = Stroke(width = strokeWidthPx)
        )
        
        // 2. Draw inner circle
        drawCircle(
            color = tint,
            radius = sizePx * 0.21f,
            center = Offset(sizePx / 2f, sizePx / 2f),
            style = Stroke(width = strokeWidthPx)
        )
        
        // 3. Draw top-right dot
        drawCircle(
            color = tint,
            radius = sizePx * 0.055f,
            center = Offset(sizePx * 0.72f, sizePx * 0.28f)
        )
    }
}

@Composable
fun LinkedInLogoIcon(modifier: Modifier = Modifier, tint: Color = Color.White) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "in",
            color = tint,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
            modifier = Modifier.offset(y = (-2).dp)
        )
    }
}

@Composable
fun AboutScreen() {
    val context = LocalContext.current
    var showUserManualDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text("About", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
        }

        item {
            // App Info Box
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AppLogoGraphic(
                        modifier = Modifier
                            .size(140.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "EnterFirst",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2979FF)
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Track. Manage. Save.",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF607D8B)
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "A privacy-first expense tracker that works before UPI payment. Scan once, record expenses, and pay — no bank linking, no SMS access, no credentials.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        item {
            // Developer Profile Box
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "DEVELOPER",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldGreen,
                        letterSpacing = 1.sp,
                        modifier = Modifier.align(Alignment.Start)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CreatorPortraitGraphic(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(CircleShape)
                                .border(
                                    width = 3.dp,
                                    brush = Brush.linearGradient(
                                        colors = listOf(Color(0xFFE1306C), Color(0xFF2196F3))
                                    ),
                                    shape = CircleShape
                                )
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Custom styled Instagram Button
                            IconButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/_.daxrshnxz/"))
                                    context.startActivity(intent)
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(Color(0xFF1F121C), RoundedCornerShape(14.dp))
                            ) {
                                InstagramLogoIcon(
                                    modifier = Modifier.size(24.dp),
                                    tint = Color(0xFFE1306C)
                                )
                            }

                            // Custom styled LinkedIn Button
                            IconButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.linkedin.com/in/s-divyadarshan-796076376/"))
                                    context.startActivity(intent)
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(Color(0xFF101C2B), RoundedCornerShape(14.dp))
                            ) {
                                LinkedInLogoIcon(
                                    modifier = Modifier.size(24.dp),
                                    tint = Color(0xFF0077B5)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "S DIVYA DARSHAN",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "sddarshan2007@gmail.com",
                            style = MaterialTheme.typography.bodyMedium,
                            color = EmeraldGreen,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "A Guy with interests in technology, creative problem-solving, and digital content creation. Skilled in presentation design, basic video editing, and exploring innovative ideas through creativity and technical learning.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            textAlign = TextAlign.Start,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }

        item {
            // App Info Rows (Updated: Removed Platform & Storage rows, added user manual hyperlink)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("APP INFO", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = EmeraldGreen)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Version", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Text("1.0.0", fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Data collected", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Text("None", fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Hyperlink PDF row to open EnterFirst user manual
                    Surface(
                        onClick = {
                            showUserManualDialog = true
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = EmeraldGreen.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open_user_manual_hyperlink")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PictureAsPdf,
                                    contentDescription = null,
                                    tint = EmeraldGreen,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "open Enter first user manual",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldGreen,
                                            textDecoration = TextDecoration.Underline
                                        )
                                    )
                                    Text(
                                        text = "EnterFirst_user_manual_v2 (PDF)",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Filled.ChevronRight,
                                contentDescription = "Open",
                                tint = EmeraldGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Made with ❤️ in India — EnterFirst v1.0.0",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Medium
                ),
                modifier = Modifier.padding(vertical = 12.dp)
            )
        }
    }

    if (showUserManualDialog) {
        UserManualDialog(onDismiss = { showUserManualDialog = false })
    }
}

// ------------------------------------------------------------------------------------
// CUSTOM INTERACTIVE PIE CHART / DOUGHNUT CHART IN COMPOSE CANVAS
// ------------------------------------------------------------------------------------
@Composable
fun InteractivePieChart(categorySums: Map<String, Double>) {
    val total = categorySums.values.sum()
    if (total <= 0) return

    val entries = categorySums.toList().sortedByDescending { it.second }
    var selectedSliceIndex by remember { mutableStateOf(-1) }
    val cutoutColor = MaterialTheme.colorScheme.surface

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    // Interactive slice selection based on coordinates can be implemented,
                    // but for premium simplicity we can cycle tap or show totals
                    selectedSliceIndex = (selectedSliceIndex + 1) % entries.size
                }
            }
    ) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = min(size.width, size.height) / 2 * 0.85f

        var startAngle = -90f
        entries.forEachIndexed { index, (cat, amt) ->
            val sweepAngle = ((amt / total) * 360f).toFloat()
            val color = CategoryColors[cat] ?: Color.Gray
            val isSelected = index == selectedSliceIndex

            val scaleRadius = if (isSelected) radius * 1.05f else radius

            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = true,
                size = Size(scaleRadius * 2, scaleRadius * 2),
                topLeft = Offset(center.x - scaleRadius, center.y - scaleRadius)
            )
            startAngle += sweepAngle
        }

        // Draw central cutout for Doughnut look dynamically styled
        drawCircle(
            color = cutoutColor,
            radius = radius * 0.55f,
            center = center
        )
    }

    // Inside-Pie Text details display
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.wrapContentSize()
    ) {
        if (selectedSliceIndex >= 0 && selectedSliceIndex < entries.size) {
            val (cat, amt) = entries[selectedSliceIndex]
            Text(cat, style = MaterialTheme.typography.bodySmall, color = EmeraldGreen, fontWeight = FontWeight.Bold)
            Text("₹${formatAmount(amt)}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.ExtraBold)
        } else {
            Text("Total Spent", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Text("₹${formatAmount(total)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
        }
    }
}

// ------------------------------------------------------------------------------------
// CUSTOM SMOOTH LINE CHART WITH FADE GRADIENT UNDERNEATH IN COMPOSE CANVAS
// ------------------------------------------------------------------------------------
@Composable
fun SpendingLineChart(transactions: List<Transaction>) {
    if (transactions.isEmpty()) return

    // Group transactions by simple calendar date key, sorted ascending
    val df = SimpleDateFormat("dd/MM", Locale.getDefault())
    val dateGroups = transactions.groupBy { df.format(it.timestamp) }
        .mapValues { it.value.sumOf { tx -> tx.amount } }
        .toList()
        .sortedBy { it.first }
        .takeLast(7) // Last 7 data points for neat presentation

    if (dateGroups.size < 2) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Need more transaction dates to display line graph.", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f), style = MaterialTheme.typography.bodySmall)
        }
        return
    }

    val maxVal = dateGroups.maxOf { it.second }.toFloat().coerceAtLeast(1f)

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val stepX = width / (dateGroups.size - 1)

        val points = dateGroups.mapIndexed { idx, (_, amt) ->
            val x = idx * stepX
            val y = height - (amt.toFloat() / maxVal * (height * 0.75f)) - (height * 0.1f)
            Offset(x, y)
        }

        // Draw background gradient fill under line
        val fillPath = Path().apply {
            moveTo(0f, height)
            lineTo(points.first().x, points.first().y)
            for (i in 1 until points.size) {
                lineTo(points[i].x, points[i].y)
            }
            lineTo(points.last().x, height)
            close()
        }
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(EmeraldGreen.copy(alpha = 0.3f), Color.Transparent),
                startY = 0f,
                endY = height
            )
        )

        // Draw Line
        val linePath = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) {
                lineTo(points[i].x, points[i].y)
            }
        }
        drawPath(
            path = linePath,
            color = EmeraldGreen,
            style = Stroke(width = 4.dp.toPx())
        )

        // Draw Dots and Data Labels
        points.forEachIndexed { index, pt ->
            drawCircle(
                color = EmeraldGreen,
                radius = 5.dp.toPx(),
                center = pt
            )
            drawCircle(
                color = Color.White,
                radius = 2.dp.toPx(),
                center = pt
            )
        }
    }
}

// ------------------------------------------------------------------------------------
// DUAL PARAMETER CHART PREPROCESSORS & COMPONENT
// ------------------------------------------------------------------------------------
fun prepareIncomeExpenseData(transactions: List<Transaction>): Triple<List<String>, List<Double>, List<Double>> {
    val paidTxs = transactions.filter { it.paymentStatus == "PAID" }
    val df = SimpleDateFormat("dd/MM", Locale.getDefault())
    
    // Get unique dates sorted chronologically
    val dates = paidTxs.map { df.format(it.timestamp) }.distinct().sortedBy { date ->
        paidTxs.first { tx -> df.format(tx.timestamp) == date }.timestamp
    }.takeLast(7)
    
    val incomes = dates.map { date ->
        paidTxs.filter { df.format(it.timestamp) == date && it.type == "INCOME" }.sumOf { it.amount }
    }
    
    val expenses = dates.map { date ->
        paidTxs.filter { df.format(it.timestamp) == date && it.type == "EXPENSE" }.sumOf { it.amount }
    }
    
    return Triple(dates, incomes, expenses)
}

fun prepareBankCashData(transactions: List<Transaction>): Triple<List<String>, List<Double>, List<Double>> {
    val paidTxs = transactions.filter { it.paymentStatus == "PAID" && it.type == "EXPENSE" }
    val df = SimpleDateFormat("dd/MM", Locale.getDefault())
    
    val dates = paidTxs.map { df.format(it.timestamp) }.distinct().sortedBy { date ->
        paidTxs.first { tx -> df.format(tx.timestamp) == date }.timestamp
    }.takeLast(7)
    
    val bankExpenses = dates.map { date ->
        paidTxs.filter { df.format(it.timestamp) == date && it.paymentMethod == "BANK" }.sumOf { it.amount }
    }
    
    val cashExpenses = dates.map { date ->
        paidTxs.filter { df.format(it.timestamp) == date && it.paymentMethod == "CASH" }.sumOf { it.amount }
    }
    
    return Triple(dates, bankExpenses, cashExpenses)
}

fun prepareCategoryComparisonData(transactions: List<Transaction>): Triple<List<String>, List<Double>, List<Double>> {
    val paidTxs = transactions.filter { it.paymentStatus == "PAID" && it.type == "EXPENSE" }
    
    val now = Calendar.getInstance()
    val thisMonthNum = now.get(Calendar.MONTH)
    val thisYearNum = now.get(Calendar.YEAR)
    
    val lastMonthCal = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
    val lastMonthNum = lastMonthCal.get(Calendar.MONTH)
    val lastYearNum = lastMonthCal.get(Calendar.YEAR)
    
    val thisMonthTxs = paidTxs.filter { 
        val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
        cal.get(Calendar.MONTH) == thisMonthNum && cal.get(Calendar.YEAR) == thisYearNum
    }
    
    val lastMonthTxs = paidTxs.filter { 
        val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
        cal.get(Calendar.MONTH) == lastMonthNum && cal.get(Calendar.YEAR) == lastYearNum
    }
    
    val categories = listOf("Food", "Travel", "Shopping", "Bills", "Health", "Entertainment")
    
    val thisMonthSpending = categories.map { cat ->
        thisMonthTxs.filter { it.category == cat }.sumOf { it.amount }
    }
    
    val lastMonthSpending = categories.map { cat ->
        lastMonthTxs.filter { it.category == cat }.sumOf { it.amount }
    }
    
    return Triple(categories, thisMonthSpending, lastMonthSpending)
}

@Composable
fun FinancialComparisonCard(transactions: List<Transaction>) {
    var selectedCompareMode by remember { mutableStateOf("Income vs Expense") }
    var selectedChartType by remember { mutableStateOf("Bar") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Compare Parameters",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    horizontalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    val types = listOf("Bar", "Line")
                    types.forEach { type ->
                        val active = selectedChartType == type
                        Box(
                            modifier = Modifier
                                .background(if (active) EmeraldGreen else Color.Transparent)
                                .clickable { selectedChartType = type }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                type,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val modes = listOf("Income vs Expense", "Bank vs Cash", "Category Comparison")
                modes.forEach { mode ->
                    val active = selectedCompareMode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (active) EmeraldGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, if (active) EmeraldGreen else Color.Transparent, RoundedCornerShape(8.dp))
                            .clickable { selectedCompareMode = mode }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (mode) {
                                "Income vs Expense" -> "Inc vs Exp"
                                "Bank vs Cash" -> "Bank vs Cash"
                                "Category Comparison" -> "Cat Compare"
                                else -> mode
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (active) EmeraldGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                when (selectedCompareMode) {
                    "Income vs Expense" -> {
                        val (labels, data1, data2) = prepareIncomeExpenseData(transactions)
                        if (labels.isEmpty() || (data1.all { it == 0.0 } && data2.all { it == 0.0 })) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No comparison data yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                        } else {
                            DualParameterChart(
                                labels = labels,
                                dataset1 = data1,
                                dataset2 = data2,
                                legend1 = "Income",
                                legend2 = "Expense",
                                color1 = HighDensitySuccess,
                                color2 = CrimsonRed,
                                isLineChart = selectedChartType == "Line"
                            )
                        }
                    }
                    "Bank vs Cash" -> {
                        val (labels, data1, data2) = prepareBankCashData(transactions)
                        if (labels.isEmpty() || (data1.all { it == 0.0 } && data2.all { it == 0.0 })) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No payment mode data yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                        } else {
                            DualParameterChart(
                                labels = labels,
                                dataset1 = data1,
                                dataset2 = data2,
                                legend1 = "Bank/UPI",
                                legend2 = "Cash",
                                color1 = EmeraldGreen,
                                color2 = Color(0xFFF4B400),
                                isLineChart = selectedChartType == "Line"
                            )
                        }
                    }
                    "Category Comparison" -> {
                        val (labels, data1, data2) = prepareCategoryComparisonData(transactions)
                        if (labels.isEmpty() || (data1.all { it == 0.0 } && data2.all { it == 0.0 })) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No category data for this month vs last month.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                        } else {
                            DualParameterChart(
                                labels = labels,
                                dataset1 = data1,
                                dataset2 = data2,
                                legend1 = "This Month",
                                legend2 = "Last Month",
                                color1 = EmeraldGreen,
                                color2 = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                isLineChart = selectedChartType == "Line"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DualParameterChart(
    labels: List<String>,
    dataset1: List<Double>,
    dataset2: List<Double>,
    legend1: String,
    legend2: String,
    color1: Color,
    color2: Color,
    isLineChart: Boolean
) {
    val maxVal = (dataset1 + dataset2).maxOrNull()?.toFloat()?.coerceAtLeast(1f) ?: 1f
    var hoveredIndex by remember { mutableStateOf(-1) }
    
    val gridLineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val axisLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = axisLabelColor, fontSize = 9.sp)
    val primaryColor = MaterialTheme.colorScheme.primary

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color1))
            Spacer(modifier = Modifier.width(4.dp))
            Text(legend1, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.width(16.dp))
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color2))
            Spacer(modifier = Modifier.width(4.dp))
            Text(legend2, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }

        Canvas(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .pointerInput(labels, dataset1, dataset2) {
                    detectTapGestures { offset ->
                        val totalPoints = labels.size
                        if (totalPoints > 0) {
                            val chartWidth = size.width - 60.dp.toPx()
                            val xStep = chartWidth / if (totalPoints > 1) (totalPoints - 1).toFloat() else 1f
                            val index = ((offset.x - 45.dp.toPx() + xStep / 2) / xStep).toInt().coerceIn(0, totalPoints - 1)
                            hoveredIndex = if (index == hoveredIndex) -1 else index
                        }
                    }
                }
        ) {
            val width = size.width
            val height = size.height
            val leftPadding = 45.dp.toPx()
            val rightPadding = 15.dp.toPx()
            val topPadding = 15.dp.toPx()
            val bottomPadding = 25.dp.toPx()
            
            val chartWidth = width - leftPadding - rightPadding
            val chartHeight = height - topPadding - bottomPadding

            val gridLines = 3
            for (i in 0..gridLines) {
                val ratio = i.toFloat() / gridLines
                val y = topPadding + chartHeight * (1f - ratio)
                
                drawLine(
                    color = gridLineColor,
                    start = Offset(leftPadding, y),
                    end = Offset(width - rightPadding, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )

                val value = ratio * maxVal
                val valueText = formatAmount(value.toDouble())
                drawText(
                    textMeasurer = textMeasurer,
                    text = "₹$valueText",
                    style = labelStyle,
                    topLeft = Offset(4.dp.toPx(), y - 8.dp.toPx())
                )
            }

            val totalPoints = labels.size
            if (totalPoints == 0) return@Canvas

            val xPoints = FloatArray(totalPoints) { i ->
                if (totalPoints > 1) {
                    leftPadding + i * (chartWidth / (totalPoints - 1))
                } else {
                    leftPadding + chartWidth / 2
                }
            }

            if (isLineChart) {
                val points1 = dataset1.mapIndexed { idx, v ->
                    val x = xPoints[idx]
                    val y = topPadding + chartHeight * (1f - (v.toFloat() / maxVal))
                    Offset(x, y)
                }

                val points2 = dataset2.mapIndexed { idx, v ->
                    val x = xPoints[idx]
                    val y = topPadding + chartHeight * (1f - (v.toFloat() / maxVal))
                    Offset(x, y)
                }

                if (points1.size > 1) {
                    val path1 = Path().apply {
                        moveTo(xPoints.first(), topPadding + chartHeight)
                        points1.forEach { lineTo(it.x, it.y) }
                        lineTo(xPoints.last(), topPadding + chartHeight)
                        close()
                    }
                    drawPath(
                        path = path1,
                        brush = Brush.verticalGradient(
                            colors = listOf(color1.copy(alpha = 0.2f), Color.Transparent),
                            startY = topPadding,
                            endY = topPadding + chartHeight
                        )
                    )
                }

                if (points2.size > 1) {
                    val path2 = Path().apply {
                        moveTo(xPoints.first(), topPadding + chartHeight)
                        points2.forEach { lineTo(it.x, it.y) }
                        lineTo(xPoints.last(), topPadding + chartHeight)
                        close()
                    }
                    drawPath(
                        path = path2,
                        brush = Brush.verticalGradient(
                            colors = listOf(color2.copy(alpha = 0.2f), Color.Transparent),
                            startY = topPadding,
                            endY = topPadding + chartHeight
                        )
                    )
                }

                if (points1.size > 1) {
                    val linePath1 = Path().apply {
                        moveTo(points1.first().x, points1.first().y)
                        for (i in 1 until points1.size) {
                            lineTo(points1[i].x, points1[i].y)
                        }
                    }
                    drawPath(path = linePath1, color = color1, style = Stroke(width = 3.dp.toPx()))
                }

                if (points2.size > 1) {
                    val linePath2 = Path().apply {
                        moveTo(points2.first().x, points2.first().y)
                        for (i in 1 until points2.size) {
                            lineTo(points2[i].x, points2[i].y)
                        }
                    }
                    drawPath(path = linePath2, color = color2, style = Stroke(width = 3.dp.toPx()))
                }

                points1.forEachIndexed { idx, pt ->
                    val isHovered = idx == hoveredIndex
                    drawCircle(
                        color = color1,
                        radius = if (isHovered) 7.dp.toPx() else 4.dp.toPx(),
                        center = pt
                    )
                    drawCircle(
                        color = Color.White,
                        radius = if (isHovered) 3.dp.toPx() else 1.5.dp.toPx(),
                        center = pt
                    )
                }

                points2.forEachIndexed { idx, pt ->
                    val isHovered = idx == hoveredIndex
                    drawCircle(
                        color = color2,
                        radius = if (isHovered) 7.dp.toPx() else 4.dp.toPx(),
                        center = pt
                    )
                    drawCircle(
                        color = Color.White,
                        radius = if (isHovered) 3.dp.toPx() else 1.5.dp.toPx(),
                        center = pt
                    )
                }

            } else {
                val groupWidth = if (totalPoints > 1) (xPoints[1] - xPoints[0]) else chartWidth
                val barWidth = (groupWidth * 0.25f).coerceIn(4.dp.toPx(), 16.dp.toPx())
                val spaceBetween = 2.dp.toPx()

                dataset1.forEachIndexed { idx, v1 ->
                    val v2 = dataset2[idx]
                    val groupCenter = xPoints[idx]
                    
                    val barHeight1 = (v1.toFloat() / maxVal) * chartHeight
                    val barHeight2 = (v2.toFloat() / maxVal) * chartHeight
                    
                    val x1 = groupCenter - barWidth - spaceBetween / 2
                    val y1 = topPadding + chartHeight - barHeight1
                    
                    val x2 = groupCenter + spaceBetween / 2
                    val y2 = topPadding + chartHeight - barHeight2

                    if (barHeight1 > 0) {
                        drawRoundRect(
                            color = color1,
                            topLeft = Offset(x1, y1),
                            size = Size(barWidth, barHeight1),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                    }

                    if (barHeight2 > 0) {
                        drawRoundRect(
                            color = color2,
                            topLeft = Offset(x2, y2),
                            size = Size(barWidth, barHeight2),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                    }

                    if (idx == hoveredIndex) {
                        drawRoundRect(
                            color = color1.copy(alpha = 0.08f),
                            topLeft = Offset(groupCenter - groupWidth / 2, topPadding),
                            size = Size(groupWidth, chartHeight),
                            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                        )
                    }
                }
            }

            labels.forEachIndexed { idx, label ->
                val x = xPoints[idx]
                val textLayoutResult = textMeasurer.measure(label, labelStyle)
                drawText(
                    textLayoutResult = textLayoutResult,
                    topLeft = Offset(x - textLayoutResult.size.width / 2, topPadding + chartHeight + 6.dp.toPx())
                )
            }

            if (hoveredIndex >= 0 && hoveredIndex < totalPoints) {
                val hX = xPoints[hoveredIndex]
                drawLine(
                    color = primaryColor.copy(alpha = 0.4f),
                    start = Offset(hX, topPadding),
                    end = Offset(hX, topPadding + chartHeight),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                )
            }
        }

        if (hoveredIndex >= 0 && hoveredIndex < labels.size) {
            val title = labels[hoveredIndex]
            val val1 = dataset1[hoveredIndex]
            val val2 = dataset2[hoveredIndex]
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("$legend1: ₹${formatAmount(val1)}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = color1)
                    Text("$legend2: ₹${formatAmount(val2)}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = color2)
                }
            }
        }
    }
}

// ------------------------------------------------------------------------------------
// QR BARCODE ANALYZER FOR REAL CAMERA SCANS
// ------------------------------------------------------------------------------------
class QrAnalyzer(private val onQrCodeScanned: (String) -> Unit) : ImageAnalysis.Analyzer {
    private val scanner = BarcodeScanning.getClient()

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        val rawValue = barcode.rawValue
                        if (!rawValue.isNullOrBlank()) {
                            onQrCodeScanned(rawValue)
                            break
                        }
                    }
                }
                .addOnFailureListener {
                    it.printStackTrace()
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
}

@Composable
fun QrCameraPreview(
    modifier: Modifier = Modifier,
    onQrCodeScanned: (String) -> Unit,
    flashEnabled: Boolean = false
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var cameraInstance by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }

    LaunchedEffect(flashEnabled, cameraInstance) {
        try {
            cameraInstance?.cameraControl?.enableTorch(flashEnabled)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    DisposableEffect(lifecycleOwner) {
        onDispose {
            try {
                cameraInstance?.cameraControl?.enableTorch(false)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                if (cameraProviderFuture.isDone) {
                    cameraProviderFuture.get().unbindAll()
                }
            } catch (e: Exception) {
                // Ignore disposal errors
            }
        }
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                imageAnalysis.setAnalyzer(
                    ContextCompat.getMainExecutor(ctx),
                    QrAnalyzer(onQrCodeScanned)
                )

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    val camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                    cameraInstance = camera
                    camera.cameraControl.enableTorch(flashEnabled)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(context))

            previewView
        },
        modifier = modifier
    )
}

// ------------------------------------------------------------------------------------
// IMAGES LOCAL PERSISTENCE UTILITIES
// ------------------------------------------------------------------------------------
fun saveImageLocally(context: Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val receiptsDir = File(context.filesDir, "receipts")
        receiptsDir.mkdirs()
        val filename = "receipt_${System.currentTimeMillis()}.jpg"
        val destFile = File(receiptsDir, filename)
        FileOutputStream(destFile).use { fos ->
            inputStream.copyTo(fos)
        }
        destFile.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun saveBitmapLocally(context: Context, bitmap: Bitmap): String? {
    return try {
        val receiptsDir = File(context.filesDir, "receipts")
        receiptsDir.mkdirs()
        val filename = "receipt_${System.currentTimeMillis()}.jpg"
        val destFile = File(receiptsDir, filename)
        FileOutputStream(destFile).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
        }
        destFile.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun loadBitmapFromPath(context: Context, path: String): Bitmap? {
    return try {
        val file = File(path)
        if (!file.exists()) return null

        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(file.absolutePath, options)

        var inSampleSize = 1
        val maxTargetDim = 2800
        while (options.outWidth / inSampleSize > maxTargetDim || options.outHeight / inSampleSize > maxTargetDim) {
            inSampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
        }
        val decoded = BitmapFactory.decodeFile(file.absolutePath, decodeOptions) ?: return null

        val rotation = try {
            val exif = android.media.ExifInterface(file.absolutePath)
            when (exif.getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, android.media.ExifInterface.ORIENTATION_NORMAL)) {
                android.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                android.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                android.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } catch (e: Exception) {
            0f
        }

        if (rotation != 0f) {
            val matrix = android.graphics.Matrix().apply { postRotate(rotation) }
            Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        } else {
            decoded
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * Creates a high-resolution temporary camera file and returns its FileProvider content Uri and absolute path.
 */
fun createCameraTempUri(context: Context): Pair<Uri, String>? {
    return try {
        val receiptsDir = File(context.filesDir, "receipts")
        if (!receiptsDir.exists()) receiptsDir.mkdirs()
        val tempFile = File(receiptsDir, "camera_capture_${System.currentTimeMillis()}.jpg")
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempFile
        )
        Pair(uri, tempFile.absolutePath)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

// ------------------------------------------------------------------------------------
// QR GENERATOR UTILITIES
// ------------------------------------------------------------------------------------
fun generateQrCode(text: String, width: Int = 512, height: Int = 512): Bitmap? {
    return try {
        val bitMatrix: BitMatrix = MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, width, height)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        bitmap
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun shareQrCodeImage(context: Context, qrBitmap: Bitmap) {
    try {
        val cachePath = File(context.cacheDir, "images")
        cachePath.mkdirs()
        val stream = FileOutputStream("$cachePath/upi_qr.png")
        qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.close()

        val imageFile = File(cachePath, "upi_qr.png")
        val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", imageFile)

        if (contentUri != null) {
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                setDataAndType(contentUri, context.contentResolver.getType(contentUri))
                putExtra(Intent.EXTRA_STREAM, contentUri)
                type = "image/png"
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share UPI QR Code"))
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

// ------------------------------------------------------------------------------------
// FORMATTING UTILITIES
// ------------------------------------------------------------------------------------
fun formatAmount(amount: Double): String {
    return if (amount % 1.0 == 0.0) {
        amount.toLong().toString()
    } else {
        String.format(Locale.getDefault(), "%.2f", amount)
    }
}

object SoundEffectManager {
    private var activeMediaPlayer: MediaPlayer? = null
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun play(context: Context) {
        init(context)
        try {
            activeMediaPlayer?.let { player ->
                try {
                    if (player.isPlaying) {
                        player.stop()
                    }
                    player.release()
                } catch (e: Exception) {
                    // Ignore release errors
                }
                activeMediaPlayer = null
            }

            activeMediaPlayer = MediaPlayer.create(context.applicationContext, com.example.R.raw.money_counting)?.apply {
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                setAudioAttributes(audioAttributes)
                setOnCompletionListener { mp ->
                    try {
                        mp.release()
                        if (activeMediaPlayer == mp) activeMediaPlayer = null
                    } catch (e: Exception) {
                        // Ignore
                    }
                }
                setOnErrorListener { mp, _, _ ->
                    try {
                        mp.release()
                        if (activeMediaPlayer == mp) activeMediaPlayer = null
                    } catch (e: Exception) {
                        // Ignore
                    }
                    true
                }
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playDoubleTapSound(context: Context? = null) {
        // Fix for duplicate transaction double-tap: do NOT play current balance money_counting SFX!
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 75)
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 100)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try {
                    toneGen.release()
                } catch (_: Exception) {}
            }, 160)
        } catch (e: Exception) {
            try {
                val targetContext = context ?: appContext
                val am = targetContext?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                am?.playSoundEffect(AudioManager.FX_KEY_CLICK, 0.7f)
            } catch (_: Exception) {}
        }
    }
}

fun playSoundEffect(context: Context) {
    try {
        SoundEffectManager.play(context)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun formatTimestamp(timestamp: Long): String {
    val date = Date(timestamp)
    val now = Calendar.getInstance()
    val txDate = Calendar.getInstance().apply { timeInMillis = timestamp }

    return when {
        now.get(Calendar.YEAR) == txDate.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == txDate.get(Calendar.DAY_OF_YEAR) -> {
            SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)
        }
        now.get(Calendar.YEAR) == txDate.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) - txDate.get(Calendar.DAY_OF_YEAR) == 1 -> {
            "Yesterday"
        }
        else -> {
            SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
        }
    }
}

fun isWithinDays(timestamp: Long, days: Int): Boolean {
    val cutoff = System.currentTimeMillis() - (days * 24L * 60L * 60L * 1000L)
    return timestamp >= cutoff
}

fun isSameDay(t1: Long, t2: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = t1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = t2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

// ------------------------------------------------------------------------------------
// CUSTOM APP LOGO GRAPHIC (VECTOR BASED)
// ------------------------------------------------------------------------------------
@Composable
fun AppLogoGraphic(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = com.example.R.drawable.app_logo),
        contentDescription = "App Logo",
        modifier = modifier
    )
}

// ------------------------------------------------------------------------------------
// CUSTOM CREATOR PORTRAIT GRAPHIC (VECTOR BASED ILLUSTRATION OF S DIVYA DARSHAN)
// ------------------------------------------------------------------------------------
@Composable
fun CreatorPortraitGraphic(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = com.example.R.drawable.dev_portrait),
        contentDescription = "S Divya Darshan",
        modifier = modifier,
        contentScale = androidx.compose.ui.layout.ContentScale.Crop
    )
}

// ------------------------------------------------------------------------------------
// PREMIUM SPLASH SCREEN COMPOSABLE
// ------------------------------------------------------------------------------------
@Composable
fun SplashScreen() {
    val context = LocalContext.current
    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                if (android.os.Build.VERSION.SDK_INT >= 28) {
                    add(coil.decode.ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }

    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data(com.example.R.raw.splash_gif)
            .allowHardware(false)
            .crossfade(false)
            .build(),
        imageLoader = imageLoader
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF12151A)), // Dark matching background
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painter,
            contentDescription = "Splash GIF Animation",
            modifier = Modifier.fillMaxSize(),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop
        )
    }
}

// ------------------------------------------------------------------------------------
// GENERAL FILE OFFLINE RESOLVER & DOWNLOAD UTILITY
// ------------------------------------------------------------------------------------
fun getOfflineFile(context: Context, fileName: String): File {
    val dir = context.getExternalFilesDir(null) ?: context.filesDir
    return File(dir, fileName)
}

fun downloadFileOffline(context: Context, fileUrl: String, fileName: String, onComplete: (File?) -> Unit = {}) {
    val targetFile = getOfflineFile(context, fileName)
    if (targetFile.exists() && targetFile.length() > 0) {
        onComplete(targetFile)
        return
    }
    
    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
        try {
            val url = java.net.URL(fileUrl)
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.connect()
            if (connection.responseCode == java.net.HttpURLConnection.HTTP_OK) {
                val inputStream = connection.inputStream
                val dir = context.getExternalFilesDir(null) ?: context.filesDir
                val tempFile = File(dir, "$fileName.tmp")
                FileOutputStream(tempFile).use { outputStream ->
                    val buffer = ByteArray(4096)
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                    }
                }
                inputStream.close()
                tempFile.renameTo(targetFile)
                onComplete(targetFile)
            } else {
                onComplete(null)
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            onComplete(null)
        }
    }
}

// ------------------------------------------------------------------------------------
// OFFLINE ASSETS DOWNLOADING PROGRESS SCREEN (SHOWN AFTER FIRST SETUP COMPLETED)
// ------------------------------------------------------------------------------------
@Composable
fun AssetDownloadScreen(onComplete: () -> Unit) {
    val context = LocalContext.current
    var progress by remember { mutableStateOf(0f) }
    var statusText by remember { mutableStateOf("Initializing secure storage...") }

    LaunchedEffect(Unit) {
        // Pre-init sound pool
        SoundEffectManager.init(context)
        
        // Start downloading assets in parallel in the background
        val videoUrl = "https://www.image2url.com/r2/default/videos/1783157762794-f8aa8214-09c6-4ef7-ba39-06b9daf66b24.mp4"
        val gifUrl = "https://www.image2url.com/r2/default/gifs/1783154865887-c073b625-3318-47fb-bf68-01faf2bc6996.gif"
        
        downloadFileOffline(context, videoUrl, "zero_balance_video.mp4")
        downloadFileOffline(context, gifUrl, "splash_gif.gif")
        
        // Smoothly animate progress bar over ~7.5 seconds
        val totalSteps = 100
        val delayPerStep = 75L
        for (i in 1..totalSteps) {
            progress = i / 100f
            statusText = when {
                i < 15 -> "Initializing secure storage..."
                i < 35 -> "Preloading transaction SFX..."
                i < 65 -> "Caching offline zero balance video..."
                i < 85 -> "Optimizing splash screen GIF..."
                else -> "Finalizing setup, welcome to your secure workspace!"
            }
            kotlinx.coroutines.delay(delayPerStep)
        }
        
        onComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF12151A)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            AppLogoGraphic(modifier = Modifier.size(100.dp))
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "Setting Up Your Workspace",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Caching offline assets for instant, lag-free experience.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.Gray
                ),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(48.dp))
            
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = Color(0xFF05C492),
                trackColor = Color(0xFF232B35)
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFF05C492),
                    fontWeight = FontWeight.Medium
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.labelLarge.copy(
                    color = Color.LightGray
                )
            )
        }
    }
}

// ------------------------------------------------------------------------------------
// OFFLINE ZERO BALANCE VIDEO POPUP COMPOSABLE
// ------------------------------------------------------------------------------------
@Composable
fun ZeroBalanceVideoPopup(
    customVideoPath: String? = null,
    onDismiss: () -> Unit
) {
    var videoView: VideoView? = null
    var isPrepared by remember { mutableStateOf(false) }
    var isPlaybackError by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    DisposableEffect(Unit) {
        onDispose {
            try {
                videoView?.stopPlayback()
                videoView?.suspend()
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)),
        contentAlignment = Alignment.Center
    ) {
        if (!isPlaybackError) {
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        videoView = this
                        layoutParams = android.widget.FrameLayout.LayoutParams(
                            android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                            android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                        )
                        
                        try {
                            val customFile = customVideoPath?.let { java.io.File(it) }
                            if (customFile != null && customFile.exists() && customFile.length() > 0) {
                                setVideoPath(customFile.absolutePath)
                            } else {
                                val rawUri = android.net.Uri.parse("android.resource://" + ctx.packageName + "/" + com.example.R.raw.zero_balance_video)
                                setVideoURI(rawUri)
                            }
                            
                            setOnPreparedListener { mediaPlayer ->
                                mediaPlayer.isLooping = true
                                mediaPlayer.start()
                                isPrepared = true
                            }
                            setOnErrorListener { _, what, extra ->
                                isPlaybackError = true
                                isPrepared = true
                                true // Consumed
                            }
                        } catch (e: Exception) {
                            isPlaybackError = true
                            isPrepared = true
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            if (!isPrepared) {
                CircularProgressIndicator(
                    color = EmeraldGreen,
                    modifier = Modifier.size(48.dp)
                )
            }
        } else {
            // Elegant Zero Balance Fallback Banner Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(EmeraldGreen.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("₹0", color = EmeraldGreen, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Zero Balance Alert",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "All set! Your net registered balance is currently ₹0.00. Add transactions or top up your account to get started.",
                        color = Color(0xFF94A3B8),
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                    ) {
                        Text("Got It", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Close button 'X' on top-right
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(24.dp)
                .size(48.dp)
                .background(Color.White.copy(alpha = 0.85f), shape = CircleShape)
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Close Video",
                tint = Color.Black,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

