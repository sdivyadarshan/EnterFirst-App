package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Robust arithmetic expression evaluator supporting +, -, ×, ÷, and decimals
 * with standard BODMAS operator precedence.
 */
object CalculatorEvaluator {

    fun isExpression(input: String): Boolean {
        return input.any { it == '+' || it == '-' || it == '×' || it == '*' || it == '÷' || it == '/' }
    }

    fun evaluate(input: String): Double? {
        if (input.isBlank()) return null
        
        // Clean up expression
        var sanitized = input.replace(" ", "")
            .replace("×", "*")
            .replace("÷", "/")
            .replace("−", "-")

        // Remove trailing operators
        while (sanitized.isNotEmpty() && (sanitized.last() == '+' || sanitized.last() == '-' || sanitized.last() == '*' || sanitized.last() == '/' || sanitized.last() == '.')) {
            sanitized = sanitized.dropLast(1)
        }

        if (sanitized.isBlank()) return null

        return try {
            parseAndEvaluate(sanitized)
        } catch (e: Exception) {
            null
        }
    }

    fun formatAmount(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.toLong().toString()
        } else {
            String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
        }
    }

    private fun parseAndEvaluate(expr: String): Double {
        // Tokenize numbers and operators
        val tokens = mutableListOf<String>()
        var currentNumber = StringBuilder()

        var i = 0
        while (i < expr.length) {
            val c = expr[i]
            if (c in '0'..'9' || c == '.') {
                currentNumber.append(c)
            } else if (c == '+' || c == '-' || c == '*' || c == '/') {
                if (currentNumber.isNotEmpty()) {
                    tokens.add(currentNumber.toString())
                    currentNumber = StringBuilder()
                    tokens.add(c.toString())
                } else if (c == '-') {
                    // Leading negative number
                    currentNumber.append(c)
                } else {
                    // Ignore misplaced operator
                }
            }
            i++
        }
        if (currentNumber.isNotEmpty()) {
            tokens.add(currentNumber.toString())
        }

        if (tokens.isEmpty()) return 0.0
        if (tokens.size == 1) return tokens[0].toDoubleOrNull() ?: 0.0

        // Step 1: Process * and /
        val pass1Tokens = mutableListOf<String>()
        var idx = 0
        while (idx < tokens.size) {
            val token = tokens[idx]
            if (token == "*" || token == "/") {
                val prev = pass1Tokens.removeAt(pass1Tokens.size - 1).toDoubleOrNull() ?: 0.0
                val next = if (idx + 1 < tokens.size) tokens[idx + 1].toDoubleOrNull() ?: 0.0 else 0.0
                val result = if (token == "*") {
                    prev * next
                } else {
                    if (next == 0.0) prev else prev / next
                }
                pass1Tokens.add(result.toString())
                idx += 2
            } else {
                pass1Tokens.add(token)
                idx++
            }
        }

        // Step 2: Process + and -
        if (pass1Tokens.isEmpty()) return 0.0
        var total = pass1Tokens[0].toDoubleOrNull() ?: 0.0
        var pIdx = 1
        while (pIdx < pass1Tokens.size) {
            val op = pass1Tokens[pIdx]
            val nextVal = if (pIdx + 1 < pass1Tokens.size) pass1Tokens[pIdx + 1].toDoubleOrNull() ?: 0.0 else 0.0
            if (op == "+") {
                total += nextVal
            } else if (op == "-") {
                total -= nextVal
            }
            pIdx += 2
        }

        return total
    }
}

