package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.AuditLog
import com.example.data.model.Demand
import com.example.data.model.SyncSettings
import com.example.data.model.TechUser
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun TIDemandasApp(
    viewModel: MainViewModel = viewModel()
) {
    val currentTab by viewModel.currentTab.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncSuccess by viewModel.syncSuccess.collectAsState()

    // Dialog state collectors
    val viewingDemand by viewModel.viewingDemand.collectAsState()
    val editingDemand by viewModel.editingDemand.collectAsState()
    val isCreatingDemand by viewModel.isCreatingDemand.collectAsState()
    
    val editingTechUser by viewModel.editingTechUser.collectAsState()
    val isCreatingTechUser by viewModel.isCreatingTechUser.collectAsState()
    val uploadState by viewModel.uploadState.collectAsState()

    var confirmDeleteState by remember { mutableStateOf<ConfirmDeleteState?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Clear sync notifications with toast style
    LaunchedEffect(syncSuccess) {
        syncSuccess?.let { message ->
            scope.launch {
                snackbarHostState.showSnackbar(message)
                viewModel.clearSyncNotification()
            }
        }
    }

    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()

    if (!isLoggedIn) {
        LoginScreen(viewModel = viewModel)
    } else {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(BgDark)
                .statusBarsPadding()
                .navigationBarsPadding(),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopHeaderBar(
                    isSyncing = isSyncing,
                    userName = userProfile?.nome ?: "Inacio",
                    onSyncClick = { viewModel.runSincronizacao() }
                )
            },
            bottomBar = {
                BottomNavBar(
                    currentTab = currentTab,
                    onTabSelected = { viewModel.selectTab(it) }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(BgDark)
            ) {
                when (currentTab) {
                    "painel" -> PainelScreen(viewModel)
                    "demandas" -> DemandasScreen(viewModel, onDeleteDemand = { confirmDeleteState = ConfirmDeleteState.DeleteDemand(it) })
                    "usuarios" -> UsuariosScreen(viewModel)
                    "auditoria" -> AuditoriaScreen(viewModel)
                    "ajustes" -> AjustesScreen(viewModel)
                }
            }
        }

        // Modal Overlays
        viewingDemand?.let { demand ->
            DetalhesChamadoDialog(
                demand = demand,
                onDismiss = { viewModel.closeViewDemand() },
                onToggleStatus = { viewModel.toggleDemandStatus(demand) }
            )
        }

        editingDemand?.let { demand ->
            EditarChamadoDialog(
                demand = demand,
                onDismiss = { viewModel.closeEditDemand() },
                uploadState = uploadState,
                onUploadFile = { uri -> viewModel.uploadFile(uri) },
                onDeleteUploadedFile = { url -> confirmDeleteState = ConfirmDeleteState.DeleteAttachment(url) },
                onResetUpload = { viewModel.resetUploadState() },
                onSave = { updated -> viewModel.updateDemand(updated) }
            )
        }

        if (isCreatingDemand) {
            RegistrarChamadoDialog(
                onDismiss = { viewModel.closeCreateDemand() },
                uploadState = uploadState,
                onUploadFile = { uri -> viewModel.uploadFile(uri) },
                onDeleteUploadedFile = { url -> confirmDeleteState = ConfirmDeleteState.DeleteAttachment(url) },
                onResetUpload = { viewModel.resetUploadState() },
                onSave = { titulo, desc, prio, url, name -> viewModel.createDemand(titulo, desc, prio, url, name) }
            )
        }

        editingTechUser?.let { user ->
            EditarUsuarioDialog(
                user = user,
                onDismiss = { viewModel.closeEditTechUser() },
                onSave = { updated, senha -> viewModel.updateTechUser(updated, senha) },
                onDelete = {
                    confirmDeleteState = ConfirmDeleteState.DeleteTechUser(user)
                    viewModel.closeEditTechUser()
                }
            )
        }

        if (isCreatingTechUser) {
            NovoUsuarioDialog(
                onDismiss = { viewModel.closeCreateTechUser() },
                onSave = { nome, email, nivel, ativo, senha -> viewModel.createTechUser(nome, email, nivel, ativo, senha) }
            )
        }

        confirmDeleteState?.let { state ->
            ConfirmDeleteDialog(
                state = state,
                onConfirm = {
                    when (state) {
                        is ConfirmDeleteState.DeleteDemand -> {
                            viewModel.deleteDemand(state.demand)
                        }
                        is ConfirmDeleteState.DeleteTechUser -> {
                            viewModel.deleteTechUser(state.user)
                        }
                        is ConfirmDeleteState.DeleteAttachment -> {
                            viewModel.deleteUploadedFile(state.fileUrl)
                        }
                    }
                    confirmDeleteState = null
                },
                onDismiss = { confirmDeleteState = null }
            )
        }
    }
}

