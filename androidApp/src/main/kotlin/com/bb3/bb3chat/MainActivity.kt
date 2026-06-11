package com.bb3.bb3chat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.bb3.bb3chat.feature.token.domain.usecase.HeartbeatUseCase
import com.bb3.bb3chat.presentation.navigation.BB3ChatApp
import com.bb3.bb3chat.ui.theme.BB3ChatTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val heartbeat: HeartbeatUseCase by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            BB3ChatTheme {
                BB3ChatApp()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Heartbeat mỗi lần mở app thành công
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { heartbeat() }
        }
    }
}
