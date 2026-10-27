package app.underbed.ui.yard

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.underbed.container
import app.underbed.domain.Food
import app.underbed.domain.Genre
import app.underbed.ui.Home
import app.underbed.ui.Nav
import app.underbed.ui.components.Btn
import app.underbed.ui.components.Chip
import app.underbed.ui.components.Field
import app.underbed.ui.components.Flow
import app.underbed.ui.components.Kind
import app.underbed.ui.components.Label
import app.underbed.ui.components.Sheet
import app.underbed.ui.components.dots
import app.underbed.ui.theme.C
import app.underbed.ui.theme.T
import java.time.Instant

@Composable
fun GraveSheet(h: Home, id: Long, nav: Nav) {
    val w = h.w
    val c = LocalContext.current.container
    val m = h.s.meal(id) ?: run { nav.pop(); return }
    var epitaph by remember(id) { mutableStateOf(m.epitaph) }
    var digging by remember { mutableStateOf(false) }
    // The epitaph is kept when the sheet closes, however it closes.
    DisposableEffect(id) { onDispose { if (epitaph != m.epitaph) c.write { repo.epitaph(id, epitaph) } } }

    Sheet(w.t("Здесь покоится", "Here lies"), { nav.pop() }, w.t("Закрыть", "Close")) {
        Text(m.text, style = T.Title.copy(textAlign = TextAlign.Center), modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
        val meta = listOfNotNull(
            if (m.food == Food.BOOK) w.t("книга", "book") else w.t("фильм", "film"),
            m.maker.ifBlank { null }, m.year?.toString(),
        ).joinToString(" · ")
        Text(meta, style = T.Small.copy(textAlign = TextAlign.Center), modifier = Modifier.fillMaxWidth())
        val day = Instant.ofEpochMilli(m.at).atZone(h.zone).toLocalDate()
        Text(
            w.t("мурашки ", "goosebumps ") + dots(m.level) + " · " + w.level(m.level) +
                (m.jumps?.let { w.t(" · подпрыгнул{|а} ", " · jumped ") + w.times(it) } ?: "") +
                "\n" + w.t("съеден ", "eaten on ") + w.dateY(day) +
                (m.spotList().takeIf { it.isNotEmpty() }?.let { s -> "\n" + w.t("почувствовал{|а}: ", "felt in: ") + s.joinToString(", ") { w.spot(it) } } ?: ""),
            style = T.Small.copy(textAlign = TextAlign.Center), modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        )
        Label(w.t("Эпитафия", "Epitaph"))
        Field(epitaph, { epitaph = it }, w.t("Напиши пару слов на камне…", "A few words for the stone…"), style = T.Voice.copy(fontSize = 20.sp, textAlign = TextAlign.Center), minLines = 2, max = 140)
        Label(w.t("Жанр и камень", "Kind and stone"))
        Flow { for (g in Genre.entries) Chip(w.genre(g), g == m.genre) { c.write { repo.regenre(id, g) } } }
        if (!digging) Btn(w.t("Выкопать", "Dig it up"), Modifier.fillMaxWidth().padding(top = 22.dp), Kind.GHOST) { digging = true }
        else {
            Text(w.t("Выкопать и забыть? Сытость за тот день уменьшится.", "Dig it up and forget it? That day's meal goes too."), style = T.Small.copy(color = C.wax), modifier = Modifier.padding(top = 18.dp))
            Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Btn(w.t("Оставить", "Leave it"), Modifier.weight(1f).padding(end = 6.dp), Kind.GHOST) { digging = false }
                Btn(w.t("Выкопать", "Dig it up"), Modifier.weight(1f).padding(start = 6.dp)) { c.write { repo.dig(id) }; nav.pop() }
            }
        }
    }
}
