package org.umn.maxwash

import org.junit.Assert.*
import org.junit.Test
import org.umn.maxwash.data.*


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
    private fun order(id: String, service: String, status: LaundryStatus, timestamp: Long = 0L) =
        LaundryOrder(id, service, 2.0, "", "outlet", status, 14000, "",
            listOf(StatusEvent(status, "")), timestamp, 24)

    @Test fun filtersUseSuppliedOrdersAndDoNotInventDemoRecords() {
        val orders = listOf(order("A", "Wash & Fold", LaundryStatus.PROCESSING),
            order("B", "Wash & Iron", LaundryStatus.COLLECTED),
            order("C", "Iron Only", LaundryStatus.READY))
        assertEquals(2, orders.filterOrders(OrderFilter.ACTIVE, "").size)
        assertEquals(listOf(orders[1]), orders.filterOrders(OrderFilter.COMPLETED, ""))
        assertEquals(orders, orders.filterOrders(OrderFilter.ALL, ""))
        assertTrue(emptyList<LaundryOrder>().filterOrders(OrderFilter.ALL, "").isEmpty())
    }

    @Test fun searchTrimsQueryAndMatchesIdOrServiceIgnoringCase() {
        val orders = listOf(order("LOCAL-123", "Wash & Fold", LaundryStatus.RECEIVED))
        assertEquals(orders, orders.filterOrders(OrderFilter.ALL, " local-123 "))
        assertEquals(orders, orders.filterOrders(OrderFilter.ACTIVE, "WASH & FOLD"))
        assertTrue(orders.filterOrders(OrderFilter.ALL, "missing").isEmpty())
        assertTrue(orders.filterOrders(OrderFilter.COMPLETED, "LOCAL-123").isEmpty())
    }

    @Test fun allStatusesHaveValidProgressAndOnlyCollectedIsCompleted() {
        LaundryStatus.entries.forEach {
            val order = order("id", "service", it)
            assertTrue(order.progress > 0f && order.progress <= 1f)
            assertEquals(it == LaundryStatus.COLLECTED, order.isCompleted)
        }
        assertEquals(1f, order("id", "service", LaundryStatus.COLLECTED).progress)
    }

    @Test fun passwordsUseRandomSaltsAndVerifyWithoutStoringPlaintext() {
        val first = PasswordHasher.hash("LocalPass123")
        val second = PasswordHasher.hash("LocalPass123")
        assertNotEquals(first.salt, second.salt)
        assertNotEquals(first.value, second.value)
        assertNotEquals("LocalPass123", first.value)
        assertTrue(PasswordHasher.verify("LocalPass123", first.value, first.salt))
        assertFalse(PasswordHasher.verify("WrongPass123", first.value, first.salt))
    }

    @Test fun monthlySummaryUsesJakartaMonthAtUtcBoundary() {
        val now = java.time.Instant.parse("2026-10-01T00:00:00Z").toEpochMilli()
        val octoberInJakarta = java.time.Instant.parse("2026-09-30T18:00:00Z").toEpochMilli()
        val septemberInJakarta = java.time.Instant.parse("2026-09-30T16:00:00Z").toEpochMilli()
        assertTrue(LocalTimeFormat.isThisMonth(octoberInJakarta, now))
        assertFalse(LocalTimeFormat.isThisMonth(septemberInJakarta, now))
    }
}
