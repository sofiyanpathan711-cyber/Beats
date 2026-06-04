package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites", primaryKeys = ["userId", "trackId"])
data class FavoriteEntity(
    val userId: String,
    val trackId: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String,
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_tracks",
    primaryKeys = ["playlistId", "trackId"]
)
data class PlaylistTrackEntity(
    val playlistId: Int,
    val trackId: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "local_tracks")
data class LocalTrackEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val audioUrl: String,
    val coverUrl: String = "",
    val genre: String = "Local File",
    val dateModified: Long = System.currentTimeMillis()
)

@Entity(tableName = "track_metadata")
data class TrackMetadataEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val audioUrl: String,
    val coverUrl: String = "",
    val genre: String = ""
)

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey val queryText: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "equalizer_presets", primaryKeys = ["userId", "name"])
data class EqualizerPresetEntity(
    val userId: String,
    val name: String,
    val band60Hz: Float,    // dB value, e.g. -15.0 to +15.0
    val band230Hz: Float,
    val band910Hz: Float,
    val band4kHz: Float,
    val band14kHz: Float,
    val isSystemPreset: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "track_lyrics", primaryKeys = ["userId", "trackId"])
data class TrackLyricsEntity(
    val userId: String,
    val trackId: String,
    val lyricsText: String, // Timed LRC raw text string
    val timestamp: Long = System.currentTimeMillis()
)

