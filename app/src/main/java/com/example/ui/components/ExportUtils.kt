package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.provider.CalendarContract
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.local.entity.EventEntity
import com.example.data.local.entity.ExpenseEntity
import com.example.data.local.entity.ParticipantEntity
import com.example.ui.viewmodel.FinancialSummary
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportUtils {

    fun formatCurrency(amount: Double): String {
        val ptBr = Locale("pt", "BR")
        val formatter = NumberFormat.getCurrencyInstance(ptBr)
        return formatter.format(amount)
    }

    fun formatDate(timeMillis: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale("pt", "BR"))
        return sdf.format(Date(timeMillis))
    }

    fun formatDateOnly(timeMillis: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
        return sdf.format(Date(timeMillis))
    }

    fun createGoogleCalendarIntent(context: Context, event: EventEntity) {
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, event.eventDateMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, event.eventDateMillis + (3 * 3600 * 1000L))
            putExtra(CalendarContract.Events.TITLE, "🎉 ${event.title}")
            putExtra(CalendarContract.Events.EVENT_LOCATION, event.location)
            putExtra(
                CalendarContract.Events.DESCRIPTION,
                "Lembretes sugeridos para o evento:\n• 7 Dias antes (Planejamento & Compras)\n• 1 Dia antes (Preparação Final)\n• 3 Horas antes (Organização do Local)\n\nOrganizado via Festas & Eventos."
            )
            putExtra(CalendarContract.Events.ACCESS_LEVEL, CalendarContract.Events.ACCESS_PRIVATE)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Não foi possível abrir o aplicativo de Agenda.", Toast.LENGTH_SHORT).show()
        }
    }

    fun openWhatsAppMessage(context: Context, rawPhone: String, message: String) {
        var cleanPhone = rawPhone.replace(Regex("[^0-9]"), "")
        if (cleanPhone.isNotEmpty() && !cleanPhone.startsWith("55") && cleanPhone.length in 10..11) {
            cleanPhone = "55$cleanPhone"
        }

        val encodedMessage = Uri.encode(message)
        val url = if (cleanPhone.isNotEmpty()) {
            "https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMessage"
        } else {
            "https://api.whatsapp.com/send?text=$encodedMessage"
        }

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Aplicativo do WhatsApp não encontrado.", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareBitmapAsJpg(context: Context, bitmap: Bitmap, title: String) {
        try {
            val cachePath = File(context.cacheContextOrFiles(), "images")
            cachePath.mkdirs()
            val file = File(cachePath, "resumo_${System.currentTimeMillis()}.jpg")
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
            stream.close()

            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Compartilhar Imagem JPG"))
        } catch (e: Exception) {
            Toast.makeText(context, "Erro ao exportar imagem: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun exportFinancialSummaryPdf(
        context: Context,
        event: EventEntity,
        summary: FinancialSummary,
        participants: List<ParticipantEntity>,
        expenses: List<ExpenseEntity>
    ) {
        try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            val titlePaint = Paint().apply {
                color = Color.rgb(42, 18, 77)
                textSize = 20f
                isFakeBoldText = true
            }

            val subtitlePaint = Paint().apply {
                color = Color.rgb(80, 80, 80)
                textSize = 12f
            }

            val sectionPaint = Paint().apply {
                color = Color.rgb(108, 74, 182)
                textSize = 14f
                isFakeBoldText = true
            }

            val textPaint = Paint().apply {
                color = Color.BLACK
                textSize = 10f
            }

            val boldTextPaint = Paint().apply {
                color = Color.BLACK
                textSize = 10f
                isFakeBoldText = true
            }

            val linePaint = Paint().apply {
                color = Color.LTGRAY
                strokeWidth = 1f
            }

            var y = 40f

            // Header
            canvas.drawText("Relatório Financeiro: ${event.title}", 40f, y, titlePaint)
            y += 20f
            canvas.drawText("Data do Evento: ${formatDate(event.eventDateMillis)} | Local: ${event.location}", 40f, y, subtitlePaint)
            y += 25f

            canvas.drawLine(40f, y, 555f, y, linePaint)
            y += 20f

            // Summary Section
            canvas.drawText("Resumo Financeiro", 40f, y, sectionPaint)
            y += 18f

            canvas.drawText("Orçamento Total: ${formatCurrency(summary.budget)}", 40f, y, boldTextPaint)
            canvas.drawText("Total Gasto: ${formatCurrency(summary.totalSpent)}", 220f, y, boldTextPaint)
            canvas.drawText("Saldo Orçamento: ${formatCurrency(summary.budgetBalance)}", 400f, y, boldTextPaint)
            y += 15f

            canvas.drawText("Meta Arrecadação: ${formatCurrency(summary.totalExpectedCollection)}", 40f, y, textPaint)
            canvas.drawText("Total Arrecadado: ${formatCurrency(summary.totalCollected)}", 220f, y, textPaint)
            canvas.drawText("Valor Faltante: ${formatCurrency(summary.missingCollection)}", 400f, y, textPaint)
            y += 25f

            canvas.drawLine(40f, y, 555f, y, linePaint)
            y += 20f

            // Expenses Table
            canvas.drawText("Lista de Gastos (${expenses.size} itens)", 40f, y, sectionPaint)
            y += 18f

            canvas.drawText("Item / Categoria", 40f, y, boldTextPaint)
            canvas.drawText("Status", 300f, y, boldTextPaint)
            canvas.drawText("Valor (R$)", 480f, y, boldTextPaint)
            y += 12f

            expenses.take(15).forEach { exp ->
                canvas.drawText("${exp.title} (${exp.category.label})", 40f, y, textPaint)
                canvas.drawText(if (exp.isPurchased) "Comprado" else "A comprar", 300f, y, textPaint)
                canvas.drawText(formatCurrency(exp.amount), 480f, y, textPaint)
                y += 14f
            }

            if (expenses.size > 15) {
                canvas.drawText("... e mais ${expenses.size - 15} itens", 40f, y, subtitlePaint)
                y += 14f
            }

            y += 15f
            canvas.drawLine(40f, y, 555f, y, linePaint)
            y += 20f

            // Participants Payments Table
            canvas.drawText("Recebimentos por Participante (${participants.size} pessoas)", 40f, y, sectionPaint)
            y += 18f

            canvas.drawText("Nome / Família", 40f, y, boldTextPaint)
            canvas.drawText("Tipo", 260f, y, boldTextPaint)
            canvas.drawText("Pago", 360f, y, boldTextPaint)
            canvas.drawText("Telefone", 460f, y, boldTextPaint)
            y += 12f

            participants.take(20).forEach { p ->
                canvas.drawText("${p.name} (${p.familyGroup})", 40f, y, textPaint)
                canvas.drawText(p.type.label, 260f, y, textPaint)
                canvas.drawText(formatCurrency(p.paidAmount), 360f, y, textPaint)
                canvas.drawText(if (p.phone.isNotBlank()) p.phone else "-", 460f, y, textPaint)
                y += 14f
            }

            pdfDocument.finishPage(page)

            val cachePath = File(context.cacheContextOrFiles(), "documents")
            cachePath.mkdirs()
            val pdfFile = File(cachePath, "relatorio_evento_${System.currentTimeMillis()}.pdf")
            pdfDocument.writeTo(FileOutputStream(pdfFile))
            pdfDocument.close()

            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, "Relatório Financeiro: ${event.title}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Exportar Relatório PDF"))

        } catch (e: Exception) {
            Toast.makeText(context, "Erro ao gerar PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun Context.cacheContextOrFiles(): File {
        return externalCacheDir ?: cacheDir
    }
}
