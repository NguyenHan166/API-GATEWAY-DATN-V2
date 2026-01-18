package com.asoft.artsal.photo.ui.gallery

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.artsal.photo.editor.collage.maker.R
import com.artsal.photo.editor.collage.maker.databinding.ItemPhotoSelectedBinding
import com.asoft.artsal.photo.data.model.GalleryPhoto
import com.asoft.artsal.photo.extensions.loadImageAssetsWithCompress
import com.asoft.artsal.photo.extensions.onClick

class SelectedPhotosAdapter(
    private val onItemClick: (GalleryPhoto) -> Unit
) : ListAdapter<GalleryPhoto, SelectedPhotosAdapter.SelectedPhotoViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectedPhotoViewHolder {
        val binding =
            ItemPhotoSelectedBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SelectedPhotoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SelectedPhotoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SelectedPhotoViewHolder(private val binding: ItemPhotoSelectedBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(galleryPhoto: GalleryPhoto) {
            binding.root.onClick {
                onItemClick(galleryPhoto)
            }

            binding.imgItem.loadImageAssetsWithCompress(
                uri = galleryPhoto.uri,
                enableCompression = false
            )

            val params = binding.root.layoutParams
            val size = binding.root.context.resources.getDimensionPixelSize(R.dimen.dp_60)
            params.width = size
            params.height = size
            binding.root.layoutParams = params
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<GalleryPhoto>() {
        override fun areItemsTheSame(oldItem: GalleryPhoto, newItem: GalleryPhoto): Boolean {
            return oldItem.uri == newItem.uri
        }

        override fun areContentsTheSame(oldItem: GalleryPhoto, newItem: GalleryPhoto): Boolean {
            return oldItem == newItem
        }
    }
}
