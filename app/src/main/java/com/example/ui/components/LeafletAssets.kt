package com.example.ui.components

import android.content.Context
import android.util.Base64
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

private const val LEAFLET_LOG_TAG = "LeafletMap"

/** WebViewClient/WebChromeClient com logging para depurar via `adb logcat -s LeafletMap`. */
fun debugLeafletWebViewClient(): WebViewClient = object : WebViewClient() {
    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
        super.onReceivedError(view, request, error)
        Log.e(LEAFLET_LOG_TAG, "onReceivedError url=${request?.url} error=${error?.description}")
    }
}

fun debugLeafletWebChromeClient(): WebChromeClient = object : WebChromeClient() {
    override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
        Log.d(
            LEAFLET_LOG_TAG,
            "console[${consoleMessage.messageLevel()}] ${consoleMessage.message()} (${consoleMessage.sourceId()}:${consoleMessage.lineNumber()})"
        )
        return true
    }
}

/**
 * Carrega o Leaflet (CSS/JS/ícones) dos assets do app e embute tudo inline no HTML,
 * em vez de referenciar via href/src relativo a um baseURL. Depender de resolução
 * relativa de recursos dentro de loadDataWithBaseURL se mostrou pouco confiável em
 * alguns WebViews (página ficava em branco silenciosamente); embutindo tudo, a única
 * requisição de rede que resta é a dos tiles do mapa em si (inevitável).
 */
private object LeafletAssetCache {
    @Volatile private var css: String? = null
    @Volatile private var js: String? = null
    @Volatile private var markerIconB64: String? = null
    @Volatile private var markerIcon2xB64: String? = null
    @Volatile private var markerShadowB64: String? = null

    fun css(context: Context): String =
        css ?: readAssetText(context, "leaflet/leaflet.css").also { css = it }

    fun js(context: Context): String =
        js ?: readAssetText(context, "leaflet/leaflet.js").also { js = it }

    fun markerIcon(context: Context): String =
        markerIconB64 ?: readAssetBase64(context, "leaflet/images/marker-icon.png").also { markerIconB64 = it }

    fun markerIcon2x(context: Context): String =
        markerIcon2xB64 ?: readAssetBase64(context, "leaflet/images/marker-icon-2x.png").also { markerIcon2xB64 = it }

    fun markerShadow(context: Context): String =
        markerShadowB64 ?: readAssetBase64(context, "leaflet/images/marker-shadow.png").also { markerShadowB64 = it }

    private fun readAssetText(context: Context, path: String): String =
        context.assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }

    private fun readAssetBase64(context: Context, path: String): String =
        context.assets.open(path).use { it.readBytes() }.let { Base64.encodeToString(it, Base64.NO_WRAP) }
}

fun buildInlineLeafletDocument(context: Context, bodyScript: String): String {
    val css = LeafletAssetCache.css(context)
    val js = LeafletAssetCache.js(context)
    val iconB64 = LeafletAssetCache.markerIcon(context)
    val icon2xB64 = LeafletAssetCache.markerIcon2x(context)
    val shadowB64 = LeafletAssetCache.markerShadow(context)
    return """
        <!DOCTYPE html>
        <html>
        <head>
          <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
          <style>$css</style>
          <style>html, body { margin: 0; padding: 0; }</style>
        </head>
        <body>
          <div id="map"></div>
          <script>$js</script>
          <script>
            // Em algumas WebViews, document.body/#map resolvem altura CSS (100%/absolute)
            // como 0 mesmo com window.innerWidth/innerHeight corretos — um bug de
            // containing-block. Em vez de depender da cascata de CSS, aplicamos o
            // tamanho em pixels diretamente via JS, usando o valor confiável da janela.
            var mapEl = document.getElementById('map');
            mapEl.style.width = window.innerWidth + 'px';
            mapEl.style.height = window.innerHeight + 'px';

            // L.Icon.Default.mergeOptions NÃO funciona com URLs data: — a classe IconDefault
            // sempre concatena um "imagePath" (auto-detectado via CSS) na frente do valor
            // informado, mesmo que já seja uma URI completa, gerando algo como
            // ".../images/data:image/png;base64,..." e um 404. Por isso construímos um
            // L.icon() explícito e passamos via {icon: pinIcon} em cada marker.
            var pinIcon = L.icon({
              iconUrl: 'data:image/png;base64,$iconB64',
              iconRetinaUrl: 'data:image/png;base64,$icon2xB64',
              shadowUrl: 'data:image/png;base64,$shadowB64',
              iconSize: [25, 41],
              iconAnchor: [12, 41],
              popupAnchor: [1, -34],
              shadowSize: [41, 41]
            });
            $bodyScript

            // O card do mapa fica dentro de um bloco condicional (accordion): se o Leaflet
            // inicializar antes do container assumir seu tamanho final, ele "trava" num
            // tamanho quase zero e nunca recalcula sozinho. Observamos mudanças de tamanho
            // e recalculamos o mapa sempre que necessário, em vez de apostar num delay fixo.
            if (window.ResizeObserver && typeof map !== 'undefined') {
              var ro = new ResizeObserver(function() { map.invalidateSize(); });
              ro.observe(mapEl);
            }
          </script>
        </body>
        </html>
    """.trimIndent()
}
