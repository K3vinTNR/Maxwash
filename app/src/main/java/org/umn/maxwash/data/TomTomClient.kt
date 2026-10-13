package org.umn.maxwash.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.URLEncoder

data class MapHttpResponse(val status: Int, val contentType: String, val body: ByteArray)

fun interface MapHttpTransport {
    fun get(url: String, headers: Map<String, String>): MapHttpResponse
}

/** Only TomTom Orbis v2 is used. Credentials stay in native HTTPS request headers. */
class TomTomClient(private val apiKey: String, private val transport: MapHttpTransport = TomTomHttpTransport()) {
    val isConfigured: Boolean get() = apiKey.isNotBlank()
    private fun headers(accept: String) = mapOf(
        "TomTom-Api-Key" to apiKey,
        "TomTom-Api-Version" to "2",
        "Accept" to accept,
        "Accept-Language" to "id-ID",
        "User-Agent" to "Maxwash/1.0 (org.umn.maxwash)"
    )

    fun geocode(address: String): MapPoint? {
        if (!isConfigured || address.isBlank()) return null
        val query = URLEncoder.encode(address.trim(), "UTF-8")
        val response = transport.get("https://api.tomtom.com/maps/orbis/places/geocode" +
            "?query=$query&maxResults=1&countryCodesIso2=ID&types=address,street,intersection",
            headers("application/json") + ("Attributes" to "results.position"))
        if (response.status != 200) throw IOException("Address service unavailable (${response.status})")
        try {
            val position = JSONObject(response.body.toString(Charsets.UTF_8))
                .optJSONArray("results")?.optJSONObject(0)?.optJSONObject("position") ?: return null
            if (position.optString("type") != "Point") return null
            val coordinates = position.optJSONArray("coordinates") ?: return null
            if (coordinates.length() < 2) return null
            // GeoJSON is longitude, latitude; MapPoint deliberately uses latitude, longitude.
            return runCatching { MapPoint(coordinates.getDouble(1), coordinates.getDouble(0)) }.getOrNull()
        } catch (_: JSONException) { return null }
    }

    /** Deny unrelated paths/hosts so a key can never follow an arbitrary WebView request. */
    fun tile(url: String): MapHttpResponse? {
        val uri = runCatching { URI(url) }.getOrNull() ?: return null
        if (!isConfigured || uri.scheme != "https" || uri.host != "api.tomtom.com" ||
            uri.port != -1 || uri.userInfo != null ||
            !uri.path.matches(Regex("/maps/orbis/display/raster/tile/[0-9]+/[0-9]+/[0-9]+")) ||
            uri.rawQuery !in setOf("style=street-light&tileSize=256", "style=street-light&tileSize=512")) return null
        return transport.get(url, headers("image/png"))
    }
}

class TomTomAddressLocator(private val client: TomTomClient) : AddressLocator {
    override suspend fun locate(address: String): MapPoint? = try {
        runInterruptible(Dispatchers.IO) { client.geocode(address) }
    } catch (_: IOException) { null }
}

private class TomTomHttpTransport : MapHttpTransport {
    override fun get(url: String, headers: Map<String, String>): MapHttpResponse {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 5_000
            connection.readTimeout = 7_000
            // Do not forward credentials to redirects, even if an endpoint changes.
            connection.instanceFollowRedirects = false
            headers.forEach(connection::setRequestProperty)
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.use { it.readBytes() } ?: byteArrayOf()
            return MapHttpResponse(status, connection.contentType.orEmpty().substringBefore(';'), body)
        } finally { connection.disconnect() }
    }
}
