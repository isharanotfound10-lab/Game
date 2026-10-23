package com.velvet.superkeyboard

import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.view.animation.LinearInterpolator

/** Horizontal glass reflection: 2100 ms travel, 1200 ms rest, 35% intensity. */
class ShineGlassDrawable(
    private val baseColor: Int,
    private val accentColor: Int,
    private val cornerRadius: Float
) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val boundsRect = RectF()
    private var phase = 0f
    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 3300L
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            phase = it.animatedValue as Float
            invalidateSelf()
        }
        start()
    }

    override fun draw(canvas: Canvas) {
        boundsRect.set(bounds)
        if (boundsRect.isEmpty) return
        paint.shader = null
        paint.color = baseColor
        canvas.drawRoundRect(boundsRect, cornerRadius, cornerRadius, paint)
        val travel = 2100f / 3300f
        if (phase <= travel) {
            val centerX = boundsRect.left - boundsRect.width() * 0.5f +
                (phase / travel) * boundsRect.width() * 2f
            val stripeWidth = boundsRect.width() * 0.34f
            paint.shader = LinearGradient(
                centerX - stripeWidth, 0f, centerX + stripeWidth, 0f,
                intArrayOf(Color.TRANSPARENT, Color.argb(89, 255, 255, 255), Color.TRANSPARENT),
                floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP
            )
            canvas.save()
            canvas.clipRect(boundsRect)
            canvas.drawRoundRect(boundsRect, cornerRadius, cornerRadius, paint)
            canvas.restore()
            paint.shader = null
        }
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        paint.color = Color.argb(95, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor))
        canvas.drawRoundRect(boundsRect, cornerRadius, cornerRadius, paint)
        paint.style = Paint.Style.FILL
    }

    override fun setAlpha(alpha: Int) { paint.alpha = alpha; invalidateSelf() }
    override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) { paint.colorFilter = colorFilter; invalidateSelf() }
    @Deprecated("Deprecated in Android")
    override fun getOpacity(): Int = android.graphics.PixelFormat.TRANSLUCENT
    override fun onBoundsChange(bounds: android.graphics.Rect) {
        super.onBoundsChange(bounds)
        boundsRect.set(bounds)
    }
    override fun setVisible(visible: Boolean, restart: Boolean): Boolean {
        if (visible) {
            if (restart) animator.start()
        } else animator.cancel()
        return super.setVisible(visible, restart)
    }
}