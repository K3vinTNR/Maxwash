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
            app.mapLookup = { org.umn.maxwash.data.MapPoint(-6.23, 106.81) }
            app.mapClient = app.offlineMapClient()
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

    @Test fun pickupUsesNewProfileAddressAndNotesWithoutRestarting() {
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as TestMaxwashApplication
        val mapRequests = java.util.concurrent.ConcurrentLinkedQueue<String>()
        app.mapLookup = { mapRequests.add(it); org.umn.maxwash.data.MapPoint(-6.23, 106.81) }
        awaitLogin()
        compose.onNodeWithTag("open_register").performClick()
        input("register_name", "Alamat Pelanggan")
        input("register_phone", "081234567898")
        input("register_email", "address@example.com")
        input("register_password", "AddressPass123")
        input("register_confirm", "AddressPass123")
        closeSoftKeyboard()
        compose.onNodeWithTag("register_submit").performScrollTo().performClick()
        awaitTag("home_greeting")
        compose.onNodeWithTag("home_menu_Layanan").performScrollTo().performClick()
        awaitTag("pickup_address")
        compose.onNodeWithTag("pickup_address").performScrollTo().assertTextEquals("Tambahkan alamat penjemputan")
        compose.onNodeWithTag("pickup_continue").assertIsNotEnabled()

        compose.onNodeWithTag("header_profile").performClick()
        saveProfileAddress("Jl. Alamat Profil No. 12")
        compose.onNodeWithTag("profile_notes").performScrollTo().performTextReplacement("Pagar biru dekat pos satpam")
        closeSoftKeyboard()
        compose.onNodeWithTag("save_preferences").performScrollTo().performClick()
        awaitFeedbackGone("Perubahan profil disimpan")
        openPickupFromProfile()
        compose.onNodeWithTag("pickup_address").performScrollTo().assertTextEquals("Jl. Alamat Profil No. 12")
        compose.onNodeWithText("Pagar biru dekat pos satpam").assertExists()
        compose.onNodeWithTag("pickup_continue").assertIsEnabled()
        compose.waitUntil(10_000) { mapRequests.contains("Jl. Alamat Profil No. 12") }

        compose.onNodeWithTag("header_profile").performClick()
        saveProfileAddress("Jl. Alamat Profil Baru No. 34")
        openPickupFromProfile()
        compose.onNodeWithTag("pickup_address").performScrollTo().assertTextEquals("Jl. Alamat Profil Baru No. 34")
        compose.waitUntil(10_000) { mapRequests.contains("Jl. Alamat Profil Baru No. 34") }
        compose.activityRule.scenario.recreate()
        awaitTag("pickup_address")
        compose.onNodeWithTag("pickup_address").performScrollTo().assertTextEquals("Jl. Alamat Profil Baru No. 34")
        compose.onNodeWithText("Pagar biru dekat pos satpam").assertExists()
        val saved = runBlocking { app.repository.readData() }
        org.junit.Assert.assertEquals("Jl. Alamat Profil Baru No. 34", saved.customer!!.address)
        org.junit.Assert.assertTrue(saved.orders.isEmpty())
    }

    @Test fun profileUpdatesPreserveCustomPickupAddressAfterRecreation() {
        login()
        compose.onNodeWithTag("home_menu_Layanan").performScrollTo().performClick()
        awaitTag("pickup_address")
        compose.onNodeWithTag("pickup_edit_address").performScrollTo().performClick()
        compose.onNodeWithTag("pickup_address_input").performTextReplacement("Jl. Penjemputan Khusus No. 56")
        compose.onNodeWithTag("pickup_notes_input").performTextReplacement("Jemput di lobi kantor")
        closeSoftKeyboard()
        compose.onNodeWithTag("pickup_address_save").performClick()
        compose.activityRule.scenario.recreate()
        awaitTag("pickup_address")
        compose.onNodeWithTag("header_profile").performClick()
        saveProfileAddress("Jl. Alamat Rumah No. 78")
        openPickupFromProfile()
        compose.onNodeWithTag("pickup_address").performScrollTo().assertTextEquals("Jl. Penjemputan Khusus No. 56")
        compose.onNodeWithText("Jemput di lobi kantor").assertExists()
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as TestMaxwashApplication
        org.junit.Assert.assertEquals("Jl. Alamat Rumah No. 78", runBlocking { app.repository.readData().customer!!.address })
    }

    private fun saveProfileAddress(address: String) {
        compose.onNodeWithTag("edit_profile").performScrollTo().performClick()
        compose.onNodeWithTag("edit_address").performScrollTo().performTextReplacement(address)
        closeSoftKeyboard()
        compose.onNodeWithTag("edit_save").performClick()
        awaitFeedbackGone("Data diri diperbarui")
        compose.onNodeWithText(address).assertExists()
    }

    private fun openPickupFromProfile() {
        compose.onNodeWithTag("nav_home").performClick()
        awaitTag("home_greeting")
        compose.onNodeWithTag("home_menu_Layanan").performScrollTo().performClick()
        awaitTag("pickup_address")
    }

    @Test fun pickupMapSwitchesAddressAndOutletAndOpensExternalMap() {
        login()
        compose.onNodeWithTag("home_menu_Layanan").performScrollTo().performClick()
        awaitTag("pickup_map")
        awaitText("Titik dari pencarian alamat. Periksa lokasi di peta.")
        compose.onNodeWithTag("pickup_map_address").assertIsSelected()
        compose.onNodeWithTag("pickup_map_target").assertTextEquals("Alamat Penjemputan")
        compose.onNodeWithTag("pickup_map_outlet").performScrollTo().performClick()
        compose.onNodeWithTag("pickup_map_outlet").assertIsSelected()
        compose.onNodeWithTag("pickup_map_target").assertTextEquals("MAXWASH Bintaro Sektor 7")
        compose.onNodeWithTag("pickup_outlet").performScrollTo().performClick()
        compose.onNodeWithTag("pickup_outlet_OUT-002").performClick()
        compose.onNodeWithTag("pickup_map_target").performScrollTo().assertTextEquals("MAXWASH Senopati")
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val filter = IntentFilter(Intent.ACTION_VIEW).apply {
            addDataScheme("https")
            addDataAuthority("www.google.com", null)
            addDataPath("/maps/search/", PatternMatcher.PATTERN_LITERAL)
        }
        val monitor = instrumentation.addMonitor(filter, Instrumentation.ActivityResult(Activity.RESULT_OK, null), true)
        try {
            compose.onNodeWithTag("pickup_open_map").performClick()
            compose.waitForIdle()
            org.junit.Assert.assertEquals(1, monitor.hits)
        } finally { instrumentation.removeMonitor(monitor) }
        compose.onNodeWithTag("pickup_map_address").performScrollTo().performClick()
        compose.activityRule.scenario.recreate()
        awaitTag("pickup_map")
        compose.onNodeWithTag("pickup_map_address").assertIsSelected()
        compose.onNodeWithTag("pickup_map_target").assertTextEquals("Alamat Penjemputan")
    }

    @Test fun unresolvedPickupAddressKeepsNavigationAvailable() {
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as TestMaxwashApplication
        app.mapLookup = { null }
        login()
        compose.onNodeWithTag("home_menu_Layanan").performScrollTo().performClick()
        awaitTag("pickup_map")
        awaitText("Lokasi alamat belum tersedia. Periksa alamat atau coba lagi.")
        compose.onNodeWithTag("pickup_map_status").assertTextEquals("Lokasi alamat belum tersedia. Periksa alamat atau coba lagi.")
        compose.onNodeWithTag("pickup_continue").assertIsEnabled().performClick()
        awaitTag("choose_service_screen")
        org.junit.Assert.assertEquals(2, runBlocking { app.repository.readData().orders.size })
    }

    @Test fun missingTomTomKeyDoesNotRequestLocationsOrBlockServiceNavigation() {
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as TestMaxwashApplication
        app.mapClient = org.umn.maxwash.data.TomTomClient("")
        app.mapLookup = { error("Unconfigured maps must not request addresses") }
        login()
        compose.onNodeWithTag("home_menu_Layanan").performScrollTo().performClick()
        awaitTag("pickup_map_placeholder")
        compose.onNodeWithTag("pickup_map").assertDoesNotExist()
        compose.onNodeWithText("Peta belum tersedia").assertExists()
        compose.onNodeWithTag("pickup_continue").assertIsEnabled().performClick()
        awaitTag("choose_service_screen")
        org.junit.Assert.assertEquals(2, runBlocking { app.repository.readData().orders.size })
    }

    @Test fun checkoutPaymentCreatesOrderOnHomeAndTracksItAfterRecreation() {
        login()
        compose.onNodeWithTag("home_menu_Layanan").performScrollTo().performClick()
        awaitTag("pickup_screen")
        compose.onNodeWithTag("nav_orders").assertDoesNotExist()
        capture("checkout-pickup")
        compose.onNodeWithTag("pickup_edit_address").performScrollTo().performClick()
        compose.onNodeWithTag("pickup_address_input").performTextReplacement("Jl. Senopati No. 42B, Jakarta Selatan")
        compose.onNodeWithTag("pickup_notes_input").performTextReplacement("Pagar hitam depan pos satpam")
        closeSoftKeyboard()
        compose.onNodeWithTag("pickup_address_save").performClick()
        compose.onNodeWithTag("pickup_outlet").performScrollTo().performClick()
        compose.onNodeWithTag("pickup_outlet_OUT-002").performClick()
        compose.onNodeWithTag("pickup_continue").performClick()
        awaitTag("choose_service_screen")
        compose.onNodeWithTag("service_continue").assertIsNotEnabled()
        capture("checkout-services")
        compose.onNodeWithTag("checkout_service_SVC-003").performScrollTo().performClick()
        compose.onNodeWithTag("fragrance_Ocean Breeze").performScrollTo().performClick()
        compose.onNodeWithTag("checkout_hypoallergenic").performScrollTo().performClick()
        input("checkout_weight", "4.8")
        input("checkout_clothing_notes", "Pisahkan pakaian putih")
        closeSoftKeyboard()
        compose.onNodeWithTag("checkout_estimate_total").assertTextEquals("Rp 78.800")
        capture("checkout-service-options")
        compose.onNodeWithTag("service_continue").performClick()
        awaitTag("confirm_payment_screen")
        compose.onNodeWithTag("payment_service").assertTextEquals("Cuci Kilat Express")
        compose.onNodeWithTag("payment_outlet").assertTextEquals("Senopati")
        compose.onNodeWithTag("payment_weight").assertTextEquals("4.8 kg")
        compose.onNodeWithTag("payment_method_AT_OUTLET").performScrollTo().performClick()
        compose.onNodeWithTag("payment_edit_promo").performScrollTo().performClick()
        compose.onNodeWithTag("payment_promo_input").performTextInput("NEWCLEAN5K")
        closeSoftKeyboard()
        compose.onNodeWithTag("payment_promo_save").performClick()
        compose.onNodeWithTag("payment_total").performScrollTo().assertTextEquals("Rp 73.800")
        capture("checkout-payment-bill")
        compose.activityRule.scenario.recreate()
        awaitTag("confirm_payment_screen")
        compose.onNodeWithTag("payment_total").performScrollTo().assertTextEquals("Rp 73.800")
        compose.onNodeWithTag("payment_method_AT_OUTLET").performScrollTo().assertIsSelected()
        compose.onNodeWithTag("payment_service").performScrollTo()
        capture("checkout-payment")
        compose.onNodeWithTag("checkout_back").performClick()
        awaitTag("choose_service_screen")
        compose.onNodeWithTag("checkout_service_SVC-003").performScrollTo().assertIsSelected()
        compose.onNodeWithTag("service_continue").performClick()
        awaitTag("confirm_payment_screen")
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as TestMaxwashApplication
        runBlocking { org.junit.Assert.assertEquals(2, app.repository.readData().orders.size) }
        compose.onNodeWithTag("payment_pay_now").performTouchInput { doubleClick() }
        awaitTag("home_greeting")
        compose.onNodeWithTag("confirm_payment_screen").assertDoesNotExist()
        compose.onNodeWithTag("nav_orders").assertIsDisplayed()
        val data = runBlocking { app.repository.readData() }
        org.junit.Assert.assertEquals(3, data.orders.size)
        val order = data.orders.first()
        org.junit.Assert.assertEquals(73800, order.total)
        org.junit.Assert.assertEquals("OUT-002", order.outletId)
        org.junit.Assert.assertEquals(LaundryStatus.RECEIVED, order.status)
        org.junit.Assert.assertEquals(3, data.notifications.size)
        compose.onNodeWithTag("home_order_id").assertTextEquals("ORDER #${order.id}")
        compose.onNodeWithTag("home_order_service").assertTextEquals(order.service)
        compose.onNodeWithTag("home_order_weight").assertTextEquals("4.8 kg")
        awaitFeedbackGone("Pembayaran berhasil. Pesanan Anda sudah dibuat.")
        capture("checkout-order-home")
        compose.activityRule.scenario.recreate()
        awaitTag("home_order_id")
        compose.onNodeWithTag("home_order_id").assertTextEquals("ORDER #${order.id}")
        compose.onNodeWithTag("home_track").performScrollTo().performClick()
        awaitTag("order_details")
        compose.onNodeWithTag("detail_id").assertTextEquals("#${order.id}")
        compose.onNodeWithTag("detail_service").assertTextEquals(order.service)
        compose.onNodeWithTag("detail_weight").assertTextEquals("4.8 kg")
        compose.onNodeWithTag("detail_total").assertTextEquals("Rp 73.800")
        compose.onNodeWithTag("detail_status").assertTextEquals("Pesanan Diterima")
        capture("checkout-order-tracking")
        compose.activityRule.scenario.recreate()
        awaitTag("detail_id")
        compose.onNodeWithTag("detail_id").assertTextEquals("#${order.id}")
        compose.onNodeWithTag("detail_total").assertTextEquals("Rp 73.800")
        runBlocking { org.junit.Assert.assertEquals(3, app.repository.readData().orders.size) }
        compose.onNodeWithTag("back").performClick()
        awaitTag("home_greeting")
        compose.onNodeWithTag("home_menu_Layanan").performScrollTo().performClick()
        compose.onNodeWithTag("pickup_continue").performClick()
        awaitTag("choose_service_screen")
        compose.onNodeWithTag("service_continue").assertIsNotEnabled()
    }

    @Test fun dropOffCanNavigateWithoutAddingAnAddressAndUnavailableCategoriesCannotContinue() {
        login()
        compose.onNodeWithTag("home_menu_Layanan").performScrollTo().performClick()
        awaitTag("pickup_screen")
        compose.onNodeWithTag("collection_DROP_OFF").performScrollTo().performClick()
        compose.onNodeWithTag("pickup_address").assertDoesNotExist()
        compose.onNodeWithTag("pickup_continue").performClick()
        awaitTag("choose_service_screen")
        compose.onNodeWithTag("category_Satuan / Bedding").performClick()
        compose.onNodeWithText("Layanan belum tersedia").assertExists()
        compose.onNodeWithTag("service_continue").assertIsNotEnabled()
        compose.onNodeWithTag("category_Kiloan").performClick()
        input("checkout_weight", "2")
        closeSoftKeyboard()
        compose.onNodeWithTag("service_continue").performClick()
        awaitTag("confirm_payment_screen")
        compose.onNodeWithTag("payment_collection").assertTextContains("Antar Mandiri", substring = true)
        compose.onNodeWithTag("checkout_back").performClick()
        compose.onNodeWithTag("checkout_back").performClick()
        awaitTag("pickup_screen")
        compose.onNodeWithTag("back").performClick()
        awaitTag("home_greeting")
        compose.onNodeWithTag("nav_orders").assertIsDisplayed()
    }
}
