package com.bb3.bb3chat.core.di

import com.bb3.bb3chat.core.db.DatabaseDriverFactory
import com.bb3.bb3chat.core.platform.ImageProcessor
import com.bb3.bb3chat.core.platform.IntruderCapture
import com.bb3.bb3chat.core.platform.IosIntruderCapture
import com.bb3.bb3chat.core.platform.FakePushNotifier
import com.bb3.bb3chat.core.platform.NotificationPermission
import com.bb3.bb3chat.core.platform.IosVoicePlayer
import com.bb3.bb3chat.core.platform.IosVoiceRecorder
import com.bb3.bb3chat.core.platform.SensorManager
import com.bb3.bb3chat.core.platform.VoicePlayer
import com.bb3.bb3chat.core.platform.VoiceRecorder
import com.bb3.bb3chat.core.storage.KeyValueStorage
import com.bb3.bb3chat.core.storage.KeyValueStorageImpl
import com.bb3.bb3chat.feature.messaging.data.IosFirebaseMessageRepository
import com.bb3.bb3chat.feature.messaging.domain.repository.MessageRepository
import com.bb3.bb3chat.feature.pairing.data.IosPairingSessionRepository
import com.bb3.bb3chat.feature.pairing.data.IosRoomCodeLobbyRepository
import com.bb3.bb3chat.feature.pairing.domain.repository.PairingSessionRepository
import com.bb3.bb3chat.feature.pairing.domain.repository.RoomCodeLobbyRepository
import com.bb3.bb3chat.feature.token.data.IosFcmTokenRegistrar
import com.bb3.bb3chat.feature.token.data.TokenRepositoryImpl
import com.bb3.bb3chat.feature.token.domain.repository.FcmTokenRegistrar
import com.bb3.bb3chat.feature.token.domain.repository.TokenRepository
import org.koin.dsl.module

val iosModule = module {
    single<KeyValueStorage> { KeyValueStorageImpl() }
    single { DatabaseDriverFactory() }
    single { ImageProcessor() }
    single { SensorManager() }
    single { FakePushNotifier() }
    single { NotificationPermission() }
    single<VoiceRecorder> { IosVoiceRecorder() }
    single<VoicePlayer> { IosVoicePlayer() }
    single<IntruderCapture> { IosIntruderCapture() }

    single<MessageRepository> { IosFirebaseMessageRepository(get()) }

    single<PairingSessionRepository> { IosPairingSessionRepository() }
    single<RoomCodeLobbyRepository> { IosRoomCodeLobbyRepository() }

    single<FcmTokenRegistrar> { IosFcmTokenRegistrar() }

    single<TokenRepository> {
        TokenRepositoryImpl(
            httpClient        = get(),
            storage           = get(),
            vpsRelayUrl       = "https://relay.bb3.internal",
            fcmTokenRegistrar = get()
        )
    }
}
