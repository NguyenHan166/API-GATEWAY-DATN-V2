package com.asoft.artsal.photo.ui.template.adapter

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.artsal.photo.editor.collage.maker.databinding.AdsNativeMediumSquareBinding
import com.artsal.photo.editor.collage.maker.databinding.ItemNativeSquareAdsBinding
import com.artsal.photo.editor.collage.maker.databinding.ItemPhotoBinding
import com.asoft.artsal.photo.data.model.Template
import com.asoft.artsal.photo.extensions.gone
import com.asoft.artsal.photo.extensions.inflater
import com.asoft.artsal.photo.extensions.invisible
import com.asoft.artsal.photo.extensions.loadImageAssetsWithCompress
import com.asoft.artsal.photo.extensions.visible
import com.minsap.ad.ads.MinSapNativeAd
import com.minsap.ad.ads.common.NativeAdState
import com.minsap.ad.config.NativeAdValue
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.concurrent.Executors

@SuppressLint("TimberArgCount")
class SelectTemplateAdapter(
    private val onSelect: ((Template) -> Unit)? = null,
    callBack: DiffUtil.ItemCallback<SelectTemplateView> = DiffUtil()
) : ListAdapter<SelectTemplateView, RecyclerView.ViewHolder>(
    AsyncDifferConfig.Builder(callBack)
        .setBackgroundThreadExecutor(Executors.newSingleThreadExecutor()).build()
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {

            TYPE_CONTENT -> {
                AppViewHolder(ItemPhotoBinding.inflate(parent.inflater, parent, false))
            }

            TYPE_ADS -> {
                NativeAdsViewHolder(
                    itemBinding = ItemNativeSquareAdsBinding.inflate(
                        parent.inflater, parent, false
                    ),
                )
            }

            else -> {

            }
        } as RecyclerView.ViewHolder
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val data = getItem(position)) {
            is SelectTemplateView.NativeAds -> {
                (holder as? NativeAdsViewHolder)?.bind()
            }

            is SelectTemplateView.Content -> {
                (holder as? AppViewHolder)?.bind(data)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is SelectTemplateView.NativeAds -> TYPE_ADS
            is SelectTemplateView.Content -> TYPE_CONTENT
        }
    }

    inner class AppViewHolder(private var itemBinding: ItemPhotoBinding) :
        RecyclerView.ViewHolder(itemBinding.root) {

        fun bind(app: SelectTemplateView.Content) {
            val template = app.template
            itemBinding.root.setOnClickListener {
                onSelect?.invoke(template)
            }

            itemBinding.run {
                val params = imgPhoto.layoutParams
                if (params is ConstraintLayout.LayoutParams) {
                    params.dimensionRatio = "H,${template.radio}:1"
                }
                imgPhoto.loadImageAssetsWithCompress(
                    app.template.imagePath,
                    enableCompression = false,
//                    overrideWidth = if (app.template.typeView == TypeView.Landscape) 656 else null,
//                    overrideHeight = if (app.template.typeView == TypeView.Landscape) 345 else null
                )
            }
        }
    }

    inner class NativeAdsViewHolder(
        private val itemBinding: ItemNativeSquareAdsBinding,
    ) : RecyclerView.ViewHolder(itemBinding.root) {

        fun bind(
            createAdNative: MinSapNativeAd? = null,
            nativeAdValue: NativeAdValue? = null,
            adsBinding: AdsNativeMediumSquareBinding? = null,
            subscribe: MutableStateFlow<NativeAdState?>? = null,
        ) {
            createAdNative?.showNativeAd(
                frameAd = itemBinding.frameNative,
                adsBinding = adsBinding ?: return,
                nativeAdValue = nativeAdValue,
                subscribe = subscribe ?: return,
                isReload = true,
                isConfigRatio = false
            )
        }

        fun bindError() {
        }
    }

    companion object {
        const val TYPE_ADS = 0
        const val TYPE_CONTENT = 1
    }
}

class DiffUtil<Item : Any> : DiffUtil.ItemCallback<Item>() {
    override fun areItemsTheSame(oldItem: Item, newItem: Item): Boolean {
        if (oldItem is SelectTemplateView.Content && newItem is SelectTemplateView.Content) {
            return oldItem.template.id == newItem.template.id
        }
        return oldItem == newItem
    }

    @SuppressLint("DiffUtilEquals")
    override fun areContentsTheSame(oldItem: Item, newItem: Item): Boolean {
        if (oldItem is SelectTemplateView.Content && newItem is SelectTemplateView.Content) {
            return oldItem.template.imagePath == newItem.template.imagePath && oldItem.template.typeView == newItem.template.typeView
        }
        return oldItem == newItem
    }
}

sealed class SelectTemplateView {
    data class Content(val template: Template) : SelectTemplateView()
    data class NativeAds(val nativeAdValue: NativeAdValue? = null) : SelectTemplateView()
}
