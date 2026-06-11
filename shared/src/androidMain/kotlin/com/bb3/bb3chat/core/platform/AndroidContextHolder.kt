package com.bb3.bb3chat.core.platform

import android.content.Context

object AndroidContextHolder {
    private var applicationContext: Context? = null

    fun init(context: Context) {
        applicationContext = context.applicationContext
    }

    fun getOrNull(): Context? = applicationContext
}
