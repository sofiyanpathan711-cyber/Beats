package com.example.data.repository

import com.example.data.local.FavoriteEntity
import com.example.data.local.MusicDao
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistTrackEntity
import com.example.data.model.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MusicRepository(private val musicDao: MusicDao) {

    // 100% Free and Ad-Free Streaming Catalog
    val catalog = listOf(
        Track(
            id = "track_1",
            title = "Midnight Lounge",
            artist = "Aether Flow",
            album = "Cosmic Coffee Shop",
            durationMs = 372000,
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
            coverUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=500&auto=format&fit=crop&q=80",
            genre = "Chillhop"
        ),
        Track(
            id = "track_2",
            title = "Warm Horizon",
            artist = "Solaris Kid",
            album = "Golden Hour Session",
            durationMs = 423000,
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
            coverUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=500&auto=format&fit=crop&q=80",
            genre = "Lo-Fi"
        ),
        Track(
            id = "track_3",
            title = "Neon Highway",
            artist = "Cyber Runner",
            album = "Retro Overdrive",
            durationMs = 302000,
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
            coverUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500&auto=format&fit=crop&q=80",
            genre = "Synthwave"
        ),
        Track(
            id = "track_4",
            title = "Ocean Acoustic",
            artist = "Coral Breeze",
            album = "Sand & Strings",
            durationMs = 310000,
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3",
            coverUrl = "https://images.unsplash.com/photo-1510915361894-db8b60106cb1?w=500&auto=format&fit=crop&q=80",
            genre = "Acoustic"
        ),
        Track(
            id = "track_5",
            title = "Deep Focus Ambient",
            artist = "Brainwave Zen",
            album = "Mind Palace",
            durationMs = 288000,
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-10.mp3",
            coverUrl = "https://images.unsplash.com/photo-1498038432885-c6f3f1b912ee?w=500&auto=format&fit=crop&q=80",
            genre = "Ambient"
        ),
        Track(
            id = "track_6",
            title = "Velvet Club Jazz",
            artist = "The Blue Notes Trio",
            album = "Late Night Sessions",
            durationMs = 354000,
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-12.mp3",
            coverUrl = "https://images.unsplash.com/photo-1506157786151-b8491531f063?w=500&auto=format&fit=crop&q=80",
            genre = "Jazz"
        )
    )

    private val localTracksCache = java.util.concurrent.ConcurrentHashMap<String, Track>()

    val localTracks: Flow<List<Track>> = musicDao.getLocalTracks().map { entities ->
        entities.map { entity ->
            val track = Track(
                id = entity.id,
                title = entity.title,
                artist = entity.artist,
                album = entity.album,
                durationMs = entity.durationMs,
                audioUrl = entity.audioUrl,
                coverUrl = entity.coverUrl.ifEmpty { "https://images.unsplash.com/photo-1510915361894-db8b60106cb1?w=500&auto=format&fit=crop&q=80" },
                genre = entity.genre
            )
            localTracksCache[track.id] = track
            track
        }
    }

    fun startStorageScan(context: android.content.Context) {
        com.example.service.MediaScanService.start(context)
    }

    fun getTrackById(id: String): Track? {
        return catalog.find { it.id == id } ?: localTracksCache[id]
    }

    // Favorites
    fun getFavorites(userId: String): Flow<List<Track>> = musicDao.getFavorites(userId).map { entities ->
        resolveTracks(entities.map { it.trackId })
    }

    private suspend fun resolveTracks(trackIds: List<String>): List<Track> {
        val resolved = mutableListOf<Track>()
        val missingIds = mutableListOf<String>()

        for (id in trackIds) {
            val track = getTrackById(id)
            if (track != null) {
                resolved.add(track)
            } else {
                missingIds.add(id)
            }
        }

        if (missingIds.isNotEmpty()) {
            val localEntities = musicDao.getLocalTracksByIds(missingIds)
            val metadataEntities = musicDao.getTrackMetadataByIds(missingIds)

            for (id in missingIds) {
                val local = localEntities.find { it.id == id }
                if (local != null) {
                    resolved.add(
                        Track(
                            id = local.id,
                            title = local.title,
                            artist = local.artist,
                            album = local.album,
                            durationMs = local.durationMs,
                            audioUrl = local.audioUrl,
                            coverUrl = local.coverUrl.ifEmpty { "https://images.unsplash.com/photo-1510915361894-db8b60106cb1?w=500&auto=format&fit=crop&q=80" },
                            genre = local.genre
                        )
                    )
                    continue
                }

                val meta = metadataEntities.find { it.id == id }
                if (meta != null) {
                    resolved.add(
                        Track(
                            id = meta.id,
                            title = meta.title,
                            artist = meta.artist,
                            album = meta.album,
                            durationMs = meta.durationMs,
                            audioUrl = meta.audioUrl,
                            coverUrl = meta.coverUrl,
                            genre = meta.genre
                        )
                    )
                }
            }
        }

        return resolved
    }

    suspend fun addFavorite(track: Track, userId: String) {
        musicDao.insertTrackMetadata(
            com.example.data.local.TrackMetadataEntity(
                id = track.id,
                title = track.title,
                artist = track.artist,
                album = track.album,
                durationMs = track.durationMs,
                audioUrl = track.audioUrl,
                coverUrl = track.coverUrl,
                genre = track.genre
            )
        )
        musicDao.insertFavorite(FavoriteEntity(userId = userId, trackId = track.id))
    }

    suspend fun removeFavorite(trackId: String, userId: String) {
        musicDao.deleteFavorite(userId, trackId)
    }

    fun isFavorite(trackId: String, userId: String): Flow<Boolean> {
        return musicDao.isFavorite(userId, trackId)
    }

    // Playlists
    fun getPlaylists(userId: String): Flow<List<PlaylistEntity>> = musicDao.getPlaylists(userId)

    suspend fun createPlaylist(userId: String, name: String, description: String = ""): Long {
        return musicDao.insertPlaylist(PlaylistEntity(userId = userId, name = name, description = description))
    }

    suspend fun deletePlaylist(playlist: PlaylistEntity) {
        musicDao.deletePlaylist(playlist)
    }

    // Playlist tracks
    fun getTracksInPlaylist(playlistId: Int): Flow<List<Track>> {
        return musicDao.getTracksInPlaylist(playlistId).map { entities ->
            resolveTracks(entities.map { it.trackId })
        }
    }

    suspend fun addTrackToPlaylist(playlistId: Int, track: Track) {
        musicDao.insertTrackMetadata(
            com.example.data.local.TrackMetadataEntity(
                id = track.id,
                title = track.title,
                artist = track.artist,
                album = track.album,
                durationMs = track.durationMs,
                audioUrl = track.audioUrl,
                coverUrl = track.coverUrl,
                genre = track.genre
            )
        )
        musicDao.insertPlaylistTrack(PlaylistTrackEntity(playlistId, track.id))
    }

    suspend fun removeTrackFromPlaylist(playlistId: Int, trackId: String) {
        musicDao.removeTrackFromPlaylist(playlistId, trackId)
    }

    // Search History Repository APIs
    val searchHistory: Flow<List<String>> = musicDao.getSearchHistory().map { list ->
        list.map { it.queryText }
    }

    suspend fun addSearchHistory(query: String) {
        if (query.isBlank()) return
        musicDao.insertSearchHistory(com.example.data.local.SearchHistoryEntity(queryText = query.trim(), timestamp = System.currentTimeMillis()))
    }

    suspend fun removeSearchHistory(query: String) {
        musicDao.deleteSearchHistory(query)
    }

    suspend fun clearSearchHistory() {
        musicDao.clearSearchHistory()
    }

    // Equalizer Repository APIs
    fun getEqualizerPresets(userId: String): Flow<List<com.example.data.local.EqualizerPresetEntity>> = musicDao.getAllEqualizerPresets(userId).map { customPresets ->
        val systemPresets = listOf(
            com.example.data.local.EqualizerPresetEntity(userId, "Flat", 0f, 0f, 0f, 0f, 0f, isSystemPreset = true),
            com.example.data.local.EqualizerPresetEntity(userId, "Bass Booster", 8f, 5f, 0f, -1f, -3f, isSystemPreset = true),
            com.example.data.local.EqualizerPresetEntity(userId, "Vocal Booster", -3f, -1f, 4f, 6f, 3f, isSystemPreset = true),
            com.example.data.local.EqualizerPresetEntity(userId, "Electronic", 6f, 3f, -1f, 4f, 5f, isSystemPreset = true),
            com.example.data.local.EqualizerPresetEntity(userId, "Pop", 2f, 4f, 3f, 1f, -2f, isSystemPreset = true),
            com.example.data.local.EqualizerPresetEntity(userId, "Classic", 4f, 2f, 0f, -3f, -4f, isSystemPreset = true)
        )
        val customFiltered = customPresets.filter { custom -> 
            systemPresets.none { it.name.equals(custom.name, ignoreCase = true) } 
        }
        systemPresets + customFiltered
    }

    suspend fun saveEqualizerPreset(preset: com.example.data.local.EqualizerPresetEntity) {
        musicDao.insertEqualizerPreset(preset)
    }

    suspend fun deleteEqualizerPreset(userId: String, name: String) {
        musicDao.deleteEqualizerPreset(userId, name)
    }

    // Sync Lyrics Repository APIs
    fun getTrackLyrics(userId: String, track: com.example.data.model.Track): Flow<com.example.data.model.SongLyrics> {
        return musicDao.getTrackLyrics(userId, track.id).map { entity ->
            if (entity != null) {
                com.example.data.model.LyricsParser.parse(track.id, entity.lyricsText)
            } else {
                com.example.data.provider.LyricsProvider.getLyricsForTrack(track)
            }
        }
    }

    suspend fun saveTrackLyrics(userId: String, trackId: String, lyricsText: String) {
        musicDao.insertTrackLyrics(
            com.example.data.local.TrackLyricsEntity(
                userId = userId,
                trackId = trackId,
                lyricsText = lyricsText,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun clearCustomTrackLyrics(userId: String, trackId: String) {
        musicDao.deleteTrackLyrics(userId, trackId)
    }
}
