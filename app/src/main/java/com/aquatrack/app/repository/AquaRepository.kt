package com.aquatrack.app.repository

import androidx.lifecycle.LiveData
import com.aquatrack.app.data.Fish
import com.aquatrack.app.data.Tank
import com.aquatrack.app.data.db.AquaTrackDatabase

class AquaRepository(private val db: AquaTrackDatabase) {
    fun observeTanks(): LiveData<List<Tank>> = db.tankDao().observeAll()
    fun observeTankById(tankId: Long): LiveData<Tank?> = db.tankDao().observeById(tankId)
    fun observeFish(): LiveData<List<Fish>> = db.fishDao().observeAll()
    fun observeFishForTank(tankId: Long): LiveData<List<Fish>> = db.fishDao().observeByTank(tankId)

    suspend fun getTankById(tankId: Long): Tank? = db.tankDao().getById(tankId)
    suspend fun getFishById(fishId: Long): Fish? = db.fishDao().getById(fishId)

    suspend fun insertTank(tank: Tank) = db.tankDao().insert(tank)
    suspend fun updateTank(tank: Tank) = db.tankDao().update(tank)
    suspend fun deleteTank(tank: Tank) = db.tankDao().delete(tank)

    suspend fun insertFish(fish: Fish) = db.fishDao().insert(fish)
    suspend fun updateFish(fish: Fish) = db.fishDao().update(fish)
    suspend fun deleteFish(fish: Fish) = db.fishDao().delete(fish)
}
