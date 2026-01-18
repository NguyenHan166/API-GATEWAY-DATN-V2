package com.asoft.artsal.photo.customview

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
import com.artsal.photo.editor.collage.maker.R
import com.artsal.photo.editor.collage.maker.databinding.ViewSelectHabitsBinding
import com.asoft.artsal.photo.extensions.loadImageDrawableWithCompress
import timber.log.Timber

class HabitsView(context: Context, attrs: AttributeSet?, defStyle: Int) :
    ConstraintLayout(context, attrs, defStyle) {

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    private lateinit var binding: ViewSelectHabitsBinding
    private var imgFavorite: Drawable? = null
    private var nameFavorite: String? = null

    init {
        init(context)
        loadAttributes(context, attrs, defStyle)
    }

    private fun init(context: Context) {
        binding = ViewSelectHabitsBinding.inflate(LayoutInflater.from(context), this, true)
    }

    private fun loadAttributes(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        Timber.i("loadAttributes...: $attrs")
        val typedArray = context.theme.obtainStyledAttributes(
            attrs, R.styleable.HabitsLayout, defStyleAttr, 0
        )
        try {
            imgFavorite = typedArray.getDrawable(R.styleable.HabitsLayout_imgHabit)
            nameFavorite = typedArray.getString(R.styleable.HabitsLayout_nameHabit)
            binding.run {
                tvEvent.text = nameFavorite
                imgIcon.loadImageDrawableWithCompress(drawableRes = imgFavorite ?: return)
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        } finally {
            typedArray.recycle()
        }
    }

    fun setSelectState(isSelect: Boolean) {
        Timber.i("setSelectState: $isSelect")
        binding.rootHabits.isSelected = isSelect
    }

    fun getSelectState(): Boolean {
    Timber.i("getSelectState: ${binding.rootHabits.isSelected}")
        return binding.rootHabits.isSelected
    }

}