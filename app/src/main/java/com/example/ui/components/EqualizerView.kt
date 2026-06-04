package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.example.ui.theme.GlowMagenta
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.TropicalTeal

@Composable
fun EqualizerView(
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val barCount = 12
    val animationStates = List(barCount) { index ->
        val duration = 600 + (index * 120) % 500
        val infiniteTransition = rememberInfiniteTransition(label = "eq_$index")
        
        if (isPlaying) {
            infiniteTransition.animateFloat(
                initialValue = 0.15f,
                targetValue = 0.95f,
                animationSpec = infiniteRepeatable(
                    animation = tween(duration, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar_$index"
            )
        } else {
            remember { mutableStateOf(0.12f) }
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        val width = size.width
        val height = size.height
        val spacing = 12.dp.toPx()
        val totalSpacing = spacing * (barCount - 1)
        val barWidth = (width - totalSpacing) / barCount

        val gradient = Brush.linearGradient(
            colors = listOf(NeonViolet, GlowMagenta, TropicalTeal)
        )

        for (i in 0 until barCount) {
            val barHeightState = animationStates[i].value
            val barHeight = height * barHeightState
            val x = i * (barWidth + spacing)
            val y = height - barHeight

            drawRoundRect(
                brush = gradient,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
            )
        }
    }
}
