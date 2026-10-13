package org.umn.maxwash.ui.screens

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.http.SslError
import android.view.MotionEvent
import android.view.ViewGroup
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.SslErrorHandler
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import org.umn.maxwash.MaxwashApplication
import org.umn.maxwash.data.INDONESIA_MAP_URL
import org.umn.maxwash.data.MapPoint
import org.umn.maxwash.data.TomTomClient
import java.io.ByteArrayInputStream
import java.io.IOException
import org.umn.maxwash.ui.theme.*

private enum class MapLoad { LOADING, READY, FAILED }

@Composable
fun PickupMap(address: String, outletAddress: String, outletName: String, pickup: Boolean,
    onOpenMap: (String) -> Unit, feedback: (String) -> Unit) {
    val context = LocalContext.current
    val application = context.applicationContext as MaxwashApplication
    val locator = application.addressLocator
    val tomTom = application.tomTomClient
    val configured = tomTom.isConfigured
    var showOutlet by rememberSaveable(pickup) { mutableStateOf(!pickup) }
    var retry by remember { mutableIntStateOf(0) }
    val target = (if (showOutlet) outletAddress else address).trim()
    val label = if (showOutlet) outletName else "Alamat Penjemputan"
    var point by remember(target) { mutableStateOf<MapPoint?>(null) }
    var locating by remember(target) { mutableStateOf(configured && target.isNotBlank()) }
    var pageLoad by remember { mutableStateOf(MapLoad.LOADING) }

    LaunchedEffect(target, retry) {
        point = null
        locating = configured && target.isNotBlank()
        if (locating) point = locator.locate(target)
        locating = false
    }
    val mapUrl = point?.embeddedMapUrl() ?: INDONESIA_MAP_URL

    Column(Modifier.fillMaxWidth().testTag("pickup_map_card"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (pickup) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = !showOutlet, onClick = { showOutlet = false }, label = { Text("Alamat Anda") },
                modifier = Modifier.testTag("pickup_map_address"))
            FilterChip(selected = showOutlet, onClick = { showOutlet = true }, label = { Text("Outlet") },
                modifier = Modifier.testTag("pickup_map_outlet"))
        }
        Box(Modifier.fillMaxWidth().height(300.dp).clip(RoundedCornerShape(24.dp)).background(WashSoftBlue)) {
            // Keep the provider's map controls and attribution unobstructed.
            if (configured) key(mapUrl, retry) {
                EmbeddedMap(mapUrl, tomTom, Modifier.fillMaxSize().testTag("pickup_map"),
                    onLoad = { pageLoad = it }, feedback = feedback)
            }
            if (!configured || pageLoad != MapLoad.READY) Column(
                Modifier.fillMaxSize().background(WashSoftBlue).testTag("pickup_map_placeholder"),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                if (!configured) {
                    Icon(Icons.Outlined.Map, null, Modifier.size(32.dp), tint = WashTeal)
                    Spacer(Modifier.height(8.dp))
                    Text("Peta belum tersedia", style = MaterialTheme.typography.titleSmall)
                    Text("Anda tetap dapat melanjutkan memilih layanan.", color = WashMuted,
                        style = MaterialTheme.typography.bodySmall)
                } else if (pageLoad == MapLoad.LOADING) {
                    CircularProgressIndicator(Modifier.size(28.dp), color = WashTeal, strokeWidth = 3.dp)
                    Spacer(Modifier.height(10.dp))
                    Text("Memuat peta…", color = WashMuted, style = MaterialTheme.typography.bodySmall)
                } else {
                    Icon(Icons.Outlined.Map, null, Modifier.size(32.dp), tint = WashTeal)
                    Spacer(Modifier.height(8.dp))
                    Text("Peta belum dapat dimuat", style = MaterialTheme.typography.titleSmall)
                    Text("Periksa koneksi atau coba lagi nanti.", color = WashMuted, style = MaterialTheme.typography.bodySmall)
                    TextButton({ retry++ }, Modifier.testTag("pickup_map_retry")) { Text("Coba Lagi") }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = WashNavy,
                    modifier = Modifier.testTag("pickup_map_target"))
                Text(when {
                    !configured -> "Peta belum tersedia saat ini."
                    locating -> "Mencari lokasi alamat…"
                    target.isBlank() -> "Tambahkan alamat untuk menampilkan titik."
                    point != null -> "Titik dari pencarian alamat. Periksa lokasi di peta."
                    else -> "Lokasi alamat belum tersedia. Periksa alamat atau coba lagi."
                }, style = MaterialTheme.typography.bodySmall, color = WashMuted,
                    modifier = Modifier.testTag("pickup_map_status"))
            }
            IconButton({ onOpenMap(target) }, Modifier.testTag("pickup_open_map"), enabled = target.isNotBlank()) {
                Icon(Icons.AutoMirrored.Outlined.OpenInNew, "Buka alamat ini di Maps", tint = WashTeal)
            }
        }
        if (configured && !locating && target.isNotBlank() && point == null && pageLoad != MapLoad.FAILED) {
            TextButton({ retry++ }, Modifier.testTag("pickup_map_locate_retry")) { Text("Cari Lokasi Lagi") }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled") // Bundled renderer + native TomTom tile requests; no JS bridge.
@Composable
private fun EmbeddedMap(url: String, tomTom: TomTomClient, modifier: Modifier = Modifier,
    onLoad: (MapLoad) -> Unit, feedback: (String) -> Unit) {
    var failed by remember(url) { mutableStateOf(false) }
    var finished by remember(url) { mutableStateOf(false) }
    val viewReference = remember { arrayOfNulls<WebView>(1) }
    val currentOnLoad by rememberUpdatedState(onLoad)
    val currentFeedback by rememberUpdatedState(feedback)
    LaunchedEffect(url) {
        currentOnLoad(MapLoad.LOADING)
        repeat(40) {
            delay(500)
            if (finished || failed) return@LaunchedEffect
            viewReference[0]?.evaluateJavascript("window.maxwashMapState") { state ->
                when (state) {
                    "\"ready\"" -> { finished = true; currentOnLoad(MapLoad.READY) }
                    "\"failed\"" -> { failed = true; currentOnLoad(MapLoad.FAILED) }
                }
            }
        }
        if (!finished && !failed) {
            failed = true
            currentOnLoad(MapLoad.FAILED)
        }
    }
    AndroidView(modifier = modifier, factory = { context ->
        EmbeddedMapView(context).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            viewReference[0] = this
            settings.apply {
                javaScriptEnabled = true
                allowFileAccess = false
                allowContentAccess = false
                setGeolocationEnabled(false)
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                cacheMode = WebSettings.LOAD_DEFAULT
                userAgentString = "$userAgentString Maxwash/1.0 (org.umn.maxwash)"
            }
            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                    if (request.url.host != "api.tomtom.com") return null
                    return try {
                        val response = tomTom.tile(request.url.toString()) ?: return unavailableTile(403)
                        if (response.status == 200 && response.contentType == "image/png")
                            WebResourceResponse("image/png", null, ByteArrayInputStream(response.body))
                        else unavailableTile(response.status)
                    } catch (_: IOException) { unavailableTile(503) }
                }
                override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                    currentOnLoad(MapLoad.LOADING)
                }
                override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
                    handler.cancel()
                    fail()
                }
                override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                    if (request.isForMainFrame || request.url.host == "api.tomtom.com") fail()
                }
                override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, errorResponse: WebResourceResponse) {
                    if (request.isForMainFrame || request.url.host == "api.tomtom.com") fail()
                }
                private fun fail() {
                    failed = true
                    currentOnLoad(MapLoad.FAILED)
                }
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    val uri = request.url
                    if (uri.scheme == "https") try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                    } catch (_: ActivityNotFoundException) {
                        currentFeedback("Browser belum tersedia.")
                    }
                    return true
                }
            }
            val html = context.assets.open("map/pickup-map.html").bufferedReader().use { it.readText() }
                .replace("/*LEAFLET_CSS*/", context.assets.open("map/leaflet.css").bufferedReader().use { it.readText() })
                .replace("/*LEAFLET_JS*/", context.assets.open("map/leaflet.js").bufferedReader().use { it.readText() })
            // A local HTTPS origin supplies only the numeric viewport/marker, never credentials.
            loadDataWithBaseURL(url, html, "text/html", "UTF-8", null)
        }
    }, onRelease = { view ->
        viewReference[0] = null
        view.stopLoading()
        view.webViewClient = WebViewClient()
        view.destroy()
    })
}

private fun unavailableTile(status: Int) = WebResourceResponse("text/plain", "UTF-8",
    if (status in 400..599) status else 502, "Map unavailable", emptyMap(), ByteArrayInputStream(byteArrayOf()))

/** Let map drags/pinch gestures complete without the enclosing page consuming them. */
private class EmbeddedMapView(context: Context) : WebView(context) {
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) post { evaluateJavascript("window.maxwashResizeMap && window.maxwashResizeMap()", null) }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        parent?.requestDisallowInterceptTouchEvent(event.actionMasked != MotionEvent.ACTION_UP &&
            event.actionMasked != MotionEvent.ACTION_CANCEL)
        if (event.actionMasked == MotionEvent.ACTION_UP) performClick()
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean = super.performClick()
}
