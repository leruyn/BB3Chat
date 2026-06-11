package com.bb3.bb3chat.core.di

import com.bb3.bb3chat.core.db.DatabaseDriverFactory
import com.bb3.bb3chat.core.platform.AndroidIntruderCapture
import com.bb3.bb3chat.core.platform.ImageProcessor
import com.bb3.bb3chat.core.platform.IntruderCapture
import com.bb3.bb3chat.core.platform.AndroidVoicePlayer
import com.bb3.bb3chat.core.platform.AndroidVoiceRecorder
import com.bb3.bb3chat.core.platform.FakePushNotifier
import com.bb3.bb3chat.core.platform.NotificationPermission
import com.bb3.bb3chat.core.platform.SensorManager
import com.bb3.bb3chat.core.platform.VoicePlayer
import com.bb3.bb3chat.core.platform.VoiceRecorder
import com.bb3.bb3chat.core.storage.KeyValueStorage
import com.bb3.bb3chat.core.storage.KeyValueStorageImpl
import com.bb3.bb3chat.feature.messaging.data.FirebaseMessageRepository
import com.bb3.bb3chat.feature.messaging.domain.repository.MessageRepository
import com.bb3.bb3chat.feature.pairing.data.FirebasePairingSessionRepository
import com.bb3.bb3chat.feature.pairing.data.FirebaseRoomCodeLobbyRepository
import com.bb3.bb3chat.feature.pairing.domain.repository.PairingSessionRepository
import com.bb3.bb3chat.feature.pairing.domain.repository.RoomCodeLobbyRepository
import com.bb3.bb3chat.feature.token.data.AndroidFcmTokenRegistrar
import com.bb3.bb3chat.feature.token.data.TokenRepositoryImpl
import com.bb3.bb3chat.feature.token.domain.repository.FcmTokenRegistrar
import com.bb3.bb3chat.feature.token.domain.repository.TokenRepository
import com.google.firebase.firestore.FirebaseFirestore
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val androidModule = module {
    single<KeyValueStorage>        { KeyValueStorageImpl(androidContext()) }
    single { DatabaseDriverFactory(androidContext()) }
    single { ImageProcessor() }
    single { SensorManager(androidContext()) }
    single { FakePushNotifier(androidContext()) }
    single { NotificationPermission(androidContext()) }
    single<VoiceRecorder> { AndroidVoiceRecorder(androidContext()) }
    single<VoicePlayer> { AndroidVoicePlayer(androidContext()) }
    single<IntruderCapture> { AndroidIntruderCapture(androidContext(), get()) }
    single { FirebaseFirestore.getInstance() }

    single<MessageRepository> {
        FirebaseMessageRepository(
            firestore      = get(),
            sessionManager = get()
        )
    }

    single<PairingSessionRepository> {
        FirebasePairingSessionRepository(firestore = get())
    }

    single<RoomCodeLobbyRepository> {
        FirebaseRoomCodeLobbyRepository(firestore = get())
    }

    single<FcmTokenRegistrar> { AndroidFcmTokenRegistrar(get()) }

    single<TokenRepository> {
        TokenRepositoryImpl(
            httpClient        = get(),
            storage           = get(),
            vpsRelayUrl       = "https://relay.bb3.internal",   // ← thay bằng URL VPS thật
            fcmTokenRegistrar = get()
        )
    }
}
