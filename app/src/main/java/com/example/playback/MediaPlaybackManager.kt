package com.example.playback

import android.content.Context
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object MediaPlaybackManager {
    private const val TAG = "MediaPlaybackManager"

    private var exoPlayer: ExoPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackProgress = MutableStateFlow(0L)
    val playbackProgress: StateFlow<Long> = _playbackProgress.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _shuffleMode = MutableStateFlow(false)
    val shuffleMode: StateFlow<Boolean> = _shuffleMode.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.ALL)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private var originalQueue: List<Track> = emptyList()
    private val _currentQueue = MutableStateFlow<List<Track>>(emptyList())
    val currentQueue: StateFlow<List<Track>> = _currentQueue.asStateFlow()

    private var progressJob: Job? = null
    private var context: Context? = null

    enum class RepeatMode {
        OFF, ONE, ALL
    }

    fun init(context: Context) {
        if (this.context != null) return
        this.context = context.applicationContext
        try {
            setupExoPlayer()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize ExoPlayer synchronously inside init", e)
        }
    }

    private fun setupExoPlayer() {
        val currentContext = context ?: return
        if (exoPlayer != null) return

        try {
            exoPlayer = ExoPlayer.Builder(currentContext).build().apply {
                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlayingChanged: Boolean) {
                        _isPlaying.value = isPlayingChanged
                        if (isPlayingChanged) {
                            startProgressTracker()
                        } else {
                            stopProgressTracker()
                        }
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        when (playbackState) {
                            Player.STATE_READY -> {
                                _duration.value = duration
                            }
                            Player.STATE_ENDED -> {
                                _isPlaying.value = false
                            }
                        }
                    }

                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        val currentItemIndex = currentMediaItemIndex
                        val queue = _currentQueue.value
                        if (currentItemIndex in queue.indices) {
                            val track = queue[currentItemIndex]
                            _currentTrack.value = track
                            _duration.value = track.durationMs
                            _playbackProgress.value = 0L
                        }
                    }

                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        Log.e(TAG, "ExoPlayer error: ${error.message}", error)
                        _isPlaying.value = false
                        stopProgressTracker()
                    }
                })

                // Sync initial state
                repeatMode = getMedia3RepeatMode(_repeatMode.value)
                shuffleModeEnabled = _shuffleMode.value
            }
            startProgressTracker()
        } catch (e: Exception) {
            Log.e(TAG, "Error in setupExoPlayer: ${e.message}", e)
        }
    }

    fun setQueue(tracks: List<Track>, startWithTrack: Track? = null) {
        originalQueue = tracks
        if (_shuffleMode.value) {
            val shuffled = tracks.shuffled().toMutableList()
            if (startWithTrack != null) {
                shuffled.remove(startWithTrack)
                shuffled.add(0, startWithTrack)
            }
            _currentQueue.value = shuffled
        } else {
            _currentQueue.value = tracks
        }

        if (exoPlayer == null && context != null) {
            setupExoPlayer()
        }

        exoPlayer?.let { player ->
            player.clearMediaItems()
            val mediaItems = _currentQueue.value.map { track ->
                MediaItem.fromUri(track.audioUrl)
            }
            player.addMediaItems(mediaItems)

            if (startWithTrack != null) {
                val index = _currentQueue.value.indexOfFirst { it.id == startWithTrack.id }
                if (index != -1) {
                    player.seekTo(index, 0L)
                    _currentTrack.value = startWithTrack
                    _duration.value = startWithTrack.durationMs
                    _playbackProgress.value = 0L
                    player.prepare()
                    player.play()
                } else {
                    playTrack(startWithTrack)
                }
            } else if (_currentQueue.value.isNotEmpty()) {
                val firstTrack = _currentQueue.value.first()
                _currentTrack.value = firstTrack
                _duration.value = firstTrack.durationMs
                _playbackProgress.value = 0L
                player.prepare()
            }
        }
    }

    fun playTrack(track: Track) {
        try {
            _currentTrack.value = track
            _playbackProgress.value = 0L
            _duration.value = track.durationMs

            if (exoPlayer == null && context != null) {
                setupExoPlayer()
            }

            exoPlayer?.let { player ->
                val index = _currentQueue.value.indexOfFirst { it.id == track.id }
                if (index != -1 && player.mediaItemCount == _currentQueue.value.size) {
                    player.seekTo(index, 0L)
                    player.prepare()
                    player.play()
                } else {
                    _currentQueue.value = listOf(track)
                    originalQueue = listOf(track)
                    player.clearMediaItems()
                    player.setMediaItem(MediaItem.fromUri(track.audioUrl))
                    player.prepare()
                    player.play()
                }
                _isPlaying.value = true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing track: ${e.message}", e)
            _isPlaying.value = false
        }
    }

    fun togglePlayPause() {
        if (exoPlayer == null && context != null) {
            setupExoPlayer()
        }
        exoPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
            } else {
                if (player.playbackState == Player.STATE_IDLE) {
                    player.prepare()
                }
                player.play()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.let { player ->
            try {
                player.seekTo(positionMs)
                _playbackProgress.value = positionMs
            } catch (e: Exception) {
                Log.e(TAG, "Error seeking: ${e.message}")
            }
        }
    }

    fun playNext() {
        exoPlayer?.let { player ->
            if (player.hasNextMediaItem()) {
                player.seekToNextMediaItem()
            } else {
                if (_repeatMode.value == RepeatMode.ALL && _currentQueue.value.isNotEmpty()) {
                    player.seekTo(0, 0L)
                } else {
                    player.pause()
                    _isPlaying.value = false
                }
            }
        }
    }

    fun playPrevious() {
        exoPlayer?.let { player ->
            if (player.currentPosition > 5000) {
                player.seekTo(0L)
                _playbackProgress.value = 0L
                return
            }
            if (player.hasPreviousMediaItem()) {
                player.seekToPreviousMediaItem()
            } else {
                if (_repeatMode.value == RepeatMode.ALL && _currentQueue.value.isNotEmpty()) {
                    player.seekTo(_currentQueue.value.size - 1, 0L)
                } else {
                    player.seekTo(0L)
                    _playbackProgress.value = 0L
                }
            }
        }
    }

    fun toggleShuffle() {
        val newShuffle = !_shuffleMode.value
        _shuffleMode.value = newShuffle

        exoPlayer?.let { player ->
            player.shuffleModeEnabled = newShuffle

            val current = _currentTrack.value
            if (newShuffle) {
                val shuffled = originalQueue.shuffled().toMutableList()
                if (current != null) {
                    shuffled.remove(current)
                    shuffled.add(0, current)
                }
                _currentQueue.value = shuffled
            } else {
                _currentQueue.value = originalQueue
            }

            val currentIndex = _currentQueue.value.indexOfFirst { it.id == current?.id }
            player.clearMediaItems()
            val mediaItems = _currentQueue.value.map { MediaItem.fromUri(it.audioUrl) }
            player.addMediaItems(mediaItems)
            if (currentIndex != -1) {
                player.seekTo(currentIndex, player.currentPosition)
            }
        }
    }

    fun toggleRepeatMode() {
        val currentMode = _repeatMode.value
        val newMode = when (currentMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        _repeatMode.value = newMode
        exoPlayer?.let { player ->
            player.repeatMode = getMedia3RepeatMode(newMode)
        }
    }

    private fun getMedia3RepeatMode(repeatMode: RepeatMode): Int {
        return when (repeatMode) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (true) {
                exoPlayer?.let { player ->
                    try {
                        if (player.isPlaying) {
                            _playbackProgress.value = player.currentPosition
                        }
                    } catch (e: Exception) {
                        // Suppress logs
                    }
                }
                delay(500)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun releasePlayer() {
        exoPlayer?.let { player ->
            try {
                if (player.isPlaying) {
                    player.stop()
                }
            } catch (e: Exception) {}
            player.release()
        }
        exoPlayer = null
    }
}
