package org.umn.maxwash.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import org.umn.maxwash.ui.MaxwashViewModel
import org.umn.maxwash.ui.components.*
import org.umn.maxwash.ui.theme.*

@Composable
fun LocationScreen(vm: MaxwashViewModel, feedback: (String) -> Unit) {
    val context = LocalContext.current
    val customer = vm.customer ?: return
    val outlet = vm.outlet(vm.selectedOutletId) ?: vm.outlets.firstOrNull()
    if (outlet == null) { EmptyState("Belum ada outlet", "Data outlet belum tersedia."); return }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Pilih Outlet", style = MaterialTheme.typography.headlineSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            vm.outlets.forEach { item -> FilterChip(item.id == outlet.id, { vm.selectOutlet(item.id) },
                label = { Text(item.name.removePrefix("MAXWASH "), style = MaterialTheme.typography.labelMedium) },
                shape = CircleShape, modifier = Modifier.testTag("outlet_${item.id}")) }
        }
        WashCard {
            SectionLabel("ALAMAT ANDA")
            Text(customer.address.ifBlank { "Lengkapi alamat melalui Profil" }, style = MaterialTheme.typography.bodyMedium)
        }
        WashCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IconTile(Icons.Outlined.Storefront)
                Column(Modifier.weight(1f)) {
                    Text(outlet.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.testTag("location_outlet"))
                    Text(outlet.address, style = MaterialTheme.typography.bodySmall, color = WashMuted)
                }
            }
            Text("Jam operasional: ${outlet.openingHours}", style = MaterialTheme.typography.bodySmall, color = WashGreen)
        }
        PrimaryButton("Buka Petunjuk Arah →", {
            val destination = Uri.encode(outlet.address)
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$destination"))
            try {
                context.startActivity(intent)
            } catch (_: ActivityNotFoundException) {
                feedback("Aplikasi peta atau browser belum tersedia di perangkat ini.")
            }
        }, Modifier.testTag("direction"))
    }
}
