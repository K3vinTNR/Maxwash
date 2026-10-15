package org.umn.maxwash.export

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import org.umn.maxwash.data.*
import org.umn.maxwash.ui.components.rupiah
import java.io.OutputStream
import kotlin.math.ceil
import kotlin.math.max

/** Vector PDF, written to the location selected through Android's document picker. */
object ReceiptPdf {
    fun write(receipt: DigitalReceipt, order: LaundryOrder, output: OutputStream) {
        require(receipt.isPaid) { "Nota hanya tersedia untuk pembayaran lunas." }
        require(receipt.hasValidTotal && receipt.total == order.total && receipt.orderId == order.id) {
            "Rincian nota tidak sesuai dengan transaksi."
        }
        val document = PdfDocument()
        try {
            val writer = ReceiptPages(document)
            writer.paragraph("MAXWASH", 24f, TEAL, bold = true)
            writer.paragraph("NOTA DIGITAL / E-RECEIPT", 11f, GRAY)
            writer.gap(12f)
            writer.paragraph(receipt.outletName, 17f, bold = true)
            writer.paragraph(receipt.outletAddress, 11f, GRAY)
            writer.rule()
            writer.pair("No. Nota", receipt.number)
            writer.pair("Status Pembayaran", "LUNAS")
            writer.pair("Waktu Transaksi", LocalTimeFormat.display(requireNotNull(receipt.paidAt), true).replace('·', '-'))
            writer.pair("No. Pesanan", receipt.orderId)
            writer.pair("Waktu Pesanan", LocalTimeFormat.display(receipt.orderedAt, true).replace('·', '-'))
            writer.gap(8f)
            writer.paragraph("PELANGGAN", 10f, GRAY, bold = true)
            writer.paragraph(receipt.customerName, 12f, bold = true)
            writer.paragraph(receipt.customerPhone, 11f)
            writer.paragraph(receipt.customerEmail, 11f)
            if (receipt.customerAddress.isNotBlank()) writer.paragraph(receipt.customerAddress, 11f, GRAY)
            writer.rule()
            writer.paragraph("RINCIAN PENIMBANGAN & CUCI", 11f, GRAY, bold = true)
            writer.gap(8f)
            writer.item(receipt.serviceName, rupiah(receipt.subtotal),
                "Berat estimasi ${receiptWeight(receipt.weightKg)} x ${rupiah(receipt.pricePerKg)} / kg")
            writer.paragraph("Aroma: ${receipt.fragrance.ifBlank { "Tanpa parfum" }}", 10f, TEAL)
            if (receipt.detergent > 0) writer.item("Deterjen Hypoallergenic", rupiah(receipt.detergent), "Tambahan perawatan cucian")
            if (receipt.discount > 0) writer.item("Diskon Promo", "-${rupiah(receipt.discount)}", receipt.promoCode)
            if (receipt.clothingNotes.isNotBlank()) {
                writer.gap(6f)
                writer.paragraph("Catatan pakaian", 10f, GRAY, bold = true)
                writer.paragraph(receipt.clothingNotes, 11f)
            }
            writer.rule()
            writer.pair("Metode Pembayaran", receipt.paymentLabel)
            writer.pair("Biaya Layanan POS", "GRATIS")
            writer.gap(8f)
            writer.pair("TOTAL AKHIR", rupiah(receipt.total), prominent = true)
            writer.rule()
            writer.paragraph("STATUS PENGAMBILAN: ${order.collectionTitle}", 11f, GREEN, bold = true)
            writer.paragraph(order.collectionMessage.replace('·', '-'), 11f, GREEN)
            writer.gap(10f)
            writer.paragraph("Terima kasih telah menggunakan MAXWASH.", 11f)
            writer.paragraph("Transaksi simulasi MAXWASH. Simpan nota ini bersama nomor pesanan Anda.", 9f, GRAY)
            writer.finish()
            document.writeTo(output)
        } finally { document.close() }
    }

    private const val TEAL = 0xFF056B93.toInt()
    private const val GRAY = 0xFF66717A.toInt()
    private const val GREEN = 0xFF10785E.toInt()
    private const val INK = 0xFF142635.toInt()

    private class ReceiptPages(private val document: PdfDocument) {
        private var pageNumber = 0
        private lateinit var page: PdfDocument.Page
        private val canvas: Canvas get() = page.canvas
        private var y = 44f
        private val left = 44f
        private val right = 551f

        init { start() }

        private fun start() {
            page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, ++pageNumber).create())
            canvas.drawColor(Color.WHITE)
            canvas.drawRect(0f, 0f, 595f, 7f, Paint().apply { color = TEAL })
            y = 44f
        }

        private fun ensure(height: Float) {
            if (y + height > 780f) { finish(); start() }
        }

        fun gap(height: Float) { ensure(height); y += height }

        fun paragraph(text: String, size: Float = 11f, color: Int = INK, bold: Boolean = false,
            x: Float = left, width: Float = right - left) {
            val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = size
                this.color = color
                typeface = Typeface.create("sans-serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
            }
            val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, width.toInt())
                .setAlignment(Layout.Alignment.ALIGN_NORMAL).setIncludePad(false).build()
            val lineHeight = ceil(paint.fontMetrics.descent - paint.fontMetrics.ascent + 2f)
            for (line in 0 until layout.lineCount) {
                ensure(lineHeight)
                canvas.drawText(text.substring(layout.getLineStart(line), layout.getLineEnd(line)).trimEnd('\n', '\r'),
                    x, y - paint.fontMetrics.ascent, paint)
                y += lineHeight
            }
        }

        fun rule() {
            ensure(26f)
            y += 12f
            canvas.drawLine(left, y, right, y, Paint().apply { color = 0xFFE0E5ED.toInt(); strokeWidth = 1f })
            y += 14f
        }

        fun pair(label: String, value: String, prominent: Boolean = false) {
            // Keep each pair together; variable-length text wraps instead of colliding with the amount.
            val size = if (prominent) 17f else 11f
            val valuePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = size }
            val layout = StaticLayout.Builder.obtain(value, 0, value.length, valuePaint, 247).build()
            ensure(max(26f, layout.height + 8f))
            val startY = y
            paragraph(label, size, if (prominent) TEAL else GRAY, bold = prominent, width = 245f)
            val labelBottom = y
            y = startY
            paragraph(value, size, if (prominent) TEAL else INK, bold = true, x = 304f, width = 247f)
            y = max(labelBottom, y) + 5f
        }

        fun item(title: String, amount: String, detail: String) {
            ensure(50f)
            val startY = y
            paragraph(title, 12f, bold = true, width = 355f)
            val titleBottom = y
            y = startY
            paragraph(amount, 12f, bold = true, x = 409f, width = 142f)
            y = max(titleBottom, y) + 3f
            paragraph(detail, 10f, GRAY)
            gap(7f)
        }

        fun finish() {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 8f; color = GRAY }
            canvas.drawText("MAXWASH - Transaksi simulasi", left, 817f, paint)
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("Halaman $pageNumber", right, 817f, paint)
            document.finishPage(page)
        }
    }
}
