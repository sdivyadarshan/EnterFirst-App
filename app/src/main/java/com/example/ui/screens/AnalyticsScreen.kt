package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.CrimsonRed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Transaction
import com.example.ui.FinanceViewModel
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

data class CategorySpend(
    val category: String,
    val amount: Double,
    val count: Int,
    val percentage: Float,
    val colorHex: String,
    val color: Color
)

data class MonthlyTrend(
    val monthLabel: String,
    val expense: Double,
    val income: Double
)

enum class AnalyticsPeriod(val label: String) {
    THIS_MONTH("This Month"),
    LAST_MONTH("Last Month"),
    LAST_3_MONTHS("Last 3 Months"),
    ALL_TIME("All Time")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsDashboardScreen(
    viewModel: FinanceViewModel,
    onBackClick: (() -> Unit)? = null
) {
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val customCategories by viewModel.customCategories.collectAsStateWithLifecycle()
    val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()

    var selectedPeriod by remember { mutableStateOf(AnalyticsPeriod.THIS_MONTH) }
    var selectedCategoryName by remember { mutableStateOf<String?>(null) }
    var useRechartsEngine by remember { mutableStateOf(true) }

    // Filter transactions based on selected period
    val now = remember { System.currentTimeMillis() }
    val filteredTransactions = remember(transactions, selectedPeriod) {
        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH)
        val currentYear = cal.get(Calendar.YEAR)

        when (selectedPeriod) {
            AnalyticsPeriod.THIS_MONTH -> {
                transactions.filter { tx ->
                    cal.timeInMillis = tx.timestamp
                    cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear
                }
            }
            AnalyticsPeriod.LAST_MONTH -> {
                cal.add(Calendar.MONTH, -1)
                val targetMonth = cal.get(Calendar.MONTH)
                val targetYear = cal.get(Calendar.YEAR)
                transactions.filter { tx ->
                    cal.timeInMillis = tx.timestamp
                    cal.get(Calendar.MONTH) == targetMonth && cal.get(Calendar.YEAR) == targetYear
                }
            }
            AnalyticsPeriod.LAST_3_MONTHS -> {
                val threeMonthsAgo = now - (90L * 24 * 60 * 60 * 1000L)
                transactions.filter { it.timestamp >= threeMonthsAgo }
            }
            AnalyticsPeriod.ALL_TIME -> transactions
        }
    }

    // Spending breakdown
    val expenseTransactions = remember(filteredTransactions) {
        filteredTransactions.filter { it.type == "EXPENSE" && it.paymentStatus != "FAILED" }
    }
    val incomeTransactions = remember(filteredTransactions) {
        filteredTransactions.filter { it.type == "INCOME" && it.paymentStatus != "FAILED" }
    }

    val totalExpense = remember(expenseTransactions) { expenseTransactions.sumOf { it.amount } }
    val totalIncome = remember(incomeTransactions) { incomeTransactions.sumOf { it.amount } }
    val netSavings = totalIncome - totalExpense

    val categorySpends = remember(expenseTransactions, totalExpense, customCategories) {
        val grouped = expenseTransactions.groupBy { it.category }
        grouped.map { (cat, txs) ->
            val sum = txs.sumOf { it.amount }
            val pct = if (totalExpense > 0) ((sum / totalExpense) * 100).toFloat() else 0f
            val color = getCategoryColor(cat)
            val hex = String.format("#%06X", (0xFFFFFF and color.toArgb()))
            CategorySpend(
                category = cat,
                amount = sum,
                count = txs.size,
                percentage = pct,
                colorHex = hex,
                color = color
            )
        }.sortedByDescending { it.amount }
    }

