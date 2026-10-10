package org.umn.maxwash.data.local

import androidx.room.*

@Entity(tableName = "outlets")
data class OutletEntity(
    @PrimaryKey val id: String, val name: String, val address: String,
    val distanceKm: Double, val openingHours: String
)

@Entity(tableName = "services")
data class ServiceEntity(
    @PrimaryKey val id: String, val name: String, val pricePerKg: Int, val turnaroundHours: Int
)

@Entity(tableName = "fragrances")
data class FragranceEntity(@PrimaryKey val name: String, val position: Int)

@Entity(tableName = "promotions")
data class PromotionEntity(@PrimaryKey val id: String, val title: String, val message: String)

@Entity(
    tableName = "customers",
    indices = [Index(value = ["email"], unique = true), Index(value = ["phone"], unique = true), Index("outletId")],
    foreignKeys = [ForeignKey(entity = OutletEntity::class, parentColumns = ["id"], childColumns = ["outletId"], onDelete = ForeignKey.SET_NULL)]
)
data class CustomerEntity(
    @PrimaryKey val id: String, val name: String, val phone: String, val email: String,
    val address: String, val fragrance: String, val notes: String,
    val notificationEnabled: Boolean, val outletId: String?, val membership: String, val points: Int,
    val passwordHash: String, val passwordSalt: String
)

@Entity(
    tableName = "orders",
    indices = [Index("customerId"), Index("outletId"), Index("serviceId")],
    foreignKeys = [
        ForeignKey(entity = CustomerEntity::class, parentColumns = ["id"], childColumns = ["customerId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = OutletEntity::class, parentColumns = ["id"], childColumns = ["outletId"]),
        ForeignKey(entity = ServiceEntity::class, parentColumns = ["id"], childColumns = ["serviceId"])
    ]
)
data class OrderEntity(
    @PrimaryKey val id: String, val customerId: String, val serviceId: String,
    val weightKg: Double, val createdAt: Long, val outletId: String,
    val status: String, val total: Int, val estimatedCollectionAt: Long
)

@Entity(
    tableName = "status_events", primaryKeys = ["orderId", "status"],
    foreignKeys = [ForeignKey(entity = OrderEntity::class, parentColumns = ["id"], childColumns = ["orderId"], onDelete = ForeignKey.CASCADE)]
)
data class StatusEventEntity(val orderId: String, val status: String, val occurredAt: Long)

@Entity(
    tableName = "notifications", indices = [Index("orderId")],
    foreignKeys = [ForeignKey(entity = OrderEntity::class, parentColumns = ["id"], childColumns = ["orderId"], onDelete = ForeignKey.CASCADE)]
)
data class NotificationEntity(
    @PrimaryKey val id: String, val orderId: String, val title: String,
    val message: String, val createdAt: Long, val isRead: Boolean
)

@Entity(
    tableName = "app_session", indices = [Index("customerId"), Index("selectedOutletId")],
    foreignKeys = [
        ForeignKey(entity = CustomerEntity::class, parentColumns = ["id"], childColumns = ["customerId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = OutletEntity::class, parentColumns = ["id"], childColumns = ["selectedOutletId"], onDelete = ForeignKey.SET_NULL)
    ]
)
data class SessionEntity(@PrimaryKey val id: Int = 1, val customerId: String? = null, val selectedOutletId: String? = null)

@Entity(tableName = "metadata")
data class MetadataEntity(@PrimaryKey val key: String, val value: String)

data class OrderWithDetails(
    @Embedded val order: OrderEntity,
    @Relation(parentColumn = "serviceId", entityColumn = "id") val service: ServiceEntity,
    @Relation(parentColumn = "id", entityColumn = "orderId") val history: List<StatusEventEntity>,
    @Relation(parentColumn = "id", entityColumn = "orderId") val notifications: List<NotificationEntity>
)

data class CustomerWithOrders(
    @Embedded val customer: CustomerEntity,
    @Relation(parentColumn = "id", entityColumn = "customerId", entity = OrderEntity::class)
    val orders: List<OrderWithDetails>
)
