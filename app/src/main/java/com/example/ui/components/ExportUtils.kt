package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.provider.CalendarContract
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.local.entity.EventEntity
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

    /**
     * JPEG não tem canal alfa: qualquer pixel transparente no bitmap capturado
     * (ex: quando a biblioteca de captura não inclui o fundo do Compose) vira
     * preto na hora da compressão. Para evitar isso, "achatamos" o bitmap sobre
     * um fundo opaco antes de comprimir, garantindo que nenhuma transparência
     * sobreviva independente do que a captura produziu.
     */
    fun flattenOntoOpaqueBackground(source: Bitmap, backgroundColor: Int = Color.WHITE): Bitmap {
        // Bitmaps com Config.HARDWARE não podem ser desenhados por um Canvas de software
        // ("Software rendering doesn't support hardware bitmaps") — a captura em algumas
        // versões do Android/Compose produz esse tipo, então convertemos antes de desenhar.
        val softwareSource = if (source.config == Bitmap.Config.HARDWARE) {
            source.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            source
        }
        val flattened = Bitmap.createBitmap(softwareSource.width, softwareSource.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(flattened)
        canvas.drawColor(backgroundColor)
        canvas.drawBitmap(softwareSource, 0f, 0f, null)
        return flattened
    }

    fun shareBitmapAsJpg(context: Context, bitmap: Bitmap, title: String) {
        try {
            val flattened = flattenOntoOpaqueBackground(bitmap)
            val cachePath = File(context.cacheContextOrFiles(), "images")
            cachePath.mkdirs()
            val file = File(cachePath, "resumo_${System.currentTimeMillis()}.jpg")
            val stream = FileOutputStream(file)
            flattened.compress(Bitmap.CompressFormat.JPEG, 95, stream)
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

    private fun Context.cacheContextOrFiles(): File {
        return externalCacheDir ?: cacheDir
    }
}
