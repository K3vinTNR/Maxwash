package org.umn.maxwash.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import org.umn.maxwash.data.*
import org.umn.maxwash.ui.MaxwashViewModel
import org.umn.maxwash.ui.components.*
import org.umn.maxwash.ui.theme.*

@Composable
fun ProfileScreen(vm: MaxwashViewModel, onLogout: () -> Unit, onLocation: () -> Unit, feedback: (String) -> Unit) {
    val customer = vm.customer ?: return
    val scope = rememberCoroutineScope()
    var saving by remember { mutableStateOf(false) }
    var editing by rememberSaveable { mutableStateOf(false) }
    var fragrance by rememberSaveable(customer.id) { mutableStateOf(customer.fragrance) }
    var notes by rememberSaveable(customer.id) { mutableStateOf(customer.notes) }
    var notifications by rememberSaveable(customer.id) { mutableStateOf(customer.notificationEnabled) }
    var fragranceMenu by remember { mutableStateOf(false) }
    val outlet = vm.outlet(customer.outletId)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        ProfileIdentityCard(customer)
        ProfileCard {
            ProfileSectionHeader(Icons.Outlined.Badge, "Data Diri", "Informasi akun dan alamat Anda") {
                FilledTonalButton({ editing = true }, Modifier.testTag("edit_profile"), enabled = !saving,
                    shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(horizontal = 12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = WashSoftBlue, contentColor = WashTeal)) {
                    Icon(Icons.Outlined.Edit, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Edit", style = MaterialTheme.typography.labelMedium)
                }
            }
            ProfileRow(Icons.Outlined.Badge, "Nama Lengkap", customer.name)
            ProfileDivider()
            ProfileRow(Icons.Outlined.ChatBubbleOutline, "Nomor Handphone", customer.phone)
            ProfileDivider()
            ProfileRow(Icons.Outlined.Email, "Email", customer.email)
            ProfileDivider()
            ProfileRow(Icons.Outlined.LocationOn, "Alamat", customer.address.ifBlank { "Belum diisi" })
        }
        ProfileCard {
            ProfileSectionHeader(Icons.Outlined.Tune, "Preferensi Layanan", "Sesuaikan perawatan cucian Anda")
            Text("Aroma Parfum Favorit", style = MaterialTheme.typography.labelMedium, color = WashMuted)
            Box(Modifier.fillMaxWidth()) {
                Surface(color = WashBackground, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, WashBorder)) {
                    Row(Modifier.fillMaxWidth().clickable(enabled = !saving) { fragranceMenu = true }
                        .padding(14.dp).testTag("profile_fragrance"), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Outlined.Spa, null, Modifier.size(20.dp), tint = WashTeal)
                        Text(fragrance.ifBlank { "Pilih aroma parfum" }, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Icon(Icons.Outlined.ExpandMore, "Pilih aroma parfum", tint = WashMuted)
                    }
                }
                DropdownMenu(fragranceMenu, { fragranceMenu = false }) {
                    vm.fragrances.forEach { value ->
                        DropdownMenuItem(text = { Text(value) }, onClick = { fragrance = value; fragranceMenu = false })
                    }
                }
            }
            WashField(notes, { notes = it }, "Catatan Khusus Tetap", "profile_notes", multiline = true, enabled = !saving)
            Text("Tambahkan catatan agar layanan sesuai kebutuhan.", style = MaterialTheme.typography.bodySmall, color = WashMuted)
            Surface(color = Color(0xFFF5F8FB), shape = RoundedCornerShape(14.dp)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ProfileIcon(Icons.Outlined.NotificationsNone, background = Color.White)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("Update Status Laundry", style = MaterialTheme.typography.titleSmall)
                        Text("Preferensi pembaruan pesanan", style = MaterialTheme.typography.bodySmall, color = WashMuted)
                    }
                    Switch(notifications, { notifications = it }, Modifier.testTag("profile_notifications"), enabled = !saving)
                }
            }
        }
        ProfileCard {
            ProfileSectionHeader(Icons.Outlined.Storefront, "Outlet Langganan", "Outlet pilihan untuk cucian Anda") {
                TextButton({
                    val next = vm.outlets.getOrNull((vm.outlets.indexOfFirst { it.id == customer.outletId } + 1) % vm.outlets.size)
                    if (next != null) {
                        saving = true
                        scope.launch(Dispatchers.Main.immediate) {
                            if (vm.updateProfile(customer.copy(outletId = next.id))) feedback("Outlet langganan diubah")
                            saving = false
                        }
                    }
                }, Modifier.testTag("change_profile_outlet"), enabled = !saving && vm.outlets.isNotEmpty()) { Text("Ganti") }
            }
            Text(outlet?.name ?: "Belum dipilih", style = MaterialTheme.typography.titleMedium)
            if (outlet != null) {
                Text(outlet.address, style = MaterialTheme.typography.bodySmall, color = WashMuted)
                Surface(color = Color(0xFFEBF6F0), shape = RoundedCornerShape(8.dp)) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Outlined.Schedule, null, Modifier.size(15.dp), tint = WashGreen)
                        Text(outlet.openingHours, color = WashGreen, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            HorizontalDivider(color = WashBorder)
            TextButton({ vm.selectOutlet(customer.outletId); onLocation() },
                Modifier.align(Alignment.End), enabled = customer.outletId.isNotBlank(), contentPadding = PaddingValues(horizontal = 4.dp)) {
                Text("Lihat lokasi outlet", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.width(6.dp))
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.size(16.dp))
            }
        }
        Button({
            saving = true
            scope.launch(Dispatchers.Main.immediate) {
                if (vm.updateProfile(customer.copy(fragrance = fragrance, notes = notes, notificationEnabled = notifications))) feedback("Perubahan profil disimpan")
                saving = false
            }
        }, Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("save_preferences"), enabled = !saving,
            shape = RoundedCornerShape(14.dp), elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)) {
            Icon(Icons.Outlined.Check, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (saving) "Menyimpan…" else "Simpan Perubahan", style = MaterialTheme.typography.labelLarge)
        }
        TextButton(onLogout, Modifier.align(Alignment.CenterHorizontally).testTag("logout"), colors = ButtonDefaults.textButtonColors(contentColor = WashError)) {
            Icon(Icons.AutoMirrored.Outlined.Logout, null, Modifier.size(18.dp)); Text(" Keluar dari Akun")
        }
        Text("Laundry Pintar & Praktis", Modifier.fillMaxWidth(), style = MaterialTheme.typography.labelSmall, color = WashMuted, textAlign = TextAlign.Center)
    }
    if (editing) EditProfileDialog(customer, { editing = false }) {
        if (!saving) {
            saving = true
            scope.launch(Dispatchers.Main.immediate) {
                if (vm.updateProfile(it)) { editing = false; feedback("Data diri diperbarui") }
                saving = false
            }
        }
    }
}

