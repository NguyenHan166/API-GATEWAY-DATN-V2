package com.asoft.artsal.photo.ui.collage.bottomsheet

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.artsal.photo.editor.collage.maker.R
import com.artsal.photo.editor.collage.maker.databinding.BottomsheetBackgroundBinding
import com.asoft.artsal.photo.base.BaseBottomSheet
import com.asoft.artsal.photo.extensions.initRecyclerViewAdapter
import com.asoft.artsal.photo.extensions.onClick
import com.asoft.artsal.photo.ui.collage.viewmodel.CollageViewModel
import com.asoft.artsal.photo.ui.collage.adapter.ColorAdapter
import com.asoft.artsal.photo.utils.AppUtils
import com.asoft.artsal.photo.utils.FirebaseEventUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BackgroundBottomSheetFragment : BaseBottomSheet<BottomsheetBackgroundBinding>() {

    private var colorBackground: Int = Color.WHITE

    private val collageViewModel: CollageViewModel by activityViewModels<CollageViewModel>()

    private val colorAdapter by lazy {
        ColorAdapter { selectedColor ->
            onColorSelected(selectedColor)
        }
    }
    override fun getTheme(): Int {
        return R.style.KeyboardInputStyle
    }
    private var onColorSelectedListener: ((Int) -> Unit)? = null
    private var isInitialized = false

    override fun onInflateView(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): BottomsheetBackgroundBinding {
        return BottomsheetBackgroundBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }



    override fun setupUi() {
        colorBackground = collageViewModel.getBackgroundColor()
        setupRecyclerView()
        setupHeader()
        isInitialized = true
    }

    private fun setupRecyclerView() {
        binding?.rvBackground?.initRecyclerViewAdapter(
            yourAdapter = colorAdapter,
            yourLayoutManager = LinearLayoutManager(
                context,
                LinearLayoutManager.HORIZONTAL,
                false
            ),
            fixedSize = true,
        )
        colorAdapter.submitList(AppUtils.colorTypes)

    }

    private fun setupHeader() {
        binding?.header?.apply {
            imgDone.onClick {
                FirebaseEventUtils.logEventTracking(context, "background_save")

                colorBackground = collageViewModel.getBackgroundColor()
                dismiss()
            }
            imgClose.onClick {
                FirebaseEventUtils.logEventTracking(context, "background_close")

                collageViewModel.updateBackgroundColor(colorBackground)
                dismiss()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        colorAdapter.setCurrentColor(colorBackground)
        binding?.rvBackground?.smoothScrollToPosition(colorAdapter.selectedPositionPublic)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        collageViewModel.showViewHeader.call(true)
    }

    private fun onColorSelected(color: Int) {
        FirebaseEventUtils.logEventTracking(context, "background_change")

        if (isInitialized) {
            onColorSelectedListener?.invoke(color)
            collageViewModel.updateBackgroundColor(color)
        }
    }

    companion object {
        fun newInstance(): BackgroundBottomSheetFragment {
            return BackgroundBottomSheetFragment()
        }
    }
}