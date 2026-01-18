package com.asoft.artsal.photo.extensions

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.icu.lang.UCharacter.IndicPositionalCategory.BOTTOM
import android.os.Handler
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.annotation.Px
import androidx.annotation.StringRes
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintSet
import androidx.constraintlayout.widget.Group
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.children
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.asoft.artsal.photo.utils.SafeOnClickListener
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import androidx.core.graphics.createBitmap
import android.graphics.RectF

const val matchParent = ViewGroup.LayoutParams.MATCH_PARENT
const val wrapContent = ViewGroup.LayoutParams.WRAP_CONTENT

fun View.hideKeyboard() {
    val imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(this.windowToken, 0)
}

fun View.isShowKeyboard() : Boolean {
    val insets = ViewCompat.getRootWindowInsets(this) ?: return false
    return insets.isVisible(WindowInsetsCompat.Type.ime())
}

fun View.showKeyboard() {
    val imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.showSoftInput(this, 0)
}

fun View.visible() {
    if (visibility != View.VISIBLE) visibility = View.VISIBLE
}

fun View.gone() {
    if (visibility != View.GONE) visibility = View.GONE
}

fun View.invisible() {
    if (visibility != View.INVISIBLE) visibility = View.INVISIBLE
}

fun View.visibleIf(condition: Boolean, gone: Boolean = true) =
    if (condition) {
        visible()
    } else {
        if (gone) gone() else invisible()
    }

inline fun <reified T : View> ViewGroup.inflate(@LayoutRes resId: Int) =
    LayoutInflater.from(context).inflate(resId, this, false) as T

inline val ViewGroup.inflater: LayoutInflater get() = LayoutInflater.from(context)

val RecyclerView.hasItems: Boolean
    get() = (adapter?.itemCount ?: 0) > 0

fun View.showSnackBar(
    message: String,
    duration: Int = Snackbar.LENGTH_SHORT,
    anchor: View? = null
) {
    Snackbar.make(this, message, duration)
        .setAnchorView(anchor)
        .show()
}

fun View.showSnackBar(
    @StringRes textId: Int,
    duration: Int = Snackbar.LENGTH_SHORT,
    anchor: View? = null
) {
    Snackbar.make(this, textId, duration)
        .setAnchorView(anchor)
        .show()
}

fun View.showSnackBar(
    @StringRes textId: Int,
    duration: Int = Snackbar.LENGTH_SHORT,
    @IdRes anchor: Int
) {
    Snackbar.make(this, textId, duration)
        .setAnchorView(anchor)
        .show()
}

fun View.focusAndShowKeyboard() {
    fun View.showTheKeyboardNow() {
        if (isFocused) {
            post {
                val imm =
                    context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
            }
        }
    }

    requestFocus()
    if (hasWindowFocus()) {
        showTheKeyboardNow()
    } else {
        viewTreeObserver.addOnWindowFocusChangeListener(
            object : ViewTreeObserver.OnWindowFocusChangeListener {
                override fun onWindowFocusChanged(hasFocus: Boolean) {
                    if (hasFocus) {
                        this@focusAndShowKeyboard.showTheKeyboardNow()
                        viewTreeObserver.removeOnWindowFocusChangeListener(this)
                    }
                }
            })
    }
}

fun View.onClick(safe: Boolean = true, action: (View) -> Unit) = setOnClickListener(
    SafeOnClickListener(safe, action)
)

fun View.onLongClick(action: (View) -> Unit) = setOnLongClickListener {
    action(it)
    true
}

@SuppressLint("ClickableViewAccessibility")
fun View.hideKeyboardClickOutSide() {
    fun View.setUpTouchListener(view: View) {
        if (view !is EditText || view !is ImageView) {
            view.setOnTouchListener { _, _ ->
                this.hideKeyboard()
                false
            }
        }
    }

    if (this is ViewGroup) {
        for (i in 0 until this.childCount) {
            val innerView = this.getChildAt(i)
            setUpTouchListener(innerView)
        }
    }
}

fun RecyclerView.getLastVisibleItemPosition(lastPosition: (Int) -> Unit) {
    this.addOnScrollListener(object : RecyclerView.OnScrollListener() {

        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                val lastVisibleItem = (layoutManager as LinearLayoutManager?)?.findLastCompletelyVisibleItemPosition()
                lastPosition.invoke(lastVisibleItem ?: 0)
            }
        }

        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            val lastVisibleItem =
                (layoutManager as LinearLayoutManager?)?.findLastVisibleItemPosition()
        }
    })
}

val View.isKeyboardShown: Boolean
    get() {
        val rect = Rect()
        getWindowVisibleDisplayFrame(rect)
        val screenHeight = rootView.height

        // rect.bottom is the position above soft keypad or device button.
        // if keypad is shown, the r.bottom is smaller than that before.
        val keypadHeight = screenHeight - rect.bottom
        return keypadHeight > screenHeight * 0.15
    }

