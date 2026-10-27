package app.underbed.ui.art

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import app.underbed.domain.Food
import app.underbed.domain.Genre
import app.underbed.domain.Meal
import app.underbed.ui.theme.C
import app.underbed.ui.theme.Hand
import kotlin.math.cos
import kotlin.math.sin

/**
 * The graveyard on the hill: every film and book the pet ate lies here under a stone that matches
 * its kind of horror. The newest are in front. Drawn in a 390×600 design box.
 */
object Yard {
    const val W = 390f
    const val H = 600f
    const val CAP = 17

    class Placed(val m: Meal, val x: Float, val y: Float, val s: Float, val i: Int)

    private class Row(val y: Float, val s: Float, val n: Int)
    private val ROWS = listOf(Row(287f, .45f, 5), Row(347f, .58f, 5), Row(424f, .72f, 4), Row(512f, .88f, 3))

    /** Where each of the newest graves stands, in drawing order: back rows first. */
    fun layout(graves: List<Meal>): List<Placed> {
        val list = graves.sortedByDescending { it.at }.take(CAP)
        val r = Rng(17)
        class Slot(val row: Row, val j: Int, val dx: Float, val dy: Float)
        val slots = ROWS.reversed().flatMap { row -> (0 until row.n).map { j -> Slot(row, j, (r.next() - .5f) * 14 * row.s, (r.next() - .5f) * 6) } }
        return list.mapIndexed { i, m ->
            val sl = slots[i]
            Placed(m, 48 + (W - 80) * (sl.j + .5f) / sl.row.n + sl.dx + (if (sl.row.n % 2 == 0) 10 else 0), sl.row.y + sl.dy, sl.row.s, i)
        }.reversed()
    }

    /** The grave under a tap, front ones first. */
    fun hit(placed: List<Placed>, p: Offset): Meal? = placed.reversed().firstOrNull { g ->
        p.x in g.x - 30 * g.s..g.x + 30 * g.s && p.y in g.y - 98 * g.s..g.y + 8 * g.s
    }?.m

    fun DrawScope.scene(placed: List<Placed>, phase: Double, t: Float, tm: TextMeasurer, more: String?) {
        drawRect(C.paper, Offset.Zero, Size(W, H))
        val sky = Rect(0f, 0f, W, 260f)
        masked(sky, Brush.verticalGradient(0f to Color.Black, 1f to Color.Transparent, endY = 260f)) { hatch(sky, 3f, 0f, 1f, alpha = .8f) }
        drawCircle(C.paper, 40f, Offset(300f, 70f))
        moon(Offset(300f, 70f), 30f, phase)
        // Bats.
        for ((i, bat) in listOf("M80 70q6 -8 12 -2q4 -4 6 0q2 -4 6 0q6 -6 12 2q-8 -2 -12 4q-4 -3 -6 0q-2 -3 -6 0q-4 -6 -12 -4z", "M150 40q4 -6 9 -1q3 -3 4 0q2 -3 4 0q5 -5 9 1q-6 -1 -9 3q-3 -2 -4 0q-2 -2 -4 0q-3 -4 -9 -3z").withIndex()) {
            val k = t * .7f + i * 2.4f
            withTransform({ translate(-30f * (1 - cos(k)) / 2, 12f * sin(k) / 2) }) { ink(bat) }
        }
        // A chapel on the far hill with one lit window.
        withTransform({ translate(332f, 212f) }) {
            ink("M-16 -26h32v26h-32zM-20 -26L0 -40L20 -26ZM-5 -58h10v20h-10zM-8 -58Q-9 -70 0 -76Q9 -70 8 -58Z")
            pen("M0 -76L0 -88M-4 -84L4 -84", 1.6f)
            drawRect(C.glow, Offset(-3f, -16f), Size(6f, 16f))
        }
        ink("M0 248Q110 196 230 236T390 214L390 600L0 600Z", C.paper)
        pen("M0 248Q110 196 230 236T390 214", 1.8f)
        hatch(Rect(0f, 520f, W, H), 5f, 50f, alpha = .35f)
        for ((d, w) in tree) pen(d, w)
        ink("M38 262Q44 270 54 262Z")
        for (g in placed) withTransform({ translate(g.x, g.y); scale(g.s, g.s, Offset.Zero) }) { grave(g.m, g.i, tm) }
        if (more != null) {
            val l = tm.measure(more, TextStyle(fontFamily = Hand, fontSize = (12f / density).sp, color = C.ink))
            drawText(l, topLeft = Offset(W / 2 - l.size.width / 2f, 532f + 4f))
        }
        // Fog drifting between the rows.
        for ((i, f) in listOf(Triple(90f, 360f, 140f), Triple(280f, 440f, 150f)).withIndex()) {
            val dx = 30f * (1 - cos(t * (if (i == 0) .28f else .2f))) * (if (i == 0) 1 else -1)
            val c = Offset(f.first + dx, f.second)
            withTransform({ scale(1f, .14f, c) }) { glow(c, f.third, C.paper, .9f) }
        }
        pen(fence, 2.4f)
        ink(spikes)
    }

