package org.umn.maxwash.data

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PasswordHasher {
    private const val ITERATIONS = 120_000
    data class Hash(val value: String, val salt: String)

    fun hash(password: String): Hash {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        return Hash(derive(password, salt), Base64.getEncoder().encodeToString(salt))
    }

    fun verify(password: String, hash: String, salt: String): Boolean =
        MessageDigest.isEqual(derive(password, Base64.getDecoder().decode(salt)).toByteArray(), hash.toByteArray())

    private fun derive(password: String, salt: ByteArray): String {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, 256)
        return try {
            Base64.getEncoder().encodeToString(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded)
        } finally { spec.clearPassword() }
    }
}
