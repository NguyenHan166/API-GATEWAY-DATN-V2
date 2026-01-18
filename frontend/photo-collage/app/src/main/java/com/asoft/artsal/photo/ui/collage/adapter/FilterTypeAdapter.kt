package com.asoft.artsal.photo.ui.collage.adapter

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.artsal.photo.editor.collage.maker.R
import com.artsal.photo.editor.collage.maker.databinding.ItemFilterTypeBinding
import com.asoft.artsal.photo.data.model.FilterType
import com.asoft.artsal.photo.data.model.FilterTypeItem
import com.asoft.artsal.photo.extensions.inflater
import com.asoft.artsal.photo.extensions.loadImageAssetsWithCompress
import com.asoft.artsal.photo.extensions.loadImageDrawableWithCompress

class FilterTypeAdapter(
    private val onSelect: ((FilterTypeItem) -> Unit)? = null,
) : ListAdapter<FilterTypeItem, FilterTypeAdapter.FilterTypeViewHolder>(FilterTypeDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = FilterTypeViewHolder(
        ItemFilterTypeBinding.inflate(parent.inflater, parent, false)
    )

    override fun onBindViewHolder(holder: FilterTypeViewHolder, position: Int) {
        getItem(position)?.let {
            holder.bind(it)
        }
    }

    override fun onBindViewHolder(
        holder: FilterTypeViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            payloads.forEach { payload ->
                when (payload) {
                    is FilterTypePayload.SelectChanged -> holder.bindSelectState(payload.isSelect)
                }
            }
        }
    }

    inner class FilterTypeViewHolder(private val itemBinding: ItemFilterTypeBinding) :
        RecyclerView.ViewHolder(itemBinding.root) {

        fun bind(filterType: FilterTypeItem) {
            itemBinding.root.setOnClickListener {
                onSelect?.invoke(filterType)
            }

            itemBinding.run {
                ivFilterThumbnail.loadImageDrawableWithCompress(filterType.res)
                tvFilterName.text = getFilterTypeName(filterType.type, itemBinding.root.context)
                bindSelectState(false)
            }
        }

        internal fun bindSelectState(selected: Boolean) {
            itemBinding.root.isSelected = selected
        }
    }

    private fun getFilterTypeName(filterType: FilterType, context: Context): String {
        return when (filterType) {
            FilterType.Moody ->
                context.getString(R.string.moody)
            FilterType.Nature -> context.getString(R.string.nature)
            FilterType.Portrait -> context.getString(R.string.portrait)
            FilterType.BAndW -> context.getString(R.string.black_and_white)
            FilterType.Cinematic -> context.getString(R.string.cinematic)
            FilterType.Landscape -> context.getString(R.string.landscape)
            FilterType.LifeStyle -> context.getString(R.string.lifestyle)
        }
    }

    private object FilterTypeDiffCallback : DiffUtil.ItemCallback<FilterTypeItem>() {
        override fun areItemsTheSame(oldItem: FilterTypeItem, newItem: FilterTypeItem): Boolean {
            return oldItem.type == newItem.type
        }

        override fun areContentsTheSame(oldItem: FilterTypeItem, newItem: FilterTypeItem): Boolean {
            return oldItem == newItem
        }

        override fun getChangePayload(oldItem: FilterTypeItem, newItem: FilterTypeItem): Any? {
            return when {
                oldItem.type != newItem.type -> FilterTypePayload.SelectChanged(false)
                else -> super.getChangePayload(oldItem, newItem)
            }
        }
    }
}

sealed class FilterTypePayload {
    data class SelectChanged(val isSelect: Boolean) : FilterTypePayload()
} 