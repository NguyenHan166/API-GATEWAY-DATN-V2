package com.asoft.artsal.photo.customview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.artsal.photo.editor.collage.maker.R;
import com.google.android.material.imageview.ShapeableImageView;
import android.graphics.LinearGradient;
import android.graphics.Shader;

public class MaskImageView extends ShapeableImageView {
    private OnMaskImageViewClickListener mListener;
    public interface OnMaskImageViewClickListener {
        void onMaskImageViewClick(MaskImageView e);
    }

    private final Paint mMaskedPaint = new Paint();
    private final Paint mCopyPaint = new Paint();
    private final Rect mMaskBounds = new Rect();
    private final RectF mViewBoundsF = new RectF();
    private Drawable mMaskDrawable;
    private boolean backgroundDefault;
    private float mCornerRadius = 0f;
    private Path mCornerPath = new Path();
    private final RectF mCornerRectF = new RectF();
    private int mStrokeColor = Color.TRANSPARENT;
    private float mStrokeWidth = 0f;
    private final Paint mStrokePaint = new Paint();
    // Các biến cho gesture: drag & zoom
    private static final int MODE_NONE = 0;
    private static final int MODE_DRAG = 1;
    private static final int MODE_ZOOM = 2;
    private int mode = MODE_NONE;
    private final Paint borderPaint = new Paint();

    private float lastX, lastY;
    private float startDistance = 0f;
    private float midX = 0f, midY = 0f;
    // Matrix chuyển đổi ảnh
    private Matrix mImageMatrix = new Matrix();
    private Matrix mSavedMatrix = new Matrix();

    private Boolean mIsFirstSelect = false;
    private Boolean mIsSelected = false;
    private int mSelectedStrokeColor = Color.RED; // Màu đỏ rõ ràng cho selected
    private int mUnSelectedStrokeColor = Color.TRANSPARENT;
    private float mSelectedStrokeWidth = 8f; // Độ dày border khi selected (tăng lên)
    private float mUnSelectedStrokeWidth = 0f;

    // Gradient colors for selected border (3 colors)
    private int[] mSelectedGradientColors = {
        Color.parseColor("#FF6B35"), // Orange
        Color.parseColor("#F7931E"), // Yellow-Orange  
        Color.parseColor("#FFD700")  // Gold
    };
    private boolean mUseGradientBorder = true; // Enable/disable gradient

    /**
     * Set up matrix to initially display image with CENTER_CROP-like effect
     * Call this method after setting image from URI
     */
    public void setCenterCropMatrix(Uri imageUri) {
        // Reset matrix
        mImageMatrix = new Matrix();

        // Get drawable dimensions
        Drawable drawable = getDrawable();
        if (drawable == null) return;

        int dwidth = drawable.getIntrinsicWidth();
        int dheight = drawable.getIntrinsicHeight();

        // No drawable dimensions, return
        if (dwidth == 0 || dheight == 0) return;

        // Get view dimensions
        int vwidth = getWidth() - getPaddingLeft() - getPaddingRight();
        int vheight = getHeight() - getPaddingTop() - getPaddingBottom();

        // No view dimensions yet, post for later
        if (vwidth == 0 || vheight == 0) {
            post(() -> setCenterCropMatrix(imageUri));
            return;
        }

        // Calculate the scale needed to fill the view
        float scale;
        float dx = 0, dy = 0;

        // Calculate the scaling factor needed to fill the view
        float scaleX = (float) vwidth / dwidth;
        float scaleY = (float) vheight / dheight;

        // Use the larger scaling factor to ensure the image fills the view
        scale = Math.max(scaleX, scaleY);

        // Calculate translation so image is centered
        dx = (vwidth - dwidth * scale) * 0.5f;
        dy = (vheight - dheight * scale) * 0.5f;

        // Apply transformations
        mImageMatrix.setScale(scale, scale);
        mImageMatrix.postTranslate(Math.round(dx), Math.round(dy));

        // Apply the matrix
        setScaleType(ScaleType.MATRIX);
        setImageMatrix(mImageMatrix);

        // Save the initial matrix for reference
        mSavedMatrix.set(mImageMatrix);
    }

    /**
     * Convenience method to set image from URI and apply center crop matrix
     */
    public void setImageWithCenterCrop(Uri imageUri) {
        setImageURI(imageUri);
        // Post to ensure the image is loaded before calculating matrix
        post(() -> setCenterCropMatrix(imageUri));
    }

