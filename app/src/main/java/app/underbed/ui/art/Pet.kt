package app.underbed.ui.art

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import app.underbed.domain.Creature
import app.underbed.domain.Genre
import app.underbed.ui.theme.C

enum class Mouth { GRIN, OPEN, COVERED, SLEEP }

/** How the pet looks this frame: its mouth, the fed glow, an eyelid, what has grown on it, and its breath. */
class Look(
    val mouth: Mouth = Mouth.GRIN,
    val sat: Float = .4f,
    val blink: Float = 0f,
    val traits: List<Genre> = emptyList(),
    /** -1..1, a slow breath. */
    val breathe: Float = 0f,
    /** Upwards drift of the ghost, in units. */
    val float: Float = 0f,
)

/** Where the eyes and mouth sit for each creature, in units of a body about 150 wide centred on its middle. */
private class Spec(
    val ex: Float, val ey: Float, val er: Float, val my: Float, val ms: Float, val belly: Offset, val top: Float,
    /** Marks on the body (scars, stitches) are drawn in paper on a dark body, in ink on a pale one. */
    val darkBody: Boolean, val darkFace: Boolean, val fangs: Boolean = false,
)

private fun spec(c: Creature) = when (c) {
    Creature.BUKA -> Spec(24f, -14f, 10f, 16f, 1f, Offset(0f, 38f), -66f, true, true)
    Creature.WOLF -> Spec(23f, -22f, 9f, 32f, .8f, Offset(0f, 38f), -62f, true, true)
    Creature.KIKI -> Spec(20f, -14f, 9f, 28f, .85f, Offset(0f, 35f), -66f, true, true)
    Creature.GHOST -> Spec(20f, -34f, 10f, -6f, .8f, Offset(0f, 22f), -82f, false, false)
    Creature.ZOMBIE -> Spec(22f, -22f, 10f, 14f, 1f, Offset(0f, 40f), -72f, false, false)
    Creature.VAMPIRE -> Spec(11f, -30f, 5.5f, -13f, .5f, Offset(0f, 32f), -62f, true, false, fangs = true)
    Creature.DOLL -> Spec(15f, -42f, 8f, -20f, .55f, Offset(0f, 32f), -84f, false, false)
}

private val P = C.paper
private val I = C.ink

/**
 * The pet, drawn around [center] at [scale] units to a pixel (or design unit). Cross-hatched ink like
 * an Edward Gorey drawing; colour only in its eyes and in the belly when it is fed.
 */
fun DrawScope.pet(c: Creature, center: Offset, scale: Float, look: Look = Look()) {
    val s = spec(c)
    withTransform({
        translate(center.x, center.y - look.float * scale)
        scale(scale * (1 + look.breathe * .012f), scale * (1 - look.breathe * .012f), Offset.Zero)
    }) {
        body(c)
        for (g in look.traits) trait(g, s, c)
        glow(s.belly, 40f, C.glow, look.sat.coerceIn(0f, 1f) * .95f)
        eyes(c, s, look)
        withTransform({ translate(0f, s.my); scale(if (look.mouth == Mouth.COVERED) maxOf(.6f, s.ms) else s.ms, if (look.mouth == Mouth.COVERED) maxOf(.6f, s.ms) else s.ms, Offset.Zero) }) {
            mouth(c, s, look.mouth)
        }
    }
}

/** Where the mouth is, in the pet's units below its centre: food flies there. */
fun mouthY(c: Creature) = spec(c).my + 6f

/** Fur: a dark oval with a fringe of short strokes. */
private fun furPath(rx: Float, ry: Float, seed: Int, n: Int = 150): String {
    val r = Rng(seed)
    val sb = StringBuilder()
    for (i in 0 until n) {
        val a = i.toFloat() / n * Math.PI.toFloat() * 2 + r.next() * .05f
        val b = (r.next() - .5f) * .28f
        val r2 = 1.02f + r.next() * .2f
        sb.append("M${f1(kotlin.math.cos(a) * rx * .8f)} ${f1(kotlin.math.sin(a) * ry * .8f)}")
        sb.append("L${f1(kotlin.math.cos(a + b) * rx * r2)} ${f1(kotlin.math.sin(a + b) * ry * r2)}")
    }
    return sb.toString()
}

private fun DrawScope.fur(rx: Float, ry: Float, seed: Int) {
    drawOval(I, Offset(-rx * .93f, -ry * .93f), Size(rx * 1.86f, ry * 1.86f))
    pen(furPath(rx, ry, seed), 2.3f)
}

