package app.underbed.ui.yard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import app.underbed.domain.Food
import app.underbed.domain.Genre
import app.underbed.domain.Moon
import app.underbed.ui.Home
import app.underbed.ui.Nav
import app.underbed.ui.Overlay
import app.underbed.ui.art.Yard
import app.underbed.ui.art.Yard.scene
import app.underbed.ui.components.TopBar
import app.underbed.ui.components.rememberSeconds
import app.underbed.ui.theme.C
import app.underbed.ui.theme.T

@Composable
fun YardScreen(h: Home, nav: Nav) {
    val w = h.w
    val t by rememberSeconds()
    val tm = rememberTextMeasurer()
    val graves = h.s.graves
    val placed = remember(graves) { Yard.layout(graves) }
    val films = graves.count { it.food == Food.FILM }
    val books = graves.size - films
    Column(Modifier.verticalScroll(rememberScrollState())) {
        TopBar(
            w.t("Погост", "Graveyard"),
            if (graves.isEmpty()) w.t("пока ни одной могилки", "not a single grave yet")
            else listOfNotNull(
                films.takeIf { it > 0 }?.let { w.n(it, "фильм", "фильма", "фильмов", "film", "films") },
                books.takeIf { it > 0 }?.let { w.n(it, "книга", "книги", "книг", "book", "books") },
            ).joinToString(" · "),
        )
        Box(Modifier.fillMaxWidth().border(1.5.dp, C.ink)) {
            Canvas(
                Modifier.fillMaxWidth().aspectRatio(Yard.W / Yard.H).pointerInput(placed) {
                    detectTapGestures { p ->
                        val u = size.width / Yard.W
                        Yard.hit(placed, Offset(p.x / u, p.y / u))?.let { nav.push(Overlay.Grave(it.id)) }
                    }
                },
            ) {
                val u = size.width / Yard.W
                val more = (graves.size - Yard.CAP).takeIf { it > 0 }?.let { w.t("и ещё $it в фамильном склепе", "and $it more in the family crypt") }
                scale(u, u, Offset.Zero) { scene(placed, Moon.phase(h.now), t, tm, more) }
            }
        }
        Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
            Text(
                if (graves.isEmpty()) w.t("Здесь будет лежать каждый съеденный ужастик и каждая книга ужасов. Покорми Буку — и появится первая могилка.", "Every horror film and book it eats will lie here. Feed it and the first grave appears.")
                else w.t("Нажми на камень — прочитаешь эпитафию или напишешь свою.", "Tap a stone to read its epitaph or write your own."),
                style = T.Small,
            )
            if (graves.isNotEmpty()) {
                Text(w.t("Кто здесь лежит", "Who lies here"), style = T.Label, modifier = Modifier.padding(top = 16.dp))
                Box(Modifier.fillMaxWidth().padding(top = 4.dp).height(1.dp).background(C.ink))
                val counts = h.s.genres()
                for (g in Genre.entries.sortedByDescending { counts[it] ?: 0 }) {
                    val n = counts[g] ?: continue
                    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(w.genre(g), style = T.Body, modifier = Modifier.weight(1f))
                        Text(stoneName(h, g), style = T.Small, modifier = Modifier.padding(end = 12.dp))
                        Text("$n", style = T.BodyB)
                    }
                }
            }
        }
    }
}

private fun stoneName(h: Home, g: Genre) = when (g) {
    Genre.GOTHIC -> h.w.t("крест с вороном", "cross with a raven")
    Genre.SLASHER -> h.w.t("треснувшая плита", "cracked slab")
    Genre.BODY -> h.w.t("плита со швом", "stitched slab")
    Genre.FOLK -> h.w.t("голубец", "roofed wooden post")
    Genre.COSMIC -> h.w.t("обелиск с глазом", "obelisk with an eye")
    Genre.JHORROR -> h.w.t("каменная стопка", "stacked stones")
    Genre.PSYCHO -> h.w.t("надколотый камень", "chipped headstone")
    Genre.OTHER -> h.w.t("простой камень", "plain headstone")
}
