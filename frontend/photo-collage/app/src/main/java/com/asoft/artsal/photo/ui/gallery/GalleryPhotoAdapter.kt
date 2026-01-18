package com.asoft.artsal.photo.ui.gallery

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.artsal.photo.editor.collage.maker.databinding.ItemPhotoGalleryBinding
import com.asoft.artsal.photo.data.model.GalleryPhoto
import com.asoft.artsal.photo.extensions.invisible
import com.asoft.artsal.photo.extensions.loadImageAssetsWithCompress
import com.asoft.artsal.photo.extensions.onClick
import com.asoft.artsal.photo.extensions.visible

class GalleryPhotoAdapter(
    private val onItemClick: (GalleryPhoto, Boolean) -> Unit
) : ListAdapter<GalleryPhoto, GalleryPhotoAdapter.VaultHideViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): VaultHideViewHolder {
        val binding =
            ItemPhotoGalleryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VaultHideViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VaultHideViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onBindViewHolder(
        holder: VaultHideViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            payloads.forEach { payload ->
                when (payload) {
                    is ItemCheckPayload.SelectItem -> holder.bindCheckState(payload.isSelected)
                }
            }
        }
    }

    inner class VaultHideViewHolder(private val binding: ItemPhotoGalleryBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(galleryPhoto: GalleryPhoto) {
            binding.root.onClick {
                onItemClick(galleryPhoto, !galleryPhoto.isSelected)
            }

            binding.imgItem.loadImageAssetsWithCompress(
                uri = galleryPhoto.uri,
                enableCompression = false
            )

            bindCheckState(galleryPhoto.isSelected)
        }

        internal fun bindCheckState(isSelected: Boolean) {
            binding.root.isSelected = isSelected
            binding.imgSelector.isSelected = isSelected

            if (isSelected) {
                binding.imgSelector.visible()
                binding.imgItem.strokeWidth = 10f
                val strokeColor = "#559EFF".toColorInt()
                binding.imgItem.strokeColor = ColorStateList.valueOf(strokeColor)

            } else {
                binding.imgSelector.invisible()
                binding.imgItem.strokeWidth = 0f
                binding.imgItem.strokeColor = null
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<GalleryPhoto>() {
        override fun areItemsTheSame(oldItem: GalleryPhoto, newItem: GalleryPhoto): Boolean {
            return oldItem.uri == newItem.uri
        }

        override fun areContentsTheSame(oldItem: GalleryPhoto, newItem: GalleryPhoto): Boolean {
            return oldItem == newItem
        }

        override fun getChangePayload(oldItem: GalleryPhoto, newItem: GalleryPhoto): Any? {
            return when {
                oldItem.isSelected != newItem.isSelected -> ItemCheckPayload.SelectItem(newItem.isSelected)
                else -> super.getChangePayload(oldItem, newItem)
            }
        }
    }
}

sealed class ItemCheckPayload {
    data class SelectItem(val isSelected: Boolean) : ItemCheckPayload()
}