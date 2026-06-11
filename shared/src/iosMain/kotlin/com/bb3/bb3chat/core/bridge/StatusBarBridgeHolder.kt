package com.bb3.bb3chat.core.bridge

import com.bb3.bb3chat.core.platform.AppStatusBarStyle

object StatusBarBridgeHolder {
    private var style: AppStatusBarStyle = AppStatusBarStyle.DarkBackground
    private var onChanged: (() -> Unit)? = null

    fun register(onChanged: () -> Unit) {
        this.onChanged = onChanged
        onChanged()
    }

    fun setStyle(newStyle: AppStatusBarStyle) {
        if (style == newStyle) return
        style = newStyle
        onChanged?.invoke()
    }

    fun isLightContent(): Boolean = style == AppStatusBarStyle.DarkBackground
}
