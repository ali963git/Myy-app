package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey val id: String, // format: "category_id", e.g. "article_1"
    val type: String,          // "article", "audio", "video"
    val title: String,
    val info: String,
    val timestamp: Long = System.currentTimeMillis()
)
