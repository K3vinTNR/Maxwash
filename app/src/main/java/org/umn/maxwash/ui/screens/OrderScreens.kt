package org.umn.maxwash.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.umn.maxwash.data.*
import org.umn.maxwash.ui.MaxwashViewModel
import org.umn.maxwash.ui.components.*
import org.umn.maxwash.ui.theme.*

@Composable
fun OrdersScreen(vm: MaxwashViewModel, onOrder: (String) -> Unit, feedback: (String) -> Unit) {
    val orders = vm.visibleOrders
    LazyColumn(Modifier.fillMaxSize().testTag("orders_list"), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            OutlinedTextField(vm.search, { vm.search = it }, Modifier.fillMaxWidth().testTag("order_search"),
                placeholder = { Text("Cari no. pesanan / layanan…", style = MaterialTheme.typography.bodyMedium) },
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                trailingIcon = if (vm.search.isNotEmpty()) ({ IconButton({ vm.search = "" }) { Icon(Icons.Outlined.Close, "Hapus pencarian") } }) else null,
                singleLine = true, shape = MaterialTheme.shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color.Transparent,
                    unfocusedContainerColor = WashSoftBlue, focusedContainerColor = WashSoftBlue))
        }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OrderFilter.entries.forEach { filter ->
                    val count = MockRepository.filterOrders(filter, "").size
                    FilterChip(vm.filter == filter, { vm.filter = filter }, label = { Text("${filter.label} ($count)", style = MaterialTheme.typography.labelSmall) },
                        shape = CircleShape, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = WashTeal, selectedLabelColor = Color.White,
                            containerColor = WashSoftBlue), border = null, modifier = Modifier.testTag("filter_${filter.name}"))
                }
            }
        }
        item {
            WashCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconTile(Icons.Outlined.DryCleaning, background = Color(0xFFCCE9FF))
                    Column(Modifier.weight(1f)) {
                        SectionLabel("AKTIVITAS BULAN INI")
                        Text("${weight(MockRepository.orders.sumOf { it.weightKg })} · ${MockRepository.orders.size} pesanan", style = MaterialTheme.typography.titleMedium)
                    }
                    Icon(Icons.Outlined.AutoAwesome, null, tint = WashTeal)
                }
            }
        }
        if (orders.isEmpty()) item { EmptyState("Tidak ada pesanan", "Coba kata kunci lain atau ubah filter.", Modifier.testTag("orders_empty")) }
        items(orders, key = { it.id }) { order -> OrderCard(order, onOrder, feedback) }
        item { Text("Menampilkan ${orders.size} pesanan dari data simulasi", Modifier.padding(vertical = 20.dp), style = MaterialTheme.typography.bodySmall, color = WashMuted) }
    }
}

@Composable
private fun OrderCard(order: LaundryOrder, onOrder: (String) -> Unit, feedback: (String) -> Unit) {
    WashCard(Modifier.testTag("order_${order.id}").clickable { onOrder(order.id) }) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconTile(Icons.Outlined.LocalLaundryService, Modifier.size(30.dp))
            Column(Modifier.weight(1f)) {
                Text("#${order.id}", style = MaterialTheme.typography.titleSmall)
                Text(order.date, style = MaterialTheme.typography.labelSmall, color = WashMuted)
            }
            StatusPill(order.status)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(order.service, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            Text(rupiah(order.total), color = WashTeal, style = MaterialTheme.typography.titleMedium)
        }
        Text("Berat: ${weight(order.weightKg)} · ${if (order.service == "Express Laundry") "Express" else "Reguler"}", style = MaterialTheme.typography.bodySmall, color = WashMuted)
        Text(MockRepository.outlet(order.outletId).name, style = MaterialTheme.typography.bodySmall, color = WashMuted)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button({ onOrder(order.id) }, Modifier.weight(1f), shape = MaterialTheme.shapes.small) { Text(if (order.isCompleted) "Lihat Rincian" else "Lacak Pesanan", style = MaterialTheme.typography.labelMedium) }
            FilledTonalButton({ feedback("Nota demo ${order.id} · ${rupiah(order.total)} · ${order.service}") }, Modifier.weight(1f), shape = MaterialTheme.shapes.small,
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = WashSoftBlue, contentColor = WashNavy)) { Text("Nota Digital", style = MaterialTheme.typography.labelMedium) }
        }
    }
}

