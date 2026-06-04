package com.example.data.spotify

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.Track
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID

object SpotifyManager {
    private const val TAG = "SpotifyManager"
    private const val PREFS_NAME = "spotify_prefs"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_REFRESH_TOKEN = "refresh_token"
    private const val KEY_EXPIRES_AT = "expires_at"
    private const val KEY_VERIFIER = "code_verifier"
    private const val KEY_STATE = "auth_state"
    private const val KEY_IS_SANDBOX = "is_sandbox"

    const val REDIRECT_URI = "beats://spotify-callback"

    private val client = OkHttpClient()
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    private val _isAuthorized = MutableStateFlow(false)
    val isAuthorized: StateFlow<Boolean> = _isAuthorized.asStateFlow()

    private val _playlists = MutableStateFlow<List<SpotifyPlaylist>>(emptyList())
    val playlists: StateFlow<List<SpotifyPlaylist>> = _playlists.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isSandbox = prefs?.getBoolean(KEY_IS_SANDBOX, false) ?: false
        val accessToken = prefs?.getString(KEY_ACCESS_TOKEN, null)
        val expiresAt = prefs?.getLong(KEY_EXPIRES_AT, 0L) ?: 0L

        Log.d(TAG, "Initializing: isSandbox=$isSandbox, hasToken=${!accessToken.isNullOrEmpty()}")

