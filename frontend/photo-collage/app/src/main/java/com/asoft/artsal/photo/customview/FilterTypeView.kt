package com.asoft.artsal.photo.customview

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.artsal.photo.editor.collage.maker.databinding.ViewFilterTypeBinding
import com.asoft.artsal.photo.data.filterTypes
import com.asoft.artsal.photo.data.model.Filter
import com.asoft.artsal.photo.data.model.FilterTypeItem
import com.asoft.artsal.photo.extensions.gone
import com.asoft.artsal.photo.extensions.initRecyclerViewAdapter
import com.asoft.artsal.photo.extensions.invisible
import com.asoft.artsal.photo.extensions.visible
import com.asoft.artsal.photo.ui.collage.adapter.FilterAdapter
import com.asoft.artsal.photo.ui.collage.adapter.FilterTypeAdapter
import com.asoft.artsal.photo.utils.FirebaseEventUtils
import timber.log.Timber

class FilterTypeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding: ViewFilterTypeBinding =
        ViewFilterTypeBinding.inflate(LayoutInflater.from(context), this, true)

    private val filterTypeAdapter by lazy {
        FilterTypeAdapter { filterType ->
            onFilterTypeSelected(filterType)
        }
    }

    interface OnFilterTypeListener {
        fun onCloseClicked(isReset: Boolean = false)
        fun onFilterTypeSelected(filterType: FilterTypeItem)
        fun onFilterSelected(filter: Filter)
    }

    private var listener: OnFilterTypeListener? = null
    private val filterAdapter by lazy { FilterAdapter {
        filter -> listener?.onFilterSelected(filter)
        binding.header.imgDone.visible()
    } }

    init {
        setupUI()
        setupListeners()
    }

    private fun setupUI() {
        binding.header.imgDone.gone()
        binding.rvFilterType.initRecyclerViewAdapter(
            yourAdapter = filterTypeAdapter,
            yourLayoutManager = LinearLayoutManager(
                context, LinearLayoutManager.HORIZONTAL, false
            ),
            fixedSize = true,
        )

        filterTypeAdapter.submitList(filterTypes)
        binding.containerFilterItem.gone()
        binding.rvFilter.initRecyclerViewAdapter(
            yourAdapter = filterAdapter,
            yourLayoutManager = LinearLayoutManager(
                context, LinearLayoutManager.HORIZONTAL, false
            ),
            fixedSize = true,
        )

        filterAdapter.setOnScrollToPositionListener { position ->
            binding.rvFilter.smoothScrollToPosition(position)
        }
    }


    private fun setupListeners() {
        binding.header.apply {
            imgDone.setOnClickListener {
                FirebaseEventUtils.logEventTracking(context, "filter_save")
                // Disable button to prevent multiple clicks
                isEnabled = false
                resetState()
                // Re-enable after a short delay
                postDelayed({ isEnabled = true }, 500)
            }
            imgClose.setOnClickListener {
                FirebaseEventUtils.logEventTracking(context, "filter_close")
                // Disable button to prevent multiple clicks
                isEnabled = false
                resetState(true)
                // Re-enable after a short delay
                postDelayed({ isEnabled = true }, 500)
            }
        }

        binding.btnBack.setOnClickListener {
            filterAdapter.submitList(null)
            binding.run {
                containerFilterItem.invisible()
                rvFilterType.visible()
            }
        }
    }

    private fun onFilterTypeSelected(filterType: FilterTypeItem) {
        Timber.d("Filter type selected: ${filterType.type}")
        binding.containerFilterItem.visible()
        listener?.onFilterTypeSelected(filterType)
    }

    fun setOnFilterTypeListener(listener: OnFilterTypeListener) {
        this.listener = listener
    }

    fun show() {
        visibility = VISIBLE
    }

    fun hide() {
        visibility = GONE
    }

    fun setFilters(filters: List<Filter>) {
        filterAdapter.submitList(filters)
        binding.rvFilterType.invisible()
        filterAdapter.setCurrentFilter(null)
    }

    fun setCurrentSelectedFilter(filter: Filter?, shouldScroll: Boolean = true) {
        filterAdapter.setCurrentFilter(filter, shouldScroll)
    }

    fun resetState(isDeleteFilter: Boolean = false) {
        listener?.onCloseClicked(isDeleteFilter)
        hide()
        binding.run {
            containerFilterItem.gone()
            header.imgDone.gone()
            rvFilterType.visible()
        }
        filterAdapter.submitList(null)
    }
} 