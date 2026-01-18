package com.asoft.artsal.photo.ui.collage.adapter

import android.annotation.SuppressLint
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.artsal.photo.editor.collage.maker.databinding.ItemBackgroundColorBinding

class ColorAdapter(
    private val onColorSelected: (Int) -> Unit
) : ListAdapter<Int, ColorAdapter.ColorViewHolder>(ColorDiffCallback()) {

    private var selectedPosition = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorViewHolder {
        val binding = ItemBackgroundColorBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ColorViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ColorViewHolder, position: Int) {
        if (selectedPosition == position) {
            holder.itemView.requestFocus();
        }
        holder.bind(getItem(position), position == selectedPosition)
    }

    inner class ColorViewHolder(
        private val binding: ItemBackgroundColorBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val oldPosition = selectedPosition
                    selectedPosition = position
                    notifyItemChanged(oldPosition)
                    notifyItemChanged(selectedPosition)
                    onColorSelected(getItem(position))
                }
            }
        }

        @SuppressLint("UseCompatLoadingForDrawables")
        fun bind(color: Int, isSelected: Boolean) {
            when {
                isSelected -> {
                    val drawable = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        setColor(color)
                        setStroke(
                            (1.5 * binding.root.context.resources.displayMetrics.density).toInt(),
                            "#89CFF0".toColorInt()
                        )
                    }
                    binding.colorView.background = drawable
                    binding.borderDefault.visibility = android.view.View.GONE
                }

                color == android.graphics.Color.WHITE -> {
                    binding.colorView.setBackgroundColor(color)
                    binding.borderDefault.visibility = android.view.View.VISIBLE
                }

                else -> {
                    binding.colorView.setBackgroundColor(color)
                    binding.borderDefault.visibility = android.view.View.GONE
                }
            }
        }
    }

    fun setCurrentColor(color: Int) {
        val position = currentList.indexOf(color)
        if (position != -1) {
            val oldPosition = selectedPosition
            selectedPosition = position
            notifyItemChanged(oldPosition)
            notifyItemChanged(selectedPosition)
        }
    }

    val selectedPositionPublic: Int
        get() = selectedPosition

    private class ColorDiffCallback : DiffUtil.ItemCallback<Int>() {
        override fun areItemsTheSame(oldItem: Int, newItem: Int): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: Int, newItem: Int): Boolean {
            return oldItem == newItem
        }
    }
} 