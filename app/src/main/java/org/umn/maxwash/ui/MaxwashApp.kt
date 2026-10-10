package org.umn.maxwash.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

@Composable
fun MaxwashApp(vm: MaxwashViewModel = viewModel(), startDestination: String = "splash") {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: startDestination
    val auth = route in listOf("splash", "login", "register")
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
    Surface(Modifier.fillMaxSize(), color = WashBackground) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Scaffold(Modifier.widthIn(max = 600.dp).fillMaxSize(), containerColor = WashBackground,
                topBar = {
                    if (!auth) WashHeader(when (route) {
                        "home" -> "HOME"; "orders" -> "PESANAN SAYA"; "history" -> "RIWAYAT PESANAN"
                        "profile" -> "PROFILE & QR CODE"; "location" -> "LOKASI LAUNDRY"
                        "notifications" -> "NOTIFIKASI"; else -> "ORDER TRACKING"
                    }, vm.unreadCount, { nav.navigate("notifications") { launchSingleTop = true } }, { topNavigate("profile") },
                        if (route in listOf("location", "notifications", "orders/{orderId}")) ({ nav.popBackStack() }) else null)
                },
                bottomBar = { if (!auth) WashBottomBar(if (route == "orders/{orderId}") "orders" else route, ::topNavigate) },
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
                    composable("orders") { OrdersScreen(vm, ::details, feedback) }
                    composable("history") { OrdersScreen(vm, ::details, feedback) }
                    composable("orders/{orderId}", arguments = listOf(navArgument("orderId") { type = NavType.StringType })) { backStack ->
                        val id = backStack.arguments?.getString("orderId")
                        OrderDetailsScreen(vm, id, {
                            vm.order(id)?.let { vm.selectOutlet(it.outletId) }
                            nav.navigate("location")
                        }, feedback)
                    }
                    composable("location") { LocationScreen(vm, feedback) }
                    composable("profile") { ProfileScreen(vm, ::logout, { nav.navigate("location") }, feedback) }
                    composable("notifications") { NotificationsScreen(vm, ::details) }
                }
            }
        }
    }
}
