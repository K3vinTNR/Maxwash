package org.umn.maxwash

import android.webkit.WebView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.umn.maxwash.data.TomTomAddressLocator
import org.umn.maxwash.data.TomTomClient
import org.umn.maxwash.ui.screens.PickupMap
import org.umn.maxwash.ui.theme.MaxwashTheme
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Optional live integration check: requires internet and an enabled TomTom API key. */
@RunWith(AndroidJUnit4::class)
class PickupMapNetworkTest {
    @get:Rule val compose = createComposeRule()

    @Test fun addressResolvesAndRealMapTilesRender() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val app = instrumentation.targetContext.applicationContext as TestMaxwashApplication
        org.junit.Assume.assumeTrue("TOMTOM_API_KEY is required", BuildConfig.TOMTOM_API_KEY.isNotBlank())
        app.mapClient = TomTomClient(BuildConfig.TOMTOM_API_KEY)
        val realLocator = TomTomAddressLocator(app.mapClient)
        app.mapLookup = realLocator::locate
        compose.setContent {
            MaxwashTheme {
                Surface {
                    Column(Modifier.padding(20.dp)) {
                        PickupMap("Jl. Senopati No. 42, Kebayoran Baru, Jakarta Selatan",
                            "Jl. Kasuari No. 14, Bintaro, Tangerang Selatan", "MAXWASH Bintaro Sektor 7", true,
                            onOpenMap = {}, feedback = {})
                    }
                }
            }
        }
        compose.waitUntil(25_000) {
            compose.onAllNodesWithText("Titik dari pencarian alamat. Periksa lokasi di peta.").fetchSemanticsNodes().isNotEmpty()
        }
        val tilesLoaded = AtomicBoolean(false)
        var webView: WebView? = null
        onView(isAssignableFrom(WebView::class.java)).check { view, error ->
            if (error != null) throw error
            webView = view as WebView
            org.junit.Assert.assertTrue(view.width > 0 && view.height > 0)
        }
        try { compose.waitUntil(40_000) {
            instrumentation.runOnMainSync {
                webView!!.evaluateJavascript(
                    "window.maxwashMapState === 'ready' && document.getElementById('map').clientWidth > 0 && document.getElementById('map').clientHeight > 0 && Array.from(document.images).some(i => i.src.includes('api.tomtom.com/maps/orbis/display/raster/tile/') && !i.src.includes('/tile/0/0/0?') && i.complete && i.naturalWidth > 0) && document.querySelector('.pickup-pin') !== null && document.getElementById('credits') !== null") {
                    if (it == "true") tilesLoaded.set(true)
                }
            }
            tilesLoaded.get()
        } } finally {
            if (tilesLoaded.get()) compose.waitUntil(5_000) {
                compose.onAllNodesWithText("Memuat peta…").fetchSemanticsNodes().isEmpty()
            }
            val snapshot = File(app.getExternalFilesDir("screenshots"), "pickup-tomtom-page.txt")
            val done = CountDownLatch(1)
            instrumentation.runOnMainSync {
                webView!!.evaluateJavascript("JSON.stringify({url:location.href,title:document.title,body:document.body.innerText,width:innerWidth,height:innerHeight,mapWidth:document.getElementById('map').clientWidth,mapHeight:document.getElementById('map').clientHeight,images:Array.from(document.images).map(i=>({src:i.src,loaded:i.complete,width:i.naturalWidth}))})") {
                    snapshot.writeText(it)
                    done.countDown()
                }
            }
            done.await(3, TimeUnit.SECONDS)
            val file = File(app.getExternalFilesDir("screenshots"), "pickup-tomtom-map.png")
            file.parentFile!!.mkdirs()
            instrumentation.uiAutomation.takeScreenshot().let { bitmap ->
                file.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
            }
        }
        compose.onNodeWithText("Peta belum dapat dimuat").assertDoesNotExist()
    }
}
