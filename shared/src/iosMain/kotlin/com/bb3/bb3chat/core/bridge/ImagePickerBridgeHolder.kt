package com.bb3.bb3chat.core.bridge

/** Swift PHPicker presents the gallery; Kotlin receives picked JPEG bytes. */
object ImagePickerBridgeHolder {
    private var onPicked: ((ByteArray) -> Unit)? = null
    private var presentPickerFn: (() -> Unit)? = null

    fun bindPresenter(present: () -> Unit) {
        presentPickerFn = present
    }

    fun setCallback(callback: (ByteArray) -> Unit) {
        onPicked = callback
    }

    fun clearCallback() {
        onPicked = null
    }

    fun launch() {
        presentPickerFn?.invoke()
    }

    fun deliverPicked(bytes: ByteArray) {
        onPicked?.invoke(bytes)
    }
}
