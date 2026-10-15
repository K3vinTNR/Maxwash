package org.umn.maxwash.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import org.umn.maxwash.data.*
import org.umn.maxwash.ui.components.*
import org.umn.maxwash.ui.theme.*

@Composable
fun PickupScreen(draft: CheckoutDraft, outlets: List<Outlet>, onChange: (CheckoutDraft) -> Unit,
    onContinue: () -> Unit, feedback: (String) -> Unit) {
    val context = LocalContext.current
    var editingAddress by rememberSaveable { mutableStateOf(false) }
    var editingSchedule by rememberSaveable { mutableStateOf(false) }
    var outletMenu by remember { mutableStateOf(false) }
    val outlet = outlets.find { it.id == draft.outletId }
    LaunchedEffect(draft.collection) {
        if (draft.collection != CollectionMethod.PICKUP) {
            onChange(draft.copy(collection = CollectionMethod.PICKUP))
        }
    }
    fun openMap(address: String) {
        if (address.isBlank()) { feedback("Tambahkan alamat terlebih dahulu."); return }
        try { context.startActivity(Intent(Intent.ACTION_VIEW, "https://www.google.com/maps/search/?api=1&query=${Uri.encode(address)}".toUri())) }
        catch (_: ActivityNotFoundException) { feedback("Aplikasi peta atau browser belum tersedia.") }
    }
    Column(Modifier.fillMaxSize().testTag("pickup_screen")) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Spacer(Modifier.height(12.dp))
            PickupMap(draft.address, outlet?.address.orEmpty(), outlet?.name ?: "Pilih Outlet", true, ::openMap, feedback)
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                .background(WashSoftBlue).padding(horizontal = 16.dp, vertical = 14.dp)
                .testTag("collection_PICKUP"), horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.DeliveryDining, null, Modifier.size(20.dp), tint = WashTeal)
                Spacer(Modifier.width(8.dp))
                Text(CollectionMethod.PICKUP.label, style = MaterialTheme.typography.labelLarge, color = WashTeal)
            }
            Row(Modifier.fillMaxWidth().background(Color(0xFFB8F8E0), RoundedCornerShape(22.dp)).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.Verified, null, Modifier.size(20.dp), tint = WashGreen)
                Text("Pilih alamat dan jadwal penjemputan Anda",
                    style = MaterialTheme.typography.labelMedium, color = WashGreen)
            }
            CheckoutCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Pill("⌂ Rumah (Alamat Penjemputan)")
                    Spacer(Modifier.weight(1f))
                    TextButton({ editingAddress = true }, Modifier.testTag("pickup_edit_address")) { Text("Ubah ›") }
                }
                Text(draft.address.ifBlank { "Tambahkan alamat penjemputan" }, style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.testTag("pickup_address"))
                Row(Modifier.fillMaxWidth().background(WashSoftBlue, RoundedCornerShape(10.dp)).padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.EditNote, null, Modifier.size(20.dp), tint = WashMuted)
                    Column {
                        Text("Catatan Patokan Driver:", style = MaterialTheme.typography.labelMedium)
                        Text(draft.addressNotes.ifBlank { "Tambahkan patokan agar alamat mudah ditemukan." }, style = MaterialTheme.typography.bodySmall, color = WashMuted)
                    }
                }
            }
            CheckoutCard(Modifier.clickable { editingSchedule = true }.testTag("pickup_schedule")) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CheckoutIcon(Icons.Outlined.Schedule, background = Color(0xFFD0E9FF))
                    Column(Modifier.weight(1f)) {
                        Text("Jadwal Penjemputan", style = MaterialTheme.typography.labelMedium, color = WashMuted)
                        Text("${if (draft.pickupDay == 1) "Besok" else "Lusa"}, ${draft.pickupSlot} WIB", style = MaterialTheme.typography.titleSmall)
                    }
                    Icon(Icons.Outlined.EditCalendar, "Ubah jadwal", tint = WashMuted)
                }
            }
            Box {
                CheckoutCard(Modifier.clickable { outletMenu = true }.testTag("pickup_outlet")) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.size(8.dp).background(WashGreen, CircleShape))
                        Column(Modifier.weight(1f)) {
                            Text(outlet?.name ?: "Pilih Outlet", style = MaterialTheme.typography.titleSmall)
                            Text(outlet?.address ?: "Belum ada outlet yang dipilih", style = MaterialTheme.typography.bodySmall, color = WashMuted)
                        }
                        Icon(Icons.Outlined.ExpandMore, "Pilih outlet", tint = WashTeal)
                    }
                    outlet?.let { Pill("Jam operasional · ${it.openingHours}", WashGreen, Color(0xFFB8F8E0)) }
                }
                DropdownMenu(outletMenu, { outletMenu = false }) {
                    outlets.forEach { item -> DropdownMenuItem(text = { Text(item.name) },
                        onClick = { onChange(draft.copy(outletId = item.id)); outletMenu = false }, modifier = Modifier.testTag("pickup_outlet_${item.id}")) }
                }
            }
        }
        CheckoutFooter {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Alamat & jadwal pilihan", style = MaterialTheme.typography.labelSmall, color = WashMuted)
                Text("Jemput ke Lokasi", style = MaterialTheme.typography.labelSmall, color = WashGreen)
            }
            CheckoutAction("Konfirmasi Lokasi Penjemputan  →",
                onContinue, "pickup_continue", Modifier.fillMaxWidth(), draft.address.isNotBlank() && outlet != null)
        }
    }
    if (editingAddress) PickupAddressDialog(draft, { editingAddress = false }) {
        onChange(it); editingAddress = false
    }
    if (editingSchedule) AlertDialog(onDismissRequest = { editingSchedule = false }, title = { Text("Jadwal Penjemputan") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(1 to "Besok", 2 to "Lusa").forEach { (day, label) ->
                listOf("09:00–11:00", "13:00–14:00", "16:00–18:00").forEach { slot ->
                    TextButton({ onChange(draft.copy(pickupDay = day, pickupSlot = slot)); editingSchedule = false }, Modifier.fillMaxWidth()) {
                        Text("$label, $slot WIB")
                    }
                }
            }
        } }, confirmButton = { TextButton({ editingSchedule = false }) { Text("Tutup") } })
}

@Composable
private fun PickupAddressDialog(draft: CheckoutDraft, onDismiss: () -> Unit, onSave: (CheckoutDraft) -> Unit) {
    var address by rememberSaveable { mutableStateOf(draft.address) }
    var notes by rememberSaveable { mutableStateOf(draft.addressNotes) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Alamat Penjemputan") },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            WashField(address, { address = it }, "Alamat Lengkap", "pickup_address_input", multiline = true)
            WashField(notes, { notes = it }, "Patokan / Catatan Driver", "pickup_notes_input", multiline = true)
        } }, confirmButton = { TextButton({ onSave(draft.copy(address = address.trim(), addressNotes = notes.trim())) },
            Modifier.testTag("pickup_address_save"), enabled = address.isNotBlank()) { Text("Simpan") } },
        dismissButton = { TextButton(onDismiss) { Text("Batal") } })
}

