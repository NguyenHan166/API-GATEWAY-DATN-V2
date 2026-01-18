package com.asoft.artsal.photo.extensions

import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.artsal.photo.editor.collage.maker.databinding.AdsNativeFullBinding
import com.artsal.photo.editor.collage.maker.databinding.AdsNativeMediumRectangleBinding
import com.artsal.photo.editor.collage.maker.databinding.AdsNativeMediumSquareBinding
import com.artsal.photo.editor.collage.maker.databinding.AdsNativeMediumSquareHomeBinding
import com.minsap.ad.ads.common.NativeAdState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

fun Fragment.getAdNativeMediumRectangleBinding() = AdsNativeMediumRectangleBinding.inflate(
    layoutInflater
)

fun Fragment.getAdNativeMediumSquareBinding() = AdsNativeMediumSquareBinding.inflate(
    layoutInflater
)

fun Fragment.getAdNativeFullBinding() = AdsNativeFullBinding.inflate(
    layoutInflater
)

fun Fragment.getAdNativeMediumSquareHomeBinding() = AdsNativeMediumSquareHomeBinding.inflate(
    layoutInflater
)

fun MutableStateFlow<NativeAdState?>.getNativeAdData(): NativeAdState.NativeAdData? {
    return this.value as? NativeAdState.NativeAdData
}

inline fun <T> Flow<T>.collectNativeAdData(
    fragment: Fragment,
    crossinline action: suspend (value: T) -> Unit,
): Job = fragment.lifecycleScope.launch {
    fragment.viewLifecycleOwner.repeatOnLifecycle(state = Lifecycle.State.RESUMED) {
        Timber.d("Start collecting native ad...")
        collect { action(it) }
    }
}