package com.bb3.bb3chat.crypto

import com.bb3.bb3chat.core.crypto.RoomKeyDeriver
import kotlin.test.Test
import kotlin.test.assertContentEquals

/**
 * Sorting invariant — actual key bytes require platform CryptoManager (tested on device).
 */
class RoomKeyDeriverTest {

    @Test
    fun `derive is commutative regardless of code order`() {
        val keyAB = RoomKeyDeriver.derive("A1B2C3D4", "E5F6G7H8")
        val keyBA = RoomKeyDeriver.derive("E5F6G7H8", "A1B2C3D4")
        assertContentEquals(keyAB, keyBA)
    }
}