@Composable
private fun ProfileIdentityCard(customer: Customer) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFDCE9EF)), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFFEAF5FA), Color.White)))
            .padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Surface(Modifier.size(72.dp).shadow(3.dp, CircleShape), shape = CircleShape, color = WashTeal,
                    border = BorderStroke(3.dp, Color.White)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(customer.name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1).uppercase() },
                            style = MaterialTheme.typography.headlineLarge, color = Color.White)
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("PELANGGAN MAXWASH", style = MaterialTheme.typography.labelSmall, color = WashTeal)
                    Text(customer.name, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.testTag("profile_name"))
                    Text(customer.phone, style = MaterialTheme.typography.bodySmall, color = WashMuted)
                }
            }
            HorizontalDivider(color = WashTeal.copy(alpha = 0.10f))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Outlined.WorkspacePremium, null, Modifier.size(19.dp), tint = WashTeal)
                    Text(customer.membership, style = MaterialTheme.typography.labelLarge, color = WashTeal)
                }
                Surface(shape = RoundedCornerShape(10.dp), color = Color.White, border = BorderStroke(1.dp, WashBorder)) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Outlined.Stars, null, Modifier.size(17.dp), tint = WashGreen)
                        Text("${customer.points} Poin", style = MaterialTheme.typography.labelMedium, color = WashNavy)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileCard(content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, WashBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp), content = content)
    }
}

@Composable
private fun ProfileSectionHeader(icon: ImageVector, title: String, subtitle: String, action: @Composable (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ProfileIcon(icon)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = WashMuted)
        }
        action?.invoke()
    }
}

@Composable
private fun ProfileIcon(icon: ImageVector, background: Color = WashSoftBlue) {
    Box(Modifier.size(36.dp).background(background, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
        Icon(icon, null, Modifier.size(19.dp), tint = WashTeal)
    }
}

@Composable
private fun ProfileDivider() = HorizontalDivider(Modifier.padding(start = 50.dp), color = WashBorder)

@Composable
private fun ProfileRow(icon: ImageVector, label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        ProfileIcon(icon, Color(0xFFF5F8FB))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = WashMuted)
            Text(value, style = MaterialTheme.typography.bodyMedium)
        }
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
