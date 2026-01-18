package com.asoft.artsal.photo.base

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsetsController
import androidx.activity.OnBackPressedCallback
import androidx.annotation.CallSuper
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.fragment.findNavController
import androidx.viewbinding.ViewBinding
import com.asoft.artsal.photo.ui.ads.PrepareLoadingAdDialog
import timber.log.Timber

abstract class BaseFragment<B : ViewBinding> : Fragment() {
    private var _binding: B? = null
    val binding: B?
        get() = _binding

    protected open val isInsets = true

    protected open val isLightStatusBar = true

    // Loading Dialog (lazy initialization)
    private val loadingDialog by lazy { LoadingDialog(requireContext()) }
    private val loadingAdsDialog by lazy { PrepareLoadingAdDialog(requireContext()) }

    // Lifecycle Observer for logging
    private val lifecycleObserver = object : DefaultLifecycleObserver {
        override fun onCreate(owner: LifecycleOwner) {
            Timber.tag("[LIFECYCLE_DEBUG]").d("${this@BaseFragment}::onCreate")
        }

        override fun onStart(owner: LifecycleOwner) {
            Timber.tag("[LIFECYCLE_DEBUG]").d("${this@BaseFragment}::onStart")
        }

        override fun onResume(owner: LifecycleOwner) {
            Timber.tag("[LIFECYCLE_DEBUG]").d("${this@BaseFragment}::onResume")
        }

        override fun onPause(owner: LifecycleOwner) {
            Timber.tag("[LIFECYCLE_DEBUG]").d("${this@BaseFragment}::onPause")
        }

        override fun onStop(owner: LifecycleOwner) {
            Timber.tag("[LIFECYCLE_DEBUG]").d("${this@BaseFragment}::onStop")
        }

        override fun onDestroy(owner: LifecycleOwner) {
            Timber.tag("[LIFECYCLE_DEBUG]").d("${this@BaseFragment}::onDestroy")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.tag("[VIEW_DEBUG]").d("${this@BaseFragment}::onCreate")
        // Add lifecycle observer for Fragment lifecycle
        lifecycle.addObserver(lifecycleObserver)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Timber.tag("[VIEW_DEBUG]").d("${this@BaseFragment}::onCreateView")
        val binding = onInflateView(inflater, container)
        _binding = binding
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.tag("[VIEW_DEBUG]").d("${this@BaseFragment}::onViewCreated")
//        activity?.hideNavigationBar()
        if (isInsets) {
            ViewCompat.setOnApplyWindowInsetsListener(binding?.root ?: return) { v, insets ->
                val originalPaddingLeft = v.paddingLeft
                val originalPaddingTop = v.paddingTop
                val originalPaddingRight = v.paddingRight
                val originalPaddingBottom = v.paddingBottom

                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                val navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
                v.setPadding(
                    systemBars.left + originalPaddingLeft,
                    systemBars.top,
                    systemBars.right + originalPaddingRight,
                    navBars.bottom
                )
                insets
            }
        }
        if (isLightStatusBar) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                activity?.window?.insetsController?.setSystemBarsAppearance(
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                )
            } else {
                @Suppress("DEPRECATION")
                activity?.window?.decorView?.systemUiVisibility =
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                activity?.window?.insetsController?.setSystemBarsAppearance(
                    0,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                )
            } else {
                @Suppress("DEPRECATION")
                activity?.window?.decorView?.systemUiVisibility =
                    activity?.window?.decorView?.systemUiVisibility?.and(
                        View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
                    ) ?: 0
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    handleBackPress()
                }
            }
        )
        initListener()
        setupUi()
        renderUi()
    }

    override fun onResume() {
        super.onResume()
        Timber.tag("[VIEW_DEBUG]").d("${this@BaseFragment}::onResume")
    }

    override fun onPause() {
        super.onPause()
        Timber.tag("[VIEW_DEBUG]").d("${this@BaseFragment}::onPause")
    }

    override fun onStop() {
        super.onStop()
        Timber.tag("[VIEW_DEBUG]").d("${this@BaseFragment}::onStop")
    }

    @CallSuper
    override fun onDestroyView() {
        super.onDestroyView()
        Timber.tag("[VIEW_DEBUG]").d("${this@BaseFragment}::onDestroyView")
        _binding = null
        if (loadingDialog.isShowing) {
            loadingDialog.dismiss()
        }
        if (loadingAdsDialog.isShowing) {
            loadingAdsDialog.dismiss()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.tag("[VIEW_DEBUG]").d("${this@BaseFragment}::onDestroy")
    }

    protected abstract fun onInflateView(inflater: LayoutInflater, container: ViewGroup?): B

    protected abstract fun initListener()

    protected abstract fun setupUi()

    protected abstract fun renderUi()

    protected open fun handleBackPress() {
        if (!findNavController().popBackStack()) {
            // If backstack is empty, finish Activity
            requireActivity().finish()
        }
    }

    fun handleException(throwable: Throwable) {
        Timber.d("handleException: ${throwable.message}")
    }

    fun showLoading(show: Boolean) {
        if (show && !loadingDialog.isShowing && !isStateSaved) {
            loadingDialog.show()
        } else if (!show && loadingDialog.isShowing) {
            loadingDialog.dismiss()
        }
    }

    fun showLoadingAds(show: Boolean) {
        if (show && !loadingAdsDialog.isShowing && !isStateSaved) {
            loadingAdsDialog.show()
        } else if (!show && loadingAdsDialog.isShowing) {
            loadingAdsDialog.dismiss()
        }
    }
}