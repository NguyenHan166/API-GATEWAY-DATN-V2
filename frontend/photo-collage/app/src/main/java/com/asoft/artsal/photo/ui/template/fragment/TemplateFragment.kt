package com.asoft.artsal.photo.ui.template.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.fragment.app.activityViewModels
import androidx.viewpager2.widget.ViewPager2
import com.artsal.photo.editor.collage.maker.R
import com.artsal.photo.editor.collage.maker.databinding.FragmentTemplateBinding
import com.artsal.photo.editor.collage.maker.databinding.ItemTabTemplateBinding
import com.asoft.artsal.photo.base.BaseFragment
import com.asoft.artsal.photo.data.model.TemplateType
import com.asoft.artsal.photo.extensions.collectIn
import com.asoft.artsal.photo.extensions.invisible
import com.asoft.artsal.photo.extensions.onClick
import com.asoft.artsal.photo.extensions.popBackStack
import com.asoft.artsal.photo.extensions.visible
import com.asoft.artsal.photo.ui.home.viewmodel.HomeViewModel
import com.asoft.artsal.photo.ui.template.adapter.TemplateVPAdapter
import com.asoft.artsal.photo.utils.FirebaseEventUtils
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class TemplateFragment : BaseFragment<FragmentTemplateBinding>() {

    override val isInsets: Boolean
        get() = false

    override val isLightStatusBar: Boolean
        get() = false
    private val homeViewModel: HomeViewModel by activityViewModels<HomeViewModel>()

    private var handlerBindView: Handler? = null
    private var tabBinding: ItemTabTemplateBinding? = null
    private var templateVPAdapter: TemplateVPAdapter? = null
    private var currentSelectedChipId: Int = R.id.trending
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
    ): FragmentTemplateBinding {
        return FragmentTemplateBinding.inflate(inflater, container, false)
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
                    tabText?.alpha = 1f
                    tabBackground?.isSelected = true
                    when (tab.position) {
                        0 -> {
                            homeViewModel.updateCurrentTab(TemplateType.Trending)
                            setDefaultSelection(R.id.trending)
                        }

                        1 -> {
                            homeViewModel.updateCurrentTab(TemplateType.All)
                            setDefaultSelection(R.id.all)
                        }

                        2 -> {
                            val type = homeViewModel.getMutableTypeSelected()
                            homeViewModel.updateCurrentTab(type)
                            when (type) {
                                TemplateType.Birthdays -> setDefaultSelection(R.id.birthday)
                                TemplateType.Holidays -> setDefaultSelection(R.id.holiday)
                                TemplateType.Travel -> setDefaultSelection(R.id.travel)
                                TemplateType.Seasons -> setDefaultSelection(R.id.season)
                                TemplateType.Milestones -> setDefaultSelection(R.id.milestone)
                                TemplateType.Moments -> setDefaultSelection(R.id.moment)
                                else -> {}
                            }

                        }

                        3 -> {
                            homeViewModel.updateCurrentTab(TemplateType.SpecialDays)
                            setDefaultSelection(R.id.special)
                        }
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                tab?.let {
                    val customView = tab.customView
                    val tabText = customView?.findViewById<TextView>(R.id.tvTabName)
                    val tabBackground = customView?.findViewById<View>(R.id.root_tab_view)
                    tabText?.alpha = 0.5f
//                    tabText?.setTextColor(
//                        ContextCompat.getColor(
//                            requireContext(),
//                            R.color.white_50
//                        )
//                    )
                    tabBackground?.isSelected = false
                }
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }
        })

        binding?.viewHeader?.btnBack?.onClick {
            popBackStack()
        }
        binding?.btnArrowDown?.onClick {
            FirebaseEventUtils.logEventTracking(context, "home_cate")
            binding?.viewCategory?.root?.visible()
            binding?.tabLayout?.invisible()
            binding?.btnArrowDown?.invisible()
        }
        binding?.viewCategory?.imgClose?.onClick {
            binding?.viewCategory?.root?.invisible()
            binding?.tabLayout?.visible()
            binding?.btnArrowDown?.visible()
        }
        binding?.viewCategory?.viewOverlay?.onClick {
            binding?.viewCategory?.root?.invisible()
            binding?.tabLayout?.visible()
            binding?.btnArrowDown?.visible()
        }
    }

    override fun setupUi() {
        FirebaseEventUtils.logEventTracking(context, "home_view")
        binding?.run {
            viewHeader.tvTitleTab.setText(R.string.template)
        }
        templateVPAdapter = TemplateVPAdapter(
            childFragmentManager,
            viewLifecycleOwner.lifecycle,
        )
        binding?.viewPager2?.adapter = templateVPAdapter
        binding?.viewPager2?.offscreenPageLimit = 1
        setupTabLayout()
        setupChipGroup()

        binding?.tabLayout?.getTabAt(0)?.select()
    }

    override fun renderUi() {
        homeViewModel.currentTabSelected.collectIn(
            this,
            action = { templateType ->
                templateType?.let { type ->
                    val tabIndex = when (type) {
                        TemplateType.Trending -> 0
                        TemplateType.All -> 1
                        TemplateType.SpecialDays -> 3
                        else -> 2
                    }

                    binding?.tabLayout?.getTabAt(tabIndex)?.apply {
                        if (tabIndex == 2) {
                            val customView = this.customView
                            val tabText = customView?.findViewById<TextView>(R.id.tvTabName)
                            tabText?.text = getString(type.title)
                        }
                        if (!isSelected) {
                            select()
                        }
                    }
                }
            }
        )
    }

    private fun setupChipGroup() {
        binding?.viewCategory?.chipgroupContent?.visibility = View.VISIBLE

        binding?.viewCategory?.chipgroupContent?.isSingleSelection = true

        applyChipTextColors()

        binding?.viewCategory?.chipgroupContent?.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isEmpty()) {
                val previouslySelectedChip = group.findViewById<Chip>(currentSelectedChipId)
                previouslySelectedChip?.isChecked = true
                return@setOnCheckedStateChangeListener
            }

            currentSelectedChipId = checkedIds.first()
            updateChipAppearance(group, checkedIds)

            if (checkedIds.isNotEmpty()) {
                val selectedChip = group.findViewById<Chip>(checkedIds.first())
                handleChipSelection(selectedChip)
            }
        }

        setDefaultSelection()
    }

    private fun applyChipTextColors() {
        val chipGroup = binding?.viewCategory?.chipgroupContent
        for (i in 0 until (chipGroup?.childCount ?: 0)) {
            val chip = chipGroup?.getChildAt(i) as? Chip
            chip?.setTextColor(
                ContextCompat.getColorStateList(
                    requireContext(),
                    R.color.chip_text_color
                )
            )
        }
    }

    private fun updateChipAppearance(group: ChipGroup, checkedIds: List<Int>) {
        for (i in 0 until group.childCount) {
            val chip = group.getChildAt(i) as? Chip
            chip?.let {
                if (checkedIds.contains(it.id)) {
                    it.alpha = 1.0f
                    it.setTextColor(ContextCompat.getColor(activity ?: return, R.color.white))
                } else {
                    it.alpha = 0.5f
                    it.setTextColor(ContextCompat.getColor(activity ?: return, R.color.white_50))
                }
            }
        }
    }

    private fun handleChipSelection(selectedChip: Chip) {
        when (selectedChip.text.toString()) {
            getString(R.string.trending) -> {
                FirebaseEventUtils.logEventTracking(context, "home_cate_trend")

                homeViewModel.updateCurrentTab(TemplateType.Trending)
            }

            getString(R.string.all) -> {
                FirebaseEventUtils.logEventTracking(context, "home_cate_all")

                homeViewModel.updateCurrentTab(TemplateType.All)
            }

            getString(R.string.birthdays) -> {
                FirebaseEventUtils.logEventTracking(context, "home_cate_birthday")
                homeViewModel.updateCurrentTab(TemplateType.Birthdays)
            }

            getString(R.string.special_days) -> {
                FirebaseEventUtils.logEventTracking(context, "home_cate_special")
                homeViewModel.updateCurrentTab(TemplateType.SpecialDays)
            }

            getString(R.string.holidays) -> {
                FirebaseEventUtils.logEventTracking(context, "home_cate_holiday")
                homeViewModel.updateCurrentTab(TemplateType.Holidays)
            }

            getString(R.string.travel) -> {
                FirebaseEventUtils.logEventTracking(context, "home_cate_travel")
                homeViewModel.updateCurrentTab(TemplateType.Travel)
            }

            getString(R.string.seasons) -> {
                FirebaseEventUtils.logEventTracking(context, "home_cate_season")
                homeViewModel.updateCurrentTab(TemplateType.Seasons)
            }

            getString(R.string.milestones) -> {
                FirebaseEventUtils.logEventTracking(context, "home_cate_milestone")
                homeViewModel.updateCurrentTab(TemplateType.Milestones)
            }

            getString(R.string.moments) -> {
                FirebaseEventUtils.logEventTracking(context, "home_cate_moment")
                homeViewModel.updateCurrentTab(TemplateType.Moments)
            }
        }
    }

    private fun setDefaultSelection(id: Int = R.id.trending) {
        val allChip = binding?.viewCategory?.chipgroupContent?.findViewById<Chip>(id)
        allChip?.isChecked = true
        currentSelectedChipId = id
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
        binding?.tabLayout?.setPadding(0, 0, 0, 0)
    }

    @SuppressLint("ResourceAsColor")
    private fun createCustomTabView(title: String): View {
        val view = layoutInflater.inflate(R.layout.item_tab_template, tabBinding?.root, false)
        val image = view.findViewById<ImageView>(R.id.img_icon)
        val textView = view.findViewById<TextView>(R.id.tvTabName)
        textView.text = title
        if (title != "Trending") {
            textView.setTextColor("#FFFFFF".toColorInt())
            textView.alpha = 0.5f
        }
        if (title == getString(R.string.trending)) image.visible()
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
            getString(R.string.tab_trending),
            getString(R.string.tab_all_photos),
            getString(R.string.tab_birthdays),
            getString(R.string.tab_special_days),
        )
    }

    override fun onResume() {
        super.onResume()
        homeViewModel.preloadNativeAdTempHome()
    }
}