package org.umn.maxwash.data

/** Coordinates returned by an address provider, never an assumed customer/GPS position. */
data class MapPoint(val latitude: Double, val longitude: Double) {
    init {
        require(latitude.isFinite() && latitude in -90.0..90.0)
        require(longitude.isFinite() && longitude in -180.0..180.0)
    }

    fun embeddedMapUrl(): String {
        val mapLatitude = latitude.coerceIn(-85.04612878, 85.04612878)
        val south = mapLatitude - .005
        val north = mapLatitude + .005
        val west = (longitude - .008).coerceAtLeast(-180.0)
        val east = (longitude + .008).coerceAtMost(180.0)
        return MAP_DOCUMENT_URL +
            "?bbox=$west,$south,$east,$north&marker=$latitude,$longitude"
    }
}

fun interface AddressLocator {
    suspend fun locate(address: String): MapPoint?
}

// A real overview with no pin when the user has not supplied a resolvable address.
private const val MAP_DOCUMENT_URL = "https://appassets.androidplatform.net/map/pickup.html"
const val INDONESIA_MAP_URL = "$MAP_DOCUMENT_URL?bbox=94.5,-11.5,141.5,6.5"
