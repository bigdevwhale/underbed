package app.underbed.ui.bed

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.underbed.container
import app.underbed.data.Fed
import app.underbed.data.Found
import app.underbed.data.Search
import app.underbed.domain.Food
import app.underbed.domain.Genre
import app.underbed.domain.Rules
import app.underbed.domain.Spot
import app.underbed.ui.Home
import app.underbed.ui.Nav
import app.underbed.ui.Reaction
import app.underbed.ui.components.Btn
import app.underbed.ui.components.Chip
import app.underbed.ui.components.Field
import app.underbed.ui.components.Flow
import app.underbed.ui.components.Label
import app.underbed.ui.components.Sheet
import app.underbed.ui.components.dots
import app.underbed.ui.theme.C
import app.underbed.ui.theme.T
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.random.Random

/** What was picked from the search, so the grave gets its maker, year and stone. */
private class Pick(val title: String, val maker: String, val year: Int?, val genre: Genre?, val notScary: Boolean)

@Composable
fun FeedSheet(h: Home, nav: Nav, start: Food = Food.FILM, preset: String = "", presetLevel: Int = 0) {
    val w = h.w
    val c = LocalContext.current.container
    var food by remember { mutableStateOf(start) }
    var text by remember { mutableStateOf(preset) }
    var pick by remember { mutableStateOf<Pick?>(null) }
    var genre by remember { mutableStateOf(Genre.OTHER) }
    var level by remember { mutableIntStateOf(presetLevel) }
    val spots = remember { mutableStateListOf<Spot>() }
    var found by remember { mutableStateOf<List<Found>>(emptyList()) }
    var offline by remember { mutableStateOf(false) }
    var searching by remember { mutableStateOf(false) }
    var shake by remember { mutableStateOf(false) }

    LaunchedEffect(food, text, pick) {
        found = emptyList(); offline = false
        if (!food.fiction || pick != null || text.trim().length < 2) return@LaunchedEffect
        val local = h.horror.find(text, food).map { Found(it.name(h.ru), it.maker(h.ru), it.year, it.genre, true) }
        found = local
        delay(450)
        searching = true
        val online = Search.search(food, text, h.ru)
        searching = false
        offline = online == null
        val seen = local.map { it.title.lowercase() }.toHashSet()
        found = (local + online.orEmpty().filter { seen.add(it.title.lowercase()) }).take(12)
    }

    Sheet(w.t("Чем кормим?", "What's for dinner?"), { nav.pop() }, w.t("Закрыть", "Close")) {
        Flow {
            for (f in Food.entries) Chip("${w.glyph(f)} ${w.food(f)}", f == food) { food = f; pick = null }
        }
        val hint = when (food) {
            Food.FILM -> w.t("Что посмотрел{|а}? Название или режиссёр", "What did you watch? Title or director")
            Food.BOOK -> w.t("Что прочитал{|а}? Название или автор", "What did you read? Title or author")
            Food.NEWS -> w.t("О чём новость? Одной строкой", "What was the news? One line")
            Food.OWN -> w.t("Чего боишься?", "What are you afraid of?")
            Food.CASE -> w.t("Что случилось? Тёмный подъезд, шорох на даче…", "What happened? A dark stairwell, a rustle at night…")
        }
        Field(text, { text = it; pick = null }, hint, Modifier.padding(top = 12.dp).then(if (shake) Modifier.border(2.dp, C.wax, RoundedCornerShape(10.dp)) else Modifier))
        if (food.fiction && pick == null && found.isNotEmpty()) {
            Column(Modifier.fillMaxWidth().padding(top = 4.dp).clip(RoundedCornerShape(8.dp)).border(1.dp, C.line, RoundedCornerShape(8.dp))) {
                for (f in found) {
                    Column(Modifier.fillMaxWidth().clickable {
                        text = f.title
                        pick = Pick(f.title, f.maker, f.year, f.genre, f.known && f.genre == null)
                        f.genre?.let { genre = it }
                    }.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text(f.title, style = T.BodyB)
                        Text(listOfNotNull(f.maker.ifBlank { null }, f.year?.toString(), f.genre?.let { w.genre(it) }).joinToString(" · "), style = T.Small)
                    }
                }
            }
        }
        if (food.fiction && pick == null) {
            val note = when {
                searching -> w.t("Ищу в каталогах…", "Looking in the catalogues…")
                offline -> w.t("Нет интернета — ищу только в своей книжке ужасов.", "No internet: only my own book of horrors.")
                else -> null
            }
            note?.let { Text(it, style = T.Small, modifier = Modifier.padding(top = 4.dp)) }
        }
        pick?.let { p -> Text(listOfNotNull(p.maker.ifBlank { null }, p.year?.toString()).joinToString(" · ").ifBlank { w.t("выбрано", "picked") }, style = T.Small, modifier = Modifier.padding(top = 4.dp)) }
        if (food.fiction) {
            Label(w.t("Жанр — от него зависит камень на погосте", "Kind: it decides the stone in the graveyard"))
            Flow { for (g in Genre.entries) Chip(w.genre(g), g == genre) { genre = g } }
        }
        Text(
            when (food) {
                Food.FILM -> w.t("Ужастик сытный и полезный. На погосте появится его могилка.", "A horror film is filling and good for it. It gets a grave in the graveyard.")
                Food.BOOK -> w.t("Книга ужасов переваривается дольше, зато сытнее. Камень ей — раскрытая книга.", "A horror book takes longer to digest and fills more. Its stone is an open book.")
                Food.NEWS -> w.t("Новости для Буки — фастфуд. Сегодня съедено ${h.s.newsEaten(h.today, h.zone)} из ${Rules.NEWS_A_DAY}.", "News is fast food. Eaten today: ${h.s.newsEaten(h.today, h.zone)} of ${Rules.NEWS_A_DAY}.")
                Food.OWN -> w.t("Бука заберёт этот страх на ночь, а утром вернёт огрызок.", "It takes this fear for the night and gives you back the core in the morning.")
                Food.CASE -> w.t("Живой страх, самый свежий.", "A live fear, the freshest kind.")
            },
            style = T.Small, modifier = Modifier.padding(top = 10.dp),
        )
        Label(w.t("Насколько было страшно?", "How scary was it?"))
        Goosebumps(level, w.t("Держи палец,\nпока страшно", "Hold your finger down\nwhile it's scary")) { level = it }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (level > 0) dots(level) else "", style = T.Small.copy(color = C.ink), modifier = Modifier.weight(1f))
            Text(w.level(level), style = T.Voice)
        }
        if (food == Food.FILM || food == Food.BOOK || food == Food.CASE) {
            Label(w.t("Где почувствовал{|а}?", "Where did you feel it?"))
            Flow { for (s in Spot.entries) Chip(w.spot(s), s in spots) { if (s in spots) spots.remove(s) else spots.add(s) } }
        }
        Btn(w.t("Скормить", "Feed it"), Modifier.fillMaxWidth().padding(top = 18.dp)) {
            val title = text.trim()
            if (title.isEmpty()) { shake = true; return@Btn }
            val lv = level.coerceAtLeast(1)
            val f = food; val g = genre; val p = pick; val sp = spots.toSet()
            val love = h.creature.loves.contains(g)
            val before = h.s.newsEaten(h.today, h.zone)
            c.write {
                val (fed, _) = repo.feed(f, title, lv, p?.maker ?: "", p?.year, if (f.fiction) g else Genre.OTHER, sp)
                val say = when {
                    fed == Fed.REFUSED -> w.refused()
                    f == Food.NEWS -> w.news(before)
                    f == Food.OWN -> w.own()
                    f == Food.CASE -> w.case()
                    else -> w.fed(lv, love, p?.notScary == true, f == Food.BOOK)
                }
                withContext(Dispatchers.Main) { nav.reaction = Reaction(say, fed == Fed.REFUSED) }
            }
            nav.pop()
        }
    }
    LaunchedEffect(shake) { if (shake) { delay(900); shake = false } }
}

