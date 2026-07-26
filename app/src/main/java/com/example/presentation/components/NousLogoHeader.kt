package com.example.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NousLogoHeader(
    modifier: Modifier = Modifier,
    showText: Boolean = true,
    iconSize: Int = 100
) {
    val isLight = MaterialTheme.colorScheme.background == Color.White
    val slashColor = if (isLight) Color(0xFF111111) else Color(0xFFEEEEEE)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Canvas(modifier = Modifier.size(iconSize.dp)) {
            val width = size.width
            val height = size.height

            // Center diagonal slash
            rotate(degrees = -35f, pivot = Offset(width / 2f, height / 2f)) {
                drawRoundRect(
                    color = slashColor,
                    topLeft = Offset(width / 2f - (iconSize / 10f).dp.toPx(), height / 2f - (iconSize * 0.4f).dp.toPx()),
                    size = Size((iconSize / 5f).dp.toPx(), (iconSize * 0.8f).dp.toPx()),
                    cornerRadius = CornerRadius((iconSize / 10f).dp.toPx(), (iconSize / 10f).dp.toPx())
                )
            }

            // Blue Sphere 1 (Bottom Left)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF80B3FF), Color(0xFF005EFA)),
                    center = Offset(width / 2f - (iconSize * 0.24f).dp.toPx(), height / 2f + (iconSize * 0.12f).dp.toPx()),
                    radius = (iconSize * 0.12f).dp.toPx()
                ),
                radius = (iconSize * 0.1f).dp.toPx(),
                center = Offset(width / 2f - (iconSize * 0.24f).dp.toPx(), height / 2f + (iconSize * 0.12f).dp.toPx())
            )

            // Blue Sphere 2 (Top Right)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF80B3FF), Color(0xFF005EFA)),
                    center = Offset(width / 2f + (iconSize * 0.24f).dp.toPx(), height / 2f - (iconSize * 0.28f).dp.toPx()),
                    radius = (iconSize * 0.12f).dp.toPx()
                ),
                radius = (iconSize * 0.1f).dp.toPx(),
                center = Offset(width / 2f + (iconSize * 0.24f).dp.toPx(), height / 2f - (iconSize * 0.28f).dp.toPx())
            )
        }

        if (showText) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "nous",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = 4.sp,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}
