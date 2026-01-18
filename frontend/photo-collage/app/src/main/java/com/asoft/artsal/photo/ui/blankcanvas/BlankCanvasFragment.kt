package com.asoft.artsal.photo.ui.blankcanvas

import android.view.LayoutInflater
import android.view.ViewGroup
import com.artsal.photo.editor.collage.maker.R
import com.artsal.photo.editor.collage.maker.databinding.FragmentBlankCanvasBinding
import com.asoft.artsal.photo.base.BaseFragment
import com.asoft.artsal.photo.extensions.navigateToWithAnim
import com.asoft.artsal.photo.extensions.onClick
import com.asoft.artsal.photo.extensions.popBackStack

class BlankCanvasFragment : BaseFragment<FragmentBlankCanvasBinding>() {
    override val isInsets: Boolean
        get() = true
    override val isLightStatusBar: Boolean
        get() = false

    override fun onInflateView(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentBlankCanvasBinding {
        return FragmentBlankCanvasBinding.inflate(inflater, container, false)
    }

    override fun initListener() {
        binding?.run {
            btnStory.onClick {
                navigateToWithAnim(R.id.action_blank_canvas_fragment_to_generate_story_comic_fragment)
            }
            btnRelight.onClick {
                navigateToWithAnim(R.id.action_blank_canvas_fragment_to_relight_fragment)

            }
            btnBackground.onClick {
                navigateToWithAnim(R.id.action_blank_canvas_fragment_to_background_fragment)
            }
            btnStyle.onClick {
                navigateToWithAnim(
                    R.id.action_blank_canvas_fragment_to_style_fragment
                )
            }
            appBar.btnBack.onClick {
                popBackStack()
            }
        }
    }

    override fun setupUi() {
        binding?.appBar?.tvTitleTab?.setText(R.string.ai_generate)
    }

    override fun renderUi() {
    }
}