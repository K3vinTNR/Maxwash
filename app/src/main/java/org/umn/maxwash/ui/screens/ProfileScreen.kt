package org.umn.maxwash.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.umn.maxwash.data.*
import org.umn.maxwash.ui.MaxwashViewModel
import org.umn.maxwash.ui.components.*
import org.umn.maxwash.ui.theme.*

@Composable
fun ProfileScreen(vm: MaxwashViewModel, onLogout: () -> Unit, onLocation: () -> Unit, feedback: (String) -> Unit) {
    val customer = vm.customer
    var editing by rememberSaveable { mutableStateOf(false) }
    var qrDialog by rememberSaveable { mutableStateOf(false) }
    var fragrance by rememberSaveable { mutableStateOf(customer.fragrance) }
    var notes by rememberSaveable { mutableStateOf(customer.notes) }
    var notifications by rememberSaveable { mutableStateOf(customer.notificationEnabled) }
    var fragranceMenu by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(84.dp).background(WashLavender, CircleShape).padding(5.dp).background(Color(0xFFC8E9EF), CircleShape), contentAlignment = Alignment.Center) {
                Text(customer.name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1).uppercase() }, style = MaterialTheme.typography.headlineLarge, color = WashTeal)
            }
            Text(customer.name, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.testTag("profile_name"))
            Text(customer.phone, color = WashMuted)
            Pill("★ Member Gold · 140 Poin", WashTeal, WashLavender)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("DATA DIRI")
            TextButton({ editing = true }, Modifier.testTag("edit_profile")) { Icon(Icons.Outlined.Edit, null, Modifier.size(16.dp)); Text(" Edit") }
        }
        WashCard {
            ProfileRow(Icons.Outlined.Badge, "Nama Lengkap", customer.name)
            HorizontalDivider(color = WashBorder)
            ProfileRow(Icons.Outlined.ChatBubbleOutline, "Nomor Handphone", customer.phone)
            HorizontalDivider(color = WashBorder)
            ProfileRow(Icons.Outlined.Email, "Email", customer.email)
            HorizontalDivider(color = WashBorder)
            ProfileRow(Icons.Outlined.LocationOn, "Alamat", customer.address)
        }
        WashCard(Modifier.clickable { qrDialog = true }) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MockQr(customer.id, Modifier.size(96.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("QR Pelanggan", style = MaterialTheme.typography.titleMedium)
                    Text(customer.id, style = MaterialTheme.typography.labelLarge, color = WashTeal)
                    Text("Tunjukkan identitas Anda di outlet.", style = MaterialTheme.typography.bodySmall, color = WashMuted)
                    TextButton({ qrDialog = true }, Modifier.testTag("open_qr")) { Text("Perbesar QR") }
                }
            }
        }
        SectionLabel("PREFERENSI LAYANAN")
        WashCard {
            Text("Aroma Parfum Favorit", style = MaterialTheme.typography.labelMedium)
            Box {
                Row(Modifier.fillMaxWidth().background(WashSoftBlue, MaterialTheme.shapes.small)
                    .clickable { fragranceMenu = true }.padding(14.dp).testTag("profile_fragrance"), verticalAlignment = Alignment.CenterVertically) {
                    Text(fragrance, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Icon(Icons.Outlined.ExpandMore, "Pilih aroma parfum", tint = WashMuted)
                }
                DropdownMenu(fragranceMenu, { fragranceMenu = false }) {
                    listOf("Lavender Fresh", "Ocean Breeze", "Baby Soft").forEach { value ->
                        DropdownMenuItem(text = { Text(value) }, onClick = { fragrance = value; fragranceMenu = false })
                    }
                }
            }
            WashField(notes, { notes = it }, "Catatan Khusus Tetap", "profile_notes", multiline = true)
            Text("Otomatis disertakan pada tampilan profil", style = MaterialTheme.typography.labelSmall, color = WashMuted)
            HorizontalDivider(color = WashBorder)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconTile(Icons.Outlined.NotificationsNone)
                Column(Modifier.weight(1f).padding(start = 10.dp)) {
                    Text("Update Status Laundry", style = MaterialTheme.typography.titleSmall)
                    Text("Preferensi notifikasi demo", style = MaterialTheme.typography.bodySmall, color = WashMuted)
                }
                Switch(notifications, { notifications = it }, Modifier.testTag("profile_notifications"))
            }
        }
        SectionLabel("OUTLET LANGGANAN")
        WashCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconTile(Icons.Outlined.Storefront)
                Column(Modifier.weight(1f)) {
                    Text(MockRepository.outlet(customer.outletId).name, style = MaterialTheme.typography.titleSmall)
                    Text("• Buka hingga 21:00", color = WashGreen, style = MaterialTheme.typography.bodySmall)
                }
                TextButton({
                    val next = if (customer.outletId == "OUT-001") "OUT-002" else "OUT-001"
                    vm.updateProfile(customer.copy(outletId = next)); vm.selectedOutletId = next
                    feedback("Outlet langganan diubah")
                }, Modifier.testTag("change_profile_outlet")) { Text("Ganti") }
            }
            TextButton({ vm.selectedOutletId = customer.outletId; onLocation() }) { Text("Lihat lokasi outlet →") }
        }
        PrimaryButton("Simpan Perubahan", {
            if (vm.updateProfile(customer.copy(fragrance = fragrance, notes = notes, notificationEnabled = notifications))) feedback("Perubahan profil disimpan")
        }, Modifier.testTag("save_preferences"))
        TextButton(onLogout, Modifier.align(Alignment.CenterHorizontally).testTag("logout"), colors = ButtonDefaults.textButtonColors(contentColor = WashError)) {
            Icon(Icons.AutoMirrored.Outlined.Logout, null, Modifier.size(18.dp)); Text(" Keluar dari Akun")
        }
        Text("MAXWASH CUSTOMER · UTS PROTOTYPE", Modifier.fillMaxWidth(), style = MaterialTheme.typography.labelSmall, color = WashMuted, textAlign = TextAlign.Center)
    }
    if (editing) EditProfileDialog(customer, { editing = false }) {
        if (vm.updateProfile(it)) { editing = false; feedback("Data diri diperbarui") }
    }
    if (qrDialog) AlertDialog(onDismissRequest = { qrDialog = false }, title = { Text("QR Pelanggan") },
        text = { Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            MockQr(customer.id, Modifier.size(220.dp)); Text(customer.name); Text(customer.id, color = WashTeal)
            Text("Identitas visual untuk demo; belum digunakan untuk pemindaian.", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        } }, confirmButton = { TextButton({ qrDialog = false }, Modifier.testTag("close_qr")) { Text("Tutup") } })
}

