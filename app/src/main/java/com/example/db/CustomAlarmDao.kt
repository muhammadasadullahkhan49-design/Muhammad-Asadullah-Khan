package com.example.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomAlarmDao {

    @Query("SELECT * FROM custom_alarms ORDER BY hour ASC, minute ASC")
    fun getAllAlarms(): Flow<List<CustomAlarm>>

    @Query("SELECT * FROM custom_alarms WHERE isEnabled = 1")
    suspend fun getActiveAlarms(): List<CustomAlarm>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlarm(alarm: CustomAlarm): Long

    @Update
    suspend fun updateAlarm(alarm: CustomAlarm)

    @Delete
    suspend fun deleteAlarm(alarm: CustomAlarm)

    @Query("DELETE FROM custom_alarms WHERE id = :id")
    suspend fun deleteAlarmById(id: Int)
}
