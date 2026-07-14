package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.data.api.SecureSessionManager
import com.example.data.api.WebSocketManager
import com.example.data.api.WebSocketEvent
import com.example.data.api.UserProfile
import com.example.ui.theme.isDarkThemeGlobal
import com.exemplo.tidemandas.network.*
import com.example.data.model.AuditLog
import com.example.data.model.Demand
import com.example.data.model.SyncSettings
import com.example.data.model.TechUser
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application, viewModelScope)
    private val repository = AppRepository(
        database.demandDao(),
        database.techUserDao(),
        database.auditLogDao(),
        database.syncSettingsDao()
    )

    // Secure Storage and API integration
    private val secureSessionManager = SecureSessionManager(application)
    private val webSocketManager = WebSocketManager()
    
    private val _isLoggedIn = MutableStateFlow(secureSessionManager.hasToken())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    fun getLastLoggedEmail(): String {
        return secureSessionManager.getLastEmail() ?: ""
    }

    private val _isLoggingIn = MutableStateFlow(false)
    val isLoggingIn: StateFlow<Boolean> = _isLoggingIn.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    // Real-time API Dashboard statistics (Requirement 2)
    private val _apiStats = MutableStateFlow<StatsResponse?>(null)
    val apiStats: StateFlow<StatsResponse?> = _apiStats.asStateFlow()

    private val _isLoadingDemands = MutableStateFlow(false)
    val isLoadingDemands: StateFlow<Boolean> = _isLoadingDemands.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val apiService: TiDemandaService = ApiClient.createService(secureSessionManager) {
        // Callback para erro 401 (Unauthorized) - Requirement 4
        _isLoggedIn.value = false
        _userProfile.value = null
        _loginError.value = "Sessão expirada. Faça login novamente."
    }

    init {
        // Carrega a preferência de tema salva localmente de imediato para feedback instantâneo
        val savedTheme = secureSessionManager.getTheme()
        if (savedTheme != null) {
            isDarkThemeGlobal = (savedTheme == "dark")
        }

        // Se já possui token salvo, tenta carregar o perfil do usuário automaticamente na rota protegida
        if (secureSessionManager.hasToken()) {
            fetchUserProfile()
            fetchDashboardStats()
            fetchDemandsFromApi()
            fetchAuditLogs()
            fetchTechUsers()
            connectWebSocket()
        }
    }

    fun performLogin(email: String, senha: String) {
        viewModelScope.launch {
            if (_isLoggingIn.value) return@launch
            _isLoggingIn.value = true
            _loginError.value = null
            
            try {
                // Tenta realizar a chamada HTTP POST para a rota de login pública
                val response = apiService.login(
                    LoginRequest(
                        email = email,
                        password = senha
                    )
                )
                
                val token = response.token
                if (token.isEmpty()) {
                    throw IllegalStateException("Token de autenticação não retornado pelo servidor.")
                }
                
                // Armazena com segurança o token recebido no corpo da resposta (Requirement 2)
                secureSessionManager.saveToken(token)
                
                // Salva o último e-mail logado
                secureSessionManager.saveLastEmail(email)
                
                _isLoggedIn.value = true
                
                // Carrega o perfil do usuário logo após o login bem-sucedido
                fetchUserProfile()
                fetchDashboardStats()
                fetchDemandsFromApi()
                fetchAuditLogs()
                fetchTechUsers()
                connectWebSocket()
                
                repository.insertAuditLog(
                    AuditLog(
                        executor = email.substringBefore("@").uppercase(),
                        operacao = "LOGIN_SUCCESS",
                        modulo = "auth",
                        detalhes = "Login realizado com sucesso via API corporativa."
                    )
                )
            } catch (e: retrofit2.HttpException) {
                val errorBody = e.response()?.errorBody()?.string()
                _loginError.value = "Erro no Servidor (${e.code()}): ${errorBody ?: e.message()}"
            } catch (e: Exception) {
                _loginError.value = "Falha ao realizar login: ${e.localizedMessage ?: "Erro de conexão"}"
            } finally {
                _isLoggingIn.value = false
            }
        }
    }

    fun refreshData() {
        if (!_isLoggedIn.value) return
        viewModelScope.launch {
            if (_isRefreshing.value) return@launch
            _isRefreshing.value = true
            try {
                fetchUserProfile()
                fetchDashboardStats()
                fetchDemandsFromApi()
                fetchAuditLogs()
                fetchTechUsers()
                connectWebSocket()
                kotlinx.coroutines.delay(800)
            } catch (e: Exception) {
                // Ignore
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    /**
     * Rota Protegida de Exemplo (Requirement 3)
     * Busca dados do perfil enviando o token JWT automaticamente no Header via Interceptor.
     */
    fun fetchUserProfile() {
        viewModelScope.launch {
            try {
                val response = apiService.getPerfil()
                if (response.success) {
                    val apiUser = response.user
                    val profile = UserProfile(
                        id = apiUser.id,
                        nome = apiUser.name,
                        email = apiUser.email,
                        nivel = apiUser.role.uppercase(),
                        ativo = apiUser.active == 1,
                        theme = apiUser.theme
                    )
                    _userProfile.value = profile
                    val userTheme = apiUser.theme
                    if (userTheme != null) {
                        val localTheme = secureSessionManager.getTheme()
                        if (localTheme == null) {
                            isDarkThemeGlobal = (userTheme == "dark")
                            secureSessionManager.saveTheme(userTheme)
                        } else {
                            isDarkThemeGlobal = (localTheme == "dark")
                        }
                    }
                }
            } catch (e: Exception) {
                // Caso ocorra 401, o interceptor limpa o token e chama o callback de desautorização automaticamente
                repository.insertAuditLog(
                    AuditLog(
                        executor = "SISTEMA",
                        operacao = "PROFILE_FETCH_FAILED",
                        modulo = "user_profile",
                        detalhes = "Erro ao buscar perfil protegido: ${e.localizedMessage}"
                    )
                )
            }
        }
    }

    /**
     * Atualiza o tema de preferência do usuário (Requirement E)
     */
    fun updateThemePreference(theme: String, onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            // Atualiza de imediato localmente para oferecer feedback instantâneo e robustez caso o servidor dê 500
            isDarkThemeGlobal = (theme == "dark")
            secureSessionManager.saveTheme(theme)
            _userProfile.value?.let { currentProfile ->
                _userProfile.value = currentProfile.copy(theme = theme)
            }
            
            try {
                val response = apiService.updateTheme(ThemeRequest(theme))
                if (response.success) {
                    onResult(true, "Tema alterado com sucesso para ${if (theme == "dark") "Escuro" else "Claro"}!")
                } else {
                    onResult(true, "Tema alterado com sucesso para ${if (theme == "dark") "Escuro" else "Claro"} (offline).")
                }
            } catch (e: Exception) {
                // Caso ocorra erro HTTP 500 ou problemas de rede, mantemos a alteração local com sucesso
                onResult(true, "Tema alterado com sucesso para ${if (theme == "dark") "Escuro" else "Claro"} (offline).")
            }
        }
    }

    /**
     * Atualiza as informações do usuário logado
     */
    fun updateLoggedUserProfile(name: String, email: String, onResult: (Boolean, String) -> Unit) {
        val currentProfile = _userProfile.value
        if (currentProfile == null) {
            onResult(false, "Nenhum usuário logado.")
            return
        }
        viewModelScope.launch {
            try {
                val editRequest = EditUserRequest(
                    name = name,
                    email = email,
                    role = currentProfile.nivel.lowercase(),
                    active = if (currentProfile.ativo) 1 else 0,
                    password = null
                )
                val updatedUser = apiService.updateUser(currentProfile.id, editRequest)
                _userProfile.value = currentProfile.copy(
                    nome = updatedUser.name,
                    email = updatedUser.email,
                    theme = updatedUser.theme
                )
                repository.insertAuditLog(
                    AuditLog(
                        executor = updatedUser.name,
                        operacao = "PROFILE_UPDATED",
                        modulo = "user_profile",
                        detalhes = "Informações do usuário atualizadas com sucesso."
                    )
                )
                onResult(true, "Informações atualizadas com sucesso!")
            } catch (e: Exception) {
                // Fallback local/offline para robustez caso o servidor retorne 403 Forbidden ou outro erro
                _userProfile.value = currentProfile.copy(
                    nome = name,
                    email = email
                )
                repository.insertAuditLog(
                    AuditLog(
                        executor = currentProfile.nome,
                        operacao = "PROFILE_UPDATED_LOCAL",
                        modulo = "user_profile",
                        detalhes = "Informações do perfil atualizadas localmente (erro do servidor: ${e.localizedMessage})."
                    )
                )
                onResult(true, "Informações atualizadas localmente!")
            }
        }
    }

    /**
     * Altera a própria senha do colaborador logado (Requirement F)
     */
    fun changeLoggedUserPassword(currentPass: String, newPass: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val response = apiService.changePassword(
                    ChangePasswordRequest(
                        currentPassword = currentPass,
                        newPassword = newPass
                    )
                )
                repository.insertAuditLog(
                    AuditLog(
                        executor = _userProfile.value?.nome ?: "Colaborador",
                        operacao = "PASSWORD_CHANGED",
                        modulo = "user_profile",
                        detalhes = "Senha alterada com sucesso."
                    )
                )
                onResult(true, response.message ?: "Senha alterada com sucesso!")
            } catch (e: retrofit2.HttpException) {
                val errorBody = e.response()?.errorBody()?.string()
                val errorMsg = try {
                    if (errorBody != null) {
                        if (errorBody.contains("\"error\"")) {
                            val regex = "\"error\"\\s*:\\s*\"([^\"]+)\"".toRegex()
                            regex.find(errorBody)?.groupValues?.get(1) ?: "Senha atual incorreta."
                        } else if (errorBody.contains("\"message\"")) {
                            val regex = "\"message\"\\s*:\\s*\"([^\"]+)\"".toRegex()
                            regex.find(errorBody)?.groupValues?.get(1) ?: "Erro ao alterar senha."
                        } else {
                            "Senha atual incorreta."
                        }
                    } else {
                        "Senha atual incorreta."
                    }
                } catch (ex: Exception) {
                    "Senha atual incorreta ou erro de validação."
                }
                onResult(false, errorMsg)
            } catch (e: Exception) {
                onResult(false, "Erro de rede: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Efetua o Logout limpando o token seguro.
     */
    fun performLogout() {
        viewModelScope.launch {
            val email = _userProfile.value?.email ?: "Usuário"
            webSocketManager.disconnect()
            secureSessionManager.clearToken()
            _isLoggedIn.value = false
            _userProfile.value = null
            _loginError.value = null
            _apiStats.value = null
            
            repository.insertAuditLog(
                AuditLog(
                    executor = email.substringBefore("@").uppercase(),
                    operacao = "LOGOUT",
                    modulo = "auth",
                    detalhes = "Sessão encerrada voluntariamente pelo usuário."
                )
            )
        }
    }

    fun fetchDashboardStats() {
        viewModelScope.launch {
            if (!_isLoggedIn.value) return@launch
            try {
                val stats = apiService.getStats()
                _apiStats.value = stats
            } catch (e: Exception) {
                // Ignore or log error
            }
        }
    }

    fun fetchDemandsFromApi(query: String? = null) {
        if (!_isLoggedIn.value) return
        viewModelScope.launch {
            _isLoadingDemands.value = true
            try {
                val fetched = apiService.getDemands(query)
                val localDemands = fetched.map { apiDemand ->
                    Demand(
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
                }
                repository.clearAndInsertDemands(localDemands)
            } catch (e: Exception) {
                // Ignore
            } finally {
                _isLoadingDemands.value = false
            }
        }
    }

    fun fetchAuditLogs() {
        if (!_isLoggedIn.value) return
        viewModelScope.launch {
            try {
                val logs = apiService.getAuditLogs()
                val dbLogs = logs.map { log ->
                    com.example.data.model.AuditLog(
                        id = log.id,
                        dataHora = parseIsoDate(log.created_at),
                        executor = log.user_name ?: "SISTEMA",
                        operacao = log.action,
                        modulo = log.module ?: if (!log.target_type.isNullOrBlank()) "${log.target_type} #${log.target_id ?: ""}" else "",
                        detalhes = log.details ?: ""
                    )
                }
                repository.clearAndInsertAuditLogs(dbLogs)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun fetchTechUsers() {
        if (!_isLoggedIn.value) return
        viewModelScope.launch {
            try {
                val apiUsers = apiService.getUsers()
                val techUsers = apiUsers.map { apiUser ->
                    TechUser(
                        id = apiUser.id,
                        nome = apiUser.name,
                        email = apiUser.email,
                        nivel = apiUser.role.uppercase(),
                        ativo = apiUser.active == 1
                    )
                }
                repository.clearAndInsertTechUsers(techUsers)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    // Current screen state
    private val _currentTab = MutableStateFlow("demandas") // Default to demands tab
    val currentTab: StateFlow<String> = _currentTab.asStateFlow()

    // Search and filter states
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedPriorityFilter = MutableStateFlow<String?>("NORMAL") // Match normal active filter in screenshot
    val selectedPriorityFilter: StateFlow<String?> = _selectedPriorityFilter.asStateFlow()

    private val _selectedStatusTab = MutableStateFlow("PENDENTES") // "PENDENTES" or "CONCLUIDOS"
    val selectedStatusTab: StateFlow<String> = _selectedStatusTab.asStateFlow()

    // Connection & Sync States
    private val _syncLogs = MutableStateFlow<List<String>>(emptyList())
    val syncLogs: StateFlow<List<String>> = _syncLogs.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncSuccess = MutableStateFlow<String?>(null)
    val syncSuccess: StateFlow<String?> = _syncSuccess.asStateFlow()

    // Observed databases flows
    val demands: StateFlow<List<Demand>> = repository.allDemandsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val techUsers: StateFlow<List<TechUser>> = repository.allTechUsersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val auditLogs: StateFlow<List<AuditLog>> = repository.allAuditLogsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val syncSettings: StateFlow<SyncSettings> = repository.syncSettingsFlow
        .map { it ?: SyncSettings() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SyncSettings())

    // Active View / Edit States (Dialog Controls)
    private val _viewingDemand = MutableStateFlow<Demand?>(null)
    val viewingDemand: StateFlow<Demand?> = _viewingDemand.asStateFlow()

    private val _editingDemand = MutableStateFlow<Demand?>(null)
    val editingDemand: StateFlow<Demand?> = _editingDemand.asStateFlow()

    private val _isCreatingDemand = MutableStateFlow(false)
    val isCreatingDemand: StateFlow<Boolean> = _isCreatingDemand.asStateFlow()

    private val _editingTechUser = MutableStateFlow<TechUser?>(null)
    val editingTechUser: StateFlow<TechUser?> = _editingTechUser.asStateFlow()

    private val _isCreatingTechUser = MutableStateFlow(false)
    val isCreatingTechUser: StateFlow<Boolean> = _isCreatingTechUser.asStateFlow()

    // Filtered demands
    val filteredDemands: StateFlow<List<Demand>> = combine(
        demands,
        searchQuery,
        selectedPriorityFilter,
        selectedStatusTab
    ) { demandList, query, priority, statusTab ->
        demandList.filter { demand ->
            val matchesQuery = query.isEmpty() || 
                demand.titulo.contains(query, ignoreCase = true) || 
                demand.descricao.contains(query, ignoreCase = true) || 
                demand.protocolo.contains(query, ignoreCase = true)

            val matchesPriority = priority == null || demand.prioridade == priority

            val matchesStatus = if (statusTab == "PENDENTES") {
                demand.status == "PENDENTE"
            } else {
                demand.status == "CONCLUIDO"
            }

            matchesQuery && matchesPriority && matchesStatus
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectTab(tab: String) {
        _currentTab.value = tab
        if (_isLoggedIn.value) {
            when (tab) {
                "painel" -> {
                    fetchDashboardStats()
                    fetchDemandsFromApi()
                }
                "demandas" -> {
                    fetchDemandsFromApi()
                }
                "usuarios" -> {
                    fetchTechUsers()
                }
                "auditoria" -> {
                    fetchAuditLogs()
                }
                "ajustes" -> {
                    fetchUserProfile()
                }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setPriorityFilter(priority: String?) {
        _selectedPriorityFilter.value = priority
    }

    fun setStatusTab(status: String) {
        _selectedStatusTab.value = status
    }

    // Dialog trigger methods
    fun openViewDemand(demand: Demand) {
        _viewingDemand.value = demand
    }

    fun closeViewDemand() {
        _viewingDemand.value = null
    }

    fun openCreateDemand() {
        _isCreatingDemand.value = true
        resetUploadState()
    }

    fun closeCreateDemand() {
        _isCreatingDemand.value = false
        resetUploadState()
    }

    fun openEditDemand(demand: Demand) {
        _editingDemand.value = demand
        initUploadFromDemand(demand)
    }

    fun closeEditDemand() {
        _editingDemand.value = null
        resetUploadState()
    }

    fun initUploadFromDemand(demand: Demand) {
        val url = demand.anexoUrl
        if (!url.isNullOrBlank()) {
            val fileName = url.substringAfterLast("/")
            setUploadSuccess(url, fileName)
        } else {
            resetUploadState()
        }
    }

    fun openCreateTechUser() {
        _isCreatingTechUser.value = true
    }

    fun closeCreateTechUser() {
        _isCreatingTechUser.value = false
    }

    fun openEditTechUser(user: TechUser) {
        _editingTechUser.value = user
    }

    fun closeEditTechUser() {
        _editingTechUser.value = null
    }

    // CRUD - Demands
    fun createDemand(titulo: String, descricao: String, prioridade: String, anexoUrl: String? = null, anexoNome: String? = null) {
        viewModelScope.launch {
            if (_isLoggedIn.value) {
                try {
                    val priorityInt = when (prioridade.uppercase()) {
                        "ALTA" -> 2
                        "NORMAL" -> 1
                        else -> 0
                    }
                    val apiResponse = apiService.createDemand(
                        CreateDemandRequest(
                            name = titulo,
                            description = descricao,
                            priority = priorityInt,
                            files = if (!anexoUrl.isNullOrBlank()) listOf(DemandFileRequest(name = anexoNome ?: "Anexo", url = anexoUrl)) else null
                        )
                    )
                    val localDemand = Demand(
                        id = apiResponse.id,
                        titulo = apiResponse.name,
                        descricao = apiResponse.description ?: "",
                        prioridade = when (apiResponse.priority) {
                            2 -> "ALTA"
                            1 -> "NORMAL"
                            else -> "SEM_PRIORIDADE"
                        },
                        status = if (apiResponse.done == 1) "CONCLUIDO" else "PENDENTE",
                        anexoUrl = apiResponse.file_url,
                        protocolo = "#${String.format("%06d", apiResponse.id)}",
                        filesJson = apiResponse.files
                    )
                    repository.insertDemand(localDemand, _userProfile.value?.nome ?: "Inacio")
                    // Refresh stats and list
                    fetchDashboardStats()
                    fetchDemandsFromApi()
                } catch (e: Exception) {
                    // local fallback on network error
                    val randomProtocol = String.format("#%06d", (100..99999).random())
                    val newDemand = Demand(
                        titulo = titulo,
                        descricao = descricao,
                        prioridade = prioridade,
                        status = "PENDENTE",
                        protocolo = randomProtocol,
                        anexoUrl = anexoUrl,
                        filesJson = if (!anexoUrl.isNullOrBlank()) "[{\"name\":\"${anexoNome ?: "Anexo"}\",\"url\":\"$anexoUrl\"}]" else null
                    )
                    repository.insertDemand(newDemand, _userProfile.value?.nome ?: "Inacio")
                }
            } else {
                val randomProtocol = String.format("#%06d", (100..99999).random())
                val newDemand = Demand(
                    titulo = titulo,
                    descricao = descricao,
                    prioridade = prioridade,
                    status = "PENDENTE",
                    protocolo = randomProtocol,
                    anexoUrl = anexoUrl,
                    filesJson = if (!anexoUrl.isNullOrBlank()) "[{\"name\":\"${anexoNome ?: "Anexo"}\",\"url\":\"$anexoUrl\"}]" else null
                )
                repository.insertDemand(newDemand, _userProfile.value?.nome ?: "Inacio")
            }
            closeCreateDemand()
        }
    }

    fun updateDemand(demand: Demand) {
        viewModelScope.launch {
            if (_isLoggedIn.value && demand.id > 0) {
                try {
                    val priorityInt = when (demand.prioridade.uppercase()) {
                        "ALTA" -> 2
                        "NORMAL" -> 1
                        else -> 0
                    }
                    apiService.updateDemand(
                        id = demand.id,
                        demand = CreateDemandRequest(
                            name = demand.titulo,
                            description = demand.descricao,
                            priority = priorityInt,
                            files = if (!demand.anexoUrl.isNullOrBlank()) listOf(DemandFileRequest(name = "Anexo", url = demand.anexoUrl)) else null
                        )
                    )
                } catch (e: Exception) {
                    // Ignora erro de rede e atualiza localmente
                }
            }
            repository.updateDemand(demand, _userProfile.value?.nome ?: "Inacio")
            closeEditDemand()
            // Refresh stats and list
            fetchDashboardStats()
            fetchDemandsFromApi()
        }
    }

    fun deleteDemand(demand: Demand) {
        viewModelScope.launch {
            if (_isLoggedIn.value && demand.id > 0) {
                try {
                    apiService.deleteDemand(id = demand.id)
                } catch (e: Exception) {
                    // Ignora erro de rede e remove localmente
                }
            }
            repository.deleteDemand(demand, _userProfile.value?.nome ?: "Inacio")
            // Refresh stats and list
            fetchDashboardStats()
            fetchDemandsFromApi()
        }
    }

    fun reorderDemands(ordersList: List<com.exemplo.tidemandas.network.ReorderItem>) {
        viewModelScope.launch {
            if (_isLoggedIn.value) {
                try {
                    // Update locally first for maximum responsiveness
                    val currentDemands = demands.value.toMutableList()
                    ordersList.forEach { order ->
                        val idx = currentDemands.indexOfFirst { it.id == order.id }
                        if (idx != -1) {
                            currentDemands[idx] = currentDemands[idx].copy(sortOrder = order.sort_order)
                        }
                    }
                    // Sort local list and persist locally
                    currentDemands.sortBy { it.sortOrder }
                    repository.clearAndInsertDemands(currentDemands)

                    // Call backend to persist
                    apiService.reorderDemands(
                        com.exemplo.tidemandas.network.ReorderDemandsRequest(orders = ordersList)
                    )

                    // Local Audit Log
                    repository.insertAuditLog(
                        com.example.data.model.AuditLog(
                            executor = _userProfile.value?.nome ?: "Suporte Local",
                            operacao = "REORDER_DEMANDS",
                            modulo = "demands",
                            detalhes = "Reordenou a lista de chamados no painel (${ordersList.size} itens reordenados)."
                        )
                    )
                } catch (e: Exception) {
                    // Refresh from API in case of network or sync failure
                    fetchDemandsFromApi()
                }
            }
        }
    }

    fun toggleDemandStatus(demand: Demand) {
        viewModelScope.launch {
            val updated = demand.copy(
                status = if (demand.status == "PENDENTE") "CONCLUIDO" else "PENDENTE"
            )
            if (_isLoggedIn.value && demand.id > 0) {
                // 1. Tenta atualizar pelo endpoint específico de status com os campos redundantes
                try {
                    apiService.updateDemandStatus(
                        id = demand.id,
                        request = StatusRequest(
                            status = updated.status.lowercase(),
                            done = if (updated.status == "CONCLUIDO") 1 else 0,
                            isDone = updated.status == "CONCLUIDO"
                        )
                    )
                } catch (e: Exception) {
                    android.util.Log.e("MainViewModel", "Erro ao atualizar status via endpoint /status", e)
                }

                // 2. Tenta atualizar também via endpoint geral de demanda (PUT /demands/{id})
                try {
                    val priorityInt = when (updated.prioridade.uppercase()) {
                        "ALTA" -> 2
                        "NORMAL" -> 1
                        else -> 0
                    }
                    apiService.updateDemand(
                        id = updated.id,
                        demand = CreateDemandRequest(
                            name = updated.titulo,
                            description = updated.descricao,
                            priority = priorityInt,
                            files = if (!updated.anexoUrl.isNullOrBlank()) listOf(DemandFileRequest(name = "Anexo", url = updated.anexoUrl)) else null,
                            status = updated.status.lowercase(),
                            done = if (updated.status == "CONCLUIDO") 1 else 0
                        )
                    )
                } catch (e: Exception) {
                    android.util.Log.e("MainViewModel", "Erro ao atualizar status via endpoint geral", e)
                }
            }
            repository.updateDemand(updated, _userProfile.value?.nome ?: "Inacio")
            
            // Refresh stats and list
            fetchDashboardStats()
            fetchDemandsFromApi()
            
            // Log this specific action to audit logs
            repository.insertAuditLog(
                AuditLog(
                    executor = _userProfile.value?.nome ?: "Inacio",
                    operacao = "UPDATE_DEMAND",
                    modulo = "demands #${demand.id}",
                    detalhes = "Alterou status de '${demand.titulo}' para ${updated.status}"
                )
            )
        }
    }

    // CRUD - Tech Users
    fun createTechUser(nome: String, email: String, nivel: String, ativo: Boolean, senha: String) {
        viewModelScope.launch {
            if (_isLoggedIn.value) {
                try {
                    val apiResponse = apiService.createUser(
                        CreateUserRequest(
                            name = nome,
                            email = email,
                            password = senha,
                            role = nivel.lowercase()
                        )
                    )
                    val newUser = TechUser(
                        id = apiResponse.id,
                        nome = apiResponse.name,
                        email = apiResponse.email,
                        nivel = apiResponse.role.uppercase(),
                        ativo = apiResponse.active == 1
                    )
                    repository.insertTechUser(newUser, _userProfile.value?.nome ?: "Inacio")
                    // Refresh users
                    runSincronizacao()
                } catch (e: Exception) {
                    // local fallback
                    val newUser = TechUser(
                        nome = nome,
                        email = email,
                        nivel = nivel,
                        ativo = ativo
                    )
                    repository.insertTechUser(newUser, _userProfile.value?.nome ?: "Inacio")
                }
            } else {
                val newUser = TechUser(
                    nome = nome,
                    email = email,
                    nivel = nivel,
                    ativo = ativo
                )
                repository.insertTechUser(newUser, _userProfile.value?.nome ?: "Inacio")
            }
            closeCreateTechUser()
        }
    }

    fun updateTechUser(user: TechUser, senha: String? = null) {
        viewModelScope.launch {
            if (_isLoggedIn.value && user.id > 0) {
                try {
                    val apiResponse = apiService.updateUser(
                        id = user.id,
                        request = com.exemplo.tidemandas.network.EditUserRequest(
                            name = user.nome,
                            email = user.email,
                            role = user.nivel.lowercase(),
                            active = if (user.ativo) 1 else 0,
                            password = if (senha.isNullOrBlank()) null else senha
                        )
                    )
                    val updatedUser = user.copy(
                        nome = apiResponse.name,
                        email = apiResponse.email,
                        nivel = apiResponse.role.uppercase(),
                        ativo = apiResponse.active == 1
                    )
                    repository.updateTechUser(updatedUser, _userProfile.value?.nome ?: "Inacio")
                } catch (e: Exception) {
                    // Ignora erro de rede
                    repository.updateTechUser(user, _userProfile.value?.nome ?: "Inacio")
                }
            } else {
                repository.updateTechUser(user, _userProfile.value?.nome ?: "Inacio")
            }
            closeEditTechUser()
            // Refresh users
            runSincronizacao()
        }
    }

    fun deleteTechUser(user: TechUser) {
        viewModelScope.launch {
            if (_isLoggedIn.value && user.id > 0) {
                try {
                    apiService.deleteUser(id = user.id)
                } catch (e: Exception) {
                    // Ignora erro de rede
                }
            }
            repository.deleteTechUser(user, _userProfile.value?.nome ?: "Inacio")
            // Refresh users
            runSincronizacao()
        }
    }

    // Sync Actions
    fun updateSyncSettings(settings: SyncSettings) {
        viewModelScope.launch {
            repository.saveSyncSettings(settings, _userProfile.value?.nome ?: "Inacio")
        }
    }

    fun runSincronizacao() {
        viewModelScope.launch {
            if (_isSyncing.value) return@launch
            _isSyncing.value = true
            _syncLogs.value = emptyList()
            _syncSuccess.value = null

            val result = repository.performSync(apiService, syncSettings.value) { step ->
                _syncLogs.value = _syncLogs.value + step
            }

            _isSyncing.value = false
            if (result.isSuccess) {
                _syncSuccess.value = "Sincronização realizada!"
            } else {
                _syncSuccess.value = "Erro: Sincronização falhou."
            }
        }
    }

    fun clearSyncNotification() {
        _syncSuccess.value = null
    }

    // Attachment Upload & Delete Support
    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Idle)
    val uploadState: StateFlow<UploadState> = _uploadState.asStateFlow()

    fun uploadFile(uri: android.net.Uri) {
        viewModelScope.launch {
            _uploadState.value = UploadState.Loading
            try {
                val context = getApplication<Application>().applicationContext
                val contentResolver = context.contentResolver
                
                var fileName = "file"
                val cursor = contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            fileName = it.getString(nameIndex)
                        }
                    }
                }

                val inputStream = contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    _uploadState.value = UploadState.Error("Não foi possível abrir o arquivo.")
                    return@launch
                }

                val bytes = inputStream.readBytes()
                val requestFile = okhttp3.RequestBody.create(
                    (contentResolver.getType(uri) ?: "application/octet-stream").toMediaTypeOrNull(),
                    bytes
                )
                val body = okhttp3.MultipartBody.Part.createFormData("file", fileName, requestFile)

                val response = apiService.uploadFile(body)
                _uploadState.value = UploadState.Success(response.fileUrl, response.fileName)
            } catch (e: Exception) {
                _uploadState.value = UploadState.Error(e.message ?: "Erro desconhecido ao fazer upload.")
            }
        }
    }

    fun deleteUploadedFile(fileUrl: String) {
        viewModelScope.launch {
            try {
                apiService.deleteFile(DeleteUploadRequest(fileUrl))
                _uploadState.value = UploadState.Idle
            } catch (e: Exception) {
                // Ignore or log
            }
        }
    }
    
    fun resetUploadState() {
        _uploadState.value = UploadState.Idle
    }

    fun setUploadSuccess(fileUrl: String, fileName: String) {
        _uploadState.value = UploadState.Success(fileUrl, fileName)
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

    private var webSocketJob: kotlinx.coroutines.Job? = null

    private fun connectWebSocket() {
        val token = secureSessionManager.getToken() ?: return
        webSocketManager.connect(token)
        
        webSocketJob?.cancel()
        webSocketJob = viewModelScope.launch {
            webSocketManager.events.collect { event ->
                fetchDemandsFromApi()
                fetchDashboardStats()
                fetchAuditLogs()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        webSocketManager.disconnect()
    }
}

sealed interface UploadState {
    object Idle : UploadState
    object Loading : UploadState
    data class Success(val fileUrl: String, val fileName: String) : UploadState
    data class Error(val message: String) : UploadState
}

class MainViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
