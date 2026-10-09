package com.example.nmf.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocked_sites")
data class BlockedSite(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val domain: String,
    val dateAdded: Long = System.currentTimeMillis(),
)