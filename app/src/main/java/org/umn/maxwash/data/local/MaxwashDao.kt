package org.umn.maxwash.data.local

import androidx.room.*

@Dao
interface MaxwashDao {
    @Query("SELECT * FROM app_session WHERE id = 1")
    suspend fun session(): SessionEntity?

    @Query("SELECT value FROM metadata WHERE `key` = :key")
    suspend fun metadata(key: String): String?

    @Transaction
    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun customerWithOrders(id: String): CustomerWithOrders?

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun customer(id: String): CustomerEntity?

    @Query("SELECT * FROM customers WHERE email = :identifier OR phone = :identifier LIMIT 1")
    suspend fun findAccount(identifier: String): CustomerEntity?

    @Query("SELECT COUNT(*) FROM customers WHERE id != :excludeId AND (email = :email OR phone = :phone)")
    suspend fun conflictingAccounts(email: String, phone: String, excludeId: String = ""): Int

    @Query("SELECT * FROM outlets ORDER BY id")
    suspend fun outlets(): List<OutletEntity>

    @Query("SELECT * FROM services ORDER BY id")
    suspend fun services(): List<ServiceEntity>

    @Query("SELECT * FROM fragrances ORDER BY position")
    suspend fun fragrances(): List<FragranceEntity>

    @Query("SELECT * FROM promotions ORDER BY id")
    suspend fun promotions(): List<PromotionEntity>

    @Insert suspend fun insertCustomers(customers: List<CustomerEntity>)
    @Update suspend fun updateCustomer(customer: CustomerEntity)
    @Upsert suspend fun saveSession(session: SessionEntity)
    @Insert suspend fun insertMetadata(metadata: MetadataEntity)
    @Upsert suspend fun saveOutlets(outlets: List<OutletEntity>)
    @Upsert suspend fun saveServices(services: List<ServiceEntity>)
    @Upsert suspend fun saveFragrances(fragrances: List<FragranceEntity>)
    @Upsert suspend fun savePromotions(promotions: List<PromotionEntity>)
    @Upsert suspend fun saveOrders(orders: List<OrderEntity>)
    @Insert suspend fun insertOrder(order: OrderEntity)
    @Upsert suspend fun saveHistory(history: List<StatusEventEntity>)
    @Upsert suspend fun saveNotifications(notifications: List<NotificationEntity>)

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id AND orderId IN (SELECT id FROM orders WHERE customerId = :customerId)")
    suspend fun markRead(id: String, customerId: String)

    @Query("UPDATE notifications SET isRead = 1 WHERE orderId IN (SELECT id FROM orders WHERE customerId = :customerId)")
    suspend fun markAllRead(customerId: String)
}
