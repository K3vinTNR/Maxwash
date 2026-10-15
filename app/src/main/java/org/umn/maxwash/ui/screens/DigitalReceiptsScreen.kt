package org.umn.maxwash.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.umn.maxwash.data.*
import org.umn.maxwash.export.ReceiptPdf
import org.umn.maxwash.ui.MaxwashViewModel
import org.umn.maxwash.ui.components.*
import org.umn.maxwash.ui.theme.*
import java.io.IOException

@Composable
fun DigitalReceiptsScreen(vm: MaxwashViewModel, orderId: String? = null,
    onAllReceipts: () -> Unit, feedback: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var exportId by rememberSaveable { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    val savePdf = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        val receipt = vm.receipt(exportId)
        val order = vm.order(exportId)
        exportId = null
        if (uri != null) {
            if (receipt == null || !receipt.isPaid || order == null) feedback("Nota pembayaran lunas tidak tersedia.")
            else scope.launch {
                saving = true
                try {
                    withContext(Dispatchers.IO) {
                        val output = context.contentResolver.openOutputStream(uri, "w")
                            ?: throw IOException("Tidak dapat membuka lokasi penyimpanan")
                        output.use { ReceiptPdf.write(receipt, order, it) }
                    }
                    feedback("Nota PDF berhasil disimpan.")
                } catch (error: Exception) {
                    if (error is CancellationException) throw error
                    feedback("Nota PDF gagal disimpan. Silakan coba lagi.")
                } finally { saving = false }
            }
        }
    }
    val receipts = vm.paidReceipts
    val selected = vm.receipt(orderId)
    if (orderId != null && selected?.isPaid != true) {
        Column(Modifier.fillMaxSize().testTag("receipt_unavailable"), horizontalAlignment = Alignment.CenterHorizontally) {
            EmptyState("Nota lunas belum tersedia", if (selected != null)
                "Pesanan ini menggunakan Bayar di Outlet. Nota dapat diterbitkan setelah pembayaran tercatat lunas."
                else "Data pembayaran pesanan ini belum tercatat. Nota lunas tidak diterbitkan tanpa rincian transaksi.")
            TextButton(onAllReceipts, Modifier.testTag("receipt_show_all")) { Text("Lihat Semua Nota Digital") }
        }
        return
    }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex =
        receipts.indexOfFirst { it.orderId == orderId }.coerceAtLeast(0))
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Bukti Pembayaran", style = MaterialTheme.typography.titleMedium)
                Text("Gulir untuk melihat nota sebelumnya", style = MaterialTheme.typography.bodySmall, color = WashMuted)
            }
            Pill("${receipts.size} Nota")
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth().testTag("receipts_list"), state = listState,
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)) {
            if (receipts.isEmpty()) item {
                EmptyState("Belum ada nota lunas", "Nota Digital akan muncul di sini setelah pembayaran pesanan tercatat lunas.",
                    Modifier.testTag("receipts_empty"))
            }
            items(receipts, key = { it.orderId }) { receipt ->
                val order = vm.order(receipt.orderId)
                if (order != null) DigitalReceiptCard(receipt, order, !saving && exportId == null && receipt.hasValidTotal) {
                    exportId = receipt.orderId
                    savePdf.launch("MAXWASH-${receipt.number}.pdf")
                }
            }
        }
    }
}

