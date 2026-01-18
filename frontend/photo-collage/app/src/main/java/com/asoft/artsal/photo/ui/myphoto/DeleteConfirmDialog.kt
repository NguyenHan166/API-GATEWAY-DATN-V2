package com.asoft.artsal.photo.ui.myphoto

import android.view.LayoutInflater
import com.artsal.photo.editor.collage.maker.databinding.DialogDeleteConfirmBinding
import com.asoft.artsal.photo.base.AlertDialogFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DeleteConfirmDialog(
    private val onConfirm: () -> Unit = {}
) : AlertDialogFragment<DialogDeleteConfirmBinding>() {

    override fun setupUi() {
        binding?.run {
            btnDelete.setOnClickListener {
                onConfirmClick()
            }
            btnCancel.setOnClickListener {
                dismiss()
            }
        }
    }

    override fun onInflateView(inflater: LayoutInflater): DialogDeleteConfirmBinding =
        DialogDeleteConfirmBinding.inflate(inflater)

    fun onConfirmClick() {
        onConfirm.invoke()
        dismiss()
    }

    companion object {
        fun newInstance(onConfirm: () -> Unit = {}): DeleteConfirmDialog {
            return DeleteConfirmDialog(onConfirm)
        }
    }



}