package com.asoft.artsal.photo.ui.collage.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.asoft.artsal.photo.ui.collage.fragment.PageCollageFragment
import java.io.Serializable

class CollageVPAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle,
) : FragmentStateAdapter(fragmentManager, lifecycle)  {

    val pages = CollageType.entries

    override fun createFragment(position: Int): Fragment {
        return PageCollageFragment.newInstance(pages[position])
        }

    override fun getItemCount(): Int {
        return pages.size
    }
}

enum class CollageType: Serializable {
    All, Collage2, Collage3, Collage4, Collage5,
}