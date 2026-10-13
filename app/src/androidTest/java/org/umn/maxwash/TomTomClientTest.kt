package org.umn.maxwash

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.umn.maxwash.data.*
import java.io.IOException
import java.net.URI

@RunWith(AndroidJUnit4::class)
class TomTomClientTest {
    @Test fun geocodingUsesGeoJsonCoordinateOrderAndPrivateHeaders() {
        var requests = 0
        val client = TomTomClient("test-key", MapHttpTransport { url, headers ->
            requests++
            assertEquals("api.tomtom.com", URI(url).host)
            assertTrue(url.contains("countryCodesIso2=ID"))
            assertTrue(url.contains("Jl.+Senopati+%26+No.+42"))
            assertFalse(url.contains("test-key"))
            assertEquals("test-key", headers["TomTom-Api-Key"])
            assertEquals("2", headers["TomTom-Api-Version"])
            assertEquals("results.position", headers["Attributes"])
            response("""{"results":[{"position":{"type":"Point","coordinates":[106.81,-6.23]}}]}""")
        })
        assertEquals(MapPoint(-6.23, 106.81), client.geocode(" Jl. Senopati & No. 42 "))
        assertEquals(1, requests)
    }

    @Test fun noKeyOrBlankAddressNeverMakesRequests() {
        val deny = MapHttpTransport { _, _ -> error("Unexpected request") }
        assertNull(TomTomClient("", deny).geocode("Senopati"))
        assertNull(TomTomClient("test-key", deny).geocode(" "))
        assertNull(TomTomClient("", deny).tile(tileUrl))
    }

    @Test fun invalidOrEmptyResultsNeverCreateAssumedPins() {
        listOf("""{"results":[]}""", "broken JSON",
            """{"results":[{"position":{"type":"Point","coordinates":[106.81,91]}}]}""",
            """{"results":[{"position":{"type":"Point","coordinates":[106.81]}}]}""",
            """{"results":[{"position":{"type":"Polygon","coordinates":[106.81,-6.23]}}]}""").forEach { body ->
            assertNull(TomTomClient("test-key", MapHttpTransport { _, _ -> response(body) }).geocode("Senopati"))
        }
    }

    @Test fun rejectedKeyAndNetworkErrorsAreRecoverable() = runBlocking {
        val rejected = TomTomClient("test-key", MapHttpTransport { _, _ -> MapHttpResponse(403, "application/json", byteArrayOf()) })
        assertNull(TomTomAddressLocator(rejected).locate("Senopati"))
        val offline = TomTomClient("test-key", MapHttpTransport { _, _ -> throw IOException("Offline") })
        assertNull(TomTomAddressLocator(offline).locate("Senopati"))
    }

    @Test fun tileCredentialsOnlyGoToApprovedTomTomRequests() {
        var requests = 0
        val client = TomTomClient("test-key", MapHttpTransport { url, headers ->
            requests++
            assertTrue(url == tileUrl || url == tileUrl.replace("tileSize=256", "tileSize=512"))
            assertFalse(url.contains("test-key"))
            assertEquals("test-key", headers["TomTom-Api-Key"])
            assertEquals("image/png", headers["Accept"])
            MapHttpResponse(200, "image/png", byteArrayOf(1, 2, 3))
        })
        assertNotNull(client.tile(tileUrl))
        assertNotNull(client.tile(tileUrl.replace("tileSize=256", "tileSize=512")))
        listOf(tileUrl.replace("https:", "http:"), tileUrl.replace("api.tomtom.com", "example.com"),
            tileUrl.replace("api.tomtom.com", "api.tomtom.com.example.com"),
            tileUrl.replace("api.tomtom.com", "api.tomtom.com:8443"),
            tileUrl.replace("/display/raster/tile/", "/unrelated/"), "$tileUrl&key=other").forEach {
            assertNull(client.tile(it))
        }
        assertEquals(2, requests)
    }

    private fun response(body: String) = MapHttpResponse(200, "application/json", body.toByteArray())
    private val tileUrl = "https://api.tomtom.com/maps/orbis/display/raster/tile/15/26106/16954?style=street-light&tileSize=256"
}
