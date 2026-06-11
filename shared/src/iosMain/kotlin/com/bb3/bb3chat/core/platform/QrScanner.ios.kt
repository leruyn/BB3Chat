@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.bb3.bb3chat.core.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import com.bb3.bb3chat.core.bridge.QrScannerBridgeHolder
import platform.UIKit.UIView

@Composable
actual fun QrScannerView(
    isActive: Boolean,
    onCodeDetected: (String) -> Unit,
    modifier: Modifier
) {
    DisposableEffect(isActive, onCodeDetected) {
        if (isActive) {
            QrScannerBridgeHolder.setCallback(onCodeDetected)
        } else {
            QrScannerBridgeHolder.clearCallback()
            QrScannerBridgeHolder.stop()
        }
        onDispose {
            QrScannerBridgeHolder.clearCallback()
            QrScannerBridgeHolder.stop()
        }
    }

    if (!isActive) return

    UIKitView(
        modifier = modifier,
        factory = {
            UIView().also { view ->
                QrScannerBridgeHolder.start(view)
            }
        },
        update = { view ->
            QrScannerBridgeHolder.updateFrame(view)
        },
        onRelease = {
            QrScannerBridgeHolder.stop()
        }
    )
}
