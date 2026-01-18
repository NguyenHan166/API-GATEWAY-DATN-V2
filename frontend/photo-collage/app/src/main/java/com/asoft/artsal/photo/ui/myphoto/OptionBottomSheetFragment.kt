package com.asoft.artsal.photo.ui.myphoto

import android.app.Activity
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import com.artsal.photo.editor.collage.maker.R
import com.artsal.photo.editor.collage.maker.databinding.BottomSheetOptionBinding
import com.asoft.artsal.photo.base.BaseBottomSheet
import com.asoft.artsal.photo.extensions.collectIn
import com.asoft.artsal.photo.extensions.loadImageAssetsWithCompress
import com.asoft.artsal.photo.extensions.onClick
import com.asoft.artsal.photo.extensions.toast
import com.asoft.artsal.photo.ui.myphoto.viewmodel.MyPhotoViewModel
import com.asoft.artsal.photo.utils.FirebaseEventUtils
import com.asoft.artsal.photo.utils.ImageSharingHelper
import com.asoft.artsal.photo.utils.StorageUtils
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale


@AndroidEntryPoint
class OptionBottomSheetFragment : BaseBottomSheet<BottomSheetOptionBinding>() {

    private lateinit var imageSharingHelper: ImageSharingHelper
    private val myViewModel by activityViewModels<MyPhotoViewModel>()
    private val deletePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            myViewModel.onDeletePermissionGranted()
            onDeleteItem?.invoke()
            dismiss()
        }
    }
    var onDeleteItem: (() -> Unit)? = null

    override fun getTheme(): Int {
        return R.style.KeyboardInputStyle
    }

    override fun onInflateView(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): BottomSheetOptionBinding {
        return BottomSheetOptionBinding.inflate(layoutInflater, container, false)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.apply {
            setFlags(
                WindowManager.LayoutParams.FLAG_DIM_BEHIND,
                WindowManager.LayoutParams.FLAG_DIM_BEHIND
            )
            setDimAmount(0.5f)
        }
        return dialog
    }

    override fun setupUi() {

        imageSharingHelper = ImageSharingHelper(requireContext())

        myViewModel.deleteResult.collectIn(this, Lifecycle.State.STARTED) { result ->
            when (result) {
                is StorageUtils.DeleteResult.Success -> {
                    dismiss()
                }

                is StorageUtils.DeleteResult.RequirePermission -> {
                    val request = IntentSenderRequest.Builder(result.intentSender).build()
                    deletePermissionLauncher.launch(request)
                }

                is StorageUtils.DeleteResult.Error -> {
                }

                is StorageUtils.DeleteResult.NotFound -> {
                    toast(getString(R.string.image_not_found_in_system))
                    dismiss()
                }

                null -> {}
            }
        }

        binding?.run {
            rowDelete.onClick {
                FirebaseEventUtils.logEventTracking(context, "myphoto_click_delete")

                DeleteConfirmDialog.newInstance {
                    myViewModel.deletePhoto()
                    onDeleteItem?.invoke()
                }.show(parentFragmentManager, DeleteConfirmDialog::class.java.name)
            }
            rowShare.onClick {
                FirebaseEventUtils.logEventTracking(context, "myphoto_click_share")
                imageSharingHelper.shareImageToApps(
                    requireContext(),
                    image = myViewModel.photoSelected.value?.uri ?: return@onClick
                )
            }
        }

        myViewModel.photoSelected.collectIn(
            fragment = this,
            action = {
                binding?.run {
                    imgPreview.loadImageAssetsWithCompress(uri = it?.uri ?: return@collectIn)
                    tvCreated.text = String.format(
                        Locale.getDefault(),
                        "%s %s",
                        getString(R.string.created),
                        it.timeString,
                    )
                }
            }
        )
    }

    override fun onDestroyView() {
        deletePermissionLauncher.unregister()
        myViewModel.clearDeleteResult()
        super.onDestroyView()
    }

    companion object {
        fun newInstance(): OptionBottomSheetFragment {
            return OptionBottomSheetFragment()
        }
    }
}