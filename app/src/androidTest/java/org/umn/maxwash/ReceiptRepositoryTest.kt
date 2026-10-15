package org.umn.maxwash

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.umn.maxwash.data.*
import org.umn.maxwash.data.local.*
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ReceiptRepositoryTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val name = "receipt-test-${UUID.randomUUID()}.db"
    private lateinit var database: MaxwashDatabase
    private lateinit var repository: RoomRepository

    private fun open() {
        database = Room.databaseBuilder(context, MaxwashDatabase::class.java, name)
            .addMigrations(MaxwashDatabase.MIGRATION_1_2).build()
        repository = RoomRepository(database, CatalogData(context))
    }

    @Before fun setup() = runBlocking {
        open()
        repository.initialize()
        repository.register("Sari Putri", "081234567890", "sari@example.com", "ReceiptPass123", "ReceiptPass123")
    }

    @After fun cleanup() { database.close(); context.deleteDatabase(name) }

    private fun draft() = CheckoutDraft(address = "Jl. Senopati No. 42, Jakarta Selatan", outletId = "OUT-002",
        serviceId = "SVC-003", weightInput = "4.8", fragrance = "Ocean Breeze", hypoallergenic = true,
        clothingNotes = "Pisahkan pakaian putih", promoCode = "NEWCLEAN5K")

    @Test fun receiptKeepsOriginalIdentityPricesAndPaymentAfterEditsReopenAndAccountSwitch() = runBlocking {
        val id = repository.createCheckoutOrder(draft())
        val original = repository.readData().receipts.single()
        assertEquals(id, original.orderId)
        assertTrue(original.isPaid && original.hasValidTotal)
        assertEquals(76800, original.subtotal)
        assertEquals(2000, original.detergent)
        assertEquals(5000, original.discount)
        assertEquals(73800, original.total)
        assertEquals(PaymentMethod.QRIS.name, original.paymentMethod)
        val customer = repository.readData().customer!!
        repository.updateProfile(customer.copy(name = "Nama Baru", address = "Alamat Baru"))
        val dao = database.dao()
        dao.saveServices(dao.services().map { it.copy(name = "Layanan Baru", pricePerKg = 99000) })
        dao.saveOutlets(dao.outlets().map { it.copy(name = "Outlet Baru", address = "Alamat Outlet Baru") })
        database.close(); open(); repository.initialize()
        assertEquals(original, repository.readData().receipts.single())
        repository.register("Budi", "081234567891", "budi@example.com", "BudiPass123", "BudiPass123")
        assertTrue(repository.readData().receipts.isEmpty())
        assertTrue(repository.login("sari@example.com", "ReceiptPass123"))
        assertEquals(original, repository.readData().receipts.single())
    }

    @Test fun onlyPaidCheckoutsHavePaidReceiptsAndManualRecordsDoNotInventPayment() = runBlocking {
        val qris = repository.createCheckoutOrder(draft())
        val bank = repository.createCheckoutOrder(draft().copy(payment = PaymentMethod.VIRTUAL_ACCOUNT))
        val cash = repository.createCheckoutOrder(draft().copy(payment = PaymentMethod.AT_OUTLET))
        val manual = repository.createOrder("SVC-001", 2.0, "OUT-001")
        val receipts = repository.readData().receipts
        assertEquals(setOf(qris, bank), receipts.filter { it.isPaid }.map { it.orderId }.toSet())
        assertNull(receipts.single { it.orderId == cash }.paidAt)
        assertFalse(receipts.any { it.orderId == manual })
        assertEquals(3, receipts.map { it.number }.distinct().size)
    }

    @Test fun failingReceiptInsertRollsBackOrderHistoryAndNotification() = runBlocking {
        val before = repository.readData()
        database.openHelper.writableDatabase.execSQL("CREATE TRIGGER reject_receipt BEFORE INSERT ON receipts BEGIN SELECT RAISE(ABORT, 'receipt unavailable'); END")
        try {
            try { repository.createCheckoutOrder(draft()); fail("Expected receipt failure") }
            catch (_: android.database.sqlite.SQLiteException) { }
            assertEquals(before, repository.readData())
        } finally { database.openHelper.writableDatabase.execSQL("DROP TRIGGER reject_receipt") }
    }

    @Test fun versionOneMigrationPreservesAccountsOrdersHistoryAndNotificationsWithoutInventingPaidReceipts() = runBlocking {
        database.close()
        context.deleteDatabase(name)
        val testContext = InstrumentationRegistry.getInstrumentation().context
        val schema = JSONObject(testContext.assets.open("org.umn.maxwash.data.local.MaxwashDatabase/1.json")
            .bufferedReader().use { it.readText() }).getJSONObject("database")
        val file = context.getDatabasePath(name)
        file.parentFile!!.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(file, null).use { old ->
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i)
                val table = entity.getString("tableName")
                old.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                val indices = entity.optJSONArray("indices")
                if (indices != null) for (j in 0 until indices.length()) {
                    old.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table))
                }
            }
            val setup = schema.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) old.execSQL(setup.getString(i))
            old.execSQL("INSERT INTO outlets VALUES ('OUT-001','MAXWASH Lama','Alamat Lama',1.2,'08:00-21:00')")
            old.execSQL("INSERT INTO services VALUES ('SVC-001','Wash & Fold',7000,24)")
            val password = PasswordHasher.hash("OldPass123")
            old.execSQL("INSERT INTO customers VALUES ('OLD','Data Lama','081234567899','old@example.com','Alamat','','',1,'OUT-001','Member',0,?,?)",
                arrayOf(password.value, password.salt))
            old.execSQL("INSERT INTO orders VALUES ('OLD-ORDER','OLD','SVC-001',2.0,1000,'OUT-001','RECEIVED',14000,86401000)")
            old.execSQL("INSERT INTO status_events VALUES ('OLD-ORDER','RECEIVED',1000)")
            old.execSQL("INSERT INTO notifications VALUES ('OLD-N','OLD-ORDER','Pesanan Dicatat','Data lama',1000,0)")
            old.execSQL("INSERT INTO app_session VALUES (1,'OLD','OUT-001')")
            old.execSQL("INSERT INTO metadata VALUES ('initialized','1')")
            old.version = 1
        }
        open(); repository.initialize()
        val migrated = repository.readData()
        assertEquals("Data Lama", migrated.customer!!.name)
        assertEquals("OLD-ORDER", migrated.orders.single().id)
        assertEquals(14000, migrated.orders.single().total)
        assertEquals(1, migrated.orders.single().history.size)
        assertEquals(1, migrated.notifications.size)
        assertTrue(migrated.receipts.isEmpty())
        repository.logout()
        assertTrue(repository.login("old@example.com", "OldPass123"))
        repository.createCheckoutOrder(CheckoutDraft(address = "Alamat", outletId = "OUT-001", serviceId = "SVC-001", weightInput = "2"))
        assertTrue(repository.readData().receipts.single().isPaid)
        assertEquals(2, repository.readData().orders.size)
    }
}
