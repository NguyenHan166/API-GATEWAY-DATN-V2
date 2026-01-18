package com.asoft.artsal.photo.ui.ai.fragment

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.artsal.photo.editor.collage.maker.R
import com.artsal.photo.editor.collage.maker.databinding.FragmentResultGenBinding
import com.asoft.artsal.photo.base.BaseFragment
import com.asoft.artsal.photo.extensions.downloadAndSaveImageFromUrl
import com.asoft.artsal.photo.extensions.loadImageDrawable
import com.asoft.artsal.photo.extensions.loadImageFromUrl
import com.asoft.artsal.photo.extensions.loadImageFromUrlWithLoading
import com.asoft.artsal.photo.extensions.onClick
import com.asoft.artsal.photo.extensions.popBackStack
import com.asoft.artsal.photo.extensions.toast
import com.asoft.artsal.photo.ui.ai.viewmodel.ImageViewModel
import com.asoft.artsal.photo.utils.Const
import kotlinx.coroutines.launch
import timber.log.Timber


class ResultGenFragment : BaseFragment<FragmentResultGenBinding>() {
    override val isLightStatusBar: Boolean
        get() = false
    private val viewModel: ImageViewModel by activityViewModels<ImageViewModel>()


    override fun onInflateView(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentResultGenBinding {
        return FragmentResultGenBinding.inflate(inflater, container, false)
    }

    override fun initListener() {
        binding?.run {
            btnDownload.onClick {
                downloadResultImage()
            }
            btnBack.onClick {
                popBackStack()
            }
            btnHome.onClick {
                popBackStack(R.id.home_fragment)
            }
        }
    }

    override fun setupUi() {
        binding?.run {
            imgFacebook.loadImageDrawable(R.drawable.img_facebook)
            imgInstagram.loadImageDrawable(R.drawable.img_instagram)
            imgX.loadImageDrawable(R.drawable.img_twitter)
            imgMessenger.loadImageDrawable(R.drawable.img_messenger)
            imgMore.loadImageDrawable(R.drawable.img_more)
        }
    }

    override fun renderUi() {
        binding?.run {
            imgResult.loadImageFromUrlWithLoading(viewModel.imageUrlToDown)
        }
    }

    private fun downloadResultImage() {
        val resultUrl = viewModel.imageUrlToDown

        if (resultUrl.isNullOrBlank()) {
            requireContext().toast("Không có ảnh để lưu")
            Timber.w("downloadResultImage: No result URL available")
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            downloadAndSaveImageFromUrl(
                imageUrl = resultUrl,
                folder = "/${Const.APP_FOLDER}/${Const.STYLE_TRANSFER_FOLDER}",
                fileNamePrefix = "style_transfer",
                onLoading = { isLoading ->
                    showLoading(isLoading)
                },
                onSuccess = { filePath ->
                    Timber.d("downloadResultImage: Saved successfully to $filePath")
                },
                onFailure = { error ->
                    Timber.e("downloadResultImage: $error")
                }
            )
        }
    }

}