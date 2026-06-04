package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.LyricsParser
import com.example.data.model.SongLyrics
import com.example.data.model.Track
import com.example.ui.theme.GlowMagenta
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.TropicalTeal
import kotlinx.coroutines.launch

@Composable
fun LyricDisplay(
    track: Track,
    progressMs: Long,
    isPlaying: Boolean,
    lyrics: SongLyrics?,
    onSeek: (Long) -> Unit,
    onSaveLyrics: (String) -> Unit,
    onClearCustomLyrics: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showEditorDialog by remember { mutableStateOf(false) }
    var editorTextInput by remember { mutableStateOf("") }
    
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    // Determine active index based on active playback millisecond timing
    val activeIndex = remember(lyrics, progressMs) {
        lyrics?.getActiveLineIndex(progressMs) ?: -1
    }

    // Centered Auto-Scroll with smooth easing whenever the active line shifts
    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0) {
            // Centers the active line at around 30% of scroll screen height offset
            listState.animateScrollToItem(index = activeIndex, scrollOffset = -220)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF07050A)) // Sleek, modern deep space canvas background
            .testTag("lyrics_container")
    ) {
        // Control Bar for Lyrics
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lyrics,
                    contentDescription = null,
                    tint = TropicalTeal,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SYNCED LYRICS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Revert to Default / Reset button
                IconButton(
                    onClick = onClearCustomLyrics,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                        .testTag("lyrics_revert_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset Custom Lyrics",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Edit Button
                IconButton(
                    onClick = {
                        // Pre-populate raw editable timed lyrics
                        editorTextInput = if (lyrics != null) {
                            LyricsParser.format(lyrics)
                        } else {
                            // Dummy start template
                            "[00:00.00] ♫ [Instrumental Intro] ♫\n[00:10.00] Verse 1 starting..."
                        }
                        showEditorDialog = true
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                        .testTag("lyrics_edit_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Edit Song Lyrics",
                        tint = TropicalTeal,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Dividers
        Divider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), thickness = 1.dp)

        // Contents stream
        if (lyrics == null || lyrics.lines.isEmpty()) {
            // Empty State
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = GlowMagenta.copy(alpha = 0.4f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "No Lyrics Available",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Write and synchronize your own custom lyrics for this track using our timed LRC format editor!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Button(
                        onClick = {
                            editorTextInput = "[00:00.00] ♫ [Instrumental Intro] ♫\n[00:10.00] Enter beautiful song verses..."
                            showEditorDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonViolet),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Create Custom Lyrics")
                    }
                }
            }
        } else {
            // Interactive Lazy lyric log lists
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(top = 80.dp, bottom = 120.dp, start = 20.dp, end = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                itemsIndexed(lyrics.lines) { index, line ->
                    val isActive = index == activeIndex
                    
                    // High-quality styling transitions
                    val textSize = if (isActive) 24.sp else 17.sp
                    val textWeight = if (isActive) FontWeight.ExtraBold else FontWeight.SemiBold
                    val textAlpha = if (isActive) 1f else 0.45f
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("lyric_line_$index")
                            .clickable {
                                // Tap to seek directement to lyric verse timestamp!
                                onSeek(line.timestampMs)
                            }
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.Start
                     ) {
                        if (isActive) {
                             // Neon gradient text stroke glow design block
                             Text(
                                 text = line.text,
                                 fontSize = textSize,
                                 fontWeight = textWeight,
                                 lineHeight = 32.sp,
                                 color = TropicalTeal,
                                 modifier = Modifier.shadow(
                                     elevation = 4.dp,
                                     shape = RoundedCornerShape(4.dp),
                                     clip = false,
                                     ambientColor = TropicalTeal,
                                     spotColor = NeonViolet
                                 )
                             )
                        } else {
                            Text(
                                text = line.text,
                                fontSize = textSize,
                                fontWeight = textWeight,
                                lineHeight = 26.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = textAlpha)
                            )
                        }
                    }
                }
            }
        }

        // Floating hints help
        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = NeonViolet,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Tip: Tap on any lyric line to jump playback directly to that position!",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }

        // Custom Timed LRC Lyric Editor Dialog Modal
        if (showEditorDialog) {
            Dialog(onDismissRequest = { showEditorDialog = false }) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.EditNote,
                                contentDescription = null,
                                tint = TropicalTeal,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Timed Lyric Creator",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        Text(
                            text = "Write your synchronized lyrics. Use standard minutes/seconds bracket timestamps like [01:05.30] or millisecond separators like timestampMs|text on each line.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        OutlinedTextField(
                            value = editorTextInput,
                            onValueChange = { editorTextInput = it },
                            placeholder = { Text("[00:00.00] First Verse\n[00:12.50] Second Verse...") },
                            minLines = 8,
                            maxLines = 12,
                            shape = RoundedCornerShape(12.dp),
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("lyrics_editor_text_field")
                        )
                        
                        Spacer(modifier = Modifier.height(14.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { showEditorDialog = false }) {
                                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Button(
                                onClick = {
                                    if (editorTextInput.isNotBlank()) {
                                        onSaveLyrics(editorTextInput)
                                        showEditorDialog = false
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonViolet),
                                enabled = editorTextInput.isNotBlank(),
                                modifier = Modifier.testTag("lyrics_save_confirm_button")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Apply Lyrics")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
