package com.example

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppAnnouncement
import com.example.data.AppDatabase
import com.example.data.FinanceRepository
import com.example.data.Preference
import com.example.data.Transaction
import com.example.ui.FinanceViewModel
import com.example.ui.UpiQrCode
import com.example.ui.components.MultiUpiQrViewerDialog
import com.example.ui.screens.AnalyticsDashboardScreen
import com.example.ui.screens.AppTab
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.generateQrCode
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class AppShowcaseScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var repository: FinanceRepository
    private lateinit var viewModel: FinanceViewModel
    private lateinit var sampleQrPath1: String
    private lateinit var sampleQrPath2: String

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        val txDao = database.transactionDao()
        val prefDao = database.preferenceDao()
        val annDao = database.announcementDao()
        repository = FinanceRepository(txDao, prefDao, annDao)

        // Create crisp sample UPI QR PNG files in cacheDir
        val qrDir = File(context.cacheDir, "sample_qrs").apply { mkdirs() }
        val qrFile1 = File(qrDir, "gpay_qr.png")
        val qrFile2 = File(qrDir, "paytm_qr.png")
        generateQrCode("upi://pay?pa=enterfirst@okaxis&pn=EnterFirst%20User&cu=INR", 512, 512)?.let { bmp ->
            FileOutputStream(qrFile1).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        generateQrCode("upi://pay?pa=enterfirst@paytm&pn=EnterFirst%20Paytm&cu=INR", 512, 512)?.let { bmp ->
            FileOutputStream(qrFile2).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        sampleQrPath1 = qrFile1.absolutePath
        sampleQrPath2 = qrFile2.absolutePath

        val now = System.currentTimeMillis()
        val hour = 3_600_000L
        val day = 24 * hour

        runBlocking {
            prefDao.insertPreference(Preference("is_setup_completed", "true"))
            prefDao.insertPreference(Preference("initial_bank_balance", "42500.0"))
            prefDao.insertPreference(Preference("initial_cash_balance", "4850.0"))
            prefDao.insertPreference(Preference("monthly_budget_limit", "25000.0"))
            prefDao.insertPreference(Preference("is_dark_theme", "true"))
            prefDao.insertPreference(Preference("is_battery_dialog_shown", "true"))
            prefDao.insertPreference(Preference("is_upi_qr_setup_done", "true"))
            prefDao.insertPreference(Preference("auto_backup_frequency", "DAILY"))
            prefDao.insertPreference(Preference("google_email", "user@enterfirst.app"))
            prefDao.insertPreference(Preference("google_name", "Aarav Sharma"))

            val qrJson = """
                [
                  {"id":"qr_1","appName":"Google Pay","imagePath":"$sampleQrPath1"},
                  {"id":"qr_2","appName":"Paytm","imagePath":"$sampleQrPath2"}
                ]
            """.trimIndent()
            prefDao.insertPreference(Preference("user_upi_qrs_json", qrJson))

            annDao.insertAnnouncement(
                AppAnnouncement(
                    title = "EnterFirst v1.0 Ready",
                    message = "Multi-UPI QR Hub, Offline Ledger & Google Drive Auto-Backup active.",
                    category = "FEATURE",
                    senderEmail = "Admin",
                    priority = "NORMAL",
                    isRead = true
                )
            )

            // Sample realistic transactions
            txDao.insertTransaction(
                Transaction(
                    type = "INCOME",
                    amount = 35000.0,
                    party = "TechNova Solutions",
                    category = "Others",
                    note = "Monthly Freelance Payout",
                    timestamp = now - 5 * day,
                    paymentStatus = "PAID",
                    paymentMethod = "BANK"
                )
            )
            txDao.insertTransaction(
                Transaction(
                    type = "EXPENSE",
                    amount = 420.0,
                    party = "Swiggy Instamart",
                    category = "Food",
                    note = "Fresh groceries & coffee",
                    timestamp = now - 2 * hour,
                    paymentStatus = "PAID",
                    paymentMethod = "BANK",
                    merchantUpiId = "swiggy@icici"
                )
            )
            txDao.insertTransaction(
                Transaction(
                    type = "EXPENSE",
                    amount = 185.0,
                    party = "Namma Metro",
                    category = "Travel",
                    note = "Metro card recharge",
                    timestamp = now - 5 * hour,
                    paymentStatus = "PAID",
                    paymentMethod = "BANK",
                    merchantUpiId = "metro@sbi"
                )
            )
            txDao.insertTransaction(
                Transaction(
                    type = "EXPENSE",
                    amount = 2499.0,
                    party = "BESCOM Electricity",
                    category = "Bills",
                    note = "Monthly power bill",
                    timestamp = now - 2 * day,
                    paymentStatus = "PAID",
                    paymentMethod = "BANK",
                    merchantUpiId = "bescom@hdfcbank"
                )
            )
            txDao.insertTransaction(
                Transaction(
                    type = "EXPENSE",
                    amount = 3250.0,
                    party = "Reliance Trends",
                    category = "Shopping",
                    note = "Apparel & accessories",
                    timestamp = now - 3 * day,
                    paymentStatus = "PAID",
                    paymentMethod = "BANK",
                    merchantUpiId = "trends@axisbank"
                )
            )
            txDao.insertTransaction(
                Transaction(
                    type = "EXPENSE",
                    amount = 640.0,
                    party = "Apollo Pharmacy",
                    category = "Health",
                    note = "Vitamins & first aid",
                    timestamp = now - 4 * day,
                    paymentStatus = "PAID",
                    paymentMethod = "CASH"
                )
            )
            txDao.insertTransaction(
                Transaction(
                    type = "EXPENSE",
                    amount = 1150.0,
                    party = "BookMyShow IMAX",
                    category = "Entertainment",
                    note = "Weekend movie tickets",
                    timestamp = now - 6 * day,
                    paymentStatus = "PAID",
                    paymentMethod = "BANK",
                    merchantUpiId = "bms@kotak"
                )
            )
        }

        viewModel = FinanceViewModel(repository)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Composable
    private fun ShowcaseScaffold(
        selectedTab: AppTab,
        content: @Composable () -> Unit
    ) {
        MyApplicationTheme(darkTheme = true) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 1.dp
                    ) {
                        val navBarColors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        NavigationBarItem(
                            selected = selectedTab == AppTab.Home,
                            onClick = {},
                            icon = { Icon(if (selectedTab == AppTab.Home) Icons.Filled.Home else Icons.Outlined.Home, contentDescription = "Home") },
                            label = { Text("Home", fontWeight = if (selectedTab == AppTab.Home) FontWeight.Bold else FontWeight.Normal, fontSize = 11.sp) },
                            colors = navBarColors
                        )
                        NavigationBarItem(
                            selected = selectedTab == AppTab.History,
                            onClick = {},
                            icon = { Icon(if (selectedTab == AppTab.History) Icons.AutoMirrored.Filled.List else Icons.AutoMirrored.Outlined.List, contentDescription = "History") },
                            label = { Text("History", fontWeight = if (selectedTab == AppTab.History) FontWeight.Bold else FontWeight.Normal, fontSize = 11.sp) },
                            colors = navBarColors
                        )
                        NavigationBarItem(
                            selected = selectedTab == AppTab.Analytics,
                            onClick = {},
                            icon = { Icon(if (selectedTab == AppTab.Analytics) Icons.Filled.PieChart else Icons.Outlined.PieChart, contentDescription = "Analytics") },
                            label = { Text("Analytics", fontWeight = if (selectedTab == AppTab.Analytics) FontWeight.Bold else FontWeight.Normal, fontSize = 11.sp) },
                            colors = navBarColors
                        )
                        NavigationBarItem(
                            selected = selectedTab == AppTab.Settings,
                            onClick = {},
                            icon = { Icon(if (selectedTab == AppTab.Settings) Icons.Filled.Settings else Icons.Outlined.Settings, contentDescription = "Settings") },
                            label = { Text("Settings", fontWeight = if (selectedTab == AppTab.Settings) FontWeight.Bold else FontWeight.Normal, fontSize = 11.sp) },
                            colors = navBarColors
                        )
                        NavigationBarItem(
                            selected = selectedTab == AppTab.About,
                            onClick = {},
                            icon = { Icon(if (selectedTab == AppTab.About) Icons.Filled.Person else Icons.Outlined.Person, contentDescription = "About") },
                            label = { Text("About", fontWeight = if (selectedTab == AppTab.About) FontWeight.Bold else FontWeight.Normal, fontSize = 11.sp) },
                            colors = navBarColors
                        )
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(innerPadding)
                ) {
                    content()
                }
            }
        }
    }

    private fun settleUi() {
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(800)
        composeTestRule.waitForIdle()
    }

    @Test
    fun capture_home_dashboard() {
        composeTestRule.setContent {
            ShowcaseScaffold(selectedTab = AppTab.Home) {
                DashboardScreen(
                    viewModel = viewModel,
                    onAddClick = {},
                    onScanClick = {},
                    onSeeAllHistory = {},
                    onSettingsClick = {},
                    onAnalyticsClick = {},
                    onEditClick = {}
                )
            }
        }
        settleUi()
        composeTestRule.onRoot().captureRoboImage(filePath = "../docs/images/screenshot_home.png")
    }

    @Test
    fun capture_upi_qr_hub() {
        composeTestRule.setContent {
            ShowcaseScaffold(selectedTab = AppTab.Home) {
                DashboardScreen(
                    viewModel = viewModel,
                    onAddClick = {},
                    onScanClick = {},
                    onSeeAllHistory = {},
                    onSettingsClick = {},
                    onAnalyticsClick = {},
                    onEditClick = {}
                )
                MultiUpiQrViewerDialog(
                    upiQrs = listOf(
                        UpiQrCode(id = "qr_1", appName = "Google Pay", imagePath = sampleQrPath1),
                        UpiQrCode(id = "qr_2", appName = "Paytm", imagePath = sampleQrPath2)
                    ),
                    onDismiss = {},
                    onEditClicked = {}
                )
            }
        }
        settleUi()
        composeTestRule.onNode(isDialog()).captureRoboImage(filePath = "../docs/images/screenshot_upi_qr_hub.png")
    }

    @Test
    fun capture_history_screen() {
        composeTestRule.setContent {
            ShowcaseScaffold(selectedTab = AppTab.History) {
                HistoryScreen(
                    viewModel = viewModel,
                    onEditClick = {}
                )
            }
        }
        settleUi()
        composeTestRule.onRoot().captureRoboImage(filePath = "../docs/images/screenshot_history.png")
    }

    @Test
    fun capture_analytics_screen() {
        composeTestRule.setContent {
            ShowcaseScaffold(selectedTab = AppTab.Analytics) {
                AnalyticsDashboardScreen(
                    viewModel = viewModel,
                    onBackClick = {}
                )
            }
        }
        settleUi()
        // Switch from WebView Recharts to Compose Native Donut Chart so Robolectric renders it crisply
        composeTestRule.onNodeWithText("Recharts").performClick()
        settleUi()
        composeTestRule.onRoot().captureRoboImage(filePath = "../docs/images/screenshot_analytics.png")
    }

    @Test
    fun capture_settings_screen() {
        composeTestRule.setContent {
            ShowcaseScaffold(selectedTab = AppTab.Settings) {
                SettingsScreen(
                    viewModel = viewModel,
                    onPreviewZeroBalance = {}
                )
            }
        }
        settleUi()
        composeTestRule.onRoot().captureRoboImage(filePath = "../docs/images/screenshot_settings.png")
    }
}
