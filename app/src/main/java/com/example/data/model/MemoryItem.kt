package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_memory")
data class MemoryItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fact: String,
    val category: String = "General",
    val createdAt: Long = System.currentTimeMillis(),
    val source: String = "Voice interaction"
)
