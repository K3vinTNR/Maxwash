package org.umn.maxwash.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.umn.maxwash.data.*
import org.umn.maxwash.ui.components.*
import org.umn.maxwash.ui.theme.*

@Composable
fun ChooseServiceScreen(draft: CheckoutDraft, services: List<LaundryService>, fragrances: List<String>,
    onChange: (CheckoutDraft) -> Unit, onContinue: () -> Unit) {
    val selectedService = services.find { it.id == draft.serviceId }
    val quote = draft.quote(selectedService)
    val order = listOf("SVC-002", "SVC-003", "SVC-001", "SVC-004")
    val sortedServices = services.sortedBy { order.indexOf(it.id).takeIf { index -> index >= 0 } ?: Int.MAX_VALUE }
    Column(Modifier.fillMaxSize().testTag("choose_service_screen")) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            CheckoutCard(color = WashSoftBlue) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CheckoutIcon(Icons.Outlined.Spa, background = Color(0xFFD0E9FF))
                    Column {
                        Text("Standar Higienis MAXWASH", style = MaterialTheme.typography.labelLarge, color = WashTeal)
                        Text("Pilih perawatan yang sesuai dengan cucian Anda.", style = MaterialTheme.typography.bodySmall, color = WashMuted)
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Kategori Layanan", style = MaterialTheme.typography.titleSmall)
                Text("${if (draft.category == "Kiloan") services.size else 0} PILIHAN", style = MaterialTheme.typography.labelSmall, color = WashTeal)
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Kiloan" to Icons.Outlined.HourglassBottom, "Satuan / Bedding" to Icons.Outlined.Bed,
                    "Dry Cleaning" to Icons.Outlined.DryCleaning).forEach { (category, icon) ->
                    FilterChip(draft.category == category, { onChange(draft.copy(category = category)) },
                        label = { Text(category, fontSize = 11.sp) }, leadingIcon = { Icon(icon, null, Modifier.size(15.dp)) },
                        shape = RoundedCornerShape(20.dp), modifier = Modifier.height(30.dp).testTag("category_$category"),
                        colors = FilterChipDefaults.filterChipColors(containerColor = Color.White, selectedContainerColor = WashTeal,
                            selectedLabelColor = Color.White, selectedLeadingIconColor = Color.White), border = null)
                }
            }
            if (draft.category == "Kiloan") sortedServices.forEach { service ->
                ServiceChoice(service, service.id == draft.serviceId) { onChange(draft.copy(serviceId = service.id)) }
            } else CheckoutCard {
                EmptyState("Layanan belum tersedia", "Pilih kategori Kiloan untuk melanjutkan.")
            }
            if (draft.category == "Kiloan") {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FoldedLaundryArt(Modifier.size(90.dp))
                        Column(Modifier.weight(1f).padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text("PERAWATAN LEMBUT", style = MaterialTheme.typography.labelMedium, color = WashTeal)
                            Text("Pilih perawatan dan aroma yang sesuai dengan pakaian Anda.", style = MaterialTheme.typography.bodySmall, color = WashMuted)
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Spa, null, Modifier.size(18.dp), tint = WashTeal)
                    Spacer(Modifier.width(5.dp))
                    Text("Pilihan Aroma Parfum", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                    Text("Gratis", style = MaterialTheme.typography.labelMedium, color = WashGreen)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    fragrances.forEachIndexed { index, fragrance ->
                        FragranceChoice(fragrance, draft.fragrance == fragrance, index, Modifier.weight(1f)) {
                            onChange(draft.copy(fragrance = fragrance))
                        }
                    }
                }
                CheckoutCard(color = WashSoftBlue) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CheckoutIcon(Icons.Outlined.HealthAndSafety, color = WashGreen, background = Color.White)
                        Column(Modifier.weight(1f)) {
                            Text("Deterjen Hypoallergenic +Rp 2.000", style = MaterialTheme.typography.labelMedium, color = WashTeal)
                            Text("Pilihan untuk kulit sensitif & bayi", style = MaterialTheme.typography.bodySmall, color = WashMuted)
                        }
                        Switch(draft.hypoallergenic, { onChange(draft.copy(hypoallergenic = it)) }, Modifier.testTag("checkout_hypoallergenic"))
                    }
                }
                WashField(draft.clothingNotes, { onChange(draft.copy(clothingNotes = it)) },
                    "Catatan pakaian khusus", "checkout_clothing_notes", multiline = true)
                WashField(draft.weightInput, { onChange(draft.copy(weightInput = it)) }, "Estimasi Berat (kg)", "checkout_weight",
                    KeyboardType.Decimal, if (draft.weightInput.isNotBlank() && draft.weightKg == null) "Masukkan berat lebih dari 0, maksimal 100 kg" else null)
                Text("Berat akhir dikonfirmasi saat cucian ditimbang di outlet.", style = MaterialTheme.typography.bodySmall, color = WashMuted)
            }
        }
        CheckoutFooter {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(draft.weightKg?.let { "Est. ${weight(it)}" } ?: "Estimasi biaya", style = MaterialTheme.typography.bodySmall, color = WashMuted)
                    Text(rupiah(quote.total), color = WashTeal, fontSize = 21.sp, fontWeight = FontWeight.Bold, modifier = Modifier.testTag("checkout_estimate_total"))
                }
                CheckoutAction("Lanjut Bayar  →", onContinue, "service_continue", Modifier.weight(1.4f),
                    draft.locationReady && draft.category == "Kiloan" && draft.weightKg != null && selectedService != null)
            }
        }
    }
}

