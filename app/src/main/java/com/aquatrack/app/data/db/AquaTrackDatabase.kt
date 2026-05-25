package com.aquatrack.app.data.db

import android.content.Context
import androidx.room.migration.Migration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.aquatrack.app.data.Fish
import com.aquatrack.app.data.Tank
import com.aquatrack.app.data.dao.FishDao
import com.aquatrack.app.data.dao.TankDao

@Database(entities = [Tank::class, Fish::class], version = 2, exportSchema = false)
abstract class AquaTrackDatabase : RoomDatabase() {
    abstract fun tankDao(): TankDao
    abstract fun fishDao(): FishDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tanks ADD COLUMN imageUri TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE fish ADD COLUMN imageUri TEXT NOT NULL DEFAULT ''")
            }
        }

        @Volatile
        private var INSTANCE: AquaTrackDatabase? = null

        fun getInstance(context: Context): AquaTrackDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AquaTrackDatabase::class.java,
                    "aquatrack.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
