package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.entity.EventEntity
import com.example.ui.components.BudgetVsSpentChart
import com.example.ui.components.CountdownCard
import com.example.ui.components.ExportUtils
import com.example.ui.components.FinancialSummaryCard
import com.example.ui.components.LocationMapCard
import com.example.ui.components.ShareCenterDialog
import com.example.ui.viewmodel.PartyUiState
import com.example.ui.viewmodel.PartyViewModel
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
    onNavigateToInvitations: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    var showEventSelector by remember { mutableStateOf(false) }
    var showShareCenter by remember { mutableStateOf(false) }
    val useWhatsApp by viewModel.useWhatsApp.collectAsStateWithLifecycle()

    val activeEvent = uiState.activeEvent

    Scaffold(
        topBar = {
            // Cabeçalho customizado (em vez de TopAppBar de altura fixa) para que
            // títulos longos + subtítulo nunca fiquem cortados/espremidos.
            Surface(color = MaterialTheme.colorScheme.surface) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
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
                    IconButton(onClick = onNavigateToSetup) {
                        Icon(
                            imageVector = Icons.Default.EditCalendar,
                            contentDescription = "Configurar Evento"
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Configurações"
                        )
                    }
                }
            }
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
                    .nestedScroll(rememberNestedScrollInteropConnection())
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Countdown Hero Card
                CountdownCard(
                    event = activeEvent,
                    onGoogleCalendarClick = {
                        ExportUtils.createGoogleCalendarIntent(context, activeEvent)
                    }
                )

                // Location Map (minimized by default)
                LocationMapCard(location = activeEvent.location)

                // Quick Actions & Sharing Banner
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

                        if (useWhatsApp) {
                            Button(
                                onClick = onNavigateToInvitations,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                            ) {
                                Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Enviar Convites", style = MaterialTheme.typography.labelMedium, color = Color.White)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        OutlinedButton(
                            onClick = { showShareCenter = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Painel de Compartilhamento", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                // 2. Financial Summary Card
                FinancialSummaryCard(summary = uiState.financialSummary)

                // 3. Budget vs Spent Category Chart
                BudgetVsSpentChart(
                    budget = activeEvent.budget,
                    expenses = uiState.expenses,
                    categories = uiState.categories
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

                    if (useWhatsApp) {
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
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Event Selector Dialog ("Meus Eventos"), no estilo da versão web
        if (showEventSelector) {
            Dialog(onDismissRequest = { showEventSelector = false }) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                            .heightIn(max = 560.dp)
                    ) {
                        Text(
                            text = "Meus Eventos",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primaryContainer,
                                            MaterialTheme.colorScheme.primary
                                        )
                                    )
                                )
                                .clickable {
                                    showEventSelector = false
                                    onNavigateToSetup()
                                }
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Adicionar Novo Evento",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Column(
                            modifier = Modifier.verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            uiState.events.forEach { ev ->
                                val isActive = ev.id == activeEvent?.id
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                            else MaterialTheme.colorScheme.surface
                                        )
                                        .border(
                                            width = if (isActive) 1.5.dp else 1.dp,
                                            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .clickable {
                                            viewModel.selectActiveEvent(ev.id)
                                            showEventSelector = false
                                        }
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = ev.title,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.CalendarMonth,
                                                contentDescription = null,
                                                modifier = Modifier.size(12.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "${ExportUtils.formatDateOnly(ev.eventDateMillis)} • ${ExportUtils.formatCurrency(ev.budget)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    if (isActive) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedButton(
                            onClick = { showEventSelector = false },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Fechar")
                        }
                    }
                }
            }
        }

        // Centro de Compartilhamento
        if (showShareCenter && activeEvent != null) {
            ShareCenterDialog(
                event = activeEvent,
                participants = uiState.participants,
                expenses = uiState.expenses,
                categories = uiState.categories,
                summary = uiState.financialSummary,
                viewModel = viewModel,
                onDismiss = { showShareCenter = false }
            )
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
