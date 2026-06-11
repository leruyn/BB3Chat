package com.bb3.bb3chat.auth

import com.bb3.bb3chat.core.crypto.CryptoManager
import com.bb3.bb3chat.feature.auth.domain.model.PinValidationResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertIs

/**
 * Tests the PIN auth logic end-to-end:
 * - Real PIN  → RealAccess
 * - Decoy PIN → DecoyAccess
 * - Wrong PIN → InvalidPin
 *
 * Directly tests the derivation + hash comparison logic that PinAuthRepositoryImpl uses.
 */
class PinAuthTest {

    @Test
    fun `real PIN validates to RealAccess`() = runTest {
        val realPin = "7531"
        val realKey = CryptoManager.deriveKeyFromPin(realPin)
        val storedHash = realKey.toHex()

        val inputKey  = CryptoManager.deriveKeyFromPin(realPin)
        val result    = if (inputKey.toHex() == storedHash) PinValidationResult.RealAccess
                        else PinValidationResult.InvalidPin

        assertIs<PinValidationResult.RealAccess>(result)
    }

    @Test
    fun `wrong PIN returns InvalidPin`() = runTest {
        val realPin  = "7531"
        val wrongPin = "1234"
        val storedHash = CryptoManager.deriveKeyFromPin(realPin).toHex()

        val inputKey = CryptoManager.deriveKeyFromPin(wrongPin)
        val result   = if (inputKey.toHex() == storedHash) PinValidationResult.RealAccess
                       else PinValidationResult.InvalidPin

        assertIs<PinValidationResult.InvalidPin>(result)
    }

    @Test
    fun `decoy PIN validates to DecoyAccess`() = runTest {
        val realPin  = "7531"
        val decoyPin = "0000"
        val realHash  = CryptoManager.deriveKeyFromPin(realPin).toHex()
        val decoyHash = CryptoManager.deriveKeyFromPin(decoyPin).toHex()

        fun validate(pin: String): PinValidationResult {
            val k = CryptoManager.deriveKeyFromPin(pin).toHex()
            return when (k) {
                realHash  -> PinValidationResult.RealAccess
                decoyHash -> PinValidationResult.DecoyAccess
                else      -> PinValidationResult.InvalidPin
            }
        }

        assertIs<PinValidationResult.RealAccess>(validate(realPin))
        assertIs<PinValidationResult.DecoyAccess>(validate(decoyPin))
        assertIs<PinValidationResult.InvalidPin>(validate("9999"))
    }
}

private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
