package com.asoft.artsal.photo.ui.collage.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.artsal.photo.editor.collage.maker.databinding.ItemFilterBinding
import com.asoft.artsal.photo.data.model.Filter
import com.asoft.artsal.photo.extensions.gone
import com.asoft.artsal.photo.extensions.inflater
import com.asoft.artsal.photo.extensions.loadImageAssetsWithCompress
import com.asoft.artsal.photo.extensions.visible

class FilterAdapter(
    private val onSelect: ((Filter) -> Unit)? = null,
) : ListAdapter<Filter, FilterAdapter.FilterViewHolder>(FilterDiffCallback) {

    private var selectedPosition = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = FilterViewHolder(
        ItemFilterBinding.inflate(parent.inflater, parent, false)
    )

    override fun onBindViewHolder(holder: FilterViewHolder, position: Int) {
        getItem(position)?.let { filter ->
            holder.bind(filter, position == selectedPosition)
        }
    }

    inner class FilterViewHolder(private val binding: ItemFilterBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val filter = getItem(position)
                    val oldPosition = selectedPosition
                    selectedPosition = position
                    notifyItemChanged(oldPosition)
                    notifyItemChanged(selectedPosition)
                    onSelect?.invoke(filter)
                }
            }
        }

        fun bind(filter: Filter, isSelected: Boolean) {
            binding.ivFilterPreview.loadImageAssetsWithCompress(filter.imagePath)

            if (isSelected) {
                binding.viewFilterSelected.visible()
            } else {
                binding.viewFilterSelected.gone()
            }
        }
    }

    fun setCurrentFilter(filter: Filter?, scrollToPosition: Boolean = false) {
        if (filter == null) {
            val oldPosition = selectedPosition
            selectedPosition = -1
            notifyItemChanged(oldPosition)
            return
        }

        val position = currentList.indexOfFirst { it.id == filter.id }
        if (position != -1) {
            val oldPosition = selectedPosition
            selectedPosition = position
            notifyItemChanged(oldPosition)
            notifyItemChanged(selectedPosition)

            if (scrollToPosition) {
                onScrollToPositionRequested?.invoke(position)
            }
        }
    }

    private var onScrollToPositionRequested: ((Int) -> Unit)? = null

    fun setOnScrollToPositionListener(listener: (Int) -> Unit) {
        onScrollToPositionRequested = listener
    }

    private object FilterDiffCallback : DiffUtil.ItemCallback<Filter>() {
        override fun areItemsTheSame(oldItem: Filter, newItem: Filter): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Filter, newItem: Filter): Boolean {
            return oldItem == newItem
        }
    }
}


