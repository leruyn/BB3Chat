package com.bb3.bb3chat.crypto

import com.bb3.bb3chat.core.crypto.RoomIdDeriver
import kotlin.test.Test
import kotlin.test.assertEquals

class RoomIdDeriverTest {

    @Test
    fun `derive is commutative regardless of code order`() {
        val idAB = RoomIdDeriver.derive("A1B2C3D4", "E5F6G7H8")
        val idBA = RoomIdDeriver.derive("E5F6G7H8", "A1B2C3D4")
        assertEquals(idAB, idBA)
    }

    @Test
    fun `derive normalizes case`() {
        val lower = RoomIdDeriver.derive("a1b2c3d4", "e5f6g7h8")
        val upper = RoomIdDeriver.derive("A1B2C3D4", "E5F6G7H8")
        assertEquals(lower, upper)
    }
}
