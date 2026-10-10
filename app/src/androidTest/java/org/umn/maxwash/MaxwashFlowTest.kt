package org.umn.maxwash

import android.graphics.Bitmap
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

@RunWith(AndroidJUnit4::class)
class MaxwashFlowTest {
    val compose = createAndroidComposeRule<MainActivity>()
    @get:Rule val rules: TestRule = RuleChain.outerRule(object : ExternalResource() {
        override fun before() {
            val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as TestMaxwashApplication
            app.database.clearAllTables()
            runBlocking { app.repository.initialize() }
        }
    }).around(compose)
    private fun awaitLogin() = compose.waitUntil(10_000) { compose.onAllNodesWithTag("demo_login").fetchSemanticsNodes().isNotEmpty() }
    private fun demo() { awaitLogin(); compose.onNodeWithTag("demo_login").performClick(); awaitTag("home_greeting"); compose.onNodeWithTag("home_greeting").assertIsDisplayed() }
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
    private fun captureNative(name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.getExternalFilesDir("screenshots"), "$name.png")
        file.outputStream().use { InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun orderIdFilterAndBackNavigation() {
        awaitLogin(); capture("login"); demo(); capture("home")
        compose.onNodeWithTag("nav_orders").performClick(); capture("my-orders")
        compose.onNodeWithTag("order_MW-1001").performClick()
        compose.onNodeWithTag("detail_id").assertTextEquals("#MW-1001")
        compose.onNodeWithTag("detail_status").assertTextEquals("Sedang Diproses"); capture("order-details")
        compose.onNodeWithText("Hubungi Kasir Outlet").performScrollTo(); capture("order-timeline")
        compose.onNodeWithTag("back").performClick()
        compose.onNodeWithTag("filter_COMPLETED").performClick()
        compose.onNodeWithTag("order_MW-1001").assertDoesNotExist()
        compose.onNodeWithTag("order_MW-1003").performClick()
        compose.onNodeWithTag("detail_id").assertTextEquals("#MW-1003")
        compose.onNodeWithTag("detail_status").assertTextEquals("Sudah Diambil")
        compose.onNodeWithTag("back").performClick()
        compose.onNodeWithTag("order_MW-1003").assertExists()
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
    @Test fun locationSelectionRefreshAndDirectionAreLocal() {
        demo(); compose.onNodeWithText("Lokasi", useUnmergedTree = true).performScrollTo().performClick(); capture("laundry-location")
        compose.onNodeWithTag("outlet_OUT-002").performScrollTo().performClick()
        awaitText("MAXWASH Senopati")
        compose.onNodeWithTag("location_outlet").assertTextEquals("MAXWASH Senopati")
        compose.onNodeWithTag("refresh_location").performScrollTo().performClick()
        compose.onNodeWithTag("location_distance").assertTextContains("2.5 km", substring = true)
        awaitFeedbackGone("Data outlet lokal dimuat ulang")
        compose.onNodeWithTag("direction").performScrollTo().performClick()
        compose.waitUntil(10_000) { runCatching { compose.onNodeWithText("Petunjuk Arah Demo").assertIsDisplayed() }.isSuccess }
        captureNative("direction-dialog")
        compose.onNodeWithTag("direction_close").performClick()
    }
    @Test fun profileEditsQrAndLogout() {
        demo(); compose.onNodeWithTag("nav_profile").performClick(); capture("profile-qr")
        compose.onNodeWithTag("edit_profile").performClick()
        compose.onNodeWithTag("edit_name").performTextReplacement("Andi Wijaya")
        closeSoftKeyboard(); compose.onNodeWithTag("edit_save").performClick()
        awaitText("Andi Wijaya")
        compose.onNodeWithTag("profile_name").assertTextEquals("Andi Wijaya")
        awaitFeedbackGone("Data diri diperbarui")
        compose.onNodeWithTag("profile_fragrance").performScrollTo().performClick()
        compose.onNodeWithText("Ocean Breeze").performClick()
        compose.onNodeWithTag("profile_notifications").performScrollTo().assertIsOn().performClick()
        compose.onNodeWithTag("profile_notifications").assertIsOff()
        compose.onNodeWithTag("save_preferences").performScrollTo().performClick()
        awaitFeedbackGone("Perubahan profil disimpan"); capture("profile-preferences")
        compose.onNodeWithTag("open_qr").performScrollTo().performClick()
        compose.waitUntil(10_000) { runCatching { compose.onNodeWithTag("close_qr").assertIsDisplayed() }.isSuccess }
        captureNative("qr-dialog")
        compose.onNodeWithTag("close_qr").assertIsDisplayed().performClick()
        compose.onNodeWithTag("nav_home").performClick()
        compose.onNodeWithTag("home_greeting").assertTextContains("Andi", substring = true)
        compose.onNodeWithTag("nav_profile").performClick()
        compose.onNodeWithTag("profile_notifications").performScrollTo().assertIsOff()
        compose.onNodeWithTag("profile_fragrance").performScrollTo().assertTextContains("Ocean Breeze", substring = true)
        compose.onNodeWithTag("logout").performScrollTo().performClick()
        awaitLogin()
        compose.onNodeWithTag("demo_login").assertExists()
        compose.onNodeWithTag("nav_home").assertDoesNotExist()
    }
    @Test fun notificationsOpenTheirOwnOrderAndRetainReadState() {
        demo(); compose.onNodeWithTag("nav_notifications").performClick(); capture("notifications")
        compose.onNodeWithTag("notification_N-001").performClick()
        compose.onNodeWithTag("detail_id").assertTextEquals("#MW-1002")
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
        demo(); compose.onNodeWithTag("nav_orders").performClick()
        compose.onNodeWithTag("filter_COMPLETED").performClick()
        compose.activityRule.scenario.recreate(); compose.waitForIdle()
        compose.onNodeWithTag("order_MW-1003").assertExists()
        compose.onNodeWithTag("order_MW-1001").assertDoesNotExist()
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
        compose.onNodeWithTag("order_MW-1001").assertDoesNotExist()
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
        compose.onNodeWithTag("order_MW-1001").assertDoesNotExist()
        compose.onNodeWithText("Rp 17.500").assertExists()
    }
}
