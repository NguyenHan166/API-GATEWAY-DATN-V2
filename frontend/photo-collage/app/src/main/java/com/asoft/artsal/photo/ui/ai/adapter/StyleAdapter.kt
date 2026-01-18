package com.asoft.artsal.photo.ui.ai.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.artsal.photo.editor.collage.maker.databinding.ItemStyleBinding
import com.artsal.photo.editor.collage.maker.databinding.ItemStyleComicBinding
import com.asoft.artsal.photo.extensions.inflater
import com.asoft.artsal.photo.extensions.loadImageDrawableWithCompress

data class StyleItem(
    val id: String,
    val name: String,
    val imageRes: Int,
    val styleValue: String? = null // Value to send to API (e.g., "anime", "cinematic", etc.)
)

class StyleAdapter(
    private val onItemClick: (StyleItem) -> Unit
) : ListAdapter<StyleItem, StyleAdapter.StyleViewHolder>(DiffCallback) {

    private var selectedPosition: Int = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StyleViewHolder {
        val binding = ItemStyleBinding.inflate(parent.inflater, parent, false)
        return StyleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StyleViewHolder, position: Int) {
        holder.bind(getItem(position), position == selectedPosition)
    }

    fun selectStyle(position: Int) {
        val oldPosition = selectedPosition
        selectedPosition = position
        notifyItemChanged(oldPosition)
        notifyItemChanged(selectedPosition)
    }

    inner class StyleViewHolder(
        private val binding: ItemStyleBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(style: StyleItem, isSelected: Boolean) {
            binding.apply {
                // Set style name
                tvStyleName.text = style.name

                // Load image
                imgStyleThumbnail.loadImageDrawableWithCompress(
                    drawableRes = style.imageRes,
                    enableCompression = false
                )

                // Set selected state
                containerThumbnail.isSelected = isSelected
                imgCheckSelected.visibility = if (isSelected) android.view.View.VISIBLE else android.view.View.GONE

                // Set click listener
                root.setOnClickListener {
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        selectStyle(position)
                        onItemClick(style)
                    }
                }
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<StyleItem>() {
        override fun areItemsTheSame(oldItem: StyleItem, newItem: StyleItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: StyleItem, newItem: StyleItem): Boolean {
            return oldItem == newItem
        }
    }
}

