package com.asoft.artsal.photo.ui.template.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.asoft.artsal.photo.ui.template.fragment.PageTemplateFragment
import java.io.Serializable

class TemplateVPAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle,
) : FragmentStateAdapter(fragmentManager, lifecycle) {

    val pages = TemplateTypeTabs.entries

    override fun createFragment(position: Int): Fragment {
        return PageTemplateFragment.newInstance(pages[position])
    }

    override fun getItemCount(): Int {
        return pages.size
    }
}

enum class TemplateTypeTabs : Serializable {
    Trending, All, Mutable, SpecialDays
}