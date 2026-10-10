package org.umn.maxwash.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import org.umn.maxwash.data.*

class MaxwashViewModel : ViewModel() {
    var customer by mutableStateOf(MockRepository.customer)
        private set
    private var accountPassword = MockRepository.demoPassword
    var signedIn by mutableStateOf(false)
        private set
    var filter by mutableStateOf(OrderFilter.ALL)
    var search by mutableStateOf("")
    var selectedOutletId by mutableStateOf("OUT-001")
    var locationVersion by mutableStateOf(0)
        private set
    var readNotificationIds by mutableStateOf(MockRepository.notifications.filter { it.initiallyRead }.map { it.id }.toSet())
        private set
    val visibleOrders get() = MockRepository.filterOrders(filter, search)
    val unreadCount get() = MockRepository.notifications.count { it.id !in readNotificationIds }

    fun register(name: String, phone: String, email: String, password: String, confirmation: String): Boolean {
        if (!FormValidation.registration(name, phone, email, password, confirmation)) return false
        customer = MockRepository.customer.copy(name = name.trim(), phone = phone, email = email.trim())
        accountPassword = password
        signedIn = true
        return true
    }
    fun login(identifier: String, password: String): Boolean {
        signedIn = (identifier.trim().equals(customer.email, true) || identifier.trim() == customer.phone) && password == accountPassword
        return signedIn
    }
    fun loginDemo() { customer = MockRepository.customer; accountPassword = MockRepository.demoPassword; signedIn = true }
    fun updateProfile(profile: Customer): Boolean {
        if (FormValidation.name(profile.name) != null || FormValidation.phone(profile.phone) != null || FormValidation.email(profile.email) != null || profile.address.isBlank()) return false
        customer = profile.copy(name = profile.name.trim(), email = profile.email.trim(), address = profile.address.trim())
        return true
    }
    fun refreshLocation() { locationVersion = (locationVersion + 1) % 3 }
    fun distanceKm() = MockRepository.outlet(selectedOutletId).distanceKm + locationVersion * 0.1
    fun markRead(id: String) { readNotificationIds = readNotificationIds + id }
    fun markAllRead() { readNotificationIds = MockRepository.notifications.map { it.id }.toSet() }
    fun logout() { signedIn = false; filter = OrderFilter.ALL; search = "" }
}
