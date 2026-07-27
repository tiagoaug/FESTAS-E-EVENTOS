package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private enum class CalcOp { ADD, SUBTRACT, MULTIPLY, DIVIDE }

private fun compute(a: Double, b: Double, op: CalcOp): Double = when (op) {
    CalcOp.ADD -> a + b
    CalcOp.SUBTRACT -> a - b
    CalcOp.MULTIPLY -> a * b
    CalcOp.DIVIDE -> if (b == 0.0) 0.0 else a / b
}

/**
 * Calculadora simples embutida para inserir o resultado em campos de valor (R$).
 */
@Composable
fun MiniCalculator(
    onDismiss: () -> Unit,
    onUseResult: (Double) -> Unit
) {
    var display by remember { mutableStateOf("0") }
    var stored by remember { mutableStateOf<Double?>(null) }
    var pendingOp by remember { mutableStateOf<CalcOp?>(null) }
    var overwrite by remember { mutableStateOf(true) }

    fun inputDigit(d: String) {
        display = if (overwrite || display == "0") d else display + d
        overwrite = false
    }

    fun inputDot() {
        display = if (overwrite) "0." else if (display.contains(".")) display else "$display."
        overwrite = false
    }

    fun clearAll() {
        display = "0"
        stored = null
        pendingOp = null
        overwrite = true
    }

    fun backspace() {
        display = if (display.length > 1) display.dropLast(1) else "0"
    }

    fun chooseOp(op: CalcOp) {
        val current = display.toDoubleOrNull() ?: 0.0
        val prevStored = stored
        val prevOp = pendingOp
        if (prevStored != null && prevOp != null) {
            val result = compute(prevStored, current, prevOp)
            stored = result
            display = result.toString()
        } else {
            stored = current
        }
        pendingOp = op
        overwrite = true
    }

    fun equals() {
        val current = display.toDoubleOrNull() ?: 0.0
        val prevStored = stored
        val prevOp = pendingOp
        if (prevStored != null && prevOp != null) {
            val result = compute(prevStored, current, prevOp)
            display = result.toString()
            stored = null
            pendingOp = null
            overwrite = true
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Calculadora") },
        text = {
            androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = display,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.End
                    )
                }

                val rows = listOf(
                    listOf("C", "⌫", "÷", "×"),
                    listOf("7", "8", "9", "-"),
                    listOf("4", "5", "6", "+"),
                    listOf("1", "2", "3", "="),
                )
                rows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        row.forEach { key ->
                            val isEquals = key == "="
                            val onClick: () -> Unit = {
                                when (key) {
                                    "C" -> clearAll()
                                    "⌫" -> backspace()
                                    "÷" -> chooseOp(CalcOp.DIVIDE)
                                    "×" -> chooseOp(CalcOp.MULTIPLY)
                                    "-" -> chooseOp(CalcOp.SUBTRACT)
                                    "+" -> chooseOp(CalcOp.ADD)
                                    "=" -> equals()
                                    else -> inputDigit(key)
                                }
                            }
                            if (isEquals) {
                                Button(onClick = onClick, modifier = Modifier.weight(1f)) {
                                    Text(key, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                OutlinedButton(onClick = onClick, modifier = Modifier.weight(1f)) {
                                    Text(key, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = { inputDigit("0") }, modifier = Modifier.weight(2f)) {
                        Text("0", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(onClick = { inputDot() }, modifier = Modifier.weight(1f)) {
                        Text(",", fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onUseResult(display.toDoubleOrNull() ?: 0.0)
                onDismiss()
            }) {
                Text("Usar Resultado")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar")
            }
        }
    )
}
