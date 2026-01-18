package com.asoft.artsal.photo.ui.home.fragment

import android.media.Rating
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.artsal.photo.editor.collage.maker.R
import com.artsal.photo.editor.collage.maker.databinding.FragmentHomeParentBinding
import com.asoft.artsal.photo.base.BaseFragment
import com.asoft.artsal.photo.extensions.navigateToWithAnim
import com.asoft.artsal.photo.ui.home.viewmodel.HomeViewModel
import com.asoft.artsal.photo.ui.settings.fragment.RatingDialog
import com.asoft.artsal.photo.utils.FirebaseEventUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlin.getValue

@AndroidEntryPoint
class HomeParentFragment : BaseFragment<FragmentHomeParentBinding>() {
    private var navController: NavController? = null

    private val homeViewModel: HomeViewModel by activityViewModels<HomeViewModel>()

    override fun onInflateView(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentHomeParentBinding = FragmentHomeParentBinding.inflate(inflater, container, false)

    override fun initListener() {
        ViewCompat.setOnApplyWindowInsetsListener(
            binding?.btmNavigation ?: return
        ) { view, insets ->
            val navigationBarsInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            view.updatePadding(bottom = navigationBarsInsets.bottom)
            WindowInsetsCompat.CONSUMED
        }

        val navHostFragment = childFragmentManager.findFragmentById(
            R.id.navHostFragment
        ) as? NavHostFragment
        navController = navHostFragment?.navController ?: return
        binding?.btmNavigation?.setupWithNavController(navController ?: return)

        binding?.btmNavigation?.setOnItemSelectedListener { item ->
            when(item.itemId) {

                R.id.template_fragment -> {
                    FirebaseEventUtils.logEventTracking(context, "home_click_template")
                    navController?.navigate(R.id.template_fragment)
                    true
                }

                R.id.collage_fragment -> {
                    FirebaseEventUtils.logEventTracking(context, "home_click_collage")
                    navController?.navigate(R.id.collage_fragment)
                    true
                }

                R.id.my_photo_fragment -> {
                    FirebaseEventUtils.logEventTracking(context, "home_click_myphoto")
                    navController?.navigate(R.id.my_photo_fragment)
                    true
                }
                else -> false
            }
        }
    }

    override fun setupUi() {
        RatingDialog.newInstance().show(childFragmentManager, "RatingDialog")
    }

    override fun renderUi() {
    }
}