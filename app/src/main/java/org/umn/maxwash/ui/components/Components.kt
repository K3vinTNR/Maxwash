package org.umn.maxwash.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.umn.maxwash.data.LaundryStatus
import org.umn.maxwash.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

fun rupiah(value: Int): String = "Rp " + NumberFormat.getIntegerInstance(Locale.forLanguageTag("id-ID")).format(value)
fun weight(value: Double): String = String.format(Locale.US, "%.1f kg", value)

@Composable
fun WashLogo(size: Int = 36) {
    Box(Modifier.size(size.dp).background(WashTeal, RoundedCornerShape((size / 3).dp)), contentAlignment = Alignment.Center) {
        Icon(Icons.Outlined.LocalLaundryService, null, Modifier.size((size * .65f).dp), tint = Color.White)
    }
}

@Composable
fun WashHeader(title: String, unread: Int, onNotifications: () -> Unit, onProfile: () -> Unit, onBack: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) IconButton(onClick = onBack, modifier = Modifier.testTag("back")) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Kembali")
        } else Spacer(Modifier.width(4.dp))
        WashLogo()
        Column(Modifier.weight(1f).padding(start = 8.dp)) {
            Row {
                Text("MAX", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("WASH", color = WashTeal, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            Text(title, style = MaterialTheme.typography.labelSmall)
        }
        IconButton(onClick = onNotifications, modifier = Modifier.testTag("nav_notifications")) {
            BadgedBox(badge = { if (unread > 0) Badge(containerColor = WashTeal) { Text(unread.toString()) } }) {
                Icon(Icons.Outlined.NotificationsNone, "Notifikasi", tint = WashMuted)
            }
        }
        IconButton(onClick = onProfile, modifier = Modifier.testTag("header_profile")) {
            Box(Modifier.size(32.dp).background(WashTeal, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Person, "Profil pelanggan", Modifier.size(20.dp), tint = Color.White)
            }
        }
    }
}

@Composable
fun WashBottomBar(selected: String, onNavigate: (String) -> Unit) {
    NavigationBar(containerColor = WashBackground, tonalElevation = 0.dp) {
        listOf(Triple("home", "Beranda", Icons.Outlined.Home), Triple("orders", "Pesanan", Icons.Outlined.LocalShipping),
            Triple("history", "Riwayat", Icons.AutoMirrored.Outlined.ReceiptLong), Triple("profile", "Profil", Icons.Outlined.AccountCircle)).forEach { (route, label, icon) ->
            NavigationBarItem(selected = selected == route, onClick = { onNavigate(route) },
                icon = { Icon(icon, null, Modifier.size(22.dp)) },
                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent,
                    selectedIconColor = WashTeal, selectedTextColor = WashTeal, unselectedIconColor = WashMuted, unselectedTextColor = WashMuted),
                modifier = Modifier.testTag("nav_$route"))
        }
    }
}

@Composable
fun WashCard(modifier: Modifier = Modifier, color: Color = Color.White, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = color),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable
fun IconTile(icon: ImageVector, modifier: Modifier = Modifier, background: Color = WashSoftBlue, tint: Color = WashTeal) {
    Box(modifier.size(38.dp).background(background, MaterialTheme.shapes.small), contentAlignment = Alignment.Center) {
        Icon(icon, null, Modifier.size(22.dp), tint = tint)
    }
}

@Composable
fun Pill(text: String, color: Color = WashTeal, background: Color = WashSoftBlue) {
    Text(text, Modifier.background(background, CircleShape).padding(horizontal = 9.dp, vertical = 4.dp),
        color = color, style = MaterialTheme.typography.labelSmall)
}

@Composable
fun StatusPill(status: LaundryStatus) = Pill("• ${status.label}",
    if (status.ordinal >= LaundryStatus.READY.ordinal) WashGreen else WashTeal,
    if (status.ordinal >= LaundryStatus.READY.ordinal) Color(0xFFE0F6EE) else WashLavender)

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(onClick, modifier.fillMaxWidth().heightIn(min = 48.dp), enabled = enabled, shape = MaterialTheme.shapes.small) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun WashField(value: String, onValueChange: (String) -> Unit, label: String, tag: String,
    keyboardType: KeyboardType = KeyboardType.Text, error: String? = null, password: Boolean = false,
    multiline: Boolean = false, enabled: Boolean = true) {
    var visible by rememberSaveable { mutableStateOf(false) }
    OutlinedTextField(value, onValueChange, modifier = Modifier.fillMaxWidth().testTag(tag),
        label = { Text(label) }, singleLine = !multiline, enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, capitalization = if (tag.endsWith("name")) KeyboardCapitalization.Words else KeyboardCapitalization.None,
            imeAction = if (multiline) ImeAction.Default else ImeAction.Next),
        shape = MaterialTheme.shapes.small, isError = error != null,
        supportingText = if (error != null) ({ Text(error) }) else null,
        visualTransformation = if (password && !visible) PasswordVisualTransformation() else VisualTransformation.None,
        trailingIcon = if (password) ({ IconButton(onClick = { visible = !visible }) {
            Icon(if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, if (visible) "Sembunyikan password" else "Tampilkan password")
        } }) else null,
        colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = WashSoftBlue, focusedContainerColor = WashSoftBlue,
            unfocusedBorderColor = WashBorder, focusedBorderColor = WashTeal))
}

@Composable
fun EmptyState(title: String, message: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Outlined.Inbox, null, Modifier.size(42.dp), tint = WashTeal)
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = WashMuted)
    }
}

@Composable
fun SectionLabel(text: String) { Text(text, style = MaterialTheme.typography.labelMedium, color = WashMuted) }

@Composable
fun MockQr(customerId: String, modifier: Modifier = Modifier) {
    // Identity visualization only: no encoded token or scanner is required for the UTS prototype.
    Canvas(modifier.size(152.dp).background(Color.White).semantics { contentDescription = "QR visual pelanggan $customerId, simulasi" }.testTag("customer_qr")) {
        val count = 29
        val unit = size.minDimension / (count + 8)
        fun inFinder(x: Int, y: Int): Boolean = (x < 8 && y < 8) || (x >= count - 8 && y < 8) || (x < 8 && y >= count - 8)
        for (y in 0 until count) for (x in 0 until count) {
            if (!inFinder(x, y) && ((x * 17 + y * 31 + x * y + customerId.hashCode()) and 3) < 2)
                drawRect(WashNavy, Offset((x + 4) * unit, (y + 4) * unit), Size(unit, unit))
        }
        for ((x, y) in listOf(0 to 0, count - 7 to 0, 0 to count - 7)) {
            drawRect(WashNavy, Offset((x + 4) * unit, (y + 4) * unit), Size(7 * unit, 7 * unit))
            drawRect(Color.White, Offset((x + 5) * unit, (y + 5) * unit), Size(5 * unit, 5 * unit))
            drawRect(WashNavy, Offset((x + 6) * unit, (y + 6) * unit), Size(3 * unit, 3 * unit))
        }
    }
}
