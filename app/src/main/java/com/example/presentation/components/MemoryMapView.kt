package com.example.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.brain.memory.EpisodicEventEntity

@Composable
fun MemoryMapView(
    events: List<EpisodicEventEntity>,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val eventsWithLocation = events.filter { it.latitude != null && it.longitude != null }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale *= zoom
                    offset += pan
                }
            }
    ) {
        if (eventsWithLocation.isEmpty()) {
            Text(
                text = "No geo-tagged memories found in the Space-Time Continuum.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
            
            // Draw grid lines (stylized map)
            val gridSize = 100f * scale
            val startX = (offset.x % gridSize) - gridSize
            val startY = (offset.y % gridSize) - gridSize
            
            for (x in 0..(size.width / gridSize).toInt() + 2) {
                drawLine(
                    color = Color.White.copy(alpha = 0.1f),
                    start = Offset(startX + x * gridSize, 0f),
                    end = Offset(startX + x * gridSize, size.height),
                    strokeWidth = 1f
                )
            }
            for (y in 0..(size.height / gridSize).toInt() + 2) {
                drawLine(
                    color = Color.White.copy(alpha = 0.1f),
                    start = Offset(0f, startY + y * gridSize),
                    end = Offset(size.width, startY + y * gridSize),
                    strokeWidth = 1f
                )
            }

            // Simple Mercator-ish projection for pins
            // Assuming the first event is the "center" if not panned
            val baseLat = eventsWithLocation.firstOrNull()?.latitude ?: 0.0
            val baseLon = eventsWithLocation.firstOrNull()?.longitude ?: 0.0

            for (event in eventsWithLocation) {
                val dx = (event.longitude!! - baseLon) * 200000 * scale
                val dy = (baseLat - event.latitude!!) * 200000 * scale // Flip Y
                
                val pinPos = center + offset + Offset(dx.toFloat(), dy.toFloat())
                
                // Draw pin pulse
                drawCircle(
                    color = Color.Cyan.copy(alpha = 0.3f),
                    radius = 20f * scale,
                    center = pinPos,
                    style = Stroke(width = 2f)
                )
                drawCircle(
                    color = Color.Cyan,
                    radius = 6f * scale,
                    center = pinPos
                )
            }
        }
    }
}
