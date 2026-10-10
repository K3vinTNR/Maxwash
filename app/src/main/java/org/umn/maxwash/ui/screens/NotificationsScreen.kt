package org.umn.maxwash.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import org.umn.maxwash.data.MockRepository
import org.umn.maxwash.ui.MaxwashViewModel
import org.umn.maxwash.ui.components.*
import org.umn.maxwash.ui.theme.*

@Composable
fun NotificationsScreen(vm: MaxwashViewModel, onOrder: (String) -> Unit) {
    var unreadOnly by rememberSaveable { mutableStateOf(false) }
    val notifications = MockRepository.notifications.filter { !unreadOnly || it.id !in vm.readNotificationIds }
    LazyColumn(Modifier.fillMaxSize().testTag("notifications_list"), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Pemberitahuan", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                Pill("${vm.unreadCount} Baru")
            }
            TextButton(vm::markAllRead, Modifier.testTag("mark_all_read")) { Text("✓ Tandai semua dibaca") }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(!unreadOnly, { unreadOnly = false }, label = { Text("Semua (${MockRepository.notifications.size})") }, shape = CircleShape)
                FilterChip(unreadOnly, { unreadOnly = true }, label = { Text("Belum Dibaca") }, shape = CircleShape, modifier = Modifier.testTag("unread_filter"))
            }
        }
        if (notifications.isEmpty()) item { EmptyState("Semua sudah dibaca", "Tidak ada notifikasi baru.", Modifier.testTag("notifications_empty")) }
        items(notifications, key = { it.id }) { notification ->
            val unread = notification.id !in vm.readNotificationIds
            WashCard(Modifier.testTag("notification_${notification.id}").clickable { vm.markRead(notification.id); onOrder(notification.orderId) }) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconTile(if (notification.id == "N-001") Icons.Outlined.CheckCircle else Icons.Outlined.LocalLaundryService,
                        background = if (notification.id == "N-001") WashMint else WashLavender)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(notification.title, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                            if (unread) Box(Modifier.size(7.dp).background(WashTeal, CircleShape))
                        }
                        Text(notification.message, style = MaterialTheme.typography.bodyMedium, color = WashMuted)
                        Text("#${notification.orderId} · ${notification.time}", style = MaterialTheme.typography.labelSmall, color = WashTeal)
                        Text(if (unread) "Belum dibaca" else "Sudah dibaca", style = MaterialTheme.typography.labelSmall, color = if (unread) WashTeal else WashMuted)
                    }
                }
            }
        }
        item { Text("Semua notifikasi status telah ditampilkan", Modifier.padding(vertical = 18.dp), style = MaterialTheme.typography.bodySmall, color = WashMuted) }
    }
}
