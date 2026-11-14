package com.example.ui.components

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CalculatorNumpad(
    currentAmount: String,
    onAmountChange: (String) -> Unit,
    selectedTimestamp: Long = System.currentTimeMillis(),
    onDateSelected: (Long) -> Unit = {},
    onConfirm: () -> Unit = {},
    accentColor: Color = Color(0xFF10B981),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isToday = remember(selectedTimestamp) {
        val nowCal = Calendar.getInstance()
        val txCal = Calendar.getInstance().apply { timeInMillis = selectedTimestamp }
        nowCal.get(Calendar.YEAR) == txCal.get(Calendar.YEAR) &&
                nowCal.get(Calendar.DAY_OF_YEAR) == txCal.get(Calendar.DAY_OF_YEAR)
    }

    val dateLabel = remember(selectedTimestamp, isToday) {
        if (isToday) {
            "Today"
        } else {
            SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(selectedTimestamp))
        }
    }

    fun handleKeyPress(key: String) {
        when (key) {
            "BACKSPACE" -> {
                if (currentAmount.isNotEmpty()) {
                    onAmountChange(currentAmount.dropLast(1))
                }
            }
            "CLEAR" -> {
                onAmountChange("")
            }
            "DATE" -> {
                val cal = Calendar.getInstance().apply { timeInMillis = selectedTimestamp }
                DatePickerDialog(
                    context,
                    { _, y, m, d ->
                        val newCal = Calendar.getInstance().apply {
                            set(y, m, d, 12, 0, 0)
                        }
                        onDateSelected(newCal.timeInMillis)
                    },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
                ).show()
            }
            "EVAL_OR_CONFIRM" -> {
                val evaluated = evaluateMathExpression(currentAmount)
                if (evaluated != currentAmount) {
                    onAmountChange(evaluated)
                } else {
                    onConfirm()
                }
            }
            "+", "-", "*", "/" -> {
                // Prevent duplicate consecutive operators
                if (currentAmount.isNotEmpty()) {
                    val last = currentAmount.last()
                    if (last in listOf('+', '-', '*', '/', 'x', '÷')) {
                        onAmountChange(currentAmount.dropLast(1) + key)
                    } else {
                        onAmountChange(currentAmount + key)
                    }
                }
            }
            "." -> {
                // Only allow one dot per number segment
                val segments = currentAmount.split(Regex("[+\\-*/x÷]"))
                val lastSegment = segments.lastOrNull() ?: ""
                if (!lastSegment.contains(".")) {
                    onAmountChange(if (currentAmount.isEmpty()) "0." else "$currentAmount.")
                }
            }
            else -> {
                // Digit 0..9
                if (currentAmount == "0" && key != ".") {
                    onAmountChange(key)
                } else {
                    onAmountChange(currentAmount + key)
                }
            }
        }
    }

    Surface(
        color = Color(0xFF131722),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Row 1: 7 | 8 | 9 | Today (Date)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                KeypadButton("7", modifier = Modifier.weight(1f)) { handleKeyPress("7") }
                KeypadButton("8", modifier = Modifier.weight(1f)) { handleKeyPress("8") }
                KeypadButton("9", modifier = Modifier.weight(1f)) { handleKeyPress("9") }
                KeypadDateButton(
                    label = dateLabel,
                    modifier = Modifier.weight(1f)
                ) { handleKeyPress("DATE") }
            }

            // Row 2: 4 | 5 | 6 | +
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                KeypadButton("4", modifier = Modifier.weight(1f)) { handleKeyPress("4") }
                KeypadButton("5", modifier = Modifier.weight(1f)) { handleKeyPress("5") }
                KeypadButton("6", modifier = Modifier.weight(1f)) { handleKeyPress("6") }
                KeypadOperatorButton("+", modifier = Modifier.weight(1f)) { handleKeyPress("+") }
            }

            // Row 3: 1 | 2 | 3 | - / *
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                KeypadButton("1", modifier = Modifier.weight(1f)) { handleKeyPress("1") }
                KeypadButton("2", modifier = Modifier.weight(1f)) { handleKeyPress("2") }
                KeypadButton("3", modifier = Modifier.weight(1f)) { handleKeyPress("3") }
                KeypadOperatorButton("-", modifier = Modifier.weight(1f)) { handleKeyPress("-") }
            }

            // Row 4: . | 0 | ⌫ | ✓
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                KeypadButton(".", modifier = Modifier.weight(1f)) { handleKeyPress(".") }
                KeypadButton("0", modifier = Modifier.weight(1f)) { handleKeyPress("0") }
                KeypadIconButton(
                    icon = Icons.Default.Backspace,
                    contentDescription = "Backspace",
                    modifier = Modifier.weight(1f)
                ) { handleKeyPress("BACKSPACE") }
                KeypadConfirmButton(
                    accentColor = accentColor,
                    modifier = Modifier.weight(1f)
                ) { handleKeyPress("EVAL_OR_CONFIRM") }
            }
        }
    }
}

@Composable
private fun KeypadButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF222836))
            .clickable(onClick = onClick)
            .testTag("keypad_btn_$text"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
    }
}

@Composable
private fun KeypadOperatorButton(
    operator: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E2430))
            .clickable(onClick = onClick)
            .testTag("keypad_op_$operator"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = operator,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF94A3B8)
        )
    }
}

@Composable
private fun KeypadDateButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E2430))
            .clickable(onClick = onClick)
            .testTag("keypad_date_btn"),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = Color(0xFFFBBF24), // Warm golden calendar icon
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFBBF24)
            )
        }
    }
}

@Composable
private fun KeypadIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF222836))
            .clickable(onClick = onClick)
            .testTag("keypad_icon_$contentDescription"),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color(0xFFE2E8F0),
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun KeypadConfirmButton(
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(accentColor)
            .clickable(onClick = onClick)
            .testTag("keypad_confirm_btn"),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = "Confirm",
            tint = Color.Black,
            modifier = Modifier.size(26.dp)
        )
    }
}

/**
 * Simple arithmetic evaluator for simple sum/subtraction expressions on the keypad
 */
private fun evaluateMathExpression(expr: String): String {
    try {
        if (expr.isBlank()) return ""
        // If it's just a number
        val simpleNumber = expr.toDoubleOrNull()
        if (simpleNumber != null) {
            return if (simpleNumber % 1.0 == 0.0) {
                simpleNumber.toLong().toString()
            } else {
                simpleNumber.toString()
            }
        }

        // Parse basic tokens like 100 + 50 - 20
        var total = 0.0
        var currentOp = '+'
        var currentNum = StringBuilder()

        val cleanExpr = expr.replace("x", "*").replace("÷", "/")

        for (i in 0 until cleanExpr.length) {
            val ch = cleanExpr[i]
            if (ch.isDigit() || ch == '.') {
                currentNum.append(ch)
            }
            if (!ch.isDigit() && ch != '.' || i == cleanExpr.length - 1) {
                val num = currentNum.toString().toDoubleOrNull() ?: 0.0
                when (currentOp) {
                    '+' -> total += num
                    '-' -> total -= num
                    '*' -> total *= num
                    '/' -> if (num != 0.0) total /= num
                }
                currentOp = ch
                currentNum = StringBuilder()
            }
        }

        return if (total % 1.0 == 0.0) {
            total.toLong().toString()
        } else {
            "%.2f".format(Locale.ROOT, total)
        }
    } catch (e: Exception) {
        return expr
    }
}
