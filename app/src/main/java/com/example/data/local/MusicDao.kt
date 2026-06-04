package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MusicDao {
    // Favorites
    @Query("SELECT * FROM favorites WHERE userId = :userId ORDER BY addedAt DESC")
    fun getFavorites(userId: String): Flow<List<FavoriteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(fav: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE userId = :userId AND trackId = :trackId")
    suspend fun deleteFavorite(userId: String, trackId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE userId = :userId AND trackId = :trackId)")
    fun isFavorite(userId: String, trackId: String): Flow<Boolean>

    // Playlists
    @Query("SELECT * FROM playlists WHERE userId = :userId ORDER BY createdAt DESC")
    fun getPlaylists(userId: String): Flow<List<PlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Delete
    suspend fun deletePlaylist(playlist: PlaylistEntity)

    // Playlist Tracks
    @Query("SELECT * FROM playlist_tracks WHERE playlistId = :playlistId")
    fun getTracksInPlaylist(playlistId: Int): Flow<List<PlaylistTrackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistTrack(playlistTrack: PlaylistTrackEntity)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun removeTrackFromPlaylist(playlistId: Int, trackId: String)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun clearTracksInPlaylist(playlistId: Int)

    // Local Tracks
    @Query("SELECT * FROM local_tracks ORDER BY dateModified DESC")
    fun getLocalTracks(): Flow<List<LocalTrackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocalTracks(tracks: List<LocalTrackEntity>)

    @Query("SELECT * FROM local_tracks WHERE id = :id LIMIT 1")
    suspend fun getLocalTrackById(id: String): LocalTrackEntity?

    @Query("SELECT * FROM local_tracks WHERE id IN (:ids)")
    suspend fun getLocalTracksByIds(ids: List<String>): List<LocalTrackEntity>

    @Query("DELETE FROM local_tracks")
    suspend fun clearAllLocalTracks()

    // Track Metadata Persistence
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrackMetadata(track: TrackMetadataEntity)

    @Query("SELECT * FROM track_metadata WHERE id = :id LIMIT 1")
    suspend fun getTrackMetadataById(id: String): TrackMetadataEntity?

    @Query("SELECT * FROM track_metadata WHERE id IN (:ids)")
    suspend fun getTrackMetadataByIds(ids: List<String>): List<TrackMetadataEntity>

    // Search History Persistent Queries
    @Query("SELECT * FROM search_history ORDER BY timestamp DESC LIMIT 15")
    fun getSearchHistory(): Flow<List<SearchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearchHistory(entry: SearchHistoryEntity)

    @Query("DELETE FROM search_history WHERE queryText = :queryText")
    suspend fun deleteSearchHistory(queryText: String)

    @Query("DELETE FROM search_history")
    suspend fun clearSearchHistory()

    // Equalizer Presets Queries
    @Query("SELECT * FROM equalizer_presets WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAllEqualizerPresets(userId: String): Flow<List<EqualizerPresetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEqualizerPreset(preset: EqualizerPresetEntity)

    @Query("DELETE FROM equalizer_presets WHERE userId = :userId AND name = :name")
    suspend fun deleteEqualizerPreset(userId: String, name: String)

    @Query("SELECT * FROM equalizer_presets WHERE userId = :userId AND name = :name LIMIT 1")
    suspend fun getEqualizerPresetByName(userId: String, name: String): EqualizerPresetEntity?

    // Track Lyrics Queries
    @Query("SELECT * FROM track_lyrics WHERE userId = :userId AND trackId = :trackId LIMIT 1")
    fun getTrackLyrics(userId: String, trackId: String): Flow<TrackLyricsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrackLyrics(lyrics: TrackLyricsEntity)

    @Query("DELETE FROM track_lyrics WHERE userId = :userId AND trackId = :trackId")
    suspend fun deleteTrackLyrics(userId: String, trackId: String)
}
