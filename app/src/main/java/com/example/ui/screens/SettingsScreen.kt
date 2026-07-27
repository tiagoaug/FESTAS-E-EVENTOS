package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.CATEGORY_COLOR_SWATCHES
import com.example.data.local.entity.CategoryEntity
import com.example.ui.components.colorFromHex
import com.example.ui.theme.AppThemeId
import com.example.ui.theme.FONT_OPTIONS
import com.example.ui.theme.fontFamilyFor
import com.example.ui.viewmodel.PartyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: PartyViewModel,
    onNavigateBack: () -> Unit
) {
    val useWhatsApp by viewModel.useWhatsApp.collectAsStateWithLifecycle()
    val appTheme by viewModel.appTheme.collectAsStateWithLifecycle()
    val appFont by viewModel.appFont.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showCategoryDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var deleteTarget by remember { mutableStateOf<CategoryEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configurações", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Preferências Gerais
            item {
                SettingsCard(title = "Preferências Gerais") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = null,
                            tint = Color(0xFF25D366)
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 12.dp)
                        ) {
                            Text(
                                text = "Usar WhatsApp",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Mostra os botões e atalhos de WhatsApp pelo app. Se desativado, eles ficam ocultos e o espaço é reduzido.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = useWhatsApp,
                            onCheckedChange = { viewModel.setUseWhatsApp(it) }
                        )
                    }
                }
            }

            // Tema de Cores
            item {
                SettingsCard(title = "Tema de Cores") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppThemeId.entries.chunked(2).forEach { rowThemes ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowThemes.forEach { theme ->
                                    val selected = appTheme == theme
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(16.dp))
                                            .then(
                                                if (selected) {
                                                    Modifier.background(theme.swatch.copy(alpha = 0.12f))
                                                } else Modifier
                                            )
                                            .clickable { viewModel.setAppTheme(theme) }
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(theme.swatch),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (selected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(theme.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Fonte do Aplicativo
            item {
                SettingsCard(title = "Fonte do Aplicativo") {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        FONT_OPTIONS.forEach { option ->
                            val selected = appFont == option.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .then(
                                        if (selected) Modifier.background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                                        else Modifier
                                    )
                                    .clickable { viewModel.setAppFont(option.id) }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Aa Festas & Eventos",
                                        fontFamily = fontFamilyFor(option.id),
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = option.label,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (selected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Categorias de Gastos
            item {
                SettingsCard(
                    title = "Categorias de Gastos",
                    headerAction = {
                        IconButton(onClick = {
                            editingCategory = null
                            showCategoryDialog = true
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "Adicionar Categoria")
                        }
                    }
                ) {
                    Column {
                        uiState.categories.forEach { category ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(colorFromHex(category.color))
                                )
                                Text(
                                    text = category.name,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 10.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                IconButton(onClick = {
                                    editingCategory = category
                                    showCategoryDialog = true
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Editar", modifier = Modifier.size(18.dp))
                                }
                                IconButton(onClick = { deleteTarget = category }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = Color(0xFFD32F2F), modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                        if (uiState.categories.isEmpty()) {
                            Text(
                                text = "Nenhuma categoria cadastrada ainda.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCategoryDialog) {
        CategoryEditDialog(
            category = editingCategory,
            onDismiss = { showCategoryDialog = false },
            onSave = { name, color ->
                if (editingCategory != null) {
                    viewModel.updateCategory(editingCategory!!.copy(name = name, color = color))
                } else {
                    viewModel.addCategory(CategoryEntity(name = name, color = color))
                }
                showCategoryDialog = false
            }
        )
    }

    deleteTarget?.let { category ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Excluir Categoria?") },
            text = { Text("Tem certeza que deseja excluir '${category.name}'? Gastos existentes com essa categoria ficarão sem categoria.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteCategory(category)
                    deleteTarget = null
                }) {
                    Text("Excluir", color = Color(0xFFD32F2F))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun SettingsCard(
    title: String,
    headerAction: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(true) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                headerAction?.invoke()
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Recolher" else "Expandir",
                    modifier = Modifier.rotate(if (expanded) 180f else 0f)
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    content()
                }
            }
        }
    }
}

@Composable
private fun CategoryEditDialog(
    category: CategoryEntity?,
    onDismiss: () -> Unit,
    onSave: (name: String, color: String) -> Unit
) {
    var name by remember { mutableStateOf(category?.name ?: "") }
    var color by remember { mutableStateOf(category?.color ?: CATEGORY_COLOR_SWATCHES.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (category == null) "Nova Categoria" else "Editar Categoria") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome da Categoria") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Text("Cor:", style = MaterialTheme.typography.labelMedium)
                FlowRowSwatches(
                    swatches = CATEGORY_COLOR_SWATCHES,
                    selected = color,
                    onSelect = { color = it }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onSave(name.trim(), color) }) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun FlowRowSwatches(
    swatches: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    swatches.chunked(6).forEach { rowSwatches ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            rowSwatches.forEach { hex ->
                val isSelected = hex.equals(selected, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(colorFromHex(hex))
                        .clickable { onSelect(hex) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
