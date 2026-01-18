package com.asoft.artsal.photo.ui.myphoto.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.artsal.photo.editor.collage.maker.databinding.ItemMyPhotoBinding
import com.asoft.artsal.photo.data.model.Photo
import com.asoft.artsal.photo.extensions.inflater
import com.asoft.artsal.photo.extensions.loadImageAssetsWithCompress
import com.asoft.artsal.photo.extensions.loadImageDrawableWithCompress
import com.asoft.artsal.photo.extensions.onClick

class MyPhotoAdapter(
    private val onOptionClick: (Photo) -> Unit,
    private val onPhotoClick: (Photo) -> Unit
) : ListAdapter<Photo, MyPhotoAdapter.MyPhotoViewHolder>(PhotoDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyPhotoViewHolder {
        val binding = ItemMyPhotoBinding.inflate(parent.inflater, parent, false)
        return MyPhotoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyPhotoViewHolder, position: Int) {
        getItem(position)?.let { holder.bind(it) }
    }

    inner class MyPhotoViewHolder(private val binding: ItemMyPhotoBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Photo) {
            binding.imgMyPhotos.loadImageAssetsWithCompress(uri = item.uri)
            binding.tvDate.text = item.timeString
            binding.containerOptions.onClick { onOptionClick(item) }
            binding.imgMyPhotos.onClick { onPhotoClick(item) }
        }
    }

    private object PhotoDiffCallback : DiffUtil.ItemCallback<Photo>() {
        override fun areItemsTheSame(oldItem: Photo, newItem: Photo): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Photo, newItem: Photo): Boolean =
            oldItem == newItem
    }
}