@Composable
private fun ServiceChoice(service: LaundryService, selected: Boolean, onClick: () -> Unit) {
    val icon = when (service.id) {
        "SVC-003" -> Icons.Outlined.Bolt
        "SVC-001" -> Icons.Outlined.Checkroom
        "SVC-004" -> Icons.Outlined.Iron
        else -> Icons.Outlined.LocalLaundryService
    }
    val description = when (service.id) {
        "SVC-002" -> "Cuci + Pengeringan Drum + Setrika Uap Rapi"
        "SVC-003" -> "Selesai kilat, langsung siap antar"
        "SVC-001" -> "Cuci bersih & dikeringkan wangi, tanpa setrika"
        "SVC-004" -> "Pelurusan serat pakaian dengan setrika uap"
        else -> service.name
    }
    Card(Modifier.fillMaxWidth().selectable(selected, onClick = onClick, role = Role.RadioButton).testTag("checkout_service_${service.id}"),
        shape = RoundedCornerShape(16.dp), border = if (selected) BorderStroke(1.5.dp, WashTeal) else null,
        colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
            CheckoutIcon(icon)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(service.checkoutName, fontSize = 19.sp, lineHeight = 23.sp, fontWeight = FontWeight.SemiBold)
                if (service.id in listOf("SVC-002", "SVC-003")) Pill(if (service.id == "SVC-002") "● Rekomendasi" else "Prioritas",
                    if (service.id == "SVC-002") WashGreen else WashMuted, if (service.id == "SVC-002") Color(0xFFB8F8E0) else WashLavender)
                Text(description, style = MaterialTheme.typography.bodySmall, color = WashMuted)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Outlined.Schedule, null, Modifier.size(14.dp), tint = WashMuted)
                    Text("Est. ${if (service.turnaroundHours >= 24) "${service.turnaroundHours / 24} Hari" else "${service.turnaroundHours} Jam"}",
                        style = MaterialTheme.typography.labelSmall, color = if (service.id == "SVC-003") WashTeal else WashMuted)
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SelectedCircle(selected)
                Text(rupiah(service.pricePerKg), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (selected) WashTeal else WashNavy)
                Text("/kg", style = MaterialTheme.typography.bodySmall, color = WashMuted)
            }
        }
    }
}

@Composable
private fun FragranceChoice(name: String, selected: Boolean, index: Int, modifier: Modifier, onClick: () -> Unit) {
    val icon: ImageVector = when (index) { 0 -> Icons.Outlined.LocalFlorist; 1 -> Icons.Outlined.Air; else -> Icons.Outlined.ChildCare }
    Card(modifier.selectable(selected, onClick = onClick, role = Role.RadioButton).testTag("fragrance_$name"), shape = RoundedCornerShape(14.dp),
        border = if (selected) BorderStroke(1.5.dp, WashTeal) else null, colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, null, Modifier.size(20.dp), tint = if (selected) WashTeal else WashMuted)
            Text(name.replace(" ", "\n"), style = MaterialTheme.typography.labelMedium)
            Text(when (index) { 0 -> "Menenangkan"; 1 -> "Segar Aktif"; else -> "Lembut Alami" }, fontSize = 9.sp, color = WashMuted)
        }
    }
}

@Composable
private fun FoldedLaundryArt(modifier: Modifier) {
    Canvas(modifier.background(Color(0xFFE3ECE8))) {
        val w = size.width; val h = size.height
        listOf(Color(0xFF8EA6B5), Color(0xFFF5F0E5), Color(0xFFB8CAC3), Color.White).forEachIndexed { index, color ->
            val y = h * (.72f - index * .13f)
            drawRoundRect(color.copy(alpha = .8f), Offset(w * .15f, y + h * .015f), Size(w * .74f, h * .15f), CornerRadius(w * .025f))
            drawRoundRect(color, Offset(w * .12f, y), Size(w * .72f, h * .12f), CornerRadius(w * .035f))
            drawLine(color.copy(alpha = .7f), Offset(w * .18f, y + h * .085f), Offset(w * .77f, y + h * .085f), 2f)
        }
    }
}
