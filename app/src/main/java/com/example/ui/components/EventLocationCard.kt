package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
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
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch

private const val DEFAULT_LAT = -23.5505
private const val DEFAULT_LNG = -46.6333

private val BR_STATES = listOf(
    "AC" to "Acre", "AL" to "Alagoas", "AP" to "Amapá", "AM" to "Amazonas",
    "BA" to "Bahia", "CE" to "Ceará", "DF" to "Distrito Federal", "ES" to "Espírito Santo",
    "GO" to "Goiás", "MA" to "Maranhão", "MT" to "Mato Grosso", "MS" to "Mato Grosso do Sul",
    "MG" to "Minas Gerais", "PA" to "Pará", "PB" to "Paraíba", "PR" to "Paraná",
    "PE" to "Pernambuco", "PI" to "Piauí", "RJ" to "Rio de Janeiro", "RN" to "Rio Grande do Norte",
    "RS" to "Rio Grande do Sul", "RO" to "Rondônia", "RR" to "Roraima", "SC" to "Santa Catarina",
    "SP" to "São Paulo", "SE" to "Sergipe", "TO" to "Tocantins",
)

private data class AddressFields(
    val street: String,
    val number: String,
    val neighborhood: String,
    val city: String,
    val state: String,
)

private fun composeAddress(fields: AddressFields): String {
    val streetPart = listOf(fields.street.trim(), fields.number.trim()).filter { it.isNotEmpty() }.joinToString(", ")
    val cityStatePart = listOf(fields.city.trim(), fields.state.trim()).filter { it.isNotEmpty() }.joinToString(" - ")
    val middlePart = listOf(fields.neighborhood.trim(), cityStatePart).filter { it.isNotEmpty() }.joinToString(", ")
    return listOf(streetPart, middlePart).filter { it.isNotEmpty() }.joinToString(" - ")
}

private fun buildDraggablePinHtml(context: android.content.Context, lat: Double, lon: Double): String {
    val script = """
        var map = L.map('map', { attributionControl: false }).setView([$lat, $lon], 15);
        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 19 }).addTo(map);
        var marker = L.marker([$lat, $lon], { draggable: true, icon: pinIcon }).addTo(map);
        marker.on('dragend', function() {
          var pos = marker.getLatLng();
          AndroidPinBridge.onPinMoved(pos.lat, pos.lng);
        });
        map.on('click', function(e) {
          marker.setLatLng(e.latlng);
          AndroidPinBridge.onPinMoved(e.latlng.lat, e.latlng.lng);
        });
    """.trimIndent()
    return buildInlineLeafletDocument(context, script)
}

private class PinBridge(private val onMoved: (Double, Double) -> Unit) {
    @JavascriptInterface
    fun onPinMoved(lat: Double, lng: Double) {
        Handler(Looper.getMainLooper()).post { onMoved(lat, lng) }
    }
}

