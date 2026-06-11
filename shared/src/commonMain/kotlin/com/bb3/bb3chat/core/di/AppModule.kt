package com.bb3.bb3chat.core.di

import com.bb3.bb3chat.core.crypto.SessionManager
import com.bb3.bb3chat.core.network.HttpClientFactory
import com.bb3.bb3chat.feature.auth.data.repository.PinAuthRepositoryImpl
import com.bb3.bb3chat.feature.auth.domain.repository.PinAuthRepository
import com.bb3.bb3chat.feature.auth.domain.usecase.SetupPinUseCase
import com.bb3.bb3chat.feature.auth.domain.usecase.ValidatePinUseCase
import com.bb3.bb3chat.feature.auth.presentation.PinViewModel
import com.bb3.bb3chat.feature.messaging.domain.usecase.OpenChatroomUseCase
import com.bb3.bb3chat.feature.messaging.domain.usecase.SendImageUseCase
import com.bb3.bb3chat.feature.messaging.domain.usecase.SendMessageUseCase
import com.bb3.bb3chat.feature.messaging.presentation.inbox.InboxViewModel
import com.bb3.bb3chat.feature.messaging.domain.repository.MessageRepository
import com.bb3.bb3chat.feature.security.domain.usecase.TriggerLocalPanicUseCase
import com.bb3.bb3chat.feature.token.domain.usecase.HeartbeatUseCase as TokenHeartbeatUseCase
import com.bb3.bb3chat.presentation.navigation.ChatroomViewModelFactory
import org.koin.dsl.module

val coreModule = module {
    single { HttpClientFactory.create() }
    single { SessionManager() }
}

val authModule = module {
    single<PinAuthRepository> { PinAuthRepositoryImpl(get(), get(), get()) }
    factory { ValidatePinUseCase(get()) }
    factory { SetupPinUseCase(get()) }
    factory { PinViewModel(get(), get(), get()) }
}

val messagingModule = module {
    single<com.bb3.bb3chat.feature.messaging.domain.repository.RoomRepository> {
        com.bb3.bb3chat.feature.messaging.data.LocalRoomRepository(get(), get())
    }
    factory { OpenChatroomUseCase(get()) }
    factory { SendMessageUseCase(get()) }
    factory { SendImageUseCase(get(), get(), get()) }
    factory { InboxViewModel(get(), get()) }
    single {
        ChatroomViewModelFactory(
            sessionManager = get(),
            messageRepo    = get(),
            openChatroom   = get(),
            sendMessage    = get(),
            sendImage      = get(),
            imageProcessor = get(),
            triggerPanic   = get()
        )
    }
}

val securityModule = module {
    factory { TriggerLocalPanicUseCase(get()) }
    factory {
        com.bb3.bb3chat.feature.security.domain.usecase.CaptureIntruderUseCase(get(), get())
    }
}

val storeModule = module {
    factory { com.bb3.bb3chat.feature.store.presentation.VipStoreViewModel(get()) }
}

val pairingModule = module {
    factory { com.bb3.bb3chat.feature.pairing.domain.usecase.ConnectRoomUseCase(get(), get()) }
    factory { com.bb3.bb3chat.feature.pairing.presentation.QrHandshakeViewModel(get()) }
}

val tokenModule = module {
    factory { TokenHeartbeatUseCase(get()) }
}
