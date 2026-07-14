package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.data.model.AuditLog
import com.example.data.model.Demand
import com.example.data.model.DemandFile
import com.example.data.model.getFilesList
import com.example.data.model.SyncSettings
import com.example.data.model.TechUser
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

// Helper to format timestamps
fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy, HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

fun formatShortDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

// Check if a ticket was created more than a month ago (+1 Mês Badge)
fun isAnniversary(timestamp: Long): Boolean {
    val ticketCal = Calendar.getInstance().apply { timeInMillis = timestamp }
    val currentCal = Calendar.getInstance()
    val diffInMillis = currentCal.timeInMillis - ticketCal.timeInMillis
    val diffInDays = diffInMillis / (1000 * 60 * 60 * 24)
    return diffInDays >= 30
}

@Composable
fun StatusDonutChart(
    completedCount: Int,
    pendingCount: Int,
    modifier: Modifier = Modifier
) {
    val total = completedCount + pendingCount
    val completedAngle = if (total > 0) (completedCount.toFloat() / total * 360f) else 270f
    val pendingAngle = if (total > 0) (pendingCount.toFloat() / total * 360f) else 90f

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.PieChart,
                    contentDescription = null,
                    tint = TextGray,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Distribuição por Status",
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(160.dp)
            ) {
                Canvas(modifier = Modifier.size(140.dp)) {
                    val strokeWidth = 16.dp.toPx()
                    // Background track
                    drawCircle(
                        color = Color(0xFF161E36),
                        radius = size.minDimension / 2 - strokeWidth / 2,
                        style = Stroke(width = strokeWidth)
                    )

                    if (total > 0) {
                        // Completed Segment (Green)
                        drawArc(
                            color = StatusCompleted,
                            startAngle = -90f,
                            sweepAngle = completedAngle,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                        // Pending Segment (Orange)
                        drawArc(
                            color = StatusPending,
                            startAngle = -90f + completedAngle,
                            sweepAngle = pendingAngle,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$total",
                        color = TextWhite,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "TOTAL",
                        color = TextGray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(StatusCompleted, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Concluídas ($completedCount)",
                        color = TextGray,
                        fontSize = 12.sp
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(StatusPending, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Pendentes ($pendingCount)",
                        color = TextGray,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun PriorityBarChart(
    highCount: Int,
    normalCount: Int,
    lowCount: Int,
    modifier: Modifier = Modifier
) {
    val maxCount = maxOf(highCount, normalCount, lowCount, 5).toFloat()

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.BarChart,
                    contentDescription = null,
                    tint = TextGray,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Pendentes por Prioridade",
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                // Background grid lines
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    for (i in 0..4) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color(0xFF1E293B).copy(alpha = 0.5f))
                        )
                    }
                }

                // Bars
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    BarItem(
                        label = "Alta",
                        count = highCount,
                        maxCount = maxCount,
                        color = PriorityHigh
                    )
                    BarItem(
                        label = "Normal",
                        count = normalCount,
                        maxCount = maxCount,
                        color = PriorityNormal
                    )
                    BarItem(
                        label = "Sem Prio",
                        count = lowCount,
                        maxCount = maxCount,
                        color = PriorityLow
                    )
                }
            }
        }
    }
}

