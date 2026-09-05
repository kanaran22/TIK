package com.geotask.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.geotask.app.data.RepeatType
import com.geotask.app.data.SavedPlace
import com.geotask.app.data.SavedPlaceRepository
import com.geotask.app.data.Task
import com.geotask.app.data.TaskRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TaskViewModel(
    private val repository: TaskRepository,
    private val savedPlaceRepository: SavedPlaceRepository
) : ViewModel() {

    val tasks: StateFlow<List<Task>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** taskId -> the set of epoch days it was marked done, for streaks/stats. */
    val completions: StateFlow<Map<Long, Set<Long>>> = repository.observeAllCompletions()
        .map { rows -> rows.groupBy({ it.taskId }, { it.epochDay }).mapValues { it.value.toSet() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val savedPlaces: StateFlow<List<SavedPlace>> = savedPlaceRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun save(task: Task) {
        viewModelScope.launch { repository.save(task) }
    }

    /** The list checkbox: a one-off task completes for good; a repeating task completes just today. */
    fun toggleChecked(task: Task, checked: Boolean) {
        viewModelScope.launch {
            if (RepeatType.fromStorage(task.repeatType) == RepeatType.ONCE) {
                repository.setSeriesActive(task, active = !checked)
            } else {
                repository.setCompletedToday(task, completed = checked)
            }
        }
    }

    fun delete(task: Task) {
        viewModelScope.launch { repository.delete(task) }
    }

    /** Restores a task deleted a moment ago, completion history included — the "Undo" action. */
    fun restore(task: Task, completedDays: Set<Long>) {
        viewModelScope.launch { repository.restore(task, completedDays) }
    }

    suspend fun getById(id: Long): Task? = repository.getById(id)

    fun savePlace(name: String, latitude: Double, longitude: Double, radiusMeters: Float) {
        viewModelScope.launch {
            savedPlaceRepository.save(SavedPlace(name = name, latitude = latitude, longitude = longitude, radiusMeters = radiusMeters))
        }
    }

    fun deletePlace(place: SavedPlace) {
        viewModelScope.launch { savedPlaceRepository.delete(place) }
    }
}