private fun DrawScope.body(c: Creature) {
    when (c) {
        Creature.BUKA -> fur(72f, 62f, 11)
        Creature.WOLF -> {
            for (s in listOf(-1, 1)) {
                ink("M${s * 58} -28L${s * 46} -100L${s * 16} -52Z")
                pen("M${s * 58} -28L${s * 46} -100L${s * 16} -52Z", 5f)
                pen("M${s * 46} -44L${s * 45} -82L${s * 28} -54", 1f, P, .7f)
            }
            fur(68f, 60f, 22)
            val snout = Path().apply { addOval(Rect(-25f, -4f, 25f, 28f)) }
            hatch(Rect(-25f, -4f, 25f, 28f), 4f, 50f, .9f, P, clip = snout)
            drawPath(snout, P, style = Stroke(1.2f))
            ink("M-8 1L8 1L0 10Z")
            pen("M-8 1L8 1L0 10Z", 1.3f, P)
        }
        Creature.KIKI -> {
            val r = Rng(5)
            val hair = StringBuilder()
            for (i in 0 until 22) {
                val x = -58 + i * 5.5f + r.next() * 3
                val y0 = -58 + kotlin.math.abs(x) * .25f
                hair.append("M${f1(x)} ${f1(y0)}q${f1(r.next() * 14 - 7)} 30 ${f1((if (x < 0) -8 else 8) - r.next() * 4)} ${f1(60 + r.next() * 40)}")
            }
            for (s in listOf(-1, 1)) pen("M${s * 20} -56L${s * 30} -92L${s * 42} -104M${s * 30} -92L${s * 24} -110M${s * 28} -80L${s * 44} -84", 3.2f)
            fur(64f, 64f, 33)
            pen(hair.toString(), 3f)
            for (i in 0 until 7) {
                val x = -34 + i * 11
                val y = -58 + kotlin.math.abs(x) * .2f
                pen("M$x ${f1(y)}q4 -9 8 0", 1f, P, .75f)
            }
            val nose = Ink.p("M-4 -6Q2 12 18 20Q3 16 -2 6Z")
            hatch(Rect(-6f, -8f, 20f, 22f), 4f, 50f, .9f, P, clip = nose)
            drawPath(nose, P, style = Stroke(1.2f))
        }
        Creature.GHOST -> {
            val sheet = "M-58 62C-66 10 -62 -78 0 -82C62 -78 66 10 58 62L44 48L30 64L15 49L0 65L-15 49L-30 64L-44 48Z"
            ink(sheet, P.copy(alpha = .93f))
            hatch(Rect(14f, -90f, 84f, 80f), 5f, 50f, 1f, alpha = .65f, clip = Ink.p(sheet))
            pen("M-30 -40Q-40 10 -36 50M24 -30Q34 10 30 52", 1f, I, .6f)
            pen(sheet, 2.4f)
        }
        Creature.ZOMBIE -> {
            val zb = "M-62 60C-70 20 -58 -44 -30 -58C-20 -72 10 -74 28 -60C56 -46 66 0 62 60Z"
            val arm = "M52 2L84 -14L92 -12L87 -7L94 -4L87 0L92 5L83 5L56 20Z"
            ink(arm, P); pen(arm, 2f)
            ink(zb, P)
            hatch(Rect(-80f, -80f, 80f, 70f), 5f, 50f, 1f, alpha = .55f, clip = Ink.p(zb))
            hatch(Rect(10f, -80f, 80f, 70f), 4.5f, -40f, 1f, alpha = .6f, clip = Ink.p(zb))
            pen(zb, 2.4f)
            pen("M-28 -52Q0 -36 28 -50", 1.6f)
            val st = StringBuilder()
            var x = -26
            while (x <= 26) { val y = -50 + x * x / 140f; st.append("M${x - 2} ${f1(y - 4)}L${x + 2} ${f1(y + 4)}"); x += 7 }
            pen(st.toString(), 1.4f)
            pen("M-40 30L-20 26L-12 40", 1.3f)
        }
        Creature.VAMPIRE -> {
            ink("M-76 64L-58 -8C-62 -50 -70 -78 -52 -98L-34 -52C-12 -44 12 -44 34 -52L52 -98C70 -78 62 -50 58 -8L76 64Z")
            hatch(Rect(-32f, -8f, 32f, 64f), 4f, 50f, .9f, P, .8f, clip = Ink.p("M-30 64L-18 -6L18 -6L30 64Z"))
            pen("M-52 -98L-34 -52M52 -98L34 -52", 1f, P, .5f)
            for (s in listOf(-1, 1)) { ink("M${s * 26} -36L${s * 40} -50L${s * 28} -22Z", P); pen("M${s * 26} -36L${s * 40} -50L${s * 28} -22Z", 1.6f) }
            val head = Path().apply { addOval(Rect(Offset(0f, -30f), 28f)) }
            drawPath(head, P)
            hatch(Rect(10f, -60f, 40f, 0f), 5f, 50f, 1f, alpha = .7f, clip = head)
            drawPath(head, I, style = Stroke(2f))
            ink("M-29 -36C-31 -62 -10 -66 0 -54C10 -66 31 -62 29 -36C23 -50 10 -52 0 -40C-10 -52 -23 -50 -29 -36Z")
        }
        Creature.DOLL -> {
            val dress = "M-34 -8L-58 62L58 62L34 -8Z"
            ink(dress, P)
            hatch(Rect(-60f, -10f, 60f, 64f), 5f, 50f, 1f, alpha = .5f, clip = Ink.p(dress))
            pen(dress, 2.2f)
            val sc = StringBuilder(); var x = -30
            while (x < 30) { sc.append("M$x -8q5 8 10 0"); x += 10 }
            ink(sc.toString(), P); pen(sc.toString(), 1.5f)
            for (s in listOf(-1, 1)) { drawCircle(P, 7f, Offset(s * 48f, 32f)); drawCircle(I, 7f, Offset(s * 48f, 32f), style = Stroke(1.8f)) }
            drawCircle(P, 40f, Offset(0f, -42f))
            drawCircle(I, 40f, Offset(0f, -42f), style = Stroke(2.2f))
            for (s in listOf(-1, 1)) hatch(Rect(Offset(s * 24f, -28f), 7f), 4.5f, -40f, 1f, alpha = .7f, clip = Path().apply { addOval(Rect(Offset(s * 24f, -28f), 7f)) })
            pen("M8 -81L3 -70L12 -62L5 -51", 1.4f)
            ink("M-43 -38C-47 -82 -20 -92 0 -90C20 -92 47 -82 43 -38L43 -14L35 -14L35 -50C35 -56 30 -58 24 -57L-24 -57C-30 -58 -35 -56 -35 -50L-35 -14L-43 -14Z")
        }
    }
}

