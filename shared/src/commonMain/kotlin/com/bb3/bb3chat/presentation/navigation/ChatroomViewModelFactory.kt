package com.bb3.bb3chat.presentation.navigation

import com.bb3.bb3chat.core.crypto.SessionManager
import com.bb3.bb3chat.core.platform.ImageProcessor
import com.bb3.bb3chat.feature.messaging.domain.repository.MessageRepository
import com.bb3.bb3chat.feature.messaging.domain.usecase.OpenChatroomUseCase
import com.bb3.bb3chat.feature.messaging.domain.usecase.SendImageUseCase
import com.bb3.bb3chat.feature.messaging.domain.usecase.SendMessageUseCase
import com.bb3.bb3chat.feature.messaging.presentation.chatroom.ChatroomViewModel
import com.bb3.bb3chat.feature.security.domain.usecase.TriggerLocalPanicUseCase

/**
 * Manual factory so ChatroomViewModel can receive roomId at construction time
 * while all other dependencies come from the Koin graph.
 */
class ChatroomViewModelFactory(
    private val sessionManager : SessionManager,
    private val messageRepo    : MessageRepository,
    private val openChatroom   : OpenChatroomUseCase,
    private val sendMessage    : SendMessageUseCase,
    private val sendImage      : SendImageUseCase,
    private val imageProcessor : ImageProcessor,
    private val triggerPanic   : TriggerLocalPanicUseCase
) {
    fun create(roomId: String): ChatroomViewModel = ChatroomViewModel(
        roomId         = roomId,
        sessionManager = sessionManager,
        messageRepo    = messageRepo,
        openChatroom   = openChatroom,
        sendMessage    = sendMessage,
        sendImage      = sendImage,
        imageProcessor = imageProcessor,
        triggerPanic   = triggerPanic
    )
}