    private val tree: List<Pair<String, Float>> by lazy {
        val r = Rng(4)
        val out = ArrayList<Pair<String, Float>>()
        fun br(x: Float, y: Float, a: Float, l: Float, w: Float, d: Int) {
            val x2 = x + cos(a) * l; val y2 = y + sin(a) * l
            out += "M${f1(x)} ${f1(y)}L${f1(x2)} ${f1(y2)}" to w
            if (d > 0) { br(x2, y2, a - .45f - r.next() * .3f, l * .72f, w * .65f, d - 1); br(x2, y2, a + .35f + r.next() * .3f, l * .68f, w * .62f, d - 1) }
        }
        br(44f, 262f, (-Math.PI / 2 - .08).toFloat(), 56f, 9f, 5)
        out
    }

    private val fence: String by lazy {
        val sb = StringBuilder("M0 566L390 566M0 586L390 586")
        var x = 6
        while (x < 390) { sb.append("M$x 600L$x 554"); x += 15 }
        sb.toString()
    }
    private val spikes: String by lazy {
        val sb = StringBuilder()
        var x = 6
        while (x < 390) { sb.append("M${x - 4} 556L$x 546L${x + 4} 556Z"); x += 15 }
        sb.toString()
    }

    /** The stone a meal gets: a book always lies under an open book. */
    private enum class Stone { CROSS, SLAB, GOLUBETS, OBELISK, STACK, STONE, BOOK }

    private fun stone(m: Meal) = if (m.food == Food.BOOK) Stone.BOOK else when (m.genre) {
        Genre.GOTHIC -> Stone.CROSS
        Genre.SLASHER, Genre.BODY -> Stone.SLAB
        Genre.FOLK -> Stone.GOLUBETS
        Genre.COSMIC -> Stone.OBELISK
        Genre.JHORROR -> Stone.STACK
        Genre.PSYCHO, Genre.OTHER -> Stone.STONE
    }

    private fun short(s: String) = if (s.length > 9) s.take(8).trimEnd() + "…" else s

    private fun DrawScope.word(tm: TextMeasurer, text: String, at: Offset, size: Float, rotate: Float = 0f) {
        val l = tm.measure(short(text), TextStyle(fontFamily = Hand, fontSize = (size / density).sp, color = C.ink), maxLines = 1, overflow = TextOverflow.Clip)
        withTransform({ rotate(rotate, at) }) {
            drawText(l, topLeft = Offset(at.x - l.size.width / 2f, at.y - l.size.height / 2f))
        }
    }

    private fun DrawScope.stoneFill(d: String) { ink(d, C.paper); pen(d, 2f) }

    private fun DrawScope.shade(x1: Int, x2: Int, y1: Int, y2: Int) {
        val sb = StringBuilder()
        var x = x1
        while (x <= x2) { sb.append("M$x ${y1}L$x $y2"); x += 3 }
        pen(sb.toString(), .8f)
    }