/**
 * Custom In-App Calculator Keypad matching the custom UI design.
 * Features:
 *  - 4x5 Keypad grid: Numbers 0-9, Decimal, Backspace (with long-press clear)
 *  - Arithmetic operators: +, -, ×, ÷
 *  - Date shortcut button (Today / Yesterday / custom date picker)
 *  - Checkmark (Done/Calculate) button to evaluate and dismiss
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorKeypad(
    modifier: Modifier = Modifier,
    amountText: String,
    onAmountChange: (String) -> Unit,
    selectedTimestamp: Long = System.currentTimeMillis(),
    onDateSelected: ((Long) -> Unit)? = null,
    onDone: () -> Unit,
    accentColor: Color = Color(0xFFE53935)
) {
    var showDatePickerDialog by remember { mutableStateOf(false) }

    // Date picker dialog
    if (showDatePickerDialog && onDateSelected != null) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedTimestamp
        )
        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            onDateSelected(it)
                        }
                        showDatePickerDialog = false
                    }
                ) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Format date button label
    val dateLabel = remember(selectedTimestamp) {
        val nowCal = Calendar.getInstance()
        val targetCal = Calendar.getInstance().apply { timeInMillis = selectedTimestamp }

        val isToday = nowCal.get(Calendar.YEAR) == targetCal.get(Calendar.YEAR) &&
                nowCal.get(Calendar.DAY_OF_YEAR) == targetCal.get(Calendar.DAY_OF_YEAR)

        val isYesterday = nowCal.get(Calendar.YEAR) == targetCal.get(Calendar.YEAR) &&
                nowCal.get(Calendar.DAY_OF_YEAR) - targetCal.get(Calendar.DAY_OF_YEAR) == 1

        when {
            isToday -> "Today"
            isYesterday -> "Yesterday"
            else -> SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(selectedTimestamp))
        }
    }

    // Keypad Click Handlers
    fun onDigitClick(digit: String) {
        if (amountText == "0" && digit != ".") {
            onAmountChange(digit)
        } else {
            onAmountChange(amountText + digit)
        }
    }

    fun onDotClick() {
        if (amountText.isEmpty()) {
            onAmountChange("0.")
            return
        }
        // Extract current number segment after last operator
        val lastOpIndex = amountText.indexOfLast { it == '+' || it == '-' || it == '×' || it == '*' || it == '÷' || it == '/' }
        val currentSegment = if (lastOpIndex >= 0) amountText.substring(lastOpIndex + 1) else amountText
        if (!currentSegment.contains('.')) {
            if (currentSegment.isEmpty()) {
                onAmountChange(amountText + "0.")
            } else {
                onAmountChange(amountText + ".")
            }
        }
    }

    fun onOperatorClick(op: String) {
        if (amountText.isEmpty()) {
            if (op == "-") {
                onAmountChange("-")
            }
            return
        }
        val lastChar = amountText.last()
        if (lastChar == '+' || lastChar == '-' || lastChar == '×' || lastChar == '*' || lastChar == '÷' || lastChar == '/') {
            // Replace operator
            onAmountChange(amountText.dropLast(1) + op)
        } else if (lastChar == '.') {
            onAmountChange(amountText.dropLast(1) + op)
        } else {
            onAmountChange(amountText + op)
        }
    }

    fun onBackspace() {
        if (amountText.isNotEmpty()) {
            onAmountChange(amountText.dropLast(1))
        }
    }

    fun onClearAll() {
        onAmountChange("")
    }

    fun onCalculateOrDone() {
        if (CalculatorEvaluator.isExpression(amountText)) {
            val result = CalculatorEvaluator.evaluate(amountText)
            if (result != null) {
                onAmountChange(CalculatorEvaluator.formatAmount(result))
            }
        }
        onDone()
    }

    // Keypad Surface
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars),
        color = Color(0xFF1E2024),
        border = BorderStroke(1.dp, Color(0xFF2E323B)),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        shadowElevation = 12.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Row 1: [ 7 ] [ 8 ] [ 9 ] [ 📅 Today ]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CalculatorKey(text = "7", modifier = Modifier.weight(1f), onClick = { onDigitClick("7") })
                CalculatorKey(text = "8", modifier = Modifier.weight(1f), onClick = { onDigitClick("8") })
                CalculatorKey(text = "9", modifier = Modifier.weight(1f), onClick = { onDigitClick("9") })
                
                // Date Button (spans 2 column width)
                DateShortcutKey(
                    label = dateLabel,
                    modifier = Modifier.weight(2f),
                    onClick = {
                        if (onDateSelected != null) {
                            showDatePickerDialog = true
                        }
                    }
                )
            }

            // Row 2: [ 4 ] [ 5 ] [ 6 ] [ + ] [ - ]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CalculatorKey(text = "4", modifier = Modifier.weight(1f), onClick = { onDigitClick("4") })
                CalculatorKey(text = "5", modifier = Modifier.weight(1f), onClick = { onDigitClick("5") })
                CalculatorKey(text = "6", modifier = Modifier.weight(1f), onClick = { onDigitClick("6") })
                OperatorKey(text = "+", modifier = Modifier.weight(1f), onClick = { onOperatorClick("+") })
                OperatorKey(text = "-", modifier = Modifier.weight(1f), onClick = { onOperatorClick("-") })
            }

            // Row 3: [ 1 ] [ 2 ] [ 3 ] [ × ] [ ÷ ]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CalculatorKey(text = "1", modifier = Modifier.weight(1f), onClick = { onDigitClick("1") })
                CalculatorKey(text = "2", modifier = Modifier.weight(1f), onClick = { onDigitClick("2") })
                CalculatorKey(text = "3", modifier = Modifier.weight(1f), onClick = { onDigitClick("3") })
                OperatorKey(text = "×", modifier = Modifier.weight(1f), onClick = { onOperatorClick("×") })
                OperatorKey(text = "÷", modifier = Modifier.weight(1f), onClick = { onOperatorClick("÷") })
            }

            // Row 4: [ . ] [ 0 ] [ ⌫ ] [ ✓ ]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CalculatorKey(text = ".", modifier = Modifier.weight(1f), onClick = { onDotClick() })
                CalculatorKey(text = "0", modifier = Modifier.weight(1f), onClick = { onDigitClick("0") })
                BackspaceKey(
                    modifier = Modifier.weight(1f),
                    onClick = { onBackspace() },
                    onLongClick = { onClearAll() }
                )
                // Done Checkmark Button (spans 2 column width)
                DoneKey(
                    modifier = Modifier.weight(2f),
                    accentColor = accentColor,
                    onClick = { onCalculateOrDone() }
                )
            }
        }
    }
}

@Composable
private fun CalculatorKey(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(54.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF26282E))
            .clickable { onClick() }
            .testTag("calc_key_$text"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
    }
}

@Composable
private fun OperatorKey(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(54.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF32353E))
            .clickable { onClick() }
            .testTag("calc_op_$text"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF90CAF9)
        )
    }
}

@Composable
private fun DateShortcutKey(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(54.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF2D2A26))
            .clickable { onClick() }
            .testTag("calc_date_btn"),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.CalendarMonth,
                contentDescription = "Select Date",
                tint = Color(0xFFF59E0B),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF59E0B)
            )
        }
    }
}

@Composable
private fun BackspaceKey(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(54.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF26282E))
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongClick() }
                )
            }
            .testTag("calc_backspace_btn"),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Backspace,
            contentDescription = "Backspace",
            tint = Color(0xFFCFD8DC),
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun DoneKey(
    modifier: Modifier = Modifier,
    accentColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(54.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF383C48))
            .clickable { onClick() }
            .testTag("calc_done_btn"),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = "Confirm Amount",
            tint = Color.White,
            modifier = Modifier.size(26.dp)
        )
    }
}
