package com.bb3.bb3chat.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.sp

@Composable
actual fun ChatImageContent(bytes: ByteArray?, modifier: Modifier) {
    if (bytes == null) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text("🖼", fontSize = 32.sp)
        }
        return
    }
    val bitmap = remember(bytes) {
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
    }
    if (bitmap != null) {
        Image(
            bitmap             = bitmap,
            contentDescription = null,
            modifier           = modifier,
            contentScale       = ContentScale.Crop
        )
    } else {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text("🖼", fontSize = 32.sp)
        }
    }
}
