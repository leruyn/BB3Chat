package com.bb3.bb3chat

import android.app.Application
import com.bb3.bb3chat.core.platform.AndroidContextHolder
import com.bb3.bb3chat.core.di.androidModule
import com.bb3.bb3chat.core.di.initKoin
import com.bb3.bb3chat.core.firebase.FirebaseAuthInitializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger

class BB3ChatApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        AndroidContextHolder.init(this)
        initKoin {
            androidLogger()
            androidContext(this@BB3ChatApplication)
            modules(androidModule)
        }
        appScope.launch {
            runCatching { FirebaseAuthInitializer.ensureSignedIn() }
        }
    }
}