/** The goosebump meter: hold a finger down while it's scary; bumps rise and the level climbs to 5. */
@Composable
fun Goosebumps(level: Int, hint: String, onLevel: (Int) -> Unit) {
    val haptic = LocalHapticFeedback.current
    var pressed by remember { mutableStateOf(false) }
    val bumps = remember { mutableStateListOf<Triple<Float, Float, Float>>() }
    LaunchedEffect(pressed) {
        if (!pressed) return@LaunchedEffect
        bumps.clear()
        val r = Random(System.nanoTime())
        val start = System.currentTimeMillis()
        var last = 0
        while (true) {
            val el = (System.currentTimeMillis() - start) / 600f
            val lv = kotlin.math.ceil(el).toInt().coerceIn(1, 5)
            if (lv != last) { last = lv; onLevel(lv); haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
            val want = (el * 18).toInt().coerceAtMost(90)
            while (bumps.size < want) bumps.add(Triple(.04f + r.nextFloat() * .92f, .06f + r.nextFloat() * .86f, .6f + r.nextFloat() * .9f))
            if (lv == 5 && bumps.size >= 90) break
            delay(16)
        }
    }
    // A level from before (a demo, a screenshot) still shows as bumps.
    val shown = if (bumps.isEmpty() && level > 0) remember(level) { val r = Random(level); List(level * 18) { Triple(.04f + r.nextFloat() * .92f, .06f + r.nextFloat() * .86f, .6f + r.nextFloat() * .9f) } } else bumps
    Box(
        Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(12.dp)).background(C.paperLight).border(1.5.dp, C.ink, RoundedCornerShape(12.dp))
            .pointerInput(Unit) { awaitEachGesture { awaitFirstDown(); pressed = true; waitForUpOrCancellation(); pressed = false } },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            for ((x, y, s) in shown) drawCircle(C.ink, 2.6.dp.toPx() * s, Offset(x * size.width, y * size.height))
        }
        if (shown.isEmpty()) Text(hint, style = T.Voice.copy(color = C.muted, textAlign = TextAlign.Center, fontSize = 17.sp))
    }
}
