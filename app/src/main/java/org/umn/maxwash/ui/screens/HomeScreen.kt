package org.umn.maxwash.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import java.util.Locale
import org.umn.maxwash.ui.MaxwashViewModel
import org.umn.maxwash.ui.components.*
import org.umn.maxwash.ui.theme.*

@Composable
fun HomeScreen(vm: MaxwashViewModel, onOrder: (String) -> Unit, navigate: (String) -> Unit,
    onLocation: () -> Unit, feedback: (String) -> Unit) {
    val customer = vm.customer ?: return
    val active = vm.orders.firstOrNull { !it.isCompleted }
    val latest = vm.notifications.firstOrNull()
    val monthOrders = vm.monthOrders
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Halo, ${customer.name.substringBefore(" ")} 👋", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.testTag("home_greeting"))
                Pill("★ ${customer.membership} · ${customer.points} Poin")
            }
            FilledTonalIconButton({ navigate("profile") }, colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = Color.White)) {
                Icon(Icons.Outlined.QrCode2, "Lihat QR pelanggan", tint = WashTeal)
            }
        }
        if (active != null) WashCard {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("ORDER #${active.id}", style = MaterialTheme.typography.titleMedium)
                StatusPill(active.status)
            }
            Row(Modifier.fillMaxWidth().background(WashSoftBlue, MaterialTheme.shapes.medium).padding(12.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconTile(Icons.Outlined.Update, background = Color.White)
                Column(Modifier.weight(1f)) {
                    Text("Perkiraan Siap", style = MaterialTheme.typography.bodySmall, color = WashMuted)
                    Text(active.estimatedCollection, style = MaterialTheme.typography.labelMedium)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Layanan", style = MaterialTheme.typography.bodySmall, color = WashMuted)
                    Text(active.service, style = MaterialTheme.typography.labelMedium, color = WashTeal)
                    Text(weight(active.weightKg), style = MaterialTheme.typography.labelSmall, color = WashTeal)
                }
            }
            PrimaryButton("Lacak Pesanan →", { onOrder(active.id) }, Modifier.testTag("home_track"))
        } else WashCard {
            EmptyState("Belum ada pesanan aktif", "Catat pesanan Anda melalui menu Pesanan.", Modifier.testTag("home_orders_empty"))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Menu Utama", style = MaterialTheme.typography.titleSmall)
            TextButton({ navigate("orders") }) { Text("Semua Pesanan") }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf(Triple("Pesanan", Icons.Outlined.LocalLaundryService, { navigate("orders") }),
                Triple("Cek Tarif", Icons.Outlined.Calculate, { feedback(vm.services.joinToString(" · ") { "${it.name} ${rupiah(it.pricePerKg)}/kg" }.ifBlank { "Tarif belum tersedia." }) }),
                Triple("Nota Digital", Icons.AutoMirrored.Outlined.ReceiptLong, {
                    val receipt = vm.orders.firstOrNull { it.isCompleted } ?: vm.orders.firstOrNull()
                    if (receipt != null) onOrder(receipt.id) else feedback("Belum ada pesanan untuk ditampilkan.")
                }),
                Triple("Lokasi", Icons.Outlined.LocationOn, onLocation)).forEach { (label, icon, action) ->
                Column(Modifier.weight(1f).clickable(onClick = action).padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconTile(icon, Modifier.size(56.dp), Color.White)
                    Text(label, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        WashCard {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                listOf(Triple("Bulan Ini", monthOrders.size.toString(), "Pesanan"),
                    Triple("Total Cucian", String.format(Locale.US, "%.1f", vm.orders.sumOf { it.weightKg }), "Kilogram"),
                    Triple("Pesanan Aktif", vm.orders.count { !it.isCompleted }.toString(), "Berjalan")).forEach { (label, value, suffix) ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(label, style = MaterialTheme.typography.bodySmall, color = WashMuted)
                        Text(value, style = MaterialTheme.typography.headlineSmall, color = WashTeal)
                        Text(suffix, style = MaterialTheme.typography.bodySmall, color = WashMuted)
                    }
                }
            }
        }
        vm.promotions.forEach { promotion -> Row(Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(WashBlue, WashTeal)), MaterialTheme.shapes.large).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.LocalOffer, null, tint = Color.White)
            Column(Modifier.weight(1f)) {
                Text("PROMO SPESIAL", style = MaterialTheme.typography.labelSmall, color = Color.White)
                Text(promotion.title, style = MaterialTheme.typography.titleMedium, color = Color.White)
            }
            FilledTonalButton({ feedback(promotion.message) }, colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color.White, contentColor = WashTeal)) { Text("Info") }
        } }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Aktivitas Terakhir", style = MaterialTheme.typography.titleSmall)
            TextButton({ navigate("notifications") }) { Text("Lihat Semua") }
        }
        if (latest != null) WashCard(Modifier.clickable { vm.markRead(latest.id); onOrder(latest.orderId) }) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IconTile(Icons.Outlined.CheckCircle, tint = WashGreen)
                Column { Text(latest.title, style = MaterialTheme.typography.titleSmall); Text("#${latest.orderId} · ${latest.time}", style = MaterialTheme.typography.bodySmall, color = WashMuted) }
            }
        } else Text("Belum ada aktivitas laundry.", color = WashMuted)
    }
}