@Composable
fun TopHeaderBar(
    isSyncing: Boolean,
    userName: String = "Inacio",
    onSyncClick: () -> Unit
) {
    Surface(
        color = SurfaceDark,
        border = BorderStroke(1.dp, Color(0xFF1E293B)),
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Brand Logo matching screenshot (Blue clipboard square + title)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(BlueAccent, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Assignment,
                        contentDescription = "Clipboard Logo",
                        tint = TextWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "TI DEMANDAS",
                        color = TextWhite,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Gestão de chamados",
                        color = TextGray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Sync pill and User avatar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // SINC. ATIVA pill mimicking web dashboard
                Surface(
                    onClick = onSyncClick,
                    color = if (isSyncing) Color(0xFF1D4ED8).copy(alpha = 0.2f) else Color(0xFF10B981).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(
                        1.dp, 
                        if (isSyncing) Color(0xFF3B82F6).copy(alpha = 0.5f) else Color(0xFF10B981).copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.height(30.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(
                                    if (isSyncing) Color(0xFF3B82F6) else Color(0xFF10B981), 
                                    CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isSyncing) "SINC. ATIVA" else "SINC. ONLINE",
                            color = if (isSyncing) Color(0xFF60A5FA) else Color(0xFF34D399),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Mini Profile Avatar Badge
                Row(
                    modifier = Modifier
                        .background(Color(0xFF161E36), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(BlueAccent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (userName.isNotEmpty()) userName.take(1).uppercase() else "U",
                            color = TextWhite,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = userName.uppercase(),
                        color = TextWhite,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun BottomNavBar(
    currentTab: String,
    onTabSelected: (String) -> Unit
) {
    NavigationBar(
        containerColor = SurfaceDark,
        tonalElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, Color(0xFF1E293B)), RoundedCornerShape(0.dp))
    ) {
        val navItems = listOf(
            Triple("painel", "Painel", Icons.Default.Dashboard),
            Triple("demandas", "Demandas", Icons.AutoMirrored.Filled.ListAlt),
            Triple("usuarios", "Equipe", Icons.Default.Group),
            Triple("auditoria", "Auditoria", Icons.Default.Shield),
            Triple("ajustes", "Ajustes", Icons.Default.Settings)
        )

        navItems.forEach { (route, label, icon) ->
            NavigationBarItem(
                selected = currentTab == route,
                onClick = { onTabSelected(route) },
                icon = { Icon(imageVector = icon, contentDescription = label) },
                label = { Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = TextWhite,
                    selectedTextColor = TextWhite,
                    indicatorColor = BlueAccent,
                    unselectedIconColor = TextGray,
                    unselectedTextColor = TextGray
                )
            )
        }
    }
}

// -------------------------------------------------------------
// SCREEN 1: PAINEL / DASHBOARD
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PainelScreen(viewModel: MainViewModel) {
    val demands by viewModel.demands.collectAsState()
    val apiStats by viewModel.apiStats.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    
    val total = apiStats?.summary?.total ?: demands.size
    val completed = apiStats?.summary?.completed ?: demands.count { it.status == "CONCLUIDO" }
    val pending = apiStats?.summary?.pending ?: demands.count { it.status == "PENDENTE" }
    
    val highPrioPending = apiStats?.summary?.high_pending ?: apiStats?.summary?.high ?: demands.count { it.status == "PENDENTE" && it.prioridade == "ALTA" }
    val normalPrioPending = apiStats?.summary?.normal_pending ?: apiStats?.summary?.normal ?: demands.count { it.status == "PENDENTE" && it.prioridade == "NORMAL" }
    val lowPrioPending = apiStats?.summary?.none_pending ?: apiStats?.summary?.none ?: demands.count { it.status == "PENDENTE" && it.prioridade == "SEM_PRIORIDADE" }

    val successRate = if (total > 0) (completed.toFloat() / total * 100) else 100f

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refreshData() },
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        item {
            Column {
                Text(
                    text = "Painel Executivo",
                    color = TextWhite,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Acompanhamento geral de métricas de TI",
                    color = TextGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Stats grid in 3 rows on compact mobile, or elegant grid rows
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    DashboardStatCard(
                        title = "TOTAL",
                        value = "$total",
                        color = Color(0xFF1E69FF),
                        subtitle = "Chamados abertos",
                        icon = Icons.Default.BarChart,
                        onClick = {
                            viewModel.setPriorityFilter(null)
                            viewModel.setStatusTab("PENDENTES")
                            viewModel.selectTab("demandas")
                        },
                        modifier = Modifier.weight(1f)
                    )
                    DashboardStatCard(
                        title = "CONCLUÍDOS",
                        value = "$completed",
                        color = StatusCompleted,
                        subtitle = String.format("%.1f%% de sucesso", successRate),
                        icon = Icons.Default.CheckCircle,
                        onClick = {
                            viewModel.setPriorityFilter(null)
                            viewModel.setStatusTab("CONCLUIDOS")
                            viewModel.selectTab("demandas")
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    DashboardStatCard(
                        title = "PENDENTES",
                        value = "$pending",
                        color = StatusPending,
                        subtitle = "Em atendimento",
                        icon = Icons.Default.HourglassEmpty,
                        onClick = {
                            viewModel.setPriorityFilter(null)
                            viewModel.setStatusTab("PENDENTES")
                            viewModel.selectTab("demandas")
                        },
                        modifier = Modifier.weight(1f)
                    )
                    DashboardStatCard(
                        title = "ALTA PRIO",
                        value = "$highPrioPending",
                        color = PriorityHigh,
                        subtitle = "Críticos ativos",
                        icon = Icons.Default.Warning,
                        onClick = {
                            viewModel.setPriorityFilter("ALTA")
                            viewModel.setStatusTab("PENDENTES")
                            viewModel.selectTab("demandas")
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    DashboardStatCard(
                        title = "NORMAL",
                        value = "$normalPrioPending",
                        color = PriorityNormal,
                        subtitle = "Chamados padrão",
                        icon = Icons.Default.Assignment,
                        onClick = {
                            viewModel.setPriorityFilter("NORMAL")
                            viewModel.setStatusTab("PENDENTES")
                            viewModel.selectTab("demandas")
                        },
                        modifier = Modifier.weight(1f)
                    )
                    DashboardStatCard(
                        title = "SEM PRIO",
                        value = "$lowPrioPending",
                        color = PriorityLow,
                        subtitle = "Chamados baixos",
                        icon = Icons.Default.Info,
                        onClick = {
                            viewModel.setPriorityFilter("SEM_PRIORIDADE")
                            viewModel.setStatusTab("PENDENTES")
                            viewModel.selectTab("demandas")
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Charts Section
        item {
            StatusDonutChart(
                completedCount = completed,
                pendingCount = pending
            )
        }

        item {
            PriorityBarChart(
                highCount = highPrioPending,
                normalCount = normalPrioPending,
                lowCount = lowPrioPending
            )
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
    }
}

@Composable
fun DashboardStatCard(
    title: String,
    value: String,
    color: Color,
    subtitle: String,
    icon: ImageVector,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFF1E293B)),
        modifier = if (onClick != null) {
            modifier.clickable(onClick = onClick)
        } else {
            modifier
        }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = TextGray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(color.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                color = TextWhite,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black
            )

            Text(
                text = subtitle,
                color = TextGray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// -------------------------------------------------------------
// SCREEN 2: DEMANDAS / CHAMADOS LISTING
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DemandasScreen(viewModel: MainViewModel, onDeleteDemand: (Demand) -> Unit) {
    val filteredDemands by viewModel.filteredDemands.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedPriorityFilter by viewModel.selectedPriorityFilter.collectAsState()
    val selectedStatusTab by viewModel.selectedStatusTab.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refreshData() },
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
        // Title block + New ticket button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "TI Demandas",
                    color = TextWhite,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Gestão de chamados técnicos de TI",
                    color = TextGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Button(
                onClick = { viewModel.openCreateDemand() },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BlueAccent, contentColor = TextWhite),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                modifier = Modifier
                    .height(38.dp)
                    .testTag("nova_demanda_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Nova Demanda", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Search Bar (Input matching web visual)
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            placeholder = { Text("Buscar chamado...", color = Color(0xFF475569)) },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = TextGray) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("buscar_chamado_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextWhite,
                unfocusedTextColor = TextWhite,
                focusedBorderColor = BlueAccent,
                unfocusedBorderColor = Color(0xFF1E293B),
                focusedContainerColor = SurfaceDark,
                unfocusedContainerColor = SurfaceDark
            ),
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Priority Filter Dropdown/Chips Mimicry
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("PRIORIDADE:", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)

            val priorities = listOf("ALTA", "NORMAL", "SEM_PRIORIDADE", null)
            priorities.forEach { prio ->
                val label = prio ?: "TODOS"
                val isSelected = selectedPriorityFilter == prio
                
                Surface(
                    onClick = { viewModel.setPriorityFilter(prio) },
                    color = if (isSelected) BlueAccent else Color(0xFF161E36),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, if (isSelected) BlueAccent else Color(0xFF1E293B)),
                    modifier = Modifier.height(28.dp)
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) TextWhite else TextGray,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Active filter pill indicator
        selectedPriorityFilter?.let { activeFilter ->
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .background(Color(0xFFEF4444).copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                    .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .clickable { viewModel.setPriorityFilter(null) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "FILTRO ATIVO: $activeFilter",
                    color = Color(0xFFF87171),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = Color(0xFFF87171),
                    modifier = Modifier.size(10.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Tab state header: PENDENTES (3) vs CONCLUÍDOS (819)
        val demandsListTotal by viewModel.demands.collectAsState()
        val apiStats by viewModel.apiStats.collectAsState()
        val totalPending = apiStats?.summary?.pending ?: demandsListTotal.count { it.status == "PENDENTE" }
        val totalCompleted = apiStats?.summary?.completed ?: demandsListTotal.count { it.status == "CONCLUIDO" }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, Color(0xFF1E293B)), RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceDark)
        ) {
            TabHeaderButton(
                label = "PENDENTES ($totalPending)",
                isSelected = selectedStatusTab == "PENDENTES",
                onClick = { viewModel.setStatusTab("PENDENTES") },
                modifier = Modifier.weight(1f)
            )
            TabHeaderButton(
                label = "CONCLUÍDOS ($totalCompleted)",
                isSelected = selectedStatusTab == "CONCLUIDOS",
                onClick = { viewModel.setStatusTab("CONCLUIDOS") },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        var isReorderMode by remember { mutableStateOf(false) }
        val localDemands = remember(filteredDemands) { mutableStateListOf<Demand>().apply { addAll(filteredDemands) } }

        // Reordering Control Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isReorderMode) "Arrastar alça ☰ ou usar setas para ordenar" else "Modo de ordenação inativo",
                color = if (isReorderMode) BlueAccent else TextGray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            
            TextButton(
                onClick = { isReorderMode = !isReorderMode },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.height(28.dp).testTag("toggle_reorder_mode_button")
            ) {
                Icon(
                    imageVector = if (isReorderMode) Icons.Default.Check else Icons.Default.Reorder,
                    contentDescription = null,
                    tint = if (isReorderMode) BlueAccent else TextGray,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isReorderMode) "Pronto" else "Reordenar",
                    color = if (isReorderMode) BlueAccent else TextWhite,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Ticket Cards List
        if (localDemands.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.SearchOff,
                        contentDescription = null,
                        tint = TextGray,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Nenhum chamado encontrado",
                        color = TextGrayLight,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Revise os filtros selecionados ou digite outra busca",
                        color = TextGray,
                        fontSize = 11.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(localDemands, key = { it.id }) { demand ->
                    val index = localDemands.indexOfFirst { it.id == demand.id }
                    var dragAccumulator by remember(demand.id) { mutableStateOf(0f) }
                    
                    val dragModifier = if (isReorderMode) {
                        Modifier.pointerInput(demand.id) {
                            detectVerticalDragGestures(
                                onDragStart = { dragAccumulator = 0f },
                                onDragEnd = {
                                    val orders = localDemands.mapIndexed { idx, d ->
                                        com.exemplo.tidemandas.network.ReorderItem(id = d.id, sort_order = idx)
                                    }
                                    viewModel.reorderDemands(orders)
                                },
                                onVerticalDrag = { change, dragAmount ->
                                    change.consume()
                                    dragAccumulator += dragAmount
                                    val currentIdx = localDemands.indexOfFirst { it.id == demand.id }
                                    if (currentIdx != -1) {
                                        if (dragAccumulator > 150f && currentIdx < localDemands.size - 1) {
                                            val temp = localDemands[currentIdx]
                                            localDemands[currentIdx] = localDemands[currentIdx + 1]
                                            localDemands[currentIdx + 1] = temp
                                            dragAccumulator = 0f
                                            
                                            val orders = localDemands.mapIndexed { idx, d ->
                                                com.exemplo.tidemandas.network.ReorderItem(id = d.id, sort_order = idx)
                                            }
                                            viewModel.reorderDemands(orders)
                                        } else if (dragAccumulator < -150f && currentIdx > 0) {
                                            val temp = localDemands[currentIdx]
                                            localDemands[currentIdx] = localDemands[currentIdx - 1]
                                            localDemands[currentIdx - 1] = temp
                                            dragAccumulator = 0f
                                            
                                            val orders = localDemands.mapIndexed { idx, d ->
                                                com.exemplo.tidemandas.network.ReorderItem(id = d.id, sort_order = idx)
                                            }
                                            viewModel.reorderDemands(orders)
                                        }
                                    }
                                }
                            )
                        }
                    } else {
                        Modifier
                    }

                    TicketCardItem(
                        demand = demand,
                        onViewClick = { viewModel.openViewDemand(demand) },
                        onEditClick = { viewModel.openEditDemand(demand) },
                        onDeleteClick = { onDeleteDemand(demand) },
                        isReorderMode = isReorderMode,
                        dragModifier = dragModifier,
                        onMoveUp = if (index > 0) {
                            {
                                val temp = localDemands[index]
                                localDemands[index] = localDemands[index - 1]
                                localDemands[index - 1] = temp
                                
                                val orders = localDemands.mapIndexed { idx, d ->
                                    com.exemplo.tidemandas.network.ReorderItem(id = d.id, sort_order = idx)
                                }
                                viewModel.reorderDemands(orders)
                            }
                        } else null,
                        onMoveDown = if (index < localDemands.size - 1) {
                            {
                                val temp = localDemands[index]
                                localDemands[index] = localDemands[index + 1]
                                localDemands[index + 1] = temp
                                
                                val orders = localDemands.mapIndexed { idx, d ->
                                    com.exemplo.tidemandas.network.ReorderItem(id = d.id, sort_order = idx)
                                }
                                viewModel.reorderDemands(orders)
                            }
                        } else null
                    )
                }
            }
        }
    }
    }
}

@Composable
fun TabHeaderButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .background(if (isSelected) Color(0xFF1E69FF).copy(alpha = 0.12f) else Color.Transparent)
            .clickable(onClick = onClick)
            .drawBehind {
                if (isSelected) {
                    val strokeWidth = 3.dp.toPx()
                    val y = size.height - strokeWidth / 2
                    drawLine(
                        color = BlueAccent,
                        start = androidx.compose.ui.geometry.Offset(0f, y),
                        end = androidx.compose.ui.geometry.Offset(size.width, y),
                        strokeWidth = strokeWidth
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) TextWhite else TextGray,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun TicketCardItem(
    demand: Demand,
    onViewClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    isReorderMode: Boolean = false,
    dragModifier: Modifier = Modifier,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null
) {
    val priorityColor = when (demand.prioridade) {
        "ALTA" -> PriorityHigh
        "NORMAL" -> PriorityNormal
        else -> PriorityLow
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Color(0xFF1E293B)),
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isReorderMode) Modifier else Modifier.clickable(onClick = onViewClick))
            .testTag("ticket_card_${demand.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Drag handle if in reorder mode
            if (isReorderMode) {
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = "Arrastar para reordenar",
                    tint = TextGray,
                    modifier = dragModifier
                        .padding(end = 8.dp)
                        .size(24.dp)
                )
            }

            // Drag handle / side dot indicator matching visual
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(36.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(priorityColor)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Main Text block
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(priorityColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = demand.prioridade,
                            color = priorityColor,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = demand.protocolo,
                        color = TextGray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = demand.titulo,
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = TextGray,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Criado em ${formatShortDate(demand.dataCriacao)}",
                        color = TextGray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    // Special Orange Anniversary badge (+1 Mês)
                    if (isAnniversary(demand.dataCriacao) && demand.status == "PENDENTE") {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFF59E0B), RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🎂 +1 Mês", color = BgDark, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Action or Reorder Buttons
            if (isReorderMode) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { onMoveUp?.invoke() },
                        enabled = onMoveUp != null,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "Mover para cima",
                            tint = if (onMoveUp != null) TextWhite else TextGray.copy(alpha = 0.3f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = { onMoveDown?.invoke() },
                        enabled = onMoveDown != null,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = "Mover para baixo",
                            tint = if (onMoveDown != null) TextWhite else TextGray.copy(alpha = 0.3f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(onClick = onViewClick, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Visibility, contentDescription = "Ver", tint = TextGray, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onEditClick, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Editar", tint = TextGray, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Excluir", tint = PriorityHigh, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// SCREEN 3: CORPO TÉCNICO / USERS
// -------------------------------------------------------------
@Composable
fun UsuariosScreen(viewModel: MainViewModel) {
    val userProfile by viewModel.userProfile.collectAsState()
    val techUsers by viewModel.techUsers.collectAsState()

    val isMaster = userProfile?.nivel == "MASTER"

    if (!isMaster) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Acesso Restrito",
                    tint = PriorityHigh,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Acesso Restrito",
                    color = TextWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Apenas administradores com nível Master podem acessar o Módulo de Equipe.",
                    color = TextGray,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Corpo Técnico",
                    color = TextWhite,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Gestão de acessos e permissões do sistema de TI",
                    color = TextGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Button(
                onClick = { viewModel.openCreateTechUser() },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BlueAccent, contentColor = TextWhite),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                modifier = Modifier
                    .height(38.dp)
                    .testTag("novo_usuario_button")
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Novo Usuário", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Responsive grid header mimicking screenshot table
        Surface(
            color = SurfaceDark,
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomEnd = 0.dp, bottomStart = 0.dp),
            border = BorderStroke(1.dp, Color(0xFF1E293B))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("TÉCNICO", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f))
                Text("NÍVEL", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Text("STATUS", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .background(BgDark)
        ) {
            items(techUsers) { user ->
                TechUserRowItem(
                    user = user,
                    onClick = { viewModel.openEditTechUser(user) }
                )
            }
        }
    }
}

@Composable
fun TechUserRowItem(
    user: TechUser,
    onClick: () -> Unit
) {
    val initial = user.nome.take(1).uppercase()
    val avatarColor = when (initial) {
        "I" -> Color(0xFF3B82F6)
        "J" -> Color(0xFF8B5CF6)
        "L" -> Color(0xFFEC4899)
        else -> Color(0xFF10B981)
    }

    Surface(
        color = BgDark,
        border = BorderStroke(1.dp, Color(0xFF1E293B)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // User identity with colored avatar
            Row(
                modifier = Modifier.weight(1.5f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(avatarColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initial,
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = user.nome,
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = user.email,
                        color = TextGray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // LEVEL badge (MASTER or COLABORADOR)
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .border(
                            1.dp, 
                            if (user.nivel == "MASTER") Color(0xFFA78BFA) else Color(0xFF60A5FA), 
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = user.nivel,
                        color = if (user.nivel == "MASTER") Color(0xFFA78BFA) else Color(0xFF60A5FA),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // STATUS outline badge (ATIVO or DESABILITADO)
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .border(
                            1.dp, 
                            if (user.ativo) StatusCompleted else PriorityHigh, 
                            RoundedCornerShape(12.dp)
                        )
                        .background(
                            if (user.ativo) StatusCompleted.copy(alpha = 0.05f) else PriorityHigh.copy(alpha = 0.05f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (user.ativo) "ATIVO" else "DESABILITADO",
                        color = if (user.ativo) StatusCompleted else PriorityHigh,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// SCREEN 4: HISTÓRICO / AUDITORIA LOGS
// -------------------------------------------------------------
@Composable
fun AuditoriaScreen(viewModel: MainViewModel) {
    val userProfile by viewModel.userProfile.collectAsState()
    val auditLogs by viewModel.auditLogs.collectAsState()

    val isMaster = userProfile?.nivel == "MASTER"

    if (!isMaster) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Acesso Restrito",
                    tint = PriorityHigh,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Acesso Restrito",
                    color = TextWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Apenas administradores com nível Master podem acessar o Módulo de Auditoria.",
                    color = TextGray,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = "Histórico de Atividade",
                color = TextWhite,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "Log de auditoria e monitoramento de operações",
                color = TextGray,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Terminal style banner matching screenshot
        Surface(
            color = BgDark,
            border = BorderStroke(1.dp, Color(0xFF1E293B)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = null,
                    tint = Color(0xFF60A5FA),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "MONITORAMENTO EM TEMPO REAL - BANCO DE DADOS OPERACIONAL",
                    color = Color(0xFF60A5FA),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Scrollable audit list
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(auditLogs) { log ->
                AuditLogRowItem(log = log)
            }
        }
    }
}

@Composable
fun AuditLogRowItem(log: AuditLog) {
    val opColor = when (log.operacao) {
        "LOGIN_SUCCESS" -> StatusCompleted
        "CREATE_DEMAND" -> Color(0xFF10B981)
        "UPDATE_DEMAND" -> Color(0xFF3B82F6)
        "DELETE_DEMAND" -> PriorityHigh
        else -> TextGray
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(opColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = log.executor,
                        color = TextWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(opColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = log.operacao,
                            color = opColor,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = formatShortDate(log.dataHora),
                    color = TextGray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = log.detalhes,
                color = TextGrayLight,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            if (log.modulo.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Módulo: ${log.modulo}",
                    color = TextGray,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// -------------------------------------------------------------
// SCREEN 5: AJUSTES / CREDENTIALS
// -------------------------------------------------------------
@Composable
fun AjustesScreen(viewModel: MainViewModel) {
    val userProfile by viewModel.userProfile.collectAsState()
    val context = LocalContext.current

    var showEditProfileDialog by remember { mutableStateOf(false) }

    if (showEditProfileDialog && userProfile != null) {
        var editSenhaAtual by remember { mutableStateOf("") }
        var editNovaSenha by remember { mutableStateOf("") }
        var isSavingProfile by remember { mutableStateOf(false) }

        androidx.compose.ui.window.Dialog(onDismissRequest = { showEditProfileDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Alterar Senha",
                        color = TextWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = editSenhaAtual,
                        onValueChange = { editSenhaAtual = it },
                        label = { Text("Senha Atual") },
                        placeholder = { Text("Digite sua senha atual") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth().testTag("edit_profile_current_password"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite, unfocusedTextColor = TextWhite,
                            focusedBorderColor = BlueAccent, unfocusedBorderColor = Color(0xFF1E293B)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = editNovaSenha,
                        onValueChange = { editNovaSenha = it },
                        label = { Text("Nova Senha") },
                        placeholder = { Text("Digite a nova senha desejada") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth().testTag("edit_profile_new_password"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite, unfocusedTextColor = TextWhite,
                            focusedBorderColor = BlueAccent, unfocusedBorderColor = Color(0xFF1E293B)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showEditProfileDialog = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFF1E293B)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextGray)
                        ) {
                            Text("Cancelar", fontSize = 13.sp)
                        }

                        Button(
                            onClick = {
                                if (editSenhaAtual.isBlank() || editNovaSenha.isBlank()) {
                                    android.widget.Toast.makeText(context, "Por favor, preencha a senha atual e a nova senha.", android.widget.Toast.LENGTH_SHORT).show()
                                } else {
                                    isSavingProfile = true
                                    viewModel.changeLoggedUserPassword(
                                        currentPass = editSenhaAtual,
                                        newPass = editNovaSenha
                                    ) { passSuccess, passMsg ->
                                        isSavingProfile = false
                                        android.widget.Toast.makeText(context, passMsg, android.widget.Toast.LENGTH_SHORT).show()
                                        if (passSuccess) {
                                            showEditProfileDialog = false
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("save_profile_btn"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BlueAccent, contentColor = Color.White)
                        ) {
                            if (isSavingProfile) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Salvar", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Ajustes de TI",
                    color = TextWhite,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Gerencie seu perfil, preferências de tema e sessão",
                    color = TextGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Section: Sessão Ativa & Rota Protegida (Requirements 2, 3 & 4)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Sessão Segura (JWT)",
                                color = TextWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Dados obtidos via rota protegida (perfil)",
                                color = TextGray,
                                fontSize = 11.sp
                            )
                        }
                        
                        IconButton(
                            onClick = { viewModel.fetchUserProfile() },
                            modifier = Modifier.background(SurfaceDarkVariant, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Recarregar Perfil",
                                tint = BlueAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    if (userProfile != null) {
                        Surface(
                            color = BgDark,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFF1E293B)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Nome:", color = TextGray, fontSize = 12.sp)
                                    Text(userProfile?.nome ?: "", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Email:", color = TextGray, fontSize = 12.sp)
                                    Text(userProfile?.email ?: "", color = TextWhite, fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Nível:", color = TextGray, fontSize = 12.sp)
                                    Text(userProfile?.nivel ?: "", color = BlueAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Status:", color = TextGray, fontSize = 12.sp)
                                    Text(
                                        if (userProfile?.ativo == true) "ATIVO" else "INATIVO",
                                        color = if (userProfile?.ativo == true) StatusCompleted else PriorityHigh,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedButton(
                                    onClick = { showEditProfileDialog = true },
                                    modifier = Modifier.fillMaxWidth().height(36.dp).testTag("edit_profile_trigger"),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, Color(0xFF1E293B)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BlueAccent)
                                ) {
                                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Alterar Senha", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Section: Preferência de Tema (Requirement E)
                        Text(
                            text = "PREFERÊNCIA DE TEMA",
                            color = TextGray,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val currentTheme = userProfile?.theme ?: "dark"

                            // Botão Modo Claro
                            Button(
                                onClick = {
                                    viewModel.updateThemePreference("light") { success, msg ->
                                        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (currentTheme == "light") BlueAccent else SurfaceDarkVariant,
                                    contentColor = if (currentTheme == "light") Color.White else TextWhite
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .testTag("theme_light_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WbSunny,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Modo Claro", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            // Botão Modo Escuro
                            Button(
                                onClick = {
                                    viewModel.updateThemePreference("dark") { success, msg ->
                                        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (currentTheme == "dark") BlueAccent else SurfaceDarkVariant,
                                    contentColor = if (currentTheme == "dark") Color.White else TextWhite
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .testTag("theme_dark_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NightsStay,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Modo Escuro", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(BgDark, RoundedCornerShape(8.dp))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Nenhum dado carregado. Toque para recarregar.",
                                color = TextGray,
                                fontSize = 12.sp
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = { viewModel.performLogout() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7F1D1D), contentColor = Color(0xFFFECACA)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("logout_btn"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Encerrar Sessão", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }



        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// -------------------------------------------------------------
// SECURITY CONFIRM DELETE FLOW
// -------------------------------------------------------------
sealed class ConfirmDeleteState {
    data class DeleteDemand(val demand: Demand) : ConfirmDeleteState()
    data class DeleteTechUser(val user: TechUser) : ConfirmDeleteState()
    data class DeleteAttachment(val fileUrl: String) : ConfirmDeleteState()
}

@Composable
fun ConfirmDeleteDialog(
    state: ConfirmDeleteState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val title = "Confirmar Exclusão"
    val message = when (state) {
        is ConfirmDeleteState.DeleteDemand -> "Tem certeza de que deseja excluir permanentemente o chamado '${state.demand.titulo}'? Esta ação registrará um log de auditoria de segurança."
        is ConfirmDeleteState.DeleteTechUser -> "Tem certeza de que deseja excluir permanentemente o colaborador '${state.user.nome}'? Esta ação registrará um log de auditoria de segurança."
        is ConfirmDeleteState.DeleteAttachment -> "Tem certeza de que deseja excluir permanentemente este anexo?"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Aviso de Segurança",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    color = TextWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Text(
                text = message,
                color = TextGray,
                fontSize = 14.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444), contentColor = TextWhite),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Confirmar Exclusão", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = TextGray)
            ) {
                Text("Cancelar")
            }
        },
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 6.dp
    )
}
