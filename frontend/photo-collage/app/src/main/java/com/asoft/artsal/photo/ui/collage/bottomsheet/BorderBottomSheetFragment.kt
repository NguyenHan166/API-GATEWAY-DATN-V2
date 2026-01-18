package com.asoft.artsal.photo.ui.collage.bottomsheet

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.activityViewModels
import com.artsal.photo.editor.collage.maker.R
import com.artsal.photo.editor.collage.maker.databinding.BottomsheetBorderBinding
import com.asoft.artsal.photo.base.BaseBottomSheet
import com.asoft.artsal.photo.extensions.onClick
import com.asoft.artsal.photo.ui.collage.viewmodel.CollageViewModel
import com.asoft.artsal.photo.utils.FirebaseEventUtils

class BorderBottomSheetFragment : BaseBottomSheet<BottomsheetBorderBinding>() {
    private var borderOuter: Float = 0f
    private var borderInner: Float = 0f
    private var borderRadius: Float = 0f

    private val collageViewModel: CollageViewModel by activityViewModels<CollageViewModel>()

    override fun onInflateView(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): BottomsheetBorderBinding {
        return BottomsheetBorderBinding.inflate(inflater, container, false)
    }

    override fun getTheme(): Int {
        return R.style.KeyboardInputStyle
    }

    override fun setupUi() {
        binding?.run {
            val borderObserver = collageViewModel.border.value
            seekBarBorderOuter.apply {
                max = collageViewModel.maxSpacing.toInt()
                progress = borderObserver.borderOuter.toInt()
            }
            tvBorderOuter.text = borderObserver.borderOuter.toInt().toString()
//            seekBarBorderInner.apply {
//                max = collageViewModel.maxSpacing.toInt()
//                progress = borderObserver.borderInner.toInt()
//            }
            tvBorderInner.text = borderObserver.borderInner.toInt().toString()
            seekBarBorderRadius.apply {
                max = collageViewModel.maxBorder.toInt()
                progress = borderObserver.borderRadius.toInt()
            }
            tvBorderRadius.text = borderObserver.borderRadius.toInt().toString()

            seekBarBorderOuter.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    p0: SeekBar?,
                    p1: Int,
                    p2: Boolean
                ) {
                    FirebaseEventUtils.logEventTracking(context, "border_margin_change")

                    tvBorderOuter.text = p1.toString()
                    collageViewModel.updateBorderOuter(p1.toFloat())
                }

                override fun onStartTrackingTouch(p0: SeekBar?) {
                }

                override fun onStopTrackingTouch(p0: SeekBar?) {
                }
            })
            seekBarBorderRadius.setOnSeekBarChangeListener(object :
                SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    p0: SeekBar?,
                    p1: Int,
                    p2: Boolean
                ) {
                    FirebaseEventUtils.logEventTracking(context, "border_radius_change")

                    tvBorderRadius.text = p1.toString()
                    collageViewModel.updateBorderRadius(p1.toFloat())

                }

                override fun onStartTrackingTouch(p0: SeekBar?) {
                }

                override fun onStopTrackingTouch(p0: SeekBar?) {
                }
            })

            header.imgDone.onClick {
                FirebaseEventUtils.logEventTracking(context, "border_save")

                val border = collageViewModel.border.value
                borderOuter = border.borderOuter
                borderInner = border.borderInner
                borderRadius = border.borderRadius
                dismiss()
            }
            header.imgClose.onClick {
                FirebaseEventUtils.logEventTracking(context, "border_close")

                collageViewModel.updateBorderOuter(borderOuter)
                collageViewModel.updateBorderInner(borderInner)
                collageViewModel.updateBorderRadius(borderRadius)
                dismiss()
            }


        }
    }

    override fun onResume() {
        super.onResume()
        val borderSetup = collageViewModel.getBorder()
        borderOuter = borderSetup.borderOuter
        borderInner = borderSetup.borderInner
        borderRadius = borderSetup.borderRadius
    }

    override fun onDestroyView() {
        super.onDestroyView()
        collageViewModel.showViewHeader.call(true)
    }

    companion object {
        fun newInstance() = BorderBottomSheetFragment()
    }
}