package org.umn.maxwash

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class MaxwashFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun awaitLogin() = compose.waitUntil(10_000) { compose.onAllNodesWithTag("demo_login").fetchSemanticsNodes().isNotEmpty() }
    private fun demo() { awaitLogin(); compose.onNodeWithTag("demo_login").performClick(); compose.onNodeWithTag("home_greeting").assertIsDisplayed() }
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
        compose.onNodeWithTag("location_outlet").assertTextEquals("MAXWASH Senopati")
        compose.onNodeWithTag("refresh_location").performScrollTo().performClick()
        compose.onNodeWithTag("location_distance").assertTextContains("2.6 km", substring = true)
        awaitFeedbackGone("Lokasi simulasi diperbarui")
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
        compose.onNodeWithTag("demo_login").assertExists()
        compose.onNodeWithTag("nav_home").assertDoesNotExist()
    }
    @Test fun notificationsOpenTheirOwnOrderAndRetainReadState() {
        demo(); compose.onNodeWithTag("nav_notifications").performClick(); capture("notifications")
        compose.onNodeWithTag("notification_N-001").performClick()
        compose.onNodeWithTag("detail_id").assertTextEquals("#MW-1002")
        compose.onNodeWithTag("back").performClick()
        compose.onNodeWithText("1 Baru").assertExists()
        compose.onNodeWithTag("mark_all_read").performClick()
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
}
