package com.asoft.artsal.photo.utils

class MessageEvent(
    private val messageString: String? = null,
    val type: Type,
    val indefinite: Boolean = false,
) {

    companion object {
        fun info(messageResId: String, indefinite: Boolean = false) =
            MessageEvent(messageResId, Type.INFO, indefinite)

        fun error(messageResId: String) = MessageEvent(messageResId, Type.ERROR)
    }

    private var isConsumed: Boolean = false

    fun consume(): String? =
        if (!isConsumed) {
            isConsumed = true
            messageString
        } else {
            null
        }

    fun peek(): String? = messageString

    enum class Type {
        INFO,
        ERROR
    }
}