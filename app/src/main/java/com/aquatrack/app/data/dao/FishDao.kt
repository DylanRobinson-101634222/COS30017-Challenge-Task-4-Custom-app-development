package com.aquatrack.app.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.aquatrack.app.data.Fish

@Dao
interface FishDao {
    @Query("SELECT * FROM fish ORDER BY speciesName ASC")
    fun observeAll(): LiveData<List<Fish>>

    @Query("SELECT * FROM fish WHERE tankId = :tankId ORDER BY speciesName ASC")
    fun observeByTank(tankId: Long): LiveData<List<Fish>>

    @Query("SELECT * FROM fish WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Fish?

    @Insert
    suspend fun insert(fish: Fish): Long

    @Update
    suspend fun update(fish: Fish)

    @Delete
    suspend fun delete(fish: Fish)
}