@Composable
private fun ProfileRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        IconTile(icon)
        Column(Modifier.weight(1f)) { Text(label, style = MaterialTheme.typography.labelSmall, color = WashMuted); Text(value, style = MaterialTheme.typography.bodyMedium) }
    }
}

@Composable
private fun EditProfileDialog(customer: Customer, onDismiss: () -> Unit, onSave: (Customer) -> Unit) {
    var name by rememberSaveable { mutableStateOf(customer.name) }
    var phone by rememberSaveable { mutableStateOf(customer.phone) }
    var email by rememberSaveable { mutableStateOf(customer.email) }
    var address by rememberSaveable { mutableStateOf(customer.address) }
    val valid = FormValidation.name(name) == null && FormValidation.phone(phone) == null && FormValidation.email(email) == null && address.isNotBlank()
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Edit Data Diri") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                WashField(name, { name = it }, "Nama Lengkap", "edit_name", error = FormValidation.name(name))
                WashField(phone, { phone = it.filter { c -> c in '0'..'9' } }, "Nomor Handphone", "edit_phone", KeyboardType.Number, FormValidation.phone(phone))
                WashField(email, { email = it }, "Email", "edit_email", KeyboardType.Email, FormValidation.email(email))
                WashField(address, { address = it }, "Alamat", "edit_address", error = if (address.isBlank()) "Alamat wajib diisi" else null, multiline = true)
            }
        }, confirmButton = { TextButton({ onSave(customer.copy(name = name, phone = phone, email = email, address = address)) }, enabled = valid, modifier = Modifier.testTag("edit_save")) { Text("Simpan") } },
        dismissButton = { TextButton(onDismiss) { Text("Batal") } })
}
