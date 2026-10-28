package app.underbed

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import app.underbed.domain.Creature
import app.underbed.domain.Genre
import app.underbed.ui.art.Bed
import app.underbed.ui.art.Bed.scene
import app.underbed.ui.art.Look
import app.underbed.ui.art.Mouth
import app.underbed.ui.art.pet
import app.underbed.ui.theme.C
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Draws the pets and the scene under the bed to PNGs (run with `-Prender`): a look at the art without a phone. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class RenderArt {
    private val out = File(System.getProperty("render.out") ?: "build/art").apply { mkdirs() }

    private fun render(name: String, w: Int, h: Int, draw: DrawScope.() -> Unit) {
        val image = ImageBitmap(w, h)
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(image), Size(w.toFloat(), h.toFloat()), draw)
        File(out, "$name.png").outputStream().use { image.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun pets() {
        for (m in Mouth.entries) render("pets-${m.name.lowercase()}", 1400, 260) {
            drawRect(C.paper)
            for ((i, c) in Creature.entries.withIndex()) pet(c, Offset(100f + i * 200f, 140f), 1f, Look(mouth = m, sat = .7f))
        }
        render("pets-traits", 1600, 280) {
            drawRect(C.paper)
            for ((i, g) in Genre.entries.withIndex()) pet(Creature.entries[i % 7], Offset(100f + i * 200f, 160f), 1f, Look(traits = listOf(g), sat = .5f))
        }
    }

    /** Each creature alone on a clear ground, large, for posters. */
    @Test fun poster() {
        for (c in Creature.entries) render("poster-${c.name.lowercase()}", 440, 520) {
            val traits = if (c == Creature.BUKA) listOf(Genre.GOTHIC) else emptyList()
            pet(c, Offset(220f, 290f), 2f, Look(sat = .7f, traits = traits))
        }
    }

    @Test fun bed() {
        render("bed", 1080, 1163) {
            scale(1080f / Bed.W, 1080f / Bed.W, Offset.Zero) {
                scene(2f)
                pet(Creature.BUKA, Bed.PET, 1f, Look(sat = .6f, traits = listOf(Genre.GOTHIC, Genre.FOLK)))
            }
        }
    }
}

/** The launcher icon drawn from its own vectors into docs/logo.png (run with `-Prender`). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class RenderArtIcon {
    @Test fun logo() {
        val c = ApplicationProvider.getApplicationContext<android.content.Context>()
        val bmp = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bmp)
        for (id in listOf(R.drawable.ic_launcher_background, R.drawable.ic_launcher_foreground)) {
            ContextCompat.getDrawable(c, id)!!.apply { setBounds(0, 0, 512, 512); draw(canvas) }
        }
        File("../docs/logo.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
