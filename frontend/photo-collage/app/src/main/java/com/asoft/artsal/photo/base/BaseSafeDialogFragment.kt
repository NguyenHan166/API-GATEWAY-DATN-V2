package com.asoft.artsal.photo.base

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.core.view.WindowCompat
import androidx.fragment.app.DialogFragment
import androidx.viewbinding.ViewBinding
import com.artsal.photo.editor.collage.maker.R
import com.asoft.artsal.photo.extensions.hideNavigationBar

abstract class BaseSafeDialogFragment<B : ViewBinding> : DialogFragment() {

    protected open val isInsets = true
    private var viewBinding: B? = null

    protected val binding: B?
        get() = viewBinding
    protected open val isHideNavigationBar = true

    @Suppress("DEPRECATION")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        if (isHideNavigationBar) {
            dialog.hideNavigationBar()
            dialog.window?.let { window ->
                WindowCompat.setDecorFitsSystemWindows(window, true)
                window.statusBarColor = Color.TRANSPARENT
                window.navigationBarColor = Color.TRANSPARENT
            }
        }
        return dialog
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(
            STYLE_NORMAL,
            R.style.KeyboardInputStyle
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = onInflateView(inflater, container)
        viewBinding = binding
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.window?.setBackgroundDrawableResource(getBackgroundColor())
        setupUi()
    }

    @CallSuper
    override fun onDestroyView() {
        viewBinding = null
        super.onDestroyView()
    }

    protected abstract fun setupUi()

    protected abstract fun onInflateView(inflater: LayoutInflater, container: ViewGroup?): B

    protected open fun getBackgroundColor() = R.color.transparent

}