/** What has grown on the pet from the horror it was fed most. */
private fun DrawScope.trait(g: Genre, s: Spec, c: Creature) {
    val mark = if (s.darkBody) P else I
    val t = s.top
    when (g) {
        Genre.GOTHIC -> {
            // A top hat, a little askew.
            withTransform({ rotate(-8f, Offset(6f, t)) }) {
                drawRect(I, Offset(-12f, t - 34f), Size(36f, 34f))
                drawRect(C.wax, Offset(-12f, t - 10f), Size(36f, 6f))
                drawOval(I, Offset(-24f, t - 4f), Size(60f, 9f))
                pen("M-12 ${t - 34}h36M-24 ${t + .5f}h60", 1.2f, P, .7f)
            }
        }
        Genre.SLASHER -> pen("M18 6L40 30M26 2L46 24M14 16L32 38", 2.2f, mark, .9f)
        Genre.FOLK -> if (c != Creature.KIKI) for (k in listOf(-1, 1)) pen("M${k * 22} ${t + 8}L${k * 32} ${t - 26}L${k * 44} ${t - 36}M${k * 32} ${t - 26}L${k * 26} ${t - 44}M${k * 30} ${t - 14}L${k * 46} ${t - 18}", 3.2f)
            else for (i in 0 until 9) pen("M${-40 + i * 10} ${t + 14}q3 -8 6 0", 1.4f, P, .9f)
        Genre.COSMIC -> {
            for ((x, k) in listOf(-44f to -1, -16f to -1, 16f to 1, 44f to 1)) {
                pen("M$x 52Q${x + k * 6} 74 ${x + k * 22} 78Q${x + k * 34} 78 ${x + k * 30} 68", 7f)
                drawCircle(P, 1.4f, Offset(x + k * 4f, 64f))
                drawCircle(P, 1.4f, Offset(x + k * 14f, 75f))
            }
        }
        Genre.JHORROR -> {
            val r = Rng(41)
            val sb = StringBuilder()
            for (i in 0 until 26) {
                val x = -50 + i * 4f
                if (kotlin.math.abs(x) < s.ex + s.er + 2) continue
                sb.append("M${f1(x)} ${f1(t + 4)}q${f1((r.next() - .5f) * 10)} 40 ${f1(x * .12f)} ${f1(110 + r.next() * 30)}")
            }
            pen(sb.toString(), 2.4f)
        }
        Genre.PSYCHO -> pen("M-6 ${t + 10}L2 ${t + 26}L-8 ${t + 40}L4 ${t + 58}L-4 ${t + 74}", 1.8f, mark)
        Genre.BODY -> {
            pen("M-36 22Q0 34 36 22", 1.6f, mark)
            for (x in listOf(-28, -16, -4, 8, 20, 30)) pen("M$x ${22 + 4}l3 -8", 1.4f, mark)
        }
        Genre.OTHER -> Unit
    }
}

