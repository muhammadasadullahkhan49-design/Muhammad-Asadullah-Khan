package com.example.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_alarms")
data class CustomAlarm(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val hour: Int,
    val minute: Int,
    val durationMinutes: Int = 30,
    val isEnabled: Boolean = true,
    val isRecurring: Boolean = true,
    val repeatDays: String = "Mon,Tue,Wed,Thu,Fri,Sat,Sun", // Comma separated
    val createdAt: Long = System.currentTimeMillis()
)
