package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.ParticipantEntity
import com.example.ui.components.ExportUtils
import com.example.ui.components.PaymentDialog
import com.example.ui.viewmodel.PartyUiState
import com.example.ui.viewmodel.PartyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentsScreen(
    uiState: PartyUiState,
    viewModel: PartyViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val activeEvent = uiState.activeEvent
    val summary = uiState.financialSummary

    var paymentDialogParticipant by remember { mutableStateOf<ParticipantEntity?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredParticipants = remember(uiState.participants, searchQuery) {
        if (searchQuery.isBlank()) {
            uiState.participants
        } else {
            uiState.participants.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.familyGroup.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    // Grouping by Family for detailed breakdown
    val familyGroupSummary = remember(uiState.participants, activeEvent) {
        uiState.participants.groupBy { it.familyGroup.ifBlank { "Sem Família" } }
            .mapValues { (_, familyMembers) ->
                val familyTarget = familyMembers.sumOf { p ->
                    if (activeEvent != null) viewModel.calculateParticipantTarget(p, activeEvent, uiState.participants) else 0.0
                }
                val familyPaid = familyMembers.sumOf { it.paidAmount }
                val isFullyPaid = familyPaid >= familyTarget && familyTarget > 0
                Triple(familyMembers.size, familyTarget, familyPaid)
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Módulo de Recebimentos", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // Top Header Collection Summary
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1A29)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Arrecadação Geral do Evento",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Arrecadado", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                            Text(
                                text = ExportUtils.formatCurrency(summary.totalCollected),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2ECC71)
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("Valor Faltante", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                            Text(
                                text = ExportUtils.formatCurrency(summary.missingCollection),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (summary.missingCollection > 0) Color(0xFFFF6B6B) else Color(0xFF2ECC71)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val collectionPercentage = if (summary.totalExpectedCollection > 0) (summary.totalCollected / summary.totalExpectedCollection).coerceIn(0.0, 1.0).toFloat() else 0f
                    LinearProgressIndicator(
                        progress = { collectionPercentage },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = Color(0xFF2ECC71),
                        trackColor = Color.White.copy(alpha = 0.2f)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Meta de Arrecadação: ${ExportUtils.formatCurrency(summary.totalExpectedCollection)} • ${(collectionPercentage * 100).toInt()}% Concluído",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar participante ou família...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Participants List for Giving Payment Discharge
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filteredParticipants, key = { it.id }) { participant ->
                    val target = if (activeEvent != null) viewModel.calculateParticipantTarget(participant, activeEvent, uiState.participants) else 0.0
                    val isPaidFull = participant.paidAmount >= target && target > 0
                    val isPartial = participant.paidAmount > 0 && !isPaidFull

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = participant.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${participant.familyGroup} • Meta: ${ExportUtils.formatCurrency(target)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Pago até agora: ${ExportUtils.formatCurrency(participant.paidAmount)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPaidFull) Color(0xFF2E7D32) else if (isPartial) Color(0xFFF57F17) else Color(0xFFC62828)
                                )
                            }

                            Button(
                                onClick = { paymentDialogParticipant = participant },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isPaidFull) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(if (isPaidFull) "Alterar" else "Dar Baixa", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Dialog for Payment Update
        paymentDialogParticipant?.let { participant ->
            val target = if (activeEvent != null) viewModel.calculateParticipantTarget(participant, activeEvent, uiState.participants) else 0.0
            PaymentDialog(
                participant = participant,
                target = target,
                onDismiss = { paymentDialogParticipant = null },
                onConfirm = { paid ->
                    viewModel.updateParticipantPayment(participant.id, paid)
                    paymentDialogParticipant = null
                }
            )
        }
    }
}
