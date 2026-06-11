package com.bb3.bb3chat.feature.messaging.domain.model

data class InboxRoom(
    val id: String,
    val alias: String,
    val avatarIndex: Int,
    val lastSnippet: String,
    val lastTimestamp: Long,
    val unreadCount: Int,
    val isPinned: Boolean,
    val isMuted: Boolean
)
