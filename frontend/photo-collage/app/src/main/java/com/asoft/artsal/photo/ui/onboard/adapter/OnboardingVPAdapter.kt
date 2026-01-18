package com.asoft.artsal.photo.ui.onboard.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.asoft.artsal.photo.ui.onboard.fragment.FullAdsNativeOBFragment
import com.asoft.artsal.photo.ui.onboard.fragment.OnboardingPageType
import com.asoft.artsal.photo.ui.onboard.fragment.PageOnboardingFragment
import com.asoft.artsal.photo.ui.onboard.fragment.WelcomeFragment

class OnboardingVPAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle,
    private val showFullAd1: Boolean,
    private val showFullAd2: Boolean,
) : FragmentStateAdapter(fragmentManager, lifecycle) {

    val pages = mutableListOf<OnboardingPageType>().apply {
        add(OnboardingPageType.OB)
        add(OnboardingPageType.OB)
        if (showFullAd1) add(OnboardingPageType.AD1)
        add(OnboardingPageType.OB)
        if (showFullAd2) add(OnboardingPageType.AD2)
        add(OnboardingPageType.WELCOME)
    }

    override fun getItemCount(): Int = pages.size

    override fun createFragment(position: Int): Fragment {
        return when (pages[position]) {
            OnboardingPageType.OB -> PageOnboardingFragment.newInstance(position)
            OnboardingPageType.AD1, OnboardingPageType.AD2 -> FullAdsNativeOBFragment.newInstance(pages[position])
            OnboardingPageType.WELCOME -> WelcomeFragment.newInstance()
        }
    }
}