package org.umn.maxwash

import org.junit.Assert.*
import org.junit.Test
import org.umn.maxwash.data.*


class MaxwashUnitTest {
    @Test fun mapCoordinatesRejectInvalidValuesAndProduceNonemptyBoundsAtWorldEdges() {
        listOf(Double.NaN to 0.0, Double.POSITIVE_INFINITY to 0.0, 91.0 to 0.0, 0.0 to 181.0).forEach { (lat, lon) ->
            assertTrue(runCatching { MapPoint(lat, lon) }.isFailure)
        }
        listOf(MapPoint(90.0, 180.0), MapPoint(-90.0, -180.0)).forEach { point ->
            val bounds = java.net.URI(point.embeddedMapUrl()).query.substringAfter("bbox=").substringBefore('&').split(',').map(String::toDouble)
            assertTrue(bounds[0] < bounds[2])
            assertTrue(bounds[1] < bounds[3])
            assertTrue(bounds[0] >= -180.0 && bounds[2] <= 180.0)
        }
    }

    @Test fun mapUrlsKeepCoordinatesInDecimalFormatAndOverviewHasNoAssumedPin() {
        val previousLocale = java.util.Locale.getDefault()
        try {
            java.util.Locale.setDefault(java.util.Locale.forLanguageTag("id-ID"))
            val url = MapPoint(-6.23, 106.81).embeddedMapUrl()
            assertEquals("appassets.androidplatform.net", java.net.URI(url).host)
            assertTrue(url.endsWith("&marker=-6.23,106.81"))
            assertFalse(INDONESIA_MAP_URL.contains("marker="))
        } finally { java.util.Locale.setDefault(previousLocale) }
    }

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

    @Test fun emptyPickupDraftUsesSavedProfileLocationWithoutResettingServiceChoices() {
        val draft = CheckoutDraft(outletId = "outlet", serviceId = "service", weightInput = "4.8",
            fragrance = "Ocean Breeze", hypoallergenic = true, payment = PaymentMethod.AT_OUTLET)
        assertEquals(draft.copy(address = "Jl. Profil No. 12", addressNotes = "Pagar biru"),
            draft.syncProfileLocation("", "", "Jl. Profil No. 12", "Pagar biru"))
    }

    @Test fun inheritedPickupLocationFollowsProfileChangesAndRemoval() {
        val draft = CheckoutDraft(address = "Jl. Lama", addressNotes = "Patokan lama", outletId = "outlet")
        val updated = draft.syncProfileLocation("Jl. Lama", "Patokan lama", "Jl. Baru", "Patokan baru")
        assertEquals(draft.copy(address = "Jl. Baru", addressNotes = "Patokan baru"), updated)
        val cleared = updated.syncProfileLocation("Jl. Baru", "Patokan baru", "", "")
        assertEquals("", cleared.address)
        assertFalse(cleared.locationReady)
    }

    @Test fun profileChangesPreserveCustomPickupAddressAndExplicitlyClearedNotes() {
        val draft = CheckoutDraft(address = "Jl. Kantor", addressNotes = "", outletId = "outlet")
        assertEquals(draft, draft.syncProfileLocation("Jl. Rumah Lama", "Catatan lama", "Jl. Rumah Baru", "Catatan baru"))
        val addressOnly = draft.copy(address = "Jl. Rumah Lama")
            .syncProfileLocation("Jl. Rumah Lama", "Catatan lama", "Jl. Rumah Baru", "Catatan baru")
        assertEquals("Jl. Rumah Baru", addressOnly.address)
        assertEquals("", addressOnly.addressNotes)
    }

    @Test fun checkoutQuoteUsesSelectedRateWeightAndOptionalDetergent() {
        val service = LaundryService("SVC-002", "Wash & Iron", 10000, 24)
        val draft = CheckoutDraft(weightInput = "4,8", hypoallergenic = true)
        assertEquals(4.8, draft.weightKg!!, 0.001)
        assertEquals(CheckoutQuote(48000, 2000, 0), draft.quote(service))
        assertEquals(50000, draft.quote(service).total)
        assertEquals(48000, draft.copy(hypoallergenic = false).quote(service).total)
    }

    @Test fun checkoutPromoCannotMakeTheBillNegativeOrApplyUnknownCode() {
        val service = LaundryService("service", "Service", 10000, 24)
        val draft = CheckoutDraft(weightInput = "0.1", promoCode = " newclean5k ")
        assertEquals(CheckoutQuote(1000, 0, 1000), draft.quote(service))
        assertEquals(0, draft.quote(service).total)
        assertEquals(1000, draft.copy(promoCode = "unknown").quote(service).total)
        assertEquals(0, draft.quote(null).total)
    }

    @Test fun checkoutRejectsInvalidWeightsAndRequiresAddressOnlyForPickup() {
        listOf("", "0", "-1", "101", "NaN", "Infinity", "text").forEach {
            assertNull(it, CheckoutDraft(weightInput = it).weightKg)
        }
        assertFalse(CheckoutDraft(outletId = "outlet").locationReady)
        assertTrue(CheckoutDraft(outletId = "outlet", address = "Jl. Pengujian").locationReady)
        assertTrue(CheckoutDraft(outletId = "outlet", collection = CollectionMethod.DROP_OFF).locationReady)
        assertFalse(CheckoutDraft(collection = CollectionMethod.DROP_OFF).locationReady)
    }
}
