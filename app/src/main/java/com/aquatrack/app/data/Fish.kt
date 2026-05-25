package com.aquatrack.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "fish",
    foreignKeys = [
        ForeignKey(
            entity = Tank::class,
            parentColumns = ["id"],
            childColumns = ["tankId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("tankId")]
)
data class Fish(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tankId: Long,
    val speciesName: String,
    val scientificName: String,
    val quantity: Int,
    val minTempC: Float,
    val maxTempC: Float,
    val imageUri: String = ""
)
