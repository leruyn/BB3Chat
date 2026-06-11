package com.bb3.bb3chat.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun ChatImageContent(bytes: ByteArray?, modifier: Modifier)
