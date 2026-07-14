package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "demands")
data class Demand(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val titulo: String,
    val descricao: String,
    val prioridade: String, // "SEM_PRIORIDADE", "NORMAL", "ALTA"
    val status: String,     // "PENDENTE", "CONCLUIDO"
    val dataCriacao: Long = System.currentTimeMillis(),
    val anexoUrl: String? = null,
    val protocolo: String = "",
    val sortOrder: Int = 0,
    val filesJson: String? = null
) : Serializable

data class DemandFile(val name: String, val url: String) : Serializable

fun Demand.getFilesList(): List<DemandFile> {
    val list = mutableListOf<DemandFile>()
    if (!filesJson.isNullOrBlank()) {
        try {
            val array = org.json.JSONArray(filesJson)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val name = obj.optString("name", "")
                val url = obj.optString("url", "")
                if (url.isNotEmpty()) {
                    list.add(DemandFile(name.ifEmpty { url.substringAfterLast("/") }, url))
                }
            }
        } catch (e: Exception) {
            // ignore
        }
    }
    // Fallback if list is empty but anexoUrl is present
    if (list.isEmpty() && !anexoUrl.isNullOrBlank()) {
        list.add(DemandFile(anexoUrl.substringAfterLast("/"), anexoUrl))
    }
    return list
}
