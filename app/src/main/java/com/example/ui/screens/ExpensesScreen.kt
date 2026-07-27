package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entity.ExpenseCategory
import com.example.data.local.entity.ExpenseEntity
import com.example.ui.components.BudgetVsSpentChart
import com.example.ui.components.ExportUtils
import com.example.ui.components.getCategoryColor
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

    var selectedCategoryFilter by remember { mutableStateOf<ExpenseCategory?>(null) }
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<ExpenseEntity?>(null) }

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
            FloatingActionButton(
                onClick = { showAddExpenseDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Novo Gasto")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .capturable(captureController),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Budget vs Spent Visual Chart Component
            if (activeEvent != null) {
                BudgetVsSpentChart(
                    budget = activeEvent.budget,
                    expenses = uiState.expenses
                )
            }

            // Category Filter Chips
            ScrollableTabRow(
                selectedTabIndex = if (selectedCategoryFilter == null) 0 else ExpenseCategory.entries.indexOf(selectedCategoryFilter) + 1,
                edgePadding = 0.dp,
                divider = {}
            ) {
                FilterChip(
                    selected = (selectedCategoryFilter == null),
                    onClick = { selectedCategoryFilter = null },
                    label = { Text("Todas") },
                    modifier = Modifier.padding(end = 6.dp)
                )
                ExpenseCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = (selectedCategoryFilter == cat),
                        onClick = { selectedCategoryFilter = cat },
                        label = { Text(cat.label) },
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
                            onTogglePurchased = { isChecked ->
                                viewModel.toggleExpensePurchased(expense.id, isChecked)
                            },
                            onEditClick = { editingExpense = expense },
                            onDeleteClick = { viewModel.deleteExpense(expense) }
                        )
                    }
                }
            }
        }

        // Add / Edit Expense Dialog
        if (showAddExpenseDialog || editingExpense != null) {
            val isEditing = editingExpense != null
            val itemToEdit = editingExpense

            var title by remember { mutableStateOf(itemToEdit?.title ?: "") }
            var amountText by remember { mutableStateOf(itemToEdit?.amount?.toString() ?: "") }
            var category by remember { mutableStateOf(itemToEdit?.category ?: ExpenseCategory.FOOD) }
            var isPurchased by remember { mutableStateOf(itemToEdit?.isPurchased ?: false) }

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
                            ExpenseCategory.entries.forEach { cat ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                ) {
                                    RadioButton(
                                        selected = (category == cat),
                                        onClick = { category = cat }
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(cat.label, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = isPurchased,
                                onCheckedChange = { isPurchased = it }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Já comprado / pago", style = MaterialTheme.typography.bodyMedium)
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
                                    isPurchased = isPurchased
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

@Composable
private fun ExpenseItemCard(
    expense: ExpenseEntity,
    onTogglePurchased: (Boolean) -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
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
            Checkbox(
                checked = expense.isPurchased,
                onCheckedChange = onTogglePurchased
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = getCategoryColor(expense.category).copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = expense.category.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = getCategoryColor(expense.category),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (expense.isPurchased) "Comprado" else "A Comprar",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (expense.isPurchased) Color(0xFF2E7D32) else Color(0xFFE65100)
                    )
                }
            }

            Text(
                text = ExportUtils.formatCurrency(expense.amount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row {
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
