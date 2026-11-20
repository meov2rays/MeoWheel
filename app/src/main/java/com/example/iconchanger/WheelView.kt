package com.example.iconchanger

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.text.TextPaint
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import kotlin.math.*
import kotlin.random.Random

val PALETTE = intArrayOf(
    0xFFFF6B6B.toInt(), 0xFFFFA94D.toInt(), 0xFFFFD43B.toInt(), 0xFF69DB7C.toInt(),
    0xFF38D9A9.toInt(), 0xFF4DABF7.toInt(), 0xFF9775FA.toInt(), 0xFFF783AC.toInt()
)

fun colorAt(i: Int, n: Int): Int {
    var k = i % PALETTE.size
    if (n > 1 && i == n - 1 && k == 0) k = 4
    return PALETTE[k]
}

class WheelView(ctx: Context) : View(ctx) {
    var items: List<String> = emptyList()
        set(v) { field = v; invalidate() }
    private val d = resources.displayMetrics.density
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.RIGHT
        setShadowLayer(3f, 0f, 1.5f, 0x88000000.toInt())
    }

    override fun onDraw(c: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val outer = min(width, height) / 2f - 2 * d
        p.style = Paint.Style.FILL
        p.shader = LinearGradient(0f, 0f, width.toFloat(), height.toFloat(),
            0xFFFFE08A.toInt(), 0xFFC98A12.toInt(), Shader.TileMode.CLAMP)
        c.drawCircle(cx, cy, outer, p)
        p.shader = null
        p.color = 0xFF1B1238.toInt()
        c.drawCircle(cx, cy, outer - 6 * d, p)
        for (k in 0 until 24) {
            val a = k * 2.0 * PI / 24
            p.color = if (k % 2 == 0) 0xFFFFF3B0.toInt() else 0xFFFF8A3D.toInt()
            c.drawCircle(
                cx + (cos(a) * (outer - 14 * d)).toFloat(),
                cy + (sin(a) * (outer - 14 * d)).toFloat(), 3 * d, p
            )
        }
        val n = items.size
        if (n == 0) return
        val r = outer - 24 * d
        val rect = RectF(cx - r, cy - r, cx + r, cy + r)
        val sw = 360f / n
        for (i in 0 until n) {
            p.color = colorAt(i, n)
            c.drawArc(rect, -90f + i * sw - sw / 2, sw, true, p)
        }
        p.style = Paint.Style.STROKE
        p.strokeWidth = 1.5f * d
        p.color = 0x66FFFFFF
        for (i in 0 until n) {
            val a = Math.toRadians((-90f + i * sw - sw / 2).toDouble())
            c.drawLine(cx, cy, cx + (r * cos(a)).toFloat(), cy + (r * sin(a)).toFloat(), p)
        }
        p.style = Paint.Style.FILL

        val arc = (r * 0.6f * Math.toRadians(sw.toDouble())).toFloat()
        tp.textSize = min(r * 0.13f, max(arc * 0.6f, 11 * d))
        val maxW = r * 0.62f
        for (i in 0 until n) {
            c.save()
            c.rotate(-90f + i * sw, cx, cy)
            val t = TextUtils.ellipsize(items[i], tp, maxW, TextUtils.TruncateAt.END).toString()
            c.drawText(t, cx + r - 14 * d, cy + tp.textSize / 3f, tp)
            c.restore()
        }

        p.shader = RadialGradient(cx, cy, r * 0.14f, 0xFFFFF3B0.toInt(), 0xFFE0A100.toInt(), Shader.TileMode.CLAMP)
        c.drawCircle(cx, cy, r * 0.14f, p)
        p.shader = null
        p.color = 0xFF1B1238.toInt()
        c.drawCircle(cx, cy, r * 0.05f, p)
    }
}

class PointerView(ctx: Context) : View(ctx) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        pivotX = w / 2f
        pivotY = h * 0.3f
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        p.style = Paint.Style.FILL
        p.color = Color.WHITE
        val path = Path().apply {
            moveTo(w * 0.5f, h)
            lineTo(w * 0.1f, h * 0.38f)
            lineTo(w * 0.9f, h * 0.38f)
            close()
        }
        c.drawPath(path, p)
        c.drawCircle(w / 2f, h * 0.3f, w * 0.42f, p)
        p.color = 0xFFF2456B.toInt()
        c.drawCircle(w / 2f, h * 0.3f, w * 0.18f, p)
    }
}

class ConfettiView(ctx: Context) : View(ctx) {
    private class P(var x: Float, var y: Float, var vx: Float, var vy: Float,
                    val s: Float, val c: Int, var r: Float, val vr: Float)

    private val ps = ArrayList<P>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var life = 0f

    fun burst() {
        val d = resources.displayMetrics.density
        ps.clear()
        repeat(140) {
            val a = (-PI / 2 + (Random.nextFloat() - 0.5f) * PI * 1.1).toFloat()
            val sp = (400 + Random.nextFloat() * 900) * d
            ps.add(P(width / 2f, height * 0.45f, cos(a) * sp, sin(a) * sp,
                (5 + Random.nextFloat() * 6) * d, PALETTE[Random.nextInt(PALETTE.size)],
                Random.nextFloat() * 360f, (Random.nextFloat() - 0.5f) * 720f))
        }
        var last = 0f
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 3000
            addUpdateListener { a ->
                val f = a.animatedValue as Float
                val dt = (f - last) * 3f
                last = f
                life = f
                for (q in ps) {
                    q.x += q.vx * dt
                    q.y += q.vy * dt
                    q.vy += 1400 * d * dt
                    q.r += q.vr * dt
                }
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(c: Canvas) {
        val alpha = (255 * (1f - max(0f, (life - 0.7f) / 0.3f))).toInt().coerceIn(0, 255)
        for (q in ps) {
            c.save()
            c.translate(q.x, q.y)
            c.rotate(q.r)
            paint.color = q.c
            paint.alpha = alpha
            c.drawRect(-q.s / 2, -q.s / 4, q.s / 2, q.s / 4, paint)
            c.restore()
        }
    }
}

class FlowLayout(ctx: Context, private val hGap: Int, private val vGap: Int) : ViewGroup(ctx) {
    override fun onMeasure(wSpec: Int, hSpec: Int) {
        val maxW = MeasureSpec.getSize(wSpec) - paddingLeft - paddingRight
        var x = 0
        var y = 0
        var rowH = 0
        for (i in 0 until childCount) {
            val v = getChildAt(i)
            v.measure(MeasureSpec.makeMeasureSpec(maxW, MeasureSpec.AT_MOST),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED))
            if (x > 0 && x + v.measuredWidth > maxW) { x = 0; y += rowH + vGap; rowH = 0 }
            x += v.measuredWidth + hGap
            rowH = max(rowH, v.measuredHeight)
        }
        setMeasuredDimension(MeasureSpec.getSize(wSpec), y + rowH + paddingTop + paddingBottom)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val maxW = (r - l) - paddingLeft - paddingRight
        val rtl = layoutDirection == LAYOUT_DIRECTION_RTL
        var x = 0
        var y = 0
        var rowH = 0
        for (i in 0 until childCount) {
            val v = getChildAt(i)
            val w = v.measuredWidth
            val h = v.measuredHeight
            if (x > 0 && x + w > maxW) { x = 0; y += rowH + vGap; rowH = 0 }
            val left = if (rtl) (r - l) - paddingRight - x - w else paddingLeft + x
            v.layout(left, paddingTop + y, left + w, paddingTop + y + h)
            x += w + hGap
            rowH = max(rowH, h)
        }
    }
}
