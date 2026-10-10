package org.umn.maxwash.data

enum class LaundryStatus(val label: String, val description: String) {
    RECEIVED("Pesanan Diterima", "Cucian diterima dan berat telah diperiksa oleh outlet."),
    PROCESSING("Sedang Diproses", "Cucian Anda sedang dicuci, dikeringkan, dan dirapikan."),
    FINISHED("Sudah Selesai", "Proses pencucian dan perapian sudah selesai."),
    PACKED("Sudah Dipacking", "Cucian dikemas dengan rapi dan higienis."),
    READY("Siap Diambil", "Silakan ambil cucian Anda di outlet MAXWASH."),
    COLLECTED("Sudah Diambil", "Cucian telah diserahkan kepada pelanggan. Terima kasih!")
}

data class Customer(
    val id: String, val name: String, val phone: String, val email: String,
    val address: String = "", val fragrance: String = "",
    val notes: String = "",
    val notificationEnabled: Boolean = true, val outletId: String = "",
    val membership: String = "Member", val points: Int = 0
)
data class Outlet(val id: String, val name: String, val address: String, val distanceKm: Double,
    val openingHours: String)
data class StatusEvent(val status: LaundryStatus, val time: String)
data class LaundryOrder(
    val id: String, val service: String, val weightKg: Double, val date: String,
    val outletId: String, val status: LaundryStatus, val total: Int,
    val estimatedCollection: String, val history: List<StatusEvent>,
    val createdAt: Long, val turnaroundHours: Int
) {
    val isCompleted get() = status == LaundryStatus.COLLECTED
    val progress get() = (status.ordinal + 1) / LaundryStatus.entries.size.toFloat()
}
data class LaundryNotification(val id: String, val orderId: String, val title: String,
    val message: String, val time: String, val isRead: Boolean, val createdAt: Long)

data class LaundryService(val id: String, val name: String, val pricePerKg: Int, val turnaroundHours: Int)
data class Promotion(val id: String, val title: String, val message: String)

data class LocalData(
    val customer: Customer? = null,
    val orders: List<LaundryOrder> = emptyList(),
    val notifications: List<LaundryNotification> = emptyList(),
    val outlets: List<Outlet> = emptyList(),
    val services: List<LaundryService> = emptyList(),
    val fragrances: List<String> = emptyList(),
    val promotions: List<Promotion> = emptyList(),
    val selectedOutletId: String = ""
)

fun List<LaundryOrder>.filterOrders(filter: OrderFilter, query: String) = filter { order ->
    (filter == OrderFilter.ALL || (filter == OrderFilter.COMPLETED) == order.isCompleted) &&
        (query.isBlank() || order.id.contains(query.trim(), true) || order.service.contains(query.trim(), true))
}
enum class OrderFilter(val label: String) { ALL("Semua"), ACTIVE("Sedang Berjalan"), COMPLETED("Selesai") }