private fun DrawScope.eyes(c: Creature, s: Spec, look: Look) {
    val col = Color(c.eye)
    val edge = if (s.darkFace) P else I
    if (look.mouth == Mouth.SLEEP) {
        for (k in listOf(-1, 1)) { val x = k * s.ex; pen("M${x - s.er} ${s.ey}Q$x ${s.ey + s.er * .7f} ${x + s.er} ${s.ey}", 2f, edge) }
        return
    }
    val open = (1f - look.blink).coerceIn(.08f, 1f)
    for (k in listOf(-1, 1)) {
        val x = k * s.ex; val y = s.ey
        when (c) {
            Creature.GHOST -> {
                drawOval(I, Offset(x - 9, y - 13 * open), Size(18f, 26f * open))
                glow(Offset(x, y + 2), 10f, col, .6f * open)
                drawCircle(col, 2.4f * open, Offset(x, y + 2))
            }
            Creature.DOLL -> {
                glow(Offset(x, y), s.er * 2.4f, col, .45f)
                drawCircle(I, s.er, Offset(x, y))
                drawCircle(Color(0xFF5A4C40), s.er * .7f, Offset(x, y), style = Stroke(1f))
                for ((a, b) in listOf(-2 to -2, 2 to -2, -2 to 2, 2 to 2)) drawCircle(col, 1f, Offset(x + a, y + b))
            }
            else -> {
                val r = s.er * (if (c == Creature.ZOMBIE && k > 0) .62f else 1f)
                glow(Offset(x, y), r * 2.4f, col, .55f)
                drawOval(col, Offset(x - r, y - r * 1.15f * open), Size(r * 2, r * 2.3f * open))
                drawOval(I, Offset(x - r * .22f, y - r * .8f * open), Size(r * .44f, r * 1.6f * open))
            }
        }
    }
}

private fun DrawScope.mouth(c: Creature, s: Spec, m: Mouth) {
    val edge = if (s.darkFace) P else I
    val cavity = Color(0xFF0A0807)
    when {
        m == Mouth.OPEN -> {
            drawOval(cavity, Offset(-19f, -7f), Size(38f, 30f))
            drawOval(edge, Offset(-19f, -7f), Size(38f, 30f), style = Stroke(1.6f))
            for (x in listOf(-9, -3, 3, 9)) ink("M${x - 3} -5L${x + 3} -5L$x 2Z", P)
            for (x in listOf(-6, 0, 6)) ink("M${x - 3} 21L${x + 3} 21L$x 15Z", P)
        }
        m == Mouth.COVERED -> for (k in listOf(-1, 1)) {
            drawOval(if (s.darkFace) I else P, Offset(k * 13f - 15, -5f), Size(30f, 22f))
            drawOval(edge, Offset(k * 13f - 15, -5f), Size(30f, 22f), style = Stroke(1.6f))
            for (x in listOf(-6, 0, 6)) pen("M${k * 13 + x} -3l0 -5", 1.6f, edge)
        }
        m == Mouth.SLEEP -> pen("M-8 4Q0 8 8 4", 2f, edge)
        c == Creature.DOLL -> {
            pen("M-18 0Q0 6 18 0", 1.6f)
            for (x in listOf(-12, -6, 0, 6, 12)) pen("M$x -3L$x 5", 1.3f)
        }
        else -> {
            ink("M-24 0Q0 20 24 0Q0 9 -24 0Z", cavity)
            pen("M-24 0Q0 20 24 0Q0 9 -24 0Z", 1.5f, edge)
            ink("M-19 2L-16 7L-13 3.6L-10 9L-6 5L-3 10L0 5.4L3 10L6 5L10 9L13 3.6L16 7L19 2Z", P)
            if (s.fangs) { ink("M-9 4L-5 4L-7 15Z M5 4L9 4L7 15Z", P); pen("M-9 4L-5 4L-7 15Z M5 4L9 4L7 15Z", .8f) }
        }
    }
}

/** The pet seen small, from behind the popcorn or in a tile: the same drawing at another size. */
fun DrawScope.petTile(c: Creature, look: Look = Look(sat = .3f)) {
    drawRect(Color(0xFF231D18))
    translate(0f, 0f) { pet(c, Offset(size.width / 2, size.height * .58f), size.height / 200f, look) }
}
