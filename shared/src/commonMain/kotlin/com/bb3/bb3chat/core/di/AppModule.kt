package com.bb3.bb3chat.core.di

import com.bb3.bb3chat.core.crypto.SessionManager
import com.bb3.bb3chat.core.network.HttpClientFactory
import com.bb3.bb3chat.feature.auth.data.repository.PinAuthRepositoryImpl
import com.bb3.bb3chat.feature.auth.domain.repository.PinAuthRepository
import com.bb3.bb3chat.feature.auth.domain.usecase.SetupPinUseCase
import com.bb3.bb3chat.feature.auth.domain.usecase.ValidatePinUseCase
import com.bb3.bb3chat.feature.auth.presentation.PinViewModel
import com.bb3.bb3chat.feature.messaging.domain.usecase.OpenChatroomUseCase
import com.bb3.bb3chat.feature.messaging.data.ScheduledMessageStore
import com.bb3.bb3chat.feature.messaging.domain.usecase.SendImageUseCase
import com.bb3.bb3chat.feature.messaging.domain.usecase.SendMessageUseCase
import com.bb3.bb3chat.feature.messaging.domain.usecase.SendVoiceUseCase
import com.bb3.bb3chat.feature.security.domain.usecase.ExecutePanicUseCase
import com.bb3.bb3chat.feature.messaging.presentation.inbox.InboxViewModel
import com.bb3.bb3chat.feature.messaging.domain.repository.MessageRepository
import com.bb3.bb3chat.feature.security.domain.usecase.TriggerLocalPanicUseCase
import com.bb3.bb3chat.feature.token.domain.usecase.HeartbeatUseCase as TokenHeartbeatUseCase
import com.bb3.bb3chat.presentation.navigation.ChatroomViewModelFactory
import org.koin.dsl.module

val coreModule = module {
    single { HttpClientFactory.create() }
    single { SessionManager() }
    single { com.bb3.bb3chat.core.disguise.DisguiseConfig(get()) }
    single { com.bb3.bb3chat.core.security.SecurityPreferences(get()) }
    single { com.bb3.bb3chat.core.vip.VipEntitlements(get()) }
    single { com.bb3.bb3chat.core.profile.UserProfileRepository(get(), get()) }
    single {
        com.bb3.bb3chat.core.platform.FakePushCoordinator(get(), get(), get(), get())
            .also { it.start() }
    }
}

val authModule = module {
    single<PinAuthRepository> { PinAuthRepositoryImpl(get(), get(), get()) }
    factory { ValidatePinUseCase(get()) }
    factory { SetupPinUseCase(get()) }
    factory { PinViewModel(get(), get(), get(), get(), get(), get()) }
}

val messagingModule = module {
    single<com.bb3.bb3chat.feature.messaging.domain.repository.RoomRepository> {
        com.bb3.bb3chat.feature.messaging.data.LocalRoomRepository(get(), get())
    }
    factory { OpenChatroomUseCase(get()) }
    factory { SendMessageUseCase(get()) }
    factory { SendImageUseCase(get(), get(), get()) }
    factory { SendVoiceUseCase(get(), get()) }
    single { ScheduledMessageStore(get()) }
    factory { InboxViewModel(get(), get(), get(), get()) }
    single {
        ChatroomViewModelFactory(
            sessionManager  = get(),
            messageRepo     = get(),
            openChatroom    = get(),
            sendMessage     = get(),
            sendImage       = get(),
            sendVoice       = get(),
            imageProcessor  = get(),
            executePanic    = get(),
            vipEntitlements = get(),
            scheduledStore  = get(),
            voiceRecorder   = get(),
            voicePlayer     = get(),
            fakePushCoordinator = get()
        )
    }
}

val securityModule = module {
    factory { TriggerLocalPanicUseCase(get()) }
    factory { ExecutePanicUseCase(get(), get(), get(), get(), get()) }
    factory { com.bb3.bb3chat.feature.security.domain.usecase.CheckDeadmanUseCase(get(), get()) }
    single {
        com.bb3.bb3chat.feature.security.domain.usecase.CaptureIntruderUseCase(get())
    }
}

val storeModule = module {
    factory { com.bb3.bb3chat.feature.store.presentation.VipStoreViewModel(get()) }
}

val settingsModule = module {
    factory {
        com.bb3.bb3chat.feature.settings.presentation.SettingsViewModel(
            get(), get(), get(), get(), get(), get()
        )
    }
}

val profileModule = module {
    factory { com.bb3.bb3chat.feature.profile.presentation.ProfileSetupViewModel(get()) }
}

val pairingModule = module {
    factory { com.bb3.bb3chat.core.platform.QrCodeGenerator() }
    factory {
        com.bb3.bb3chat.feature.pairing.domain.usecase.ConnectRoomUseCase(get(), get(), get())
    }
    factory {
        com.bb3.bb3chat.feature.pairing.presentation.QrHandshakeViewModel(get(), get(), get(), get(), get())
    }
}

val tokenModule = module {
    factory { TokenHeartbeatUseCase(get()) }
}
