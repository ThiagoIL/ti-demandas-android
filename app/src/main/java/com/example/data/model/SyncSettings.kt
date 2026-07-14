package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "sync_settings")
data class SyncSettings(
    @PrimaryKey val id: Int = 1,
    val mysqlHost: String = "localhost",
    val mysqlPort: String = "3306",
    val mysqlDb: String = "ti_demandas",
    val mysqlUser: String = "root",
    val mysqlPass: String = "",
    val supabaseUrl: String = "",
    val supabaseKey: String = "",
    val supabaseBucket: String = "chamados",
    val useCloudSync: Boolean = false // Toggle between offline simulation or direct sync attempt
) : Serializable
