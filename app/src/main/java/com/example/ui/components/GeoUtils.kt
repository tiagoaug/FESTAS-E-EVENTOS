package com.example.ui.components

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

data class GeoResult(val lat: Double, val lon: Double)

/**
 * Geocodifica o endereço via Nominatim (OpenStreetMap), fora da WebView, para que
 * falhas de rede/parse sejam tratadas em Kotlin em vez de silenciosamente numa
 * página HTML que não conseguimos depurar.
 */
suspend fun geocodeAddress(address: String): GeoResult? = withContext(Dispatchers.IO) {
    try {
        val query = Uri.encode(address)
        val url = URL("https://nominatim.openstreetmap.org/search?format=json&limit=1&q=$query")
        val connection = url.openConnection() as HttpURLConnection
        connection.setRequestProperty("User-Agent", "FestasEEventosApp/1.0 (Android)")
        connection.connectTimeout = 8000
        connection.readTimeout = 8000
        connection.requestMethod = "GET"

        val responseCode = connection.responseCode
        if (responseCode != 200) return@withContext null

        val body = connection.inputStream.bufferedReader().use { it.readText() }
        val array = JSONArray(body)
        if (array.length() == 0) return@withContext null

        val first = array.getJSONObject(0)
        GeoResult(lat = first.getString("lat").toDouble(), lon = first.getString("lon").toDouble())
    } catch (e: Exception) {
        null
    }
}

fun isShortGoogleMapsLink(input: String): Boolean {
    val trimmed = input.trim()
    return try {
        val host = Uri.parse(trimmed).host ?: return false
        host == "maps.app.goo.gl" || host.endsWith(".goo.gl") || host == "goo.gl"
    } catch (e: Exception) {
        false
    }
}

/**
 * Extrai coordenadas de um link do Google Maps (formatos !3d/!4d, @lat,lng, ou
 * parâmetros de query q/ll/query) ou de um par "lat, lng" colado diretamente.
 */
fun parseGoogleMapsCoords(input: String): GeoResult? {
    val text = input.trim()

    Regex("""^(-?\d{1,2}\.\d+),\s*(-?\d{1,3}\.\d+)$""").find(text)?.let {
        return GeoResult(it.groupValues[1].toDouble(), it.groupValues[2].toDouble())
    }

    Regex("""!3d(-?\d{1,2}\.\d+)!4d(-?\d{1,3}\.\d+)""").find(text)?.let {
        return GeoResult(it.groupValues[1].toDouble(), it.groupValues[2].toDouble())
    }

    Regex("""@(-?\d{1,2}\.\d+),(-?\d{1,3}\.\d+)""").find(text)?.let {
        return GeoResult(it.groupValues[1].toDouble(), it.groupValues[2].toDouble())
    }

    try {
        val uri = Uri.parse(text)
        val param = uri.getQueryParameter("q") ?: uri.getQueryParameter("ll") ?: uri.getQueryParameter("query")
        if (param != null) {
            Regex("""^(-?\d{1,2}\.\d+),\s*(-?\d{1,3}\.\d+)$""").find(param.trim())?.let {
                return GeoResult(it.groupValues[1].toDouble(), it.groupValues[2].toDouble())
            }
        }
    } catch (e: Exception) {
        return null
    }

    return null
}

fun googleMapsSearchUrl(location: String, lat: Double?, lng: Double?): String {
    val query = if (lat != null && lng != null) "$lat,$lng" else location
    return "https://www.google.com/maps/search/?api=1&query=${Uri.encode(query)}"
}

fun appleMapsUrl(location: String, lat: Double?, lng: Double?): String {
    val query = if (lat != null && lng != null) "$lat,$lng" else location
    return "https://maps.apple.com/?q=${Uri.encode(query)}"
}
