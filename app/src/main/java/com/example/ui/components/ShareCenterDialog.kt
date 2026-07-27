package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.Image as ImageComposable
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.EventEntity
import com.example.data.local.entity.ExpenseEntity
import com.example.data.local.entity.FALLBACK_CATEGORY_LABEL
import com.example.data.local.entity.ParticipantEntity
import com.example.data.local.entity.CostShareMode
import com.example.ui.viewmodel.FinancialSummary
import com.example.ui.viewmodel.PartyViewModel
import dev.shreyaspatil.capturable.capturable
import dev.shreyaspatil.capturable.controller.rememberCaptureController
import kotlinx.coroutines.launch

private const val SHARE_PREFS_NAME = "festas_eventos_share_options"

private class ShareOptionsState(context: Context) {
    private val prefs = context.getSharedPreferences(SHARE_PREFS_NAME, Context.MODE_PRIVATE)

    var groupByFamily by mutableStateOf(prefs.getBoolean("groupByFamily", false))
    var alphabetical by mutableStateOf(prefs.getBoolean("alphabetical", true))
    var showTotals by mutableStateOf(prefs.getBoolean("showTotals", true))
    var showGuestList by mutableStateOf(prefs.getBoolean("showGuestList", true))
    var showAttendanceList by mutableStateOf(prefs.getBoolean("showAttendanceList", false))
    var showBudget by mutableStateOf(prefs.getBoolean("showBudget", true))
    var showExpenseItems by mutableStateOf(prefs.getBoolean("showExpenseItems", false))
    var showCountdown by mutableStateOf(prefs.getBoolean("showCountdown", true))
    var notes by mutableStateOf(prefs.getString("notes", "") ?: "")

    fun persist() {
        prefs.edit()
            .putBoolean("groupByFamily", groupByFamily)
            .putBoolean("alphabetical", alphabetical)
            .putBoolean("showTotals", showTotals)
            .putBoolean("showGuestList", showGuestList)
            .putBoolean("showAttendanceList", showAttendanceList)
            .putBoolean("showBudget", showBudget)
            .putBoolean("showExpenseItems", showExpenseItems)
            .putBoolean("showCountdown", showCountdown)
            .putString("notes", notes)
            .apply()
    }

    fun enableFullPartyData() {
        showTotals = true
        showGuestList = true
        showAttendanceList = true
        showBudget = true
        showExpenseItems = true
        showCountdown = true
        persist()
    }
}

