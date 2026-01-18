package com.asoft.artsal.photo.extensions

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.artsal.photo.editor.collage.maker.R

fun Fragment.navigateTo(
    id: Int,
    bundle: Bundle? = null,
    popUpToId: Int? = null,
    isInclusive: Boolean = false // Default false to avoid accidental removals
) {
    try {
        val options = NavOptions.Builder().apply {
            popUpToId?.let {
                setPopUpTo(it, isInclusive) // Clear back stack if needed
            }
        }.build()

        findNavController().navigate(id, bundle, options)
    } catch (ex: Exception) {
        ex.printStackTrace()
    }
}

fun Fragment.navigateToWithAnim(
    id: Int,
    bundle: Bundle? = null,
    popUpToId: Int? = null,
    isInclusive: Boolean = false // Default false to avoid accidental removals
) {
    try {
        val options = NavOptions.Builder()
            .setEnterAnim(R.anim.anim_enter_from_right)
            .setExitAnim(R.anim.anim_exit_to_left)
            .setPopEnterAnim(R.anim.anim_enter_from_left)
            .setPopExitAnim(R.anim.anim_exit_to_right).apply {
                popUpToId?.let {
                    setPopUpTo(it, isInclusive) // Clear back stack if needed
                }
            }.build()

        findNavController().navigate(id, bundle, options)
    } catch (ex: Exception) {
        ex.printStackTrace()
    }
}

fun View.navigateTo(
    id: Int,
    bundle: Bundle? = null,
    popUpToId: Int? = null,
    isInclusive: Boolean = false // Default false to avoid accidental removals
) {
    val options = NavOptions.Builder().apply {
        popUpToId?.let {
            setPopUpTo(it, isInclusive) // Clear back stack if needed
        }
    }.build()

    findNavController().navigate(id, bundle, options)
}

fun Fragment.parentNavigateTo(
    id: Int,
    bundle: Bundle? = null,
    popUpToId: Int? = null,
    isInclusive: Boolean = false
) {
    try {
        val options = NavOptions.Builder()
            .setEnterAnim(R.anim.anim_enter_from_right)
            .setExitAnim(R.anim.anim_exit_to_left)
            .setPopEnterAnim(R.anim.anim_enter_from_left)
            .setPopExitAnim(R.anim.anim_exit_to_right).apply {
                popUpToId?.let {
                    setPopUpTo(it, isInclusive)
                }
            }.build()

        requireParentFragment().requireParentFragment().findNavController()
            .navigate(id, bundle, options)
    } catch (ex: Exception) {
        ex.printStackTrace()
    }
}

fun Fragment.popBackStack(id: Int? = null, isInclusive: Boolean = false) {
    try {
        if (id == null) {
            findNavController().popBackStack()
            return
        }
        findNavController().popBackStack(id, isInclusive)
    } catch (ex: Exception) {
        ex.printStackTrace()
    }
}

fun Fragment.navigateUp() {
    findNavController().navigateUp()
}

fun View.navigateUp() {
    findNavController().navigateUp()
}