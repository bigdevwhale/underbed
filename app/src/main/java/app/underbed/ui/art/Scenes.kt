package app.underbed.ui.art

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import app.underbed.domain.Creature
import app.underbed.ui.theme.C
import kotlin.math.sin

/** The dark room seen from the television: a couch, the person and the pet in its light. 390×300. */
object Couch {
    const val W = 390f
    const val H = 300f

    /** [t] in seconds; [jump] 0..1 while the pet leaps up. */
    fun DrawScope.scene(c: Creature, look: Look, t: Float, jump: Float) {
        drawRect(C.night, Offset.Zero, Size(W, H))
        // The screen's light on the wall flickers.
        val flick = .62f + .18f * sin(t * 9.1f) * sin(t * 3.7f + 1f) + .1f * sin(t * 17f)
        drawRect(C.paper.copy(alpha = .16f * flick), Offset.Zero, Size(W, 230f))
        withTransform({ scale(1f, .6f, Offset(195f, 120f)) }) { glow(Offset(195f, 120f), 175f, C.paper, .55f * flick) }
        pen("M150 20L150 90L240 90L240 20ZM195 20L195 90M150 55L240 55", 1.8f, C.nightLine)
        ink("M18 300L18 170Q18 140 50 140L340 140Q372 140 372 170L372 300Z", Color(0xFF332A23))
        hatch(androidx.compose.ui.geometry.Rect(18f, 140f, 372f, 300f), 5f, 50f, .8f, C.ink, .6f, clip = Ink.p("M18 300L18 170Q18 140 50 140L340 140Q372 140 372 170L372 300Z"))
        ink("M8 300L8 200Q8 186 24 186Q40 186 40 200L40 300ZM350 300L350 200Q350 186 366 186Q382 186 382 200L382 300Z", Color(0xFF0E0B09))
        // The person, from the front, in silhouette.
        withTransform({ translate(115f, 0f) }) {
            drawCircle(Color(0xFF0E0B09), 30f, Offset(0f, 150f))
            ink("M-50 260Q-48 196 0 190Q48 196 50 260Z", Color(0xFF0E0B09))
            pen("M-30 150Q-30 118 0 118Q30 118 30 150", 1f, C.paper, .35f * flick)
        }
        val lift = sin(jump * Math.PI.toFloat()) * 46f
        pet(c, Offset(270f, 210f - lift), .72f, look)
        // The popcorn between them.
        withTransform({ translate(200f, 252f) }) {
            ink("M-18 -16L18 -16L13 14L-13 14Z", C.paper)
            pen("M-18 -16L18 -16L13 14L-13 14Z", 1.5f)
            pen("M-8 -16L-6 14M2 -16L1 14M11 -16L7 14", 3f, C.stripe)
            for ((x, y) in listOf(-12 to -19, -4 to -22, 5 to -20, 13 to -18, 0 to -26, -8 to -25, 9 to -25)) {
                drawCircle(C.paper, 4.5f, Offset(x.toFloat(), y.toFloat()))
                drawCircle(C.ink, 4.5f, Offset(x.toFloat(), y.toFloat()), style = androidx.compose.ui.graphics.drawscope.Stroke(1f))
            }
        }
    }
}

/** Lights out: only the rail of the bed, a night light, and two eyes underneath. 390×300. */
object Dark {
    const val W = 390f
    const val H = 300f

    fun DrawScope.scene(c: Creature, blink: Float, t: Float) {
        drawRect(C.night, Offset.Zero, Size(W, H))
        drawRect(Color(0xFF17130F), Offset(0f, 60f), Size(W, 12f))
        pen("M14 72L14 290M376 72L376 290", 10f, Color(0xFF17130F))
        glow(Offset(60f, 270f), 42f, C.glowHi, .25f + .05f * sin(t * 1.7f))
        drawCircle(C.glow.copy(alpha = .6f), 3f, Offset(60f, 272f))
        val col = Color(c.eye)
        val open = (1f - blink).coerceIn(.06f, 1f)
        for (k in listOf(-1, 1)) {
            val x = 195f + k * 26f
            glow(Offset(x, 180f), 22f, col, .45f * open)
            drawOval(col, Offset(x - 9f, 180f - 10.5f * open), Size(18f, 21f * open))
            drawOval(Color.Black, Offset(x - 2f, 180f - 8f * open), Size(4f, 16f * open))
        }
    }
}
