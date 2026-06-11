package com.bb3.bb3chat.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.sp
import org.jetbrains.skia.Image as SkiaImage

@Composable
actual fun ChatImageContent(bytes: ByteArray?, modifier: Modifier) {
    if (bytes == null) {
        ImagePlaceholder(modifier)
        return
    }
    val bitmap = remember(bytes) {
        runCatching { SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap() }.getOrNull()
    }
    if (bitmap != null) {
        Image(
            bitmap             = bitmap,
            contentDescription = null,
            modifier           = modifier,
            contentScale       = ContentScale.Crop
        )
    } else {
        ImagePlaceholder(modifier)
    }
}

@Composable
private fun ImagePlaceholder(modifier: Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Text("🖼", fontSize = 32.sp)
    }
}
