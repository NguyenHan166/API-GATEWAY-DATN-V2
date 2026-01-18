package com.asoft.artsal.photo.extensions

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Parcelable
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.coroutineScope

inline fun <T : Fragment> T.withArgs(size: Int, block: Bundle.() -> Unit): T {
    val b = Bundle(size)
    b.block()
    this.arguments = b
    return this
}

val Fragment.viewLifecycleScope
    inline get() = viewLifecycleOwner.lifecycle.coroutineScope

@Suppress("NOTHING_TO_INLINE")
inline fun <T : Parcelable> Fragment.parcelableArgument(name: String): Lazy<T> {
    return lazy(LazyThreadSafetyMode.NONE) {
        requireNotNull(arguments?.getParcelable(name)) {
            "No argument $name passed into ${javaClass.simpleName}"
        }
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun Fragment.stringArgument(name: String) = lazy(LazyThreadSafetyMode.NONE) {
    arguments?.getString(name)
}

@Suppress("unused")
fun Fragment.toast(resId: Int) = requireContext().toast(resId)

fun Fragment.toast(text: CharSequence) = requireContext().toast(text)

fun Fragment.toastGravity(text: CharSequence) = requireContext().toastGravity(text)

fun Fragment.hasPermissionGranted(text: String) =
    ContextCompat.checkSelfPermission(requireContext(), text)

fun FragmentManager.addFragment(
    fragment: Fragment,
    container: Int,
    isAddToBackStack: Boolean = false
) {
    val fragmentTransaction =
        beginTransaction().add(container, fragment, fragment::class.java.simpleName)
    if (isAddToBackStack) fragmentTransaction.addToBackStack(fragment::class.java.simpleName)
        .commit()
    else fragmentTransaction.commit()
}

fun FragmentManager.hideFragment(
    isHide: Boolean,
    fragment: Fragment,
) {
    if (isHide) {
        beginTransaction().hide(fragment).commit()
    } else {
        beginTransaction().show(fragment).commit()
    }
}

fun FragmentManager.replaceFragment(
    fragment: Fragment,
    container: Int,
    isAddToBackStack: Boolean = false
) {
    val fragmentTransaction =
        beginTransaction().replace(container, fragment, fragment::class.java.simpleName)
    if (isAddToBackStack) fragmentTransaction.addToBackStack(fragment::class.java.simpleName)
        .commit()
    else fragmentTransaction.commit()
}

fun FragmentManager.removeFragmentByTag(
    tag: String,
) {
    val frg = findFragmentByTag(tag)
    if (frg != null) {
        beginTransaction().remove(frg).commit()
        popBackStackImmediate()
    }
}

fun FragmentManager.removeFragment(
    fragment: Fragment,
) {
    beginTransaction().remove(fragment).commitAllowingStateLoss()
    popBackStackImmediate()
}

fun FragmentManager.checkFragmentActive(tag: String): Boolean {
    val fragment = findFragmentByTag(tag)
    if (fragment != null && fragment.isAdded) return true
    return false
}

fun FragmentManager.getActiveFragment(): Fragment? {
    fragments.forEach { fragment ->
        if (fragment.isVisible) {
            return fragment
        }
    }
    return null
}

fun FragmentManager.changeFragment(hideFragmentTag: String?, showFragmentTag: String?) {
    val hideFragment = findFragmentByTag(hideFragmentTag)
    val showFragment = findFragmentByTag(showFragmentTag)

    val tr = beginTransaction()

    if (hideFragment != null) {
        tr.hide(hideFragment)
    }
    if (showFragment != null) {
        tr.show(showFragment)
    }

    tr.commit()
}
inline fun FragmentManager.commitTransaction(allowStateLoss: Boolean = false, func: FragmentTransaction.() -> FragmentTransaction) {
    val transaction = beginTransaction().func()
    if (allowStateLoss) {
        transaction.commitAllowingStateLoss()
    } else {
        transaction.commit()
    }
}


