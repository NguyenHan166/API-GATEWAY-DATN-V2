package com.asoft.artsal.photo.base

import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.viewbinding.ViewBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.asoft.artsal.photo.extensions.hideNavigationBar
import androidx.core.graphics.drawable.toDrawable

abstract class AlertDialogFragment<B : ViewBinding>(
) : DialogFragment() {

    open val clickOutSide = true

    private var viewBinding: B? = null

    protected val binding: B?
        get() = viewBinding

    final override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = onInflateView(layoutInflater)
        viewBinding = binding
        return MaterialAlertDialogBuilder(requireContext(), theme)
            .setView(binding.root)
            .create()
            .also(::onBuildDialog)
    }

    final override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = viewBinding?.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.window?.setBackgroundDrawable(getBackgroundDrawableColor().toDrawable())
        setupUi()
    }

    @CallSuper
    override fun onDestroyView() {
        viewBinding = null
        super.onDestroyView()
    }

    open fun onBuildDialog(builder: AlertDialog) {
        builder.hideNavigationBar()
        if (!clickOutSide) {
            builder.setCancelable(false)
            builder.setCanceledOnTouchOutside(false)
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        activity?.hideNavigationBar()
    }

    protected abstract fun setupUi()

    protected fun bindingOrNull(): B? = viewBinding

    protected abstract fun onInflateView(inflater: LayoutInflater): B

    protected open fun getBackgroundDrawableColor(): Int = Color.TRANSPARENT
}