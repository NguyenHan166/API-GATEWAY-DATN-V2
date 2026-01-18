package com.asoft.artsal.photo.customview

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import com.artsal.photo.editor.collage.maker.R

class RatingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val starViews = kotlin.collections.ArrayList<View>()
    private var currentRating = 0
    private var maxStars = 4
    private var ratingChangeListener: OnRatingChangeListener? = null
    private var isInitialState = true

    init {
        orientation = HORIZONTAL
        setupStars()
        updateRating(0)
    }

    private fun setupStars() {
        removeAllViews()
        starViews.clear()

        for (i in 1..maxStars) {
            val starView = ImageView(context).apply {
                setImageResource(R.drawable.ic_rate)
                val params = LayoutParams(
                    context.resources.getDimensionPixelSize(R.dimen.dp_40),
                    context.resources.getDimensionPixelSize(R.dimen.dp_40)
                )

                if (i > 1) {
                    params.marginStart = context.resources.getDimensionPixelSize(R.dimen.dp_20)
                }

                layoutParams = params
                tag = i
                setOnClickListener { handleStarClick(i) }
            }
            starViews.add(starView)
            addView(starView)
        }
    }

    private fun handleStarClick(rating: Int) {
        if (isInitialState && rating <= 4) {
            isInitialState = false
            maxStars = 5
            setupStars()
        } else if (rating == 5) {
            if (isInitialState) {
                isInitialState = false
                maxStars = 5
                setupStars()
            }
        }
        updateRating(rating)
    }

    private fun updateRating(rating: Int) {
        currentRating = rating

        for (i in 0 until kotlin.comparisons.minOf(starViews.size, maxStars)) {
            val starView = starViews[i]
            if (starView is ImageView) {
                val starPosition = i + 1
                starView.setImageResource(
                    if (starPosition <= currentRating)
                        R.drawable.ic_rate_app
                    else
                        R.drawable.ic_unrate
                )
            }
        }

        ratingChangeListener?.onRatingChanged(currentRating)
    }

    fun setRating(rating: Int) {
        if (rating in 1..5) {
            if (rating == 5 && isInitialState) {
                isInitialState = false
                maxStars = 5
                setupStars()
            }
            updateRating(rating)
        }
    }

    fun getRating(): Int = currentRating

    fun setMaxStars(max: Int) {
        if (max > 0) {
            maxStars = max
            setupStars()
            if (currentRating in 1..maxStars) {
                setRating(currentRating)
            } else {
                currentRating = 0
            }
        }
    }

    fun setOnRatingChangeListener(listener: OnRatingChangeListener) {
        ratingChangeListener = listener
    }

    interface OnRatingChangeListener {
        fun onRatingChanged(rating: Int)
    }
}