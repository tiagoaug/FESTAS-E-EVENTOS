package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.ViewCompat

private const val LEAFLET_ASSET_BASE_URL = "file:///android_asset/leaflet/"

private fun buildLeafletHtml(lat: Double, lon: Double): String {
    return """
        <!DOCTYPE html>
        <html>
        <head>
          <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
          <link rel="stylesheet" href="leaflet.css" />
          <style>
            html, body, #map { height: 100%; width: 100%; margin: 0; padding: 0; }
          </style>
        </head>
        <body>
          <div id="map"></div>
          <script src="leaflet.js"></script>
          <script>
            var map = L.map('map', { attributionControl: false }).setView([$lat, $lon], 15);
            L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 19 }).addTo(map);
            L.marker([$lat, $lon]).addTo(map);
          </script>
        </body>
        </html>
    """.trimIndent()
}

/**
 * Card retrátil com o mapa do local do evento. Vem minimizado por padrão, mostrando
 * apenas o endereço, para não ocupar espaço no Dashboard até o usuário expandir.
 *
 * Usa Leaflet + OpenStreetMap (sem chave de API): o endereço é geocodificado em
 * Kotlin via Nominatim antes de montar o HTML, e o mapa carrega já centralizado
 * nas coordenadas resolvidas — evita depender de JavaScript assíncrono dentro da
 * WebView para a etapa de busca, que é praticamente impossível de depurar.
 */
@Composable
fun LocationMapCard(
    location: String,
    modifier: Modifier = Modifier,
    defaultExpanded: Boolean = false
) {
    if (location.isBlank()) return

    val context = LocalContext.current
    var expanded by remember { mutableStateOf(defaultExpanded) }
    val encodedQuery = Uri.encode(location)
    val searchUrl = "https://www.google.com/maps/search/?api=1&query=$encodedQuery"

    var geoResult by remember(location) { mutableStateOf<GeoResult?>(null) }
    var geoFailed by remember(location) { mutableStateOf(false) }
    var isLoading by remember(location) { mutableStateOf(false) }

    LaunchedEffect(location, expanded) {
        if (expanded && geoResult == null && !geoFailed) {
            isLoading = true
            val result = geocodeAddress(location)
            if (result != null) {
                geoResult = result
            } else {
                geoFailed = true
            }
            isLoading = false
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                ) {
                    Text(
                        text = "Localização do Evento",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = location,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                androidx.compose.material3.IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Minimizar mapa" else "Expandir mapa",
                        modifier = Modifier.rotate(if (expanded) 180f else 0f)
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    when {
                        isLoading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(28.dp))
                            }
                        }
                        geoResult != null -> {
                            val result = geoResult!!
                            val html = remember(result) { buildLeafletHtml(result.lat, result.lon) }
                            val loadedHtml = remember { mutableStateOf<String?>(null) }
                            AndroidView(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
                                    .clip(RoundedCornerShape(0.dp)),
                                factory = { ctx ->
                                    WebView(ctx).apply {
                                        webViewClient = WebViewClient()
                                        settings.javaScriptEnabled = true
                                        settings.domStorageEnabled = true
                                        isVerticalScrollBarEnabled = false
                                        isHorizontalScrollBarEnabled = false
                                        ViewCompat.setNestedScrollingEnabled(this, true)
                                        loadDataWithBaseURL(LEAFLET_ASSET_BASE_URL, html, "text/html", "UTF-8", null)
                                        loadedHtml.value = html
                                    }
                                },
                                update = { webView ->
                                    ViewCompat.setNestedScrollingEnabled(webView, true)
                                    if (loadedHtml.value != html) {
                                        webView.loadDataWithBaseURL(LEAFLET_ASSET_BASE_URL, html, "text/html", "UTF-8", null)
                                        loadedHtml.value = html
                                    }
                                }
                            )
                        }
                        else -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Não foi possível carregar o mapa para este endereço. Use o botão abaixo para abrir no Google Maps.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.height(16.dp))
                            Text(" Google Maps", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}
