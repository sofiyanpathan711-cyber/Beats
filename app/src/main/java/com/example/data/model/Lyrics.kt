package com.example.data.model

import java.io.Serializable

data class LyricLine(
    val timestampMs: Long,
    val text: String
) : Serializable

data class SongLyrics(
    val trackId: String,
    val lines: List<LyricLine>
) : Serializable {

    /**
     * Finds the index of the active lyric line for the given playback position.
     */
    fun getActiveLineIndex(progressMs: Long): Int {
        if (lines.isEmpty()) return -1
        
        // Find the line that has timestamp <= progressMs, and is closest to it
        var activeIndex = -1
        for (i in lines.indices) {
            if (lines[i].timestampMs <= progressMs) {
                if (activeIndex == -1 || lines[i].timestampMs > lines[activeIndex].timestampMs) {
                    activeIndex = i
                }
            }
        }
        return activeIndex
    }
}

object LyricsParser {
    /**
     * Parses custom editable timed raw text:
     * "[mm:ss] Lyric" or "timestampMs|Lyric Line"
     */
    fun parse(trackId: String, rawText: String): SongLyrics {
        val lines = mutableListOf<LyricLine>()
        
        rawText.lineSequence().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty()) return@forEach
            
            try {
                // Try parsing [mm:ss] format
                if (trimmed.startsWith("[") && trimmed.contains("]")) {
                    val endBracketIdx = trimmed.indexOf("]")
                    val timePart = trimmed.substring(1, endBracketIdx).trim()
                    val lyricText = trimmed.substring(endBracketIdx + 1).trim()
                    
                    val timeParts = timePart.split(":")
                    if (timeParts.size >= 2) {
                        val minutes = timeParts[0].toLongOrNull() ?: 0L
                        val seconds = timeParts[1].toDoubleOrNull() ?: 0.0
                        val ms = (minutes * 60 * 1000) + (seconds * 1000).toLong()
                        lines.add(LyricLine(ms, lyricText))
                    }
                } else if (trimmed.contains("|")) {
                    // Try parsing milliseconds|text format
                    val parts = trimmed.split("|", limit = 2)
                    val ms = parts[0].trim().toLongOrNull() ?: 0L
                    val text = parts[1].trim()
                    lines.add(LyricLine(ms, text))
                } else {
                    // Fallback: divide lines evenly across a minute
                    // (we skip unformated lines or add them at 0)
                }
            } catch (e: Exception) {
                // Ignore parse errors on individual lines
            }
        }
        
        // Ensure lines are sorted chronologically
        return SongLyrics(trackId, lines.sortedBy { it.timestampMs })
    }
    
    /**
     * Formats lyrics lines into a standard user-editable timed string like:
     * [00:10.00] Lyrc text
     */
    fun format(lyrics: SongLyrics): String {
        return lyrics.lines.joinToString("\n") { line ->
            val totalSeconds = line.timestampMs / 1000f
            val minutes = (totalSeconds / 60).toInt()
            val seconds = totalSeconds % 60f
            String.format("[%02d:%05.2f] %s", minutes, seconds, line.text)
        }
    }
}
