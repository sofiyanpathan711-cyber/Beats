package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import coil.compose.AsyncImage
import com.example.data.model.Track
import com.example.ui.theme.GlowMagenta
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.TropicalTeal

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BottomPlayBar(
    track: Track,
    isPlaying: Boolean,
    progressMs: Long,
    durationMs: Long,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onFavoriteToggle: () -> Unit,
    isFavorite: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "album_art_scale"
    )
    val scale by animateFloatAsState(
        targetValue = if (isPlaying) pulseScale else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "art_active_scale"
    )

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.98f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("bottom_play_bar")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(top = 6.dp, bottom = 10.dp, start = 14.dp, end = 14.dp)
        ) {
            // 1. Sleek, thin seek bar controller
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val currentSliderValue = remember(progressMs) { progressMs.toFloat() }
                var isDragging by remember { mutableStateOf(false) }
                var localPosition by remember { mutableStateOf(0f) }

                Text(
                    text = formatTime(if (isDragging) localPosition.toLong() else progressMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 6.dp)
                )

                Slider(
                    value = if (isDragging) localPosition else currentSliderValue,
                    onValueChange = {
                        isDragging = true
                        localPosition = it
                    },
                    onValueChangeFinished = {
                        isDragging = false
                        onSeek(localPosition.toLong())
                    },
                    valueRange = 0f..durationMs.coerceAtLeast(1000L).toFloat(),
                    colors = SliderDefaults.colors(
                        activeTrackColor = NeonViolet,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        thumbColor = TropicalTeal
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(20.dp)
                        .testTag("bottom_play_seek_slider")
                )

                Text(
                    text = formatTime(durationMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 2. Control & Art layout
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Album art with neon border
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.linearGradient(listOf(NeonViolet, GlowMagenta))
                        )
                        .padding(1.dp)
                        .clip(RoundedCornerShape(9.dp))
                ) {
                    AsyncImage(
                        model = track.coverUrl,
                        contentDescription = "Cover for ${track.title}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Song Info
                Column(
                    modifier = Modifier
                        .weight(1.2f)
                        .padding(end = 4.dp)
                ) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
                    )
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }

                // Buttons container
                Row(
                    modifier = Modifier.weight(2.0f),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Like button
                    IconButton(
                        onClick = onFavoriteToggle,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("bottom_play_favorite_button")
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = "Like Track",
                            tint = if (isFavorite) GlowMagenta else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Skip Previous track
                    IconButton(
                        onClick = onPrevious,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("bottom_play_prev_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipPrevious,
                            contentDescription = "Previous Track",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Replay 10 Seconds
                    IconButton(
                        onClick = { onSeek((progressMs - 10000L).coerceAtLeast(0L)) },
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("bottom_play_replay_10_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Replay10,
                            contentDescription = "Rewind 10 Seconds",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Play/Pause button
                    IconButton(
                        onClick = onPlayPause,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("bottom_play_pause_button")
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(listOf(NeonViolet, GlowMagenta)),
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Forward 10 Seconds
                    IconButton(
                        onClick = { onSeek((progressMs + 10000L).coerceAtMost(durationMs)) },
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("bottom_play_forward_10_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Forward10,
                            contentDescription = "Forward 10 Seconds",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Skip Next track
                    IconButton(
                        onClick = onNext,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("bottom_play_next_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipNext,
                            contentDescription = "Next Track",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
