package com.bb3.bb3chat.core.di

import com.bb3.bb3chat.core.db.DatabaseDriverFactory
import com.bb3.bb3chat.core.platform.ImageProcessor
import com.bb3.bb3chat.core.platform.SensorManager
import com.bb3.bb3chat.core.storage.KeyValueStorage
import com.bb3.bb3chat.core.storage.KeyValueStorageImpl
import com.bb3.bb3chat.feature.messaging.data.FirebaseMessageRepository
import com.bb3.bb3chat.feature.messaging.domain.repository.MessageRepository
import com.bb3.bb3chat.feature.token.data.TokenRepositoryImpl
import com.bb3.bb3chat.feature.token.domain.repository.TokenRepository
import com.google.firebase.firestore.FirebaseFirestore
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val androidModule = module {
    single<KeyValueStorage>        { KeyValueStorageImpl(androidContext()) }
    single { DatabaseDriverFactory(androidContext()) }
    single { ImageProcessor() }
    single { SensorManager(androidContext()) }
    single { FirebaseFirestore.getInstance() }

    single<MessageRepository> {
        FirebaseMessageRepository(
            firestore      = get(),
            sessionManager = get()
        )
    }

    single<TokenRepository> {
        TokenRepositoryImpl(
            httpClient   = get(),
            storage      = get(),
            vpsRelayUrl  = "https://relay.bb3.internal"   // ← thay bằng URL VPS thật
        )
    }
}