@Composable
fun RowScope.BarItem(
    label: String,
    count: Int,
    maxCount: Float,
    color: Color
) {
    val heightFraction = if (maxCount > 0) (count.toFloat() / maxCount) else 0f
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
    ) {
        Text(
            text = "$count",
            color = TextWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        Box(
            modifier = Modifier
                .width(42.dp)
                .fillMaxHeight(heightFraction.coerceIn(0.05f, 1f))
                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomEnd = 0.dp, bottomStart = 0.dp))
                .background(color)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = label,
            color = TextGray,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun RegistrarChamadoDialog(
    onDismiss: () -> Unit,
    uploadState: UploadState,
    onUploadFile: (android.net.Uri) -> Unit,
    onDeleteUploadedFile: (String) -> Unit,
    onResetUpload: () -> Unit,
    onSave: (titulo: String, descricao: String, prioridade: String, anexoUrl: String?, anexoNome: String?) -> Unit
) {
    var titulo by remember { mutableStateOf("") }
    var descricao by remember { mutableStateOf("") }
    var prioridade by remember { mutableStateOf("NORMAL") } // SEM_PRIORIDADE, NORMAL, ALTA

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF1E69FF).copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = BlueAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Registrar Chamado",
                        color = TextWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "TÍTULO DO CHAMADO",
                    color = TextGray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it },
                    placeholder = { Text("Ex: Instalar impressora no financeiro", color = Color(0xFF475569), fontSize = 14.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("chamado_titulo_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = BlueAccent,
                        unfocusedBorderColor = Color(0xFF1E293B),
                        focusedContainerColor = BgDark,
                        unfocusedContainerColor = BgDark
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "DESCRIÇÃO DO PROBLEMA",
                    color = TextGray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = descricao,
                    onValueChange = { descricao = it },
                    placeholder = { Text("Descreva detalhadamente a situação...", color = Color(0xFF475569), fontSize = 14.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .testTag("chamado_desc_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = BlueAccent,
                        unfocusedBorderColor = Color(0xFF1E293B),
                        focusedContainerColor = BgDark,
                        unfocusedContainerColor = BgDark
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "NÍVEL DE PRIORIDADE",
                    color = TextGray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BgDark, RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                        .padding(vertical = 4.dp, horizontal = 8.dp)
                ) {
                    PriorityRadioButton(
                        label = "SEM PRIORIDADE",
                        isSelected = prioridade == "SEM_PRIORIDADE",
                        onClick = { prioridade = "SEM_PRIORIDADE" },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF1E293B)))
                    PriorityRadioButton(
                        label = "NORMAL",
                        isSelected = prioridade == "NORMAL",
                        onClick = { prioridade = "NORMAL" },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF1E293B)))
                    PriorityRadioButton(
                        label = "ALTA PRIORIDADE",
                        isSelected = prioridade == "ALTA",
                        onClick = { prioridade = "ALTA" },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                val context = androidx.compose.ui.platform.LocalContext.current
                var tempCameraUri by remember { mutableStateOf<android.net.Uri?>(null) }

                val cameraLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.TakePicture()
                ) { success ->
                    if (success) {
                        tempCameraUri?.let { uri ->
                            val compressedUri = compressImageUri(context, uri)
                            onUploadFile(compressedUri)
                        }
                    }
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) {
                        try {
                            val uri = createTempImageUri(context)
                            tempCameraUri = uri
                            cameraLauncher.launch(uri)
                        } catch (e: Exception) {
                            android.util.Log.e("Camera", "Erro ao iniciar câmera após permissão", e)
                        }
                    } else {
                        android.widget.Toast.makeText(context, "Permissão da câmera negada", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }

                val launcher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent()
                ) { uri: android.net.Uri? ->
                    if (uri != null) {
                        onUploadFile(uri)
                    }
                }

                val onLaunchCamera = {
                    val permission = android.Manifest.permission.CAMERA
                    val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(context, permission) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    if (hasPermission) {
                        try {
                            val uri = createTempImageUri(context)
                            tempCameraUri = uri
                            cameraLauncher.launch(uri)
                        } catch (e: Exception) {
                            android.util.Log.e("Camera", "Erro ao iniciar câmera", e)
                            android.widget.Toast.makeText(context, "Erro ao abrir câmera", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        permissionLauncher.launch(permission)
                    }
                }

                val hasFile = uploadState is UploadState.Success
                val isUploading = uploadState is UploadState.Loading
                val fileLabel = if (hasFile) "ARQUIVOS ANEXOS (1)" else "ARQUIVOS ANEXOS (0)"

                Text(
                    text = fileLabel,
                    color = TextGray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                        .background(BgDark, RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    when (uploadState) {
                        is UploadState.Loading -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = BlueAccent, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "ENVIANDO ARQUIVO...",
                                    color = TextGrayLight,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        is UploadState.Success -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(
                                        imageVector = Icons.Default.InsertDriveFile,
                                        contentDescription = null,
                                        tint = BlueAccent,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = uploadState.fileName,
                                            color = TextWhite,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Upload concluído",
                                            color = Color(0xFF10B981),
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { onDeleteUploadedFile(uploadState.fileUrl) },
                                    modifier = Modifier.testTag("delete_file_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Excluir anexo",
                                        tint = Color(0xFFEF4444)
                                    )
                                }
                            }
                        }
                        is UploadState.Error -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable { onResetUpload() }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = uploadState.message,
                                    color = Color(0xFFEF4444),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                )
                                Text(
                                    text = "Clique para tentar novamente",
                                    color = TextGray,
                                    fontSize = 9.sp
                                )
                            }
                        }
                        else -> {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clickable { launcher.launch("*/*") }
                                        .padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudUpload,
                                        contentDescription = null,
                                        tint = TextGray,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "ANEXAR ARQUIVO",
                                        color = TextGrayLight,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Imagens, PDFs, docs...",
                                        color = TextGray,
                                        fontSize = 8.sp
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .fillMaxHeight()
                                        .background(Color(0xFF1E293B))
                                )

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clickable { onLaunchCamera() }
                                        .padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PhotoCamera,
                                        contentDescription = null,
                                        tint = TextGray,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "TIRAR FOTO",
                                        color = TextGrayLight,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Abrir câmera",
                                        color = TextGray,
                                        fontSize = 8.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("cancel_button"),
                        border = BorderStroke(1.dp, Color(0xFF1E293B)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextGrayLight)
                    ) {
                        Text("Cancelar", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            if (titulo.isNotBlank() && descricao.isNotBlank()) {
                                val fileUrl = if (uploadState is UploadState.Success) uploadState.fileUrl else null
                                val fileName = if (uploadState is UploadState.Success) uploadState.fileName else null
                                onSave(titulo, descricao, prioridade, fileUrl, fileName)
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(48.dp)
                            .testTag("salvar_chamado_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = BlueAccent, contentColor = TextWhite)
                    ) {
                        Text("Salvar Chamado", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun EditarChamadoDialog(
    demand: Demand,
    onDismiss: () -> Unit,
    uploadState: UploadState,
    onUploadFile: (android.net.Uri) -> Unit,
    onDeleteUploadedFile: (String) -> Unit,
    onResetUpload: () -> Unit,
    onSave: (Demand) -> Unit
) {
    var titulo by remember { mutableStateOf(demand.titulo) }
    var descricao by remember { mutableStateOf(demand.descricao) }
    var prioridade by remember { mutableStateOf(demand.prioridade) }
    var anexoUrl by remember { mutableStateOf(demand.anexoUrl) }

    LaunchedEffect(uploadState) {
        if (uploadState is UploadState.Success) {
            anexoUrl = uploadState.fileUrl
        } else if (uploadState is UploadState.Idle) {
            anexoUrl = null
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF1E69FF).copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = BlueAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Editar Chamado",
                        color = TextWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "TÍTULO DO CHAMADO",
                    color = TextGray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_titulo_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = BlueAccent,
                        unfocusedBorderColor = Color(0xFF1E293B),
                        focusedContainerColor = BgDark,
                        unfocusedContainerColor = BgDark
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "DESCRIÇÃO DO PROBLEMA",
                    color = TextGray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = descricao,
                    onValueChange = { descricao = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .testTag("edit_desc_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = BlueAccent,
                        unfocusedBorderColor = Color(0xFF1E293B),
                        focusedContainerColor = BgDark,
                        unfocusedContainerColor = BgDark
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "NÍVEL DE PRIORIDADE",
                    color = TextGray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BgDark, RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                        .padding(vertical = 4.dp, horizontal = 8.dp)
                ) {
                    PriorityRadioButton(
                        label = "SEM PRIORIDADE",
                        isSelected = prioridade == "SEM_PRIORIDADE",
                        onClick = { prioridade = "SEM_PRIORIDADE" },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF1E293B)))
                    PriorityRadioButton(
                        label = "NORMAL",
                        isSelected = prioridade == "NORMAL",
                        onClick = { prioridade = "NORMAL" },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF1E293B)))
                    PriorityRadioButton(
                        label = "ALTA PRIORIDADE",
                        isSelected = prioridade == "ALTA",
                        onClick = { prioridade = "ALTA" },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                val context = androidx.compose.ui.platform.LocalContext.current
                var tempCameraUri by remember { mutableStateOf<android.net.Uri?>(null) }

                val cameraLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.TakePicture()
                ) { success ->
                    if (success) {
                        tempCameraUri?.let { uri ->
                            val compressedUri = compressImageUri(context, uri)
                            onUploadFile(compressedUri)
                        }
                    }
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) {
                        try {
                            val uri = createTempImageUri(context)
                            tempCameraUri = uri
                            cameraLauncher.launch(uri)
                        } catch (e: Exception) {
                            android.util.Log.e("Camera", "Erro ao iniciar câmera após permissão", e)
                        }
                    } else {
                        android.widget.Toast.makeText(context, "Permissão da câmera negada", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }

                val launcher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent()
                ) { uri: android.net.Uri? ->
                    if (uri != null) {
                        onUploadFile(uri)
                    }
                }

                val onLaunchCamera = {
                    val permission = android.Manifest.permission.CAMERA
                    val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(context, permission) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    if (hasPermission) {
                        try {
                            val uri = createTempImageUri(context)
                            tempCameraUri = uri
                            cameraLauncher.launch(uri)
                        } catch (e: Exception) {
                            android.util.Log.e("Camera", "Erro ao iniciar câmera", e)
                            android.widget.Toast.makeText(context, "Erro ao abrir câmera", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        permissionLauncher.launch(permission)
                    }
                }

                val hasFile = !anexoUrl.isNullOrBlank()
                val isUploading = uploadState is UploadState.Loading
                val fileLabel = if (hasFile) "ARQUIVOS ANEXOS (1)" else "ARQUIVOS ANEXOS (0)"

                Text(
                    text = fileLabel,
                    color = TextGray,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                        .background(BgDark, RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isUploading) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = BlueAccent, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "ENVIANDO ARQUIVO...",
                                color = TextGrayLight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else if (hasFile) {
                        val currentFileUrl = anexoUrl ?: ""
                        val currentFileName = if (uploadState is UploadState.Success) {
                            uploadState.fileName
                        } else {
                            currentFileUrl.substringAfterLast("/")
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = Icons.Default.InsertDriveFile,
                                    contentDescription = null,
                                    tint = BlueAccent,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = currentFileName,
                                        color = TextWhite,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Anexo de chamado",
                                        color = Color(0xFF10B981),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                            IconButton(
                                onClick = { onDeleteUploadedFile(currentFileUrl) },
                                modifier = Modifier.testTag("delete_file_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Excluir anexo",
                                    tint = Color(0xFFEF4444)
                                )
                            }
                        }
                    } else if (uploadState is UploadState.Error) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { onResetUpload() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = uploadState.message,
                                color = Color(0xFFEF4444),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                            Text(
                                text = "Clique para tentar novamente",
                                color = TextGray,
                                fontSize = 9.sp
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable { launcher.launch("*/*") }
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = null,
                                    tint = TextGray,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "ANEXAR ARQUIVO",
                                    color = TextGrayLight,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Imagens, PDFs, docs...",
                                    color = TextGray,
                                    fontSize = 8.sp
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .fillMaxHeight()
                                    .background(Color(0xFF1E293B))
                            )

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable { onLaunchCamera() }
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = null,
                                    tint = TextGray,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "TIRAR FOTO",
                                    color = TextGrayLight,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Abrir câmera",
                                    color = TextGray,
                                    fontSize = 8.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        border = BorderStroke(1.dp, Color(0xFF1E293B)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextGrayLight)
                    ) {
                        Text("Cancelar", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            if (titulo.isNotBlank() && descricao.isNotBlank()) {
                                val currentUrl = anexoUrl
                                val newFilesJson = if (!currentUrl.isNullOrBlank()) {
                                    "[{\"name\":\"${currentUrl.substringAfterLast("/")}\",\"url\":\"$currentUrl\"}]"
                                } else {
                                    null
                                }
                                onSave(demand.copy(
                                    titulo = titulo,
                                    descricao = descricao,
                                    prioridade = prioridade,
                                    anexoUrl = currentUrl,
                                    filesJson = newFilesJson
                                ))
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(48.dp)
                            .testTag("salvar_alteracoes_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = BlueAccent, contentColor = TextWhite)
                    ) {
                        Text("Salvar Alterações", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun PriorityRadioButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = BlueAccent,
                unselectedColor = TextGray
            )
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            color = if (isSelected) TextWhite else TextGray,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun DetalhesChamadoDialog(
    demand: Demand,
    onDismiss: () -> Unit,
    onToggleStatus: () -> Unit
) {
    var activePreviewFile by remember { mutableStateOf<DemandFile?>(null) }
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

    activePreviewFile?.let { file ->
        MediaViewerDialog(
            fileUrl = file.url,
            fileName = file.name,
            onDismiss = { activePreviewFile = null }
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .background(
                                    if (demand.status == "PENDENTE") Color(0xFFF59E0B).copy(alpha = 0.15f)
                                    else Color(0xFF10B981).copy(alpha = 0.15f),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (demand.status == "PENDENTE") "EM ABERTO" else "CONCLUÍDO",
                                color = if (demand.status == "PENDENTE") StatusPending else StatusCompleted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        val priorityColor = when (demand.prioridade) {
                            "ALTA" -> PriorityHigh
                            "NORMAL" -> PriorityNormal
                            else -> PriorityLow
                        }
                        Box(
                            modifier = Modifier
                                .background(priorityColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = demand.prioridade,
                                color = priorityColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fechar",
                            tint = TextGray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title
                Text(
                    text = demand.titulo,
                    color = TextWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )

                // Date
                Text(
                    text = "ABERTO EM ${formatTimestamp(demand.dataCriacao)}",
                    color = TextGray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Description Box (matched screenshot design)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BgDark, RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        text = demand.descricao,
                        color = TextWhite,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }

                val filesList = demand.getFilesList()
                if (filesList.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "ARQUIVOS ANEXOS (${filesList.size})",
                        color = TextGray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        filesList.forEach { file ->
                            val isPreviewable = file.url.endsWith(".pdf", ignoreCase = true) ||
                                    file.url.endsWith(".png", ignoreCase = true) ||
                                    file.url.endsWith(".jpg", ignoreCase = true) ||
                                    file.url.endsWith(".jpeg", ignoreCase = true) ||
                                    file.url.endsWith(".webp", ignoreCase = true) ||
                                    file.url.endsWith(".gif", ignoreCase = true)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(BgDark, RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                                    .clickable {
                                        if (isPreviewable) {
                                            activePreviewFile = file
                                        } else {
                                            try {
                                                uriHandler.openUri(file.url)
                                            } catch (e: Exception) {
                                                // ignore
                                            }
                                        }
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.InsertDriveFile,
                                    contentDescription = null,
                                    tint = BlueAccent,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = file.name,
                                        color = TextWhite,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (isPreviewable) "Toque para abrir no app" else "Toque para abrir no navegador",
                                        color = TextGrayLight,
                                        fontSize = 10.sp
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        if (isPreviewable) {
                                            activePreviewFile = file
                                        } else {
                                            try {
                                                uriHandler.openUri(file.url)
                                            } catch (e: Exception) {
                                                // ignore
                                            }
                                        }
                                    },
                                    modifier = Modifier.testTag("open_anexo_btn_${file.name.hashCode()}")
                                ) {
                                    Icon(
                                        imageVector = if (isPreviewable) Icons.Default.Visibility else Icons.Default.OpenInNew,
                                        contentDescription = "Abrir anexo",
                                        tint = BlueAccent
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Protocol & Complete Action Box
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BgDark, RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "PROTOCOLO DE REGISTRO",
                            color = TextGray,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = demand.protocolo,
                            color = TextWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = {
                            onToggleStatus()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (demand.status == "PENDENTE") StatusCompleted else Color(0xFF475569)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = if (demand.status == "PENDENTE") Icons.Default.CheckCircle else Icons.Default.Replay,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (demand.status == "PENDENTE") "CONCLUIR CHAMADO" else "REABRIR CHAMADO",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Close Details Button
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF1E293B)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextGrayLight)
                ) {
                    Text("FECHAR DETALHES", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun NovoUsuarioDialog(
    onDismiss: () -> Unit,
    onSave: (nome: String, email: String, nivel: String, ativo: Boolean, senha: String) -> Unit
) {
    var nome by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var nivel by remember { mutableStateOf("COLABORADOR") } // MASTER, COLABORADOR
    var ativo by remember { mutableStateOf(true) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = null,
                        tint = BlueAccent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Novo Usuário (Técnico)",
                        color = TextWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(text = "NOME DO COLABORADOR", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    placeholder = { Text("Ex: Jarbas", color = Color(0xFF475569)) },
                    modifier = Modifier.fillMaxWidth().testTag("novo_usuario_nome"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = BlueAccent,
                        unfocusedBorderColor = Color(0xFF1E293B),
                        focusedContainerColor = BgDark,
                        unfocusedContainerColor = BgDark
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(text = "EMAIL", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = { Text("Ex: bolao@gmail.com", color = Color(0xFF475569)) },
                    modifier = Modifier.fillMaxWidth().testTag("novo_usuario_email"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = BlueAccent,
                        unfocusedBorderColor = Color(0xFF1E293B),
                        focusedContainerColor = BgDark,
                        unfocusedContainerColor = BgDark
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(text = "SENHA INICIAL", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                var isPasswordVisible by remember { mutableStateOf(false) }
                OutlinedTextField(
                    value = senha,
                    onValueChange = { senha = it },
                    placeholder = { Text("Ex: senhaInicial123", color = Color(0xFF475569)) },
                    modifier = Modifier.fillMaxWidth().testTag("novo_usuario_senha"),
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = if (isPasswordVisible) "Ocultar senha" else "Mostrar senha",
                                tint = TextGray
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = BlueAccent,
                        unfocusedBorderColor = Color(0xFF1E293B),
                        focusedContainerColor = BgDark,
                        unfocusedContainerColor = BgDark
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(text = "NÍVEL DE PERMISSÃO", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BgDark, RoundedCornerShape(8.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { nivel = "MASTER" }.testTag("novo_usuario_nivel_master")
                    ) {
                        RadioButton(selected = nivel == "MASTER", onClick = { nivel = "MASTER" })
                        Text("MASTER", color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { nivel = "COLABORADOR" }.testTag("novo_usuario_nivel_colaborador")
                    ) {
                        RadioButton(selected = nivel == "COLABORADOR", onClick = { nivel = "COLABORADOR" })
                        Text("COLABORADOR", color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "COLABORADOR ATIVO",
                        color = TextGray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Switch(
                        checked = ativo,
                        onCheckedChange = { ativo = it },
                        modifier = Modifier.testTag("novo_usuario_ativo_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = StatusCompleted,
                            checkedTrackColor = StatusCompleted.copy(alpha = 0.3f)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).testTag("novo_usuario_cancel_button"),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF1E293B)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextGrayLight)
                    ) {
                        Text("Cancelar")
                    }

                    Button(
                        onClick = {
                            if (nome.isNotBlank() && email.isNotBlank() && senha.isNotBlank()) {
                                onSave(nome, email, nivel, ativo, senha)
                            }
                        },
                        modifier = Modifier.weight(1.2f).testTag("novo_usuario_save_button"),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BlueAccent, contentColor = TextWhite)
                    ) {
                        Text("Criar Usuário", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun EditarUsuarioDialog(
    user: TechUser,
    onDismiss: () -> Unit,
    onSave: (TechUser, String?) -> Unit,
    onDelete: () -> Unit
) {
    var nome by remember { mutableStateOf(user.nome) }
    var email by remember { mutableStateOf(user.email) }
    var senha by remember { mutableStateOf("") }
    var nivel by remember { mutableStateOf(user.nivel) }
    var ativo by remember { mutableStateOf(user.ativo) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = BlueAccent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Editar Usuário",
                        color = TextWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(text = "NOME DO COLABORADOR", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    modifier = Modifier.fillMaxWidth().testTag("editar_usuario_nome"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = BlueAccent,
                        unfocusedBorderColor = Color(0xFF1E293B),
                        focusedContainerColor = BgDark,
                        unfocusedContainerColor = BgDark
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(text = "EMAIL", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    modifier = Modifier.fillMaxWidth().testTag("editar_usuario_email"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = BlueAccent,
                        unfocusedBorderColor = Color(0xFF1E293B),
                        focusedContainerColor = BgDark,
                        unfocusedContainerColor = BgDark
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(text = "NOVA SENHA (OPCIONAL)", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                var isPasswordVisible by remember { mutableStateOf(false) }
                OutlinedTextField(
                    value = senha,
                    onValueChange = { senha = it },
                    placeholder = { Text("Deixe em branco para não alterar", color = Color(0xFF475569), fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth().testTag("editar_usuario_senha"),
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = if (isPasswordVisible) "Ocultar senha" else "Mostrar senha",
                                tint = TextGray
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = BlueAccent,
                        unfocusedBorderColor = Color(0xFF1E293B),
                        focusedContainerColor = BgDark,
                        unfocusedContainerColor = BgDark
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(text = "NÍVEL DE PERMISSÃO", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BgDark, RoundedCornerShape(8.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { nivel = "MASTER" }.testTag("editar_usuario_nivel_master")
                    ) {
                        RadioButton(selected = nivel == "MASTER", onClick = { nivel = "MASTER" })
                        Text("MASTER", color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { nivel = "COLABORADOR" }.testTag("editar_usuario_nivel_colaborador")
                    ) {
                        RadioButton(selected = nivel == "COLABORADOR", onClick = { nivel = "COLABORADOR" })
                        Text("COLABORADOR", color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "COLABORADOR ATIVO",
                        color = TextGray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Switch(
                        checked = ativo,
                        onCheckedChange = { ativo = it },
                        modifier = Modifier.testTag("editar_usuario_ativo_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = StatusCompleted,
                            checkedTrackColor = StatusCompleted.copy(alpha = 0.3f)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = {
                            onDelete()
                            onDismiss()
                        },
                        modifier = Modifier
                            .background(Color(0xFFEF4444).copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                            .size(48.dp)
                            .testTag("editar_usuario_delete_button")
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Excluir", tint = PriorityHigh)
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).testTag("editar_usuario_cancel_button"),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF1E293B)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextGrayLight)
                    ) {
                        Text("Cancelar", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            if (nome.isNotBlank() && email.isNotBlank()) {
                                onSave(
                                    user.copy(nome = nome, email = email, nivel = nivel, ativo = ativo),
                                    if (senha.isNotBlank()) senha else null
                                )
                            }
                        },
                        modifier = Modifier.weight(1.3f).testTag("editar_usuario_save_button"),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BlueAccent, contentColor = TextWhite)
                    ) {
                        Text("Salvar", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun MediaViewerDialog(
    fileUrl: String,
    fileName: String,
    onDismiss: () -> Unit
) {
    val isPdf = fileUrl.endsWith(".pdf", ignoreCase = true)
    val isImage = fileUrl.endsWith(".png", ignoreCase = true) ||
            fileUrl.endsWith(".jpg", ignoreCase = true) ||
            fileUrl.endsWith(".jpeg", ignoreCase = true) ||
            fileUrl.endsWith(".webp", ignoreCase = true) ||
            fileUrl.endsWith(".gif", ignoreCase = true)

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xCC090D16) // Translucent BgDark
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = fileName,
                            color = TextWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isPdf) "Documento PDF" else if (isImage) "Visualizador de Imagem" else "Arquivo",
                            color = TextGrayLight,
                            fontSize = 12.sp
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .background(Color(0xFF1E293B), RoundedCornerShape(50))
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fechar",
                            tint = TextWhite,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Content
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isImage) {
                        coil.compose.SubcomposeAsyncImage(
                            model = fileUrl,
                            contentDescription = fileName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                            loading = {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = BlueAccent)
                                }
                            },
                            error = {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("Falha ao carregar imagem", color = TextWhite, fontSize = 14.sp)
                                }
                            }
                        )
                    } else if (isPdf) {
                        val encodedUrl = java.net.URLEncoder.encode(fileUrl, "UTF-8")
                        val googleDocsUrl = "https://docs.google.com/gview?embedded=true&url=$encodedUrl"
                        var webViewLoading by remember { mutableStateOf(true) }

                        Box(modifier = Modifier.fillMaxSize()) {
                            AndroidView(
                                factory = { context ->
                                    WebView(context).apply {
                                        settings.apply {
                                            javaScriptEnabled = true
                                            domStorageEnabled = true
                                            builtInZoomControls = true
                                            displayZoomControls = false
                                            loadWithOverviewMode = true
                                            useWideViewPort = true
                                        }
                                        webViewClient = object : WebViewClient() {
                                            override fun onPageFinished(view: WebView?, url: String?) {
                                                super.onPageFinished(view, url)
                                                webViewLoading = false
                                            }
                                        }
                                        loadUrl(googleDocsUrl)
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )

                            if (webViewLoading) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0xFF0F172A)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CircularProgressIndicator(color = BlueAccent)
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text("Carregando PDF...", color = TextGrayLight, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    } else {
                        val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.InsertDriveFile,
                                contentDescription = null,
                                tint = BlueAccent,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Formato não suportado para visualização direta",
                                color = TextWhite,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Você pode abrir este arquivo utilizando seu navegador ou aplicativos externos.",
                                color = TextGrayLight,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = {
                                    try {
                                        uriHandler.openUri(fileUrl)
                                    } catch (e: Exception) {
                                        // ignore
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = BlueAccent)
                            ) {
                                Icon(Icons.Default.OpenInNew, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Abrir no Navegador")
                            }
                        }
                    }
                }
            }
        }
    }
}

fun createTempImageUri(context: android.content.Context): android.net.Uri {
    val tempFile = java.io.File.createTempFile("camera_photo_", ".jpg", context.cacheDir).apply {
        createNewFile()
        deleteOnExit()
    }
    val authority = "${context.packageName}.fileprovider"
    return androidx.core.content.FileProvider.getUriForFile(context, authority, tempFile)
}

fun compressImageUri(context: android.content.Context, uri: android.net.Uri): android.net.Uri {
    try {
        val contentResolver = context.contentResolver
        val inputStream = contentResolver.openInputStream(uri) ?: return uri
        
        val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
        inputStream.close()
        
        if (bitmap == null) return uri
        
        val maxDimension = 1920
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height
        val resizedBitmap = if (originalWidth > maxDimension || originalHeight > maxDimension) {
            val scale = maxDimension.toFloat() / Math.max(originalWidth, originalHeight)
            val newWidth = (originalWidth * scale).toInt()
            val newHeight = (originalHeight * scale).toInt()
            android.graphics.Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        } else {
            bitmap
        }
        
        val compressedFile = java.io.File.createTempFile("photo_compressed_", ".jpg", context.cacheDir).apply {
            deleteOnExit()
        }
        val outputStream = java.io.FileOutputStream(compressedFile)
        resizedBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 75, outputStream)
        outputStream.flush()
        outputStream.close()
        
        if (resizedBitmap != bitmap) {
            resizedBitmap.recycle()
        }
        bitmap.recycle()
        
        val authority = "${context.packageName}.fileprovider"
        return androidx.core.content.FileProvider.getUriForFile(context, authority, compressedFile)
    } catch (e: java.lang.Exception) {
        android.util.Log.e("compressImageUri", "Erro ao comprimir imagem", e)
        return uri
    }
}