private fun daysUntil(eventDateMillis: Long): Long {
    val diff = eventDateMillis - System.currentTimeMillis()
    return Math.ceil(diff / (1000.0 * 60 * 60 * 24)).toLong()
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ShareCenterDialog(
    event: EventEntity,
    participants: List<ParticipantEntity>,
    expenses: List<ExpenseEntity>,
    categories: List<CategoryEntity>,
    summary: FinancialSummary,
    viewModel: PartyViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val options = remember { ShareOptionsState(context) }
    var copied by remember { mutableStateOf(false) }
    var showLivePreview by remember { mutableStateOf(false) }
    var previewBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    val captureController = rememberCaptureController()

    fun update(block: () -> Unit) {
        block()
        options.persist()
    }

    val isOrganizerOnly = event.costShareMode == CostShareMode.ORGANIZER_ONLY

    val sortedParticipants = remember(participants, options.alphabetical) {
        if (options.alphabetical) participants.sortedBy { it.name.lowercase() } else participants
    }
    val groupedByFamily = remember(sortedParticipants, options.groupByFamily) {
        if (options.groupByFamily) {
            val families = sortedParticipants.map { it.familyGroup.ifBlank { "Sem Família" } }.distinct()
                .let { if (options.alphabetical) it.sorted() else it }
            families.map { fam -> fam to sortedParticipants.filter { (it.familyGroup.ifBlank { "Sem Família" }) == fam } }
        } else {
            listOf(null to sortedParticipants)
        }
    }

    val paid = remember(participants, event) {
        participants.filter { p ->
            val target = viewModel.calculateParticipantTarget(p, event, participants)
            target > 0 && p.paidAmount >= target
        }
    }
    val pending = participants - paid.toSet()
    val confirmedGuests = participants.filter { it.confirmed }
    val awaitingGuests = participants.filter { !it.confirmed }
    val paidExpenses = expenses.filter { it.isPaid }
    val pendingExpenses = expenses.filter { !it.isPaid }

    fun buildShareText(): String {
        val lines = mutableListOf<String>()
        lines += "🎉 ${event.title}"
        lines += "📅 ${ExportUtils.formatDate(event.eventDateMillis)}"
        if (event.location.isNotBlank()) lines += "📍 ${event.location}"
        if (options.showCountdown) {
            val days = daysUntil(event.eventDateMillis)
            lines += "⏰ " + when {
                days > 0 -> "Faltam $days dia(s)"
                days == 0L -> "É hoje!"
                else -> "Evento já realizado"
            }
        }
        lines += ""
        if (options.showTotals) {
            lines += "📊 TOTAIS"
            lines += "Convidados: ${participants.size}"
            lines += "Confirmados: ${confirmedGuests.size}"
            if (!isOrganizerOnly) {
                lines += "Pagaram: ${paid.size}"
                lines += "Faltam Pagar: ${pending.size}"
            }
            lines += ""
        }
        if (options.showBudget) {
            lines += "💰 RESUMO FINANCEIRO"
            lines += "Orçamento: ${ExportUtils.formatCurrency(summary.budget)}"
            lines += "Total Gasto: ${ExportUtils.formatCurrency(summary.totalSpent)}"
            lines += "Saldo: ${ExportUtils.formatCurrency(summary.budget - summary.totalSpent)}"
            if (!isOrganizerOnly) {
                lines += "Arrecadado: ${ExportUtils.formatCurrency(summary.totalCollected)}"
                lines += "Falta Arrecadar: ${ExportUtils.formatCurrency(summary.missingCollection)}"
            }
            lines += ""
        }
        if (options.showGuestList) {
            lines += "👥 CONVIDADOS (${participants.size})"
            if (participants.isEmpty()) {
                lines += "Nenhum convidado cadastrado ainda."
            } else {
                groupedByFamily.forEach { (family, members) ->
                    if (family != null) lines += "— $family —"
                    members.forEach { p -> lines += "- ${p.name}${if (p.confirmed) " ✅" else ""}" }
                }
            }
            lines += ""
        }
        if (options.showAttendanceList) {
            lines += "✅ PRESENÇA CONFIRMADA (${confirmedGuests.size})"
            if (confirmedGuests.isEmpty()) lines += "Ninguém confirmou presença ainda."
            else confirmedGuests.forEach { lines += "- ${it.name}" }
            lines += ""
            lines += "❔ AGUARDANDO CONFIRMAÇÃO (${awaitingGuests.size})"
            if (awaitingGuests.isEmpty()) lines += "Todos já confirmaram presença!"
            else awaitingGuests.forEach { lines += "- ${it.name}" }
            lines += ""
        }
        if (!isOrganizerOnly) {
            lines += "✅ JÁ PAGARAM (${paid.size})"
            if (paid.isEmpty()) lines += "Ninguém quitou o valor ainda."
            else paid.forEach { lines += "- ${it.name}: ${ExportUtils.formatCurrency(it.paidAmount)}" }
            lines += ""
            lines += "⏳ FALTAM PAGAR (${pending.size})"
            if (pending.isEmpty()) lines += "Todo mundo está em dia!"
            else pending.forEach { p ->
                val target = viewModel.calculateParticipantTarget(p, event, participants)
                val missing = maxOf(0.0, target - p.paidAmount)
                lines += "- ${p.name}: falta ${ExportUtils.formatCurrency(missing)}"
            }
            lines += ""
        }
        if (options.showExpenseItems) {
            lines += "✅ ITENS PAGOS (${paidExpenses.size})"
            if (paidExpenses.isEmpty()) lines += "Nenhum item pago ainda."
            else paidExpenses.forEach { lines += "- ${it.title} (${categories.find { c -> c.id == it.category }?.name ?: FALLBACK_CATEGORY_LABEL}): ${ExportUtils.formatCurrency(it.amount)}" }
            lines += ""
            lines += "⏳ ITENS A PAGAR (${pendingExpenses.size})"
            if (pendingExpenses.isEmpty()) lines += "Nenhum item pendente!"
            else pendingExpenses.forEach { lines += "- ${it.title} (${categories.find { c -> c.id == it.category }?.name ?: FALLBACK_CATEGORY_LABEL}): ${ExportUtils.formatCurrency(it.amount)}" }
            lines += ""
        }
        if (options.notes.isNotBlank()) {
            lines += "📝 OBSERVAÇÕES"
            lines += options.notes.trim()
        }
        return lines.joinToString("\n").trim()
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .heightIn(max = 640.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Centro de Compartilhamento",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { update { options.enableFullPartyData() } },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.height(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Compartilhar Dados Completos da Festa")
                }

                Spacer(modifier = Modifier.height(8.dp))

                CheckboxRow("Separar convidados por família", options.groupByFamily) { update { options.groupByFamily = it } }
                CheckboxRow("Ordem alfabética", options.alphabetical) { update { options.alphabetical = it } }
                CheckboxRow("Mostrar totais", options.showTotals) { update { options.showTotals = it } }
                CheckboxRow("Mostrar lista de convidados", options.showGuestList) { update { options.showGuestList = it } }
                CheckboxRow("Mostrar lista de presença", options.showAttendanceList) { update { options.showAttendanceList = it } }
                CheckboxRow("Mostrar orçamento e gastos", options.showBudget) { update { options.showBudget = it } }
                CheckboxRow("Mostrar itens pagos e a pagar", options.showExpenseItems) { update { options.showExpenseItems = it } }
                CheckboxRow("Mostrar contagem regressiva", options.showCountdown) { update { options.showCountdown = it } }

                OutlinedTextField(
                    value = options.notes,
                    onValueChange = { update { options.notes = it } },
                    label = { Text("Observações") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    minLines = 2,
                    maxLines = 4
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { showLivePreview = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Visualizar Prévia")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Resumo do Evento", buildShareText()))
                            copied = true
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.height(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (copied) "Copiado!" else "Copiar Texto")
                    }
                    Button(
                        onClick = { showLivePreview = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.height(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Exportar JPG")
                    }
                }
            }
        }
    }

    if (showLivePreview) {
        Dialog(onDismissRequest = { showLivePreview = false }) {
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Prévia da Página", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        modifier = Modifier
                            .heightIn(max = 480.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Proporção de folha A4 (1 : 1,4142): largura fixa para parecer uma página
                        // de verdade, com um cabeçalho moderno em degradê trazendo os dados do evento.
                        Column(
                            modifier = Modifier
                                .width(360.dp)
                                .background(Color.White)
                                .capturable(captureController)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF9D7BF0), Color(0xFFA978D8), Color(0xFF7C6CE0), Color(0xFF6C8EF0))
                                        )
                                    )
                                    .padding(20.dp)
                            ) {
                                Text(
                                    "🎉 ${event.title}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Color.White, modifier = Modifier.height(14.dp))
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        "${event.eventType} • ${ExportUtils.formatDate(event.eventDateMillis)}",
                                        color = Color.White.copy(alpha = 0.95f),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                if (event.location.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.White, modifier = Modifier.height(14.dp))
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            event.location,
                                            color = Color.White.copy(alpha = 0.95f),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (options.showCountdown) {
                                    val days = daysUntil(event.eventDateMillis)
                                    SharePageSection("Contagem Regressiva", Icons.Default.Timer, Color(0xFF7C6CE0)) {
                                        Text(
                                            text = when {
                                                days > 0 -> "Faltam $days dia(s) para o evento"
                                                days == 0L -> "É hoje!"
                                                else -> "Evento já realizado"
                                            },
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                if (options.showTotals) {
                                    SharePageSection("Totais", Icons.Default.People, Color(0xFF7C6CE0)) {
                                        Text(
                                            "Convidados: ${participants.size} • Confirmados: ${confirmedGuests.size}" +
                                                if (!isOrganizerOnly) " • Pagaram: ${paid.size} • Faltam: ${pending.size}" else "",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }

                                if (options.showBudget) {
                                    SharePageSection("Resumo Financeiro", Icons.Default.AttachMoney, Color(0xFF2E7D32)) {
                                        Text(
                                            "Orçamento: ${ExportUtils.formatCurrency(summary.budget)} • Gasto: ${ExportUtils.formatCurrency(summary.totalSpent)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                if (options.showGuestList) {
                                    SharePageSection("Convidados (${participants.size})", Icons.Default.People, Color(0xFF7C6CE0)) {
                                        groupedByFamily.forEach { (family, members) ->
                                            if (family != null) {
                                                Text(family, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            members.forEach { p ->
                                                Text("• ${p.name}${if (p.confirmed) " ✅" else ""}", style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                    }
                                }

                                if (options.showAttendanceList) {
                                    SharePageSection("Presença Confirmada (${confirmedGuests.size})", Icons.Default.People, Color(0xFF2E7D32)) {
                                        confirmedGuests.forEach { Text("• ${it.name}", style = MaterialTheme.typography.bodySmall) }
                                    }
                                    SharePageSection("Aguardando Confirmação (${awaitingGuests.size})", Icons.Default.People, Color(0xFFF57F17)) {
                                        awaitingGuests.forEach { Text("• ${it.name}", style = MaterialTheme.typography.bodySmall) }
                                    }
                                }

                                if (options.showExpenseItems) {
                                    SharePageSection("Itens Pagos (${paidExpenses.size})", Icons.Default.ShoppingCart, Color(0xFF2E7D32)) {
                                        paidExpenses.forEach { Text("• ${it.title}: ${ExportUtils.formatCurrency(it.amount)}", style = MaterialTheme.typography.bodySmall) }
                                    }
                                    SharePageSection("Itens a Pagar (${pendingExpenses.size})", Icons.Default.ShoppingCart, Color(0xFFC62828)) {
                                        pendingExpenses.forEach { Text("• ${it.title}: ${ExportUtils.formatCurrency(it.amount)}", style = MaterialTheme.typography.bodySmall) }
                                    }
                                }

                                if (options.notes.isNotBlank()) {
                                    SharePageSection("Observações", Icons.Default.Edit, Color(0xFF7C6CE0)) {
                                        Text(options.notes, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { showLivePreview = false }, modifier = Modifier.weight(1f)) {
                            Text("Fechar")
                        }
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    try {
                                        previewBitmap = captureController.captureAsync().await()
                                    } catch (_: Throwable) {
                                        android.widget.Toast.makeText(context, "Erro ao gerar imagem.", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.height(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Exportar JPG")
                        }
                    }
                }
            }
        }
    }

    previewBitmap?.let { bitmap ->
        Dialog(onDismissRequest = { previewBitmap = null }) {
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Prévia do que será compartilhado", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    ImageComposable(
                        bitmap = bitmap,
                        contentDescription = "Prévia do resumo de compartilhamento",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        OutlinedButton(onClick = { previewBitmap = null }) {
                            Text("Cancelar")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            ExportUtils.shareBitmapAsJpg(context, bitmap.asAndroidBitmap(), "Resumo: ${event.title}")
                            previewBitmap = null
                            showLivePreview = false
                        }) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.height(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Compartilhar")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SharePageSection(
    title: String,
    icon: ImageVector,
    accentColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(accentColor.copy(alpha = 0.08f))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.height(15.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge, color = accentColor)
        }
        Spacer(modifier = Modifier.height(6.dp))
        content()
    }
}

@Composable
private fun CheckboxRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}
