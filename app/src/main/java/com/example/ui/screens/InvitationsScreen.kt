package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.ExportUtils
import com.example.ui.viewmodel.PartyUiState
import com.example.ui.viewmodel.PartyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvitationsScreen(
    uiState: PartyUiState,
    viewModel: PartyViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val activeEvent = uiState.activeEvent
    val useWhatsApp by viewModel.useWhatsApp.collectAsStateWithLifecycle()

    var templateText by remember(activeEvent) {
        mutableStateOf(
            activeEvent?.invitationTemplate
                ?: "Olá {nome}! Você está convidado(a) para {evento} no dia {data} no local {local}. Sua contribuição: {valor}. Confirme sua presença!"
        )
    }

    var isTemplateSaved by remember { mutableStateOf(false) }

    val sampleName = "Mariane Silva"
    val sampleEventName = activeEvent?.title ?: "Aniversário de 30 Anos"
    val sampleDate = ExportUtils.formatDate(activeEvent?.eventDateMillis ?: System.currentTimeMillis())
    val sampleLocation = activeEvent?.location?.ifEmpty { "Espaço Festas & Cia" } ?: "Espaço Festas & Cia"
    val sampleValue = ExportUtils.formatCurrency(50.0)

    val livePreviewMessage = remember(templateText, activeEvent) {
        templateText
            .replace("{nome}", sampleName)
            .replace("{evento}", sampleEventName)
            .replace("{data}", sampleDate)
            .replace("{local}", sampleLocation)
            .replace("{valor}", sampleValue)
    }

    if (!useWhatsApp) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Convites & WhatsApp", fontWeight = FontWeight.Bold) },
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
                    .padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Chat,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "O WhatsApp está desativado em Configurações.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onNavigateToSettings) {
                    Text("Abrir Configurações")
                }
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Convites & WhatsApp", fontWeight = FontWeight.Bold) },
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

            // Template Config Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Modelo de Mensagem de Convite",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Use as variáveis: {nome}, {evento}, {data}, {local}, {valor}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = templateText,
                        onValueChange = {
                            templateText = it
                            isTemplateSaved = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Variables Quick Insertion Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("{nome}", "{evento}", "{data}", "{local}", "{valor}").forEach { tag ->
                            SuggestionChip(
                                onClick = {
                                    templateText += " $tag"
                                },
                                label = { Text(tag, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isTemplateSaved) {
                            Text(
                                text = "✓ Modelo salvo no evento!",
                                color = Color(0xFF2E7D32),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }

                        Button(
                            onClick = {
                                if (activeEvent != null) {
                                    viewModel.updateEvent(activeEvent.copy(invitationTemplate = templateText))
                                    isTemplateSaved = true
                                }
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Salvar Modelo")
                        }
                    }
                }
            }

            // Live Preview Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFDCF8C6).copy(alpha = 0.6f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = null,
                            tint = Color(0xFF075E54),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Pré-visualização da Mensagem (WhatsApp)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF075E54)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = livePreviewMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Black
                    )
                }
            }

            Text(
                text = "Enviar Convites Individuais (${uiState.participants.size} participantes)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Participants WhatsApp Send List
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(uiState.participants, key = { it.id }) { participant ->
                    val targetValue = if (activeEvent != null) {
                        viewModel.calculateParticipantTarget(participant, activeEvent, uiState.participants)
                    } else 0.0

                    val participantMessage = templateText
                        .replace("{nome}", participant.name)
                        .replace("{evento}", activeEvent?.title ?: "Festa")
                        .replace("{data}", ExportUtils.formatDate(activeEvent?.eventDateMillis ?: 0L))
                        .replace("{local}", activeEvent?.location ?: "")
                        .replace("{valor}", ExportUtils.formatCurrency(targetValue))

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
                                    text = "Tel: ${participant.phone.ifBlank { "Não informado" }} • ${participant.familyGroup}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Button(
                                onClick = {
                                    ExportUtils.openWhatsAppMessage(context, participant.phone, participantMessage)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("WhatsApp", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
