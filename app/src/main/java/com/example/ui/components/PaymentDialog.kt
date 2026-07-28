package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.ParticipantEntity

/**
 * Dialog "Dar Baixa no Pagamento" compartilhado entre Convidados e Recebimentos,
 * com calculadora de inserção e opção de receber um valor parcial que soma ao total.
 */
@Composable
fun PaymentDialog(
    participant: ParticipantEntity,
    target: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var amountText by remember { mutableStateOf(participant.paidAmount.toString()) }
    var partialText by remember { mutableStateOf("") }
    var calculatorTarget by remember { mutableStateOf<String?>(null) } // "total" ou "partial"

    fun addPartial() {
        val partial = partialText.toDoubleOrNull() ?: 0.0
        if (partial <= 0.0) return
        val current = amountText.toDoubleOrNull() ?: 0.0
        amountText = (current + partial).toString()
        partialText = ""
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dar Baixa no Pagamento") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Participante: ${participant.name}")
                Text("Meta Individual: ${ExportUtils.formatCurrency(target)}")

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Valor Total Recebido (R$)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    trailingIcon = {
                        IconButton(onClick = { calculatorTarget = "total" }) {
                            Icon(Icons.Default.Calculate, contentDescription = "Abrir calculadora")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { amountText = target.toString() },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Quitar Total", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { amountText = "0.0" },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Zerar", fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedTextField(
                    value = partialText,
                    onValueChange = { partialText = it },
                    label = { Text("Receber Parcial Agora (R$)") },
                    placeholder = { Text("Ex: 50,00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    trailingIcon = {
                        IconButton(onClick = { calculatorTarget = "partial" }) {
                            Icon(Icons.Default.Calculate, contentDescription = "Abrir calculadora")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                TextButton(onClick = { addPartial() }) {
                    Text("Adicionar ao Total Recebido")
                }
            }
        },
        confirmButton = {
            GradientButton(
                text = "Confirmar Baixa",
                onClick = {
                    val paid = amountText.toDoubleOrNull() ?: 0.0
                    onConfirm(paid)
                }
            )
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(14.dp)) {
                Text("Cancelar", fontWeight = FontWeight.Bold)
            }
        }
    )

    calculatorTarget?.let { target ->
        MiniCalculator(
            onDismiss = { calculatorTarget = null },
            onUseResult = { value ->
                if (target == "total") amountText = value.toString() else partialText = value.toString()
            }
        )
    }
}