    /**
     * Xử lý touch event: hỗ trợ click, kéo (drag) và phóng thu nhỏ (pinch zoom)
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(!isClickable()) {
            return false;
        }
        int action = event.getAction() & MotionEvent.ACTION_MASK;
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mSavedMatrix.set(mImageMatrix);
                lastX = event.getX();
                lastY = event.getY();
                mode = MODE_DRAG;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                startDistance = spacing(event);
                if (startDistance > 10f) {
                    mSavedMatrix.set(mImageMatrix);
                    midPoint(event);
                    mode = MODE_ZOOM;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (mode == MODE_DRAG) {
                    float dx = event.getX() - lastX;
                    float dy = event.getY() - lastY;
                    mImageMatrix.set(mSavedMatrix);
                    mImageMatrix.postTranslate(dx, dy);
                    setImageMatrix(mImageMatrix);
                } else if (mode == MODE_ZOOM && event.getPointerCount() >= 2) {
                    float newDist = spacing(event);
                    if (newDist > 10f) {
                        float scale = newDist / startDistance;
                        mImageMatrix.set(mSavedMatrix);
                        mImageMatrix.postScale(scale, scale, midX, midY);
                        setImageMatrix(mImageMatrix);
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                // Nếu như không có di chuyển nhiều, coi như là click
                if (mode == MODE_DRAG) {
                    float dx = event.getX() - lastX;
                    float dy = event.getY() - lastY;
                    if (Math.sqrt(dx * dx + dy * dy) < 10) { // ngưỡng nhỏ để phân biệt click
                        if (mListener != null) {
                            mListener.onMaskImageViewClick(this);
                        }
                    }
                }
                mode = MODE_NONE;
                break;
            case MotionEvent.ACTION_POINTER_UP:
                mode = MODE_NONE;
                break;
        }
        return true;
    }

    // Tính khoảng cách giữa 2 ngón tay
    private float spacing(MotionEvent event) {
        if (event.getPointerCount() >= 2) {
            float x = event.getX(0) - event.getX(1);
            float y = event.getY(0) - event.getY(1);
            return (float) Math.sqrt(x * x + y * y);
        }
        return 0;
    }

    // Tính tâm giữa 2 ngón tay
    private void midPoint(MotionEvent event) {
        if (event.getPointerCount() >= 2) {
            midX = (event.getX(0) + event.getX(1)) / 2;
            midY = (event.getY(0) + event.getY(1)) / 2;
        }
    }

    public void setOnMaskImageViewClickListener(OnMaskImageViewClickListener listener) {
        mListener = listener;
    }


    public MaskImageView(final Context context) {
        this(context, null, 0);
    }

    public MaskImageView(final Context context, final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MaskImageView(final Context context, final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setClickable(true);  // Tự động đặt view thành clickable
        mMaskedPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        TypedArray attribs = context.obtainStyledAttributes(attrs, R.styleable.MaskImageView, defStyleAttr, 0);
        mMaskDrawable = attribs.getDrawable(R.styleable.MaskImageView_maskDrawable);
        backgroundDefault = attribs.getBoolean(R.styleable.MaskImageView_backgroundDefault, true);
        mCornerRadius = attribs.getDimension(R.styleable.MaskImageView_cornerRadius, 0f);
        mStrokeColor = attribs.getColor(R.styleable.MaskImageView_strokeColor, Color.TRANSPARENT);
        mStrokeWidth = attribs.getDimension(R.styleable.MaskImageView_strokeWidth, 0f);
        attribs.recycle();
        attribs = null;
        // Khởi tạo matrix mặc định cho ảnh
        if (mMaskDrawable != null) {
            mMaskDrawable.setBounds(mMaskBounds);
        }
        if (backgroundDefault) {
            setBackgroundColor(Color.TRANSPARENT);
        }
        mImageMatrix = new Matrix();
        setScaleType(ScaleType.CENTER_CROP);

        // Initialize stroke paint
        initStrokePaint();
//        setMatrixCenterCrop();
//        setCenterCropMatrix();
    }


    /**
     * set drawable for alpha mask, if set null, mask feature is disabled and all source image is displayed.
     * only alpha value is valid and other color attribute(R, B, and B) are ignored.
     * If the alpha value is smaller than 1.0, half transparent image is displayed
     *
     * @param mask_drawable
     */
    public synchronized void setMaskDrawable(final Drawable mask_drawable) {
        if (mMaskDrawable != mask_drawable) {
            mMaskDrawable = mask_drawable;
            if (mMaskDrawable != null) {
                mMaskDrawable.setBounds(mMaskBounds);
            }
            postInvalidate();
        }
    }

