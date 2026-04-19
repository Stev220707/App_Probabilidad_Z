package com.example.calculadoraz

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import kotlin.math.exp
import kotlin.math.pow

class GraficaGaussView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var colorActual = Color.CYAN
    private var escala = 1f

    private lateinit var scaleDetector: ScaleGestureDetector

    private val paintCurva = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        isAntiAlias = true
    }

    private val paintLinea = Paint().apply {
        strokeWidth = 6f
        isAntiAlias = true
    }

    private val paintArea = Paint().apply {
        style = Paint.Style.FILL
        alpha = 80
        isAntiAlias = true
    }

    private var posicionZ: Float = 0f
    private var animador: ValueAnimator? = null
    private var animColor: ValueAnimator? = null

    private val handler = Handler(Looper.getMainLooper())

    init {
        scaleDetector = ScaleGestureDetector(context,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    escala *= detector.scaleFactor
                    escala = escala.coerceIn(0.5f, 3f)
                    invalidate()
                    return true
                }
            })
    }

    fun dibujarZ(z: Float) {
        detenerAnimacion()

        val anim = ValueAnimator.ofFloat(posicionZ, z)
        anim.duration = 600
        anim.addUpdateListener {
            posicionZ = it.animatedValue as Float
            invalidate()
        }
        anim.start()

        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({
            iniciarAnimacion()
        }, 4000)
    }

    fun iniciarAnimacion() {
        detenerAnimacion()

        animador = ValueAnimator.ofFloat(-3f, 3f).apply {
            duration = 2500
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            addUpdateListener {
                posicionZ = it.animatedValue as Float
                invalidate()
            }
        }
        animador?.start()

        animColor = ValueAnimator.ofArgb(
            Color.CYAN, Color.MAGENTA, Color.GREEN, Color.YELLOW, Color.CYAN
        ).apply {
            duration = 4000
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener {
                colorActual = it.animatedValue as Int
                invalidate()
            }
        }
        animColor?.start()
    }

    fun detenerAnimacion() {
        animador?.cancel()
        animColor?.cancel()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)

        val w = width.toFloat()
        posicionZ = ((event.x - w / 2f) / (w / 8f)).coerceIn(-3f, 3f)

        invalidate()
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.save()
        canvas.scale(escala, escala, width / 2f, height / 2f)

        val w = width.toFloat()
        val h = height.toFloat()

        val path = Path()
        val areaPath = Path()

        for (i in 0..w.toInt()) {
            val x = (i - w / 2f) / (w / 8f)
            val y = exp(-0.5 * x.toDouble().pow(2.0)).toFloat()
            val screenY = h - (y * h * 0.8f)

            if (i == 0) {
                path.moveTo(i.toFloat(), screenY)
                areaPath.moveTo(i.toFloat(), h)
            } else {
                path.lineTo(i.toFloat(), screenY)
            }

            if (x <= posicionZ) {
                areaPath.lineTo(i.toFloat(), screenY)
            }
        }

        areaPath.lineTo((posicionZ * (w / 8f)) + (w / 2f), h)
        areaPath.close()

        paintCurva.color = colorActual
        paintLinea.color = colorActual
        paintArea.color = colorActual

        paintCurva.setShadowLayer(25f, 0f, 0f, colorActual)
        paintLinea.setShadowLayer(30f, 0f, 0f, colorActual)

        canvas.drawPath(areaPath, paintArea)
        canvas.drawPath(path, paintCurva)

        val zX = (posicionZ * (w / 8f)) + (w / 2f)
        canvas.drawLine(zX, 0f, zX, h, paintLinea)

        val paintText = Paint().apply {
            color = colorActual
            textSize = 40f
            isAntiAlias = true
        }

        canvas.drawText("Z=%.2f".format(posicionZ), 40f, 50f, paintText)

        canvas.restore()
    }
}