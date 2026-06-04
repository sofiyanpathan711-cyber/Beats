package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.BottomPlayBar
import com.example.ui.components.FullPlayerPanel
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.EqualizerScreen
import com.example.ui.theme.BeatsTheme
import androidx.compose.material.icons.filled.Tune
import com.example.ui.theme.GlowMagenta
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.TropicalTeal
import com.example.ui.viewmodel.MusicViewModel
import androidx.activity.compose.BackHandler

class MainActivity : ComponentActivity() {
    private val viewModel: MusicViewModel by lazy {
        androidx.lifecycle.ViewModelProvider(this)[MusicViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Firebase / local AuthManager fallback
        com.example.data.auth.AuthManager.initialize(applicationContext)

        // Handle cold start deep links
        intent?.data?.let { handleDeepLink(it) }

        setContent {
            BeatsTheme {
                val authState by com.example.data.auth.AuthManager.authState.collectAsState()
                if (authState is com.example.data.auth.AuthState.Authenticated) {
                    MainAppAssembly(viewModel = viewModel)
                } else {
                    com.example.ui.screens.AuthScreen(
                        onAuthSuccess = {
                            // Handled reactively
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.data?.let { handleDeepLink(it) }
    }

    private fun handleDeepLink(uri: android.net.Uri) {
         if (uri.scheme == "beats" && uri.host == "spotify-callback") {
             viewModel.handleSpotifyCallback(uri)
         }
    }
}

@Composable
fun MainAppAssembly(
    viewModel: MusicViewModel = viewModel()
) {
    val currentTab by viewModel.currentTab.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filteredTracks by viewModel.filteredTracks.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val searchHistory by viewModel.searchHistory.collectAsState()
    
    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val playbackProgress by viewModel.playbackProgress.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val shuffleMode by viewModel.shuffleMode.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val selectedPlaylist by viewModel.selectedPlaylist.collectAsState()
    val playlistTracks by viewModel.playlistTracks.collectAsState()
    val spotifySelectedPlaylist by viewModel.spotifySelectedPlaylist.collectAsState()

    val isPlayerExpanded by viewModel.isPlayerExpanded.collectAsState()
    val isEqualizerExpanded by viewModel.isEqualizerExpanded.collectAsState()
    val currentEqualizerSettings by viewModel.currentEqualizerSettings.collectAsState()
    val equalizerPresets by viewModel.equalizerPresets.collectAsState()
    val currentLyrics by viewModel.currentLyrics.collectAsState()

    // Handle physical back clicks elegantly
    if (isEqualizerExpanded) {
        BackHandler {
            viewModel.setEqualizerExpanded(false)
        }
    } else if (isPlayerExpanded) {
        BackHandler {
            viewModel.togglePlayerExpand(false)
        }
    } else if (currentTab == MusicViewModel.TabDestination.LIBRARY && selectedPlaylist != null) {
        BackHandler {
            viewModel.selectPlaylist(null)
        }
    } else if (currentTab == MusicViewModel.TabDestination.LIBRARY && spotifySelectedPlaylist != null) {
        BackHandler {
            viewModel.selectSpotifyPlaylist(null)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("app_scaffold"),
        topBar = {
            if (!isPlayerExpanded) {
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    tonalElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Futuristic gradient Beats rounded card logo
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Brush.linearGradient(listOf(NeonViolet, GlowMagenta))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(10.dp))
                        
                        Text(
                            text = "BEATS",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 3.sp,
                            color = TropicalTeal
                        )
                        
                        Spacer(modifier = Modifier.weight(1f))

                        TextButton(
                            onClick = { com.example.data.auth.AuthManager.signOut() },
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .testTag("logout_button")
                        ) {
                            Text(
                                text = "SIGN OUT",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = NeonViolet,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = { viewModel.setEqualizerExpanded(true) },
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .testTag("eq_header_toggle")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Open Studio Equalizer",
                                tint = TropicalTeal
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            if (!isPlayerExpanded) {
                Column {
                    // Inline Bottom Player Bar when there is active media
                    currentTrack?.let { activeTrack ->
                        var isFavState by remember(activeTrack, favorites) {
                            mutableStateOf(favorites.any { it.id == activeTrack.id })
                        }
                        
                        BottomPlayBar(
                            track = activeTrack,
                            isPlaying = isPlaying,
                            progressMs = playbackProgress,
                            durationMs = duration,
                            onPlayPause = { viewModel.togglePlayPause() },
                            onPrevious = { viewModel.playPrevious() },
                            onNext = { viewModel.playNext() },
                            onSeek = { viewModel.seekTo(it) },
                            onFavoriteToggle = { viewModel.toggleFavorite(activeTrack) },
                            isFavorite = isFavState,
                            onClick = { viewModel.togglePlayerExpand(true) }
                        )
                    }

                    // Master destination navigation tabs
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.background,
                        tonalElevation = 8.dp,
                        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        NavigationBarItem(
                            selected = currentTab == MusicViewModel.TabDestination.HOME,
                            onClick = { viewModel.selectTab(MusicViewModel.TabDestination.HOME) },
                            icon = { Icon(Icons.Rounded.Explore, contentDescription = null) },
                            label = { Text("Explore") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = NeonViolet,
                                selectedTextColor = NeonViolet,
                                indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.testTag("nav_explore")
                        )
                        NavigationBarItem(
                            selected = currentTab == MusicViewModel.TabDestination.SEARCH,
                            onClick = { viewModel.selectTab(MusicViewModel.TabDestination.SEARCH) },
                            icon = { Icon(Icons.Default.Search, contentDescription = null) },
                            label = { Text("Search") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = NeonViolet,
                                selectedTextColor = NeonViolet,
                                indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.testTag("nav_search")
                        )
                        NavigationBarItem(
                            selected = currentTab == MusicViewModel.TabDestination.LIBRARY,
                            onClick = { viewModel.selectTab(MusicViewModel.TabDestination.LIBRARY) },
                            icon = { Icon(Icons.Default.LibraryMusic, contentDescription = null) },
                            label = { Text("Library") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = NeonViolet,
                                selectedTextColor = NeonViolet,
                                indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.testTag("nav_library")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tab contents navigation switch
            when (currentTab) {
                MusicViewModel.TabDestination.HOME -> {
                    DashboardScreen(
                        catalog = viewModel.getCatalog(),
                        currentTrack = currentTrack,
                        isPlaying = isPlaying,
                        favorites = favorites,
                        onPlayTrack = { track, list -> viewModel.playTrack(track, list) },
                        onToggleFavorite = { track -> viewModel.toggleFavorite(track) }
                    )
                }
                MusicViewModel.TabDestination.SEARCH -> {
                    SearchScreen(
                        query = searchQuery,
                        onQueryChange = { viewModel.setQuery(it) },
                        filteredTracks = filteredTracks,
                        currentTrack = currentTrack,
                        isPlaying = isPlaying,
                        favorites = favorites,
                        onPlayTrack = { track, list -> 
                            viewModel.playTrack(track, list)
                            // Auto-add played query to search history
                            if (searchQuery.isNotBlank()) {
                                viewModel.addSearchQueryToHistory(searchQuery)
                            }
                        },
                        onToggleFavorite = { track -> viewModel.toggleFavorite(track) },
                        searchHistory = searchHistory,
                        onHistoryItemClick = { queryItem ->
                            viewModel.setQuery(queryItem)
                            viewModel.addSearchQueryToHistory(queryItem)
                        },
                        onHistoryItemDelete = { queryItem ->
                            viewModel.removeSearchQueryFromHistory(queryItem)
                        },
                        onClearHistory = {
                            viewModel.clearSearchHistory()
                        }
                    )
                }
                MusicViewModel.TabDestination.LIBRARY -> {
                    val localTracks by viewModel.localTracks.collectAsState()
                    val spotifyIsAuthorized by viewModel.spotifyIsAuthorized.collectAsState()
                    val spotifyPlaylists by viewModel.spotifyPlaylists.collectAsState()
                    val spotifyIsLoading by viewModel.spotifyIsLoading.collectAsState()
                    val spotifyPlaylistTracks by viewModel.spotifyPlaylistTracks.collectAsState()
                    val spotifyIsFetchingTracks by viewModel.spotifyIsFetchingTracks.collectAsState()

                    LibraryScreen(
                        favorites = favorites,
                        playlists = playlists,
                        selectedPlaylist = selectedPlaylist,
                        playlistTracks = playlistTracks,
                        currentTrack = currentTrack,
                        isPlaying = isPlaying,
                        onPlaylistSelected = { viewModel.selectPlaylist(it) },
                        onCreatePlaylist = { name, desc -> viewModel.createPlaylist(name, desc) },
                        onDeletePlaylist = { viewModel.deletePlaylist(it) },
                        onPlayTrack = { track, list -> viewModel.playTrack(track, list) },
                        onRemoveFromPlaylist = { id, track -> viewModel.removeTrackFromPlaylist(id, track) },
                        onAddToPlaylist = { id, track -> viewModel.addTrackToPlaylist(id, track) },
                        catalog = viewModel.getCatalog(),
                        localTracks = localTracks,
                        onScanLocalStorage = { viewModel.scanLocalStorage() },
                        spotifyIsAuthorized = spotifyIsAuthorized,
                        spotifyPlaylists = spotifyPlaylists,
                        spotifyIsLoading = spotifyIsLoading,
                        spotifySelectedPlaylist = spotifySelectedPlaylist,
                        spotifyPlaylistTracks = spotifyPlaylistTracks,
                        spotifyIsFetchingTracks = spotifyIsFetchingTracks,
                        onConnectSpotifySandbox = { viewModel.connectSpotifySandbox() },
                        onDisconnectSpotify = { viewModel.disconnectSpotify() },
                        onSelectSpotifyPlaylist = { viewModel.selectSpotifyPlaylist(it) },
                        onRefreshSpotify = { viewModel.refreshSpotify() },
                        onGetSpotifyAuthorizeUrl = { viewModel.getSpotifyAuthorizeUrl() }
                    )
                }
            }
        }
    }

    // Floating Overlay Layer: Full screen media player sheet with slide animation
    AnimatedVisibility(
        visible = isPlayerExpanded,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
    ) {
        currentTrack?.let { activeTrack ->
            val isFavState by viewModel.isTrackFavorite(activeTrack.id).collectAsState()
            FullPlayerPanel(
                track = activeTrack,
                isPlaying = isPlaying,
                progressMs = playbackProgress,
                durationMs = duration,
                shuffleMode = shuffleMode,
                repeatMode = repeatMode,
                isFavorite = isFavState,
                playlists = playlists,
                onPlayPause = { viewModel.togglePlayPause() },
                onPrevious = { viewModel.playPrevious() },
                onNext = { viewModel.playNext() },
                onSeek = { viewModel.seekTo(it) },
                onToggleShuffle = { viewModel.toggleShuffle() },
                onToggleRepeat = { viewModel.toggleRepeat() },
                onToggleFavorite = { viewModel.toggleFavorite(activeTrack) },
                onAddToPlaylist = { pId -> viewModel.addTrackToPlaylist(pId, activeTrack) },
                onClose = { viewModel.togglePlayerExpand(false) },
                lyrics = currentLyrics,
                onSaveLyrics = { text -> viewModel.saveTrackLyrics(activeTrack.id, text) },
                onClearCustomLyrics = { viewModel.clearCustomTrackLyrics(activeTrack.id) }
            )
        }
    }

    // Floating Overlay Layer: Full screen synthesizer equalizer pane sheet with slide animation
    AnimatedVisibility(
        visible = isEqualizerExpanded,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
    ) {
        EqualizerScreen(
            currentSettings = currentEqualizerSettings,
            presets = equalizerPresets,
            onPresetSelected = { viewModel.selectEqualizerPreset(it) },
            onBandUpdated = { index, db -> viewModel.updateEqualizerBand(index, db) },
            onSavePreset = { viewModel.saveEqualizerPreset(it) },
            onDeletePreset = { viewModel.deleteEqualizerPreset(it) },
            onClose = { viewModel.setEqualizerExpanded(false) }
        )
    }
}