@Composable
private fun DigitalReceiptCard(receipt: DigitalReceipt, order: LaundryOrder, canDownload: Boolean, onDownload: () -> Unit) {
    Card(Modifier.fillMaxWidth().testTag("receipt_${receipt.orderId}"), shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Box(Modifier.fillMaxWidth().height(7.dp).background(Brush.horizontalGradient(listOf(WashBlue, WashTeal))))
        Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("• E-RECEIPT MAXWASH", style = MaterialTheme.typography.labelSmall, color = WashGreen, fontWeight = FontWeight.Bold)
                Pill("✓ Lunas", WashGreen, WashMint)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(receipt.outletName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(receipt.outletAddress, style = MaterialTheme.typography.bodySmall, color = WashMuted)
            }
            Column(Modifier.fillMaxWidth().background(WashBackground, RoundedCornerShape(12.dp)).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ReceiptField("No. Nota", receipt.number, Modifier.weight(1f), teal = true)
                    ReceiptField("Waktu Transaksi", LocalTimeFormat.display(requireNotNull(receipt.paidAt), true), Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        ReceiptField("Pelanggan", receipt.customerName)
                        Text(receipt.customerPhone, style = MaterialTheme.typography.bodySmall, color = WashMuted)
                        Text(receipt.customerEmail, style = MaterialTheme.typography.bodySmall, color = WashMuted)
                    }
                    ReceiptField("No. Pesanan", receipt.orderId, Modifier.weight(1f))
                }
                if (receipt.customerAddress.isNotBlank()) ReceiptField("Alamat Pelanggan", receipt.customerAddress)
            }
        }
        ReceiptSeam()
        Column(Modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("RINCIAN PENIMBANGAN & CUCI", style = MaterialTheme.typography.labelSmall, color = WashMuted)
            ReceiptItem(Icons.Outlined.LocalLaundryService, receipt.serviceName, rupiah(receipt.subtotal),
                "Berat estimasi ${receiptWeight(receipt.weightKg)} × ${rupiah(receipt.pricePerKg)} / kg")
            Text("Aroma: ${receipt.fragrance.ifBlank { "Tanpa parfum" }}", style = MaterialTheme.typography.bodySmall, color = WashTeal)
            if (receipt.detergent > 0) ReceiptItem(Icons.Outlined.Spa, "Deterjen Hypoallergenic", rupiah(receipt.detergent), "Tambahan perawatan cucian")
            if (receipt.discount > 0) ReceiptItem(Icons.Outlined.LocalOffer, "Diskon Promo", "−${rupiah(receipt.discount)}", receipt.promoCode, green = true)
            if (receipt.clothingNotes.isNotBlank()) ReceiptField("Catatan Pakaian", receipt.clothingNotes)
        }
        ReceiptSeam()
        Column(Modifier.padding(start = 18.dp, end = 18.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            ReceiptSummary("Metode Pembayaran", receipt.paymentLabel)
            ReceiptSummary("Biaya Layanan POS", "GRATIS", green = true)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Total Akhir", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(rupiah(receipt.total), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                    color = WashTeal, modifier = Modifier.testTag("receipt_total_${receipt.orderId}"))
            }
            Row(Modifier.fillMaxWidth().background(WashMint, RoundedCornerShape(10.dp)).padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Outlined.Inventory2, null, Modifier.size(18.dp), tint = WashGreen)
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Status Pengambilan · ${order.collectionTitle}", style = MaterialTheme.typography.labelMedium, color = WashGreen)
                    Text(order.collectionMessage, style = MaterialTheme.typography.bodySmall, color = WashGreen)
                }
            }
            FilledTonalButton(onDownload, Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("receipt_download_${receipt.orderId}"),
                enabled = canDownload, shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = WashLavender, contentColor = WashTeal)) {
                Icon(Icons.Outlined.FileDownload, null, Modifier.size(18.dp))
                Spacer(Modifier.width(7.dp))
                Text("Unduh / Simpan Nota PDF")
            }
            Text("Transaksi simulasi MAXWASH", style = MaterialTheme.typography.labelSmall, color = WashMuted,
                modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}

@Composable
private fun ReceiptField(label: String, value: String, modifier: Modifier = Modifier, teal: Boolean = false) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = WashMuted)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold,
            color = if (teal) WashTeal else WashNavy)
    }
}

@Composable
private fun ReceiptItem(icon: ImageVector, title: String, amount: String, detail: String, green: Boolean = false) {
    Row(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.Top) {
        IconTile(icon, Modifier.size(32.dp), if (green) WashMint else WashSoftBlue, if (green) WashGreen else WashTeal)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = WashMuted)
        }
        Text(amount, Modifier.widthIn(max = 115.dp), style = MaterialTheme.typography.labelLarge,
            color = if (green) WashGreen else WashNavy)
    }
}

@Composable
private fun ReceiptSummary(label: String, value: String, green: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = WashMuted)
        Text(value, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall,
            color = if (green) WashGreen else WashNavy, fontWeight = FontWeight.SemiBold,
            textAlign = androidx.compose.ui.text.style.TextAlign.End)
    }
}

@Composable
private fun ReceiptSeam() {
    Canvas(Modifier.fillMaxWidth().height(30.dp)) {
        drawLine(WashBorder, Offset(18.dp.toPx(), size.height / 2), Offset(size.width - 18.dp.toPx(), size.height / 2),
            strokeWidth = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx())))
        drawCircle(WashBackground, 7.dp.toPx(), Offset(0f, size.height / 2))
        drawCircle(WashBackground, 7.dp.toPx(), Offset(size.width, size.height / 2))
    }
}
