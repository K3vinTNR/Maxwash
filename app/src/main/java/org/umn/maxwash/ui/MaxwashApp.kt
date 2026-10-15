package org.umn.maxwash.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import org.umn.maxwash.ui.components.*
import org.umn.maxwash.ui.screens.*
import org.umn.maxwash.ui.theme.*
import org.umn.maxwash.data.CheckoutDraft

@Composable
fun MaxwashApp(vm: MaxwashViewModel = viewModel(), startDestination: String = "splash") {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: startDestination
    val auth = route in listOf("splash", "login", "register")
    val checkout = route in listOf("pickup", "choose_service", "confirm_payment")
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val feedback: (String) -> Unit = { message -> scope.launch(Dispatchers.Main.immediate) { snackbar.showSnackbar(message) } }
    fun topNavigate(destination: String) {
        nav.navigate(destination) {
            popUpTo("home") { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    fun enterHome() { nav.navigate("home") { popUpTo(nav.graph.id) { inclusive = true }; launchSingleTop = true } }
    fun logout() { scope.launch(Dispatchers.Main.immediate) {
        if (vm.logout()) nav.navigate("login") { popUpTo(nav.graph.id) { inclusive = true }; launchSingleTop = true }
    } }
    fun details(id: String) { nav.navigate("orders/$id") }
    fun receipt(id: String) { nav.navigate("receipts/$id") { launchSingleTop = true } }
    LaunchedEffect(route, vm.signedIn, vm.isLoading) {
        if (!vm.isLoading) {
            if (!auth && !vm.signedIn) nav.navigate("login") { popUpTo(nav.graph.id) { inclusive = true } }
            else if (route in listOf("login", "register") && vm.signedIn) enterHome()
        }
    }
    LaunchedEffect(vm.errorMessage) {
        if (vm.errorMessage != null && !auth) {
            snackbar.showSnackbar(vm.errorMessage!!)
            vm.clearError()
        }
    }
    if (vm.isLoading || (vm.errorMessage != null && vm.outlets.isEmpty())) {
        Surface(Modifier.fillMaxSize(), color = WashBackground) {
            Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                if (vm.isLoading) CircularProgressIndicator()
                else {
                    Text(vm.errorMessage.orEmpty())
                    TextButton(vm::retryLoading) { Text("Coba lagi") }
                }
            }
        }
        return
    }
    var checkoutDraft by rememberSaveable(vm.customer?.id, stateSaver = CheckoutDraftSaver) {
        mutableStateOf(CheckoutDraft(address = vm.customer?.address.orEmpty(), addressNotes = vm.customer?.notes.orEmpty(),
            outletId = vm.selectedOutletId.ifBlank { vm.outlets.firstOrNull()?.id.orEmpty() },
            serviceId = (vm.services.find { it.id == "SVC-002" } ?: vm.services.firstOrNull())?.id.orEmpty(),
            fragrance = vm.customer?.fragrance.orEmpty().ifBlank { vm.fragrances.firstOrNull().orEmpty() }))
    }
    val profileAddress = vm.customer?.address.orEmpty()
    val profileNotes = vm.customer?.notes.orEmpty()
    var previousProfileAddress by rememberSaveable(vm.customer?.id) { mutableStateOf(profileAddress) }
    var previousProfileNotes by rememberSaveable(vm.customer?.id) { mutableStateOf(profileNotes) }
    LaunchedEffect(vm.customer?.id, profileAddress, profileNotes) {
        checkoutDraft = checkoutDraft.syncProfileLocation(previousProfileAddress, previousProfileNotes,
            profileAddress, profileNotes)
        previousProfileAddress = profileAddress
        previousProfileNotes = profileNotes
    }
    LaunchedEffect(vm.completedCheckoutOrderId) {
        if (vm.completedCheckoutOrderId != null) {
            val paid = vm.receipt(vm.completedCheckoutOrderId)?.isPaid == true
            nav.navigate("home") {
                popUpTo("home") { inclusive = false }
                launchSingleTop = true
            }
            checkoutDraft = checkoutDraft.copy(weightInput = "", promoCode = "", clothingNotes = "", hypoallergenic = false)
            vm.consumeCompletedCheckout()
            feedback(if (paid) "Pembayaran berhasil. Pesanan Anda sudah dibuat."
                else "Pesanan dibuat. Pembayaran dilakukan di outlet.")
        }
    }
    Surface(Modifier.fillMaxSize(), color = WashBackground) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Scaffold(Modifier.widthIn(max = 600.dp).fillMaxSize(), containerColor = WashBackground,
                topBar = {
                    if (route in listOf("choose_service", "confirm_payment")) CheckoutHeader(
                        if (route == "choose_service") "Pilih Layanan" else "Konfirmasi Pembayaran",
                        { if (!vm.isSubmittingCheckout) nav.popBackStack() },
                        { if (!vm.isSubmittingCheckout) topNavigate("profile") })
                    else if (!auth) WashHeader(when (route) {
                        "home" -> "HOME"; "orders" -> "PESANAN SAYA"; "history" -> "RIWAYAT PESANAN"
                        "profile" -> "PROFIL PELANGGAN"; "location" -> "LOKASI LAUNDRY"
                        "pickup" -> "PENJEMPUTAN"
                        "receipts", "receipts/{orderId}" -> "NOTA DIGITAL"
                        "notifications" -> "NOTIFIKASI"; else -> "ORDER TRACKING"
                    }, vm.unreadCount, { nav.navigate("notifications") { launchSingleTop = true } }, { topNavigate("profile") },
                        if (route in listOf("location", "notifications", "orders/{orderId}", "pickup", "receipts", "receipts/{orderId}")) ({ nav.popBackStack() }) else null)
                },
                bottomBar = { if (!auth && !checkout) WashBottomBar(when (route) {
                    "orders/{orderId}", "receipts/{orderId}" -> "orders"
                    "receipts" -> "home"
                    else -> route
                }, ::topNavigate) },
                snackbarHost = { SnackbarHost(snackbar) }) { padding ->
                NavHost(nav, startDestination, modifier = Modifier.padding(padding).imePadding()) {
                    composable("splash") {
                        LaunchedEffect(Unit) { delay(500); nav.navigate(if (vm.signedIn) "home" else "login") { popUpTo("splash") { inclusive = true } } }
                        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            WashLogo(72); Spacer(Modifier.height(16.dp)); Text("MAXWASH", style = MaterialTheme.typography.headlineLarge)
                            Text("Laundry Pintar & Praktis", color = WashMuted)
                        }
                    }
                    composable("login") { LoginScreen(vm, ::enterHome, { nav.navigate("register") }) }
                    composable("register") { RegisterScreen(vm, ::enterHome, { nav.popBackStack() }) }
                    composable("home") { HomeScreen(vm, ::details, ::topNavigate, { nav.navigate("location") }, feedback) }
                    composable("pickup") {
                        PickupScreen(checkoutDraft, vm.outlets, { checkoutDraft = it },
                            { nav.navigate("choose_service") { launchSingleTop = true } }, feedback)
                    }
                    composable("choose_service") {
                        ChooseServiceScreen(checkoutDraft, vm.services, vm.fragrances, { checkoutDraft = it },
                            { nav.navigate("confirm_payment") { launchSingleTop = true } })
                    }
                    composable("confirm_payment") {
                        ConfirmPaymentScreen(checkoutDraft, vm.services.find { it.id == checkoutDraft.serviceId },
                            vm.outlet(checkoutDraft.outletId), { checkoutDraft = it }, vm.isSubmittingCheckout,
                            { vm.submitCheckout(checkoutDraft) })
                    }
                    composable("orders") { OrdersScreen(vm, ::details, ::receipt) }
                    composable("history") { OrdersScreen(vm, ::details, ::receipt) }
                    composable("orders/{orderId}", arguments = listOf(navArgument("orderId") { type = NavType.StringType })) { backStack ->
                        val id = backStack.arguments?.getString("orderId")
                        OrderDetailsScreen(vm, id, {
                            vm.order(id)?.let { vm.selectOutlet(it.outletId) }
                            nav.navigate("location")
                        }, ::receipt)
                    }
                    composable("receipts") { DigitalReceiptsScreen(vm, onAllReceipts = { topNavigate("receipts") }, feedback = feedback) }
                    composable("receipts/{orderId}", arguments = listOf(navArgument("orderId") { type = NavType.StringType })) { backStack ->
                        DigitalReceiptsScreen(vm, backStack.arguments?.getString("orderId"), { topNavigate("receipts") }, feedback)
                    }
                    composable("location") { LocationScreen(vm, feedback) }
                    composable("profile") { ProfileScreen(vm, ::logout, { nav.navigate("location") }, feedback) }
                    composable("notifications") { NotificationsScreen(vm, ::details) }
                }
            }
        }
    }
}
