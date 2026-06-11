package com.bb3.bb3chat.core.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import com.bb3.bb3chat.core.bridge.ImagePickerBridgeHolder

@Composable
actual fun rememberImagePicker(onImagePicked: (ByteArray) -> Unit): () -> Unit {
    DisposableEffect(onImagePicked) {
        ImagePickerBridgeHolder.setCallback(onImagePicked)
        onDispose { ImagePickerBridgeHolder.clearCallback() }
    }
    return remember { { ImagePickerBridgeHolder.launch() } }
}
