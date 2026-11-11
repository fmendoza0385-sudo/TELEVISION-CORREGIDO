package com.grmconexion.lukka

import com.grmconexion.lukka.subscription.LicensePayload
import com.grmconexion.lukka.subscription.LicenseVerifier
import com.grmconexion.lukka.subscription.SubscriptionState
import com.grmconexion.lukka.subscription.SubscriptionValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.spec.ECGenParameterSpec

class SubscriptionValidatorTest {
    private val validator = SubscriptionValidator("")
    private val day = 24L * 3600 * 1000
    private val now = 1_800_000_000_000L

    private fun payload(
        result: String = "VALID", device: String = "dev1", from: Long = now - 10 * day,
        until: Long = now + 20 * day, schema: Int = 1
    ) = LicensePayload(schema, result, "80105001", "lic1", "PLAN-50", 1, from, until, device, now)

    @Test fun validLicense_isActive() =
        assertEquals(SubscriptionState.ACTIVA, validator.evaluate(payload(), "dev1", now))

    @Test fun closeToCutoff_isNearExpiry_andStillPlays() {
        val s = validator.evaluate(payload(until = now + 2 * day), "dev1", now)
        assertEquals(SubscriptionState.POR_VENCER, s)
        assertTrue(s.allowsPlayback)
    }

    @Test fun pastCutoff_isBlocked() {
        val s = validator.evaluate(payload(until = now - 1), "dev1", now)
        assertEquals(SubscriptionState.VENCIDA, s)
        assertFalse(s.allowsPlayback)
    }

    @Test fun otherDevice_isRejected() =
        assertEquals(SubscriptionState.DISPOSITIVO_NO_AUTORIZADO, validator.evaluate(payload(), "otro", now))

    @Test fun nonValidResult_neverPlays() {
        val s = validator.evaluate(payload(result = "REJECTED"), "dev1", now)
        assertFalse(s.allowsPlayback)
    }

    @Test fun unknownSchema_neverPlays() {
        assertFalse(validator.evaluate(payload(schema = 99), "dev1", now).allowsPlayback)
    }

    @Test fun incoherentDates_neverPlay() {
        assertFalse(validator.evaluate(payload(from = now + day, until = now), "dev1", now).allowsPlayback)
    }

    @Test fun payloadParsing_rejectsIncompleteJson() {
        assertNull(LicensePayload.parse("""{"schema_version":1,"result":"VALID"}"""))
        assertNotNull(
            LicensePayload.parse(
                """{"schema_version":1,"result":"VALID","user_id":"1","license_id":"a","plan_id":"PLAN-50",
                   "renewal_number":1,"valid_from_ms":1,"valid_until_ms":2,"device_id":"d","server_time_ms":1}"""
            )
        )
    }

    @Test fun signature_acceptsGenuine_rejectsTampered() {
        val kp = KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1")) }.generateKeyPair()
        val data = """{"result":"VALID"}""".toByteArray()
        val sig = Signature.getInstance("SHA256withECDSA").run { initSign(kp.private); update(data); sign() }
        assertTrue(LicenseVerifier.verify(data, sig, kp.public.encoded))
        assertFalse(LicenseVerifier.verify("""{"result":"VALID "}""".toByteArray(), sig, kp.public.encoded))
        assertFalse(LicenseVerifier.verify(data, sig, ByteArray(10)))
    }
}
