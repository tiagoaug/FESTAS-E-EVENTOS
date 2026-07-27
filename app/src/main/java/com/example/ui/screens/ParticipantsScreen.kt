package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.ParticipantEntity
import com.example.data.local.entity.ParticipantType
import com.example.ui.components.ExportUtils
import com.example.ui.components.PaymentDialog
import com.example.ui.viewmodel.PartyUiState
import com.example.ui.viewmodel.PartyViewModel

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun ParticipantsScreen(
    uiState: PartyUiState,
    viewModel: PartyViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val activeEvent = uiState.activeEvent
    val useWhatsApp by viewModel.useWhatsApp.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFamilyFilter by remember { mutableStateOf("TODAS") }
    var familyFilterExpanded by remember { mutableStateOf(false) }

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
            com.example.ui.components.GradientFab(
                onClick = { showAddParticipantDialog = true },
                icon = Icons.Default.PersonAdd,
                contentDescription = "Adicionar Convidado"
            )
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

            // Family Group Filter Chips (em acordeão, pra não ocupar espaço fixo na tela)
            if (familiesList.size > 1) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { familyFilterExpanded = !familyFilterExpanded }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Filtrar por Família: $selectedFamilyFilter",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Default.ExpandMore,
                            contentDescription = if (familyFilterExpanded) "Recolher" else "Expandir",
                            modifier = Modifier.rotate(if (familyFilterExpanded) 180f else 0f)
                        )
                    }

                    AnimatedVisibility(visible = familyFilterExpanded) {
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                                .padding(bottom = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            familiesList.forEach { family ->
                                FilterChip(
                                    selected = (selectedFamilyFilter == family),
                                    onClick = {
                                        selectedFamilyFilter = family
                                        familyFilterExpanded = false
                                    },
                                    label = { Text(family) }
                                )
                            }
                        }
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
                            showWhatsApp = useWhatsApp,
                            onEditClick = { editingParticipant = participant },
                            onPaymentClick = { paymentDialogParticipant = participant },
                            onToggleConfirmed = { viewModel.toggleParticipantConfirmed(participant.id, it) },
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

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun ParticipantItemCard(
    participant: ParticipantEntity,
    targetContribution: Double,
    showWhatsApp: Boolean,
    onEditClick: () -> Unit,
    onPaymentClick: () -> Unit,
    onToggleConfirmed: (Boolean) -> Unit,
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
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
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

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
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

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (participant.confirmed) Color(0xFFE8F5E9) else Color(0xFFFFF8E1),
                            modifier = Modifier.clickable { onToggleConfirmed(!participant.confirmed) }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = if (participant.confirmed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (participant.confirmed) Color(0xFF2E7D32) else Color(0xFFF57F17),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = if (participant.confirmed) "PRESENÇA CONFIRMADA" else "AGUARDANDO CONFIRMAÇÃO",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (participant.confirmed) Color(0xFF2E7D32) else Color(0xFFF57F17)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action capsule: botões em pílula com ícone + legenda, mirando o estilo web.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (showWhatsApp) {
                    ActionPillButton(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Chat,
                        label = "WhatsApp",
                        tint = Color(0xFF25D366),
                        onClick = onWhatsAppClick
                    )
                }
                ActionPillButton(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.CheckCircle,
                    label = "Receber",
                    tint = Color(0xFF2E7D32),
                    onClick = onPaymentClick
                )
                ActionPillButton(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Edit,
                    label = "Editar",
                    tint = MaterialTheme.colorScheme.primary,
                    onClick = onEditClick
                )
                ActionPillButton(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Delete,
                    label = "Excluir",
                    tint = Color(0xFFD32F2F),
                    onClick = onDeleteClick
                )
            }
        }
    }
}

@Composable
private fun ActionPillButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clip(RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = tint,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