    @Override
    protected synchronized void onSizeChanged(int width, int height, int old_width, int old_height) {
        final int paddingLeft = getPaddingLeft();
        final int paddingRight = getPaddingRight();
        final int paddingTop = getPaddingTop();
        final int paddingBottom = getPaddingBottom();

        // Tính kích thước khả dụng của view (sau khi trừ padding)
        int availableWidth = width - paddingLeft - paddingRight;
        int availableHeight = height - paddingTop - paddingBottom;

        if (mMaskDrawable != null) {
            // Lấy kích thước gốc của mask drawable
            int intrinsicWidth = mMaskDrawable.getIntrinsicWidth();
            int intrinsicHeight = mMaskDrawable.getIntrinsicHeight();

            // Tính tỉ lệ scale để mask vừa khít trong view mà không bị cắt
            float scaleX = (float) availableWidth / intrinsicWidth;
            float scaleY = (float) availableHeight / intrinsicHeight;
            float scale = Math.min(scaleX, scaleY);  // Để mask không bị phóng to vượt quá

            // Kích thước mới cho mask
            int maskWidth = (int) (intrinsicWidth * scale);
            int maskHeight = (int) (intrinsicHeight * scale);

            // Căn giữa mask trong view
            int left = paddingLeft + (availableWidth - maskWidth) / 2;
            int top = paddingTop + (availableHeight - maskHeight) / 2;
            mMaskBounds.set(left, top, left + maskWidth, top + maskHeight);
            mMaskDrawable.setBounds(mMaskBounds);
        } else {
            // Nếu không có mask drawable thì thiết lập bounds mặc định
            mMaskBounds.set(paddingLeft, paddingTop, paddingLeft + availableWidth, paddingTop + availableHeight);
        }

        // Thiết lập mask filter (có thể điều chỉnh theo ý bạn)
        int blurRadius = Math.min(width, height) / 3;
        mMaskedPaint.setMaskFilter(new BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL));

        // Cập nhật kích thước vùng vẽ cho canvas
        mViewBoundsF.set(0, 0, width, height);

