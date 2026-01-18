package com.asoft.artsal.photo.ui.settings.fragment

import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.artsal.photo.editor.collage.maker.R
import com.artsal.photo.editor.collage.maker.databinding.DialogRatingBinding
import com.asoft.artsal.photo.base.BaseSafeDialogFragment
import com.asoft.artsal.photo.customview.RatingView
import com.asoft.artsal.photo.extensions.invisible
import com.asoft.artsal.photo.extensions.onClick
import com.asoft.artsal.photo.extensions.openChPlay
import com.asoft.artsal.photo.extensions.sendFeedback
import com.asoft.artsal.photo.extensions.visible
import com.asoft.artsal.photo.ui.home.viewmodel.HomeViewModel
import com.asoft.artsal.photo.utils.SharePreference
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class RatingDialog : BaseSafeDialogFragment<DialogRatingBinding>() {
    @Inject
    lateinit var sharePreference: SharePreference

    private val homeViewModel by activityViewModels<HomeViewModel>()

    override fun getBackgroundColor(): Int {
        return R.color.scrimPrimary
    }

    override fun onInflateView(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): DialogRatingBinding = DialogRatingBinding.inflate(inflater)

    override fun setupUi() {
        binding?.run {
            tvTitleRate.text = getText(R.string.content_rate_default)
            tvContentRate.text =
                getText(R.string.enjoying_your_experience_let_us_know_how_we_re_doing_it_only_takes_a_few_seconds)
        }
        initListener()
    }

    private fun initListener() {
        binding?.run {
            ivClose.setOnClickListener {
                dismiss()
            }
            ratingView.apply {
                setMaxStars(5)
                setRating(5)
            }
            btnRate.onClick {
                val starRating = binding?.ratingView?.getRating() ?: 0
                if (starRating > 3) {
                    activity?.openChPlay()
                } else {
                    activity?.sendFeedback(
                        "feedback@qtonzglobal.com ",
                        "HDClip feedback",
                        rate = starRating
                    )
                }
                sharePreference.save(SharePreference.IS_RATE_APP, true)
                dismiss()
            }

            ratingView.setOnRatingChangeListener(object : RatingView.OnRatingChangeListener {
                override fun onRatingChanged(rating: Int) {

                    if (rating > 0) {
                        tvContentRate.gravity = Gravity.CENTER
                        btnRate.isEnabled = true

                        when (rating) {
                            4, 5 -> {
                                tvContentRate.text =
                                    getText(R.string.enjoying_your_experience_let_us_know_how_we_re_doing_it_only_takes_a_few_seconds)
                                tvTitleRate.text =
                                    getText(R.string.content_rate_default)
                                container.setBackgroundResource(R.drawable.bg_rating_happy)
                                imgStar.visible()
                            }


                            1, 2, 3 -> {
                                tvContentRate.text = getText(R.string.body_rate_unhappy)
                                tvTitleRate.text = getText(R.string.content_rate_unhappy)
                                container.setBackgroundResource(R.drawable.bg_rating_unhapy)
                                imgStar.invisible()
                            }
                        }
                    }
                }
            })
        }
    }

    companion object {
        fun newInstance() = RatingDialog()
    }
}