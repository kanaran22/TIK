package com.geotask.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.geotask.app.data.SavedPlaceRepository
import com.geotask.app.data.TaskRepository

class TaskViewModelFactory(
    private val repository: TaskRepository,
    private val savedPlaceRepository: SavedPlaceRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(TaskViewModel::class.java))
        return TaskViewModel(repository, savedPlaceRepository) as T
    }
}