        // Update corner path if corner radius is set
        updateCornerPath();
    }


    @Override
    protected synchronized void onDraw(final Canvas canvas) {
        if ((mViewBoundsF.width() == 0) || (mViewBoundsF.height() == 0)) {
            super.onDraw(canvas);
            return;
        }

        // Apply corner radius clipping if set
        final int clipSaveCount = mCornerRadius > 0 ? canvas.save() : -1;
        if (mCornerRadius > 0) {
            canvas.clipPath(mCornerPath);
        }

        final int saveCount = canvas.saveLayer(mViewBoundsF, mCopyPaint);
        try {
            canvas.translate(-getPaddingLeft(), -getPaddingTop());
            if (mMaskDrawable != null) {
                mMaskDrawable.draw(canvas);
                canvas.saveLayer(mViewBoundsF, mMaskedPaint);
            }
            canvas.translate(getPaddingLeft(), getPaddingTop());
            super.onDraw(canvas);

        } finally {
            canvas.restoreToCount(saveCount);
        }

        // Draw stroke if enabled
        if (mStrokeWidth > 0 && mStrokeColor != Color.TRANSPARENT) {
            if (mCornerRadius > 0) {
                canvas.drawPath(mCornerPath, mStrokePaint);
            } else {
                canvas.drawRect(mViewBoundsF, mStrokePaint);
            }
        }

        if (clipSaveCount >= 0) {
            canvas.restoreToCount(clipSaveCount);
        }

        boolean isCurrentlySelected = mIsSelected != null ? mIsSelected : false;

        Paint selectedPaint = new Paint();
        selectedPaint.setStyle(Paint.Style.STROKE);
        selectedPaint.setAntiAlias(true);

        if (isCurrentlySelected) {
            selectedPaint.setStrokeWidth(mSelectedStrokeWidth);
            
            if (mUseGradientBorder && mSelectedGradientColors.length >= 2) {
                // Create gradient for selected border
                @SuppressLint("DrawAllocation") LinearGradient gradient = new LinearGradient(
                    mViewBoundsF.left, mViewBoundsF.top,
                    mViewBoundsF.right, mViewBoundsF.bottom,
                    mSelectedGradientColors,
                    null, // Equal distribution
                    Shader.TileMode.CLAMP
                );
                selectedPaint.setShader(gradient);
            } else {
                // Fallback to solid color
                selectedPaint.setColor(mSelectedStrokeColor);
            }
        } else {
            selectedPaint.setColor(mUnSelectedStrokeColor);
            selectedPaint.setStrokeWidth(mUnSelectedStrokeWidth);
        }

        // Draw the border
        if (mCornerRadius > 0) {
            canvas.drawPath(mCornerPath, selectedPaint);
        } else {
            canvas.drawRect(mViewBoundsF, selectedPaint);
        }
    }

    /**
     * Set corner radius for the view
     *
     * @param cornerRadius corner radius in pixels
     */
    public void setCornerRadius(float cornerRadius) {
        if (mCornerRadius != cornerRadius) {
            mCornerRadius = cornerRadius;
            updateCornerPath();
            invalidate();
        }
    }

    /**
     * Get current corner radius
     *
     * @return corner radius in pixels
     */
    public float getCornerRadius() {
        return mCornerRadius;
    }

    /**
     * Update the corner path based on current view bounds and corner radius
     */
    private void updateCornerPath() {
        if (mCornerRadius > 0 && mViewBoundsF.width() > 0 && mViewBoundsF.height() > 0) {
            mCornerPath.reset();
            mCornerRectF.set(mViewBoundsF);
            // Adjust for stroke width to ensure stroke is drawn within bounds
            if (mStrokeWidth > 0) {
                float halfStroke = mStrokeWidth / 2f;
                mCornerRectF.inset(halfStroke, halfStroke);
            }
            mCornerPath.addRoundRect(mCornerRectF, mCornerRadius, mCornerRadius, Path.Direction.CW);
        } else {
            mCornerPath.reset();
        }
    }

    /**
     * Initialize stroke paint properties
     */
    private void initStrokePaint() {
        mStrokePaint.setStyle(Paint.Style.STROKE);
        mStrokePaint.setAntiAlias(true);
        mStrokePaint.setColor(mStrokeColor);
        mStrokePaint.setStrokeWidth(mStrokeWidth);
    }

    /**
     * Set border stroke color
     *
     * @param strokeColor stroke color
     */
    public void setBorderStrokeColor(int strokeColor) {
        if (mStrokeColor != strokeColor) {
            mStrokeColor = strokeColor;
            mStrokePaint.setColor(mStrokeColor);
            invalidate();
        }
    }

    /**
     * Get current border stroke color
     *
     * @return stroke color
     */
    public int getBorderStrokeColor() {
        return mStrokeColor;
    }

    /**
     * Set border stroke width
     *
     * @param strokeWidth stroke width in pixels
     */
    public void setBorderStrokeWidth(float strokeWidth) {
        if (mStrokeWidth != strokeWidth) {
            mStrokeWidth = strokeWidth;
            mStrokePaint.setStrokeWidth(mStrokeWidth);
            updateCornerPath();
            invalidate();
        }
    }

    /**
     * Get current border stroke width
     *
     * @return stroke width in pixels
     */
    public float getBorderStrokeWidth() {
        return mStrokeWidth;
    }

    public Boolean getIsFirstSelect() {
        return mIsFirstSelect;
    }

    public void setIsFirstSelect(Boolean mIsFirstSelect) {
        this.mIsFirstSelect = mIsFirstSelect;
    }

    /**
     * Set selected state for this view
     * @param isSelected true if selected, false otherwise
     */
    public void setSelected(Boolean isSelected) {
        // Handle null case
        boolean newSelected = isSelected != null ? isSelected : false;
        boolean currentSelected = mIsSelected != null ? mIsSelected : false;
        

        if (currentSelected != newSelected) {
            mIsSelected = newSelected;
            invalidate(); // Redraw to show/hide selected border
        } else {
        }
    }

    /**
     * Get selected state
     *
     * @return true if selected, false otherwise
     */
    public boolean isSelected() {
        boolean result = mIsSelected != null ? mIsSelected : false;
        return result;
    }

    /**
     * Set selected border color
     * @param color color for selected border
     */
    public void setSelectedStrokeColor(int color) {
        if (mSelectedStrokeColor != color) {
            mSelectedStrokeColor = color;
            if (mIsSelected) {
                invalidate(); // Redraw if currently selected
            }
        }
    }

    /**
     * Set selected border width
     * @param width width for selected border
     */
    public void setSelectedStrokeWidth(float width) {
        if (mSelectedStrokeWidth != width) {
            mSelectedStrokeWidth = width;
            if (mIsSelected) {
                invalidate(); // Redraw if currently selected
            }
        }
    }

    /**
     * Check if this view has a user-selected image (not default)
     * @return true if has user image, false if still default
     */
    public boolean hasUserImage() {
        // Treat as having a user image only after first selection is completed
        return mIsFirstSelect != null && mIsFirstSelect;
    }

    /**
     * Set gradient colors for selected border
     * @param colors array of colors for gradient (minimum 2 colors)
     */
    public void setSelectedGradientColors(int[] colors) {
        if (colors != null && colors.length >= 2) {
            mSelectedGradientColors = colors.clone();
            if (mIsSelected) {
                invalidate(); // Redraw if currently selected
            }
        }
    }

    /**
     * Enable or disable gradient border
     * @param useGradient true to use gradient, false to use solid color
     */
    public void setUseGradientBorder(boolean useGradient) {
        if (mUseGradientBorder != useGradient) {
            mUseGradientBorder = useGradient;
            if (mIsSelected) {
                invalidate(); // Redraw if currently selected
            }
        }
    }

    /**
     * Get current gradient colors
     * @return array of gradient colors
     */
    public int[] getSelectedGradientColors() {
        return mSelectedGradientColors.clone();
    }
}
