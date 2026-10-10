package org.umn.maxwash

import org.junit.Assert.*
import org.junit.Test
import org.umn.maxwash.data.*
import org.umn.maxwash.ui.MaxwashViewModel

class MaxwashUnitTest {
    @Test fun requiredFieldsCannotBeEmpty() {
        assertFalse(FormValidation.registration("", "", "", "", ""))
        assertNotNull(FormValidation.name(" "))
        assertNotNull(FormValidation.phone(""))
        assertNotNull(FormValidation.email(""))
        assertNotNull(FormValidation.password(""))
        assertNotNull(FormValidation.confirmation("", ""))
    }
    @Test fun phonesRejectLettersSpacesUnicodeAndWrongLengths() {
        listOf("0812abc7890", "0812 3456789", "０８１２３４５６７８９０", "081", "081234567890123", "991234567890").forEach { assertNotNull(it, FormValidation.phone(it)) }
        assertNull(FormValidation.phone("081234567890"))
        assertNull(FormValidation.phone("6281234567890"))
    }
    @Test fun emailFormatRejectsMalformedAddresses() {
        listOf("andi", "andi@", "@example.com", "a b@example.com", "a@example", "a@-example.com", "a@example..com", ".andi@example.com", "andi.@example.com", "a..b@example.com").forEach { assertNotNull(it, FormValidation.email(it)) }
        assertNull(FormValidation.email("andi.pratama+laundry@example.com"))
    }
    @Test fun passwordsAndConfirmationMustBeValid() {
        assertNotNull(FormValidation.password("1234567"))
        assertNotNull(FormValidation.confirmation("different", "Maxwash123"))
        assertNull(FormValidation.confirmation("Maxwash123", "Maxwash123"))
    }
    @Test fun validRegistrationAccepted() {
        assertTrue(FormValidation.registration("Andi Pratama", "081234567890", "andi@example.com", "Maxwash123", "Maxwash123"))
    }
    @Test fun activeAndCompletedOrdersAreDisjointAndComplete() {
        val active = MockRepository.filterOrders(OrderFilter.ACTIVE, "")
        val completed = MockRepository.filterOrders(OrderFilter.COMPLETED, "")
        assertEquals(5, active.size); assertEquals(1, completed.size)
        assertTrue(active.none { it.isCompleted }); assertTrue(completed.all { it.isCompleted })
        assertEquals(MockRepository.orders.toSet(), (active + completed).toSet())
        assertTrue(active.any { it.status == LaundryStatus.READY })
    }
    @Test fun searchWorksByIdAndServiceAndCanBeEmpty() {
        assertEquals("MW-1002", MockRepository.filterOrders(OrderFilter.ALL, " mw-1002 ").single().id)
        assertEquals(2, MockRepository.filterOrders(OrderFilter.ACTIVE, "wash & fold").size)
        assertTrue(MockRepository.filterOrders(OrderFilter.COMPLETED, "MW-1001").isEmpty())
        assertTrue(MockRepository.filterOrders(OrderFilter.ALL, "unknown").isEmpty())
    }
    @Test fun detailLookupUsesIdAndUnknownIdHasNoFallback() {
        assertNotEquals(MockRepository.order("MW-1001"), MockRepository.order("MW-1002"))
        assertEquals(LaundryStatus.READY, MockRepository.order("MW-1002")!!.status)
        assertNull(MockRepository.order("MW-9999")); assertNull(MockRepository.order(null))
    }
    @Test fun allSixStatusesHaveConsistentHistoryAndProgress() {
        assertEquals(LaundryStatus.entries.toSet(), MockRepository.orders.map { it.status }.toSet())
        MockRepository.orders.forEach { order ->
            assertEquals(order.status, order.history.last().status)
            assertEquals(LaundryStatus.entries.take(order.status.ordinal + 1), order.history.map { it.status })
            assertTrue(order.progress > 0f && order.progress <= 1f)
            assertEquals(order, MockRepository.order(order.id))
        }
        assertEquals(1f, MockRepository.order("MW-1003")!!.progress)
    }
    @Test fun allNotificationsAndOutletsResolve() {
        assertEquals(MockRepository.orders.size, MockRepository.orders.map { it.id }.toSet().size)
        MockRepository.notifications.forEach { assertNotNull(MockRepository.order(it.orderId)) }
        MockRepository.orders.forEach { assertEquals(it.outletId, MockRepository.outlet(it.outletId).id) }
    }
    @Test fun readStateUpdatesAndIsIdempotent() {
        val vm = MaxwashViewModel(); assertEquals(2, vm.unreadCount)
        vm.markRead("N-001"); assertEquals(1, vm.unreadCount)
        vm.markRead("N-001"); assertEquals(1, vm.unreadCount)
        vm.markAllRead(); assertEquals(0, vm.unreadCount)
    }
    @Test fun editedProfileIsValidatedAndUsedForLogin() {
        val vm = MaxwashViewModel()
        assertFalse(vm.updateProfile(vm.customer.copy(phone = "letters")))
        assertEquals(MockRepository.customer, vm.customer)
        assertTrue(vm.updateProfile(vm.customer.copy(name = "Budi Santoso", email = "budi@example.com")))
        assertEquals("Budi Santoso", vm.customer.name)
        assertTrue(vm.login("budi@example.com", MockRepository.demoPassword))
        assertFalse(vm.login("andi@example.com", MockRepository.demoPassword))
    }
    @Test fun registrationLoginLogoutFlowUsesLocalAccount() {
        val vm = MaxwashViewModel()
        assertFalse(vm.register("", "", "", "", ""))
        assertFalse(vm.signedIn)
        assertTrue(vm.register("Budi Santoso", "081234567891", "budi@example.com", "BudiPass123", "BudiPass123"))
        assertTrue(vm.signedIn); vm.logout(); assertFalse(vm.signedIn)
        assertTrue(vm.login("081234567891", "BudiPass123"))
        assertFalse(vm.login("budi@example.com", "wrongpass"))
    }
    @Test fun locationRefreshAndSelectionChangeDistance() {
        val vm = MaxwashViewModel(); val start = vm.distanceKm()
        vm.refreshLocation(); assertTrue(vm.distanceKm() > start)
        vm.selectedOutletId = "OUT-002"; assertTrue(vm.distanceKm() > 2.5)
    }
}
