package org.umn.maxwash.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.umn.maxwash.data.*
import org.umn.maxwash.MaxwashApplication

class MaxwashViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as MaxwashApplication).repository
    private var data by mutableStateOf(LocalData())
    var isLoading by mutableStateOf(true)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var filter by mutableStateOf(OrderFilter.ALL)
    var search by mutableStateOf("")
    val customer get() = data.customer
    val signedIn get() = customer != null
    val orders get() = data.orders
    val outlets get() = data.outlets
    val services get() = data.services
    val fragrances get() = data.fragrances
    val promotions get() = data.promotions
    val notifications get() = data.notifications
    val selectedOutletId get() = data.selectedOutletId
    val visibleOrders get() = orders.filterOrders(filter, search)
    val monthOrders get() = orders.filter { LocalTimeFormat.isThisMonth(it.createdAt) }
    val readNotificationIds get() = notifications.filter { it.isRead }.map { it.id }.toSet()
    val unreadCount get() = notifications.count { !it.isRead }

    init { load() }

    fun retryLoading() { if (!isLoading) load() }

    private fun load() {
        isLoading = true
        errorMessage = null
        viewModelScope.launch {
            try {
                repository.initialize()
                repository.observeData().collect { data = it; isLoading = false }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                errorMessage = "Data lokal gagal dimuat. Silakan coba lagi."
                isLoading = false
            }
        }
    }

    fun order(id: String?) = orders.find { it.id == id }
    fun outlet(id: String) = outlets.find { it.id == id }
    fun distanceKm() = outlet(selectedOutletId)?.distanceKm
    fun clearError() { errorMessage = null }

    suspend fun register(name: String, phone: String, email: String, password: String, confirmation: String) = write {
        repository.register(name, phone, email, password, confirmation)
    }

    suspend fun login(identifier: String, password: String) = write {
        require(repository.login(identifier, password)) { "Email/nomor HP atau password tidak cocok." }
    }

    suspend fun loginDemo() = write { repository.loginDemo() }
    suspend fun updateProfile(profile: Customer) = write { repository.updateProfile(profile) }
    suspend fun logout(): Boolean = write { repository.logout() }.also {
        if (it) { filter = OrderFilter.ALL; search = "" }
    }

    fun selectOutlet(id: String) { viewModelScope.launch { write { repository.selectOutlet(id) } } }
    fun refreshLocation() { viewModelScope.launch { write { /* Reload locally stored outlet data. */ } } }
    fun markRead(id: String) { viewModelScope.launch { write { repository.markRead(id) } } }
    fun markAllRead() { viewModelScope.launch { write { repository.markAllRead() } } }

    suspend fun createOrder(serviceId: String, weightKg: Double, outletId: String): String? {
        var id: String? = null
        return if (write { id = repository.createOrder(serviceId, weightKg, outletId) }) id else null
    }

    private suspend fun write(action: suspend () -> Unit): Boolean {
        errorMessage = null
        return try {
            action()
            data = repository.readData()
            true
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            errorMessage = if (error is IllegalArgumentException) error.message else "Perubahan gagal disimpan. Silakan coba lagi."
            false
        }
    }
}
