package com.grmconexion.lukka

import com.grmconexion.lukka.subscription.SubscriptionPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class SubscriptionPolicyTest {
    private val bogota = ZoneId.of("America/Bogota")
    private fun ms(y: Int, m: Int, d: Int, h: Int, mi: Int = 0, s: Int = 0): Long =
        ZonedDateTime.of(y, m, d, h, mi, s, 0, bogota).toInstant().toEpochMilli()

    @Test fun midMonth_goesToDay5OfNextMonthAtNoon() {
        assertEquals(ms(2026, 12, 5, 12), SubscriptionPolicy.nextCutoffAfter(ms(2026, 11, 11, 16, 9)))
    }

    @Test fun beforeNoonOnDay5_cutsThatSameDay() {
        assertEquals(ms(2026, 11, 5, 12), SubscriptionPolicy.nextCutoffAfter(ms(2026, 11, 5, 11, 59, 59)))
    }

    @Test fun exactlyAtCutoff_movesToNextMonth() {
        assertEquals(ms(2026, 12, 5, 12), SubscriptionPolicy.nextCutoffAfter(ms(2026, 11, 5, 12)))
    }

    @Test fun december_wrapsToJanuary() {
        assertEquals(ms(2027, 1, 5, 12), SubscriptionPolicy.nextCutoffAfter(ms(2026, 12, 20, 8)))
    }

    @Test fun expiryIsInclusiveAtCutoff_noGrace() {
        val until = ms(2026, 12, 5, 12)
        assertFalse(SubscriptionPolicy.isExpired(until - 1, until))
        assertTrue(SubscriptionPolicy.isExpired(until, until))
    }

    @Test fun nearExpiryWindow() {
        val until = ms(2026, 12, 5, 12)
        assertTrue(SubscriptionPolicy.isNearExpiry(until - 2L * 24 * 3600 * 1000, until))
        assertFalse(SubscriptionPolicy.isNearExpiry(until - 10L * 24 * 3600 * 1000, until))
    }
}
