package com.bb3.bb3chat.messaging

import com.bb3.bb3chat.feature.messaging.data.MessageLocalMapper
import com.bb3.bb3chat.feature.messaging.domain.model.MessageContent
import com.bb3.bb3chat.feature.messaging.domain.model.MessageStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MessageLocalMapperTest {

    @Test
    fun `readBy json round-trips`() {
        val original = mapOf("alice" to 1_700_000_000_000L, "bob" to 1_700_000_100_000L)
        val json     = MessageLocalMapper.readByToJson(original)
        val parsed   = MessageLocalMapper.parseReadBy(json)
        assertEquals(original, parsed)
    }

    @Test
    fun `parseReadBy returns empty for invalid json`() {
        assertTrue(MessageLocalMapper.parseReadBy("not-json").isEmpty())
    }
}
