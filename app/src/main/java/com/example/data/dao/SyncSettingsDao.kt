package com.example.data.dao

import androidx.room.*
import com.example.data.model.SyncSettings
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncSettingsDao {
    @Query("SELECT * FROM sync_settings WHERE id = 1")
    fun getSyncSettingsFlow(): Flow<SyncSettings?>

    @Query("SELECT * FROM sync_settings WHERE id = 1")
    suspend fun getSyncSettings(): SyncSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSyncSettings(settings: SyncSettings)
}
