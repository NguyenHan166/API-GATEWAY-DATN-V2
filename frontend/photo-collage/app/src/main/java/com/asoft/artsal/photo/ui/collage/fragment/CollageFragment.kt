package com.asoft.artsal.photo.ui.collage.fragment

import android.content.Context
import android.os.Build
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsetsController
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.viewpager2.widget.ViewPager2
import com.artsal.photo.editor.collage.maker.R
import com.artsal.photo.editor.collage.maker.databinding.FragmentCollageBinding
import com.artsal.photo.editor.collage.maker.databinding.ItemTabCollageBinding
import com.asoft.artsal.photo.base.BaseFragment
import com.asoft.artsal.photo.extensions.onClick
import com.asoft.artsal.photo.extensions.popBackStack
import com.asoft.artsal.photo.ui.collage.adapter.CollageVPAdapter
import com.asoft.artsal.photo.ui.home.viewmodel.HomeViewModel
import com.asoft.artsal.photo.utils.FirebaseEventUtils
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CollageFragment : BaseFragment<FragmentCollageBinding>() {
    override val isInsets: Boolean
        get() = false

    override val isLightStatusBar: Boolean
        get() = false

    private val homeViewModel by activityViewModels<HomeViewModel>()

    private var handlerBindView: Handler? = null
    private var tabBinding: ItemTabCollageBinding? = null
    private var collageVPAdapter: CollageVPAdapter? = null
    private val pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            binding?.viewPager2?.isUserInputEnabled = false
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
    }

    override fun onInflateView(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentCollageBinding {
        return FragmentCollageBinding.inflate(inflater, container, false)
    }


    override fun initListener() {
        binding?.viewPager2?.registerOnPageChangeCallback(pageChangeCallback)

        binding?.tabLayout?.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.let {

                    val customView = tab.customView
                    val tabText = customView?.findViewById<TextView>(R.id.tvTabName)
                    val tabBackground = customView?.findViewById<View>(R.id.root_tab_view)
                    tabText?.setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.white
                        )
                    )

                    tabBackground?.isSelected = true
                }
                when (tab?.text) {
                    "All" -> {
                        FirebaseEventUtils.logEventTracking(context, "collage_cate_all")
                    }

                    "2 photos" -> {
                        FirebaseEventUtils.logEventTracking(context, "collage_cate_2")
                    }

                    "3 photos" -> {
                        FirebaseEventUtils.logEventTracking(context, "collage_cate_3")
                    }

                    "4 photos" -> {
                        FirebaseEventUtils.logEventTracking(context, "collage_cate_4")
                    }

                    "5 photos" -> {
                        FirebaseEventUtils.logEventTracking(context, "collage_cate_5")
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                tab?.let {
                    val customView = tab.customView
                    val tabText = customView?.findViewById<TextView>(R.id.tvTabName)
                    val tabBackground = customView?.findViewById<View>(R.id.root_tab_view)
                    tabText?.setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.white_50
                        )
                    )
                    tabBackground?.isSelected = false
                }
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }
        })

        binding?.viewHeader?.btnBack?.onClick {
            popBackStack()
        }
    }

    override fun setupUi() {
        FirebaseEventUtils.logEventTracking(context, "collage_view")

        binding?.run {
            viewHeader.tvTitleTab.setText(R.string.collage)
        }

        collageVPAdapter = CollageVPAdapter(
            childFragmentManager,
            viewLifecycleOwner.lifecycle,
        )
        binding?.viewPager2?.adapter = collageVPAdapter
        binding?.viewPager2?.offscreenPageLimit = 1
        setupTabLayout()
    }

    override fun renderUi() {

    }

    private fun setupTabLayout() {
        val tabs = getTabs()
        val mediator = TabLayoutMediator(
            binding?.tabLayout ?: return,
            binding?.viewPager2 ?: return
        ) { tab, position ->
            tab.customView = createCustomTabView(tabs[position])
        }
        mediator.attach()
    }

    private fun createCustomTabView(title: String): View {
        val view = layoutInflater.inflate(R.layout.item_tab_collage, tabBinding?.root, false)
        val textView = view.findViewById<TextView>(R.id.tvTabName)
        textView.text = title
        return view
    }

    override fun onDestroyView() {
        handlerBindView?.removeCallbacksAndMessages(null)
        handlerBindView = null
        binding?.viewPager2?.unregisterOnPageChangeCallback(pageChangeCallback)
        super.onDestroyView()
    }

    private fun getTabs(): List<String> {
        return listOf(
            getString(R.string.tab_all_photos),
            getString(R.string.tab_2_photos),
            getString(R.string.tab_3_photos),
            getString(R.string.tab_4_photos),
            getString(R.string.tab_5_photos),
        )
    }

    override fun onResume() {
        super.onResume()
        homeViewModel.preloadNativeAdCollage()
    }

}