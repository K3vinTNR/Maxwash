package org.umn.maxwash

import android.graphics.Bitmap
import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.content.IntentFilter
import android.os.PatternMatcher
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import java.io.File
import org.umn.maxwash.data.LaundryStatus
import org.umn.maxwash.data.local.StatusEventEntity

@RunWith(AndroidJUnit4::class)
class MaxwashFlowTest {
    val compose = createAndroidComposeRule<MainActivity>()
    private lateinit var activeOrderId: String
    private lateinit var completedOrderId: String
    private lateinit var activeNotificationId: String
    @get:Rule val rules: TestRule = RuleChain.outerRule(object : ExternalResource() {
        override fun before() {
            val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as TestMaxwashApplication
            app.database.clearAllTables()
            runBlocking {
                val repository = app.repository
                repository.initialize()
                repository.register("Test Customer", "081234567899", "customer@example.com", "CustomerPass123", "CustomerPass123")
                val customer = repository.readData().customer!!
                repository.updateProfile(customer.copy(address = "Jl. Pengujian No. 1"))
                activeOrderId = repository.createOrder("SVC-001", 3.0, "OUT-001")
                completedOrderId = repository.createOrder("SVC-003", 2.0, "OUT-002")
                val dao = app.database.dao()
                val orders = dao.customerWithOrders(customer.id)!!.orders
                dao.saveOrders(orders.map { detail -> detail.order.copy(status =
                    if (detail.order.id == activeOrderId) LaundryStatus.PROCESSING.name else LaundryStatus.COLLECTED.name) })
                dao.saveHistory(orders.flatMap { detail ->
                    val status = if (detail.order.id == activeOrderId) LaundryStatus.PROCESSING else LaundryStatus.COLLECTED
                    LaundryStatus.entries.take(status.ordinal + 1).mapIndexed { index, stage ->
                        StatusEventEntity(detail.order.id, stage.name, detail.order.createdAt + index * 1000L)
                    }
                })
                activeNotificationId = repository.readData().notifications.single { it.orderId == activeOrderId }.id
                repository.logout()
            }
        }
    }).around(compose)
    private fun awaitLogin() = awaitTag("login_submit")
    private fun login() {
        awaitLogin()
        input("login_identifier", "customer@example.com")
        input("login_password", "CustomerPass123")
        closeSoftKeyboard()
        compose.onNodeWithTag("login_submit").performScrollTo().performClick()
        awaitTag("home_greeting")
        compose.onNodeWithTag("home_greeting").assertIsDisplayed()
    }
    private fun awaitTag(tag: String) = compose.waitUntil(10_000) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
    private fun awaitText(text: String) = compose.waitUntil(10_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    private fun capture(name: String) {
        closeSoftKeyboard(); compose.waitForIdle()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.getExternalFilesDir("screenshots"), "$name.png")
        file.parentFile!!.mkdirs()
        val bitmap = runCatching { compose.onRoot().captureToImage().asAndroidBitmap() }
            .getOrElse { InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot() }
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
    private fun input(tag: String, text: String) { compose.onNodeWithTag(tag).performScrollTo().performTextInput(text) }
    private fun awaitFeedbackGone(text: String) {
        compose.mainClock.advanceTimeBy(6_000)
        compose.waitForIdle()
        compose.waitUntil(10_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isEmpty() }
    }
    @Test fun loginHasNoDemoEntry() {
        awaitLogin()
        compose.onNodeWithText("Coba akun demo").assertDoesNotExist()
        compose.onNodeWithTag("demo_login").assertDoesNotExist()
        compose.onNodeWithTag("open_register").assertExists()
        compose.onNodeWithTag("nav_home").assertDoesNotExist()
    }

    @Test fun orderIdFilterAndBackNavigation() {
        awaitLogin(); capture("login"); login(); capture("home")
        compose.onNodeWithTag("nav_orders").performClick(); capture("my-orders")
        compose.onNodeWithTag("order_$activeOrderId").performClick()
        compose.onNodeWithTag("detail_id").assertTextEquals("#$activeOrderId")
        compose.onNodeWithTag("detail_status").assertTextEquals("Sedang Diproses"); capture("order-details")
        compose.onNodeWithText("Hubungi Kasir Outlet").assertDoesNotExist()
        compose.onNodeWithTag("back").performClick()
        compose.onNodeWithTag("filter_COMPLETED").performClick()
        compose.onNodeWithTag("order_$activeOrderId").assertDoesNotExist()
        compose.onNodeWithTag("order_$completedOrderId").performClick()
        compose.onNodeWithTag("detail_id").assertTextEquals("#$completedOrderId")
        compose.onNodeWithTag("detail_status").assertTextEquals("Sudah Diambil")
        compose.onNodeWithTag("back").performClick()
        compose.onNodeWithTag("order_$completedOrderId").assertExists()
    }
    @Test fun registerValidatesAndUpdatesCustomerAcrossScreens() {
        awaitLogin(); compose.onNodeWithTag("open_register").performClick(); capture("register")
        compose.onNodeWithTag("register_submit").assertIsNotEnabled()
        input("register_name", "Budi Santoso")
        input("register_phone", "abc")
        compose.onNodeWithText("Nomor handphone hanya boleh berisi angka").assertExists()
        compose.onNodeWithTag("register_phone").performTextReplacement("081234567891")
        input("register_email", "bad-email")
        compose.onNodeWithText("Format email tidak valid").assertExists()
        capture("register-errors")
        compose.onNodeWithTag("register_email").performTextReplacement("budi@example.com")
        input("register_password", "BudiPass123"); input("register_confirm", "different")
        compose.onNodeWithText("Konfirmasi password tidak cocok").assertExists()
        compose.onNodeWithTag("register_submit").assertIsNotEnabled()
        compose.onNodeWithTag("register_confirm").performTextReplacement("BudiPass123")
        closeSoftKeyboard()
        compose.onNodeWithTag("register_submit").performScrollTo().assertIsEnabled().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("home_greeting").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("home_greeting").assertTextContains("Budi", substring = true)
        compose.onNodeWithTag("nav_profile").performClick()
        compose.onNodeWithTag("profile_name").assertTextEquals("Budi Santoso")
    }
    @Test fun locationSelectionAndDirectionOpenMaps() {
        login(); compose.onNodeWithText("Lokasi", useUnmergedTree = true).performScrollTo().performClick(); capture("laundry-location")
        compose.onNodeWithTag("outlet_OUT-002").performScrollTo().performClick()
        awaitText("MAXWASH Senopati")
        compose.onNodeWithTag("location_outlet").assertTextEquals("MAXWASH Senopati")
        compose.onNodeWithTag("location_distance").assertDoesNotExist()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val filter = IntentFilter(Intent.ACTION_VIEW).apply {
            addDataScheme("https")
            addDataAuthority("www.google.com", null)
            addDataPath("/maps/dir/", PatternMatcher.PATTERN_LITERAL)
        }
        val monitor = instrumentation.addMonitor(filter, Instrumentation.ActivityResult(Activity.RESULT_OK, null), true)
        try {
            compose.onNodeWithTag("direction").performScrollTo().performClick()
            compose.waitForIdle()
            org.junit.Assert.assertEquals(1, monitor.hits)
            compose.onNodeWithText("Petunjuk Arah Demo").assertDoesNotExist()
        } finally { instrumentation.removeMonitor(monitor) }
    }
    @Test fun profileEditsPreferencesAndLogoutWithoutQrDemo() {
        login(); compose.onNodeWithTag("nav_profile").performClick(); capture("profile")
        compose.onNodeWithTag("open_qr").assertDoesNotExist()
        compose.onNodeWithTag("customer_qr").assertDoesNotExist()
        compose.onNodeWithTag("edit_profile").performClick()
        compose.onNodeWithTag("edit_name").performTextReplacement("Customer Wijaya")
        closeSoftKeyboard(); compose.onNodeWithTag("edit_save").performClick()
        awaitText("Customer Wijaya")
        compose.onNodeWithTag("profile_name").assertTextEquals("Customer Wijaya")
        awaitFeedbackGone("Data diri diperbarui")
        compose.onNodeWithTag("profile_fragrance").performScrollTo().performClick()
        compose.onNodeWithText("Ocean Breeze").performClick()
        compose.onNodeWithTag("profile_notifications").performScrollTo().assertIsOn().performClick()
        compose.onNodeWithTag("profile_notifications").assertIsOff()
        compose.onNodeWithTag("save_preferences").performScrollTo().performClick()
        awaitFeedbackGone("Perubahan profil disimpan"); capture("profile-preferences")
        compose.onNodeWithTag("nav_home").performClick()
        compose.onNodeWithTag("home_greeting").assertTextContains("Customer", substring = true)
        compose.onNodeWithTag("nav_profile").performClick()
        compose.onNodeWithTag("profile_notifications").performScrollTo().assertIsOff()
        compose.onNodeWithTag("profile_fragrance").performScrollTo().assertTextContains("Ocean Breeze", substring = true)
        compose.onNodeWithTag("logout").performScrollTo().performClick()
        awaitLogin()
        compose.onNodeWithTag("demo_login").assertDoesNotExist()
        compose.onNodeWithTag("nav_home").assertDoesNotExist()
    }
    @Test fun notificationsOpenTheirOwnOrderAndRetainReadState() {
        login(); compose.onNodeWithTag("nav_notifications").performClick(); capture("notifications")
        compose.onNodeWithTag("notification_$activeNotificationId").performClick()
        compose.onNodeWithTag("detail_id").assertTextEquals("#$activeOrderId")
        compose.onNodeWithTag("back").performClick()
        awaitText("1 Baru")
        compose.onNodeWithText("1 Baru").assertExists()
        compose.onNodeWithTag("mark_all_read").performClick()
        awaitText("0 Baru")
        compose.onNodeWithText("0 Baru").assertExists()
        compose.onNodeWithTag("unread_filter").performClick()
        compose.onNodeWithTag("notifications_empty").assertExists()
    }
    @Test fun stateSurvivesActivityRecreationAndEmptySearch() {
        login(); compose.onNodeWithTag("nav_orders").performClick()
        compose.onNodeWithTag("filter_COMPLETED").performClick()
        compose.activityRule.scenario.recreate(); compose.waitForIdle()
        compose.onNodeWithTag("order_$completedOrderId").assertExists()
        compose.onNodeWithTag("order_$activeOrderId").assertDoesNotExist()
        compose.onNodeWithTag("order_search").performTextInput("does-not-exist")
        compose.onNodeWithTag("orders_empty").assertExists()
    }

    @Test fun newAccountCanCreateAndReloadItsOwnLocalOrder() {
        awaitLogin()
        compose.onNodeWithTag("open_register").performClick()
        input("register_name", "Sari Utami")
        input("register_phone", "081234567892")
        input("register_email", "sari@example.com")
        input("register_password", "SariPass123")
        input("register_confirm", "SariPass123")
        closeSoftKeyboard()
        compose.onNodeWithTag("register_submit").performScrollTo().performClick()
        awaitTag("home_greeting")
        compose.onNodeWithTag("home_orders_empty").assertExists()
        compose.onNodeWithTag("nav_orders").performClick()
        compose.onNodeWithTag("order_$activeOrderId").assertDoesNotExist()
        compose.onNodeWithTag("add_order").performClick()
        input("new_order_weight", "2.5")
        closeSoftKeyboard()
        compose.onNodeWithTag("new_order_save").performClick()
        awaitTag("detail_id")
        compose.onNodeWithTag("detail_status").assertTextEquals("Pesanan Diterima")
        compose.activityRule.scenario.recreate()
        compose.waitForIdle()
        compose.onNodeWithTag("detail_status").assertTextEquals("Pesanan Diterima")
        compose.onNodeWithTag("back").performClick()
        compose.onNodeWithTag("order_$activeOrderId").assertDoesNotExist()
        compose.onNodeWithText("Rp 17.500").assertExists()
    }
}
