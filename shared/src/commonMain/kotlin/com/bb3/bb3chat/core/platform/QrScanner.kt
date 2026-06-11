package com.bb3.bb3chat.core.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Live camera preview that emits decoded QR payloads (e.g. `BB3:ABCD1234`). */
@Composable
expect fun QrScannerView(
    isActive: Boolean,
    onCodeDetected: (String) -> Unit,
    modifier: Modifier = Modifier
)
