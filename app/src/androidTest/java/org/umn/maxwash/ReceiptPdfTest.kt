package org.umn.maxwash

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.umn.maxwash.data.*
import org.umn.maxwash.export.ReceiptPdf
import java.io.ByteArrayOutputStream
import java.io.File

@RunWith(AndroidJUnit4::class)
class ReceiptPdfTest {
    private val timestamp = java.time.Instant.parse("2026-10-15T03:20:00Z").toEpochMilli()
    private fun receipt() = DigitalReceipt("MW-TEST12345678", "NT-TEST12345678", timestamp, timestamp,
        "MAXWASH Senopati", "Jl. Senopati No. 42, Kebayoran Baru, Jakarta Selatan",
        "Sari Putri", "081234567890", "sari@example.com", "Jl. Senopati No. 42B, Jakarta Selatan",
        "Cuci Kilat Express", 4.8, 16000, "Ocean Breeze", "Pisahkan pakaian putih",
        76800, 2000, 5000, "NEWCLEAN5K", PaymentMethod.QRIS.name, 73800)

    private fun order() = LaundryOrder("MW-TEST12345678", "Express Laundry", 4.8, "", "OUT-002",
        LaundryStatus.RECEIVED, 73800, "", listOf(StatusEvent(LaundryStatus.RECEIVED, "15 Okt 2026")), timestamp, 4)

    @Test fun paidPdfRendersAtA4AndLongContentContinuesOnAdditionalPages() {
        val dir = InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir("receipt-verification")!!
        dir.mkdirs()
        listOf("receipt-sample" to receipt(), "receipt-long" to receipt().copy(
            customerName = "Nama Pelanggan Panjang ".repeat(10),
            customerAddress = "Alamat penjemputan lengkap dengan patokan rumah. ".repeat(15),
            clothingNotes = "Pisahkan pakaian putih dan gunakan perawatan lembut. ".repeat(80)
        )).forEach { (name, data) ->
            val file = File(dir, "$name.pdf")
            file.outputStream().use { ReceiptPdf.write(data, order(), it) }
            assertTrue(file.length() > 1000)
            PdfRenderer(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)).use { renderer ->
                if (name == "receipt-long") assertTrue(renderer.pageCount > 1)
                else assertEquals(1, renderer.pageCount)
                for (index in 0 until renderer.pageCount) renderer.openPage(index).use { page ->
                    assertEquals(595, page.width)
                    assertEquals(842, page.height)
                    val bitmap = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                    try {
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        File(dir, "$name-page-${index + 1}.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    } finally { bitmap.recycle() }
                }
            }
        }
    }

    @Test fun unpaidMismatchedAndUnbalancedReceiptsCannotBeExported() {
        listOf(receipt().copy(paidAt = null), receipt().copy(orderId = "OTHER"),
            receipt().copy(total = 1), receipt().copy(discount = -1)).forEach { invalid ->
            val output = ByteArrayOutputStream()
            try { ReceiptPdf.write(invalid, order(), output); fail("Expected invalid receipt") }
            catch (_: IllegalArgumentException) { assertEquals(0, output.size()) }
        }
    }
}
