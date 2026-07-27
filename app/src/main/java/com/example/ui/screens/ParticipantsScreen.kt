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
import com.example.data.local.entity.ParticipantType
import com.example.ui.components.ExportUtils
import com.example.ui.viewmodel.PartyUiState
import com.example.ui.viewmodel.PartyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParticipantsScreen(
    uiState: PartyUiState,
    viewModel: PartyViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val activeEvent = uiState.activeEvent

    var searchQuery by remember { mutableStateOf("") }
    var selectedFamilyFilter by remember { mutableStateOf("TODAS") }

    var showAddParticipantDialog by remember { mutableStateOf(false) }
    var editingParticipant by remember { mutableStateOf<ParticipantEntity?>(null) }
    var paymentDialogParticipant by remember { mutableStateOf<ParticipantEntity?>(null) }

    val familiesList = remember(uiState.participants) {
        listOf("TODAS") + uiState.participants.map { it.familyGroup.ifBlank { "Sem Família" } }.distinct().sorted()
    }

    val filteredParticipants = remember(uiState.participants, searchQuery, selectedFamilyFilter) {
        uiState.participants.filter { p ->
            val matchesSearch = p.name.contains(searchQuery, ignoreCase = true) || p.phone.contains(searchQuery)
            val matchesFamily = selectedFamilyFilter == "TODAS" || p.familyGroup.equals(selectedFamilyFilter, ignoreCase = true)
            matchesSearch && matchesFamily
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Convidados & Rateio", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddParticipantDialog = true }) {
                        Icon(imageVector = Icons.Default.PersonAdd, contentDescription = "Adicionar Convidado")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddParticipantDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Adicionar Convidado")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Stats Header Badge
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Total", style = MaterialTheme.typography.labelSmall)
                        Text("${uiState.participants.size} pessoas", fontWeight = FontWeight.Bold)
                    }
                    Divider(modifier = Modifier.height(24.dp).width(1.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Adultos", style = MaterialTheme.typography.labelSmall)
                        Text("${uiState.financialSummary.adultCount}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Divider(modifier = Modifier.height(24.dp).width(1.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Crianças", style = MaterialTheme.typography.labelSmall)
                        Text("${uiState.financialSummary.childCount}", fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                    }
                }
            }

            // Search Bar & Filter Row
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar por nome ou telefone...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpar")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Family Group Filter Chips
            if (familiesList.size > 2) {
                ScrollableTabRow(
                    selectedTabIndex = familiesList.indexOf(selectedFamilyFilter).coerceAtLeast(0),
                    edgePadding = 0.dp,
                    divider = {}
                ) {
                    familiesList.forEach { family ->
                        FilterChip(
                            selected = (selectedFamilyFilter == family),
                            onClick = { selectedFamilyFilter = family },
                            label = { Text(family) },
                            modifier = Modifier.padding(end = 6.dp)
                        )
                    }
                }
            }

            // List of Participants
            if (filteredParticipants.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.GroupOff,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (searchQuery.isBlank()) "Nenhum convidado cadastrado ainda." else "Nenhum convidado encontrado.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredParticipants, key = { it.id }) { participant ->
                        val targetContribution = if (activeEvent != null) {
                            viewModel.calculateParticipantTarget(participant, activeEvent, uiState.participants)
                        } else 0.0

                        ParticipantItemCard(
                            participant = participant,
                            targetContribution = targetContribution,
                            onEditClick = { editingParticipant = participant },
                            onPaymentClick = { paymentDialogParticipant = participant },
                            onWhatsAppClick = {
                                val eventName = activeEvent?.title ?: "Festa"
                                val eventDate = ExportUtils.formatDate(activeEvent?.eventDateMillis ?: 0L)
                                val eventLoc = activeEvent?.location ?: ""
                                val valueText = ExportUtils.formatCurrency(targetContribution)

                                val message = activeEvent?.invitationTemplate
                                    ?.replace("{nome}", participant.name)
                                    ?.replace("{evento}", eventName)
                                    ?.replace("{data}", eventDate)
                                    ?.replace("{local}", eventLoc)
                                    ?.replace("{valor}", valueText)
                                    ?: "Olá ${participant.name}! Você está convidado para $eventName no dia $eventDate. Valor: $valueText."

                                ExportUtils.openWhatsAppMessage(context, participant.phone, message)
                            },
                            onDeleteClick = { viewModel.deleteParticipant(participant) }
                        )
                    }
                }
            }
        }

        // Add / Edit Participant Dialog
        if (showAddParticipantDialog || editingParticipant != null) {
            val isEditing = editingParticipant != null
            val itemToEdit = editingParticipant

            var name by remember { mutableStateOf(itemToEdit?.name ?: "") }
            var phone by remember { mutableStateOf(itemToEdit?.phone ?: "") }
            var familyGroup by remember { mutableStateOf(itemToEdit?.familyGroup ?: "Família Silva") }
            var type by remember { mutableStateOf(itemToEdit?.type ?: ParticipantType.ADULT) }

            AlertDialog(
                onDismissRequest = {
                    showAddParticipantDialog = false
                    editingParticipant = null
                },
                title = { Text(if (isEditing) "Editar Convidado" else "Novo Convidado") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Nome do Participante") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Telefone / WhatsApp (DDDNumeros)") },
                            placeholder = { Text("Ex: 11999998888") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = familyGroup,
                            onValueChange = { familyGroup = it },
                            label = { Text("Grupo / Nome da Família") },
                            placeholder = { Text("Ex: Família Silva, Amigos do Trabalho") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Text("Classificação:", style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            FilterChip(
                                selected = (type == ParticipantType.ADULT),
                                onClick = { type = ParticipantType.ADULT },
                                label = { Text("Adulto") }
                            )
                            FilterChip(
                                selected = (type == ParticipantType.CHILD),
                                onClick = { type = ParticipantType.CHILD },
                                label = { Text("Criança") }
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (name.isNotBlank() && activeEvent != null) {
                                val p = ParticipantEntity(
                                    id = itemToEdit?.id ?: "",
                                    eventId = activeEvent.id,
                                    name = name,
                                    phone = phone,
                                    type = type,
                                    familyGroup = familyGroup.ifBlank { "Sem Família" },
                                    paidAmount = itemToEdit?.paidAmount ?: 0.0
                                )
                                if (isEditing) {
                                    viewModel.updateParticipant(p)
                                } else {
                                    viewModel.addParticipant(p)
                                }
                            }
                            showAddParticipantDialog = false
                            editingParticipant = null
                        }
                    ) {
                        Text("Salvar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showAddParticipantDialog = false
                        editingParticipant = null
                    }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        // Give Payment Dialog ("Dar Baixa em Pagamento")
        paymentDialogParticipant?.let { participant ->
            val target = if (activeEvent != null) viewModel.calculateParticipantTarget(participant, activeEvent, uiState.participants) else 0.0
            var amountText by remember { mutableStateOf(participant.paidAmount.toString()) }

            AlertDialog(
                onDismissRequest = { paymentDialogParticipant = null },
                title = { Text("Dar Baixa no Pagamento") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Participante: ${participant.name}")
                        Text("Meta Individual: ${ExportUtils.formatCurrency(target)}")

                        OutlinedTextField(
                            value = amountText,
                            onValueChange = { amountText = it },
                            label = { Text("Valor Pago (R$)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { amountText = target.toString() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Quitar Total")
                            }
                            OutlinedButton(
                                onClick = { amountText = "0.0" },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Zerar")
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val paid = amountText.toDoubleOrNull() ?: 0.0
                            viewModel.updateParticipantPayment(participant.id, paid)
                            paymentDialogParticipant = null
                        }
                    ) {
                        Text("Confirmar Baixa")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { paymentDialogParticipant = null }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}

@Composable
private fun ParticipantItemCard(
    participant: ParticipantEntity,
    targetContribution: Double,
    onEditClick: () -> Unit,
    onPaymentClick: () -> Unit,
    onWhatsAppClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val isPaidFull = participant.paidAmount >= targetContribution && targetContribution > 0
    val isPartial = participant.paidAmount > 0 && !isPaidFull

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Badge
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (participant.type == ParticipantType.ADULT) MaterialTheme.colorScheme.primaryContainer else Color(0xFFFFF3E0)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (participant.type == ParticipantType.ADULT) Icons.Default.Person else Icons.Default.ChildCare,
                    contentDescription = null,
                    tint = if (participant.type == ParticipantType.ADULT) MaterialTheme.colorScheme.primary else Color(0xFFE65100)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = participant.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = participant.familyGroup,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${participant.type.label} • Rateio: ${ExportUtils.formatCurrency(targetContribution)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = when {
                            isPaidFull -> Color(0xFFE8F5E9)
                            isPartial -> Color(0xFFFFF8E1)
                            else -> Color(0xFFFFEBEE)
                        }
                    ) {
                        Text(
                            text = when {
                                isPaidFull -> "PAGO: ${ExportUtils.formatCurrency(participant.paidAmount)}"
                                isPartial -> "PARCIAL: ${ExportUtils.formatCurrency(participant.paidAmount)} / ${ExportUtils.formatCurrency(targetContribution)}"
                                else -> "PENDENTE: R$ 0,00"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isPaidFull -> Color(0xFF2E7D32)
                                isPartial -> Color(0xFFF57F17)
                                else -> Color(0xFFC62828)
                            },
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Row {
                IconButton(onClick = onWhatsAppClick) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = "Enviar WhatsApp",
                        tint = Color(0xFF25D366)
                    )
                }

                IconButton(onClick = onPaymentClick) {
                    Icon(
                        imageVector = Icons.Default.PriceCheck,
                        contentDescription = "Dar Baixa",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(onClick = onEditClick) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar", modifier = Modifier.size(18.dp))
                }

                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = Color(0xFFD32F2F), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
