package org.umn.maxwash

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.umn.maxwash.data.*
import org.umn.maxwash.data.local.*
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class RoomRepositoryTest {
    private lateinit var context: Context
    private lateinit var database: MaxwashDatabase
    private lateinit var repository: RoomRepository
    private val databaseName = "room-persistence-test-${UUID.randomUUID()}.db"

    @Before fun setup() = runBlocking {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        openDatabase()
        repository.initialize()
    }

    @After fun cleanup() {
        database.close()
        context.deleteDatabase(databaseName)
    }

    private fun openDatabase() {
        database = Room.databaseBuilder(context, MaxwashDatabase::class.java, databaseName).build()
        repository = RoomRepository(database, CatalogData(context))
    }

    private suspend fun reopen() {
        database.close()
        openDatabase()
        repository.initialize()
    }

    private suspend fun registerBudi() {
        repository.register("Budi Santoso", "081234567891", "budi@example.com", "BudiPass123", "BudiPass123")
    }

    private suspend fun expectInvalid(action: suspend () -> Unit) {
        try { action(); fail("Expected validation failure") } catch (_: IllegalArgumentException) { }
    }

    private fun rowCount(table: String): Int = database.openHelper.readableDatabase.query("SELECT COUNT(*) FROM $table").use {
        it.moveToFirst()
        it.getInt(0)
    }

    @Test fun bootstrapImportsOnlyCatalogAndNeverCreatesAccountsOrOrders() = runBlocking {
        val initial = repository.readData()
        assertNull(initial.customer)
        assertTrue(initial.orders.isEmpty())
        assertTrue(initial.notifications.isEmpty())
        assertEquals(2, initial.outlets.size)
        assertEquals(4, initial.services.size)
        assertEquals(3, initial.fragrances.size)
        listOf("customers", "orders", "status_events", "notifications").forEach { assertEquals(0, rowCount(it)) }
        assertNull(database.dao().metadata("demoCustomerId"))
        assertFalse(repository.login("andi@example.com", "Maxwash123"))
        database.dao().saveServices(listOf(ServiceEntity("SVC-001", "Updated service", 9000, 48)))
        reopen()
        assertEquals(9000, repository.readData().services.single { it.id == "SVC-001" }.pricePerKg)
        assertEquals(0, rowCount("customers"))
    }

    @Test fun legacyAccountIsRemovedWithItsOrdersAndSessionAfterProfileEdits() = runBlocking {
        registerBudi()
        val legacy = repository.readData().customer!!
        repository.createOrder("SVC-001", 2.0, "OUT-001")
        database.dao().insertMetadata(MetadataEntity("demoCustomerId", legacy.id))
        repository.updateProfile(legacy.copy(name = "Changed Name", email = "changed@example.com"))
        reopen()
        assertNull(repository.readData().customer)
        assertNull(database.dao().customer(legacy.id))
        assertEquals(SessionEntity(), database.dao().session())
        assertNull(database.dao().metadata("demoCustomerId"))
        listOf("customers", "orders", "status_events", "notifications").forEach { assertEquals(0, rowCount(it)) }
        assertFalse(repository.login("changed@example.com", "BudiPass123"))
        repository.initialize()
        assertEquals(0, rowCount("customers"))
    }

    @Test fun removingLegacyAccountPreservesRegisteredAccountDataAndActiveSession() = runBlocking {
        repository.register("Legacy Customer", "081234567890", "legacy@example.com", "LegacyPass123", "LegacyPass123")
        val legacyId = repository.readData().customer!!.id
        repository.createOrder("SVC-001", 1.0, "OUT-001")
        database.dao().insertMetadata(MetadataEntity("demoCustomerId", legacyId))
        registerBudi()
        repository.createOrder("SVC-002", 3.0, "OUT-002")
        repository.selectOutlet("OUT-002")
        val realData = repository.readData()
        val realSession = database.dao().session()
        reopen()
        assertEquals(realData, repository.readData())
        assertEquals(realSession, database.dao().session())
        assertNull(database.dao().customer(legacyId))
        assertNull(database.dao().metadata("demoCustomerId"))
        listOf("customers", "orders", "status_events", "notifications").forEach { assertEquals(1, rowCount(it)) }
        repository.logout()
        assertTrue(repository.login("budi@example.com", "BudiPass123"))
        assertEquals(realData.customer, repository.readData().customer)
    }

    @Test fun staleLegacyMetadataIsRemovedWithoutChangingRegisteredSession() = runBlocking {
        registerBudi()
        val initial = repository.readData()
        database.dao().insertMetadata(MetadataEntity("demoCustomerId", "missing-account"))
        reopen()
        assertEquals(initial, repository.readData())
        assertNull(database.dao().metadata("demoCustomerId"))
    }

    @Test fun registrationCredentialsAndSessionSurviveDatabaseReopen() = runBlocking {
        repository.register("Budi Santoso", "081234567891", " BUDI@example.com ", "BudiPass123", "BudiPass123")
        val customer = repository.readData().customer!!
        assertTrue(repository.readData().orders.isEmpty())
        assertTrue(repository.readData().notifications.isEmpty())
        val stored = database.dao().customer(customer.id)!!
        assertNotEquals("BudiPass123", stored.passwordHash)
        assertTrue(PasswordHasher.verify("BudiPass123", stored.passwordHash, stored.passwordSalt))
        reopen()
        assertEquals(customer, repository.readData().customer)
        repository.logout()
        reopen()
        assertNull(repository.readData().customer)
        assertFalse(repository.login("budi@example.com", "WrongPass123"))
        assertTrue(repository.login("081234567891", "BudiPass123"))
        repository.logout()
        assertTrue(repository.login("BUDI@example.com", "BudiPass123"))
    }

    @Test fun profilePreferencesOutletAndReadStateSurviveReopenAndLogin() = runBlocking {
        registerBudi()
        repository.createOrder("SVC-001", 2.0, "OUT-001")
        repository.createOrder("SVC-002", 1.0, "OUT-002")
        val initial = repository.readData()
        val updated = initial.customer!!.copy(name = "Budi Wijaya", email = "wijaya@example.com", fragrance = "Ocean Breeze",
            notes = "Pisahkan pakaian putih", notificationEnabled = false, outletId = "OUT-002")
        repository.updateProfile(updated)
        repository.markRead(initial.notifications.first().id)
        repository.markRead(initial.notifications.first().id)
        repository.selectOutlet("OUT-001")
        reopen()
        assertEquals(updated, repository.readData().customer)
        assertEquals("OUT-001", repository.readData().selectedOutletId)
        assertEquals(1, repository.readData().notifications.count { !it.isRead })
        repository.logout()
        assertFalse(repository.login("budi@example.com", "BudiPass123"))
        assertTrue(repository.login("wijaya@example.com", "BudiPass123"))
        assertEquals(updated, repository.readData().customer)
        assertEquals(2, repository.readData().orders.size)
        assertEquals(1, repository.readData().notifications.count { !it.isRead })
    }

    @Test fun multipleAccountsCannotReadOrMarkEachOthersOrdersAndNotifications() = runBlocking {
        registerBudi()
        val budi = repository.readData().customer!!
        val budiOrderId = repository.createOrder("SVC-001", 2.5, "OUT-002")
        val budiNotificationId = repository.readData().notifications.single().id
        assertEquals(17500, repository.readData().orders.single().total)
        assertEquals(LaundryStatus.RECEIVED, repository.readData().orders.single().status)
        assertEquals(1, repository.readData().orders.single().history.size)
        repository.register("Sari Utami", "081234567892", "sari@example.com", "SariPass123", "SariPass123")
        assertTrue(repository.readData().orders.isEmpty())
        assertTrue(repository.readData().notifications.isEmpty())
        val sariOrderId = repository.createOrder("SVC-002", 1.0, "OUT-001")
        repository.markRead(budiNotificationId)
        expectInvalid { repository.updateProfile(budi) }
        assertEquals(sariOrderId, repository.readData().orders.single().id)
        reopen()
        repository.logout()
        assertTrue(repository.login("budi@example.com", "BudiPass123"))
        assertEquals(budiOrderId, repository.readData().orders.single().id)
        assertFalse(repository.readData().notifications.single().isRead)
        repository.markAllRead()
        reopen()
        assertTrue(repository.readData().notifications.single().isRead)
    }

    @Test fun duplicateIdentifiersAndInvalidOrdersDoNotChangeSavedData() = runBlocking {
        registerBudi()
        val initial = repository.readData()
        expectInvalid { repository.register("Other User", "081234567899", "BUDI@example.com", "OtherPass123", "OtherPass123") }
        expectInvalid { repository.register("Other User", "081234567891", "other@example.com", "OtherPass123", "OtherPass123") }
        listOf(0.0, -1.0, 101.0, Double.NaN, Double.POSITIVE_INFINITY).forEach { kg ->
            expectInvalid { repository.createOrder("SVC-001", kg, "OUT-001") }
        }
        expectInvalid { repository.createOrder("missing", 1.0, "OUT-001") }
        expectInvalid { repository.createOrder("SVC-001", 1.0, "missing") }
        assertEquals(initial, repository.readData())
        repository.register("Sari Utami", "081234567892", "sari@example.com", "SariPass123", "SariPass123")
        val sari = repository.readData().customer!!
        expectInvalid { repository.updateProfile(sari.copy(email = "BUDI@example.com")) }
        assertEquals(sari, repository.readData().customer)
    }

    @Test fun checkoutOrdersKeepQuotedTotalAndTrackingAfterDatabaseReopen() = runBlocking {
        registerBudi()
        val draft = CheckoutDraft(address = "Jl. Pengujian No. 42", outletId = "OUT-002", serviceId = "SVC-003",
            weightInput = "4.8", fragrance = "Ocean Breeze", hypoallergenic = true, promoCode = "NEWCLEAN5K")
        val ids = PaymentMethod.entries.map { repository.createCheckoutOrder(draft.copy(payment = it)) }
        val saved = repository.readData()
        assertEquals(3, saved.orders.size)
        assertEquals(3, saved.notifications.size)
        saved.orders.forEach { order ->
            assertTrue(order.id in ids)
            assertEquals(73800, order.total)
            assertEquals(4.8, order.weightKg, 0.0)
            assertEquals("OUT-002", order.outletId)
            assertEquals(LaundryStatus.RECEIVED, order.status)
            assertEquals(listOf(LaundryStatus.RECEIVED), order.history.map { it.status })
        }
        reopen()
        assertEquals(saved, repository.readData())
        repository.logout()
        assertTrue(repository.login("budi@example.com", "BudiPass123"))
        assertEquals(saved, repository.readData())
    }

    @Test fun invalidCheckoutDoesNotSaveOrdersHistoryOrNotifications() = runBlocking {
        registerBudi()
        val initial = repository.readData()
        val draft = CheckoutDraft(address = "Jl. Pengujian", outletId = "OUT-001", serviceId = "SVC-001", weightInput = "2")
        listOf(draft.copy(address = ""), draft.copy(outletId = "missing"), draft.copy(serviceId = "missing"),
            draft.copy(weightInput = "invalid"), draft.copy(promoCode = "invalid")).forEach { invalid ->
            expectInvalid { repository.createCheckoutOrder(invalid) }
        }
        assertEquals(initial, repository.readData())
        listOf("orders", "status_events", "notifications").forEach { assertEquals(0, rowCount(it)) }
        repository.logout()
        expectInvalid { repository.createCheckoutOrder(draft) }
        listOf("orders", "status_events", "notifications").forEach { assertEquals(0, rowCount(it)) }
    }

    @Test fun databaseChangesEmitNewCatalogAndOrderDataToObservers() = runBlocking {
        registerBudi()
        val orderId = repository.createOrder("SVC-001", 2.0, "OUT-001")
        val ready = CompletableDeferred<Unit>()
        val observed = async {
            withTimeout(10_000) {
                repository.observeData().onEach { ready.complete(Unit) }.first { state ->
                    state.orders.any { it.service == "Updated service" }
                }
            }
        }
        ready.await()
        database.dao().saveServices(listOf(ServiceEntity("SVC-001", "Updated service", 9000, 48)))
        val changed = observed.await()
        assertEquals(9000, changed.services.single { it.id == "SVC-001" }.pricePerKg)
        assertEquals(48, changed.orders.single { it.id == orderId }.turnaroundHours)
        val id = repository.createOrder("SVC-001", 2.0, "OUT-001")
        assertEquals(18000, repository.readData().orders.single { it.id == id }.total)
    }
}
