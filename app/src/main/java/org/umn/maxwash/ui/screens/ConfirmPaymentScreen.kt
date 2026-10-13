package org.umn.maxwash.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.umn.maxwash.data.*
import org.umn.maxwash.ui.components.*
import org.umn.maxwash.ui.theme.*

@Composable
fun ConfirmPaymentScreen(draft: CheckoutDraft, service: LaundryService?, outlet: Outlet?, onChange: (CheckoutDraft) -> Unit,
    submitting: Boolean, onPay: () -> Unit) {
    val quote = draft.quote(service)
    var editingPromo by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = submitting) { }
    Column(Modifier.fillMaxSize().testTag("confirm_payment_screen")) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth().background(WashSoftBlue, RoundedCornerShape(20.dp)).padding(horizontal = 12.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("●  KONFIRMASI PEMBAYARAN", color = WashGreen, style = MaterialTheme.typography.labelSmall)
                Icon(Icons.Outlined.Lock, null, Modifier.size(15.dp), tint = WashMuted)
            }
            CheckoutCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CheckoutIcon(Icons.Outlined.LocalLaundryService, background = Color(0xFFDCE8FF))
                    Column {
                        Text("RINGKASAN LAYANAN", style = MaterialTheme.typography.labelSmall, color = WashMuted)
                        Text(service?.checkoutName ?: "Pilih layanan terlebih dahulu", style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.testTag("payment_service"))
                    }
                }
                Row(Modifier.fillMaxWidth().background(WashSoftBlue, RoundedCornerShape(10.dp)).padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("Bobot Estimasi", style = MaterialTheme.typography.labelSmall, color = WashMuted)
                        Text(draft.weightKg?.let { weight(it) } ?: "Belum diisi", style = MaterialTheme.typography.titleSmall, modifier = Modifier.testTag("payment_weight"))
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Outlet Proses", style = MaterialTheme.typography.labelSmall, color = WashMuted)
                        Text(outlet?.name?.removePrefix("MAXWASH ") ?: "Belum dipilih", style = MaterialTheme.typography.titleSmall, modifier = Modifier.testTag("payment_outlet"))
                    }
                }
                Text("${draft.collection.label} · ${if (draft.collection == CollectionMethod.PICKUP) draft.address else outlet?.address.orEmpty()}",
                    style = MaterialTheme.typography.bodySmall, color = WashMuted, modifier = Modifier.testTag("payment_collection"))
            }
            CheckoutCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CheckoutIcon(Icons.Outlined.ConfirmationNumber, color = WashGreen, background = Color(0xFFB8F8E0))
                    Column(Modifier.weight(1f)) {
                        Text(if (quote.discount > 0) "NEWCLEAN5K  ·  Aktif" else "Punya Kode Promo?", style = MaterialTheme.typography.titleSmall)
                        Text(if (quote.discount > 0) "Potongan ${rupiah(quote.discount)}" else "Tambahkan kode promo pilihan Anda",
                            style = MaterialTheme.typography.bodySmall, color = WashMuted)
                    }
                    FilledTonalButton({ editingPromo = true }, Modifier.testTag("payment_edit_promo"), enabled = !submitting,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) { Text(if (quote.discount > 0) "Ganti" else "Tambah", style = MaterialTheme.typography.labelMedium) }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("METODE PEMBAYARAN", style = MaterialTheme.typography.titleSmall)
                Text("Tersedia 3 Opsi", style = MaterialTheme.typography.labelSmall, color = WashTeal)
            }
            PaymentMethod.entries.forEach { method ->
                val selected = method == draft.payment
                Card(Modifier.fillMaxWidth().selectable(selected, enabled = !submitting, onClick = { onChange(draft.copy(payment = method)) }, role = Role.RadioButton)
                    .testTag("payment_method_${method.name}"), shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = if (selected) WashSoftBlue else Color.White)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        RadioButton(selected, { onChange(draft.copy(payment = method)) }, Modifier.size(20.dp), enabled = !submitting)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(method.label, style = MaterialTheme.typography.titleSmall)
                            Text(when (method) {
                                PaymentMethod.QRIS -> "BCA, GoPay, OVO, DANA, ShopeePay & mobile banking"
                                PaymentMethod.VIRTUAL_ACCOUNT -> "BCA, Mandiri, BRI, BNI"
                                PaymentMethod.AT_OUTLET -> "Tunai atau EDC saat pakaian bersih diambil"
                            }, style = MaterialTheme.typography.bodySmall, color = WashMuted)
                        }
                        Icon(when (method) { PaymentMethod.QRIS -> Icons.Outlined.QrCodeScanner
                            PaymentMethod.VIRTUAL_ACCOUNT -> Icons.Outlined.AccountBalance
                            PaymentMethod.AT_OUTLET -> Icons.Outlined.PointOfSale }, null, Modifier.size(20.dp), tint = if (selected) WashTeal else WashMuted)
                    }
                }
            }
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("RINCIAN TAGIHAN", style = MaterialTheme.typography.titleSmall)
                    BillLine("Subtotal Jasa", rupiah(quote.subtotal), draft.weightKg?.let { "${weight(it)} × ${rupiah(service?.pricePerKg ?: 0)} / kg" })
                    BillLine("Parfum · ${draft.fragrance.ifBlank { "Tanpa parfum" }}", "Gratis")
                    if (draft.hypoallergenic) BillLine("Deterjen Hypoallergenic", rupiah(quote.detergent))
                    if (quote.discount > 0) BillLine("Diskon Promo", "−${rupiah(quote.discount)}", color = WashGreen)
                    if (draft.clothingNotes.isNotBlank()) Text("Catatan: ${draft.clothingNotes}", style = MaterialTheme.typography.bodySmall, color = WashMuted)
                }
                Row(Modifier.fillMaxWidth().background(WashSoftBlue).padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Total Akhir", style = MaterialTheme.typography.labelMedium)
                        Text("Berdasarkan bobot estimasi", style = MaterialTheme.typography.labelSmall, color = WashGreen)
                    }
                    Text(rupiah(quote.total), fontSize = 26.sp, fontWeight = FontWeight.Bold, color = WashTeal, modifier = Modifier.testTag("payment_total"))
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.VerifiedUser, null, Modifier.size(17.dp), tint = WashGreen)
                Spacer(Modifier.width(6.dp))
                Text("Periksa rincian layanan sebelum melanjutkan", style = MaterialTheme.typography.bodySmall, color = WashMuted)
            }
        }
        CheckoutFooter {
            Button(onPay, Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("payment_pay_now"),
                enabled = !submitting && draft.locationReady && draft.weightKg != null && service != null && outlet != null, shape = RoundedCornerShape(14.dp)) {
                if (submitting) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                else Icon(Icons.Outlined.Lock, null, Modifier.size(18.dp))
                Spacer(Modifier.width(5.dp))
                Text(if (submitting) "Menyimpan Pesanan…" else "Bayar Sekarang", Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                Text(rupiah(quote.total), fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
            Text("${draft.payment.label} · Periksa kembali pilihan Anda", Modifier.align(Alignment.CenterHorizontally),
                style = MaterialTheme.typography.labelSmall, color = WashMuted)
        }
    }
    if (editingPromo) PromoDialog(draft.promoCode, { editingPromo = false }) { code ->
        onChange(draft.copy(promoCode = code)); editingPromo = false
    }
}

@Composable
private fun BillLine(label: String, amount: String, subtitle: String? = null, color: Color = WashNavy) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = color)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = WashMuted) }
        }
        Text(amount, style = MaterialTheme.typography.bodyMedium, color = color)
    }
}

@Composable
private fun PromoDialog(initialCode: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var code by rememberSaveable { mutableStateOf(initialCode) }
    val valid = code.isBlank() || code.trim().equals("NEWCLEAN5K", true)
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Kode Promo") },
        text = { WashField(code, { code = it }, "Kode Promo", "payment_promo_input",
            error = if (!valid) "Kode promo tidak tersedia" else null) },
        confirmButton = { TextButton({ onSave(code.trim().uppercase()) }, Modifier.testTag("payment_promo_save"), enabled = valid) { Text("Terapkan") } },
        dismissButton = { TextButton(onDismiss) { Text("Batal") } })
}