@Composable
fun OrderDetailsScreen(orderId: String?, onLocation: () -> Unit, feedback: (String) -> Unit) {
    val order = MockRepository.order(orderId)
    if (order == null) {
        EmptyState("Pesanan tidak ditemukan", "ID ${orderId ?: "kosong"} tidak tersedia. Silakan kembali ke daftar pesanan.", Modifier.testTag("order_missing")); return
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).testTag("order_details"), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Pill("• STATUS PESANAN")
            Text("Data simulasi", style = MaterialTheme.typography.labelSmall, color = WashMuted)
        }
        WashCard {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { SectionLabel("NOMOR PESANAN"); Pill(if (order.service == "Express Laundry") "EXPRESS 4 JAM" else "REGULER 24 JAM") }
            Text("#${order.id}", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.testTag("detail_id"))
            Row(Modifier.fillMaxWidth().background(WashSoftBlue, MaterialTheme.shapes.small).padding(12.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconTile(Icons.Outlined.DryCleaning, background = WashTeal, tint = Color.White)
                Column(Modifier.weight(1f)) {
                    Text(order.service, style = MaterialTheme.typography.titleMedium)
                    Text(weight(order.weightKg), style = MaterialTheme.typography.bodySmall, color = WashMuted)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Total Biaya", style = MaterialTheme.typography.labelSmall, color = WashMuted)
                    Text(rupiah(order.total), style = MaterialTheme.typography.titleMedium, color = WashTeal)
                }
            }
            Text("Diterima: ${order.date}", style = MaterialTheme.typography.bodySmall, color = WashMuted)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable(onClick = onLocation)) {
                Icon(Icons.Outlined.Storefront, null, Modifier.size(18.dp), tint = WashTeal)
                Text("  ${MockRepository.outlet(order.outletId).name}", style = MaterialTheme.typography.bodySmall)
            }
            Text(MockRepository.outlet(order.outletId).address, style = MaterialTheme.typography.bodySmall, color = WashMuted)
        }
        Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(WashBlue, WashTeal)), MaterialTheme.shapes.small).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Pill("TAHAP ${order.status.ordinal + 1} DARI 6", Color.White, Color(0x33002346))
                Text("• ${if (order.isCompleted) "Selesai" else "Berjalan"}", color = WashMint, style = MaterialTheme.typography.labelSmall)
            }
            Text(order.status.label, color = Color.White, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.testTag("detail_status"))
            Text(order.status.description, color = Color(0xFFD3EBF5), style = MaterialTheme.typography.bodySmall)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Progres Pengerjaan", color = Color.White, style = MaterialTheme.typography.labelMedium)
                Text("${(order.progress * 100).toInt()}%", color = Color.White, style = MaterialTheme.typography.labelMedium)
            }
            LinearProgressIndicator(progress = { order.progress }, modifier = Modifier.fillMaxWidth().height(7.dp), color = WashMint, trackColor = Color(0x66002949))
            HorizontalDivider(color = Color(0x3377CFFF))
            Text("Estimasi Pengambilan · ${order.estimatedCollection}", color = Color.White, style = MaterialTheme.typography.labelMedium)
        }
        WashCard {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Rincian Pelacakan", style = MaterialTheme.typography.titleMedium)
                Text("6 Tahapan", style = MaterialTheme.typography.labelSmall, color = WashTeal)
            }
            LaundryStatus.entries.forEach { stage ->
                val current = stage == order.status
                val done = stage.ordinal < order.status.ordinal
                val event = order.history.find { it.status == stage }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(26.dp).background(if (done || current) WashTeal else WashLavender, CircleShape), contentAlignment = Alignment.Center) {
                            if (done) Icon(Icons.Outlined.Check, null, Modifier.size(17.dp), tint = Color.White)
                            else Box(Modifier.size(if (current) 12.dp else 6.dp).background(if (current) WashMint else Color(0xFFBDC6D6), CircleShape))
                        }
                        if (stage != LaundryStatus.COLLECTED) Box(Modifier.width(2.dp).height(42.dp).background(if (done) WashTeal else WashBorder))
                    }
                    Column(Modifier.weight(1f).background(if (current) WashSoftBlue else Color.Transparent, MaterialTheme.shapes.small).padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stage.label, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, color = if (current) WashTeal else if (done) WashNavy else WashMuted)
                            if (current) Pill(if (order.isCompleted) "SELESAI" else "AKTIF") else if (done) Text("Selesai", style = MaterialTheme.typography.labelSmall, color = WashGreen)
                        }
                        Text(event?.time ?: "Menunggu tahapan sebelumnya", style = MaterialTheme.typography.bodySmall, color = WashMuted)
                    }
                }
            }
        }
        FilledTonalButton({ feedback("Menghubungi kasir adalah fitur demo. Tidak ada pesan yang dikirim.") }, Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.small) {
            Icon(Icons.Outlined.ChatBubbleOutline, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Hubungi Kasir Outlet")
        }
    }
}
