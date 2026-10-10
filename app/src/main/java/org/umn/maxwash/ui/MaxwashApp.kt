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
    val feedback: (String) -> Unit = { message -> scope.launch { snackbar.showSnackbar(message) } }
    fun topNavigate(destination: String) {
        nav.navigate(destination) {
            popUpTo("home") { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    fun enterHome() { nav.navigate("home") { popUpTo(nav.graph.id) { inclusive = true }; launchSingleTop = true } }
    fun logout() { vm.logout(); nav.navigate("login") { popUpTo(nav.graph.id) { inclusive = true }; launchSingleTop = true } }
    fun details(id: String) { nav.navigate("orders/$id") }
    LaunchedEffect(route, vm.signedIn) {
        // A process restart resets this local prototype account. Never restore a protected screen without a session.
        if (!auth && !vm.signedIn) nav.navigate("login") { popUpTo(nav.graph.id) { inclusive = true } }
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
                        LaunchedEffect(Unit) { delay(500); nav.navigate("login") { popUpTo("splash") { inclusive = true } } }
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
                        OrderDetailsScreen(id, {
                            org.umn.maxwash.data.MockRepository.order(id)?.let { vm.selectedOutletId = it.outletId }
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
