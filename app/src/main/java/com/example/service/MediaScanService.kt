package com.example.service

import android.app.Service
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.os.Environment
import android.os.IBinder
import android.provider.MediaStore
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.LocalTrackEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class MediaScanService : Service() {
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "MediaScanService onStartCommand triggered")
        
        serviceScope.launch {
            try {
                performScan(applicationContext)
            } catch (e: Exception) {
                Log.e(TAG, "Exception during media scan: ${e.message}", e)
            } finally {
                stopSelf(startId)
            }
        }
        
        return START_NOT_STICKY
    }

    private suspend fun performScan(context: Context) {
        val database = AppDatabase.getDatabase(context)
        val musicDao = database.musicDao()
        
        val foundTracks = mutableListOf<LocalTrackEntity>()
        
        // 1. Scan MediaStore
        Log.d(TAG, "Starting MediaStore query...")
        scanMediaStore(context, foundTracks)
        Log.d(TAG, "MediaStore scan retrieved ${foundTracks.size} tracks.")
        
        // 2. Scan External Files Directory
        Log.d(TAG, "Starting External files directory scan...")
        scanExternalDirectories(context, foundTracks)
        Log.d(TAG, "Scan with direct directories got total ${foundTracks.size} tracks.")
        
        // 3. Fallback: If absolutely empty, populate dummy sample files
        if (foundTracks.isEmpty()) {
            Log.d(TAG, "No local tracks found in storage. Seeding with compact sample tracks...")
            createSampleAudioFiles(context)
            // Re-run directory scan to record the files
            scanExternalDirectories(context, foundTracks)
            Log.d(TAG, "Seeding complete. New local tracks library size: ${foundTracks.size}")
        }
        
        // 4. Save to Database
        musicDao.clearAllLocalTracks()
        if (foundTracks.isNotEmpty()) {
            musicDao.insertLocalTracks(foundTracks)
            Log.d(TAG, "Persisted ${foundTracks.size} local tracks to local database table.")
        }
    }

    private fun scanMediaStore(context: Context, foundTracks: MutableList<LocalTrackEntity>) {
        val resolver = context.contentResolver
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA
        )
        
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        
        try {
            val cursor = resolver.query(uri, projection, selection, null, null)
            cursor?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                
                while (c.moveToNext()) {
                    val idNum = c.getLong(idCol)
                    val title = c.getString(titleCol) ?: "Unknown Track"
                    val artist = c.getString(artistCol) ?: "Unknown Artist"
                    val album = c.getString(albumCol) ?: "Unknown Album"
                    val duration = c.getLong(durationCol)
                    val data = c.getString(dataCol) ?: ""
                    
                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        idNum
                    ).toString()
                    
                    val id = "media_$idNum"
                    val localTrack = LocalTrackEntity(
                        id = id,
                        title = title,
                        artist = artist,
                        album = album,
                        durationMs = duration,
                        audioUrl = if (data.isNotEmpty()) data else contentUri,
                        coverUrl = "",
                        genre = "MediaStore",
                        dateModified = System.currentTimeMillis()
                    )
                    
                    if (foundTracks.none { it.id == id }) {
                        foundTracks.add(localTrack)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying MediaStore: ${e.message}")
        }
    }

    private fun scanExternalDirectories(context: Context, foundTracks: MutableList<LocalTrackEntity>) {
        val musicDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        
        val dirs = listOfNotNull(musicDir, downloadDir)
        dirs.forEach { dir ->
            if (dir.exists() && dir.isDirectory) {
                dir.listFiles()?.forEach { file ->
                    if (file.isFile && isAudioFile(file.name)) {
                        parseFileMetadata(file, foundTracks)
                    }
                }
            }
        }
    }

    private fun isAudioFile(fileName: String): Boolean {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return ext in listOf("mp3", "m4a", "wav", "ogg", "aac", "flac")
    }

    private fun parseFileMetadata(file: File, foundTracks: MutableList<LocalTrackEntity>) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(file.absolutePath)
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?: file.nameWithoutExtension.replace('_', ' ')
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?: "Unknown Device Artist"
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                ?: "Local Folder Album"
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: 180000L // Default to 3 min fallback if empty
            val genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)
                ?: "Local Metadata File"

            val id = "local_${file.name.hashCode()}"
            val localTrack = LocalTrackEntity(
                id = id,
                title = title,
                artist = artist,
                album = album,
                durationMs = durationMs,
                audioUrl = file.absolutePath,
                coverUrl = "",
                genre = genre,
                dateModified = file.lastModified()
            )
            
            if (foundTracks.none { it.id == id }) {
                foundTracks.add(localTrack)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading properties of ${file.name}: ${e.message}")
            // Fallback for missing or unreadable ID3 metadata headers
            val id = "local_${file.name.hashCode()}"
            val fallbackTrack = LocalTrackEntity(
                id = id,
                title = file.nameWithoutExtension.replace('_', ' '),
                artist = "Fallback Local Audio File",
                album = "External Album",
                durationMs = 180000L,
                audioUrl = file.absolutePath,
                coverUrl = "",
                genre = "Local Audio",
                dateModified = file.lastModified()
            )
            if (foundTracks.none { it.id == id }) {
                foundTracks.add(fallbackTrack)
            }
        } finally {
            try {
                retriever.release()
            } catch (ex: Exception) {}
        }
    }

    private fun createSampleAudioFiles(context: Context) {
        val destDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: return
        if (!destDir.exists()) {
            destDir.mkdirs()
        }
        
        val samples = listOf(
            SampleAudio("Ocean_Breeze_Ambient.ogg", "https://actions.google.com/sounds/v1/ambient/ambient_hum_air_conditioner.ogg"),
            SampleAudio("Futuristic_Teleport_Sound.ogg", "https://actions.google.com/sounds/v1/science_fiction/teleport.ogg")
        )
        
        for (sample in samples) {
            val file = File(destDir, sample.fileName)
            if (!file.exists()) {
                try {
                    Log.d(TAG, "Downloading sample ${sample.fileName} from ${sample.url}")
                    val url = URL(sample.url)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.connectTimeout = 8000
                    connection.readTimeout = 8000
                    connection.inputStream.use { input ->
                        file.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    Log.d(TAG, "Successfully downloaded sample track in background: ${file.absolutePath}")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed downloading file ${sample.fileName}: ${e.message}")
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        Log.d(TAG, "MediaScanService stopped and resource jobs cancelled")
    }

    companion object {
        private const val TAG = "MediaScanService"
        
        fun start(context: Context) {
            try {
                val intent = Intent(context, MediaScanService::class.java)
                context.startService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed starting service: ${e.message}")
            }
        }
    }
}

data class SampleAudio(val fileName: String, val url: String)
