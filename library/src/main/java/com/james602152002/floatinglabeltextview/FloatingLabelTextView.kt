package com.james602152002.floatinglabeltextview

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.VectorDrawable
import android.os.Build
import android.service.autofill.RegexValidator
import android.text.*
import android.text.InputFilter.LengthFilter
import android.text.style.BackgroundColorSpan
import android.text.style.CharacterStyle
import android.text.style.ForegroundColorSpan
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View.OnFocusChangeListener
import android.view.animation.AccelerateInterpolator
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.bitzsoft.kandroid.textWatcher
import java.lang.ref.SoftReference
import java.util.*
import kotlin.math.roundToInt

class FloatingLabelTextView : AppCompatTextView {

    private var labelHorizontalMargin = 0
    private var labelVerticalMargin = 0
    private val labelPaint: TextPaint

    var dividerStrokeWidth = 0
        set(value) {
            field = value
            dividerPaint.strokeWidth = dividerStrokeWidth.toFloat()
            updatePadding()
        }
    private val dividerPaint: Paint

    var dividerVerticalMargin = 0
        set(value) {
            field = value
            updatePadding()
        }
    var errorHorizontalMargin = 0
        set(value) {
            field = value
            updatePadding()
        }
    private val errorPaint: TextPaint

    private var highlightColor = 0
    private var dividerColor = 0
    private var mHintTextColor = 0
    private var errorColor = 0
    private var label: CharSequence? = null
    private var savedLabel: CharSequence? = null
    private var mPaddingLeft = 0
    private var mPaddingTop = 0
    private var mPaddingRight = 0
    private var mPaddingBottom = 0
    private var textPartHeight = -1

    var labelTextSize = 0f
        set(value) {
            field = value
            updatePadding()
        }
    private var hintTextSize = 0f
    var errorTextSize = 0f
        set(value) {
            field = value
            errorPaint.textSize = value
            measureTextMaxLength()
            updatePadding()
        }
    private var floatLabelAnimPercentage = 0f
    private var mAnimDuration = 0
    private var mErrorAnimDuration = 0
    private var isError = false
    private var error: CharSequence? = null
    private var errorAnimator: ObjectAnimator? = null
    private var errorPercentage = 0f
    private var mListener: OnFocusChangeListener? = null
    private var customizeListener: OnFocusChangeListener? = null
    private var hasFocus = false
    private var validatorList: MutableList<RegexValidator>? = null
    private var errorDisabled = false

    private var clearButtonPaint: Paint? = null
    var downArrowSize = 0
        set(value) {
            field = value
            invalidate()
        }
    private var downArrowColor = 0
    var enableDownArrow = false
        set(value) {
            field = value
            if (value) {
                initDownArrow()
                clearButtonPaint!!.apply {
                    textSize = downArrowSize.toFloat()
                    color = downArrowColor
                }
                customizeDownArrow(R.drawable.ic_selector_down_arrow, downArrowSize)
            } else {
                clearButtonPaint = null
                bounds = null
            }
            updatePadding()
        }
    private var downArrowHorizontalMargin = 0
    private var bounds: Rect? = null
    private var downArrowBitmap: Bitmap? = null
    private var bitmapHeight = 0

    private var multilineMode = false

    private var touchDownArrow = false

    //    private var downX = 0f
//    private var downY = 0f
//    private val touchSlop: Int by lazy { ViewConfiguration.get(context).scaledTouchSlop }
    private var clearPaintAlphaRatio = 1.0f
    private var terminateClick = false
//    private var showClearButtonWithoutFocus = false

    private var maxLength = 0
    private var showMaxLength = false
    private val maxLengthPaint: TextPaint
    private var textLengthDisplayColor = 0
    private var maxLengthTextWidth = 0
    private var startValue = -1f
    private var isMustFill = false

    private val widgetLayerRect = RectF()
    private val widgetPaint: Paint
    private val errorAnimRect = RectF()
    private val errorAnimPaint: Paint

    constructor(context: Context) : super(context) {
        init(context, null)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int)
            : super(context, attrs, defStyleAttr) {
        init(context, attrs)
    }

