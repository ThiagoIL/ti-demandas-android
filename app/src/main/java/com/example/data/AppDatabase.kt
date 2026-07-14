package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.AuditLogDao
import com.example.data.dao.DemandDao
import com.example.data.dao.SyncSettingsDao
import com.example.data.dao.TechUserDao
import com.example.data.model.AuditLog
import com.example.data.model.Demand
import com.example.data.model.SyncSettings
import com.example.data.model.TechUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

@Database(
    entities = [Demand::class, TechUser::class, AuditLog::class, SyncSettings::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun demandDao(): DemandDao
    abstract fun techUserDao(): TechUserDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun syncSettingsDao(): SyncSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ti_demandas_database"
                )
                .addCallback(AppDatabaseCallback(scope))
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDatabase(database)
                }
            }
        }

        suspend fun populateDatabase(db: AppDatabase) {
            val demandDao = db.demandDao()
            val techUserDao = db.techUserDao()
            val auditLogDao = db.auditLogDao()
            val syncSettingsDao = db.syncSettingsDao()

            // 1. Prepopulate Sync Settings
            syncSettingsDao.saveSyncSettings(
                SyncSettings(
                    id = 1,
                    mysqlHost = "192.168.1.100",
                    mysqlPort = "3306",
                    mysqlDb = "ti_demandas_db",
                    mysqlUser = "admin_ti",
                    mysqlPass = "secret_mysql_pass",
                    supabaseUrl = "https://your-supabase-project.supabase.co",
                    supabaseKey = "your-supabase-anon-key",
                    supabaseBucket = "ti-bucket-attachments",
                    useCloudSync = false
                )
            )

            // 2. Prepopulate Tech Users (from screenshots)
            val users = listOf(
                TechUser(id = 1, nome = "Inacio", email = "ti@gmail.com", nivel = "MASTER", ativo = true),
                TechUser(id = 2, nome = "Jarbas", email = "bolao@gmail.com", nivel = "COLABORADOR", ativo = true),
                TechUser(id = 3, nome = "Luanzada", email = "piru@gmail.com", nivel = "COLABORADOR", ativo = true),
                TechUser(id = 4, nome = "matheus", email = "mt@gmail.com", nivel = "COLABORADOR", ativo = false)
            )
            users.forEach { techUserDao.insertTechUser(it) }

            // 3. Prepopulate Demands with custom dates
            val cal = Calendar.getInstance()

            // IMAGENS CIVIL (Opened 09/07/2026)
            cal.set(2026, Calendar.JULY, 9, 15, 0, 31)
            val timeCivil = cal.timeInMillis

            // verificar cdi do cidadão (Opened 06/07/2026)
            cal.set(2026, Calendar.JULY, 6, 10, 23, 35)
            val timeCdi = cal.timeInMillis

            // COMPUTDOR ESCOLA ARROJADO (Opened 20/05/2026) - Older than a month (+1 Mês / Aniversário)
            cal.set(2026, Calendar.MAY, 20, 11, 40, 12)
            val timeComputador = cal.timeInMillis

            val demands = listOf(
                Demand(
                    id = 889,
                    titulo = "IMAGENS CIVIL",
                    descricao = "PEGAR COM JOAO AS DOS PONTOS",
                    prioridade = "NORMAL",
                    status = "PENDENTE",
                    dataCriacao = timeCivil,
                    protocolo = "#000889"
                ),
                Demand(
                    id = 887,
                    titulo = "verificar cdi do cidadão",
                    descricao = "Verificar pendencias de cadastro no sistema CDI",
                    prioridade = "NORMAL",
                    status = "PENDENTE",
                    dataCriacao = timeCdi,
                    protocolo = "#000887"
                ),
                Demand(
                    id = 840,
                    titulo = "COMPUTDOR ESCOLA ARROJADO",
                    descricao = "Instalar sistema operacional, restaurar backup e atualizar todos os drivers",
                    prioridade = "NORMAL",
                    status = "PENDENTE",
                    dataCriacao = timeComputador,
                    protocolo = "#000840"
                )
            )
            demands.forEach { demandDao.insertDemand(it) }

            // Create many concluded demands to match the high volume shown in dashboard (Concluídos: 839)
            // We can pre-fill 12 mock concluded items and add metadata counts for scaling!
            for (i in 1..15) {
                cal.set(2026, Calendar.JUNE, i, 9, 0, 0)
                demandDao.insertDemand(
                    Demand(
                        id = i,
                        titulo = "Demanda Concluída #${100 + i}",
                        descricao = "Atendimento de rotina finalizado com sucesso.",
                        prioridade = if (i % 3 == 0) "ALTA" else if (i % 3 == 1) "NORMAL" else "SEM_PRIORIDADE",
                        status = "CONCLUIDO",
                        dataCriacao = cal.timeInMillis,
                        protocolo = String.format("#%06d", 100 + i)
                    )
                )
            }

            // 4. Prepopulate Audit Logs (from screenshot logs)
            val auditLogs = listOf(
                AuditLog(dataHora = timeCivil + 15000, executor = "Inacio", operacao = "LOGIN_SUCCESS", modulo = "auth #1", detalhes = "Login bem-sucedido (E-mail: ti@gmail.com)"),
                AuditLog(dataHora = timeCivil, executor = "Inacio", operacao = "CREATE_DEMAND", modulo = "demands #889", detalhes = "Criou demanda: IMAGENS CIVIL"),
                AuditLog(dataHora = timeCdi + 20000, executor = "Inacio", operacao = "UPDATE_DEMAND", modulo = "demands #887", detalhes = "Atualizou demanda: verificar cdi do cidadão"),
                AuditLog(dataHora = timeCdi, executor = "Luanzada", operacao = "LOGIN_SUCCESS", modulo = "auth #3", detalhes = "Login bem-sucedido (E-mail: piru@gmail.com)")
            )
            auditLogs.forEach { auditLogDao.insertAuditLog(it) }
        }
    }
}
