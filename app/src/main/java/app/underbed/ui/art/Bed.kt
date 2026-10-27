package app.underbed.ui.art

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import app.underbed.ui.theme.C
import kotlin.math.cos
import kotlin.math.sin

/**
 * Under the bed, seen from the floor: the fringe of the bedspread, the rail and the turned legs,
 * floorboards running into the dark, a candle, a lost sock, a book and a spider. Drawn in a 390×420
 * design box; the pet sits at [PET] on top of it.
 */
object Bed {
    const val W = 390f
    const val H = 420f
    val PET = Offset(210f, 298f)
    private val FULL = Rect(0f, 0f, W, H)

    private val static: String by lazy { statics() }
    private val blanket: String by lazy { blanketPath() }

    /** [t] in seconds, for the candle and the spider. */
    fun DrawScope.scene(t: Float) {
        drawRect(C.paper, Offset.Zero, Size(W, H))
        val vy = 140f; val vx = 195f; val by = 235f; val k = (by - vy) / (H - vy)
        var x = -330f
        while (x <= 720f) { pen("M$x $H L${f1(vx + (x - vx) * k)} $by", 1f, C.ink2); x += 56f }
        pen(static, .8f, C.ink2)
        drawRect(C.ink, Offset(0f, 222f), Size(W, 13f), style = Stroke(1.2f))
        // The dark: single hatching near the candle, crossed further back, dense right under the rail.
        masked(FULL, Brush.radialGradient(0f to Color.Transparent, .28f to Color.Transparent, .85f to Color.Black, center = Offset(180f, 350f), radius = 250f)) {
            hatch(FULL, 5f, 50f)
        }
        masked(FULL, Brush.radialGradient(0f to Color.Transparent, .5f to Color.Transparent, 1f to Color.Black, center = Offset(180f, 350f), radius = 290f)) {
            hatch(FULL, 4.5f, -40f)
        }
        val under = Rect(0f, 60f, W, 230f)
        masked(under, Brush.verticalGradient(.3f to Color.Black, 1f to Color.Transparent, startY = 60f, endY = 230f)) {
            hatch(under, 3f, 0f, 1.1f)
        }
        dust()
        // A sock.
        ink("M40 372L70 366L72 386Q74 398 62 398L34 400Q24 398 28 390L44 386Z", C.paper)
        pen("M40 372L70 366L72 386Q74 398 62 398L34 400Q24 398 28 390L44 386Z", 1.6f)
        pen("M42 378L71 372M43 384L72 379", 3f)
        // A book lying shut.
        ink("M276 388L340 380L360 396L296 406Z", C.paper)
        pen("M276 388L340 380L360 396L296 406Z", 1.6f)
        ink("M296 406L296 412L360 402L360 396Z")
        pen("M300 397L330 393M304 401L322 399", 1f, C.ink2)
        candle(t)
        // The rail, and the bedspread hanging over it with its fringe.
        drawRect(C.ink, Offset(0f, 50f), Size(W, 22f))
        pen("M0 58Q100 55 200 60T390 57M0 65Q120 62 240 66T390 64", .7f, C.paper, .4f)
        ink(blanket, C.paper)
        pen(blanket, 1.6f)
        var fx = 20f
        while (fx < W) {
            pen("M$fx 0Q${fx + 8} 20 ${fx + 2} 41", 1f)
            hatch(Rect(fx + 4, 0f, fx + 22, 44f), 5f, 50f, alpha = .7f, clip = Ink.p("M${fx + 6} 0Q${fx + 14} 20 ${fx + 10} 42L${fx + 20} 42Q${fx + 22} 20 ${fx + 14} 0Z"))
            fx += 52f
        }
        pen(fringe, 1.2f)
        leg(22f); leg(368f)
        pen(web, .6f)
        // The spider goes up and down its thread.
        val dy = (1 - cos(t * 1.25f)) * 7f
        drawLine(C.ink, Offset(330f, 72f), Offset(330f, 128f + dy), .6f)
        withTransform({ translate(0f, dy) }) {
            drawOval(C.ink, Offset(326f, 127f), Size(8f, 10f))
            pen("M326 130l-6 -4M326 133l-7 0M326 136l-6 4M334 130l6 -4M334 133l7 0M334 136l6 4", .8f)
        }
        // The pet's shadow on the boards.
        drawOval(C.ink.copy(alpha = .55f), Offset(PET.x - 74f, 353f), Size(148f, 18f))
    }