fun addKeyboardVisibilityListener(rootLayout: View): Flow<Boolean> {
    return callbackFlow {
        val listener = ViewTreeObserver.OnGlobalLayoutListener {
            val r = Rect()
            rootLayout.getWindowVisibleDisplayFrame(r)
            val screenHeight = rootLayout.rootView.height
            // r.bottom is the position above soft keypad or device button.
            // if keypad is shown, the r.bottom is smaller than that before.
            val keypadHeight = screenHeight - r.bottom
            val isVisible =
                keypadHeight > screenHeight * 0.15 // 0.15 ratio is perhaps enough to determine keypad height.
            trySend(isVisible)
        }
        rootLayout.viewTreeObserver.addOnGlobalLayoutListener(listener)
        awaitClose {
            rootLayout.viewTreeObserver.removeOnGlobalLayoutListener(listener)
        }
    }
}

inline fun View.doOnNextLayout(crossinline action: (view: View) -> Unit): View.OnLayoutChangeListener {
    return object : View.OnLayoutChangeListener {
        override fun onLayoutChange(
            view: View,
            left: Int,
            top: Int,
            right: Int,
            bottom: Int,
            oldLeft: Int,
            oldTop: Int,
            oldRight: Int,
            oldBottom: Int,
        ) {
            view.removeOnLayoutChangeListener(this)
            action(view)
        }
    }.also(this::addOnLayoutChangeListener)
}

suspend inline fun View.awaitNextLayout() = suspendCancellableCoroutine<Unit> { cont ->
    val listener = doOnNextLayout { cont.resume(Unit) }
    cont.invokeOnCancellation {
        removeOnLayoutChangeListener(listener)
    }
}

fun AppCompatTextView.setGradientText(primaryColor: String, secondaryColor: String, value: String) {
    paint.shader = LinearGradient(
        0f, 0f, width.toFloat(), height.toFloat(),
        Color.parseColor(primaryColor),
        Color.parseColor(secondaryColor),
        Shader.TileMode.CLAMP
    )
    text = value
}

fun AppCompatTextView.setGradientText(
    colors: List<String>,
    vertical: Boolean = false,
    positions: FloatArray? = null
) {
    post {
        val x0 = 0f
        var x1 = width.toFloat()
        var y0 = 0f
        val y1 = height.toFloat()

        if (vertical) {
            x1 = 0f
            y0 = 0f // Start from top for vertical gradient
        }

        // Convert color strings to int array
        val colorInts = colors.map { it.toColorInt() }.toIntArray()

        // Use provided positions or create evenly distributed positions
        val colorPositions = positions ?: run {
            FloatArray(colors.size) { i -> i.toFloat() / (colors.size - 1) }
        }

        paint.shader = LinearGradient(
            x0, y0, x1, y1,
            colorInts,
            colorPositions,
            Shader.TileMode.CLAMP
        )
        invalidate()
    }
}

fun View.goneDelay(handler: Handler, delay: Long) {
    handler.postDelayed({
        gone()
    }, delay)
}

fun RadioGroup.getCheckedRadioButtonPosition(): Int {
    val radioButtonId = checkedRadioButtonId
    return children.filter { it is RadioButton }.mapIndexed { index: Int, view: View ->
        index to view
    }.firstOrNull {
        it.second.id == radioButtonId
    }?.first ?: -1
}

fun Group.setAllOnClickListener(listener: View.OnClickListener?) {
    referencedIds.forEach { id ->
        rootView.findViewById<View>(id).setOnClickListener(listener)
    }
}
fun handlePhysicalBackClicked(view: View?, onItemClickListener: () -> Unit) {
    if (view != null) {
        view.isFocusableInTouchMode = true
        view.requestFocus()
        view.setOnKeyListener(object : View.OnKeyListener {
            override fun onKey(v: View?, keyCode: Int, event: KeyEvent): Boolean {
                if (event.action == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK) {
                    onItemClickListener()
                    return true
                }
                return false
            }
        })
    }
}

fun View.setSize(width: Int, height: Int) {
    layoutParams.width = width
    layoutParams.height = height
    requestLayout()
}


fun ConstraintSet.bottomToTop(v1: View, v2: View, @Px margin: Int = 0) {
    connect(v1.id, ConstraintSet.BOTTOM, v2.id, ConstraintSet.TOP, margin)
}

fun ConstraintSet.topToParent(v1: View, @Px margin: Int = 0) {
    connect(v1.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, margin)
}
fun ConstraintSet.topToTop(v1: View, v2: View, @Px margin: Int = 0) {
    connect(v1.id, ConstraintSet.TOP, v2.id, ConstraintSet.TOP, margin)
}

fun ConstraintSet.topToBottom(v1: View, v2: View, @Px margin: Int = 0) {
    connect(v1.id, ConstraintSet.TOP, v2.id, ConstraintSet.BOTTOM, margin)
}

fun ConstraintSet.bottomToParent(v1: View, @Px margin: Int = 0) {
    connect(v1.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, margin)
}

