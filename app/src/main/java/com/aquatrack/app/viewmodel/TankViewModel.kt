package com.aquatrack.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aquatrack.app.data.Fish
import com.aquatrack.app.data.Tank
import com.aquatrack.app.data.db.AquaTrackDatabase
import com.aquatrack.app.repository.AquaRepository
import com.aquatrack.app.worker.ReminderScheduler
import kotlinx.coroutines.launch

class TankViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AquaRepository(AquaTrackDatabase.getInstance(application))

    val tanks = repository.observeTanks()
    val fish = repository.observeFish()

    fun tankById(tankId: Long) = repository.observeTankById(tankId)

    fun fishForTank(tankId: Long) = repository.observeFishForTank(tankId)

    suspend fun getTankById(tankId: Long): Tank? = repository.getTankById(tankId)
    suspend fun getFishById(fishId: Long): Fish? = repository.getFishById(fishId)

    fun saveTank(tank: Tank) {
        viewModelScope.launch {
            val savedTank = if (tank.id == 0L) {
                val newId = repository.insertTank(tank)
                tank.copy(id = newId)
            } else {
                repository.updateTank(tank)
                tank
            }
            ReminderScheduler.scheduleOrCancel(getApplication(), savedTank)
        }
    }

    fun deleteTank(tank: Tank) {
        viewModelScope.launch {
            repository.deleteTank(tank)
            ReminderScheduler.cancel(getApplication(), tank.id)
        }
    }

    fun saveFish(fish: Fish) {
        viewModelScope.launch {
            if (fish.id == 0L) repository.insertFish(fish) else repository.updateFish(fish)
        }
    }

    fun deleteFish(fish: Fish) {
        viewModelScope.launch { repository.deleteFish(fish) }
    }
}
