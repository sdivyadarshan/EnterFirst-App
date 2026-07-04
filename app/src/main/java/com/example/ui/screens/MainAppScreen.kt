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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
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
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import kotlinx.coroutines.launch
import androidx.compose.foundation.rememberScrollState
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
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import android.os.Build
import android.media.MediaPlayer

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
    Home, History, Settings, About
}

@Composable
fun MainAppScreen(viewModel: FinanceViewModel) {
    val isPreferencesLoaded by viewModel.isPreferencesLoaded.collectAsStateWithLifecycle()
    val isSetupCompleted by viewModel.isSetupCompleted.collectAsStateWithLifecycle()
    val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()

    var showSplash by remember { mutableStateOf(true) }

    LaunchedEffect(isPreferencesLoaded) {
        if (isPreferencesLoaded) {
            kotlinx.coroutines.delay(2000)
            showSplash = false
        }
    }

    var currentTab by remember { mutableStateOf(AppTab.Home) }
    var currentScreenState by remember { mutableStateOf("TABS") } // "TABS", "ADD_TRANSACTION", "SCANNER", "PAYMENT_WORKFLOW"

    var previousScreenState by remember { mutableStateOf("TABS") }
    var showZeroBalancePopup by remember { mutableStateOf(false) }

    val totalBalance by viewModel.currentTotalBalance.collectAsStateWithLifecycle()

    LaunchedEffect(currentScreenState) {
        if (currentScreenState == "TABS" && (previousScreenState == "ADD_TRANSACTION" || previousScreenState == "PAYMENT_WORKFLOW" || previousScreenState == "PAY_ENTRY")) {
            kotlinx.coroutines.delay(400)
            if (totalBalance == 0.0) {
                showZeroBalancePopup = true
            }
        }
        previousScreenState = currentScreenState
    }

    // Temporary values for payment workflow
    var scannedMerchantName by remember { mutableStateOf("") }
    var scannedMerchantUpiId by remember { mutableStateOf("") }
    var scannedAmount by remember { mutableStateOf("") }

    // Transaction ID for the pending payment
    var pendingTransactionId by remember { mutableStateOf<Int?>(null) }

    MyApplicationTheme(darkTheme = isDarkTheme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            if (showSplash) {
                SplashScreen()
            } else if (!isSetupCompleted) {
                SetupScreen(onSetupComplete = { bank, cash ->
                    viewModel.completeSetup(bank, cash)
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
                                            onSettingsClick = { currentTab = AppTab.Settings }
                                        )
                                        AppTab.History -> HistoryScreen(viewModel = viewModel)
                                        AppTab.Settings -> SettingsScreen(viewModel = viewModel)
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
                                onMerchantParsed = { name, upi, amt ->
                                    scannedMerchantName = name
                                    scannedMerchantUpiId = upi
                                    scannedAmount = amt ?: ""
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
                                onComplete = {
                                    currentScreenState = "TABS"
                                }
                            )
                        }
                    }

                    if (showZeroBalancePopup) {
                        ZeroBalanceVideoPopup(
                            onDismiss = { showZeroBalancePopup = false }
                        )
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------------------------
// 1. SETUP SCREEN
// ------------------------------------------------------------------------------------
@Composable
fun SetupScreen(onSetupComplete: (Double, Double) -> Unit) {
    var bankInput by remember { mutableStateOf("") }
    var cashInput by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .windowInsetsPadding(WindowInsets.statusBars),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Logo Icon Drawing
        AppLogoGraphic(
            modifier = Modifier
                .size(140.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

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

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Set your starting balances to begin local tracking.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

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

        Spacer(modifier = Modifier.height(16.dp))

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

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                val bank = if (bankInput.trim().isEmpty()) 0.0 else bankInput.toDoubleOrNull()
                val cash = if (cashInput.trim().isEmpty()) 0.0 else cashInput.toDoubleOrNull()
                if (bank == null || cash == null || bank < 0 || cash < 0) {
                    errorText = "Please enter valid positive amounts."
                } else {
                    onSetupComplete(bank, cash)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("setup_complete_button"),
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                "Complete Setup",
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
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
    onSettingsClick: () -> Unit
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

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var animatedBalance by remember { mutableStateOf<Double?>(null) }

    var showDeleteDialogFor by remember { mutableStateOf<Transaction?>(null) }

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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = com.example.R.drawable.app_logo),
                        contentDescription = "App Logo",
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Privacy first, Track next",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Normal
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "EnterFirst",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .clickable { onSettingsClick() },
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

        item {
            // Main Balance Card matching screenshots exactly with clean layout and emerald gradient
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(totalBalance) {
                        detectTapGestures(
                            onDoubleTap = {
                                coroutineScope.launch {
                                    val target = totalBalance
                                    val durationMs = 2000L
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
                    Text(
                        "Spending by Category",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

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
                    onLongClick = { showDeleteDialogFor = tx }
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
}

// ------------------------------------------------------------------------------------
// TRANSACTION ROW
// ------------------------------------------------------------------------------------
@Composable
fun TransactionRow(
    transaction: Transaction,
    customCategories: Map<String, String> = emptyMap(),
    onLongClick: () -> Unit
) {
    val dateStr = formatTimestamp(transaction.timestamp)
    val color = getCategoryColor(transaction.category)
    val emoji = getCategoryEmoji(transaction.category, customCategories)
    val context = LocalContext.current

    var showReceiptDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { onLongClick() },
                    onTap = {
                        if (transaction.receiptUri != null) {
                            showReceiptDialog = true
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
fun HistoryScreen(viewModel: FinanceViewModel) {
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val categoryOrder by viewModel.categoryOrder.collectAsStateWithLifecycle()
    val customCategories by viewModel.customCategories.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedTimeFilter by remember { mutableStateOf("All") } // "All", "Today", "Week", "Month"
    var selectedCategoryFilter by remember { mutableStateOf("All") } // "All", "Food", "Travel", etc.
    var showDeleteDialogFor by remember { mutableStateOf<Transaction?>(null) }
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
                Image(
                    painter = painterResource(id = com.example.R.drawable.app_logo),
                    contentDescription = "App Logo",
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
                        onLongClick = { showDeleteDialogFor = tx }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
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

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let {
            val localPath = saveBitmapLocally(context, it)
            if (localPath != null) {
                receiptPath = localPath
                receiptBitmap = bitmap
                Toast.makeText(context, "Photo captured successfully", Toast.LENGTH_SHORT).show()
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
                    val amt = amountInput.toDoubleOrNull()
                    if (amt == null || amt <= 0) {
                        Toast.makeText(context, "Please enter a valid amount.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (partyInput.trim().isEmpty()) {
                        Toast.makeText(context, "Please enter payee/received name.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    if (isUpiPaymentFlow && onPendingCreated != null) {
                        // Create transaction as PENDING and trigger QR page
                        val tx = com.example.data.Transaction(
                            type = "EXPENSE",
                            amount = amt,
                            party = partyInput,
                            category = categoryInput,
                            note = noteInput,
                            paymentStatus = "PENDING",
                            paymentMethod = "BANK",
                            merchantUpiId = prefilledMerchantUpi,
                            receiptUri = receiptPath
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
                            party = partyInput,
                            category = categoryInput,
                            note = noteInput,
                            paymentMethod = paymentMethod,
                            paymentStatus = "PAID",
                            receiptUri = receiptPath
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
                // Large Amount Input
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "₹",
                            fontSize = 44.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (type == "EXPENSE") CrimsonRed else EmeraldGreen,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        BasicTextField(
                            value = amountInput,
                            onValueChange = { amountInput = it },
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 48.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onBackground,
                                textAlign = TextAlign.Center
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .width(200.dp)
                                .testTag("amount_input")
                        )
                    }
                }
            }

            item {
                // Paid To / Purpose Name
                val label = if (type == "EXPENSE") "PAID TO / PURPOSE" else "RECEIVED FROM"
                val placeholder = if (type == "EXPENSE") "e.g. Swiggy, Rent, Petrol..." else "e.g. Salary, Freelance, Gift..."
                OutlinedTextField(
                    value = partyInput,
                    onValueChange = { partyInput = it },
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
                    onValueChange = { noteInput = it },
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
                        onClick = { cameraLauncher.launch(null) },
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
// 5. SCANNER SCREEN
// ------------------------------------------------------------------------------------
@Composable
fun ScannerScreen(
    onDismiss: () -> Unit,
    onMerchantParsed: (String, String, String?) -> Unit
) {
    var flashEnabled by remember { mutableStateOf(false) }
    var isManualEntry by remember { mutableStateOf(false) }
    var manualUpiId by remember { mutableStateOf("") }
    var manualMerchantName by remember { mutableStateOf("") }

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
                            // Parse upi string
                            val uri = Uri.parse(rawValue)
                            val pa = uri.getQueryParameter("pa")
                            val pn = uri.getQueryParameter("pn") ?: "Merchant"
                            val am = uri.getQueryParameter("am")
                            if (pa != null) {
                                onMerchantParsed(pn, pa, am)
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

            Button(
                onClick = { isManualEntry = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 32.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit UPI", tint = EmeraldGreen)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Enter UPI ID manually", color = Color.White)
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
                            onMerchantParsed(name, manualUpiId, null)
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
    onComplete: () -> Unit
) {
    val context = LocalContext.current

    // Construct the clean standard upi pay URI
    val upiUri = "upi://pay?pa=$merchantUpiId&pn=${Uri.encode(merchantName)}&am=$amount"
    val qrBitmap = remember(upiUri) { generateQrCode(upiUri) }

    LaunchedEffect(qrBitmap) {
        if (qrBitmap != null) {
            shareQrCodeImage(context, qrBitmap)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .windowInsetsPadding(WindowInsets.statusBars),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Paying Merchant",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                merchantName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = EmeraldGreen
            )
            Text(
                merchantUpiId,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "₹${formatAmount(amount)}",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Display generated UPI QR code image box
            Card(
                modifier = Modifier
                    .size(240.dp)
                    .padding(8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                if (qrBitmap != null) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "Generated Payment QR code image",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (qrBitmap != null) {
                        shareQrCodeImage(context, qrBitmap)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.testTag("share_qr_button")
            ) {
                Icon(Icons.Filled.Share, contentDescription = "Share", tint = EmeraldGreen)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Share QR Code", color = MaterialTheme.colorScheme.onSurface)
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Complete the payment using your shared UPI app, then confirm the status below.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Button(
                onClick = {
                    viewModel.updateTransactionStatus(pendingTxId, "PAID")
                    Toast.makeText(context, "Transaction saved as Paid", Toast.LENGTH_SHORT).show()
                    onComplete()
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("payment_success_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Payment Successful", color = Color.Black, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    viewModel.updateTransactionStatus(pendingTxId, "FAILED")
                    Toast.makeText(context, "Transaction saved as Failed", Toast.LENGTH_SHORT).show()
                    onComplete()
                },
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("payment_failed_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Payment Failed", color = Color.White, fontWeight = FontWeight.Bold)
            }
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
fun SettingsScreen(viewModel: FinanceViewModel) {
    val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val initialBank by viewModel.initialBankBalance.collectAsStateWithLifecycle()
    val initialCash by viewModel.initialCashBalance.collectAsStateWithLifecycle()
    val googleEmail by viewModel.googleEmail.collectAsStateWithLifecycle()
    val googleName by viewModel.googleName.collectAsStateWithLifecycle()
    val driveBackupStatus by viewModel.driveBackupStatus.collectAsStateWithLifecycle()

    var showBankEditDialog by remember { mutableStateOf(false) }
    var showCashEditDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var showTroubleshoot by remember { mutableStateOf(false) }
    var showResetBudgetDialog by remember { mutableStateOf(false) }

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = com.example.R.drawable.app_logo),
                contentDescription = "App Logo",
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

                    Spacer(modifier = Modifier.height(4.dp))

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
            // App Info Rows
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
                        Text("Platform", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Text("Android (iOS/Web supported)", fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Storage", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Text("Local only — no cloud sync", fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Data collected", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Text("None", fontWeight = FontWeight.Bold)
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
                        if (rawValue != null && rawValue.startsWith("upi://pay")) {
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

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
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
        if (file.exists()) {
            BitmapFactory.decodeFile(file.absolutePath)
        } else null
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

fun playSoundEffect(context: Context) {
    try {
        val mediaPlayer = MediaPlayer.create(context, com.example.R.raw.money_counting)
        mediaPlayer.setOnCompletionListener {
            it.release()
        }
        mediaPlayer.start()
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
            .clip(RoundedCornerShape(24.dp))
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
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }

    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data(com.example.R.raw.splash_gif)
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
// OFFLINE ZERO BALANCE VIDEO POPUP COMPOSABLE
// ------------------------------------------------------------------------------------
@Composable
fun ZeroBalanceVideoPopup(onDismiss: () -> Unit) {
    var videoView: VideoView? = null
    
    DisposableEffect(Unit) {
        onDispose {
            try {
                videoView?.stopPlayback()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { context ->
                VideoView(context).apply {
                    videoView = this
                    val rawId = com.example.R.raw.zero_balance_video
                    val uri = android.net.Uri.parse("android.resource://${context.packageName}/$rawId")
                    setVideoURI(uri)
                    setOnPreparedListener { mediaPlayer ->
                        mediaPlayer.isLooping = true
                        mediaPlayer.start()
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Close button 'X' on top-right
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(24.dp)
                .size(48.dp)
                .background(Color.White.copy(alpha = 0.7f), shape = CircleShape)
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

