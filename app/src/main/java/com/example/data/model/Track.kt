package com.example.data.model

import java.io.Serializable

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val audioUrl: String,
    val coverUrl: String,
    val genre: String
) : Serializable
