package org.umn.maxwash.data

import kotlin.math.roundToInt

enum class CollectionMethod(val label: String) { PICKUP("Jemput ke Lokasi"), DROP_OFF("Antar Mandiri") }
enum class PaymentMethod(val label: String) {
    QRIS("QRIS Universal"), VIRTUAL_ACCOUNT("Transfer Virtual Account"), AT_OUTLET("Bayar di Outlet")
}

/** Checkout choices; an order is saved only after the prototype payment is submitted. */
data class CheckoutDraft(
    val address: String = "",
    val addressNotes: String = "",
    val outletId: String = "",
    val collection: CollectionMethod = CollectionMethod.PICKUP,
    val pickupDay: Int = 1,
    val pickupSlot: String = "09:00–11:00",
    val serviceId: String = "",
    val weightInput: String = "",
    val fragrance: String = "",
    val hypoallergenic: Boolean = false,
    val clothingNotes: String = "",
    val category: String = "Kiloan",
    val promoCode: String = "",
    val payment: PaymentMethod = PaymentMethod.QRIS
) {
    val weightKg: Double? get() = weightInput.replace(',', '.').toDoubleOrNull()
        ?.takeIf { it.isFinite() && it > 0 && it <= 100 }
    val locationReady get() = outletId.isNotBlank() && (collection == CollectionMethod.DROP_OFF || address.isNotBlank())

    /** Refresh profile defaults while keeping a location edited for this pickup. */
    fun syncProfileLocation(previousAddress: String, previousNotes: String,
        profileAddress: String, profileNotes: String) = copy(
        address = if (address.isBlank() || address == previousAddress) profileAddress else address,
        addressNotes = if (addressNotes == previousNotes) profileNotes else addressNotes
    )

    fun quote(service: LaundryService?): CheckoutQuote {
        val subtotal = if (service != null) ((weightKg ?: 0.0) * service.pricePerKg).roundToInt() else 0
        val detergent = if (hypoallergenic && subtotal > 0) 2000 else 0
        val discount = if (promoCode.trim().equals("NEWCLEAN5K", true)) minOf(5000, subtotal + detergent) else 0
        return CheckoutQuote(subtotal, detergent, discount)
    }
}

data class CheckoutQuote(val subtotal: Int, val detergent: Int, val discount: Int) {
    val total get() = subtotal + detergent - discount
}

val LaundryService.checkoutName: String get() = when (id) {
    "SVC-002" -> "Cuci Komplit Reguler"
    "SVC-003" -> "Cuci Kilat Express"
    "SVC-001" -> "Cuci Kering Lipat"
    "SVC-004" -> "Setrika Uap Premium"
    else -> name
}