    init {
        val antiAliasFlag = Paint.ANTI_ALIAS_FLAG
        labelPaint = TextPaint(antiAliasFlag)
        dividerPaint = Paint(antiAliasFlag)
        errorPaint = TextPaint(antiAliasFlag)
        maxLengthPaint = TextPaint(antiAliasFlag)
        widgetPaint = Paint(antiAliasFlag)
        errorAnimPaint = Paint(antiAliasFlag)
        textWatcher {
            afterTextChanged {
                when (it.isNullOrEmpty()) {
                    true -> startAnimator(1f, 0f)
                    else -> startAnimator(0f, 1f)
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    @SuppressLint("ResourceType")
    private fun init(
        context: Context,
        attrs: AttributeSet?
    ) {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        val defaultArray =
            context.obtainStyledAttributes(intArrayOf(R.attr.colorPrimary))
        val primaryColor = defaultArray.getColor(0, 0)
        defaultArray.recycle()
        val typedArray =
            context.obtainStyledAttributes(attrs, R.styleable.FloatingLabelTextView)
        labelHorizontalMargin = typedArray.getDimensionPixelOffset(
            R.styleable.FloatingLabelTextView_j_flt_label_horizontal_margin,
            0
        )
        labelVerticalMargin = typedArray.getDimensionPixelOffset(
            R.styleable.FloatingLabelTextView_j_flt_label_vertical_margin,
            0
        )
        errorHorizontalMargin = typedArray.getDimensionPixelOffset(
            R.styleable.FloatingLabelTextView_j_flt_error_horizontal_margin,
            0
        )
        dividerVerticalMargin = typedArray.getDimensionPixelOffset(
            R.styleable.FloatingLabelTextView_j_flt_divider_vertical_margin,
            0
        )
        highlightColor = typedArray.getColor(
            R.styleable.FloatingLabelTextView_j_flt_colorHighlight,
            primaryColor
        )
        dividerColor = typedArray.getColor(
            R.styleable.FloatingLabelTextView_j_flt_colorDivider,
            Color.GRAY
        )
        errorColor = typedArray.getColor(
            R.styleable.FloatingLabelTextView_j_flt_colorError,
            Color.RED
        )
        label = typedArray.getString(R.styleable.FloatingLabelTextView_j_flt_hint)
        dividerStrokeWidth = typedArray.getDimensionPixelOffset(
            R.styleable.FloatingLabelTextView_j_flt_thickness,
            dp2px(2f)
        )
        labelTextSize = typedArray.getDimensionPixelOffset(
            R.styleable.FloatingLabelTextView_j_flt_label_textSize,
            sp2Px(16f)
        ).toFloat()
        errorTextSize = typedArray.getDimensionPixelOffset(
            R.styleable.FloatingLabelTextView_j_flt_error_textSize,
            sp2Px(16f)
        ).toFloat()
        dividerPaint.strokeWidth = dividerStrokeWidth.toFloat()
        errorPaint.textSize = errorTextSize
        mAnimDuration = typedArray.getInteger(
            R.styleable.FloatingLabelTextView_j_flt_float_anim_duration,
            800
        )
        mErrorAnimDuration = typedArray.getInteger(
            R.styleable.FloatingLabelTextView_j_flt_error_anim_duration,
            8000
        )
        errorDisabled =
            typedArray.getBoolean(R.styleable.FloatingLabelTextView_j_flt_error_disable, false)
        multilineMode = typedArray.getBoolean(
            R.styleable.FloatingLabelTextView_j_flt_multiline_mode_enable,
            false
        )
        enableDownArrow =
            typedArray.getBoolean(R.styleable.FloatingLabelTextView_j_flt_enable_down_arrow, false)
        downArrowColor = typedArray.getColor(
            R.styleable.FloatingLabelTextView_j_flt_down_arrow_color,
            -0x56000000
        )
        downArrowHorizontalMargin = typedArray.getDimensionPixelOffset(
            R.styleable.FloatingLabelTextView_j_flt_down_arrow_horizontal_margin,
            dp2px(5f)
        )
        val downArrowId =
            typedArray.getResourceId(R.styleable.FloatingLabelTextView_j_flt_down_arrow_id, -1)
        showMaxLength =
            typedArray.getBoolean(R.styleable.FloatingLabelTextView_j_flt_show_text_length, false)
        textLengthDisplayColor = typedArray.getColor(
            R.styleable.FloatingLabelTextView_j_flt_text_length_display_color,
            highlightColor
        )
        isMustFill =
            typedArray.getBoolean(R.styleable.FloatingLabelTextView_j_flt_must_fill_type, false)
//        val decimalValidation =
//            typedArray.getString(R.styleable.FloatingLabelTextView_j_flt_number_decimal_validation)
//        if (!TextUtils.isEmpty(decimalValidation)) {
//            addValidator(NumberDecimalValidator(decimalValidation))
//        }
        if (mAnimDuration < 0) mAnimDuration = 800
        if (mErrorAnimDuration < 0) mErrorAnimDuration = 8000
        val textTypedArray =
            context.obtainStyledAttributes(attrs, intArrayOf(android.R.attr.textSize))
        hintTextSize = textTypedArray.getDimensionPixelOffset(0, sp2Px(20f)).toFloat()
        if (textSize != hintTextSize) {
            textSize = hintTextSize
        }
        labelPaint.textSize = hintTextSize
        textPartHeight = (hintTextSize.roundToInt() * 1.2f).toInt()
        textTypedArray.recycle()
        val hintTypedArray =
            context.obtainStyledAttributes(attrs, intArrayOf(android.R.attr.hint))
        if (TextUtils.isEmpty(label)) label = hintTypedArray.getString(0) else hint = label
        savedLabel = label
        mHintTextColor = currentHintTextColor
        setHintTextColor(0)
        hintTypedArray.recycle()
        val backgroundTypedArray =
            context.obtainStyledAttributes(attrs, intArrayOf(android.R.attr.background))
        val background = backgroundTypedArray.getDrawable(0)
        if (background != null) {
            setBackgroundDrawable(background)
        } else setBackgroundColor(0)
        backgroundTypedArray.recycle()
        downArrowSize = typedArray.getDimensionPixelOffset(
            R.styleable.FloatingLabelTextView_j_flt_down_arrow_size,
            (textSize * .8f).toInt()
        )
        typedArray.recycle()
        val paddingArray = context.obtainStyledAttributes(
            attrs,
            intArrayOf(
                android.R.attr.padding,
                android.R.attr.paddingLeft,
                android.R.attr.paddingTop,
                android.R.attr.paddingRight,
                android.R.attr.paddingBottom
            )
        )
        if (paddingArray.hasValue(0)) {
            mPaddingBottom = paddingArray.getDimensionPixelOffset(0, 0)
            mPaddingRight = mPaddingBottom
            mPaddingTop = mPaddingRight
            mPaddingLeft = mPaddingTop
        } else {
            mPaddingLeft = (if (paddingArray.hasValue(1)) paddingArray.getDimensionPixelOffset(
                1,
                paddingLeft
            ) else 0)
            mPaddingTop = (if (paddingArray.hasValue(2)) paddingArray.getDimensionPixelOffset(
                2,
                paddingTop
            ) else 0)
            mPaddingRight = (if (paddingArray.hasValue(3)) paddingArray.getDimensionPixelOffset(
                3,
                paddingRight
            ) else 0)
            mPaddingBottom = (if (paddingArray.hasValue(4)) paddingArray.getDimensionPixelOffset(
                4,
                paddingBottom
            ) else 0)
        }
        paddingArray.recycle()
        val textLengthArray =
            context.obtainStyledAttributes(attrs, intArrayOf(android.R.attr.maxLength))
        maxLength = textLengthArray.getInteger(0, -1)
        textLengthArray.recycle()
        val onClickTypedArray =
            context.obtainStyledAttributes(attrs, intArrayOf(android.R.attr.onClick))
        if (!TextUtils.isEmpty(onClickTypedArray.getString(0))) {
            initOnClickListener()
        }
        onClickTypedArray.recycle()
        includeFontPadding = false
        initFocusChangeListener()
        setSingleLine()
        updatePadding()
        if (downArrowId >= 0) {
            customizeDownArrow(downArrowId, downArrowSize)
        }
        updateLabel()
    }

    private fun initFocusChangeListener() {
        onFocusChangeListener = OnFocusChangeListener { v, hasFocus ->
            this@FloatingLabelTextView.hasFocus = hasFocus
            if (TextUtils.isEmpty(text)) {
                if (hasFocus && floatLabelAnimPercentage != 1f) {
                    startAnimator(0f, 1f)
                } else if (!hasFocus && floatLabelAnimPercentage != 0f) {
                    startAnimator(1f, 0f)
                }
            }
            customizeListener?.onFocusChange(v, hasFocus)
        }
    }

    override fun setOnClickListener(l: OnClickListener?) {
        super.setOnClickListener(l)
        if (l != null) {
//            setRawInputType(InputType.TYPE_NULL);
//        setInputType(InputType.TYPE_NULL);
            initOnClickListener()
        }
    }

    private fun initOnClickListener() {
        isFocusable = false
        isFocusableInTouchMode = false
//        addTextChangedListener(new TextWatcher() {
//            @Override
//            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
//
//            }
//
//            @Override
//            public void onTextChanged(CharSequence s, int start, int before, int count) {
//
//            }
//
//            @Override
//            public void afterTextChanged(Editable s) {
//
//            }
//        });
    }

    private fun startAnimator(startValue: Float, endValue: Float) {
        val sameAnimator = this.startValue == startValue
        this.startValue = startValue
        if (sameAnimator) {
            return
        }
        val animator =
            ObjectAnimator.ofFloat(this, "float_label_anim_percentage", startValue, endValue)
        animator.interpolator = AccelerateInterpolator(3f)
        animator.duration = mAnimDuration.toLong()
        post { animator.start() }
    }

    override fun setOnFocusChangeListener(l: OnFocusChangeListener?) {
        if (mListener == null) {
            mListener = l
        } else {
            customizeListener = l
        }
        super.setOnFocusChangeListener(mListener)
    }

    override fun getOnFocusChangeListener(): OnFocusChangeListener? {
        return if (customizeListener != null) customizeListener else super.getOnFocusChangeListener()
    }

    private fun dp2px(dpValue: Float): Int {
        return (0.5f + dpValue * resources.displayMetrics.density).toInt()
    }

    private fun sp2Px(spValue: Float): Int {
        return (spValue * resources.displayMetrics.scaledDensity).toInt()
    }

    override fun setPadding(left: Int, top: Int, right: Int, bottom: Int) {
        mPaddingLeft = left
        this.mPaddingTop = top
        this.mPaddingRight = right
        this.mPaddingBottom = bottom
        super.setPadding(
            left,
            top + labelVerticalMargin + labelTextSize.toInt(),
            right + getDownArrowModePadding(),
            bottom + dividerStrokeWidth + dividerVerticalMargin + if (!errorDisabled) (errorTextSize * 1.2f).toInt() + (dividerVerticalMargin shl 1) else 0
        )
    }

    private fun getDownArrowModePadding(): Int {
        return if (enableDownArrow) downArrowSize + (downArrowHorizontalMargin shl 1) else 0
    }

    private fun updatePadding() {
        setPadding(mPaddingLeft, mPaddingTop, mPaddingRight, mPaddingBottom)
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val width = width
        labelPaint.color = if (hasFocus) highlightColor else mHintTextColor
        val currentTextSize =
            hintTextSize + (labelTextSize - hintTextSize) * floatLabelAnimPercentage
        labelPaint.textSize = currentTextSize
//        val scrollX = scrollX
        if (text.isNotEmpty()) {
            labelPaint.alpha = (255 * floatLabelAnimPercentage).toInt()
        } else {
            labelPaint.alpha = 255
        }

        val labelPaintDy =
            (mPaddingTop + labelTextSize + currentTextSize * (1 - floatLabelAnimPercentage) * .93f).toInt()
        label?.let {
            drawSpannableString(
                canvas,
                it,
                labelPaint,
                scrollX + labelHorizontalMargin,
                labelPaintDy
            )
        }

        val dividerY =
            (mPaddingTop + labelTextSize + labelVerticalMargin + textPartHeight * lineCount + (dividerStrokeWidth shr 1) + dividerVerticalMargin).toInt()
        if (!isError) {
            dividerPaint.color = if (hasFocus) highlightColor else dividerColor
        } else {
            dividerPaint.color = errorColor
            val errorPaintDy = (dividerY + errorTextSize + dividerVerticalMargin).toInt()
            val errorTextWidth = errorPaint.measureText(error.toString())
            val hintRepeatSpaceWidth = width / 3
            val maxDx = hintRepeatSpaceWidth + errorTextWidth
            val startX =
                errorHorizontalMargin - (maxDx * errorPercentage).toInt() + scrollX
            errorPaint.color = errorColor
            if (errorAnimator != null) {
                if (errorHorizontalMargin > 0 && errorPaint.shader == null) {
                    val marginRatio = errorHorizontalMargin.toFloat() / width
                    val gradientRatio = .025f
                    errorPaint.shader = LinearGradient(
                        0f,
                        0f,
                        width.toFloat(),
                        0f,
                        intArrayOf(0, errorColor, errorColor, 0),
                        floatArrayOf(
                            marginRatio,
                            marginRatio + gradientRatio,
                            1 - marginRatio - gradientRatio,
                            1 - marginRatio
                        ),
                        Shader.TileMode.CLAMP
                    )
                } else if (errorHorizontalMargin == 0) {
                    errorPaint.shader = null
                }
            }

            widgetLayerRect.set(0f, 0f, (width + scrollX).toFloat(), height.toFloat())

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                canvas.saveLayer(widgetLayerRect, widgetPaint)
            } else {
                @Suppress("DEPRECATION")
                canvas.saveLayer(widgetLayerRect, widgetPaint, Canvas.ALL_SAVE_FLAG)
            }

            drawSpannableString(canvas, error, errorPaint, startX, errorPaintDy)
            if (startX < 0 && startX + maxDx < width) {
                drawSpannableString(
                    canvas,
                    error,
                    errorPaint,
                    (startX + maxDx).toInt(),
                    errorPaintDy
                )
            }
            if (maxLengthTextWidth > 0) {
//                val paint = Paint()
//                paint.color = Color.WHITE
//                val rect = RectF(
//                    (width + scrollX - maxLengthTextWidth - errorHorizontalMargin).toFloat(),
//                    dividerY.toFloat(),
//                    (width + scrollX).toFloat(),
//                    height.toFloat()
//                )
                errorAnimRect.set(
                    (width + scrollX - maxLengthTextWidth - errorHorizontalMargin).toFloat(),
                    dividerY.toFloat(),
                    (width + scrollX).toFloat(),
                    height.toFloat()
                )
//                paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
//                canvas.drawRect(rect, paint)
//                paint.xfermode = null
                errorAnimPaint.apply {
                    color = Color.WHITE
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                    canvas.drawRect(errorAnimRect, errorAnimPaint)
                    xfermode = null
                }
                canvas.restore()
            }
        }
        canvas.drawLine(
            scrollX.toFloat(),
            dividerY.toFloat(),
            width + scrollX.toFloat(),
            dividerY.toFloat(),
            dividerPaint
        )
//        if (hasFocus || showClearButtonWithoutFocus) {
        drawDownArrow(canvas, scrollX)
//        }
        if (showMaxLength) drawMaxLength(
            canvas,
            width + scrollX,
            dividerY + errorTextSize + dividerVerticalMargin
        )
    }

    private fun drawSpannableString(
        canvas: Canvas,
        hint: CharSequence?,
        paint: TextPaint,
        start_x: Int,
        start_y: Int
    ) {
        // draw each span one at a time
        var ellipsizeHint = hint
        var next: Int
        var xStart = start_x.toFloat()
        var xEnd: Float
        if (paint != errorPaint)
            ellipsizeHint = TextUtils.ellipsize(
                ellipsizeHint, paint,
                (width - mPaddingLeft - mPaddingRight - labelHorizontalMargin - getDownArrowModePadding()).toFloat(),
                TextUtils.TruncateAt.END
            )
        if (ellipsizeHint is SpannableString) {
            val spannableString = ellipsizeHint
            var i = 0
            while (i < spannableString.length) {

                // find the next span transition
                next = spannableString.nextSpanTransition(
                    i,
                    spannableString.length,
                    CharacterStyle::class.java
                )

                // measure the length of the span
                xEnd = xStart + paint.measureText(spannableString, i, next)

                // draw the highlight (background color) first
                val bgSpans =
                    spannableString.getSpans(
                        i, next,
                        BackgroundColorSpan::class.java
                    )
                if (bgSpans.isNotEmpty()) {
                    val mHighlightPaint =
                        Paint(Paint.ANTI_ALIAS_FLAG)
                    mHighlightPaint.color = bgSpans[0].backgroundColor
                    canvas.drawRect(
                        xStart,
                        start_y + paint.fontMetrics.top,
                        xEnd,
                        start_y + paint.fontMetrics.bottom,
                        mHighlightPaint
                    )
                }

                // draw the text with an optional foreground color
                val fgSpans =
                    spannableString.getSpans(
                        i, next,
                        ForegroundColorSpan::class.java
                    )
                if (fgSpans.isNotEmpty()) {
                    val saveColor = paint.color
                    paint.color = fgSpans[0].foregroundColor
                    canvas.drawText(spannableString, i, next, xStart, start_y.toFloat(), paint)
                    paint.color = saveColor
                } else {
                    canvas.drawText(spannableString, i, next, xStart, start_y.toFloat(), paint)
                }
                xStart = xEnd
                i = next
            }
        } else {
            canvas.drawText(
                ellipsizeHint!!,
                0,
                ellipsizeHint.length,
                xStart,
                start_y.toFloat(),
                paint
            )
        }
    }

    private fun drawDownArrow(canvas: Canvas, scrollX: Int) {
        if (enableDownArrow && clearButtonPaint != null) {
            clearButtonPaint!!.alpha = (clearPaintAlphaRatio * 255).toInt()
            downArrowBitmap?.let {
                canvas.drawBitmap(
                    it,
                    (width - mPaddingRight + scrollX - downArrowSize - downArrowHorizontalMargin).toFloat(),
                    mPaddingTop + labelTextSize + (labelVerticalMargin + hintTextSize + dividerVerticalMargin - bitmapHeight) * .5f,
                    clearButtonPaint
                )
            }
        }
    }

    private fun measureTextMaxLength(): Int {
        if (maxLength <= 0) return 0
        return if (showMaxLength) {
            val textLength: Int = text.length
            maxLengthPaint.textSize = errorTextSize
            val lengthStrBuilder = StringBuilder()
            lengthStrBuilder.append(textLength).append("/").append(maxLength)
            val lengthStr = lengthStrBuilder.toString()
            maxLengthPaint.measureText(lengthStr).roundToInt()
        } else {
            0
        }
    }

    private fun drawMaxLength(
        canvas: Canvas,
        dx: Int,
        dy: Float
    ) {
        if (maxLengthTextWidth == 0) return
        maxLengthPaint.color = textLengthDisplayColor
        val textLength: Int = text.length
        val lengthStrBuilder = StringBuilder()
        lengthStrBuilder.append(textLength).append("/").append(maxLength)
        val lengthStr = lengthStrBuilder.toString()
        canvas.drawText(
            lengthStr,
            dx - maxLengthTextWidth - mPaddingRight.toFloat(),
            dy,
            maxLengthPaint
        )
    }

    private fun setFloat_label_anim_percentage(float_label_anim_percentage: Float) {
        this.floatLabelAnimPercentage = float_label_anim_percentage
        postInvalidate()
    }

    fun setLabelMargins(horizontal_margin: Int, vertical_margin: Int) {
        labelHorizontalMargin = horizontal_margin
        labelVerticalMargin = vertical_margin
        updatePadding()
    }

    fun getLabel(): CharSequence? {
        return label
    }

    fun setLabel(hint: CharSequence?) {
        savedLabel = hint
        updateLabel()
    }

    fun setAnimDuration(animDuration: Int) {
        var calAnimDuration = animDuration
        if (calAnimDuration < 0) calAnimDuration = 800
        this.mAnimDuration = calAnimDuration
    }

    fun getAnimDuration(): Int {
        return mAnimDuration
    }

    fun setErrorAnimDuration(errorAnimDuration: Int) {
        var calErrorAnimDuration = errorAnimDuration
        if (calErrorAnimDuration < 0) calErrorAnimDuration = 8000
        mErrorAnimDuration = calErrorAnimDuration
    }

    fun getErrorAnimDuration(): Int {
        return mErrorAnimDuration
    }

    override fun setHighlightColor(color: Int) {
        highlightColor = color
        super.setHighlightColor(color)
    }

    override fun getHighlightColor(): Int {
        return highlightColor
    }

    fun getHint_text_color(): Int {
        return mHintTextColor
    }

    fun setHint_text_color(hint_text_color: Int) {
        this.mHintTextColor = hint_text_color
    }

    fun getError_color(): Int {
        return errorColor
    }

    fun setError_color(error_color: Int) {
        this.errorColor = error_color
    }

    override fun getError(): CharSequence? {
        return error
    }

    override fun setError(error: CharSequence?) {
        if (errorDisabled) {
            return
        }
        isError = !TextUtils.isEmpty(error)
        this.error = error
        if (isError) {
            if (width > 0) {
                startErrorAnimation()
            } else {
                startErrorAnimation()
            }
        } else {
            post {
                if (errorAnimator != null) {
                    errorAnimator!!.cancel()
                    errorAnimator = null
                }
            }
        }
        invalidate()
    }

    private fun startErrorAnimation() {
        val errorLength = errorPaint.measureText(error.toString())
        val w = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        val h = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        measure(w, h)
        val width = if (width > 0) width else measuredWidth
        val maxLengthWidth = 0
        if (showMaxLength) {
            maxLengthTextWidth = measureTextMaxLength()
        }
        if (errorLength > width - (errorHorizontalMargin shl 1) - maxLengthWidth) {
            errorPercentage = 0f
            if (errorAnimator == null)
                errorAnimator = ObjectAnimator.ofFloat(this, "error_percentage", 0f, 1f)

            errorAnimator!!.apply {
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.RESTART
                startDelay = mAnimDuration.toLong()
                var calDuration =
                    (mErrorAnimDuration * errorLength / width).toInt()
                if (calDuration < 0) calDuration = 8000
                duration = calDuration.toLong()
                post { errorAnimator?.start() }
            }
        } else {
            errorPercentage = 0f
        }
    }

    private fun setError_percentage(error_percentage: Float) {
        this.errorPercentage = error_percentage
        invalidate()
    }

    override fun setTextSize(size: Float) {
        hintTextSize = size
        textPartHeight = (hintTextSize.roundToInt() * 1.2f).toInt()
        super.setTextSize(size)
    }

    override fun setTextSize(unit: Int, size: Float) {
        val c = context
        val r = c.resources
        hintTextSize = TypedValue.applyDimension(unit, size, r.displayMetrics)
        textPartHeight = (hintTextSize.roundToInt() * 1.2f).toInt()
        super.setTextSize(unit, size)
    }

    fun getDivider_color(): Int {
        return dividerColor
    }

    fun setDivider_color(divider_color: Int) {
        this.dividerColor = divider_color
    }

    fun setValidatorList(list: List<RegexValidator>?) {
        if (list != null) {
            if (validatorList == null) validatorList =
                ArrayList<RegexValidator>() else validatorList!!.clear()
            validatorList!!.addAll(list)
        }
    }

    fun getValidatorList(): List<RegexValidator>? {
        return validatorList
    }

    fun addValidator(validator: RegexValidator?) {
        if (validatorList == null) validatorList = ArrayList<RegexValidator>()
        if (validator != null) validatorList!!.add(validator)
    }

    fun isError_disabled(): Boolean {
        return errorDisabled
    }

    fun setError_disabled() {
        errorDisabled = true
        updatePadding()
    }

    fun setError_enabled() {
        errorDisabled = false
        updatePadding()
    }

    fun customizeDownArrow(drawableId: Int, down_arrow_width: Int) {
        downArrowSize = down_arrow_width
        val context = context
        val drawable = ContextCompat.getDrawable(context, drawableId)
        val resources = resources
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        val sampleBitmap = createBitmap(drawable, resources, drawableId, options)
        var sampleSize = 1
        var width = options.outWidth
        var height = options.outHeight
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && drawable is VectorDrawable) {
            width = drawable.getIntrinsicWidth()
            height = drawable.getIntrinsicHeight()
        }
        bitmapHeight = (height * down_arrow_width / width)
        val destinationHeight = bitmapHeight
        if (height > destinationHeight || width > down_arrow_width) {
            val halfHeight = height shr 1
            val halfWidth = width shr 1
            while (halfHeight / sampleSize > destinationHeight && halfWidth / sampleSize > down_arrow_width) {
                sampleSize *= 2
            }
        }
        sampleBitmap?.recycle()
        options.inSampleSize = sampleSize
        options.inJustDecodeBounds = false
        val oldBitmap = createBitmap(drawable, resources, drawableId, options)
        width = oldBitmap!!.width
        height = oldBitmap.height
        val matrix = Matrix()
        val scaleX = down_arrow_width.toFloat() / width
        val scaleY = destinationHeight.toFloat() / height
        matrix.postScale(scaleX, scaleY)
        downArrowBitmap = SoftReference(
            Bitmap.createBitmap(oldBitmap, 0, 0, width, height, matrix, true)
        ).get()
        initDownArrow()
        updatePadding()
    }

    private fun createBitmap(
        drawable: Drawable?,
        resources: Resources,
        drawableId: Int,
        options: BitmapFactory.Options
    ): Bitmap? {
        return if (drawable is BitmapDrawable) {
            getBitmap(resources, drawableId, options)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && drawable is VectorDrawable) {
            getVectorBitmap(drawable)
        } else {
            throw IllegalArgumentException("unsupported drawable type")
        }
    }

    private fun getBitmap(
        resources: Resources,
        drawableId: Int,
        options: BitmapFactory.Options
    ): Bitmap? {
        return SoftReference(
            BitmapFactory.decodeResource(resources, drawableId, options)
        ).get()
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private fun getVectorBitmap(vectorDrawable: VectorDrawable): Bitmap? {
        val bitmap = Bitmap.createBitmap(
            vectorDrawable.intrinsicWidth,
            vectorDrawable.intrinsicHeight, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        vectorDrawable.setBounds(0, 0, canvas.width, canvas.height)
        vectorDrawable.draw(canvas)
        return bitmap
    }

    private fun initDownArrow() {
        if (clearButtonPaint == null) {
            clearButtonPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        }
    }

    override fun setSingleLine() {
        if (multilineMode) return
        super.setSingleLine()
    }

    fun setMultiline_mode(enable: Boolean) {
        multilineMode = enable
        isSingleLine = !enable
    }

    fun setClear_btn_color(down_arrow_color: Int) {
        this.downArrowColor = down_arrow_color
        invalidate()
    }

    fun getClear_btn_color(): Int {
        return downArrowColor
    }

    fun getClear_btn_horizontal_margin(): Int {
        return downArrowHorizontalMargin
    }

    fun setClear_btn_horizontal_margin(down_arrow_horizontal_margin: Int) {
        this.downArrowHorizontalMargin = down_arrow_horizontal_margin
        invalidate()
    }

//    //Even your edit text doesn't have focus, your clear button still show at right.
//    fun showClearButtonWithoutFocus() {
//        showClearButtonWithoutFocus = true
//        enableDownArrow = true
//    }

//    @SuppressLint("ClickableViewAccessibility")
//    override fun onTouchEvent(event: MotionEvent): Boolean {
//        if (enableDownArrow && (hasFocus || showClearButtonWithoutFocus)) {
//            when (event.action) {
//                MotionEvent.ACTION_DOWN -> {
//                    downX = event.x
//                    downY = event.y
//                    touchDownArrow = touchDownArrow(downX, downY)
//                    if (touchDownArrow) {
//                        fadeDownArrowIcon(true)
//                        post { requestFocus() }
//                        return true
//                    }
//                }
//                MotionEvent.ACTION_MOVE -> if (touchDownArrow && (Math.abs(downX - event.x) >= touchSlop || Math.abs(
//                        downY - event.y
//                    ) >= touchSlop)
//                ) {
//                    touchDownArrow = false
//                    terminateClick = true
//                }
//                MotionEvent.ACTION_UP -> {
//                    val interruptActionUp = touchDownArrow || terminateClick
//                    if (touchDownArrow) {
//                        text = null
//                    }
//                    reset()
//                    if (interruptActionUp) return false
//                }
//                MotionEvent.ACTION_CANCEL -> reset()
//            }
//        }
//        return super.onTouchEvent(event)
//    }

    private fun touchDownArrow(x: Float, y: Float): Boolean {
        val width = width
        if (measuredWidth <= 0) {
            val w = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
            measure(w, w)
        }
        val right = if (width != 0) width else measuredWidth
        val downArrowWidth =
            (downArrowSize + (downArrowHorizontalMargin shl 1) + scaleX)
        val downArrowTop = (mPaddingTop + labelTextSize)
        val downArrowBottom =
            downArrowTop + labelVerticalMargin + textPartHeight + dividerVerticalMargin
        return x >= right - downArrowWidth && x <= right && y >= downArrowTop && y <= downArrowBottom
    }

    @Synchronized
    private fun fadeDownArrowIcon(focus: Boolean) {
        val defaultValue = 1f
        val focusValue = 0.5f
        ObjectAnimator.ofFloat(
            this, "clear_paint_alpha_ratio",
            if (focus) defaultValue else focusValue, if (focus) focusValue else defaultValue
        ).apply {
            duration = 500
        }.start()
    }

    private fun reset() {
        if (terminateClick || touchDownArrow) fadeDownArrowIcon(false)
        terminateClick = false
        touchDownArrow = false
    }

    private fun setClear_paint_alpha_ratio(clear_paint_alpha_ratio: Float) {
        this.clearPaintAlphaRatio = clear_paint_alpha_ratio
        postInvalidate()
    }

    override fun setFilters(filters: Array<InputFilter?>) {
        for (i in filters.indices) {
            val filter = filters[i]
            if (filter is LengthFilter) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    maxLength = filter.max
                } else {
                    try {
                        val field =
                            LengthFilter::class.java.getDeclaredField("mMax")
                        field.isAccessible = true
                        maxLength = field[filter] as Int
                    } catch (e: Exception) {
                    }
                }
                break
            }
        }
        super.setFilters(filters)
    }

    fun showMaxTextLength(show: Boolean) {
        showMaxLength = show
        invalidate()
    }

    fun isShowMaxTextLength(): Boolean {
        return showMaxLength
    }

    fun getText_length_display_color(): Int {
        return textLengthDisplayColor
    }

    fun setText_length_display_color(text_length_display_color: Int) {
        this.textLengthDisplayColor = text_length_display_color
        invalidate()
    }

    fun isError(): Boolean {
        return isError
    }

    fun isMustFill(): Boolean {
        return isMustFill
    }

    fun setMustFillMode(isMustFill: Boolean) {
        this.isMustFill = isMustFill
        updateLabel()
    }

    private fun updateLabel() {
        label = when (isMustFill) {
            true -> {
                SpannableString(savedLabel.toString() + " *").apply {
                    setSpan(
                        ForegroundColorSpan(Color.RED),
                        length - 1, length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
            else -> {
                savedLabel
            }
        }
        invalidate()
    }
}