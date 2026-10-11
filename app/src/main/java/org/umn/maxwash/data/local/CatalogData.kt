package org.umn.maxwash.data.local

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Imported once into Room. Screens never read this asset directly. */
class CatalogData(context: Context) {
    private val assets = context.applicationContext.assets

    fun read(): Catalog {
        val root = JSONObject(assets.open("catalog_data.json").bufferedReader().use { it.readText() })
        return Catalog(
            outlets = root.getJSONArray("outlets").objects().map {
                OutletEntity(it.getString("id"), it.getString("name"), it.getString("address"), it.getDouble("distanceKm"), it.getString("openingHours"))
            },
            services = root.getJSONArray("services").objects().map {
                ServiceEntity(it.getString("id"), it.getString("name"), it.getInt("pricePerKg"), it.getInt("turnaroundHours"))
            },
            fragrances = root.getJSONArray("fragrances").let { array -> (0 until array.length()).map { FragranceEntity(array.getString(it), it) } },
            promotions = root.getJSONArray("promotions").objects().map { PromotionEntity(it.getString("id"), it.getString("title"), it.getString("message")) }
        )
    }
}

data class Catalog(
    val outlets: List<OutletEntity>, val services: List<ServiceEntity>,
    val fragrances: List<FragranceEntity>, val promotions: List<PromotionEntity>
)

private fun JSONArray.objects() = (0 until length()).map { getJSONObject(it) }
