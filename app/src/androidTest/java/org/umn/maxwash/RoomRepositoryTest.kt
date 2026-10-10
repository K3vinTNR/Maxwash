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
        repository = RoomRepository(database, SeedData(context))
    }

    private suspend fun reopen() {
        database.close()
        openDatabase()
        repository.initialize()
    }

    private suspend fun expectInvalid(action: suspend () -> Unit) {
        try { action(); fail("Expected validation failure") } catch (_: IllegalArgumentException) { }
    }

    @Test fun seedIsImportedOnceAndHasConsistentRelations() = runBlocking {
        assertNull(repository.readData().customer)
        repository.loginDemo()
        val initial = repository.readData()
        assertEquals(6, initial.orders.size)
        assertEquals(5, initial.notifications.size)
        assertEquals(2, initial.notifications.count { !it.isRead })
        assertEquals(LaundryStatus.entries.toSet(), initial.orders.map { it.status }.toSet())
        initial.orders.forEach {
            assertEquals(it.status, it.history.last().status)
            assertEquals(LaundryStatus.entries.take(it.status.ordinal + 1), it.history.map { event -> event.status })
            assertTrue(initial.outlets.any { outlet -> outlet.id == it.outletId })
        }
        initial.notifications.forEach { assertTrue(initial.orders.any { order -> order.id == it.orderId }) }
        repository.markAllRead()
        repository.initialize()
        assertEquals(6, repository.readData().orders.size)
        assertTrue(repository.readData().notifications.all { it.isRead })
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

    @Test fun profilePreferencesOutletAndReadStateSurviveReopenAndDemoLogin() = runBlocking {
        repository.loginDemo()
        val customer = repository.readData().customer!!
        val updated = customer.copy(name = "Andi Wijaya", email = "wijaya@example.com", fragrance = "Ocean Breeze",
            notes = "Pisahkan pakaian putih", notificationEnabled = false, outletId = "OUT-002")
        repository.updateProfile(updated)
        repository.markRead("N-001")
        repository.markRead("N-001")
        repository.selectOutlet("OUT-001")
        reopen()
        assertEquals(updated, repository.readData().customer)
        assertEquals("OUT-001", repository.readData().selectedOutletId)
        assertEquals(1, repository.readData().notifications.count { !it.isRead })
        repository.logout()
        assertFalse(repository.login("andi@example.com", "Maxwash123"))
        assertTrue(repository.login("wijaya@example.com", "Maxwash123"))
        repository.logout()
        repository.loginDemo()
        assertEquals(updated, repository.readData().customer)
        assertEquals(6, repository.readData().orders.size)
        assertEquals(1, repository.readData().notifications.count { !it.isRead })
    }

    @Test fun multipleAccountsCannotReadOrMarkEachOthersOrdersAndNotifications() = runBlocking {
        repository.register("Budi Santoso", "081234567891", "budi@example.com", "BudiPass123", "BudiPass123")
        val budi = repository.readData().customer!!
        val id = repository.createOrder("SVC-001", 2.5, "OUT-002")
        repository.markRead("N-001")
        val order = repository.readData().orders.single()
        assertEquals(id, order.id)
        assertEquals(17500, order.total)
        assertEquals(LaundryStatus.RECEIVED, order.status)
        assertEquals(1, order.history.size)
        assertEquals(id, repository.readData().notifications.single().orderId)
        repository.logout()
        repository.loginDemo()
        assertEquals(6, repository.readData().orders.size)
        assertTrue(repository.readData().orders.none { it.id == id })
        assertFalse(repository.readData().notifications.single { it.id == "N-001" }.isRead)
        expectInvalid { repository.updateProfile(budi) }
        reopen()
        repository.logout()
        assertTrue(repository.login("budi@example.com", "BudiPass123"))
        assertEquals(id, repository.readData().orders.single().id)
        repository.markAllRead()
        reopen()
        assertTrue(repository.readData().notifications.single().isRead)
    }

    @Test fun duplicateIdentifiersAndInvalidOrdersDoNotChangeSavedData() = runBlocking {
        repository.loginDemo()
        val initial = repository.readData()
        expectInvalid { repository.register("Other User", "081234567899", "ANDI@example.com", "OtherPass123", "OtherPass123") }
        expectInvalid { repository.register("Other User", "081234567890", "other@example.com", "OtherPass123", "OtherPass123") }
        listOf(0.0, -1.0, 101.0, Double.NaN, Double.POSITIVE_INFINITY).forEach { kg ->
            expectInvalid { repository.createOrder("SVC-001", kg, "OUT-001") }
        }
        expectInvalid { repository.createOrder("missing", 1.0, "OUT-001") }
        expectInvalid { repository.createOrder("SVC-001", 1.0, "missing") }
        assertEquals(initial, repository.readData())
        repository.register("Budi Santoso", "081234567891", "budi@example.com", "BudiPass123", "BudiPass123")
        val budi = repository.readData().customer!!
        expectInvalid { repository.updateProfile(budi.copy(email = "ANDI@example.com")) }
        assertEquals(budi, repository.readData().customer)
    }

    @Test fun databaseChangesEmitNewCatalogAndOrderDataToObservers() = runBlocking {
        repository.loginDemo()
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
        assertEquals(48, changed.orders.single { it.id == "MW-1001" }.turnaroundHours)
        val id = repository.createOrder("SVC-001", 2.0, "OUT-001")
        assertEquals(18000, repository.readData().orders.single { it.id == id }.total)
    }
}
