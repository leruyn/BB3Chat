package com.bb3.bb3chat

import android.Manifest
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.bb3.bb3chat.core.platform.AppBackgroundCallbacks
import com.bb3.bb3chat.core.platform.NotificationPermissionHolder
import com.bb3.bb3chat.feature.token.domain.usecase.HeartbeatUseCase
import com.bb3.bb3chat.presentation.navigation.BB3ChatApp
import com.bb3.bb3chat.ui.theme.BB3ChatTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class MainActivity : ComponentActivity() {

    private val heartbeat: HeartbeatUseCase by inject()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        pendingNotificationPermission?.resume(granted)
        pendingNotificationPermission = null
    }

    private var pendingNotificationPermission: kotlin.coroutines.Continuation<Boolean>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        NotificationPermissionHolder.register {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                return@register true
            }
            suspendCoroutine { cont ->
                pendingNotificationPermission = cont
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )

        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                AppBackgroundCallbacks.notifyAppBackground()
            }
        })

        setContent {
            BB3ChatTheme {
                BB3ChatApp()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { heartbeat() }
        }
    }
}
