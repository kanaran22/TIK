package com.kanaran.tik.data

import kotlinx.coroutines.flow.Flow

class SavedPlaceRepository(private val dao: SavedPlaceDao) {
    fun observeAll(): Flow<List<SavedPlace>> = dao.observeAll()
    suspend fun save(place: SavedPlace): Long = dao.upsert(place)
    suspend fun delete(place: SavedPlace) = dao.delete(place)
}
