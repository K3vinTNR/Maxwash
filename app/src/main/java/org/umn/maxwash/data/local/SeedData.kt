package org.umn.maxwash.data.local

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import org.umn.maxwash.data.PasswordHasher
import java.time.OffsetDateTime

/** Imported once into Room. Screens never read this asset directly. */
class SeedData(context: Context) {
    private val assets = context.applicationContext.assets

    fun read(): Seed {
        val root = JSONObject(assets.open("demo_data.json").bufferedReader().use { it.readText() })
        val customers = root.getJSONArray("customers").objects().map {
            val password = PasswordHasher.hash(it.getString("password"))
            CustomerEntity(it.getString("id"), it.getString("name"), it.getString("phone"), it.getString("email"),
                it.getString("address"), it.getString("fragrance"), it.getString("notes"), it.getBoolean("notificationEnabled"),
                it.getString("outletId"), it.getString("membership"), it.getInt("points"), password.value, password.salt)
        }
        val orders = root.getJSONArray("orders").objects()
        return Seed(
            demoCustomerId = root.getString("demoCustomerId"), customers = customers,
            outlets = root.getJSONArray("outlets").objects().map {
                OutletEntity(it.getString("id"), it.getString("name"), it.getString("address"), it.getDouble("distanceKm"), it.getString("openingHours"))
            },
            services = root.getJSONArray("services").objects().map {
                ServiceEntity(it.getString("id"), it.getString("name"), it.getInt("pricePerKg"), it.getInt("turnaroundHours"))
            },
            fragrances = root.getJSONArray("fragrances").let { array -> (0 until array.length()).map { FragranceEntity(array.getString(it), it) } },
            promotions = root.getJSONArray("promotions").objects().map { PromotionEntity(it.getString("id"), it.getString("title"), it.getString("message")) },
            orders = orders.map {
                OrderEntity(it.getString("id"), it.getString("customerId"), it.getString("serviceId"), it.getDouble("weightKg"),
                    it.timestamp("createdAt"), it.getString("outletId"), it.getString("status"), it.getInt("total"), it.timestamp("estimatedCollectionAt"))
            },
            history = orders.flatMap { order -> order.getJSONArray("history").objects().map {
                StatusEventEntity(order.getString("id"), it.getString("status"), it.timestamp("occurredAt"))
            } },
            notifications = root.getJSONArray("notifications").objects().map {
                NotificationEntity(it.getString("id"), it.getString("orderId"), it.getString("title"), it.getString("message"), it.timestamp("createdAt"), it.getBoolean("isRead"))
            }
        )
    }
}

data class Seed(
    val demoCustomerId: String, val customers: List<CustomerEntity>, val outlets: List<OutletEntity>,
    val services: List<ServiceEntity>, val fragrances: List<FragranceEntity>, val promotions: List<PromotionEntity>,
    val orders: List<OrderEntity>, val history: List<StatusEventEntity>, val notifications: List<NotificationEntity>
)

private fun JSONArray.objects() = (0 until length()).map { getJSONObject(it) }
private fun JSONObject.timestamp(key: String) = OffsetDateTime.parse(getString(key)).toInstant().toEpochMilli()
