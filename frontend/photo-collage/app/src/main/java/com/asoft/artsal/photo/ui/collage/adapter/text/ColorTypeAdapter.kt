package com.asoft.artsal.photo.ui.collage.adapter.text

import android.annotation.SuppressLint
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.artsal.photo.editor.collage.maker.R
import com.artsal.photo.editor.collage.maker.databinding.ItemColorCircleBinding
import com.asoft.artsal.photo.extensions.setSize

class ColorTypeAdapter(
    private val onColorSelect: ((Int) -> Unit)? = null,
) : ListAdapter<Int, ColorTypeAdapter.ColorViewHolder>(ColorDiffCallback()) {

    private var selectedPosition = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorViewHolder {
        val binding = ItemColorCircleBinding.inflate(
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
        private val binding: ItemColorCircleBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val oldPosition = selectedPosition
                    selectedPosition = position
                    notifyItemChanged(oldPosition)
                    notifyItemChanged(selectedPosition)
                    onColorSelect?.invoke(getItem(position))
                }
            }
        }

        @SuppressLint("UseCompatLoadingForDrawables")
        fun bind(color: Int, isSelected: Boolean) {
            var drawable = GradientDrawable()
            val oldSize = binding.root.context.resources.getDimensionPixelSize(R.dimen.dp_20)
            val newSize = binding.root.context.resources.getDimensionPixelSize(R.dimen.dp_22)
            binding.colorView.setBackgroundColor(color)
            when {
                isSelected -> {
                    binding.colorView.setSize(newSize, newSize)
                    drawable = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setSize(newSize, newSize)
                        setColor(color)
                        setStroke(
                            (2 * binding.root.context.resources.displayMetrics.density).toInt(),
                            "#89CFF0".toColorInt()
                        )
                    }
                    binding.colorView.background = drawable
                    binding.borderDefault.visibility = android.view.View.GONE
                }

                color == android.graphics.Color.WHITE -> {
                    binding.colorView.setSize(oldSize, oldSize)
                    binding.colorView.setBackgroundColor(color)
                    binding.borderDefault.visibility = android.view.View.VISIBLE
                }

                else -> {
                    binding.colorView.setSize(oldSize, oldSize)
                    binding.colorView.isSelected = false
                    binding.colorView.setBackgroundColor(color)
                    drawable = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(color)
                    }
                    binding.colorView.background = drawable
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