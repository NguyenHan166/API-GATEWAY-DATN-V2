package com.asoft.artsal.photo.base

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import com.artsal.photo.editor.collage.maker.R

class LoadingDialog(context: Context) : Dialog(context) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_dialog_loading)
        setCancelable(false)
        setCanceledOnTouchOutside(false)
        window?.setBackgroundDrawableResource(R.color.transparent)
    }
}