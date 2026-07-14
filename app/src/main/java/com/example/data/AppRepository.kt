package com.example.data

import com.example.data.dao.AuditLogDao
import com.example.data.dao.DemandDao
import com.example.data.dao.SyncSettingsDao
import com.example.data.dao.TechUserDao
import com.example.data.model.AuditLog
import com.example.data.model.Demand
import com.example.data.model.SyncSettings
import com.example.data.model.TechUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class AppRepository(
    private val demandDao: DemandDao,
    private val techUserDao: TechUserDao,
    private val auditLogDao: AuditLogDao,
    private val syncSettingsDao: SyncSettingsDao
) {
    val allDemandsFlow: Flow<List<Demand>> = demandDao.getAllDemandsFlow()
    val allTechUsersFlow: Flow<List<TechUser>> = techUserDao.getAllTechUsersFlow()
    val allAuditLogsFlow: Flow<List<AuditLog>> = auditLogDao.getAllAuditLogsFlow()
    val syncSettingsFlow: Flow<SyncSettings?> = syncSettingsDao.getSyncSettingsFlow()

    suspend fun insertDemand(demand: Demand, executor: String = "Inacio"): Long = withContext(Dispatchers.IO) {
        val result = demandDao.insertDemand(demand)
        insertAuditLog(
            AuditLog(
                executor = executor,
                operacao = "CREATE_DEMAND",
                modulo = "demands #${demand.id.takeIf { it > 0 } ?: result}",
                detalhes = "Criou demanda: ${demand.titulo}"
            )
        )
        result
    }

    suspend fun updateDemand(demand: Demand, executor: String = "Inacio") = withContext(Dispatchers.IO) {
        demandDao.updateDemand(demand)
        insertAuditLog(
            AuditLog(
                executor = executor,
                operacao = "UPDATE_DEMAND",
                modulo = "demands #${demand.id}",
                detalhes = "Atualizou demanda: ${demand.titulo}"
            )
        )
    }

    suspend fun deleteDemand(demand: Demand, executor: String = "Inacio") = withContext(Dispatchers.IO) {
        demandDao.deleteDemand(demand)
        insertAuditLog(
            AuditLog(
                executor = executor,
                operacao = "DELETE_DEMAND",
                modulo = "demands #${demand.id}",
                detalhes = "Excluiu demanda: ${demand.titulo}"
            )
        )
    }

    suspend fun insertTechUser(user: TechUser, executor: String = "Inacio") = withContext(Dispatchers.IO) {
        val result = techUserDao.insertTechUser(user)
        insertAuditLog(
            AuditLog(
                executor = executor,
                operacao = "CREATE_TECH_USER",
                modulo = "users #${user.id.takeIf { it > 0 } ?: result}",
                detalhes = "Adicionou colaborador: ${user.nome}"
            )
        )
    }

    suspend fun updateTechUser(user: TechUser, executor: String = "Inacio") = withContext(Dispatchers.IO) {
        techUserDao.updateTechUser(user)
        insertAuditLog(
            AuditLog(
                executor = executor,
                operacao = "UPDATE_TECH_USER",
                modulo = "users #${user.id}",
                detalhes = "Atualizou dados de: ${user.nome} (${if (user.ativo) "ATIVO" else "DESABILITADO"})"
            )
        )
    }

    suspend fun deleteTechUser(user: TechUser, executor: String = "Inacio") = withContext(Dispatchers.IO) {
        techUserDao.deleteTechUser(user)
        insertAuditLog(
            AuditLog(
                executor = executor,
                operacao = "DELETE_TECH_USER",
                modulo = "users #${user.id}",
                detalhes = "Removeu colaborador: ${user.nome}"
            )
        )
    }

    suspend fun insertAuditLog(log: AuditLog) = withContext(Dispatchers.IO) {
        auditLogDao.insertAuditLog(log)
    }

    suspend fun clearAndInsertDemands(demandsList: List<Demand>) = withContext(Dispatchers.IO) {
        demandDao.clearAllDemands()
        for (demand in demandsList) {
            demandDao.insertDemand(demand)
        }
    }

    suspend fun clearAndInsertTechUsers(usersList: List<TechUser>) = withContext(Dispatchers.IO) {
        techUserDao.clearAllTechUsers()
        for (user in usersList) {
            techUserDao.insertTechUser(user)
        }
    }

    suspend fun clearAndInsertAuditLogs(logsList: List<AuditLog>) = withContext(Dispatchers.IO) {
        auditLogDao.clearAllAuditLogs()
        for (log in logsList) {
            auditLogDao.insertAuditLog(log)
        }
    }

    suspend fun saveSyncSettings(settings: SyncSettings, executor: String = "Inacio") = withContext(Dispatchers.IO) {
        syncSettingsDao.saveSyncSettings(settings)
        insertAuditLog(
            AuditLog(
                executor = executor,
                operacao = "UPDATE_SETTINGS",
                modulo = "config #1",
                detalhes = "Atualizou configurações de sincronização do banco de dados MySQL e bucket Supabase."
            )
        )
    }

    // Real synchronization using corporate API routes
    suspend fun performSync(
        apiService: com.exemplo.tidemandas.network.TiDemandaService,
        settings: SyncSettings,
        onStep: (String) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            onStep("Iniciando sincronização com a API corporativa (system.tipmp.com.br)...")
            delay(500)

            // Step 1: Fetch stats
            onStep("Buscando estatísticas consolidadas do painel...")
            try {
                val stats = apiService.getStats()
                onStep("Painel carregado! Total de demandas no servidor: ${stats.summary.total} (${stats.summary.completed} concluídas, ${stats.summary.pending} pendentes).")
            } catch (e: Exception) {
                onStep("Aviso: Falha ao obter estatísticas: ${e.localizedMessage}")
            }
            delay(500)

            // Step 2: Fetch and reconcile demands
            onStep("Sincronizando demandas do servidor com o banco de dados local...")
            try {
                val apiDemands = apiService.getDemands()
                var insertedCount = 0
                demandDao.clearAllDemands()
                for (apiDemand in apiDemands) {
                    val localDemand = Demand(
                        id = apiDemand.id,
                        titulo = apiDemand.name,
                        descricao = apiDemand.description ?: "",
                        prioridade = when (apiDemand.priority) {
                            2 -> "ALTA"
                            1 -> "NORMAL"
                            else -> "SEM_PRIORIDADE"
                        },
                        status = if (apiDemand.done == 1) "CONCLUIDO" else "PENDENTE",
                        dataCriacao = parseIsoDate(apiDemand.created_at),
                        anexoUrl = apiDemand.file_url,
                        protocolo = "#${String.format("%06d", apiDemand.id)}",
                        sortOrder = apiDemand.sort_order ?: 0,
                        filesJson = apiDemand.files
                    )
                    demandDao.insertDemand(localDemand)
                    insertedCount++
                }
                onStep("Sucesso! Sincronizados $insertedCount chamados no banco local.")
            } catch (e: Exception) {
                onStep("Erro ao sincronizar demandas: ${e.localizedMessage}")
                throw e
            }
            delay(500)

            // Step 3: Fetch team members (master-only or general fallback)
            onStep("Sincronizando equipe de técnicos...")
            try {
                val apiUsers = apiService.getUsers()
                var userCount = 0
                techUserDao.clearAllTechUsers()
                for (apiUser in apiUsers) {
                    val techUser = TechUser(
                        id = apiUser.id,
                        nome = apiUser.name,
                        email = apiUser.email,
                        nivel = apiUser.role.uppercase(),
                        ativo = apiUser.active == 1
                    )
                    techUserDao.insertTechUser(techUser)
                    userCount++
                }
                onStep("Equipe sincronizada: $userCount técnicos cadastrados.")
            } catch (e: Exception) {
                onStep("Aviso: Sem acesso administrativo para sincronizar equipe completa.")
            }
            delay(500)

            // Step 4: Fetch logs (master-only)
            onStep("Sincronizando histórico de auditoria...")
            try {
                val apiLogs = apiService.getAuditLogs()
                var logCount = 0
                auditLogDao.clearAllAuditLogs()
                for (apiLog in apiLogs) {
                    val auditLog = AuditLog(
                        id = apiLog.id,
                        dataHora = parseIsoDate(apiLog.created_at),
                        executor = apiLog.user_name ?: "SISTEMA",
                        operacao = apiLog.action,
                        modulo = apiLog.module ?: if (!apiLog.target_type.isNullOrBlank()) "${apiLog.target_type} #${apiLog.target_id ?: ""}" else "",
                        detalhes = apiLog.details ?: ""
                    )
                    auditLogDao.insertAuditLog(auditLog)
                    logCount++
                }
                onStep("Histórico de auditoria atualizado: $logCount registros importados.")
            } catch (e: Exception) {
                onStep("Aviso: Sem acesso administrativo para buscar logs globais.")
            }
            delay(500)

            onStep("Sincronização concluída com êxito! Banco de dados local alinhado.")

            insertAuditLog(
                AuditLog(
                    executor = "SISTEMA",
                    operacao = "DATABASE_SYNC",
                    modulo = "database #1",
                    detalhes = "Sincronização completa executada com sucesso com a API corporativa."
                )
            )

            Result.success("Banco de dados sincronizado!")
        } catch (e: Exception) {
            insertAuditLog(
                AuditLog(
                    executor = "SISTEMA",
                    operacao = "SYNC_FAILED",
                    modulo = "database #1",
                    detalhes = "Falha ao sincronizar: ${e.localizedMessage}"
                )
            )
            Result.failure(e)
        }
    }

    private fun parseIsoDate(dateStr: String?): Long {
        if (dateStr.isNullOrBlank()) return System.currentTimeMillis()
        return try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }
            sdf.parse(dateStr)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            try {
                val sdf2 = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).apply {
                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                }
                sdf2.parse(dateStr)?.time ?: System.currentTimeMillis()
            } catch (e2: Exception) {
                System.currentTimeMillis()
            }
        }
    }
}
