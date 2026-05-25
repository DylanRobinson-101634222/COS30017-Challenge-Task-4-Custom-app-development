package com.aquatrack.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tanks")
data class Tank(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val volumeLitres: Int,
    val waterType: String,
    val targetTempC: Float,
    val lastCleanedEpochMillis: Long,
    val reminderEnabled: Boolean,
    val reminderFrequency: String,
    val reminderTime: String,
    val imageUri: String = ""
)
