package org.umn.maxwash.data

import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.umn.maxwash.data.local.*
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt

class RoomRepository(private val database: MaxwashDatabase, private val seedData: SeedData) {
    private val dao = database.dao()

    suspend fun initialize() = withContext(Dispatchers.IO) {
        database.withTransaction {
            if (dao.metadata("initialized") == null) {
                val seed = seedData.read()
                dao.saveOutlets(seed.outlets)
                dao.saveServices(seed.services)
                dao.saveFragrances(seed.fragrances)
                dao.savePromotions(seed.promotions)
                dao.insertCustomers(seed.customers)
                dao.saveOrders(seed.orders)
                dao.saveHistory(seed.history)
                dao.saveNotifications(seed.notifications)
                dao.saveSession(SessionEntity())
                dao.insertMetadata(MetadataEntity("demoCustomerId", seed.demoCustomerId))
                dao.insertMetadata(MetadataEntity("initialized", "1"))
            }
        }
    }

    fun observeData(): Flow<LocalData> = database.invalidationTracker.createFlow(
        "customers", "orders", "status_events", "notifications", "outlets", "services",
        "app_session", "fragrances", "promotions"
    ).map { readData() }

    suspend fun readData(): LocalData = database.withTransaction {
        val session = dao.session()
        val account = session?.customerId?.let { dao.customerWithOrders(it) }
        val orders = account?.orders.orEmpty().sortedWith(compareByDescending<OrderWithDetails> { it.order.createdAt }.thenBy { it.order.id })
        LocalData(
            customer = account?.customer?.toModel(),
            orders = orders.map { detail ->
                val order = detail.order
                LaundryOrder(order.id, detail.service.name, order.weightKg, LocalTimeFormat.display(order.createdAt, true),
                    order.outletId, LaundryStatus.valueOf(order.status), order.total,
                    LocalTimeFormat.display(order.estimatedCollectionAt),
                    detail.history.sortedBy { it.occurredAt }.map { StatusEvent(LaundryStatus.valueOf(it.status), LocalTimeFormat.display(it.occurredAt)) },
                    order.createdAt, detail.service.turnaroundHours)
            },
            notifications = orders.flatMap { it.notifications }.sortedByDescending { it.createdAt }.map {
                LaundryNotification(it.id, it.orderId, it.title, it.message, LocalTimeFormat.display(it.createdAt), it.isRead, it.createdAt)
            },
            outlets = dao.outlets().map { Outlet(it.id, it.name, it.address, it.distanceKm, it.openingHours) },
            services = dao.services().map { LaundryService(it.id, it.name, it.pricePerKg, it.turnaroundHours) },
            fragrances = dao.fragrances().map { it.name },
            promotions = dao.promotions().map { Promotion(it.id, it.title, it.message) },
            selectedOutletId = session?.selectedOutletId.orEmpty()
        )
    }

    suspend fun register(name: String, phone: String, email: String, password: String, confirmation: String) = withContext(Dispatchers.IO) {
        require(FormValidation.registration(name, phone, email, password, confirmation)) { "Periksa kembali data pendaftaran." }
        val credentials = PasswordHasher.hash(password)
        database.withTransaction {
            val normalizedEmail = email.trim().lowercase(Locale.ROOT)
            require(dao.conflictingAccounts(normalizedEmail, phone) == 0) { "Email atau nomor HP sudah terdaftar." }
            val customer = CustomerEntity("CUST-${UUID.randomUUID()}", name.trim(), phone, normalizedEmail,
                "", dao.fragrances().firstOrNull()?.name.orEmpty(), "", true, dao.outlets().firstOrNull()?.id,
                "Member", 0, credentials.value, credentials.salt)
            dao.insertCustomers(listOf(customer))
            dao.saveSession(SessionEntity(customerId = customer.id, selectedOutletId = customer.outletId))
        }
    }

