package com.example.data.dao

import androidx.room.*
import com.example.data.model.Demand
import kotlinx.coroutines.flow.Flow

@Dao
interface DemandDao {
    @Query("SELECT * FROM demands ORDER BY sortOrder ASC, dataCriacao DESC")
    fun getAllDemandsFlow(): Flow<List<Demand>>

    @Query("SELECT * FROM demands ORDER BY sortOrder ASC, dataCriacao DESC")
    suspend fun getAllDemands(): List<Demand>

    @Query("SELECT * FROM demands WHERE id = :id")
    suspend fun getDemandById(id: Int): Demand?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDemand(demand: Demand): Long

    @Update
    suspend fun updateDemand(demand: Demand)

    @Delete
    suspend fun deleteDemand(demand: Demand)

    @Query("DELETE FROM demands")
    suspend fun clearAllDemands()
}
