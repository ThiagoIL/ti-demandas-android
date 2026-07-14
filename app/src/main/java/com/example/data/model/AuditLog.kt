package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "audit_logs")
data class AuditLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dataHora: Long = System.currentTimeMillis(),
    val executor: String,
    val operacao: String, // "CREATE_DEMAND", "UPDATE_DEMAND", "DELETE_DEMAND", "LOGIN_SUCCESS"
    val modulo: String,   // "demands #889", "auth #1", etc.
    val detalhes: String
) : Serializable
