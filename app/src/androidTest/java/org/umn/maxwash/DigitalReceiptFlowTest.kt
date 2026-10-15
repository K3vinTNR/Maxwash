package org.umn.maxwash

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.umn.maxwash.data.*
import org.umn.maxwash.data.local.StatusEventEntity
import java.io.File

@RunWith(AndroidJUnit4::class)
class DigitalReceiptFlowTest {
    private val compose = createAndroidComposeRule<MainActivity>()
    private lateinit var latest: String
    private lateinit var older: String
    private lateinit var pending: String
    private lateinit var manual: String
    @get:Rule val rules: TestRule = RuleChain.outerRule(object : ExternalResource() {
        override fun before() {
            val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as TestMaxwashApplication
            app.mapClient = app.offlineMapClient()
            app.database.clearAllTables()
            runBlocking {
                val repository = app.repository
                repository.initialize()
                repository.register("Sari Putri", "081234567890", "sari@example.com", "ReceiptPass123", "ReceiptPass123")
                val draft = CheckoutDraft(address = "Jl. Senopati No. 42B, Jakarta Selatan", outletId = "OUT-002", serviceId = "SVC-002",
                    weightInput = "3.8", fragrance = "Lavender Fresh", payment = PaymentMethod.VIRTUAL_ACCOUNT)
                older = repository.createCheckoutOrder(draft)
                latest = repository.createCheckoutOrder(draft.copy(serviceId = "SVC-003", weightInput = "4.8",
                    fragrance = "Ocean Breeze", hypoallergenic = true, promoCode = "NEWCLEAN5K",
                    clothingNotes = "Pisahkan pakaian putih", payment = PaymentMethod.QRIS))
                pending = repository.createCheckoutOrder(draft.copy(payment = PaymentMethod.AT_OUTLET))
                manual = repository.createOrder("SVC-001", 2.0, "OUT-001")
                val dao = app.database.dao()
                val old = dao.customerWithOrders(repository.readData().customer!!.id)!!.orders.single { it.order.id == older }.order
                dao.saveOrders(listOf(old.copy(status = LaundryStatus.COLLECTED.name)))
                dao.saveHistory(listOf(StatusEventEntity(older, LaundryStatus.COLLECTED.name, System.currentTimeMillis())))
            }
        }
    }).around(compose)

    private fun await(tag: String) = compose.waitUntil(20_000) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
    private fun capture(name: String) {
        compose.waitForIdle()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.getExternalFilesDir("receipt-verification"), "$name.png")
        file.parentFile!!.mkdirs()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        file.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun homeListsPaidHistoryAndSaveButtonProducesPdfForTheSelectedReceipt() {
        await("home_greeting")
        compose.onNodeWithTag("home_menu_Nota_Digital").performScrollTo().performClick()
        await("receipts_list")
        compose.onNodeWithTag("receipt_$latest").assertExists()
        compose.onNodeWithTag("receipt_$pending").assertDoesNotExist()
        capture("receipt-screen-top")
        compose.onNodeWithTag("receipt_download_$latest").performScrollTo()
        compose.onNodeWithTag("receipt_total_$latest").assertTextEquals("Rp 73.800")
        capture("receipt-screen-bottom")
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val output = File(instrumentation.targetContext.getExternalFilesDir("receipt-verification"), "receipt-from-ui.pdf")
        output.delete()
        val filter = IntentFilter(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            addDataType("application/pdf")
        }
        val monitor = instrumentation.addMonitor(filter,
            Instrumentation.ActivityResult(Activity.RESULT_OK, Intent().setData(Uri.fromFile(output))), true)
        try {
            compose.onNodeWithTag("receipt_download_$latest").performClick()
            compose.waitUntil(15_000) { output.exists() && output.length() > 1000 }
            assertEquals(1, monitor.hits)
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Nota PDF berhasil disimpan.").fetchSemanticsNodes().isNotEmpty() }
        } finally { instrumentation.removeMonitor(monitor) }
        compose.onNodeWithTag("receipts_list").performScrollToNode(hasTestTag("receipt_$older"))
        compose.onNodeWithTag("receipt_download_$older").performScrollTo()
        compose.onNodeWithText("Status Pengambilan · Sudah Diambil").assertExists()
        capture("receipt-screen-older")
        compose.activityRule.scenario.recreate()
        await("receipts_list")
        compose.onNodeWithTag("back").performClick()
        await("home_greeting")
        compose.onNodeWithTag("nav_orders").performClick()
        await("orders_list")
        compose.onNodeWithTag("orders_list").performScrollToNode(hasTestTag("order_$latest"))
        compose.onNodeWithTag("order_receipt_$latest").performClick()
        await("receipts_list")
        compose.onNodeWithTag("receipt_$latest").assertExists()
    }

    @Test fun pendingAndLegacyOrdersNeverDisplayPaidReceiptsOrDownloadButtons() {
        await("home_greeting")
        compose.onNodeWithTag("nav_orders").performClick()
        await("orders_list")
        compose.onNodeWithTag("orders_list").performScrollToNode(hasTestTag("order_$pending"))
        compose.onNodeWithTag("order_receipt_$pending").performClick()
        await("receipt_unavailable")
        compose.onNodeWithTag("receipt_download_$pending").assertDoesNotExist()
        compose.onNodeWithText("✓ Lunas").assertDoesNotExist()
        compose.onNodeWithTag("back").performClick()
        await("orders_list")
        compose.onNodeWithTag("orders_list").performScrollToNode(hasTestTag("order_$manual"))
        compose.onNodeWithTag("order_receipt_$manual").performClick()
        await("receipt_unavailable")
        compose.onNodeWithTag("receipt_download_$manual").assertDoesNotExist()
        compose.onNodeWithTag("receipt_show_all").performClick()
        await("receipts_list")
        compose.onNodeWithTag("receipt_$latest").assertExists()
    }
}
