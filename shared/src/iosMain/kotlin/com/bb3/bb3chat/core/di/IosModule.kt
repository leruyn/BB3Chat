package com.bb3.bb3chat.core.di

import com.bb3.bb3chat.core.db.DatabaseDriverFactory
import com.bb3.bb3chat.core.platform.ImageProcessor
import com.bb3.bb3chat.core.platform.SensorManager
import com.bb3.bb3chat.core.storage.KeyValueStorage
import com.bb3.bb3chat.core.storage.KeyValueStorageImpl
import com.bb3.bb3chat.feature.messaging.data.IosFirebaseMessageRepository
import com.bb3.bb3chat.feature.messaging.domain.repository.MessageRepository
import com.bb3.bb3chat.feature.token.data.TokenRepositoryImpl
import com.bb3.bb3chat.feature.token.domain.repository.TokenRepository
import org.koin.dsl.module

val iosModule = module {
    single<KeyValueStorage> { KeyValueStorageImpl() }
    single { DatabaseDriverFactory() }
    single { ImageProcessor() }
    single { SensorManager() }

    single<MessageRepository> { IosFirebaseMessageRepository(get()) }

    single<TokenRepository> {
        TokenRepositoryImpl(
            httpClient  = get(),
            storage     = get(),
            vpsRelayUrl = "https://relay.bb3.internal"
        )
    }
}
