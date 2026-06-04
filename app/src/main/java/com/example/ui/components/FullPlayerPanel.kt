package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.local.PlaylistEntity
import com.example.data.model.Track
import com.example.playback.MediaPlaybackManager
import com.example.ui.theme.GlowMagenta
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.TropicalTeal

@Composable
fun FullPlayerPanel(
    track: Track,
    isPlaying: Boolean,
    progressMs: Long,
    durationMs: Long,
    shuffleMode: Boolean,
    repeatMode: MediaPlaybackManager.RepeatMode,
    isFavorite: Boolean,
    playlists: List<PlaylistEntity>,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: (Int) -> Unit,
    onClose: () -> Unit,
    lyrics: com.example.data.model.SongLyrics?,
    onSaveLyrics: (String) -> Unit,
    onClearCustomLyrics: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showPlaylistDialog by remember { mutableStateOf(false) }

    // Vinyl album rotating animation
    val infiniteTransition = rememberInfiniteTransition(label = "vinyl_spin")
    val rotationAngle by if (isPlaying) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(20000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "spin"
        )
    } else {
        remember { mutableStateOf(0f) }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("full_player_panel")
    ) {
        // Decorative cosmic blurred circles behind
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 200.dp)
        ) {
            // Gradient atmosphere
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                NeonViolet.copy(alpha = 0.2f),
                                GlowMagenta.copy(alpha = 0.1f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        var showLyrics by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Action view
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("player_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Collapse Player",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Interactive Premium Pill Switcher
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(30.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (!showLyrics) TropicalTeal else Color.Transparent)
                            .clickable { showLyrics = false }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                            .testTag("player_track_tab")
                    ) {
                        Text(
                            text = "TRACK",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = if (!showLyrics) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (showLyrics) TropicalTeal else Color.Transparent)
                            .clickable { showLyrics = true }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                            .testTag("player_lyrics_tab")
                    ) {
                        Text(
                            text = "LYRICS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = if (showLyrics) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("player_favorite_button")
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isFavorite) "Unlike Track" else "Like Track",
                        tint = if (isFavorite) GlowMagenta else MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Central Area: Swappable Album Disk & Visualizer vs. Scrolling Lyrics Stream
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                if (!showLyrics) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // High-Craft Vinyl Cover Disk
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(260.dp)
                                    .rotate(rotationAngle)
                                    .clip(CircleShape)
                                    .background(Color(0xFF0D0B14))
                                    .border(
                                        width = 6.dp,
                                        brush = Brush.linearGradient(listOf(NeonViolet, GlowMagenta, TropicalTeal)),
                                        shape = CircleShape
                                    )
                                    .padding(16.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black)
                                    .padding(2.dp)
                                    .border(4.dp, Color(0xFF1E1C29), CircleShape)
                                    .padding(20.dp)
                                    .clip(CircleShape)
                            ) {
                                // Center Album Art
                                AsyncImage(
                                    model = track.coverUrl,
                                    contentDescription = "${track.title} Album Cover Art",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            // Standard vinyl needle pinhole details
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .background(Color.Black, CircleShape)
                                    .border(4.dp, Color.White, CircleShape)
                            )
                        }

                        // Song Info Section
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onBackground,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = track.artist,
                                style = MaterialTheme.typography.titleMedium,
                                color = TropicalTeal,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = track.album,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center
                            )
                        }

                        // Visualizer view
                        Spacer(modifier = Modifier.height(8.dp))
                        EqualizerView(isPlaying = isPlaying)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                } else {
                    // Modern Synced Scrolling Lyrics
                    LyricDisplay(
                        track = track,
                        progressMs = progressMs,
                        isPlaying = isPlaying,
                        lyrics = lyrics,
                        onSeek = onSeek,
                        onSaveLyrics = onSaveLyrics,
                        onClearCustomLyrics = onClearCustomLyrics,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Seek bar component
            Column(modifier = Modifier.fillMaxWidth()) {
                val currentSliderValue = remember(progressMs) { progressMs.toFloat() }
                Slider(
                    value = currentSliderValue,
                    onValueChange = { onSeek(it.toLong()) },
                    valueRange = 0f..durationMs.coerceAtLeast(1000L).toFloat(),
                    colors = SliderDefaults.colors(
                        activeTrackColor = NeonViolet,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                        thumbColor = TropicalTeal
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("player_seek_slider")
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(progressMs),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatTime(durationMs),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Playback Actions Section Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle Mode Button
                IconButton(
                    onClick = onToggleShuffle,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("player_shuffle_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Toggle Shuffle",
                        tint = if (shuffleMode) TropicalTeal else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Previous
                IconButton(
                    onClick = onPrevious,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("player_prev_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous Track",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Play / Pause Circle brush button
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(NeonViolet, GlowMagenta)))
                        .clickable { onPlayPause() }
                        .testTag("player_play_pause_fab"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Next
                IconButton(
                    onClick = onNext,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("player_next_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next Track",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Repeat Mode Button
                IconButton(
                    onClick = onToggleRepeat,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("player_repeat_button")
                ) {
                    val repeatIcon = when (repeatMode) {
                        MediaPlaybackManager.RepeatMode.ONE -> Icons.Default.RepeatOne
                        else -> Icons.Default.Repeat
                    }
                    val isActive = repeatMode != MediaPlaybackManager.RepeatMode.OFF
                    Icon(
                        imageVector = repeatIcon,
                        contentDescription = "Toggle Repeat",
                        tint = if (isActive) TropicalTeal else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Playlist allocation controls
            Button(
                onClick = { showPlaylistDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onBackground
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("player_add_to_playlist_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlaylistAdd,
                    contentDescription = null,
                    tint = TropicalTeal,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Add to Playlist",
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }

    // Playlist Selection Dialog Modal
    if (showPlaylistDialog) {
        Dialog(onDismissRequest = { showPlaylistDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Add to Playlist",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    if (playlists.isEmpty()) {
                        Text(
                            text = "You haven't created any playlists yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                        Button(
                            onClick = { showPlaylistDialog = false },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Go back")
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp)
                        ) {
                            items(playlists) { playlist ->
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable {
                                            onAddToPlaylist(playlist.id)
                                            showPlaylistDialog = false
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.QueueMusic,
                                            contentDescription = null,
                                            tint = NeonViolet,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = playlist.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        TextButton(onClick = { showPlaylistDialog = false }) {
                            Text("Cancel", color = GlowMagenta)
                        }
                    }
                }
            }
        }
    }
}

// Utility function to format progress and total durations
fun formatTime(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
