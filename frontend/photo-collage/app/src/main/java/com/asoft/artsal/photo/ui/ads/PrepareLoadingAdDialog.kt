package com.asoft.artsal.photo.ui.ads

import android.app.Dialog
import android.content.Context
import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Bundle
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.artsal.photo.editor.collage.maker.R
import com.artsal.photo.editor.collage.maker.databinding.DialogPrepareLoadingAdsBinding
import com.asoft.artsal.photo.extensions.hideNavigationBar
import com.asoft.artsal.photo.extensions.setGradientText

class PrepareLoadingAdDialog(context: Context) :
    Dialog(context, R.style.PrepareLoadingAdDialogStyle) {
    private var binding: DialogPrepareLoadingAdsBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogPrepareLoadingAdsBinding.inflate(layoutInflater)
        setContentView(binding?.root ?: return)
        hideNavigationBar()
        setCancelable(true)
        setCanceledOnTouchOutside(false)
        // Setup UI
        setOnDismissListener {
            hideNavigationBar()
        }
        binding?.tvLoading?.setGradientText(
            listOf(
                "#476CFF",
                "#559EFF",
                "#8BE5FF"
            )
        )
    }

    private fun applyTextGradient(textView: TextView) {
        val width = textView.paint.measureText(textView.text.toString())
        val activity = ownerActivity ?: return
        val textShader = LinearGradient(
            0f, 0f, width, textView.textSize,
            intArrayOf(
                ContextCompat.getColor(activity, R.color.primary_gradient),
                ContextCompat.getColor(activity, R.color.secondary_gradient),
                ContextCompat.getColor(activity, R.color.sub_secondary_gradient)
            ),
            null,
            Shader.TileMode.CLAMP
        )
        textView.paint.shader = textShader
    }
}