fun ConstraintSet.bottomToBottom(v1: View, v2: View, @Px margin: Int = 0) {
    connect(v1.id, BOTTOM, v2.id, BOTTOM, margin)
}

fun <T : View> T.width(function: (Int) -> Unit) {
    if (width == 0)
        viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                viewTreeObserver.removeOnGlobalLayoutListener(this)
                function(width)
            }
        })
    else function(width)
}

fun <T : View> T.height(function: (Int) -> Unit) {
    if (height == 0)
        viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                viewTreeObserver.removeOnGlobalLayoutListener(this)
                function(height)
            }
        })
    else function(height)
}

fun View.windowTopInsetSystemBars() {
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        v.setPadding(0, systemBars.top, 0, 0)
        insets
    }
}

fun View.setBlur(enable: Boolean) {
    if (!enable) {
        val paint = Paint().apply {
            maskFilter = BlurMaskFilter(10f, BlurMaskFilter.Blur.NORMAL)
        }
        setLayerType(View.LAYER_TYPE_SOFTWARE, paint) // Apply blur
        alpha = 0.5f // Reduce opacity
    } else {
        setLayerType(View.LAYER_TYPE_NONE, null) // Remove blur
        alpha = 1f // Restore opacity
    }
}

fun View.getBitmapFromView() : Bitmap {


    // Tạo Bitmap với kích thước bằng view
    val bitmap = createBitmap(this.width, this.height)

    // Tạo Canvas từ Bitmap
    val canvas = Canvas(bitmap)

    // Nếu view có background thì vẽ background, nếu không thì vẽ màu trắng
    val bgDrawable: Drawable? = this.background
    if (bgDrawable != null) {
        bgDrawable.draw(canvas)
    } else {
        canvas.drawColor(Color.WHITE)
    }

    // Vẽ nội dung view lên canvas (vậy là lên Bitmap)
    this.draw(canvas)
    return bitmap
}

private fun View.findFirstImageView(): ImageView? {
    if (this is ImageView) return this

    if (this is ViewGroup) {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val imageView = child.findFirstImageView()
            if (imageView != null) return imageView
        }
    }

    return null
}

fun ImageView.getImageDisplayBoundsForFitCenter(): RectF? {
    val drawable = drawable ?: return null

    val viewWidth = width.toFloat()
    val viewHeight = height.toFloat()
    val drawableWidth = drawable.intrinsicWidth.toFloat()
    val drawableHeight = drawable.intrinsicHeight.toFloat()

    if (drawableWidth <= 0 || drawableHeight <= 0) return null

    // Tính scale factor cho fitCenter
    val scaleX = viewWidth / drawableWidth
    val scaleY = viewHeight / drawableHeight
    val scale = minOf(scaleX, scaleY)

    // Tính kích thước sau khi scale
    val scaledWidth = drawableWidth * scale
    val scaledHeight = drawableHeight * scale

    // Tính vị trí center
    val left = (viewWidth - scaledWidth) / 2f
    val top = (viewHeight - scaledHeight) / 2f

    return RectF(left, top, left + scaledWidth, top + scaledHeight)
}

fun View.getBitmapFromViewWithImageBounds(backgroundImageId: Int? = null): Bitmap {
    val originalBitmap = getBitmapFromView()

    // Tìm ImageView background
    val backgroundImageView = findImageViewById(backgroundImageId)
        ?: findFirstImageView()

    backgroundImageView?.let { imageView ->
        val bounds = imageView.getImageDisplayBoundsForFitCenter()
        bounds?.let { rect ->
            // Convert từ tọa độ của ImageView sang tọa độ của container
            val globalBounds = convertToContainerCoordinates(imageView, rect)

            val left = globalBounds.left.toInt()
            val top = globalBounds.top.toInt()
            val width = globalBounds.width().toInt()
            val height = globalBounds.height().toInt()

            // Kiểm tra bounds hợp lệ
            if (width > 0 && height > 0 &&
                left >= 0 && top >= 0 &&
                left + width <= originalBitmap.width &&
                top + height <= originalBitmap.height) {

                return Bitmap.createBitmap(originalBitmap, left, top, width, height)
            }
        }
    }

    return originalBitmap
}

private fun View.findImageViewById(imageId: Int?): ImageView? {
    if (imageId == null) return null
    return findViewById<ImageView>(imageId)
}

private fun View.convertToContainerCoordinates(imageView: ImageView, bounds: RectF): RectF {
    // Tính offset của ImageView so với container
    var offsetX = 0f
    var offsetY = 0f
    var currentView: View = imageView

    while (currentView != this && currentView.parent is View) {
        offsetX += currentView.x
        offsetY += currentView.y
        currentView = currentView.parent as View
    }

    return RectF(
        bounds.left + offsetX,
        bounds.top + offsetY,
        bounds.right + offsetX,
        bounds.bottom + offsetY
    )
}
