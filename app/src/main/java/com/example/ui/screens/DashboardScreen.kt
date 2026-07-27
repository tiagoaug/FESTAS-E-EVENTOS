package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.local.entity.EventEntity
import com.example.ui.components.BudgetVsSpentChart
import com.example.ui.components.CountdownCard
import com.example.ui.components.ExportUtils
import com.example.ui.components.FinancialSummaryCard
import com.example.ui.viewmodel.PartyUiState
import com.example.ui.viewmodel.PartyViewModel
import dev.shreyaspatil.capturable.capturable
import dev.shreyaspatil.capturable.controller.rememberCaptureController
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun DashboardScreen(
    uiState: PartyUiState,
    viewModel: PartyViewModel,
    onNavigateToSetup: () -> Unit,
    onNavigateToParticipants: () -> Unit,
    onNavigateToExpenses: () -> Unit,
    onNavigateToPayments: () -> Unit,
    onNavigateToInvitations: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val captureController = rememberCaptureController()
    var previewBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var showEventSelector by remember { mutableStateOf(false) }

    val activeEvent = uiState.activeEvent

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = activeEvent?.title ?: "Festas & Eventos",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            if (activeEvent != null) {
                                Text(
                                    text = "🎉 ${activeEvent.eventType} • ${activeEvent.location}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (uiState.events.size > 1) {
                            IconButton(onClick = { showEventSelector = true }) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Selecionar Evento"
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSetup) {
                        Icon(
                            imageVector = Icons.Default.EditCalendar,
                            contentDescription = "Configurar Evento"
                        )
                    }
                    IconButton(onClick = {
                        if (activeEvent != null) {
                            ExportUtils.exportFinancialSummaryPdf(
                                context = context,
                                event = activeEvent,
                                summary = uiState.financialSummary,
                                participants = uiState.participants,
                                expenses = uiState.expenses
                            )
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "Exportar PDF",
                            tint = Color(0xFFD32F2F)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        if (activeEvent == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Nenhum evento ativo no momento.")
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onNavigateToSetup) {
                        Text("Criar Novo Evento")
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
                    .capturable(captureController),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Countdown Hero Card
                CountdownCard(
                    event = activeEvent,
                    onGoogleCalendarClick = {
                        ExportUtils.createGoogleCalendarIntent(context, activeEvent)
                    }
                )

                // Quick Export & Calendar Actions Banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Ações Rápidas & Exportação",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            OutlinedButton(
                                onClick = {
                                    coroutineScope.launch {
                                        try {
                                            previewBitmap = captureController.captureAsync().await()
                                        } catch (error: Throwable) {
                                            android.widget.Toast.makeText(
                                                context,
                                                "Erro ao capturar tela: ${error.message}",
                                                android.widget.Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Exportar JPG", style = MaterialTheme.typography.labelMedium)
                            }

                            Button(
                                onClick = onNavigateToInvitations,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                            ) {
                                Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Enviar Convites", style = MaterialTheme.typography.labelMedium, color = Color.White)
                            }
                        }
                    }
                }

                // 2. Financial Summary Card
                FinancialSummaryCard(summary = uiState.financialSummary)

                // 3. Budget vs Spent Category Chart
                BudgetVsSpentChart(
                    budget = activeEvent.budget,
                    expenses = uiState.expenses
                )

                // Module Navigation Shortcut Grid
                Text(
                    text = "Ações Rápidas & Módulos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ShortcutCard(
                        title = "Convidados",
                        subtitle = "${uiState.financialSummary.totalParticipants} Cadastrados",
                        icon = Icons.Default.People,
                        bgColor = com.example.ui.theme.ActionBlueContainer,
                        iconTint = com.example.ui.theme.ActionBlueOn,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToParticipants
                    )

                    ShortcutCard(
                        title = "Gastos",
                        subtitle = ExportUtils.formatCurrency(uiState.financialSummary.totalSpent),
                        icon = Icons.Default.ShoppingCart,
                        bgColor = com.example.ui.theme.ActionSkyContainer,
                        iconTint = com.example.ui.theme.ActionSkyOn,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToExpenses
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ShortcutCard(
                        title = "Rateio & Receber",
                        subtitle = "Falta ${ExportUtils.formatCurrency(uiState.financialSummary.missingCollection)}",
                        icon = Icons.Default.Payments,
                        bgColor = com.example.ui.theme.EmeraldSuccessContainer,
                        iconTint = Color(0xFF2E7D32),
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToPayments
                    )

                    ShortcutCard(
                        title = "Convites WhatsApp",
                        subtitle = "Modelos & Envio",
                        icon = Icons.Default.Chat,
                        bgColor = com.example.ui.theme.ActionPinkContainer,
                        iconTint = com.example.ui.theme.ActionPinkOn,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToInvitations
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Event Selector Dialog
        if (showEventSelector) {
            AlertDialog(
                onDismissRequest = { showEventSelector = false },
                title = { Text("Selecionar Evento") },
                text = {
                    Column {
                        uiState.events.forEach { ev ->
                            TextButton(
                                onClick = {
                                    viewModel.selectActiveEvent(ev.id)
                                    showEventSelector = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = ev.title,
                                        fontWeight = if (ev.id == activeEvent?.id) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Text(
                                        text = ExportUtils.formatDateOnly(ev.eventDateMillis),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showEventSelector = false }) {
                        Text("Fechar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showEventSelector = false
                        onNavigateToSetup()
                    }) {
                        Text("Novo Evento")
                    }
                }
            )
        }

        // JPG Export Preview Dialog
        previewBitmap?.let { bitmap ->
            Dialog(onDismissRequest = { previewBitmap = null }) {
                Card(shape = RoundedCornerShape(20.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Prévia da Imagem",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Image(
                            bitmap = bitmap,
                            contentDescription = "Prévia do resumo do evento",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 420.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { previewBitmap = null }) {
                                Text("Cancelar")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(onClick = {
                                ExportUtils.shareBitmapAsJpg(
                                    context,
                                    bitmap.asAndroidBitmap(),
                                    "Resumo: ${activeEvent?.title ?: ""}"
                                )
                                previewBitmap = null
                            }) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Compartilhar")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShortcutCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    bgColor: Color,
    iconTint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(bgColor, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
