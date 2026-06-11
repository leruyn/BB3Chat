package com.bb3.bb3chat.core.platform

import androidx.compose.runtime.Composable

/** Returns a callback that opens the platform image picker. */
@Composable
expect fun rememberImagePicker(onImagePicked: (ByteArray) -> Unit): () -> Unit
