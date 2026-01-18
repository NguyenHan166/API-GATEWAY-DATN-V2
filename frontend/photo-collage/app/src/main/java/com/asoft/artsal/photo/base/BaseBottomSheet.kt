package com.asoft.artsal.photo.base

import android.app.Dialog
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.viewbinding.ViewBinding
import com.artsal.photo.editor.collage.maker.R
import com.asoft.artsal.photo.extensions.hideNavigationBar
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.internal.ViewUtils.getBackgroundColor

abstract class BaseBottomSheet<B : ViewBinding> : BottomSheetDialogFragment() {
    private var viewBinding: B? = null

    protected val binding: B?
        get() = viewBinding

    protected open val isHideNavigationBar = true

    @Suppress("DEPRECATION")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        if (isHideNavigationBar)  {
            dialog.hideNavigationBar()
            dialog.window?.let { window ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    WindowCompat.setDecorFitsSystemWindows(window, false)
                } else {
                    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                    window.statusBarColor = Color.TRANSPARENT
                    window.navigationBarColor = Color.TRANSPARENT
                }
                window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
            }
        }
        return dialog
    }

    final override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = onInflateView(inflater, container)
        viewBinding = binding
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (dialog as? BottomSheetDialog)?.behavior?.state = STATE_EXPANDED
        setupUi()
    }

    protected abstract fun onInflateView(inflater: LayoutInflater, container: ViewGroup?): B

    protected abstract fun setupUi()

    override fun onDestroyView() {
        super.onDestroyView()
        viewBinding = null
    }

}