package org.umn.maxwash.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.umn.maxwash.data.MockRepository
import org.umn.maxwash.ui.MaxwashViewModel
import org.umn.maxwash.ui.components.*
import org.umn.maxwash.ui.theme.*
import java.util.Locale

@Composable
fun LocationScreen(vm: MaxwashViewModel, feedback: (String) -> Unit) {
    var directionDialog by rememberSaveable { mutableStateOf(false) }
    val outlet = MockRepository.outlet(vm.selectedOutletId)
    val distance = String.format(Locale.US, "%.1f km", vm.distanceKm())
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        LocalMap(vm.selectedOutletId == "OUT-002", vm.locationVersion) { vm.refreshLocation(); feedback("Lokasi simulasi diperbarui") }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MockRepository.outlets.forEach { item -> FilterChip(item.id == vm.selectedOutletId, { vm.selectedOutletId = item.id },
                label = { Text(if (item.id == "OUT-001") "Bintaro Sektor 7" else "Senopati", style = MaterialTheme.typography.labelMedium) },
                shape = CircleShape, modifier = Modifier.testTag("outlet_${item.id}")) }
        }
        Row(Modifier.fillMaxWidth().background(Color(0xFFBBF8DF), MaterialTheme.shapes.extraLarge).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Verified, null, Modifier.size(20.dp), tint = WashGreen)
            Text("  Lokasi outlet & jarak untuk simulasi", style = MaterialTheme.typography.labelMedium, color = WashGreen)
        }
        WashCard {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Pill("⌂ Titik Anda (simulasi)")
                TextButton({ vm.refreshLocation(); feedback("Titik pelanggan pada peta simulasi diperbarui") }) { Text("Perbarui") }
            }
            Text(vm.customer.address, style = MaterialTheme.typography.titleMedium)
            Text("Posisi ini menggunakan data lokal. Tidak mengakses GPS perangkat.", style = MaterialTheme.typography.bodySmall, color = WashMuted)
        }
        WashCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IconTile(Icons.Outlined.Storefront, background = Color(0xFFCCE9FF))
                Column(Modifier.weight(1f)) {
                    Text(outlet.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.testTag("location_outlet"))
                    Text(outlet.address, style = MaterialTheme.typography.bodySmall, color = WashMuted)
                }
            }
            Pill("• Buka · ${outlet.openingHours}", WashGreen, Color(0xFFBBF8DF))
            Text("$distance dari titik Anda · Estimasi 8–15 menit", style = MaterialTheme.typography.bodySmall, color = WashMuted, modifier = Modifier.testTag("location_distance"))
        }
        PrimaryButton("Direction · Petunjuk Arah →", { directionDialog = true }, Modifier.testTag("direction"))
    }
    if (directionDialog) AlertDialog(onDismissRequest = { directionDialog = false },
        icon = { Icon(Icons.Outlined.NearMe, null, tint = WashTeal) }, title = { Text("Petunjuk Arah Demo") },
        text = { Text("Tujuan: ${outlet.name}\n${outlet.address}\n\nJarak simulasi: $distance. Navigasi ini hanya demo; tidak membuka peta atau GPS nyata.") },
        confirmButton = { TextButton({ directionDialog = false }, Modifier.testTag("direction_close")) { Text("Mengerti") } })
}

@Composable
private fun LocalMap(senopati: Boolean, version: Int, onRefresh: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth().height(300.dp).clip(MaterialTheme.shapes.extraLarge).background(Color(0xFFECF1EF))
        .semantics { contentDescription = "Peta simulasi dengan marker outlet dan lokasi pelanggan" }) {
        val customerX = maxWidth * (0.28f + version * 0.03f)
        val outletX = maxWidth * if (senopati) 0.68f else 0.60f
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            drawRect(Color(0xFFD5E9DA), Offset(w * .06f, h * .28f), Size(w * .22f, h * .22f))
            drawRect(Color(0xFFD5E9DA), Offset(w * .68f, h * .62f), Size(w * .24f, h * .2f))
            for (i in 1..8) {
                val x = i * w / 8
                drawLine(Color.White, Offset(x - w * .15f, 0f), Offset(x + w * .1f, h), 12.dp.toPx())
                drawLine(Color(0xFFD1DCDD), Offset(x - w * .15f, 0f), Offset(x + w * .1f, h), 1.dp.toPx())
            }
            for (i in 1..6) {
                val y = i * h / 6
                drawLine(Color.White, Offset(0f, y), Offset(w, y - h * .1f), 10.dp.toPx())
                drawLine(Color(0xFFD1DCDD), Offset(0f, y), Offset(w, y - h * .1f), 1.dp.toPx())
            }
            val river = Path().apply { moveTo(w * .1f, h); cubicTo(w * .9f, h * .9f, w * .3f, h * .35f, w * .95f, 0f) }
            drawPath(river, Color(0xFFBCDDEC), style = Stroke(9.dp.toPx()))
            drawLine(WashTeal, Offset(w * (.28f + version * .03f) + 16.dp.toPx(), h * .66f + 16.dp.toPx()),
                Offset(w * (if (senopati) .68f else .60f) + 16.dp.toPx(), h * .3f + 16.dp.toPx()),
                3.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f)))
        }
        Box(Modifier.align(Alignment.TopCenter).padding(12.dp)) { Pill("• LOKASI SIMULASI", WashGreen, Color.White) }
        Text(if (senopati) "JAKARTA SELATAN" else "BINTARO", Modifier.align(Alignment.Center).offset(y = 30.dp), color = WashMuted, style = MaterialTheme.typography.labelSmall)
        Column(Modifier.offset(x = outletX, y = 90.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(34.dp).background(WashGreen, CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.LocalLaundryService, null, Modifier.size(22.dp), tint = Color.White) }
            Pill("MAXWASH", WashGreen, Color.White)
        }
        Column(Modifier.offset(x = customerX, y = 198.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(34.dp).background(WashTeal, CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.LocationOn, null, Modifier.size(22.dp), tint = Color.White) }
            Pill("Titik Anda", Color.White, WashTeal)
        }
        FilledIconButton(onRefresh, Modifier.align(Alignment.BottomEnd).padding(12.dp).testTag("refresh_location"),
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.White, contentColor = WashTeal)) {
            Icon(Icons.Outlined.MyLocation, "Perbarui lokasi simulasi")
        }
    }
}
