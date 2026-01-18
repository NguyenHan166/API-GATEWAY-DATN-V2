package com.asoft.artsal.photo.extensions

import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import timber.log.Timber

// For collect many flow state
// Follow lifecycle

fun <T> LifecycleOwner.lifecycleAwareLazy(initializer: () -> T): Lazy<T> = LifecycleAwareLazy(this, initializer)

private object UninitializedValue

class LifecycleAwareLazy<out T>(
    private val owner: LifecycleOwner,
    initializer: () -> T
) : Lazy<T>, DefaultLifecycleObserver {

    private var initializer: (() -> T)? = initializer

    private var _value: Any? = UninitializedValue

    @Suppress("UNCHECKED_CAST")
    override val value: T
        @MainThread
        get() {
            if (_value === UninitializedValue) {
                _value = initializer!!()
                attachToLifecycle()
            }
            return _value as T
        }

    override fun onDestroy(owner: LifecycleOwner) {
        _value = UninitializedValue
        detachFromLifecycle()
    }

    private fun attachToLifecycle() {
        if (getLifecycleOwner().lifecycle.currentState == Lifecycle.State.DESTROYED) {
            throw IllegalStateException("Initialization failed because lifecycle has been destroyed!")
        }
        getLifecycleOwner().lifecycle.addObserver(this)
    }

    private fun detachFromLifecycle() {
        getLifecycleOwner().lifecycle.removeObserver(this)
    }

    private fun getLifecycleOwner() = when (owner) {
        is Fragment -> owner.viewLifecycleOwner
        else -> owner
    }

    override fun isInitialized(): Boolean = _value !== UninitializedValue

    override fun toString(): String = if (isInitialized()) value.toString() else "Lazy value not initialized yet."
}
fun Fragment.launchAndRepeatStarted(
    vararg launchBlock: suspend () -> Unit,
    doAfterLaunch: (() -> Unit)? = null
) {
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            launchBlock.forEach {
                launch { it.invoke() }
            }
            doAfterLaunch?.invoke()
        }
    }
}

inline fun <T> Flow<T>.collectInOwner(
    owner: LifecycleOwner,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    crossinline action: suspend (value: T) -> Unit,
): Job = owner.lifecycleScope.launch {
    owner.lifecycle.repeatOnLifecycle(state = minActiveState) {
        Timber.d("Start collecting...")
        collect { action(it) }
    }
}
fun FragmentActivity.launchAndRepeatStarted(
    vararg launchBlock: suspend () -> Unit,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
) {
    lifecycleScope.launch {
        lifecycle.repeatOnLifecycle(minActiveState) {
            launchBlock.forEach {
                launch { it.invoke() }
            }
        }
    }
}

inline fun <T> Flow<T>.collectIn(
    fragment: Fragment,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    crossinline action: suspend (value: T) -> Unit,
): Job = fragment.lifecycleScope.launch {
    fragment.viewLifecycleOwner.repeatOnLifecycle(state = minActiveState) {
        Timber.d("Start collecting...")
        collect { action(it) }
    }
}

fun Fragment.launchAndRepeat(
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    doAfterLaunch: (() -> Unit)? = null
) {
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(state = minActiveState) {
            doAfterLaunch?.invoke()
        }
    }
}

fun Fragment.isAtLeastStarted(): Boolean = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
