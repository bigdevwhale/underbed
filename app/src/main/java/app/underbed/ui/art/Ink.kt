package app.underbed.ui.art

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.vector.PathParser
import app.underbed.ui.theme.C
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The pen: SVG-style paths, cross-hatching and candle glow. Every picture is drawn in its own
 * design units (the scene under the bed is 390 wide) and scaled to the screen, so a line keeps the
 * weight it has in the sketch.
 */
object Ink {
    private val cache = HashMap<String, Path>()

    /** A path from SVG path data, parsed once. */
    fun p(d: String): Path {
        if (cache.size > 3000) cache.clear()
        return cache.getOrPut(d) { PathParser().parsePathString(d).toPath() }
    }

    /** Parallel lines across [r] at [angle] degrees, [gap] apart, as one path. */
    fun lines(r: Rect, gap: Float, angle: Float): Path {
        val key = "h${r.left},${r.top},${r.right},${r.bottom},$gap,$angle"
        return cache.getOrPut(key) {
            val a = Math.toRadians(angle.toDouble())
            val dx = cos(a).toFloat(); val dy = sin(a).toFloat()
            val nx = -dy; val ny = dx
            val cs = listOf(r.topLeft, r.topRight, r.bottomLeft, r.bottomRight)
            val proj = cs.map { it.x * nx + it.y * ny }
            val lo = proj.min(); val hi = proj.max()
            val len = sqrt(r.width * r.width + r.height * r.height)
            val c = r.center
            val base = c.x * nx + c.y * ny
            val path = Path()
            var o = lo
            while (o <= hi) {
                val k = o - base
                val px = c.x + nx * k; val py = c.y + ny * k
                path.moveTo(px - dx * len, py - dy * len)
                path.lineTo(px + dx * len, py + dy * len)
                o += gap
            }
            path
        }
    }
}

fun DrawScope.ink(d: String, color: Color = C.ink) = drawPath(Ink.p(d), color)

fun DrawScope.pen(d: String, width: Float, color: Color = C.ink, alpha: Float = 1f) =
    drawPath(Ink.p(d), color, alpha = alpha, style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))

/** Hatching over [r], optionally only inside [clip]. */
fun DrawScope.hatch(r: Rect, gap: Float = 5f, angle: Float = 50f, width: Float = 1f, color: Color = C.ink, alpha: Float = 1f, clip: Path? = null) {
    val l = Ink.lines(r, gap, angle)
    val st = Stroke(width)
    clipRect(r.left, r.top, r.right, r.bottom) {
        if (clip != null) clipPath(clip) { drawPath(l, color, alpha = alpha, style = st) } else drawPath(l, color, alpha = alpha, style = st)
    }
}

/** Draws [block] and keeps it only where [mask] is opaque: hatching that fades into a candle's light. */
fun DrawScope.masked(bounds: Rect, mask: Brush, block: DrawScope.() -> Unit) {
    drawIntoCanvas { it.saveLayer(bounds, Paint()) }
    block()
    drawRect(mask, bounds.topLeft, bounds.size, blendMode = BlendMode.DstIn)
    drawIntoCanvas { it.restore() }
}

/** A soft round light. */
fun DrawScope.glow(center: Offset, radius: Float, color: Color, alpha: Float = 1f) {
    if (alpha <= 0f || radius <= 0f) return
    drawCircle(Brush.radialGradient(listOf(color.copy(alpha = alpha * color.alpha), color.copy(alpha = alpha * color.alpha * .35f), Color.Transparent), center, radius), radius, center)
}

/** The moon's disc with its real lit part: [phase] 0 new, 0.5 full. */
fun DrawScope.moon(center: Offset, r: Float, phase: Double, paper: Color = C.paper) {
    val rect = Rect(center, r)
    drawCircle(paper, r, center)
    hatch(rect, 3.4f, 50f, .9f, clip = Path().apply { addOval(rect) })
    hatch(rect, 3.4f, -40f, .9f, alpha = .6f, clip = Path().apply { addOval(rect) })
    val k = cos(2 * Math.PI * phase).toFloat()
    val rx = abs(k) * r
    val wax = phase < .5
    val lit = Path()
    lit.moveTo(center.x, center.y - r)
    lit.arcTo(rect, -90f, if (wax) 180f else -180f, false)
    val term = Rect(center.x - rx, center.y - r, center.x + rx, center.y + r)
    // Back up along the terminator: towards the lit side for a crescent, away from it past half.
    val crescent = k > 0
    val sweep = if (wax == crescent) -180f else 180f
    if (rx > .5f) lit.arcTo(term, 90f, sweep, false) else lit.lineTo(center.x, center.y - r)
    lit.close()
    drawPath(lit, paper)
    drawPath(lit, C.ink, style = Stroke(1.2f))
    drawCircle(C.ink, r, center, style = Stroke(1.2f))
}

/** A tiny seeded random, so a drawing comes out the same every frame. */
class Rng(seed: Int) {
    private var a = seed
    fun next(): Float {
        a += 0x6D2B79F5
        var t = a
        t = (t xor (t ushr 15)) * (1 or t)
        t = (t + ((t xor (t ushr 7)) * (61 or t))) xor t
        return ((t xor (t ushr 14)).toLong() and 0xFFFFFFFFL) / 4294967296f
    }
}

fun f1(v: Float) = String.format(java.util.Locale.ROOT, "%.1f", v)
