package org.umn.maxwash.data

import java.math.BigDecimal

/** Transaction snapshot: profile and catalog edits must not rewrite an issued receipt. */
data class DigitalReceipt(
    val orderId: String,
    val number: String,
    val orderedAt: Long,
    val paidAt: Long?,
    val outletName: String,
    val outletAddress: String,
    val customerName: String,
    val customerPhone: String,
    val customerEmail: String,
    val customerAddress: String,
    val serviceName: String,
    val weightKg: Double,
    val pricePerKg: Int,
    val fragrance: String,
    val clothingNotes: String,
    val subtotal: Int,
    val detergent: Int,
    val discount: Int,
    val promoCode: String,
    val paymentMethod: String,
    val total: Int
) {
    val isPaid get() = paidAt != null
    val paymentLabel get() = PaymentMethod.entries.find { it.name == paymentMethod }?.label ?: "Tidak tercatat"
    val hasValidTotal get() = subtotal >= 0 && detergent >= 0 && discount >= 0 &&
        total >= 0 && subtotal.toLong() + detergent - discount == total.toLong()
}

fun receiptWeight(value: Double): String = BigDecimal.valueOf(value).stripTrailingZeros()
    .toPlainString().replace('.', ',') + " kg"

val LaundryOrder.collectionTitle: String get() = if (isCompleted) "Sudah Diambil" else "Belum Diambil"

val LaundryOrder.collectionMessage: String get() = when {
    isCompleted -> history.find { it.status == LaundryStatus.COLLECTED }?.let { "Diambil pada ${it.time}." }
        ?: "Cucian telah diserahkan kepada pelanggan."
    status == LaundryStatus.READY -> "Cucian sudah siap diambil di outlet."
    else -> "Cucian belum diambil. Status saat ini: ${status.label}."
}
