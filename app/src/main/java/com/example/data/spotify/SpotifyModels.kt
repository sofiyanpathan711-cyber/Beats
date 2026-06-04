package com.example.data.spotify

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SpotifyTokenResponse(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "token_type") val tokenType: String,
    @Json(name = "expires_in") val expiresIn: Int,
    @Json(name = "refresh_token") val refreshToken: String?,
    @Json(name = "scope") val scope: String?
)

@JsonClass(generateAdapter = true)
data class SpotifyPlaylistsResponse(
    @Json(name = "items") val items: List<SpotifyPlaylist>,
    @Json(name = "total") val total: Int
)

@JsonClass(generateAdapter = true)
data class SpotifyPlaylist(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "description") val description: String?,
    @Json(name = "images") val images: List<SpotifyImage>?,
    @Json(name = "tracks") val tracks: SpotifyPlaylistTrackInfo,
    @Json(name = "owner") val owner: SpotifyUser? = null
)

@JsonClass(generateAdapter = true)
data class SpotifyUser(
    @Json(name = "display_name") val displayName: String?,
    @Json(name = "id") val id: String
)

@JsonClass(generateAdapter = true)
data class SpotifyImage(
    @Json(name = "url") val url: String?,
    @Json(name = "height") val height: Int?,
    @Json(name = "width") val width: Int?
)

@JsonClass(generateAdapter = true)
data class SpotifyPlaylistTrackInfo(
    @Json(name = "total") val total: Int
)

@JsonClass(generateAdapter = true)
data class SpotifyPlaylistTracksResponse(
    @Json(name = "items") val items: List<SpotifyPlaylistItem>
)

@JsonClass(generateAdapter = true)
data class SpotifyPlaylistItem(
    @Json(name = "track") val track: SpotifyTrack?
)

@JsonClass(generateAdapter = true)
data class SpotifyTrack(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "duration_ms") val durationMs: Long,
    @Json(name = "preview_url") val previewUrl: String?,
    @Json(name = "album") val album: SpotifyAlbum,
    @Json(name = "artists") val artists: List<SpotifyArtist>
)

@JsonClass(generateAdapter = true)
data class SpotifyAlbum(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "images") val images: List<SpotifyImage>?
)

@JsonClass(generateAdapter = true)
data class SpotifyArtist(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String
)