    private fun DrawScope.grave(m: Meal, i: Int, tm: TextMeasurer) {
        drawOval(C.ink.copy(alpha = .3f), Offset(-30f, -4f), Size(60f, 10f))
        when (stone(m)) {
            Stone.CROSS -> {
                stoneFill("M-20 -14h40v14h-40z"); stoneFill("M-6 -82h12v70h-12z"); stoneFill("M-25 -66h50v11h-50z")
                shade(1, 4, -80, -14)
                word(tm, m.text, Offset(0f, -7f), 9f)
                // A raven on top.
                withTransform({ translate(-2f, -87f) }) {
                    drawOval(C.ink, Offset(-7f, -5f), Size(14f, 10f)); drawCircle(C.ink, 3.6f, Offset(6f, -5f))
                    ink("M9 -6L15 -4L9 -3Z M-6 0L-14 5L-6 3Z")
                }
            }
            Stone.SLAB -> withTransform({ rotate(-5f, Offset.Zero) }) {
                stoneFill("M-21 -64h42v64h-42z")
                shade(12, 18, -62, -2)
                pen("M5 -64L1 -52L8 -44L2 -34", 1.3f)
                if (m.genre == Genre.BODY) { pen("M-18 -22L18 -28", 1.2f); pen("M-12 -27l3 8M-4 -28l3 8M4 -30l3 8M12 -31l3 8", 1f) }
                word(tm, m.text, Offset(0f, -40f), 10f)
            }
            Stone.GOLUBETS -> {
                hatch(Rect(-5f, -74f, 5f, 0f), 5f, 50f)
                drawRect(C.ink, Offset(-5f, -74f), Size(10f, 74f), style = Stroke(1.8f))
                stoneFill("M-22 -63h44v19h-44z")
                ink("M-28 -71L0 -90L28 -71L23 -67L0 -82L-23 -67Z")
                word(tm, m.text, Offset(0f, -53.5f), 9f)
            }
            Stone.OBELISK -> {
                stoneFill("M-20 -10h40v10h-40z"); stoneFill("M-13 -10L-9 -82L0 -96L9 -82L13 -10Z")
                shade(3, 9, -84, -12)
                drawOval(C.ink, Offset(-5f, -77f), Size(10f, 6f), style = Stroke(1.2f)); drawCircle(C.ink, 1.5f, Offset(0f, -74f))
                word(tm, m.text, Offset(-1f, -42f), 8f, -90f)
            }
            Stone.STACK -> {
                stoneFill("M-25 -12h50v12h-50z"); stoneFill("M-19 -24h38v12h-38z"); stoneFill("M-11 -86h22v62h-22z")
                shade(4, 9, -84, -26)
                word(tm, m.text, Offset(0f, -55f), 8f, 90f)
            }
            Stone.STONE -> {
                stoneFill("M-22 0L-22 -48Q-22 -70 0 -70Q22 -70 22 -48L22 0Z")
                shade(13, 19, -54, -2)
                if (m.genre == Genre.PSYCHO) pen("M-9 -69L-4 -58L-11 -50L-6 -42", 1.2f)
                word(tm, m.text, Offset(0f, -36f), 10f)
            }
            Stone.BOOK -> {
                stoneFill("M-26 -12h52v12h-52z")
                stoneFill("M0 -14C-8 -20 -20 -20 -28 -16L-28 -62C-20 -66 -8 -66 0 -60C8 -66 20 -66 28 -62L28 -16C20 -20 8 -20 0 -14Z")
                pen("M0 -60L0 -14", 1.6f)
                shade(16, 25, -60, -18)
                pen("M-22 -50h16M-22 -44h16M-22 -38h12", .7f)
                ink("M4 -58L4 -40L7 -44L10 -40L10 -58Z", C.wax)
                word(tm, m.text, Offset(-13f, -26f), 7f)
            }
        }
        pen("M-26 0l-3 -7M-23 0l1 -8M-20 0l4 -6M22 0l-2 -7M25 0l2 -8M28 0l5 -5", 1.1f)
        // A candle left at every third grave.
        if (i % 3 == 1) withTransform({ translate(28f, -2f) }) {
            glow(Offset(0f, -14f), 11f, C.glowHi, .9f)
            drawRect(C.paper, Offset(-2.5f, -10f), Size(5f, 10f)); drawRect(C.ink, Offset(-2.5f, -10f), Size(5f, 10f), style = Stroke(1f))
            ink("M0 -10C-2.5 -13 -1.5 -16 0 -19C1.5 -16 2.5 -13 0 -10Z", C.glow)
        }
    }
}
