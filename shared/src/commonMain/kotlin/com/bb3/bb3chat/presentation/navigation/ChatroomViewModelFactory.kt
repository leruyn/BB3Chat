package com.bb3.bb3chat.presentation.navigation

import com.bb3.bb3chat.core.crypto.SessionManager
import com.bb3.bb3chat.core.platform.FakePushCoordinator
import com.bb3.bb3chat.core.platform.ImageProcessor
import com.bb3.bb3chat.core.platform.VoicePlayer
import com.bb3.bb3chat.core.platform.VoiceRecorder
import com.bb3.bb3chat.core.vip.VipEntitlements
import com.bb3.bb3chat.feature.messaging.data.ScheduledMessageStore
import com.bb3.bb3chat.feature.messaging.domain.repository.MessageRepository
import com.bb3.bb3chat.feature.messaging.domain.usecase.OpenChatroomUseCase
import com.bb3.bb3chat.feature.messaging.domain.usecase.SendImageUseCase
import com.bb3.bb3chat.feature.messaging.domain.usecase.SendMessageUseCase
import com.bb3.bb3chat.feature.messaging.domain.usecase.SendVoiceUseCase
import com.bb3.bb3chat.feature.messaging.presentation.chatroom.ChatroomViewModel
import com.bb3.bb3chat.feature.security.domain.usecase.ExecutePanicUseCase

class ChatroomViewModelFactory(
    private val sessionManager  : SessionManager,
    private val messageRepo     : MessageRepository,
    private val openChatroom    : OpenChatroomUseCase,
    private val sendMessage     : SendMessageUseCase,
    private val sendImage       : SendImageUseCase,
    private val sendVoice       : SendVoiceUseCase,
    private val imageProcessor  : ImageProcessor,
    private val executePanic    : ExecutePanicUseCase,
    private val vipEntitlements : VipEntitlements,
    private val scheduledStore  : ScheduledMessageStore,
    private val voiceRecorder   : VoiceRecorder,
    private val voicePlayer     : VoicePlayer,
    private val fakePushCoordinator: FakePushCoordinator
) {
    fun create(roomId: String): ChatroomViewModel = ChatroomViewModel(
        roomId          = roomId,
        sessionManager  = sessionManager,
        messageRepo     = messageRepo,
        openChatroom    = openChatroom,
        sendMessage     = sendMessage,
        sendImage       = sendImage,
        sendVoice       = sendVoice,
        imageProcessor  = imageProcessor,
        executePanic    = executePanic,
        vipEntitlements = vipEntitlements,
        scheduledStore  = scheduledStore,
        voiceRecorder   = voiceRecorder,
        voicePlayer     = voicePlayer,
        fakePushCoordinator = fakePushCoordinator
    )
}
