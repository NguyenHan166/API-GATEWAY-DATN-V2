package com.asoft.artsal.photo

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProcessLifecycleObserver @Inject constructor() : DefaultLifecycleObserver {
    private val _state = MutableStateFlow(true)
    val isForegroundStateFlow get() = _state.asStateFlow()

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        Timber.tag(TAG).d("onStart")
        _state.value = true
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        Timber.tag(TAG).d("onResume")
    }
    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        Timber.tag(TAG).d("onPause")
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        Timber.tag(TAG).d("onStop")
        _state.value = false
    }
    companion object {
        private const val TAG = "ProcessLifecycleObserver"
    }
}