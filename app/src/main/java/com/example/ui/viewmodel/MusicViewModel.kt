package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.PlaylistEntity
import com.example.data.model.Track
import com.example.data.repository.MusicRepository
import com.example.data.spotify.SpotifyManager
import com.example.data.spotify.SpotifyPlaylist
import com.example.playback.MediaPlaybackManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MusicRepository

    // Search stream
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Screen navigation tabs
    enum class TabDestination {
        HOME, SEARCH, LIBRARY
    }
    private val _currentTab = MutableStateFlow(TabDestination.HOME)
    val currentTab: StateFlow<TabDestination> = _currentTab.asStateFlow()

    // Expanded full player panel state
    private val _isPlayerExpanded = MutableStateFlow(false)
    val isPlayerExpanded: StateFlow<Boolean> = _isPlayerExpanded.asStateFlow()

    // Currently filtered catalog
    private val _filteredTracks = MutableStateFlow<List<Track>>(emptyList())
    val filteredTracks: StateFlow<List<Track>> = _filteredTracks.asStateFlow()

    // Observing Room Data Access
    val favorites: StateFlow<List<Track>>
    val playlists: StateFlow<List<PlaylistEntity>>
    val localTracks: StateFlow<List<Track>>
    val searchHistory: StateFlow<List<String>>

    // Equalizer state variables
    val equalizerPresets: StateFlow<List<com.example.data.local.EqualizerPresetEntity>>
    
    private val _currentEqualizerSettings = MutableStateFlow(
        com.example.data.local.EqualizerPresetEntity("default_guest_user", "Flat", 0f, 0f, 0f, 0f, 0f, isSystemPreset = true)
    )
    val currentEqualizerSettings: StateFlow<com.example.data.local.EqualizerPresetEntity> = _currentEqualizerSettings.asStateFlow()

    private val _isEqualizerExpanded = MutableStateFlow(false)
    val isEqualizerExpanded: StateFlow<Boolean> = _isEqualizerExpanded.asStateFlow()


    // Spotify Integration States
    val spotifyIsAuthorized: StateFlow<Boolean> = SpotifyManager.isAuthorized
    val spotifyPlaylists: StateFlow<List<SpotifyPlaylist>> = SpotifyManager.playlists
    val spotifyIsLoading: StateFlow<Boolean> = SpotifyManager.isLoading

    private val _spotifySelectedPlaylist = MutableStateFlow<SpotifyPlaylist?>(null)
    val spotifySelectedPlaylist: StateFlow<SpotifyPlaylist?> = _spotifySelectedPlaylist.asStateFlow()

    private val _spotifyPlaylistTracks = MutableStateFlow<List<Track>>(emptyList())
    val spotifyPlaylistTracks: StateFlow<List<Track>> = _spotifyPlaylistTracks.asStateFlow()

    private val _spotifyIsFetchingTracks = MutableStateFlow(false)
    val spotifyIsFetchingTracks: StateFlow<Boolean> = _spotifyIsFetchingTracks.asStateFlow()

    // Media Playback Managers exposed
    val currentTrack: StateFlow<Track?> = MediaPlaybackManager.currentTrack
    val isPlaying: StateFlow<Boolean> = MediaPlaybackManager.isPlaying
    val playbackProgress: StateFlow<Long> = MediaPlaybackManager.playbackProgress
    val duration: StateFlow<Long> = MediaPlaybackManager.duration
    val shuffleMode: StateFlow<Boolean> = MediaPlaybackManager.shuffleMode
    val repeatMode: StateFlow<MediaPlaybackManager.RepeatMode> = MediaPlaybackManager.repeatMode
    val currentQueue: StateFlow<List<Track>> = MediaPlaybackManager.currentQueue

    // Synced Lyrics Flow (Declared after currentTrack to resolve initialization order check)
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val currentLyrics: StateFlow<com.example.data.model.SongLyrics?> = combine(
        currentTrack,
        com.example.data.auth.AuthManager.authState
    ) { track, state ->
        Pair(track, state)
    }.flatMapLatest { (track, state) ->
        val uId = if (state is com.example.data.auth.AuthState.Authenticated) state.userId else "default_guest_user"
        if (track != null) {
            repository.getTrackLyrics(uId, track)
        } else {
            flowOf(null)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    // Active Playlist selection for details panel
    private val _selectedPlaylist = MutableStateFlow<PlaylistEntity?>(null)
    val selectedPlaylist: StateFlow<PlaylistEntity?> = _selectedPlaylist.asStateFlow()

    private val _playlistTracks = MutableStateFlow<List<Track>>(emptyList())
    val playlistTracks: StateFlow<List<Track>> = _playlistTracks.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = MusicRepository(database.musicDao())

        // Initialize Spotify config
        SpotifyManager.init(application)

        // Initialize Media3 ExoPlayer config
        MediaPlaybackManager.init(application)

        favorites = com.example.data.auth.AuthManager.authState
            .flatMapLatest { state ->
                val uId = if (state is com.example.data.auth.AuthState.Authenticated) state.userId else "default_guest_user"
                repository.getFavorites(uId)
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        playlists = com.example.data.auth.AuthManager.authState
            .flatMapLatest { state ->
                val uId = if (state is com.example.data.auth.AuthState.Authenticated) state.userId else "default_guest_user"
                repository.getPlaylists(uId)
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        localTracks = repository.localTracks.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
 
        searchHistory = repository.searchHistory.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        equalizerPresets = com.example.data.auth.AuthManager.authState
            .flatMapLatest { state ->
                val uId = if (state is com.example.data.auth.AuthState.Authenticated) state.userId else "default_guest_user"
                repository.getEqualizerPresets(uId)
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )


        // Unified search stream: automatically merges online catalog and local tracks
        combine(_searchQuery.debounce(200), repository.localTracks) { query, localList ->
            val allTracks = repository.catalog + localList
            if (query.isBlank()) {
                allTracks
            } else {
                allTracks.filter {
                    it.title.contains(query, ignoreCase = true) ||
                    it.artist.contains(query, ignoreCase = true) ||
                    it.album.contains(query, ignoreCase = true) ||
                    it.genre.contains(query, ignoreCase = true)
                }
            }
        }
        .onEach {
            _filteredTracks.value = it
        }
        .launchIn(viewModelScope)
    }

    fun selectTab(tab: TabDestination) {
        _currentTab.value = tab
        // Clear active playlist view when switching tabs
        if (tab != TabDestination.LIBRARY) {
            _selectedPlaylist.value = null
        }
    }

    fun selectPlaylist(playlist: PlaylistEntity?) {
        _selectedPlaylist.value = playlist
        if (playlist != null) {
            viewModelScope.launch {
                repository.getTracksInPlaylist(playlist.id).collect {
                    _playlistTracks.value = it
                }
            }
        } else {
            _playlistTracks.value = emptyList()
        }
    }

    fun setQuery(query: String) {
        _searchQuery.value = query
    }

    fun addSearchQueryToHistory(query: String) {
        if (query.isNotBlank()) {
            viewModelScope.launch {
                repository.addSearchHistory(query)
            }
        }
    }

    fun removeSearchQueryFromHistory(query: String) {
        viewModelScope.launch {
            repository.removeSearchHistory(query)
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            repository.clearSearchHistory()
        }
    }

    fun togglePlayerExpand(expanded: Boolean) {
        _isPlayerExpanded.value = expanded
    }

    // Playback Wrapper methods
    fun playTrack(track: Track, queue: List<Track> = repository.catalog) {
        val activeQueue = if (queue.any { it.id == track.id }) queue else (repository.catalog + localTracks.value)
        MediaPlaybackManager.setQueue(activeQueue, track)
    }

    fun scanLocalStorage() {
        repository.startStorageScan(getApplication())
    }

    fun togglePlayPause() {
        MediaPlaybackManager.togglePlayPause()
    }

    fun playNext() {
        MediaPlaybackManager.playNext()
    }

    fun playPrevious() {
        MediaPlaybackManager.playPrevious()
    }

    fun seekTo(positionMs: Long) {
        MediaPlaybackManager.seekTo(positionMs)
    }

    fun toggleShuffle() {
        MediaPlaybackManager.toggleShuffle()
    }

    fun toggleRepeat() {
        MediaPlaybackManager.toggleRepeatMode()
    }

    private val activeUserId: String
        get() = (com.example.data.auth.AuthManager.authState.value as? com.example.data.auth.AuthState.Authenticated)?.userId ?: "default_guest_user"

    // Favorites Interaction
    fun toggleFavorite(track: Track) {
        viewModelScope.launch {
            val uId = activeUserId
            val isFav = favorites.value.any { it.id == track.id }
            if (isFav) {
                repository.removeFavorite(track.id, uId)
            } else {
                repository.addFavorite(track, uId)
            }
        }
    }

    fun isTrackFavorite(trackId: String): StateFlow<Boolean> {
        return repository.isFavorite(trackId, activeUserId).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )
    }

    // Playlist CRUD Interaction
    fun createPlaylist(name: String, description: String = "") {
        viewModelScope.launch {
            repository.createPlaylist(activeUserId, name, description)
        }
    }

    fun deletePlaylist(playlist: PlaylistEntity) {
        viewModelScope.launch {
            if (_selectedPlaylist.value?.id == playlist.id) {
                _selectedPlaylist.value = null
            }
            repository.deletePlaylist(playlist)
        }
    }

    fun addTrackToPlaylist(playlistId: Int, track: Track) {
        viewModelScope.launch {
            repository.addTrackToPlaylist(playlistId, track)
            // Re-trigger updates to playlist tracking if it is active
            _selectedPlaylist.value?.let { current ->
                if (current.id == playlistId) {
                    selectPlaylist(current)
                }
            }
        }
    }

    fun removeTrackFromPlaylist(playlistId: Int, track: Track) {
        viewModelScope.launch {
            repository.removeTrackFromPlaylist(playlistId, track.id)
            // Re-trigger updates
            _selectedPlaylist.value?.let { current ->
                if (current.id == playlistId) {
                    selectPlaylist(current)
                }
            }
        }
    }

    fun getCatalog(): List<Track> = repository.catalog

    // Spotify OAuth & API Actions
    fun connectSpotifySandbox() {
        SpotifyManager.loginSandbox(getApplication())
    }

    fun disconnectSpotify() {
        SpotifyManager.logout(getApplication())
        selectSpotifyPlaylist(null)
    }

    fun getSpotifyAuthorizeUrl(): String {
        return SpotifyManager.getAuthorizeUrl()
    }

    fun handleSpotifyCallback(uri: Uri) {
        viewModelScope.launch {
            SpotifyManager.handleDeepLink(getApplication(), uri)
        }
    }

    fun selectSpotifyPlaylist(playlist: SpotifyPlaylist?) {
        _spotifySelectedPlaylist.value = playlist
        if (playlist != null) {
            _spotifyPlaylistTracks.value = emptyList()
            _spotifyIsFetchingTracks.value = true
            viewModelScope.launch {
                val tracks = SpotifyManager.fetchPlaylistTracks(getApplication(), playlist.id)
                _spotifyPlaylistTracks.value = tracks
                _spotifyIsFetchingTracks.value = false
            }
        } else {
            _spotifyPlaylistTracks.value = emptyList()
            _spotifyIsFetchingTracks.value = false
        }
    }

    fun refreshSpotify() {
        viewModelScope.launch {
            SpotifyManager.fetchPlaylists(getApplication())
        }
    }

    // Equalizer Screen Interaction Handlers
    fun setEqualizerExpanded(expanded: Boolean) {
        _isEqualizerExpanded.value = expanded
    }

    fun selectEqualizerPreset(preset: com.example.data.local.EqualizerPresetEntity) {
        _currentEqualizerSettings.value = preset
    }

    fun updateEqualizerBand(bandIndex: Int, value: Float) {
        val current = _currentEqualizerSettings.value
        val name = if (current.isSystemPreset) "Custom" else current.name
        val uId = activeUserId
        
        _currentEqualizerSettings.value = when (bandIndex) {
            0 -> current.copy(userId = uId, name = name, isSystemPreset = false, band60Hz = value, timestamp = System.currentTimeMillis())
            1 -> current.copy(userId = uId, name = name, isSystemPreset = false, band230Hz = value, timestamp = System.currentTimeMillis())
            2 -> current.copy(userId = uId, name = name, isSystemPreset = false, band910Hz = value, timestamp = System.currentTimeMillis())
            3 -> current.copy(userId = uId, name = name, isSystemPreset = false, band4kHz = value, timestamp = System.currentTimeMillis())
            4 -> current.copy(userId = uId, name = name, isSystemPreset = false, band14kHz = value, timestamp = System.currentTimeMillis())
            else -> current
        }
    }

    fun saveEqualizerPreset(name: String) {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) return
        
        viewModelScope.launch {
            val uId = activeUserId
            val current = _currentEqualizerSettings.value
            val newPreset = current.copy(
                userId = uId,
                name = trimmedName,
                isSystemPreset = false,
                timestamp = System.currentTimeMillis()
            )
            repository.saveEqualizerPreset(newPreset)
            _currentEqualizerSettings.value = newPreset
        }
    }

    fun deleteEqualizerPreset(name: String) {
        viewModelScope.launch {
            val uId = activeUserId
            repository.deleteEqualizerPreset(uId, name)
            if (_currentEqualizerSettings.value.name.equals(name, ignoreCase = true)) {
                _currentEqualizerSettings.value = com.example.data.local.EqualizerPresetEntity(uId, "Flat", 0f, 0f, 0f, 0f, 0f, isSystemPreset = true)
            }
        }
    }

    // Lyrics State Interaction Handlers
    fun saveTrackLyrics(trackId: String, lyricsText: String) {
        viewModelScope.launch {
            repository.saveTrackLyrics(activeUserId, trackId, lyricsText)
        }
    }

    fun clearCustomTrackLyrics(trackId: String) {
        viewModelScope.launch {
            repository.clearCustomTrackLyrics(activeUserId, trackId)
        }
    }
}
