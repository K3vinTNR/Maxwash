package org.umn.maxwash.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.umn.maxwash.data.*
import org.umn.maxwash.ui.theme.*

val CheckoutDraftSaver = listSaver<CheckoutDraft, String>(
    save = { listOf(it.address, it.addressNotes, it.outletId, it.collection.name, it.pickupDay.toString(),
        it.pickupSlot, it.serviceId, it.weightInput, it.fragrance, it.hypoallergenic.toString(),
        it.clothingNotes, it.category, it.promoCode, it.payment.name) },
    restore = { CheckoutDraft(it[0], it[1], it[2], CollectionMethod.valueOf(it[3]), it[4].toInt(),
        it[5], it[6], it[7], it[8], it[9].toBoolean(), it[10], it[11], it[12], PaymentMethod.valueOf(it[13])) }
)

@Composable
fun CheckoutHeader(title: String, onBack: () -> Unit, onProfile: () -> Unit) {
    Row(Modifier.fillMaxWidth().statusBarsPadding().heightIn(min = 64.dp).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        IconButton(onBack, Modifier.testTag("checkout_back")) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Kembali", tint = WashNavy) }
        Text(title, Modifier.weight(1f), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        FilledIconButton(onProfile, Modifier.size(34.dp),
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = WashTeal)) {
            Icon(Icons.Outlined.Person, "Profil pelanggan", Modifier.size(19.dp))
        }
    }
}

@Composable
internal fun CheckoutCard(modifier: Modifier = Modifier, color: Color = Color.White,
    content: @Composable ColumnScope.() -> Unit) {
    Card(modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = color)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable
internal fun CheckoutFooter(content: @Composable ColumnScope.() -> Unit) {
    Surface(color = Color.White, shadowElevation = 8.dp) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

@Composable
internal fun CheckoutAction(text: String, onClick: () -> Unit, tag: String, modifier: Modifier = Modifier,
    enabled: Boolean = true, icon: ImageVector? = null) {
    Button(onClick, modifier.heightIn(min = 52.dp).testTag(tag), enabled = enabled, shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
        if (icon != null) { Icon(icon, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)) }
        Text(text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

@Composable
internal fun CheckoutIcon(icon: ImageVector, modifier: Modifier = Modifier, color: Color = WashTeal, background: Color = WashSoftBlue) {
    Box(modifier.size(42.dp).background(background, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
        Icon(icon, null, Modifier.size(22.dp), tint = color)
    }
}

@Composable
internal fun SelectedCircle(selected: Boolean, modifier: Modifier = Modifier) {
    Box(modifier.size(24.dp).background(if (selected) WashTeal else Color(0xFFDCE8FF), CircleShape), contentAlignment = Alignment.Center) {
        if (selected) Icon(Icons.Outlined.Check, null, Modifier.size(16.dp), tint = Color.White)
    }
}
