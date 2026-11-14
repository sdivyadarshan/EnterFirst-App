package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CalculatorAmountDisplay(
    amountText: String,
    type: String, // "EXPENSE" or "INCOME"
    isActive: Boolean,
    onActivate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val interactionSource = remember { MutableInteractionSource() }

    val accentColor = if (type == "EXPENSE") Color(0xFFFF5252) else Color(0xFF00E676)

    // Cursor animation
    val infiniteTransition = rememberInfiniteTransition(label = "cursor_blink")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursor_alpha"
    )

    val isExpr = remember(amountText) { CalculatorEvaluator.isExpression(amountText) }
    val evaluatedAmount = remember(amountText) {
        if (isExpr) CalculatorEvaluator.evaluate(amountText) else null
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                // Dismiss normal system software keyboard & clear focus from other fields
                keyboardController?.hide()
                focusManager.clearFocus()
                onActivate()
            }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (isActive) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    else Color.Transparent
                )
                .border(
                    border = BorderStroke(
                        width = if (isActive) 1.5.dp else 0.dp,
                        color = if (isActive) accentColor.copy(alpha = 0.6f) else Color.Transparent
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 24.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.testTag("calculator_amount_display")
            ) {
                // Currency Symbol
                Text(
                    text = "₹",
                    fontSize = 44.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = accentColor,
                    modifier = Modifier.padding(end = 8.dp)
                )

                // Expression / Value
                Text(
                    text = if (amountText.isEmpty()) "0" else amountText,
                    fontSize = if (amountText.length > 10) 32.sp else 44.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (amountText.isEmpty()) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                            else MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                // Blinking Cursor when active
                if (isActive) {
                    Box(
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .width(3.dp)
                            .height(40.dp)
                            .background(accentColor.copy(alpha = cursorAlpha))
                    )
                }
            }
        }

        // Live calculation preview if there is an arithmetic expression
        AnimatedVisibility(
            visible = isExpr && evaluatedAmount != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            if (evaluatedAmount != null) {
                Surface(
                    color = accentColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(top = 6.dp)
                ) {
                    Text(
                        text = "= ₹ ${CalculatorEvaluator.formatAmount(evaluatedAmount)}",
                        color = accentColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
