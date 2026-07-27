package com.example.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.ExpenseEntity
import com.example.data.local.entity.FALLBACK_CATEGORY_COLOR
import com.example.data.local.entity.FALLBACK_CATEGORY_LABEL
import com.example.ui.components.BudgetVsSpentChart
import com.example.ui.components.ExportUtils
import com.example.ui.components.colorFromHex
import com.example.ui.viewmodel.PartyUiState
import com.example.ui.viewmodel.PartyViewModel
import dev.shreyaspatil.capturable.capturable
import dev.shreyaspatil.capturable.controller.rememberCaptureController
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun ExpensesScreen(
    uiState: PartyUiState,
    viewModel: PartyViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val captureController = rememberCaptureController()
    var previewBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    val activeEvent = uiState.activeEvent
    val categories = uiState.categories

    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<ExpenseEntity?>(null) }
    var detailExpense by remember { mutableStateOf<ExpenseEntity?>(null) }

    val filteredExpenses = remember(uiState.expenses, selectedCategoryFilter) {
        if (selectedCategoryFilter == null) {
            uiState.expenses
        } else {
            uiState.expenses.filter { it.category == selectedCategoryFilter }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Controle de Gastos", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (activeEvent != null) {
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
                        }
                    }) {
                        Icon(imageVector = Icons.Default.Image, contentDescription = "Exportar JPG", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { showAddExpenseDialog = true }) {
                        Icon(imageVector = Icons.Default.AddShoppingCart, contentDescription = "Adicionar Gasto")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            com.example.ui.components.GradientFab(
                onClick = { showAddExpenseDialog = true },
                icon = Icons.Default.Add,
                contentDescription = "Novo Gasto"
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .capturable(captureController)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Budget vs Spent Visual Chart Component
            if (activeEvent != null) {
                BudgetVsSpentChart(
                    budget = activeEvent.budget,
                    expenses = uiState.expenses,
                    categories = categories
                )
            }

            // Category Filter Chips
            ScrollableTabRow(
                selectedTabIndex = if (selectedCategoryFilter == null) 0 else categories.indexOfFirst { it.id == selectedCategoryFilter } + 1,
                edgePadding = 0.dp,
                divider = {}
            ) {
                FilterChip(
                    selected = (selectedCategoryFilter == null),
                    onClick = { selectedCategoryFilter = null },
                    label = { Text("Todas") },
                    modifier = Modifier.padding(end = 6.dp)
                )
                categories.forEach { cat ->
                    FilterChip(
                        selected = (selectedCategoryFilter == cat.id),
                        onClick = { selectedCategoryFilter = cat.id },
                        label = { Text(cat.name) },
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }
            }

            // List of Expenses
            if (filteredExpenses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.RemoveShoppingCart,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Nenhum item de gasto cadastrado nesta categoria.",
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
                    items(filteredExpenses, key = { it.id }) { expense ->
                        ExpenseItemCard(
                            expense = expense,
                            onClick = { detailExpense = expense }
                        )
                    }
                }
            }
        }

        // Add / Edit Expense Dialog
        if ((showAddExpenseDialog || editingExpense != null) && categories.isNotEmpty()) {
            val isEditing = editingExpense != null
            val itemToEdit = editingExpense

            var title by remember { mutableStateOf(itemToEdit?.title ?: "") }
            var amountText by remember { mutableStateOf(itemToEdit?.amount?.toString() ?: "") }
            var category by remember { mutableStateOf(itemToEdit?.category ?: categories.first().id) }
            var isPurchased by remember { mutableStateOf(itemToEdit?.isPurchased ?: false) }
            var isPaid by remember { mutableStateOf(itemToEdit?.isPaid ?: false) }

            AlertDialog(
                onDismissRequest = {
                    showAddExpenseDialog = false
                    editingExpense = null
                },
                title = { Text(if (isEditing) "Editar Item de Gasto" else "Novo Item de Gasto") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Descrição do Item") },
                            placeholder = { Text("Ex: Salgadinhos, Refrigerantes, Decoração") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = amountText,
                            onValueChange = { amountText = it },
                            label = { Text("Valor Estimado/Pago (R$)") },
                            placeholder = { Text("Ex: 150.00") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Text("Categoria:", style = MaterialTheme.typography.labelMedium)
                        Column {
                            categories.forEach { cat ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                ) {
                                    RadioButton(
                                        selected = (category == cat.id),
                                        onClick = { category = cat.id }
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(RoundedCornerShape(50))
                                            .background(colorFromHex(cat.color))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(cat.name, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }

                        Text("Status de Compra:", style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = !isPurchased,
                                onClick = { isPurchased = false },
                                label = { Text("A Comprar") },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = isPurchased,
                                onClick = { isPurchased = true },
                                label = { Text("Comprado") },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Text("Status de Pagamento:", style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = !isPaid,
                                onClick = { isPaid = false },
                                label = { Text("A Pagar") },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = isPaid,
                                onClick = { isPaid = true },
                                label = { Text("Pago") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val amount = amountText.toDoubleOrNull() ?: 0.0
                            if (title.isNotBlank() && activeEvent != null) {
                                val exp = ExpenseEntity(
                                    id = itemToEdit?.id ?: "",
                                    eventId = activeEvent.id,
                                    title = title,
                                    category = category,
                                    amount = amount,
                                    isPurchased = isPurchased,
                                    isPaid = isPaid,
                                    notes = itemToEdit?.notes ?: ""
                                )
                                if (isEditing) {
                                    viewModel.updateExpense(exp)
                                } else {
                                    viewModel.addExpense(exp)
                                }
                            }
                            showAddExpenseDialog = false
                            editingExpense = null
                        }
                    ) {
                        Text("Salvar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showAddExpenseDialog = false
                        editingExpense = null
                    }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        // Expense Detail Dialog (full info + observação + ações)
        detailExpense?.let { expense ->
            ExpenseDetailDialog(
                expense = expense,
                categories = categories,
                onDismiss = { detailExpense = null },
                onTogglePurchased = { viewModel.toggleExpensePurchased(expense.id, it) },
                onTogglePaid = { viewModel.toggleExpensePaid(expense.id, it) },
                onSaveNotes = { notes -> viewModel.updateExpense(expense.copy(notes = notes)) },
                onEditClick = {
                    detailExpense = null
                    editingExpense = expense
                },
                onDeleteClick = {
                    viewModel.deleteExpense(expense)
                    detailExpense = null
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
                            contentDescription = "Prévia dos gastos do evento",
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
                                    "Gastos_${activeEvent?.title ?: ""}"
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

/**
 * Card minimizado: só nome, valor e status. Toque para abrir os detalhes completos.
 */
@Composable
private fun ExpenseItemCard(
    expense: ExpenseEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        onClick = onClick
    ) {
        Box {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = expense.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (expense.isPurchased) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                        ) {
                            Text(
                                text = if (expense.isPurchased) "Comprado" else "A Comprar",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (expense.isPurchased) Color(0xFF2E7D32) else Color(0xFFE65100),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (expense.isPaid) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                        ) {
                            Text(
                                text = if (expense.isPaid) "Pago" else "A Pagar",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (expense.isPaid) Color(0xFF2E7D32) else Color(0xFFC62828),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Text(
                    text = ExportUtils.formatCurrency(expense.amount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.width(4.dp))

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Ver detalhes",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (expense.notes.isNotBlank()) {
                PulsingDot(modifier = Modifier.align(Alignment.TopEnd).padding(6.dp))
            }
        }
    }
}

@Composable
private fun PulsingDot(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseScale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )
    Box(modifier = modifier.size(20.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(Color(0xFFE5484D).copy(alpha = alpha))
        )
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(Color(0xFFE5484D))
        )
    }
}

/**
 * Popup com todas as informações do gasto: categoria, data, valor, status
 * de compra/pagamento (editáveis na hora) e campo de observação.
 */
@Composable
private fun ExpenseDetailDialog(
    expense: ExpenseEntity,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onTogglePurchased: (Boolean) -> Unit,
    onTogglePaid: (Boolean) -> Unit,
    onSaveNotes: (String) -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val category = categories.find { it.id == expense.category }
    var notesText by remember(expense.id) { mutableStateOf(expense.notes) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(20.dp)) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = expense.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = colorFromHex(category?.color ?: FALLBACK_CATEGORY_COLOR).copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = category?.name ?: FALLBACK_CATEGORY_LABEL,
                            style = MaterialTheme.typography.labelSmall,
                            color = colorFromHex(category?.color ?: FALLBACK_CATEGORY_COLOR),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = ExportUtils.formatDate(expense.dateAddedMillis),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = ExportUtils.formatCurrency(expense.amount),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Status de Compra", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !expense.isPurchased,
                        onClick = { onTogglePurchased(false) },
                        label = { Text("A Comprar") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = expense.isPurchased,
                        onClick = { onTogglePurchased(true) },
                        label = { Text("Comprado") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("Status de Pagamento", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !expense.isPaid,
                        onClick = { onTogglePaid(false) },
                        label = { Text("A Pagar") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = expense.isPaid,
                        onClick = { onTogglePaid(true) },
                        label = { Text("Pago") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Observação", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    placeholder = { Text("Ex: comprar na promoção, aguardando orçamento do fornecedor...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { onSaveNotes(notesText) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Salvar Observação")
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = onEditClick, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Editar")
                    }
                    Button(
                        onClick = { showDeleteConfirm = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Excluir")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Fechar")
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Excluir Gasto?") },
            text = { Text("Tem certeza que deseja excluir '${expense.title}'?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDeleteClick()
                }) {
                    Text("Excluir", color = Color(0xFFD32F2F))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
