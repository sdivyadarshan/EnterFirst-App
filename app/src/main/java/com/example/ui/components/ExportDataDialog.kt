package com.example.ui.components

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Transaction
import com.example.ui.FinanceViewModel
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.EmeraldGreen
import java.text.SimpleDateFormat
import java.util.*

enum class ExportFormat {
    CSV,
    JSON
}

enum class ExportDateRange {
    ALL,
    THIS_MONTH,
    LAST_MONTH,
    CUSTOM
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportDataModal(
    viewModel: FinanceViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()

    var selectedFormat by remember { mutableStateOf(ExportFormat.CSV) }
    var selectedRange by remember { mutableStateOf(ExportDateRange.ALL) }

    var customStartDate by remember { mutableStateOf(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000) }
    var customEndDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var isExporting by remember { mutableStateOf(false) }

    val filteredTransactions = remember(transactions, selectedRange, customStartDate, customEndDate) {
        val cal = Calendar.getInstance()
        when (selectedRange) {
            ExportDateRange.ALL -> transactions
            ExportDateRange.THIS_MONTH -> {
                val currentMonth = cal.get(Calendar.MONTH)
                val currentYear = cal.get(Calendar.YEAR)
                transactions.filter { tx ->
                    val txCal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
                    txCal.get(Calendar.MONTH) == currentMonth && txCal.get(Calendar.YEAR) == currentYear
                }
            }
            ExportDateRange.LAST_MONTH -> {
                cal.add(Calendar.MONTH, -1)
                val lastMonth = cal.get(Calendar.MONTH)
                val lastYear = cal.get(Calendar.YEAR)
                transactions.filter { tx ->
                    val txCal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
                    txCal.get(Calendar.MONTH) == lastMonth && txCal.get(Calendar.YEAR) == lastYear
                }
            }
            ExportDateRange.CUSTOM -> {
                transactions.filter { tx ->
                    tx.timestamp in customStartDate..customEndDate
                }
            }
        }
    }

    // SAF Document Creation Launchers
    val saveCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            isExporting = true
            viewModel.writeCsvToUri(context, uri, filteredTransactions) { success ->
                isExporting = false
                if (success) {
                    Toast.makeText(context, "Exported ${filteredTransactions.size} transactions to CSV!", Toast.LENGTH_LONG).show()
                    onDismiss()
                } else {
                    Toast.makeText(context, "Failed to save CSV file.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val saveJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            isExporting = true
            viewModel.writeJsonToUri(context, uri, filteredTransactions) { success ->
                isExporting = false
                if (success) {
                    Toast.makeText(context, "Exported ${filteredTransactions.size} transactions to JSON!", Toast.LENGTH_LONG).show()
                    onDismiss()
                } else {
                    Toast.makeText(context, "Failed to save JSON file.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val importCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            viewModel.importFromCsv(context, it) { success, msg ->
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                if (success) onDismiss()
            }
        }
    }

    val importJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            viewModel.importFromJson(context, it) { success, msg ->
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                if (success) onDismiss()
            }
        }
    }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        tonalElevation = 8.dp,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFF475569)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = null,
                            tint = EmeraldGreen,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Export Expense Data",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "Privacy-focused offline local backup",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF94A3B8)
                            )
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF94A3B8)
                    )
                }
            }

            // Format Selector: CSV vs JSON
            Text(
                text = "EXPORT FORMAT",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B)
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // CSV Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedFormat = ExportFormat.CSV }
                        .testTag("select_format_csv"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedFormat == ExportFormat.CSV)
                            EmeraldGreen.copy(alpha = 0.15f)
                        else
                            Color(0xFF1E293B)
                    ),
                    border = BorderStroke(
                        1.5.dp,
                        if (selectedFormat == ExportFormat.CSV) EmeraldGreen else Color(0xFF334155)
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📊 CSV Format",
                                fontWeight = FontWeight.Bold,
                                color = if (selectedFormat == ExportFormat.CSV) EmeraldGreen else Color.White,
                                fontSize = 15.sp
                            )
                            if (selectedFormat == ExportFormat.CSV) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = EmeraldGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Excel, Google Sheets, numbers & accounting tools",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )
                        )
                    }
                }

                // JSON Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedFormat = ExportFormat.JSON }
                        .testTag("select_format_json"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedFormat == ExportFormat.JSON)
                            Color(0xFF38BDF8).copy(alpha = 0.15f)
                        else
                            Color(0xFF1E293B)
                    ),
                    border = BorderStroke(
                        1.5.dp,
                        if (selectedFormat == ExportFormat.JSON) Color(0xFF38BDF8) else Color(0xFF334155)
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📦 JSON Backup",
                                fontWeight = FontWeight.Bold,
                                color = if (selectedFormat == ExportFormat.JSON) Color(0xFF38BDF8) else Color.White,
                                fontSize = 15.sp
                            )
                            if (selectedFormat == ExportFormat.JSON) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Complete database snapshot & 1-click restore",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            }

            // Date Range Filter
            Text(
                text = "DATE RANGE",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B)
                )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E293B)),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf(
                    ExportDateRange.ALL to "All Time",
                    ExportDateRange.THIS_MONTH to "This Month",
                    ExportDateRange.LAST_MONTH to "Last Month",
                    ExportDateRange.CUSTOM to "Custom"
                ).forEach { (range, label) ->
                    val isSelected = selectedRange == range
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) EmeraldGreen.copy(alpha = 0.25f) else Color.Transparent
                            )
                            .clickable { selectedRange = range }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) EmeraldGreen else Color(0xFF94A3B8),
                                fontSize = 11.sp
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Custom Date Pickers
            AnimatedVisibility(visible = selectedRange == ExportDateRange.CUSTOM) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val cal = Calendar.getInstance().apply { timeInMillis = customStartDate }
                            DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    val newCal = Calendar.getInstance().apply {
                                        set(y, m, d, 0, 0, 0)
                                    }
                                    customStartDate = newCal.timeInMillis
                                },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFF334155))
                    ) {
                        Text("From: ${dateFormat.format(Date(customStartDate))}", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val cal = Calendar.getInstance().apply { timeInMillis = customEndDate }
                            DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    val newCal = Calendar.getInstance().apply {
                                        set(y, m, d, 23, 59, 59)
                                    }
                                    customEndDate = newCal.timeInMillis
                                },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFF334155))
                    ) {
                        Text("To: ${dateFormat.format(Date(customEndDate))}", fontSize = 12.sp)
                    }
                }
            }

            // Summary Stats Pill
            Surface(
                color = Color(0xFF1E293B).copy(alpha = 0.6f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${filteredTransactions.size} Records Selected",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }

                    val totalAmount = filteredTransactions.sumOf { if (it.type == "EXPENSE") it.amount else -it.amount }
                    Text(
                        text = "Total: ₹%,.2f".format(Locale.getDefault(), Math.abs(totalAmount)),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (totalAmount >= 0) CrimsonRed else EmeraldGreen
                        )
                    )
                }
            }

            // Main Actions: Save Locally or Share
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Share Direct Button
                OutlinedButton(
                    onClick = {
                        val fileExt = if (selectedFormat == ExportFormat.CSV) "csv" else "json"
                        if (selectedFormat == ExportFormat.CSV) {
                            viewModel.exportToCsv(context, filteredTransactions) { uri ->
                                uri?.let { shareFile(context, it, "text/csv") }
                            }
                        } else {
                            viewModel.exportToJson(context, filteredTransactions) { uri ->
                                uri?.let { shareFile(context, it, "application/json") }
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF475569)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share", fontWeight = FontWeight.Bold)
                }

                // Save to Storage / Folder Button
                Button(
                    onClick = {
                        val defaultFileName = "enterfirst_backup_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())}.${if (selectedFormat == ExportFormat.CSV) "csv" else "json"}"
                        if (selectedFormat == ExportFormat.CSV) {
                            saveCsvLauncher.launch(defaultFileName)
                        } else {
                            saveJsonLauncher.launch(defaultFileName)
                        }
                    },
                    modifier = Modifier
                        .weight(1.5f)
                        .height(48.dp)
                        .testTag("save_export_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedFormat == ExportFormat.CSV) EmeraldGreen else Color(0xFF38BDF8)
                    ),
                    enabled = !isExporting && filteredTransactions.isNotEmpty()
                ) {
                    if (isExporting) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.SaveAlt, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Save to Device",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Divider(color = Color(0xFF334155), thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))

            // Restore / Import local backup options
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RESTORE FROM LOCAL BACKUP",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B)
                    )
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = { importCsvLauncher.launch("text/*") },
                        colors = ButtonDefaults.textButtonColors(contentColor = EmeraldGreen)
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Import CSV", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    TextButton(
                        onClick = { importJsonLauncher.launch("application/json") },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF38BDF8))
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Import JSON", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun shareFile(context: Context, uri: Uri, mimeType: String) {
    try {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Expense Backup"))
    } catch (e: Exception) {
        Toast.makeText(context, "Error sharing file: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}
