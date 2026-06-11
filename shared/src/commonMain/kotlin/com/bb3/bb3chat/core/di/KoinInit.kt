package com.bb3.bb3chat.core.di

import org.koin.core.KoinApplication
import org.koin.core.context.startKoin

fun initKoin(appDeclaration: KoinApplication.() -> Unit = {}) {
    startKoin {
        appDeclaration()
        modules(coreModule, authModule, messagingModule, securityModule, tokenModule,
                storeModule, settingsModule, profileModule, pairingModule)
    }
}
