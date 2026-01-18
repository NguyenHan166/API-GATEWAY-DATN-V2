package com.asoft.artsal.photo.ui.collage.adapter

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.artsal.photo.editor.collage.maker.R
import com.artsal.photo.editor.collage.maker.databinding.ItemStickerBinding
import com.asoft.artsal.photo.data.model.Sticker
import com.asoft.artsal.photo.extensions.inflater
import com.asoft.artsal.photo.extensions.loadImageAssetsWithCompress
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import timber.log.Timber

class StickerAdapter(
    private val onSelect: ((Bitmap) -> Unit)? = null,
) : ListAdapter<Sticker, StickerAdapter.StickerViewHolder>(StickerDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = StickerViewHolder(
        ItemStickerBinding.inflate(parent.inflater, parent, false)
    )

    override fun onBindViewHolder(holder: StickerViewHolder, position: Int) {
        getItem(position)?.let {
            holder.bind(it)
        }
    }

    override fun onBindViewHolder(
        holder: StickerViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            payloads.forEach { payload ->
                when (payload) {
                    is StickerPayload.SelectChanged -> holder.bindCheckState(payload.isSelect)
                }
            }
        }
    }

    inner class StickerViewHolder(private val itemBinding: ItemStickerBinding) :
        RecyclerView.ViewHolder(itemBinding.root) {

        fun bind(sticker: Sticker) {
            Timber.d("Binding sticker: ${sticker.name} with path: ${sticker.path}")
            
            itemView.setOnClickListener {
                Timber.d("Sticker clicked: ${sticker.name}")
                Glide.with(itemView.context)
                    .asBitmap()
                    .load(sticker.path.toUri())
                    .into(object : CustomTarget<Bitmap?>() {
                        override fun onResourceReady(
                            resource: Bitmap,
                            transition: Transition<in Bitmap?>?
                        ) {
                            onSelect?.invoke(resource)
                        }
                        override fun onLoadCleared(placeholder: Drawable?) {
                        }
                        override fun onLoadFailed(errorDrawable: Drawable?) {
                        }
                    })
            }

            itemBinding.run {
                imgSticker.loadImageAssetsWithCompress(sticker.path, enableCompression = false)
            }
        }

        internal fun bindCheckState(checked: Boolean) {
            itemBinding.root.isSelected = checked
            itemBinding.imgSticker.isSelected = checked
        }
    }

    private object StickerDiffCallback : DiffUtil.ItemCallback<Sticker>() {
        override fun areItemsTheSame(oldItem: Sticker, newItem: Sticker): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Sticker, newItem: Sticker): Boolean {
            return oldItem.path == newItem.path
        }

        override fun getChangePayload(oldItem: Sticker, newItem: Sticker): Any? {
            return when {
                else -> super.getChangePayload(oldItem, newItem)
            }
        }
    }
}

sealed class StickerPayload {
    data class SelectChanged(val isSelect: Boolean) : StickerPayload()
}