    suspend fun login(identifier: String, password: String): Boolean = withContext(Dispatchers.IO) {
        val account = dao.findAccount(identifier.trim().lowercase(Locale.ROOT)) ?: return@withContext false
        if (!PasswordHasher.verify(password, account.passwordHash, account.passwordSalt)) return@withContext false
        dao.saveSession(SessionEntity(customerId = account.id, selectedOutletId = account.outletId))
        true
    }

    suspend fun loginDemo() = database.withTransaction {
        val id = requireNotNull(dao.metadata("demoCustomerId")) { "Akun demo belum tersedia." }
        val account = requireNotNull(dao.customer(id)) { "Akun demo tidak ditemukan." }
        dao.saveSession(SessionEntity(customerId = id, selectedOutletId = account.outletId))
    }

    suspend fun updateProfile(profile: Customer) = database.withTransaction {
        val account = currentAccount()
        require(profile.id == account.id) { "Profil tidak sesuai dengan akun aktif." }
        require(FormValidation.name(profile.name) == null && FormValidation.phone(profile.phone) == null && FormValidation.email(profile.email) == null) {
            "Periksa kembali nama, nomor HP, dan email."
        }
        val email = profile.email.trim().lowercase(Locale.ROOT)
        require(dao.conflictingAccounts(email, profile.phone, account.id) == 0) { "Email atau nomor HP sudah digunakan akun lain." }
        val outletId = profile.outletId.ifBlank { null }
        require(outletId == null || dao.outlets().any { it.id == outletId }) { "Outlet tidak ditemukan." }
        require(profile.fragrance.isBlank() || dao.fragrances().any { it.name == profile.fragrance }) { "Aroma tidak tersedia." }
        dao.updateCustomer(account.copy(name = profile.name.trim(), phone = profile.phone, email = email,
            address = profile.address.trim(), fragrance = profile.fragrance, notes = profile.notes,
            notificationEnabled = profile.notificationEnabled, outletId = outletId))
        if (outletId != account.outletId) dao.saveSession(requireNotNull(dao.session()).copy(selectedOutletId = outletId))
    }

    suspend fun selectOutlet(id: String) = database.withTransaction {
        require(dao.outlets().any { it.id == id }) { "Outlet tidak ditemukan." }
        dao.saveSession(requireNotNull(dao.session()).copy(selectedOutletId = id))
    }

    suspend fun markRead(id: String) = database.withTransaction { dao.markRead(id, currentAccount().id) }
    suspend fun markAllRead() = database.withTransaction { dao.markAllRead(currentAccount().id) }
    suspend fun logout() { dao.saveSession(SessionEntity()) }

    suspend fun createOrder(serviceId: String, weightKg: Double, outletId: String): String = database.withTransaction {
        val account = currentAccount()
        require(weightKg.isFinite() && weightKg > 0 && weightKg <= 100) { "Berat harus lebih dari 0 dan maksimal 100 kg." }
        val service = requireNotNull(dao.services().find { it.id == serviceId }) { "Layanan tidak tersedia." }
        require(dao.outlets().any { it.id == outletId }) { "Outlet tidak ditemukan." }
        val now = System.currentTimeMillis()
        val id = "MW-${UUID.randomUUID().toString().replace("-", "").take(12).uppercase(Locale.ROOT)}"
        dao.insertOrder(OrderEntity(id, account.id, service.id, weightKg, now, outletId,
            LaundryStatus.RECEIVED.name, (weightKg * service.pricePerKg).roundToInt(), now + service.turnaroundHours * 3_600_000L))
        dao.saveHistory(listOf(StatusEventEntity(id, LaundryStatus.RECEIVED.name, now)))
        dao.saveNotifications(listOf(NotificationEntity("N-${UUID.randomUUID()}", id, "Pesanan Dicatat",
            "Pesanan $id dengan layanan ${service.name} tersimpan di perangkat Anda.", now, false)))
        id
    }

    private suspend fun currentAccount(): CustomerEntity =
        requireNotNull(dao.session()?.customerId?.let { dao.customer(it) }) { "Silakan masuk terlebih dahulu." }
}

private fun CustomerEntity.toModel() = Customer(id, name, phone, email, address, fragrance, notes, notificationEnabled, outletId.orEmpty(), membership, points)
