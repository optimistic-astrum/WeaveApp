package com.weave.app

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    @Insert
    suspend fun insert(notification: NotificationEntity)

    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<NotificationEntity>>

    @Query("UPDATE notifications SET isDeleted = 1 WHERE sender = :sender AND id = (SELECT id FROM notifications WHERE sender = :sender ORDER BY timestamp DESC LIMIT 1)")
    suspend fun markLastAsDeleted(sender: String)
}
