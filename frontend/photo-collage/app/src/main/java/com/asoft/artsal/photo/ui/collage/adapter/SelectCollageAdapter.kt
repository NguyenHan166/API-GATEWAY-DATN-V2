package com.asoft.artsal.photo.ui.collage.adapter

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.net.toUri
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import coil3.load
import coil3.util.CoilUtils
import com.artsal.photo.editor.collage.maker.databinding.AdsNativeMediumSquareBinding
import com.artsal.photo.editor.collage.maker.databinding.ItemNativeSquareAdsBinding
import com.artsal.photo.editor.collage.maker.databinding.ItemPhotoBinding
import com.asoft.artsal.photo.data.model.Collage
import com.asoft.artsal.photo.data.model.TypeView
import com.asoft.artsal.photo.extensions.gone
import com.asoft.artsal.photo.extensions.inflater
import com.asoft.artsal.photo.extensions.invisible
import com.asoft.artsal.photo.extensions.loadImageAssetsWithCompress
import com.asoft.artsal.photo.extensions.loadPhotoUri
import com.minsap.ad.ads.MinSapNativeAd
import com.minsap.ad.ads.common.NativeAdState
import com.minsap.ad.config.NativeAdValue
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.concurrent.Executors

@SuppressLint("TimberArgCount")
class SelectCollageAdapter(
    private val onSelect: ((Collage) -> Unit)? = null,
    private val onUnSelect: ((Collage) -> Unit)? = null,
    callBack: DiffUtil.ItemCallback<SelectView> = DiffUtil()
) : ListAdapter<SelectView, RecyclerView.ViewHolder>(
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
            is SelectView.NativeAds -> {
                (holder as? NativeAdsViewHolder)?.bind()
            }

            is SelectView.Content -> {
                val layoutParams = holder.itemView.layoutParams
                if (layoutParams is StaggeredGridLayoutManager.LayoutParams) {
                    layoutParams.isFullSpan = data.collage.typeView == TypeView.Landscape
                }
                (holder as? AppViewHolder)?.bind(data)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is SelectView.NativeAds -> TYPE_ADS
            is SelectView.Content -> TYPE_CONTENT
        }
    }

    inner class AppViewHolder(private var itemBinding: ItemPhotoBinding) :
        RecyclerView.ViewHolder(itemBinding.root) {

        fun bind(app: SelectView.Content) {
            val collage = app.collage
            itemBinding.root.setOnClickListener {
                onSelect?.invoke(collage)
            }

            itemBinding.run {
                val params = imgPhoto.layoutParams
                if (params is ConstraintLayout.LayoutParams) {
                    params.dimensionRatio = "H,${collage.radio}:1"
                }
                imgPhoto.loadImageAssetsWithCompress(
                    app.collage.imagePath,
                    enableCompression = false,
                    overrideWidth = if (app.collage.typeView == TypeView.Landscape) 656 else null,
                    overrideHeight = if (app.collage.typeView == TypeView.Landscape) 345 else null
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
        if (oldItem is SelectView.Content && newItem is SelectView.Content) {
            return oldItem.collage.id == newItem.collage.id
        }
        return oldItem == newItem
    }

    @SuppressLint("DiffUtilEquals")
    override fun areContentsTheSame(oldItem: Item, newItem: Item): Boolean {
        if (oldItem is SelectView.Content && newItem is SelectView.Content) {
            return oldItem.collage.imagePath == newItem.collage.imagePath && oldItem.collage.type == newItem.collage.type
        }
        return oldItem == newItem
    }
}

sealed class SelectView {
    data class Content(val collage: Collage) : SelectView()
    data class NativeAds(val nativeAdValue: NativeAdValue? = null) : SelectView()
}
