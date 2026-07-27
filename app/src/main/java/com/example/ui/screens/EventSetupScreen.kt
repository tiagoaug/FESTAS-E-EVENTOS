package com.example.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.CostShareMode
import com.example.data.local.entity.EventEntity
import com.example.ui.components.ExportUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.ui.viewmodel.PartyUiState
import com.example.ui.viewmodel.PartyViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventSetupScreen(
    uiState: PartyUiState,
    viewModel: PartyViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val activeEvent = uiState.activeEvent

    var title by remember(activeEvent) { mutableStateOf(activeEvent?.title ?: "") }
    var eventType by remember(activeEvent) { mutableStateOf(activeEvent?.eventType ?: "Aniversário") }
    var location by remember(activeEvent) { mutableStateOf(activeEvent?.location ?: "") }
    var budgetText by remember(activeEvent) { mutableStateOf(activeEvent?.budget?.toString() ?: "") }
    var eventDateMillis by remember(activeEvent) { mutableStateOf(activeEvent?.eventDateMillis ?: (System.currentTimeMillis() + 7 * 24 * 3600 * 1000L)) }

    var costShareMode by remember(activeEvent) { mutableStateOf(activeEvent?.costShareMode ?: CostShareMode.EQUAL) }
    var fixedAdultText by remember(activeEvent) { mutableStateOf(activeEvent?.fixedAdultPrice?.toString() ?: "50.0") }
    var fixedChildText by remember(activeEvent) { mutableStateOf(activeEvent?.fixedChildPrice?.toString() ?: "20.0") }

    val eventTypesList = listOf("Aniversário", "Casamento", "Chá de Bebê / Panela", "Formatura", "Churrasco", "Outro")

    var showDeleteConfirmDialog by remember { mutableStateOf<EventEntity?>(null) }

    val calendar = Calendar.getInstance().apply { timeInMillis = eventDateMillis }

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            eventDateMillis = calendar.timeInMillis
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    val timePickerDialog = TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
            calendar.set(Calendar.MINUTE, minute)
            eventDateMillis = calendar.timeInMillis
        },
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        true
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cadastro & Rateio do Evento", fontWeight = FontWeight.Bold) },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Form Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = if (activeEvent == null) "Novo Evento" else "Editar Dados do Evento",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Nome da Festa / Evento") },
                        placeholder = { Text("Ex: Aniversário de Lucas, Churrasco de Fim de Ano") },
                        leadingIcon = { Icon(Icons.Default.Celebration, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Event Type Selection Chips
                    Text(
                        text = "Tipo de Evento",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        eventTypesList.take(3).forEach { type ->
                            FilterChip(
                                selected = (eventType == type),
                                onClick = { eventType = type },
                                label = { Text(type, style = MaterialTheme.typography.bodySmall) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        eventTypesList.drop(3).forEach { type ->
                            FilterChip(
                                selected = (eventType == type),
                                onClick = { eventType = type },
                                label = { Text(type, style = MaterialTheme.typography.bodySmall) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Local do Evento") },
                        placeholder = { Text("Ex: Salão de Festas, Chácara Recanto") },
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = budgetText,
                        onValueChange = { budgetText = it },
                        label = { Text("Orçamento Total (R$)") },
                        placeholder = { Text("Ex: 2500.00") },
                        leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Date & Time Picker Row
                    Text(
                        text = "Data e Hora",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { datePickerDialog.show() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(ExportUtils.formatDateOnly(eventDateMillis))
                        }

                        OutlinedButton(
                            onClick = { timePickerDialog.show() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(eventDateMillis)))
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 4.dp))

                    // Cost Sharing Mode Section
                    Text(
                        text = "Sistema de Rateio de Custos",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Column(Modifier.selectableGroup()) {
                        // Option 1: Equal Division
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = (costShareMode == CostShareMode.EQUAL),
                                    onClick = { costShareMode = CostShareMode.EQUAL },
                                    role = Role.RadioButton
                                )
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (costShareMode == CostShareMode.EQUAL),
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Divisão Igualitária entre todos",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Divide o orçamento total igualmente por todos os participantes cadastrados.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Option 2: Fixed Values per Adult and Child
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = (costShareMode == CostShareMode.FIXED_TYPE),
                                    onClick = { costShareMode = CostShareMode.FIXED_TYPE },
                                    role = Role.RadioButton
                                )
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (costShareMode == CostShareMode.FIXED_TYPE),
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Valores Fixos (Adulto vs Criança)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Define um valor fixo em R$ para adultos e outro valor em R$ para crianças.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Option 3: Organizer assumes total cost
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = (costShareMode == CostShareMode.ORGANIZER_ONLY),
                                    onClick = { costShareMode = CostShareMode.ORGANIZER_ONLY },
                                    role = Role.RadioButton
                                )
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (costShareMode == CostShareMode.ORGANIZER_ONLY),
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Organizador Assume Custo Total",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Convidados não pagam nada (R$ 0,00). O custo é 100% do organizador.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Option 4: Define later, calculated from the participant list
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = (costShareMode == CostShareMode.DEFINE_LATER),
                                    onClick = { costShareMode = CostShareMode.DEFINE_LATER },
                                    role = Role.RadioButton
                                )
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (costShareMode == CostShareMode.DEFINE_LATER),
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Definir Depois (calcular pela lista)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Não define valores agora. O valor de adulto e criança é calculado automaticamente com base no orçamento e na lista de convidados (criança paga metade do valor do adulto).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Fixed values fields if FIXED_TYPE selected
                    if (costShareMode == CostShareMode.FIXED_TYPE) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = fixedAdultText,
                                onValueChange = { fixedAdultText = it },
                                label = { Text("Valor Adulto (R$)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = fixedChildText,
                                onValueChange = { fixedChildText = it },
                                label = { Text("Valor Criança (R$)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Save Button
                    Button(
                        onClick = {
                            val parsedBudget = budgetText.toDoubleOrNull() ?: 0.0
                            val parsedAdultPrice = fixedAdultText.toDoubleOrNull() ?: 0.0
                            val parsedChildPrice = fixedChildText.toDoubleOrNull() ?: 0.0

                            val eventToSave = EventEntity(
                                id = activeEvent?.id ?: "",
                                title = title.ifBlank { "Minha Festa" },
                                eventType = eventType,
                                location = location,
                                budget = parsedBudget,
                                eventDateMillis = eventDateMillis,
                                costShareMode = costShareMode,
                                fixedAdultPrice = parsedAdultPrice,
                                fixedChildPrice = parsedChildPrice
                            )

                            if (activeEvent == null) {
                                viewModel.addEvent(eventToSave)
                            } else {
                                viewModel.updateEvent(eventToSave)
                            }
                            onNavigateBack()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (activeEvent == null) "Criar Evento" else "Salvar Alterações", fontWeight = FontWeight.Bold)
                    }

                    if (activeEvent != null) {
                        OutlinedButton(
                            onClick = {
                                ExportUtils.createGoogleCalendarIntent(context, activeEvent)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sincronizar com Google Agenda (com Lembretes)")
                        }
                    }
                }
            }

            // List of Existing Events to Switch/Delete
            if (uiState.events.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Todos os Eventos Cadastrados",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        uiState.events.forEach { ev ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = ev.title,
                                        fontWeight = if (ev.id == activeEvent?.id) FontWeight.Bold else FontWeight.Normal,
                                        color = if (ev.id == activeEvent?.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${ExportUtils.formatDateOnly(ev.eventDateMillis)} • ${ExportUtils.formatCurrency(ev.budget)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row {
                                    if (ev.id != activeEvent?.id) {
                                        TextButton(onClick = { viewModel.selectActiveEvent(ev.id) }) {
                                            Text("Ativar")
                                        }
                                    }
                                    IconButton(onClick = { showDeleteConfirmDialog = ev }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Excluir Evento", tint = Color(0xFFD32F2F))
                                    }
                                }
                            }
                            Divider()
                        }
                    }
                }
            }
        }

        // Delete Event Dialog
        showDeleteConfirmDialog?.let { eventToDelete ->
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = null },
                title = { Text("Excluir Evento?") },
                text = { Text("Tem certeza que deseja excluir '${eventToDelete.title}'? Todos os participantes e gastos associados também serão apagados.") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteEvent(eventToDelete)
                        showDeleteConfirmDialog = null
                    }) {
                        Text("Excluir", color = Color(0xFFD32F2F))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmDialog = null }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}
