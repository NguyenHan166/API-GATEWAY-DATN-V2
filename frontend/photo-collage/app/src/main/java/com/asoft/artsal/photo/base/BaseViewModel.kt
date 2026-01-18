package com.asoft.artsal.photo.base

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.artsal.photo.editor.collage.maker.BuildConfig
import com.asoft.artsal.photo.utils.MessageEvent
import com.asoft.artsal.photo.utils.SingleLiveEvent
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import timber.log.Timber
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

abstract class BaseViewModel(open val app: Application) : AndroidViewModel(app) {
    val onError = SingleLiveEvent<Throwable>()
    val isLoading = MutableLiveData(false)
    protected var jobCall: Job? = null

    protected val tag: String = javaClass.simpleName

    protected val _messageChannel = Channel<MessageEvent>(Channel.BUFFERED)
    val messageChannel = _messageChannel.receiveAsFlow()

    protected fun launchJob(
        context: CoroutineContext = EmptyCoroutineContext,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.() -> Unit
    ): Job = viewModelScope.launch(context + createErrorHandler(withOutError = false), start, block)

    protected fun launchWithoutError(
        context: CoroutineContext = EmptyCoroutineContext,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.() -> Unit
    ): Job = viewModelScope.launch(context + createErrorHandler(withOutError = true), start, block)

    protected fun launchLoadingJob(
        context: CoroutineContext = EmptyCoroutineContext,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.() -> Unit
    ): Job = viewModelScope.launch(context + createErrorHandler(withOutError = false), start) {
        isLoading.postValue(true)
        try {
            block()
        } finally {
            isLoading.postValue(false)
        }
    }

    private fun createErrorHandler(withOutError: Boolean) =
        CoroutineExceptionHandler { _, throwable ->
            if (BuildConfig.DEBUG) {
                throwable.printStackTrace()
            }
            if (throwable !is CancellationException) {
                if (withOutError) {
                    throwable.stackTrace
                } else {
                    onError.postCall(throwable)
                }
            }
        }

    companion object {
        const val SUBSCRIBE_STOP_TIMEOUT = 5000L
    }

    override fun onCleared() {
        super.onCleared()
        Timber.d("OnClear VM: $this")
        jobCall?.cancel()
    }
}