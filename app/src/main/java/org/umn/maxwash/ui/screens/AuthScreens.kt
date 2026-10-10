package org.umn.maxwash.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.umn.maxwash.data.FormValidation
import org.umn.maxwash.ui.MaxwashViewModel
import org.umn.maxwash.ui.components.*
import org.umn.maxwash.ui.theme.*

@Composable
private fun AuthIntroduction(title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(80.dp).background(WashLavender, CircleShape), contentAlignment = Alignment.Center) { WashLogo(58) }
        Text(title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Text(subtitle, color = WashMuted, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
    }
}

@Composable
fun LoginScreen(vm: MaxwashViewModel, onSuccess: () -> Unit, onRegister: () -> Unit) {
    var identifier by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var usePhone by rememberSaveable { mutableStateOf(false) }
    var touched by rememberSaveable { mutableIntStateOf(0) }
    var loginError by rememberSaveable { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val identifierError = if (usePhone) FormValidation.phone(identifier) else FormValidation.email(identifier)
    val passwordError = FormValidation.password(password)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)) {
        AuthIntroduction("Masuk ke MAXWASH", "Kelola & pantau cucian Anda\nlangsung dari genggaman.")
        WashCard {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(!usePhone, { usePhone = false; identifier = ""; touched = 0; loginError = null }, label = { Text("Email") })
                FilterChip(usePhone, { usePhone = true; identifier = ""; touched = 0; loginError = null }, label = { Text("Nomor HP") })
            }
            WashField(identifier, { identifier = if (usePhone) it.filter { c -> c in '0'..'9' } else it; touched = touched or 1; loginError = null },
                if (usePhone) "Nomor Handphone" else "Email", "login_identifier",
                if (usePhone) KeyboardType.Number else KeyboardType.Email,
                if (touched and 1 != 0) identifierError else null, enabled = !busy)
            WashField(password, { password = it; touched = touched or 2; loginError = null }, "Password", "login_password", KeyboardType.Password,
                if (touched and 2 != 0) passwordError else null, password = true, enabled = !busy)
            if (loginError != null) Text(loginError!!, color = WashError, modifier = Modifier.testTag("login_error"), style = MaterialTheme.typography.bodySmall)
            PrimaryButton(if (busy) "Memeriksa akun…" else "Masuk Sekarang →", {
                busy = true
                scope.launch {
                    delay(350)
                    if (vm.login(identifier, password)) onSuccess() else loginError = "Email/nomor HP atau password tidak cocok."
                    busy = false
                }
            }, Modifier.testTag("login_submit"), identifierError == null && passwordError == null && !busy)
            Text("Akses akun pelanggan · Demo lokal", style = MaterialTheme.typography.bodySmall, color = WashMuted)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Text("Belum memiliki akun?", style = MaterialTheme.typography.bodySmall)
            TextButton(onRegister, modifier = Modifier.testTag("open_register")) { Text("Daftar Akun Baru") }
        }
        OutlinedButton({ vm.loginDemo(); onSuccess() }, Modifier.fillMaxWidth().testTag("demo_login"), shape = MaterialTheme.shapes.extraLarge) { Text("Coba akun demo") }
        Text("Laundry lebih bersih, hemat air, dan higienis.", Modifier.fillMaxWidth(), color = WashMuted,
            style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
    }
}

@Composable
fun RegisterScreen(vm: MaxwashViewModel, onSuccess: () -> Unit, onBack: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var touched by rememberSaveable { mutableIntStateOf(0) }
    var numericError by rememberSaveable { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    fun error(bit: Int, value: String?) = if (touched and bit != 0) value else null
    val valid = FormValidation.registration(name, phone, email, password, confirm) && numericError == null
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TextButton(onBack, Modifier.testTag("register_back")) { Text("← Kembali ke Login") }
        AuthIntroduction("Daftar Akun MAXWASH", "Satu akun untuk semua informasi cucian Anda.")
        WashCard {
            WashField(name, { name = it; touched = touched or 1 }, "Nama Lengkap", "register_name", error = error(1, FormValidation.name(name)), enabled = !busy)
            WashField(phone, {
                numericError = if (it.any { c -> c !in '0'..'9' }) "Nomor handphone hanya boleh berisi angka" else null
                phone = it.filter { c -> c in '0'..'9' }; touched = touched or 2
            }, "Nomor Handphone", "register_phone", KeyboardType.Number, numericError ?: error(2, FormValidation.phone(phone)), enabled = !busy)
            WashField(email, { email = it; touched = touched or 4 }, "Email", "register_email", KeyboardType.Email, error(4, FormValidation.email(email)), enabled = !busy)
            WashField(password, { password = it; touched = touched or 8 }, "Password", "register_password", KeyboardType.Password,
                error(8, FormValidation.password(password)), password = true, enabled = !busy)
            WashField(confirm, { confirm = it; touched = touched or 16 }, "Konfirmasi Password", "register_confirm", KeyboardType.Password,
                error(16, FormValidation.confirmation(confirm, password)), password = true, enabled = !busy)
            PrimaryButton(if (busy) "Membuat akun…" else "Daftar Sekarang →", {
                busy = true
                scope.launch {
                    delay(400)
                    if (vm.register(name, phone, email, password, confirm)) onSuccess()
                    busy = false
                }
            }, Modifier.testTag("register_submit"), enabled = valid && !busy)
            Text("Password minimal 8 karakter. Nomor HP menggunakan awalan 08 atau 62.", style = MaterialTheme.typography.bodySmall, color = WashMuted)
        }
        Text("Akun untuk simulasi MAXWASH. Data tersimpan selama sesi aplikasi.", Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodySmall, color = WashMuted, textAlign = TextAlign.Center)
    }
}