    private fun DrawScope.candle(t: Float) {
        val pulse = .85f + .15f * sin(t * 2.6f)
        glow(Offset(118f, 340f), 125f, C.glowHi, .75f * pulse)
        drawOval(C.paper, Offset(94f, 388f), Size(48f, 12f))
        drawOval(C.ink, Offset(94f, 388f), Size(48f, 12f), style = Stroke(1.5f))
        ink("M109 392L109 356Q113 352 118 357Q122 352 127 356L127 392Z", C.paper)
        pen("M109 392L109 356Q113 352 118 357Q122 352 127 356L127 392Z", 1.5f)
        pen("M110 362q2 10 0 14M124 360q-2 6 0 9", 1f)
        pen("M118 355L118 349", 1.4f)
        val sx = 1f + .06f * sin(t * 7.3f); val sy = 1f + .08f * sin(t * 5.1f + 1f)
        withTransform({ scale(sx, sy, Offset(118f, 350f)) }) {
            ink("M118 350C112 344 114 336 118 328C122 336 124 344 118 350Z", C.glow)
            pen("M118 350C112 344 114 336 118 328C122 336 124 344 118 350Z", .8f)
        }
    }

    private fun DrawScope.dust() {
        val r = Rng(19)
        for ((x, y, rad) in listOf(Triple(262f, 338f, 9f), Triple(150f, 300f, 6f), Triple(338f, 306f, 7f), Triple(60f, 330f, 5f))) {
            val sb = StringBuilder()
            for (i in 0 until 14) {
                val a = i * 2.3f
                sb.append(if (i == 0) "M" else "L").append(f1(x + cos(a) * rad * (.6f + r.next() * .6f))).append(' ').append(f1(y + sin(a) * rad * .6f * (.6f + r.next() * .6f)))
            }
            pen(sb.toString(), .8f)
        }
    }

    private fun DrawScope.leg(x: Float) {
        val w = listOf(72f to 15f, 86f to 10f, 100f to 18f, 114f to 10f, 300f to 8f, 352f to 13f, 384f to 9f, 404f to 15f, 410f to 15f)
        val l = w.mapIndexed { i, (y, h) -> (if (i == 0) "M" else "L") + "${x - h / 2} $y" }.joinToString("")
        val r = w.reversed().joinToString("") { (y, h) -> "L${x + h / 2} $y" }
        ink("$l${r}Z")
    }

    private fun statics(): String {
        val r = Rng(9)
        val sb = StringBuilder()
        for (y in listOf(262, 300, 352, 400)) repeat(4) {
            val x = r.next() * 360
            sb.append("M${f1(x)} ${y}L${f1(x + 20 + r.next() * 30)} $y")
        }
        return sb.toString()
    }

    private fun blanketPath(): String {
        val r = Rng(3)
        val sb = StringBuilder("M0 0L390 0L390 40")
        var x = 390f
        while (x >= 0f) { sb.append("Q${x - 13} ${f1(46 + r.next() * 4)} ${x - 26} 40"); x -= 26f }
        return sb.append("Z").toString()
    }

    private val fringe: String by lazy {
        val r = Rng(4)
        val sb = StringBuilder()
        var x = 4f
        while (x < W) { sb.append("M${f1(x)} 42L${f1(x + (r.next() - .5f) * 3)} ${f1(54 + r.next() * 5)}"); x += 6.5f }
        sb.toString()
    }

    private val web: String by lazy {
        val cx = 29f; val cy = 72f
        val sb = StringBuilder()
        for (a in 0..90 step 18) { val rad = Math.toRadians(a.toDouble()); sb.append("M$cx ${cy}L${f1(cx + cos(rad).toFloat() * 62)} ${f1(cy + sin(rad).toFloat() * 62)}") }
        for (rr in listOf(14f, 27f, 40f, 53f)) for (a in 0 until 90 step 18) {
            val r1 = Math.toRadians(a.toDouble()); val r2 = Math.toRadians(a + 18.0); val rm = Math.toRadians(a + 9.0)
            sb.append("M${f1(cx + cos(r1).toFloat() * rr)} ${f1(cy + sin(r1).toFloat() * rr)}")
            sb.append("Q${f1(cx + cos(rm).toFloat() * rr * .86f)} ${f1(cy + sin(rm).toFloat() * rr * .86f)} ${f1(cx + cos(r2).toFloat() * rr)} ${f1(cy + sin(r2).toFloat() * rr)}")
        }
        sb.toString()
    }
}