    // Monthly Trends (past 6 months)
    val monthlyTrends = remember(transactions) {
        val trends = mutableListOf<MonthlyTrend>()
        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("MMM", Locale.getDefault())

        for (i in 5 downTo 0) {
            val targetCal = Calendar.getInstance().apply {
                timeInMillis = now
                add(Calendar.MONTH, -i)
            }
            val month = targetCal.get(Calendar.MONTH)
            val year = targetCal.get(Calendar.YEAR)
            val label = sdf.format(targetCal.time)

            val monthTxs = transactions.filter { tx ->
                cal.timeInMillis = tx.timestamp
                cal.get(Calendar.MONTH) == month && cal.get(Calendar.YEAR) == year && tx.paymentStatus != "FAILED"
            }
            val exp = monthTxs.filter { it.type == "EXPENSE" }.sumOf { it.amount }
            val inc = monthTxs.filter { it.type == "INCOME" }.sumOf { it.amount }

            trends.add(MonthlyTrend(label, exp, inc))
        }
        trends
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Expense Analytics",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            "Recharts Animated Visualization",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    if (onBackClick != null) {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    // Engine Switcher
                    FilterChip(
                        selected = useRechartsEngine,
                        onClick = { useRechartsEngine = !useRechartsEngine },
                        label = {
                            Text(
                                if (useRechartsEngine) "Recharts" else "Native",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        leadingIcon = {
                            Icon(
                                if (useRechartsEngine) Icons.Filled.AutoGraph else Icons.Filled.PieChart,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldGreen.copy(alpha = 0.2f),
                            selectedLabelColor = EmeraldGreen
                        ),
                        modifier = Modifier.padding(end = 12.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Period selector chips
            item {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(AnalyticsPeriod.values()) { period ->
                        val selected = selectedPeriod == period
                        FilterChip(
                            selected = selected,
                            onClick = {
                                selectedPeriod = period
                                selectedCategoryName = null
                            },
                            label = { Text(period.label, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }

            // Summary Metrics Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = "Expenses",
                        amount = totalExpense,
                        icon = Icons.AutoMirrored.Filled.TrendingDown,
                        iconColor = CrimsonRed,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Income",
                        amount = totalIncome,
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        iconColor = EmeraldGreen,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Savings",
                        amount = netSavings,
                        icon = Icons.Filled.Savings,
                        iconColor = if (netSavings >= 0) EmeraldGreen else CrimsonRed,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Main Interactive Animated Pie Chart Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "Monthly Expense Breakdown",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    if (categorySpends.isEmpty()) "No data for this period" else "${categorySpends.size} categories active",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (useRechartsEngine) EmeraldGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = if (useRechartsEngine) "Recharts Web" else "Compose Native",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (useRechartsEngine) EmeraldGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (categorySpends.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(240.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Filled.PieChartOutline,
                                        contentDescription = null,
                                        modifier = Modifier.size(56.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "No expense transactions recorded",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        } else {
                            if (useRechartsEngine) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(300.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                ) {
                                    RechartsWebView(
                                        categoryData = categorySpends,
                                        monthlyTrends = monthlyTrends,
                                        isDark = isDarkTheme,
                                        totalSpent = totalExpense,
                                        customCategories = customCategories,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            } else {
                                NativeDonutPieChart(
                                    categoryData = categorySpends,
                                    totalSpent = totalExpense,
                                    selectedCategory = selectedCategoryName,
                                    onSelectCategory = { selectedCategoryName = it },
                                    customCategories = customCategories,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(260.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Spending Trends by Month (Bar chart section)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            "Spending Trends (Last 6 Months)",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "Month-over-month expenses vs income",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        val maxTrendVal = remember(monthlyTrends) {
                            val max = monthlyTrends.maxOfOrNull { maxOf(it.expense, it.income) } ?: 1.0
                            if (max <= 0.0) 1.0 else max
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            monthlyTrends.forEach { trend ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(120.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.Bottom
                                    ) {
                                        // Expense Bar
                                        val expRatio = (trend.expense / maxTrendVal).toFloat().coerceIn(0.04f, 1f)
                                        Box(
                                            modifier = Modifier
                                                .width(12.dp)
                                                .fillMaxHeight(expRatio)
                                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                                .background(CrimsonRed.copy(alpha = 0.85f))
                                        )

                                        Spacer(modifier = Modifier.width(4.dp))

                                        // Income Bar
                                        val incRatio = (trend.income / maxTrendVal).toFloat().coerceIn(0.04f, 1f)
                                        Box(
                                            modifier = Modifier
                                                .width(12.dp)
                                                .fillMaxHeight(incRatio)
                                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                                .background(EmeraldGreen.copy(alpha = 0.85f))
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = trend.monthLabel,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Legend
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(CrimsonRed)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Expenses", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                            Spacer(modifier = Modifier.width(16.dp))

                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldGreen)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Income", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Category Details List
            item {
                Text(
                    text = "Category Breakdown (${categorySpends.size})",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            items(categorySpends) { cat ->
                val isSelected = selectedCategoryName == cat.category
                CategoryDetailCard(
                    categorySpend = cat,
                    isSelected = isSelected,
                    customCategories = customCategories,
                    onClick = {
                        selectedCategoryName = if (isSelected) null else cat.category
                    }
                )
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    amount: Double,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "₹${formatAmount(amount)}",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun CategoryDetailCard(
    categorySpend: CategorySpend,
    isSelected: Boolean,
    customCategories: Map<String, String>,
    onClick: () -> Unit
) {
    val emoji = getCategoryEmoji(categorySpend.category, customCategories)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onClick() }
            .then(
                if (isSelected) Modifier.border(2.dp, categorySpend.color, RoundedCornerShape(16.dp))
                else Modifier
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) categorySpend.color.copy(alpha = 0.12f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = categorySpend.color.copy(alpha = 0.2f),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(emoji, fontSize = 22.sp)
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        categorySpend.category,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "${categorySpend.count} transactions • ${String.format(Locale.getDefault(), "%.1f", categorySpend.percentage)}%",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "₹${formatAmount(categorySpend.amount)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = categorySpend.color
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Percentage bar
            val animatedProgress by animateFloatAsState(
                targetValue = (categorySpend.percentage / 100f).coerceIn(0f, 1f),
                animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                label = "cat_progress"
            )

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = categorySpend.color,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

/**
 * Native Jetpack Compose Animated Donut Pie Chart with sweep animation and interactive slice selection.
 */
@Composable
fun NativeDonutPieChart(
    categoryData: List<CategorySpend>,
    totalSpent: Double,
    selectedCategory: String?,
    onSelectCategory: (String?) -> Unit,
    customCategories: Map<String, String>,
    modifier: Modifier = Modifier
) {
    var animationTarget by remember { mutableStateOf(0f) }
    LaunchedEffect(categoryData) {
        animationTarget = 0f
        kotlinx.coroutines.delay(50)
        animationTarget = 1f
    }

    val animatedSweepRatio by animateFloatAsState(
        targetValue = animationTarget,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "sweep_ratio"
    )

    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .pointerInput(categoryData) {
                    detectTapGestures { offset ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val dx = offset.x - center.x
                        val dy = offset.y - center.y
                        var angleDeg = Math.toDegrees(kotlin.math.atan2(dy.toDouble(), dx.toDouble())).toFloat()
                        if (angleDeg < 0) angleDeg += 360f

                        // Rotation started at -90 degrees
                        var adjustedAngle = (angleDeg + 90f) % 360f

                        var currentAngle = 0f
                        var foundCategory: String? = null
                        for (cat in categoryData) {
                            val sweep = (cat.percentage / 100f) * 360f
                            if (adjustedAngle >= currentAngle && adjustedAngle < currentAngle + sweep) {
                                foundCategory = cat.category
                                break
                            }
                            currentAngle += sweep
                        }
                        if (foundCategory != null) {
                            onSelectCategory(if (selectedCategory == foundCategory) null else foundCategory)
                        }
                    }
                }
        ) {
            val strokeWidth = 36.dp.toPx()
            val selectedExtraWidth = 10.dp.toPx()
            val chartRadius = (size.minDimension - strokeWidth - selectedExtraWidth) / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            val topLeft = Offset(center.x - chartRadius, center.y - chartRadius)
            val arcSize = Size(chartRadius * 2, chartRadius * 2)

            var startAngle = -90f

            for (cat in categoryData) {
                val fullSweep = (cat.percentage / 100f) * 360f
                val currentSweep = fullSweep * animatedSweepRatio

                val isSelected = selectedCategory == cat.category
                val actualStroke = if (isSelected) strokeWidth + selectedExtraWidth else strokeWidth

                drawArc(
                    color = cat.color,
                    startAngle = startAngle,
                    sweepAngle = currentSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = actualStroke, cap = StrokeCap.Round)
                )

                startAngle += fullSweep
            }
        }

        // Center Hole Information
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            val focusedCat = categoryData.find { it.category == selectedCategory }
            if (focusedCat != null) {
                val emoji = getCategoryEmoji(focusedCat.category, customCategories)
                Text(emoji, fontSize = 24.sp)
                Text(
                    focusedCat.category,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = onSurfaceColor
                )
                Text(
                    "₹${formatAmount(focusedCat.amount)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = focusedCat.color
                )
                Text(
                    "${String.format(Locale.getDefault(), "%.1f", focusedCat.percentage)}%",
                    fontSize = 11.sp,
                    color = onSurfaceVariantColor
                )
            } else {
                Text(
                    "Total Spent",
                    fontSize = 12.sp,
                    color = onSurfaceVariantColor,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "₹${formatAmount(totalSpent)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = onSurfaceColor
                )
                Text(
                    "Tap a slice",
                    fontSize = 10.sp,
                    color = onSurfaceVariantColor
                )
            }
        }
    }
}

/**
 * Recharts Web-based visualization rendered in an Android WebView.
 * Bundled with local JS assets from file:///android_asset/ and CDN fallback.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun RechartsWebView(
    categoryData: List<CategorySpend>,
    monthlyTrends: List<MonthlyTrend>,
    isDark: Boolean,
    totalSpent: Double,
    customCategories: Map<String, String>,
    modifier: Modifier = Modifier
) {
    val htmlContent = remember(categoryData, isDark, totalSpent) {
        generateRechartsHtml(
            categories = categoryData,
            monthlyTrends = monthlyTrends,
            isDark = isDark,
            totalSpent = totalSpent,
            customCategories = customCategories
        )
    }

    AndroidView(
        modifier = modifier.testTag("recharts_webview"),
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    allowFileAccess = true
                    allowContentAccess = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    cacheMode = WebSettings.LOAD_NO_CACHE
                }
                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                        android.util.Log.d("RechartsWebView", "${consoleMessage?.message()} -- From line ${consoleMessage?.lineNumber()}")
                        return true
                    }
                }
                webViewClient = WebViewClient()
                loadDataWithBaseURL("file:///android_asset/", htmlContent, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL("file:///android_asset/", htmlContent, "text/html", "UTF-8", null)
        }
    )
}

/**
 * Builds responsive HTML page with React & Recharts PieChart component.
 */
private fun generateRechartsHtml(
    categories: List<CategorySpend>,
    monthlyTrends: List<MonthlyTrend>,
    isDark: Boolean,
    totalSpent: Double,
    customCategories: Map<String, String>
): String {
    val bgColor = if (isDark) "#151921" else "#FFFFFF"
    val textColor = if (isDark) "#F0F4F8" else "#1A1C1E"
    val subTextColor = if (isDark) "#9CA3AF" else "#6B7280"
    val cardBg = if (isDark) "#1E2430" else "#F8FAFC"
    val borderColor = if (isDark) "#2E384D" else "#E2E8F0"

    val pieDataArray = JSONArray()
    for (cat in categories) {
        val obj = JSONObject()
        val emoji = customCategories[cat.category] ?: CategoryEmojis[cat.category] ?: "📦"
        obj.put("name", "${emoji} ${cat.category}")
        obj.put("value", cat.amount)
        obj.put("color", cat.colorHex)
        obj.put("percentage", cat.percentage)
        pieDataArray.put(obj)
    }

    val trendsDataArray = JSONArray()
    for (tr in monthlyTrends) {
        val obj = JSONObject()
        obj.put("month", tr.monthLabel)
        obj.put("expense", tr.expense)
        obj.put("income", tr.income)
        trendsDataArray.put(obj)
    }

    return """
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
    <style>
        * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; }
        body { background-color: $bgColor; color: $textColor; overflow-x: hidden; padding: 8px; }
        #root { width: 100%; height: 280px; display: flex; justify-content: center; align-items: center; }
        .center-label { position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%); text-align: center; pointer-events: none; }
        .center-label .title { font-size: 11px; color: $subTextColor; font-weight: 500; }
        .center-label .amount { font-size: 16px; font-weight: 700; color: $textColor; }
        .chart-container { position: relative; width: 100%; height: 280px; }
        .tooltip-card { background: $cardBg; border: 1px solid $borderColor; border-radius: 8px; padding: 8px 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.2); }
        .tooltip-title { font-size: 12px; font-weight: 700; color: $textColor; }
        .tooltip-amount { font-size: 13px; font-weight: 800; color: #00E676; margin-top: 2px; }
        .tooltip-pct { font-size: 11px; color: $subTextColor; }
        /* Clean SVG fallback if React takes time to mount */
        .fallback-ring { display: none; }
    </style>
    <!-- Script loading: asset folder with CDN fallbacks -->
    <script src="file:///android_asset/react.min.js" onerror="this.onerror=null;this.src='https://unpkg.com/react@18/umd/react.production.min.js'"></script>
    <script src="file:///android_asset/react-dom.min.js" onerror="this.onerror=null;this.src='https://unpkg.com/react-dom@18/umd/react-dom.production.min.js'"></script>
    <script src="file:///android_asset/prop-types.min.js" onerror="this.onerror=null;this.src='https://unpkg.com/prop-types@15.8.1/prop-types.min.js'"></script>
    <script src="file:///android_asset/recharts.min.js" onerror="this.onerror=null;this.src='https://unpkg.com/recharts@2.12.7/umd/Recharts.min.js'"></script>
</head>
<body>
    <div id="root">
        <div style="text-align: center; color: $subTextColor; font-size: 12px;">Rendering Animated Recharts...</div>
    </div>

    <script>
        const pieData = $pieDataArray;
        const trendsData = $trendsDataArray;
        const totalAmountStr = "₹${formatAmount(totalSpent)}";

        function mountApp() {
            if (typeof React === 'undefined' || typeof ReactDOM === 'undefined' || typeof Recharts === 'undefined') {
                setTimeout(mountApp, 100);
                return;
            }

            const { ResponsiveContainer, PieChart, Pie, Cell, Tooltip } = Recharts;

            const CustomTooltip = ({ active, payload }) => {
                if (active && payload && payload.length) {
                    const data = payload[0].payload;
                    return React.createElement('div', { className: 'tooltip-card' },
                        React.createElement('div', { className: 'tooltip-title' }, data.name),
                        React.createElement('div', { className: 'tooltip-amount' }, '₹' + Number(data.value).toLocaleString('en-IN')),
                        React.createElement('div', { className: 'tooltip-pct' }, Number(data.percentage).toFixed(1) + '% of total')
                    );
                }
                return null;
            };

            const App = () => {
                const cells = pieData.map((entry, index) => 
                    React.createElement(Cell, { key: 'cell-' + index, fill: entry.color, stroke: '$bgColor', strokeWidth: 2 })
                );

                const pieElement = React.createElement(Pie, {
                    data: pieData,
                    cx: "50%",
                    cy: "50%",
                    innerRadius: 65,
                    outerRadius: 100,
                    paddingAngle: 3,
                    dataKey: "value",
                    isAnimationActive: true,
                    animationDuration: 1200,
                    animationEasing: "ease-out"
                }, cells);

                const tooltipElement = React.createElement(Tooltip, { content: React.createElement(CustomTooltip) });

                const chartElement = React.createElement(PieChart, null, pieElement, tooltipElement);
                const containerElement = React.createElement(ResponsiveContainer, { width: "100%", height: "100%" }, chartElement);

                const centerLabel = React.createElement('div', { className: 'center-label' },
                    React.createElement('div', { className: 'title' }, 'Total Spent'),
                    React.createElement('div', { className: 'amount' }, totalAmountStr)
                );

                return React.createElement('div', { className: 'chart-container' }, containerElement, centerLabel);
            };

            const rootElement = document.getElementById('root');
            if (ReactDOM.createRoot) {
                ReactDOM.createRoot(rootElement).render(React.createElement(App));
            } else {
                ReactDOM.render(React.createElement(App), rootElement);
            }
        }

        window.addEventListener('DOMContentLoaded', mountApp);
        setTimeout(mountApp, 200);
    </script>
</body>
</html>
    """.trimIndent()
}
