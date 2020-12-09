package com.yhl.cast.client

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class ProgressBar(context: Context, attrs: AttributeSet?, defStyleAttr: Int): View(context, attrs, defStyleAttr) {

    constructor(context: Context, attrs: AttributeSet?): this(context, attrs, 0)

    constructor(context: Context): this(context, null)

    private var mProgress = 0
    private lateinit var mPaint: Paint

    init {
        val color = resources.getColor(R.color.progress_bar_front_color)
        mPaint = Paint()
        mPaint.color = color
    }



    fun setProgress(progress: Int) {
        mProgress = progress
        println("setProgress: $mProgress")
        invalidate()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        val width = measuredWidth * mProgress.toFloat() / 100.toFloat()
        println("onDraw: $width")
        if (width > 0) {
            canvas?.drawRect(0.toFloat(), 0.toFloat(), width, measuredHeight.toFloat(), mPaint)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        println("onLayout: $width, $height")
    }

}