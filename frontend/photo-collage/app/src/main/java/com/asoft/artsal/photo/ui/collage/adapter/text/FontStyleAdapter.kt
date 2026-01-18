package com.asoft.artsal.photo.ui.collage.adapter.text

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.artsal.photo.editor.collage.maker.databinding.ItemFontStyleBinding
import com.asoft.artsal.photo.data.model.FontType

class FontStyleAdapter(
    private val onSelect: ((FontType) -> Unit)? = null,
) : ListAdapter<FontType, FontStyleAdapter.FontViewHolder>(FontDiffCallback()) {

    private var selectedPosition = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FontViewHolder {
        val binding = ItemFontStyleBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FontViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FontViewHolder, position: Int) {
        if (selectedPosition == position) {
            holder.itemView.requestFocus();
        }
        holder.bind(getItem(position), position == selectedPosition)
    }

    inner class FontViewHolder(
        private val binding: ItemFontStyleBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val oldPosition = selectedPosition
                    selectedPosition = position
                    notifyItemChanged(oldPosition)
                    notifyItemChanged(selectedPosition)
                    onSelect?.invoke(getItem(position))
                }
            }
        }

        @SuppressLint("UseCompatLoadingForDrawables")
        fun bind(font: FontType, isSelected: Boolean) {
            binding.tvFontName.typeface =
                ResourcesCompat.getFont(binding.root.context, font.font)
            binding.tvFontName.text = font.showName
            when {
                isSelected -> {
                    binding.root.isSelected = true
                }

                else -> {
                    binding.root.isSelected = false
                }
            }
        }
    }

    fun setCurrentFont(font: FontType) {
        val position = currentList.indexOf(font)
        if (position != -1) {
            val oldPosition = selectedPosition
            selectedPosition = position
            notifyItemChanged(oldPosition)
            notifyItemChanged(selectedPosition)
        }
    }

    val selectedPositionPublic: Int
        get() = selectedPosition

    private class FontDiffCallback : DiffUtil.ItemCallback<FontType>() {
        override fun areItemsTheSame(oldItem: FontType, newItem: FontType): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: FontType, newItem: FontType): Boolean {
            return oldItem == newItem
        }
    }
}