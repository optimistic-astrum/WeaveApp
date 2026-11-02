package com.weave.app

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val appName: String,
    val sender: String,
    val message: String,
    val timestamp: Long,
    val isDeleted: Boolean = false
)
