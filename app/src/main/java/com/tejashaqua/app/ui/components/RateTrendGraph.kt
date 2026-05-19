package com.tejashaqua.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tejashaqua.app.ui.theme.AquaBlue
import com.tejashaqua.app.ui.theme.GrayText

@Composable
fun RateTrendGraph(
    data: List<Double>,
    labels: List<String>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return

    val textMeasurer = rememberTextMeasurer()
    val priceTextStyle = TextStyle(
        color = AquaBlue,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold
    )

    val maxValue = data.maxOrNull() ?: 0.0
    val minValue = data.minOrNull() ?: 0.0
    val baseRange = (maxValue - minValue).coerceAtLeast(1.0)
    
    // Add 20% headroom at top for labels and 10% at bottom for breathing room
    val displayMax = maxValue + (baseRange * 0.25)
    val displayMin = (minValue - (baseRange * 0.1)).coerceAtLeast(0.0)
    val displayRange = (displayMax - displayMin).coerceAtLeast(1.0)
    
    Column(modifier = modifier) {
        Box(modifier = Modifier.fillMaxWidth().height(220.dp)) {
            Canvas(modifier = Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp)) {
                val width = size.width
                val height = size.height
                val spaceBetweenPoints = width / (data.size - 1).coerceAtLeast(1)
                
                val points = data.mapIndexed { index, value ->
                    val x = index * spaceBetweenPoints
                    val y = height - ((value - displayMin) / displayRange * height).toFloat()
                    Offset(x, y)
                }

                val path = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    points.drop(1).forEach { lineTo(it.x, it.y) }
                }

                // Fill under the line
                val fillPath = Path().apply {
                    addPath(path)
                    lineTo(points.last().x, height)
                    lineTo(points.first().x, height)
                    close()
                }

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(AquaBlue.copy(alpha = 0.3f), Color.Transparent)
                    )
                )

                // Draw the line
                drawPath(
                    path = path,
                    color = AquaBlue,
                    style = Stroke(width = 3.dp.toPx())
                )

                // Draw points and price labels
                points.forEachIndexed { index, offset ->
                    drawCircle(
                        color = AquaBlue,
                        radius = 4.dp.toPx(),
                        center = offset
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 2.dp.toPx(),
                        center = offset
                    )

                    // Draw price text above the dot with a small white background for clarity
                    val priceText = "₹${data[index].toInt()}"
                    val textLayoutResult = textMeasurer.measure(priceText, priceTextStyle)
                    val textPos = Offset(
                        x = offset.x - textLayoutResult.size.width / 2,
                        y = offset.y - textLayoutResult.size.height - 6.dp.toPx()
                    )
                    
                    // Draw a semi-transparent white box behind text if it's too close to the line
                    drawRect(
                        color = Color.White.copy(alpha = 0.7f),
                        topLeft = textPos.copy(x = textPos.x - 2.dp.toPx(), y = textPos.y),
                        size = androidx.compose.ui.geometry.Size(
                            width = textLayoutResult.size.width.toFloat() + 4.dp.toPx(),
                            height = textLayoutResult.size.height.toFloat()
                        )
                    )

                    drawText(
                        textLayoutResult = textLayoutResult,
                        topLeft = textPos
                    )
                }
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            labels.forEach { label ->
                Text(text = label, fontSize = 10.sp, color = GrayText)
            }
        }
    }
}
