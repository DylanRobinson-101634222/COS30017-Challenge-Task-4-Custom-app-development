package com.aquatrack.app.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.aquatrack.app.data.Tank

@Dao
interface TankDao {
    @Query("SELECT * FROM tanks ORDER BY name ASC")
    fun observeAll(): LiveData<List<Tank>>

    @Query("SELECT * FROM tanks WHERE id = :id LIMIT 1")
    fun observeById(id: Long): LiveData<Tank?>

    @Query("SELECT * FROM tanks WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Tank?

    @Insert
    suspend fun insert(tank: Tank): Long

    @Update
    suspend fun update(tank: Tank)

    @Delete
    suspend fun delete(tank: Tank)
}
