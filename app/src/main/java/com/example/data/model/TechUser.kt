package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "tech_users")
data class TechUser(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nome: String,
    val email: String,
    val nivel: String, // "MASTER", "COLABORADOR"
    val ativo: Boolean = true
) : Serializable
