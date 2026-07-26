package com.example.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Thought
import com.example.domain.model.ThoughtConnection

@Composable
fun NodeCanvas(
    thoughts: List<Thought>,
    connections: List<ThoughtConnection>,
    selectedThought: Thought?,
    linkModeActive: Boolean,
    linkSourceId: Long?,
    onThoughtSelected: (Thought) -> Unit,
    onThoughtMoved: (Thought, Float, Float) -> Unit,
    onNodeClicked: (Thought) -> Unit,
    modifier: Modifier = Modifier
) {
    val isLight = MaterialTheme.colorScheme.background == Color.White
    val gridColor = if (isLight) Color(0xFFE2E8F0) else Color(0xFF2D3748)
    val lineColor = if (isLight) Color(0xFF94A3B8) else Color(0xFF475569)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(if (isLight) Color(0xFFF8FAFC) else Color(0xFF121214))
            .testTag("node_canvas")
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()

        // 1. Draw Grid Lines and Connection Lines
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Draw Grid Pattern
            val gridSize = 40.dp.toPx()
            for (x in 0..(size.width / gridSize).toInt()) {
                drawLine(
                    color = gridColor.copy(alpha = 0.4f),
                    start = Offset(x * gridSize, 0f),
                    end = Offset(x * gridSize, size.height),
                    strokeWidth = 1f
                )
            }
            for (y in 0..(size.height / gridSize).toInt()) {
                drawLine(
                    color = gridColor.copy(alpha = 0.4f),
                    start = Offset(0f, y * gridSize),
                    end = Offset(size.width, y * gridSize),
                    strokeWidth = 1f
                )
            }

            // Draw Connection Lines between Thoughts
            connections.forEach { connection ->
                val source = thoughts.find { it.id == connection.sourceId }
                val target = thoughts.find { it.id == connection.targetId }

                if (source != null && target != null) {
                    val startOffset = Offset(
                        x = source.xPosition * size.width,
                        y = source.yPosition * size.height
                    )
                    val endOffset = Offset(
                        x = target.xPosition * size.width,
                        y = target.yPosition * size.height
                    )

                    drawLine(
                        color = lineColor,
                        start = startOffset,
                        end = endOffset,
                        strokeWidth = 4f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                }
            }
        }

        // 2. Draw Interactive Thoughts (Nodes)
        thoughts.forEach { thought ->
            val nodeSize = (32 + (thought.importance * 8)).dp // Scale based on importance
            val density = LocalDensity.current

            // Animated coordinate transitions for smooth dragging and placement
            val posX = thought.xPosition * widthPx
            val posY = thought.yPosition * heightPx

            val nodeX = with(density) { (posX).toDp() } - (nodeSize / 2)
            val nodeY = with(density) { (posY).toDp() } - (nodeSize / 2)

            val isSelected = selectedThought?.id == thought.id
            val isLinkSource = linkSourceId == thought.id

            Box(
                modifier = Modifier
                    .offset(x = nodeX, y = nodeY)
                    .pointerInput(thought.id) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val newX = ((posX + dragAmount.x) / widthPx).coerceIn(0.05f, 0.95f)
                            val newY = ((posY + dragAmount.y) / heightPx).coerceIn(0.05f, 0.95f)
                            onThoughtMoved(thought, newX, newY)
                        }
                    }
                    .testTag("node_${thought.id}")
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.size(nodeSize + 60.dp)
                ) {
                    // Node Sphere
                    Box(
                        modifier = Modifier
                            .size(nodeSize)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = when {
                                        isLinkSource -> listOf(Color(0xFFFF9E80), Color(0xFFFF3D00))
                                        isSelected -> listOf(Color(0xFF80E5FF), Color(0xFF00B0FF))
                                        else -> listOf(Color(0xFF80B3FF), Color(0xFF005EFA))
                                    }
                                ),
                                shape = CircleShape
                            )
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable {
                                onNodeClicked(thought)
                            }
                    )

                    // Node Title Label
                    Card(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .align(Alignment.CenterHorizontally),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                            } else {
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                            }
                        ),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Text(
                            text = thought.title,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        // Empty State Guidance
        if (thoughts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Your mind workspace is empty",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "Tap '+' to add nodes and construct your mental model.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}
