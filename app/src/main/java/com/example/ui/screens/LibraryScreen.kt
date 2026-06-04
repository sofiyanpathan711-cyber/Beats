package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Headset
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.ui.theme.GlowMagenta
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.TropicalTeal

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.rounded.Refresh
import com.example.data.spotify.SpotifyPlaylist
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke

@Composable
fun LibraryScreen(
    favorites: List<Track>,
    playlists: List<PlaylistEntity>,
    selectedPlaylist: PlaylistEntity?,
    playlistTracks: List<Track>,
    currentTrack: Track?,
    isPlaying: Boolean,
    onPlaylistSelected: (PlaylistEntity?) -> Unit,
    onCreatePlaylist: (String, String) -> Unit,
    onDeletePlaylist: (PlaylistEntity) -> Unit,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onRemoveFromPlaylist: (Int, Track) -> Unit,
    onAddToPlaylist: (Int, Track) -> Unit,
    catalog: List<Track>,
    localTracks: List<Track> = emptyList(),
    onScanLocalStorage: () -> Unit = {},
    spotifyIsAuthorized: Boolean = false,
    spotifyPlaylists: List<SpotifyPlaylist> = emptyList(),
    spotifyIsLoading: Boolean = false,
    spotifySelectedPlaylist: SpotifyPlaylist? = null,
    spotifyPlaylistTracks: List<Track> = emptyList(),
    spotifyIsFetchingTracks: Boolean = false,
    onConnectSpotifySandbox: () -> Unit = {},
    onDisconnectSpotify: () -> Unit = {},
    onSelectSpotifyPlaylist: (SpotifyPlaylist?) -> Unit = {},
    onRefreshSpotify: () -> Unit = {},
    onGetSpotifyAuthorizeUrl: () -> String = { "" },
    modifier: Modifier = Modifier
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var activeTab by remember { mutableStateOf(0) } // 0 = Favorites, 1 = Playlists, 2 = Local Files, 3 = Spotify

    val permissionToRequest = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        android.Manifest.permission.READ_MEDIA_AUDIO
    } else {
        android.Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Trigger background storage scan with auto-failing permissions fallback matching
        onScanLocalStorage()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("library_screen")
    ) {
        if (selectedPlaylist != null) {
            // PLAYLIST DETAILS PANEL
            PlaylistDetailView(
                playlist = selectedPlaylist,
                tracks = playlistTracks,
                currentTrack = currentTrack,
                isPlaying = isPlaying,
                catalog = catalog,
                onBack = { onPlaylistSelected(null) },
                onPlayTrack = { track -> onPlayTrack(track, playlistTracks) },
                onAddTrack = { track -> onAddToPlaylist(selectedPlaylist.id, track) },
                onRemoveTrack = { track -> onRemoveFromPlaylist(selectedPlaylist.id, track) }
            )
        } else if (spotifySelectedPlaylist != null) {
            // SPOTIFY PLAYLIST DETAILS PANEL
            SpotifyPlaylistDetailView(
                playlist = spotifySelectedPlaylist,
                tracks = spotifyPlaylistTracks,
                currentTrack = currentTrack,
                isPlaying = isPlaying,
                isFetching = spotifyIsFetchingTracks,
                onBack = { onSelectSpotifyPlaylist(null) },
                onPlayTrack = { track -> onPlayTrack(track, spotifyPlaylistTracks) }
            )
        } else {
            // MASTER LIBRARY SCREEN (TAB SWITCHING)
            Column(modifier = Modifier.fillMaxSize()) {
                
                // Segmented Tabs
                TabRow(
                    selectedTabIndex = activeTab,
                    containerColor = Color.Transparent,
                    contentColor = NeonViolet,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                            color = NeonViolet
                        )
                    },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = { Text("Liked Songs", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium) },
                        modifier = Modifier.testTag("tab_liked_songs")
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = { Text("Playlists", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium) },
                        modifier = Modifier.testTag("tab_playlists")
                    )
                    Tab(
                        selected = activeTab == 2,
                        onClick = { activeTab = 2 },
                        text = { Text("Local Files", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium) },
                        modifier = Modifier.testTag("tab_local_files")
                    )
                    Tab(
                        selected = activeTab == 3,
                        onClick = { activeTab = 3 },
                        text = { Text("Spotify", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium) },
                        modifier = Modifier.testTag("tab_spotify")
                    )
                }

                if (activeTab == 0) {
                    // LIKED SONGS SUB VIEW
                    if (favorites.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.FavoriteBorder,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(72.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Your Liked Songs will appear here",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Tap the heart icon on any streaming track to add it.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 16.dp).padding(top = 4.dp)
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentPadding = PaddingValues(top = 10.dp, bottom = 90.dp)
                        ) {
                            items(favorites) { track ->
                                val isCurrent = currentTrack?.id == track.id
                                ListItem(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 2.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onPlayTrack(track, favorites) }
                                        .testTag("liked_song_row_${track.id}"),
                                    colors = ListItemDefaults.colors(
                                        containerColor = if (isCurrent) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent
                                    ),
                                    leadingContent = {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                        ) {
                                            AsyncImage(
                                                model = track.coverUrl,
                                                contentDescription = "Cover Image",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                    },
                                    headlineContent = {
                                        Text(
                                            text = track.title,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCurrent) TropicalTeal else MaterialTheme.colorScheme.onBackground
                                        )
                                    },
                                    supportingContent = {
                                        Text(text = track.artist, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                )
                            }
                        }
                    }
                } else if (activeTab == 1) {
                    // PLAYLISTS SUB VIEW
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${playlists.size} Created Playlists",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )

                            Button(
                                onClick = { showCreateDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonViolet),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                modifier = Modifier.testTag("create_playlist_trigger")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("New Playlist", fontWeight = FontWeight.Bold)
                            }
                        }

                        if (playlists.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.QueueMusic,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.size(72.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "No playlists found",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Organize streams. Create your first playlist by clicking the button above.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 16.dp).padding(top = 4.dp)
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentPadding = PaddingValues(bottom = 90.dp)
                            ) {
                                items(playlists) { playlist ->
                                    ListItem(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 4.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { onPlaylistSelected(playlist) }
                                            .testTag("playlist_row_${playlist.id}"),
                                        colors = ListItemDefaults.colors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                        ),
                                        leadingContent = {
                                            Box(
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .background(
                                                        Brush.linearGradient(listOf(NeonViolet, GlowMagenta)),
                                                        shape = RoundedCornerShape(10.dp)
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.QueueMusic,
                                                    contentDescription = null,
                                                    tint = Color.White
                                                )
                                            }
                                        },
                                        headlineContent = {
                                            Text(text = playlist.name, fontWeight = FontWeight.Bold)
                                        },
                                        supportingContent = {
                                            Text(
                                                text = if (playlist.description.isNotBlank()) playlist.description else "Stream playlist",
                                                maxLines = 1,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        },
                                        trailingContent = {
                                            IconButton(
                                                onClick = { onDeletePlaylist(playlist) },
                                                modifier = Modifier.testTag("delete_playlist_${playlist.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Delete,
                                                    contentDescription = "Delete Playlist",
                                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                } else if (activeTab == 2) {
                    // LOCAL FILES SUB VIEW
                    LocalTracksSubView(
                        localTracks = localTracks,
                        currentTrack = currentTrack,
                        isPlaying = isPlaying,
                        onPlayTrack = { track -> onPlayTrack(track, localTracks) },
                        onScan = {
                            launcher.launch(permissionToRequest)
                        }
                    )
                } else {
                    // SPOTIFY SUB VIEW
                    SpotifySubView(
                        isAuthorized = spotifyIsAuthorized,
                        playlists = spotifyPlaylists,
                        isLoading = spotifyIsLoading,
                        currentTrack = currentTrack,
                        isPlaying = isPlaying,
                        onConnectSandbox = onConnectSpotifySandbox,
                        onDisconnect = onDisconnectSpotify,
                        onSelectPlaylist = onSelectSpotifyPlaylist,
                        onRefresh = onRefreshSpotify,
                        onGetAuthorizeUrl = onGetSpotifyAuthorizeUrl
                    )
                }
            }
        }

        // Dialog Panel to Create Playlist
        if (showCreateDialog) {
            var playlistName by remember { mutableStateOf("") }
            var playlistDesc by remember { mutableStateOf("") }

            Dialog(onDismissRequest = { showCreateDialog = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = "Create Playlist",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = playlistName,
                            onValueChange = { playlistName = it },
                            label = { Text("Playlist Name") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("create_playlist_name_field")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = playlistDesc,
                            onValueChange = { playlistDesc = it },
                            label = { Text("Optional Description") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("create_playlist_desc_field")
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { showCreateDialog = false }) {
                                Text("Cancel", color = GlowMagenta)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (playlistName.isNotBlank()) {
                                        onCreatePlaylist(playlistName, playlistDesc)
                                        showCreateDialog = false
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonViolet),
                                modifier = Modifier.testTag("create_playlist_save_button")
                            ) {
                                Text("Create", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// Sub-Component: Playlist Detail Screen View
@Composable
fun PlaylistDetailView(
    playlist: PlaylistEntity,
    tracks: List<Track>,
    currentTrack: Track?,
    isPlaying: Boolean,
    catalog: List<Track>,
    onBack: () -> Unit,
    onPlayTrack: (Track) -> Unit,
    onAddTrack: (Track) -> Unit,
    onRemoveTrack: (Track) -> Unit
) {
    var showAddSongsDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("playlist_detail_panel")
    ) {
        // Back toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("playlist_detail_back_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Go back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Back to Library",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Beautiful playlist header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .background(
                        Brush.linearGradient(listOf(NeonViolet, GlowMagenta)),
                        shape = RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.QueueMusic,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (playlist.description.isNotBlank()) playlist.description else "Stream tracks selection",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${tracks.size} tracks",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = TropicalTeal
                )
            }
        }

        // Toolbar Action Row: Play & Add Songs buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { if (tracks.isNotEmpty()) onPlayTrack(tracks[0]) },
                enabled = tracks.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = TropicalTeal, contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("playlist_play_party_button")
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Play All", fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { showAddSongsDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onBackground
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("playlist_add_songs_trigger")
            ) {
                Icon(Icons.Rounded.PlaylistAdd, contentDescription = null, tint = NeonViolet)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Songs", fontWeight = FontWeight.Bold)
            }
        }

        Divider(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        )

        // Songs inside playlist
        if (tracks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No songs added to this playlist yet.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                items(tracks) { track ->
                    val isCurrent = currentTrack?.id == track.id
                    ListItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 2.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onPlayTrack(track) }
                            .testTag("playlist_track_row_${track.id}"),
                        colors = ListItemDefaults.colors(
                            containerColor = if (isCurrent) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent
                        ),
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(6.dp))
                            ) {
                                AsyncImage(
                                    model = track.coverUrl,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        },
                        headlineContent = {
                            Text(
                                text = track.title,
                                fontWeight = FontWeight.Bold,
                                color = if (isCurrent) TropicalTeal else MaterialTheme.colorScheme.onBackground
                            )
                        },
                        supportingContent = {
                            Text(text = track.artist, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        trailingContent = {
                            IconButton(
                                onClick = { onRemoveTrack(track) },
                                modifier = Modifier.testTag("remove_track_from_playlist_${track.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RemoveCircleOutline,
                                    contentDescription = "Remove Track",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                )
                            }
                        }
                    )
                }
            }
        }
    }

    // Add Songs Modal Dialog Browser
    if (showAddSongsDialog) {
        Dialog(onDismissRequest = { showAddSongsDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = "Add Songs to Playlist",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    val availableTracks = catalog.filter { catTrack ->
                        tracks.none { it.id == catTrack.id }
                    }

                    if (availableTracks.isEmpty()) {
                        Text(
                            text = "All catalog streams are already in this playlist!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp)
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 280.dp)
                        ) {
                            items(availableTracks) { track ->
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onAddTrack(track)
                                            showAddSongsDialog = false
                                        }
                                        .testTag("add_track_dialog_row_${track.id}")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                        ) {
                                            AsyncImage(
                                                model = track.coverUrl,
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = track.title,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = track.artist,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Default.AddCircle,
                                            contentDescription = "Add Track",
                                            tint = TropicalTeal
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    TextButton(
                        onClick = { showAddSongsDialog = false },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Done", color = GlowMagenta, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun LocalTracksSubView(
    localTracks: List<Track>,
    currentTrack: Track?,
    isPlaying: Boolean,
    onPlayTrack: (Track) -> Unit,
    onScan: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("local_tracks_subview")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${localTracks.size} Local Audio Files",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )

            Button(
                onClick = onScan,
                colors = ButtonDefaults.buttonColors(containerColor = NeonViolet),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                modifier = Modifier.testTag("scan_local_storage_button")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Scan Storage", fontWeight = FontWeight.Bold)
            }
        }

        if (localTracks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Rounded.Headset,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No local tracks found",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "We will scan your device's external storage and app folders for media files, parse metadata, and populate the player library.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp).padding(top = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onScan,
                        colors = ButtonDefaults.buttonColors(containerColor = TropicalTeal, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("scan_now_button")
                    ) {
                        Text("Scan Storage Now", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                items(localTracks) { track ->
                    val isCurrent = currentTrack?.id == track.id
                    ListItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onPlayTrack(track) }
                            .testTag("local_track_row_${track.id}"),
                        colors = ListItemDefaults.colors(
                            containerColor = if (isCurrent) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        Brush.linearGradient(listOf(NeonViolet, GlowMagenta)),
                                        shape = RoundedCornerShape(10.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Headset,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            }
                        },
                        headlineContent = {
                            Text(
                                text = track.title, 
                                fontWeight = FontWeight.Bold,
                                color = if (isCurrent) TropicalTeal else MaterialTheme.colorScheme.onBackground
                            )
                        },
                        supportingContent = {
                            Column {
                                Text(
                                    text = "${track.artist} • ${track.album}", 
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    SuggestionChip(
                                        onClick = {},
                                        label = { Text(track.genre, fontSize = 10.sp) },
                                        modifier = Modifier.height(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = formatMs(track.durationMs),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

private fun formatMs(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

@Composable
fun SpotifySubView(
    isAuthorized: Boolean,
    playlists: List<SpotifyPlaylist>,
    isLoading: Boolean,
    currentTrack: Track?,
    isPlaying: Boolean,
    onConnectSandbox: () -> Unit,
    onDisconnect: () -> Unit,
    onSelectPlaylist: (SpotifyPlaylist?) -> Unit,
    onRefresh: () -> Unit,
    onGetAuthorizeUrl: () -> String
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("spotify_subview")
    ) {
        if (!isAuthorized) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(Color(0xFF1DB954), shape = RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "Spotify Logo",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Text(
                    text = "Spotify Library Link",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                
                Text(
                    text = "Import and play your personal Spotify playlists directly within the Beats UI.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Real Account Connection",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TropicalTeal
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Opens a secure Spotify authorization page in your local browser.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                val url = onGetAuthorizeUrl()
                                if (url.isNotEmpty()) {
                                    try {
                                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(context, "Cannot open browser. URL: $url", android.widget.Toast.LENGTH_LONG).show()
                                    }
                                } else {
                                    android.widget.Toast.makeText(context, "Spotify Client ID is not configured. Look at the Secrets panel of Google AI Studio!", android.widget.Toast.LENGTH_LONG).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("spotify_connect_real_btn")
                        ) {
                            Text("Connect Real Spotify Client", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Demo Simulator Sandbox",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = NeonViolet
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "No developer API credentials? Connect a beautifully simulated mock account with authentic cyberpunk/lo-fi tracks in 1 second!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = onConnectSandbox,
                            border = BorderStroke(2.dp, NeonViolet),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("spotify_connect_sandbox_btn")
                        ) {
                            Text("Launch Demo Sandbox", fontWeight = FontWeight.Black, color = NeonViolet)
                        }
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Spotify Library",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = "${playlists.size} playlists active",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier.testTag("spotify_refresh_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Playlists", tint = TropicalTeal)
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    IconButton(
                        onClick = onDisconnect,
                        modifier = Modifier.testTag("spotify_logout_button")
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Disconnect Spotify", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF1DB954))
                }
            } else if (playlists.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.QueueMusic, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(72.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No playlists found", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(bottom = 90.dp)
                ) {
                    items(playlists) { playlist ->
                        val imageUrl = playlist.images?.firstOrNull()?.url ?: "https://images.unsplash.com/photo-1614680376593-902f74fa0d41?w=500"
                        ListItem(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSelectPlaylist(playlist) }
                                .testTag("spotify_playlist_row_${playlist.id}"),
                            colors = ListItemDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            leadingContent = {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                ) {
                                    AsyncImage(
                                        model = imageUrl,
                                        contentDescription = "Playlist Cover",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            },
                            headlineContent = {
                                Text(playlist.name, fontWeight = FontWeight.Bold, color = Color.White)
                            },
                            supportingContent = {
                                Text(
                                    text = "${playlist.tracks.total} Tracks • By ${playlist.owner?.displayName ?: "Spotify User"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingContent = {
                                Icon(Icons.Default.ArrowForward, contentDescription = "", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SpotifyPlaylistDetailView(
    playlist: SpotifyPlaylist,
    tracks: List<Track>,
    currentTrack: Track?,
    isPlaying: Boolean,
    isFetching: Boolean,
    onBack: () -> Unit,
    onPlayTrack: (Track) -> Unit
) {
    val coverUrl = playlist.images?.firstOrNull()?.url ?: "https://images.unsplash.com/photo-1614680376593-902f74fa0d41?w=500"
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("spotify_playlist_detail_panel")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("spotify_playlist_detail_back_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Go back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Back to Spotify Playlists",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                AsyncImage(
                    model = coverUrl,
                    contentDescription = "Playlist Cover",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = playlist.description ?: "Imported Spotify Playlist",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
                
                Spacer(modifier = Modifier.height(2.dp))
                
                Text(
                    text = "${playlist.tracks.total} tracks • By ${playlist.owner?.displayName ?: "User"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TropicalTeal,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (tracks.isNotEmpty()) {
                Button(
                    onClick = { onPlayTrack(tracks.first()) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("spotify_playlist_play_first_button")
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Play Live", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        Spacer(
            modifier = Modifier
                .padding(vertical = 8.dp, horizontal = 16.dp)
                .height(1.dp)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        )

        if (isFetching) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF1DB954))
            }
        } else if (tracks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No tracks loaded or found.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(top = 4.dp, bottom = 90.dp)
            ) {
                items(tracks) { track ->
                    val isCurrent = currentTrack?.id == track.id
                    ListItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 3.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onPlayTrack(track) }
                            .testTag("spotify_track_row_${track.id}"),
                        colors = ListItemDefaults.colors(
                            containerColor = if (isCurrent) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent
                        ),
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(6.dp))
                            ) {
                                AsyncImage(
                                    model = track.coverUrl,
                                    contentDescription = "Track Cover",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        },
                        headlineContent = {
                            Text(
                                text = track.title,
                                fontWeight = FontWeight.Bold,
                                color = if (isCurrent) Color(0xFF1DB954) else Color.White
                            )
                        },
                        supportingContent = {
                            Text(
                                text = "${track.artist} • ${track.album}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        },
                        trailingContent = {
                            Text(
                                text = formatMs(track.durationMs),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }
        }
    }
}
