package com.example.data.dao

import androidx.room.*
import com.example.data.model.TechUser
import kotlinx.coroutines.flow.Flow

@Dao
interface TechUserDao {
    @Query("SELECT * FROM tech_users ORDER BY id ASC")
    fun getAllTechUsersFlow(): Flow<List<TechUser>>

    @Query("SELECT * FROM tech_users ORDER BY id ASC")
    suspend fun getAllTechUsers(): List<TechUser>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTechUser(user: TechUser): Long

    @Update
    suspend fun updateTechUser(user: TechUser)

    @Delete
    suspend fun deleteTechUser(user: TechUser)

    @Query("DELETE FROM tech_users")
    suspend fun clearAllTechUsers()
}
