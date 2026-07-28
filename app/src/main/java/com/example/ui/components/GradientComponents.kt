package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Botão flutuante de ação com fundo em degradê, mirando o estilo visual da versão web
 * (em vez do FAB solid-color padrão do Material3).
 */
@Composable
fun GradientFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.Add,
    contentDescription: String? = null,
) {
    Box(
        modifier = modifier
            .size(58.dp)
            .shadow(elevation = 10.dp, shape = CircleShape, clip = false)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.primary,
                    )
                )
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription, tint = Color.White)
    }
}

/**
 * Botão de ação primária em pílula com fundo em degradê, mirando o `.btn-primary` da
 * versão web (em vez do TextButton/Button flat padrão do Material3 usado em diálogos).
 */
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 22.dp, vertical = 12.dp)
) {
    Box(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.5f)
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.primary,
                    )
                )
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides Color.White) {
            Text(text = text, fontWeight = FontWeight.Bold)
        }
    }
}
