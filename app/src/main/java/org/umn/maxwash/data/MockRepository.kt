package org.umn.maxwash.data

object MockRepository {
    val customer = Customer("CUST-001", "Andi Pratama", "081234567890", "andi@example.com",
        "Jl. Bintaro Utama Sektor 7 No. 42, Tangerang Selatan")
    const val demoPassword = "Maxwash123"
    val outlets = listOf(
        Outlet("OUT-001", "MAXWASH Bintaro Sektor 7", "Jl. Kasuari No. 14, Bintaro, Tangerang Selatan", 1.2),
        Outlet("OUT-002", "MAXWASH Senopati", "Jl. Senopati No. 42, Kebayoran Baru, Jakarta Selatan", 2.5)
    )
    private fun makeOrder(id: String, service: String, weight: Double, date: String,
        status: LaundryStatus, total: Int, outletId: String = "OUT-001") = LaundryOrder(
        id, service, weight, date, outletId, status, total,
        if (status == LaundryStatus.COLLECTED) "Sudah diambil · 7 Okt, 17:30 WIB" else "10 Okt, 17:30 WIB",
        LaundryStatus.entries.take(status.ordinal + 1).mapIndexed { index, stage ->
            StatusEvent(stage, "${date.substringBefore(" · ")} · ${listOf("09:15", "10:45", "14:00", "15:00", "17:00", "17:30")[index]} WIB")
        }
    )
    val orders = listOf(
        makeOrder("MW-1001", "Wash & Fold", 3.0, "10 Okt 2026 · 09:15 WIB", LaundryStatus.PROCESSING, 21000),
        makeOrder("MW-1002", "Wash & Iron", 5.0, "9 Okt 2026 · 09:15 WIB", LaundryStatus.READY, 50000),
        makeOrder("MW-1003", "Express Laundry", 2.0, "7 Okt 2026 · 09:15 WIB", LaundryStatus.COLLECTED, 32000, "OUT-002"),
        makeOrder("MW-1004", "Wash & Fold", 4.0, "9 Okt 2026 · 09:15 WIB", LaundryStatus.PACKED, 28000),
        makeOrder("MW-1005", "Iron Only", 3.0, "10 Okt 2026 · 09:15 WIB", LaundryStatus.RECEIVED, 18000, "OUT-002"),
        makeOrder("MW-1006", "Wash & Iron", 6.0, "9 Okt 2026 · 09:15 WIB", LaundryStatus.FINISHED, 60000)
    )
    val notifications = listOf(
        LaundryNotification("N-001", "MW-1002", "Cucian Siap Diambil!", "Laundry MW-1002 siap diambil. Tunjukkan QR pelanggan Anda kepada kasir outlet.", "10 Okt · 17:00 WIB"),
        LaundryNotification("N-002", "MW-1001", "Proses Pencucian", "Laundry MW-1001 sedang diproses di MAXWASH Bintaro Sektor 7.", "10 Okt · 10:45 WIB"),
        LaundryNotification("N-003", "MW-1004", "Cucian Selesai Dipacking", "Laundry MW-1004 telah dikemas dengan rapi dan higienis.", "9 Okt · 15:00 WIB", true),
        LaundryNotification("N-004", "MW-1005", "Pesanan Diterima di Outlet", "Pesanan MW-1005 dengan layanan Iron Only telah diterima.", "10 Okt · 09:15 WIB", true),
        LaundryNotification("N-005", "MW-1006", "Proses Laundry Selesai", "Laundry MW-1006 selesai dicuci dan dirapikan. Selanjutnya proses packing.", "9 Okt · 14:00 WIB", true)
    )
    fun order(id: String?) = orders.find { it.id == id }
    fun outlet(id: String) = outlets.first { it.id == id }
    fun filterOrders(filter: OrderFilter, query: String) = orders.filter {
        (filter == OrderFilter.ALL || (filter == OrderFilter.COMPLETED) == it.isCompleted) &&
            (query.isBlank() || it.id.contains(query.trim(), true) || it.service.contains(query.trim(), true))
    }
}