        if (isSandbox) {
            _isAuthorized.value = true
            loadSandboxPlaylists()
        } else if (!accessToken.isNullOrEmpty()) {
            if (System.currentTimeMillis() < expiresAt) {
                _isAuthorized.value = true
                triggerPlaylistsLoad(context)
            } else {
                // Token expired, attempt refresh in background
                _isAuthorized.value = true
                triggerPlaylistsLoad(context)
            }
        } else {
            _isAuthorized.value = false
        }
    }

    fun isSandboxMode(): Boolean {
        return prefs?.getBoolean(KEY_IS_SANDBOX, false) ?: false
    }

    fun getClientId(): String {
        val id = BuildConfig.SPOTIFY_CLIENT_ID
        return if (id == "spotify_placeholder_client_id" || id.trim().isBlank()) "" else id.trim()
    }

    fun getAuthorizeUrl(): String {
        val clientId = getClientId()
        if (clientId.isEmpty()) return ""

        val verifier = generateCodeVerifier()
        val challenge = generateCodeChallenge(verifier)
        val state = UUID.randomUUID().toString()

        prefs?.edit()?.apply {
            putString(KEY_VERIFIER, verifier)
            putString(KEY_STATE, state)
            apply()
        }

        return Uri.parse("https://accounts.spotify.com/authorize")
            .buildUpon()
            .appendQueryParameter("client_id", clientId)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("redirect_uri", REDIRECT_URI)
            .appendQueryParameter("scope", "playlist-read-private playlist-read-collaborative user-library-read")
            .appendQueryParameter("state", state)
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("code_challenge", challenge)
            .build()
            .toString()
    }

    suspend fun handleDeepLink(context: Context, uri: Uri): Boolean {
        init(context)
        Log.d(TAG, "Handling Deep Link callback: $uri")
        val error = uri.getQueryParameter("error")
        if (error != null) {
            Log.e(TAG, "Authorization error returned from Spotify: $error")
            return false
        }

        val code = uri.getQueryParameter("code") ?: return false
        val returnedState = uri.getQueryParameter("state")
        val savedState = prefs?.getString(KEY_STATE, null)

        if (returnedState == null || returnedState != savedState) {
            Log.e(TAG, "CSRF State validation failed. returnedState=$returnedState, savedState=$savedState")
            return false
        }

        val verifier = prefs?.getString(KEY_VERIFIER, null) ?: return false
        val clientId = getClientId()

        _isLoading.value = true
        val success = exchangeToken(clientId, code, verifier)
        _isLoading.value = false

        if (success) {
            prefs?.edit()?.putBoolean(KEY_IS_SANDBOX, false)?.apply()
            _isAuthorized.value = true
            fetchPlaylists(context)
        }
        return success
    }

    private suspend fun exchangeToken(clientId: String, code: String, verifier: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val body = FormBody.Builder()
                .add("client_id", clientId)
                .add("grant_type", "authorization_code")
                .add("code", code)
                .add("redirect_uri", REDIRECT_URI)
                .add("code_verifier", verifier)
                .build()

            val request = Request.Builder()
                .url("https://accounts.spotify.com/api/token")
                .post(body)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val tokenResponse = moshi.adapter(SpotifyTokenResponse::class.java).fromJson(responseBody)
                if (tokenResponse != null) {
                    saveTokens(tokenResponse)
                    return@withContext true
                }
            } else {
                Log.e(TAG, "Token exchange failed: Code=${response.code}, Body=$responseBody")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during token exchange: ${e.message}", e)
        }
        return@withContext false
    }

    private fun saveTokens(token: SpotifyTokenResponse) {
        val expiresAt = System.currentTimeMillis() + (token.expiresIn * 1000L)
        prefs?.edit()?.apply {
            putString(KEY_ACCESS_TOKEN, token.accessToken)
            token.refreshToken?.let { putString(KEY_REFRESH_TOKEN, it) }
            putLong(KEY_EXPIRES_AT, expiresAt)
            apply()
        }
    }

    suspend fun refreshTokenIfNeeded(context: Context): Boolean = withContext(Dispatchers.IO) {
        init(context)
        if (isSandboxMode()) return@withContext true

        val expiresAt = prefs?.getLong(KEY_EXPIRES_AT, 0L) ?: 0L
        // Refresh token if within 5 minutes of expiration
        if (System.currentTimeMillis() + 300000 > expiresAt) {
            val refreshToken = prefs?.getString(KEY_REFRESH_TOKEN, null) ?: return@withContext false
            val clientId = getClientId()

            Log.d(TAG, "Token expired or near expiration. Requesting refresh...")
            try {
                val body = FormBody.Builder()
                    .add("client_id", clientId)
                    .add("grant_type", "refresh_token")
                    .add("refresh_token", refreshToken)
                    .build()

                val request = Request.Builder()
                    .url("https://accounts.spotify.com/api/token")
                    .post(body)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                if (response.isSuccessful) {
                    val tokenResponse = moshi.adapter(SpotifyTokenResponse::class.java).fromJson(responseBody)
                    if (tokenResponse != null) {
                        saveTokens(tokenResponse)
                        Log.d(TAG, "Spotify access token refreshed successfully!")
                        return@withContext true
                    }
                } else {
                    Log.e(TAG, "Refresh failed: Code=${response.code}, Body=$responseBody")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during token refresh: ${e.message}", e)
            }
            return@withContext false
        }
        return@withContext true
    }

    private fun triggerPlaylistsLoad(context: Context) {
        val scope = kotlinx.coroutines.CoroutineScope(Dispatchers.IO)
        scope.launch {
            try {
                fetchPlaylists(context)
            } catch (e: Exception) {
                Log.e(TAG, "Error triggers loading: ${e.message}")
            }
        }
    }

    suspend fun fetchPlaylists(context: Context) = withContext(Dispatchers.IO) {
        init(context)
        if (isSandboxMode()) {
            loadSandboxPlaylists()
            return@withContext
        }

        if (!refreshTokenIfNeeded(context)) {
            Log.e(TAG, "Cannot fetch playlists: token refresh failed or unauthorized")
            return@withContext
        }

        val token = prefs?.getString(KEY_ACCESS_TOKEN, null) ?: return@withContext
        _isLoading.value = true

        try {
            val request = Request.Builder()
                .url("https://api.spotify.com/v1/me/playlists?limit=25")
                .header("Authorization", "Bearer $token")
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val playlistsResponse = moshi.adapter(SpotifyPlaylistsResponse::class.java).fromJson(responseBody)
                _playlists.value = playlistsResponse?.items ?: emptyList()
                Log.d(TAG, "Fetched ${_playlists.value.size} real Spotify playlists.")
            } else {
                Log.e(TAG, "Fetch playlists failed: Code=${response.code}, Body=$responseBody")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception fetching playlists: ${e.message}", e)
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun fetchPlaylistTracks(context: Context, playlistId: String): List<Track> = withContext(Dispatchers.IO) {
        init(context)
        if (isSandboxMode()) {
            return@withContext getSandboxTracks(playlistId)
        }

        if (!refreshTokenIfNeeded(context)) {
            return@withContext emptyList()
        }

        val token = prefs?.getString(KEY_ACCESS_TOKEN, null) ?: return@withContext listEmpty()
        Log.d(TAG, "Fetching tracks for Spotify playlist $playlistId...")

        try {
            val request = Request.Builder()
                .url("https://api.spotify.com/v1/playlists/$playlistId/tracks?limit=50")
                .header("Authorization", "Bearer $token")
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val tracksResponse = moshi.adapter(SpotifyPlaylistTracksResponse::class.java).fromJson(responseBody)
                val mapped = tracksResponse?.items?.mapNotNull { item ->
                    val t = item.track ?: return@mapNotNull null
                    val cover = t.album.images?.firstOrNull()?.url ?: "https://images.unsplash.com/photo-1614680376593-902f74fa0d41?w=500"
                    
                    // Fallback preview URL if null so the media player can stream something royalty free
                    val playUrl = if (!t.previewUrl.isNullOrEmpty()) {
                        t.previewUrl
                    } else {
                        // Return sound helix mp3 URL as high quality fallback
                        val index = Math.abs(t.id.hashCode() % 10) + 1
                        "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-$index.mp3"
                    }

                    Track(
                        id = "spotify_${t.id}",
                        title = t.name,
                        artist = t.artists.joinToString(", ") { it.name },
                        album = t.album.name,
                        durationMs = t.durationMs,
                        audioUrl = playUrl,
                        coverUrl = cover,
                        genre = "Spotify"
                    )
                } ?: emptyList()
                
                Log.d(TAG, "Parsed ${mapped.size} Spotify tracks for playlist $playlistId.")
                return@withContext mapped
            } else {
                Log.e(TAG, "Fetch playlist tracks failed: Code=${response.code}, Body=$responseBody")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception fetching playlist tracks: ${e.message}", e)
        }
        return@withContext emptyList()
    }

    private fun listEmpty(): List<Track> = emptyList()

    fun logout(context: Context) {
        init(context)
        prefs?.edit()?.clear()?.apply()
        _isAuthorized.value = false
        _playlists.value = emptyList()
        Log.d(TAG, "Spotify session cleared.")
    }

    fun loginSandbox(context: Context) {
        init(context)
        prefs?.edit()?.apply {
            putBoolean(KEY_IS_SANDBOX, true)
            putString(KEY_ACCESS_TOKEN, "sandbox_token")
            putLong(KEY_EXPIRES_AT, System.currentTimeMillis() + 86400000L) // 1 day
            apply()
        }
        _isAuthorized.value = true
        loadSandboxPlaylists()
    }

    private fun loadSandboxPlaylists() {
        _playlists.value = listOf(
            SpotifyPlaylist(
                id = "sandbox_cyber",
                name = "Neo-Tokyo Cyberpunk",
                description = "Futuristic synthwave frequencies & basslines to drive through Neo Tokyo.",
                images = listOf(SpotifyImage("https://images.unsplash.com/photo-1578894381163-e72c17f2d45f?w=500", 500, 500)),
                tracks = SpotifyPlaylistTrackInfo(4),
                owner = SpotifyUser("Beats AI", "beats_ai")
            ),
            SpotifyPlaylist(
                id = "sandbox_lofi",
                name = "Midnight Coffee Lo-Fi",
                description = "Cosmically tuned relaxing lo-fi beats for late-night music sessions.",
                images = listOf(SpotifyImage("https://images.unsplash.com/photo-1515462277126-270d878326e5?w=500", 500, 500)),
                tracks = SpotifyPlaylistTrackInfo(4),
                owner = SpotifyUser("Beats AI", "beats_ai")
            ),
            SpotifyPlaylist(
                id = "sandbox_hyper",
                name = "Arcade Hyperdrive",
                description = "Fast-paced future bass & electronic grooves to boost your levels.",
                images = listOf(SpotifyImage("https://images.unsplash.com/photo-1511512578047-dfb367046420?w=500", 500, 500)),
                tracks = SpotifyPlaylistTrackInfo(3),
                owner = SpotifyUser("Beats AI", "beats_ai")
            )
        )
    }

    private fun getSandboxTracks(playlistId: String): List<Track> {
        return when (playlistId) {
            "sandbox_cyber" -> listOf(
                Track("spotify_sb_1_1", "Neon Overdrive", "Cyber Corp", "Digital Core", 195000, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3", "https://images.unsplash.com/photo-1542751371-adc38448a05e?w=500", "Cyberpunk"),
                Track("spotify_sb_1_2", "Laser Grid Shift", "Grid Runner", "Simulation One", 232000, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3", "https://images.unsplash.com/photo-1563089145-599997674d42?w=500", "Synthwave"),
                Track("spotify_sb_1_3", "Digital Horizon", "Matrix Kid", "Cyber City", 178000, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3", "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=500", "Electro"),
                Track("spotify_sb_1_4", "Retro Speedrun", "Level Up", "Arcade Retro", 215000, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3", "https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=500", "Chiptune")
            )
            "sandbox_lofi" -> listOf(
                Track("spotify_sb_2_1", "Rainy Window Hop", "Lofi Cafe", "Espresso Sessions", 162000, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3", "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=500", "Chillhop"),
                Track("spotify_sb_2_2", "Warm Vinyl Dust", "Vintage Static", "Analog Waves", 185000, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3", "https://images.unsplash.com/photo-1484755560693-a4074577af3a?w=500", "Lo-Fi"),
                Track("spotify_sb_2_3", "Cozy Fireside Sleep", "Dreamy Sleep", "Slumber Tapes", 140000, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-7.mp3", "https://images.unsplash.com/photo-1544816155-12df9643f363?w=500", "Ambient"),
                Track("spotify_sb_2_4", "Stardust Coffee", "Spaceman", "Nebula Coffee", 204000, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3", "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=500", "Space Beats")
            )
            "sandbox_hyper" -> listOf(
                Track("spotify_sb_3_1", "Pixelated Heartbeat", "8-Bit Hero", "Boss Level", 188000, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-9.mp3", "https://images.unsplash.com/photo-1542751371-adc38448a05e?w=500", "Bitpop"),
                Track("spotify_sb_3_2", "Cosmic Bass Burst", "Subwoofer Kid", "Galaxy Drops", 220000, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-10.mp3", "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=500", "Future Bass"),
                Track("spotify_sb_3_3", "Voltage Overload", "Synth Master", "High Voltage", 205000, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-11.mp3", "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=500", "EDM")
            )
            else -> emptyList()
        }
    }

    // Cryptography Helpers for PKCE Auth Flow
    private fun generateCodeVerifier(): String {
        val secureRandom = SecureRandom()
        val code = ByteArray(64)
        secureRandom.nextBytes(code)
        return Base64.encodeToString(code, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    private fun generateCodeChallenge(codeVerifier: String): String {
        val bytes = codeVerifier.toByteArray(Charsets.US_ASCII)
        val messageDigest = MessageDigest.getInstance("SHA-256")
        val digest = messageDigest.digest(bytes)
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }
}
