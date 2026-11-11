package com.grmconexion.lukka.subscription

import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

/** Verifica una firma ECDSA P-256 / SHA-256 (formato DER) con la CLAVE PÚBLICA del servidor. */
object LicenseVerifier {
    fun verify(payload: ByteArray, signatureDer: ByteArray, publicKeyX509Der: ByteArray): Boolean = try {
        val key = KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(publicKeyX509Der))
        val sig = Signature.getInstance("SHA256withECDSA")
        sig.initVerify(key)
        sig.update(payload)
        sig.verify(signatureDer)
    } catch (e: Exception) {
        false
    }
}