/**
 * Cadastro completo de localização do evento, espelhando a versão web: endereço
 * estruturado (rua, número, bairro, cidade, UF), busca de coordenadas por
 * geocodificação ou colando um link do Google Maps, e um mapa Leaflet com pin
 * arrastável para ajuste fino.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventLocationCard(
    location: String,
    onLocationChange: (String) -> Unit,
    latitude: Double?,
    longitude: Double?,
    onCoordsChange: (Double, Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var expanded by remember { mutableStateOf(true) }
    var street by remember { mutableStateOf("") }
    var number by remember { mutableStateOf("") }
    var neighborhood by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var stateDropdownExpanded by remember { mutableStateOf(false) }

    var mapsLinkText by remember { mutableStateOf("") }
    var linkError by remember { mutableStateOf<String?>(null) }
    var resolvingLink by remember { mutableStateOf(false) }
    var geocoding by remember { mutableStateOf(false) }
    var geocodeError by remember { mutableStateOf<String?>(null) }

    val hasStructuredInput = street.isNotBlank() || number.isNotBlank() || neighborhood.isNotBlank() ||
        city.isNotBlank() || state.isNotBlank()

    LaunchedEffect(street, number, neighborhood, city, state) {
        val composed = composeAddress(AddressFields(street, number, neighborhood, city, state))
        if (composed.isNotEmpty()) onLocationChange(composed)
    }

    val lat = latitude ?: DEFAULT_LAT
    val lng = longitude ?: DEFAULT_LNG
    val hasPin = latitude != null && longitude != null

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
                    .padding(16.dp)
                    .clip(RoundedCornerShape(8.dp)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer8()
                Text(
                    text = "Localização do Evento",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Minimizar" else "Expandir",
                        modifier = Modifier.rotate(if (expanded) 180f else 0f)
                    )
                }
            }

            if (expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Endereço Completo", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)

                    if (location.isNotBlank() && !hasStructuredInput) {
                        Text(
                            text = "Endereço atual salvo: $location. Preencha os campos abaixo para atualizar.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = street,
                            onValueChange = { street = it },
                            label = { Text("Rua / Avenida") },
                            singleLine = true,
                            modifier = Modifier.weight(2f)
                        )
                        OutlinedTextField(
                            value = number,
                            onValueChange = { number = it },
                            label = { Text("Número") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = neighborhood,
                        onValueChange = { neighborhood = it },
                        label = { Text("Bairro") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = city,
                            onValueChange = { city = it },
                            label = { Text("Cidade") },
                            singleLine = true,
                            modifier = Modifier.weight(2f)
                        )
                        ExposedDropdownMenuBox(
                            expanded = stateDropdownExpanded,
                            onExpandedChange = { stateDropdownExpanded = it },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = state,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("UF") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = stateDropdownExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = stateDropdownExpanded,
                                onDismissRequest = { stateDropdownExpanded = false }
                            ) {
                                BR_STATES.forEach { (uf, name) ->
                                    DropdownMenuItem(
                                        text = { Text("$uf - $name", style = MaterialTheme.typography.bodySmall) },
                                        onClick = {
                                            state = uf
                                            stateDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (location.isNotBlank()) {
                        Text(
                            text = "Endereço: $location",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            if (location.isBlank()) return@OutlinedButton
                            geocoding = true
                            geocodeError = null
                            scope.launch {
                                val result = geocodeAddress(location)
                                if (result != null) {
                                    onCoordsChange(result.lat, result.lon)
                                } else {
                                    geocodeError = "Endereço não encontrado. Tente ser mais específico ou ajuste o pin manualmente no mapa."
                                }
                                geocoding = false
                            }
                        },
                        enabled = !geocoding && location.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (geocoding) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        } else {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                        Spacer8()
                        Text("Buscar endereço e posicionar o pin")
                    }
                    geocodeError?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }

                    Text("Colar link do Google Maps", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = mapsLinkText,
                            onValueChange = {
                                mapsLinkText = it
                                linkError = null
                            },
                            placeholder = { Text("Cole aqui o link completo do Google Maps") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = {
                            linkError = null
                            if (mapsLinkText.isBlank()) return@IconButton
                            if (isShortGoogleMapsLink(mapsLinkText)) {
                                resolvingLink = true
                                scope.launch {
                                    val resolved = resolveShortLink(mapsLinkText)
                                    val parsed = resolved?.let { parseGoogleMapsCoords(it) }
                                    if (parsed != null) {
                                        onCoordsChange(parsed.lat, parsed.lon)
                                    } else {
                                        linkError = "Não conseguimos resolver esse link curto agora. Abra-o no navegador, copie o endereço completo e cole aqui."
                                    }
                                    resolvingLink = false
                                }
                                return@IconButton
                            }
                            val parsed = parseGoogleMapsCoords(mapsLinkText)
                            if (parsed == null) {
                                linkError = "Não encontramos coordenadas nesse link. Copie o link completo da página do local no Google Maps."
                                return@IconButton
                            }
                            onCoordsChange(parsed.lat, parsed.lon)
                        }) {
                            if (resolvingLink) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            } else {
                                Icon(Icons.Default.Link, contentDescription = "Localizar pin pelo link")
                            }
                        }
                    }
                    linkError?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer8()
                        Text("Ajuste fino: toque ou arraste o pin no mapa", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }

                    val html = remember(lat, lng) { buildDraggablePinHtml(context, lat, lng) }
                    val loadedHtml = remember { mutableStateOf<String?>(null) }
                    AndroidView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                webViewClient = debugLeafletWebViewClient()
                                webChromeClient = debugLeafletWebChromeClient()
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                isVerticalScrollBarEnabled = false
                                isHorizontalScrollBarEnabled = false
                                ViewCompat.setNestedScrollingEnabled(this, true)
                                addJavascriptInterface(PinBridge(onCoordsChange), "AndroidPinBridge")
                                loadDataWithBaseURL("https://appassets.androidplatform.net/", html, "text/html", "UTF-8", null)
                                loadedHtml.value = html
                            }
                        },
                        update = { webView ->
                            ViewCompat.setNestedScrollingEnabled(webView, true)
                            // Recarregar a WebView a cada recomposição (ex: cada tecla digitada nos campos
                            // de endereço) fazia a página ficar num loop de reload, nunca terminando de
                            // renderizar o mapa. Só recarrega quando as coordenadas realmente mudam.
                            if (loadedHtml.value != html) {
                                webView.loadDataWithBaseURL("https://appassets.androidplatform.net/", html, "text/html", "UTF-8", null)
                                loadedHtml.value = html
                            }
                        }
                    )

                    if (hasPin) {
                        Text(
                            text = "Pin definido em ${"%.5f".format(lat)}, ${"%.5f".format(lng)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = {
                                val url = googleMapsSearchUrl(location, latitude, longitude)
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer8()
                            Text("Google Maps", style = MaterialTheme.typography.labelMedium)
                        }
                        OutlinedButton(
                            onClick = {
                                val url = appleMapsUrl(location, latitude, longitude)
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer8()
                            Text("Apple Maps", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Spacer8() {
    androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(8.dp))
}
