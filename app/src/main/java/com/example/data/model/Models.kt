package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class User(
    val id: String,
    val name: String,
    val email: String,
    val username: String
)

@JsonClass(generateAdapter = true)
data class LoginRequest(
    val username: String,
    val password: String
)

@JsonClass(generateAdapter = true)
data class LoginResponse(
    val token: String,
    val user: User
)

enum class Priority(val displayName: String) {
    LOW("Baixa"),
    MEDIUM("Média"),
    HIGH("Alta");

    companion object {
        fun fromString(value: String): Priority {
            return entries.firstOrNull { 
                it.name.equals(value, ignoreCase = true) || 
                it.displayName.equals(value, ignoreCase = true) 
            } ?: LOW
        }
    }
}

enum class DemandStatus(val displayName: String) {
    PENDING("Pendente"),
    IN_PROGRESS("Em Andamento"),
    COMPLETED("Finalizado");

    companion object {
        fun fromString(value: String): DemandStatus {
            return entries.firstOrNull { 
                it.name.equals(value, ignoreCase = true) || 
                it.displayName.equals(value, ignoreCase = true) 
            } ?: PENDING
        }
    }
}

@JsonClass(generateAdapter = true)
data class ModelsDemand(
    val id: String,
    val title: String,
    val description: String,
    val priority: String, // "Baixa", "Média", "Alta"
    val status: String,   // "Pendente", "Em Andamento", "Finalizado"
    val category: String,
    val requesterName: String,
    val createdAt: String
)
