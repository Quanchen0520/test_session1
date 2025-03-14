package com.example.myapplication

// 1
data class MusicResponse(
    val resultList: List<MusicItem>
)

// 2
data class MusicItem(
    val SongURL: String? = null,
    val imageURL: String? = null,
    val SongName: String? = null
)
