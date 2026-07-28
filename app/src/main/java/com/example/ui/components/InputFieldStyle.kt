package com.example.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Cantos usados nos campos de inserção "3D" das telas de cadastro/edição. */
val InputFieldShape = RoundedCornerShape(16.dp)

/**
 * Sombra sutil por baixo do campo, dando o efeito "3D" moderno pedido para os
 * formulários de cadastro/edição, em vez do OutlinedTextField chapado padrão.
 */
fun Modifier.elevatedFieldShadow(cornerRadius: Dp = 16.dp): Modifier =
    this.shadow(elevation = 3.dp, shape = RoundedCornerShape(cornerRadius), clip = false)

/** Cores do campo com fundo sólido (necessário para a sombra ficar visível). */
@Composable
fun elevatedFieldColors() = OutlinedTextFieldDefaults.colors(
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    disabledContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
)
