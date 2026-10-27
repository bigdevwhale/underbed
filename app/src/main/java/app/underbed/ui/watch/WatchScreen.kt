package app.underbed.ui.watch

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.underbed.container
import app.underbed.domain.Night
import app.underbed.ui.Home
import app.underbed.ui.Nav
import app.underbed.ui.art.Dark
import app.underbed.ui.art.Dark.scene
import app.underbed.ui.components.Btn
import app.underbed.ui.components.Card
import app.underbed.ui.components.Chip
import app.underbed.ui.components.Field
import app.underbed.ui.components.Flow
import app.underbed.ui.components.Kind
import app.underbed.ui.components.TopBar
import app.underbed.ui.components.blinkAt
import app.underbed.ui.components.rememberSeconds
import app.underbed.ui.theme.C
import app.underbed.ui.theme.T
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.format.DateTimeFormatter

@Composable
fun WatchScreen(h: Home, nav: Nav) {
    val open = h.s.openNight
    val last = h.s.lastNight
    when {
        open != null -> NightView(h, open)
        last != null && last.id != h.settings.readNight && h.now - (last.end ?: 0) < 16 * 3_600_000L -> Morning(h, last)
        else -> Setup(h)
    }
}

/** Before sleep: name what frightens you tonight, and the pet takes it under the bed. */
@Composable
internal fun Setup(h: Home) {
    val w = h.w
    val c = LocalContext.current.container
    val extra = remember { mutableStateListOf<String>() }
    var text by remember { mutableStateOf("") }
    val fromDay = h.s.ownUnwatched(h.today, h.zone)
    val fears = (fromDay + extra).distinct()
    fun add(s: String) { val v = s.trim(); if (v.isNotEmpty() && v !in fears) extra.add(v); text = "" }
    Column(Modifier.verticalScroll(rememberScrollState())) {
        TopBar(w.t("Ночной дозор", "Night watch"), w.t("перед сном", "before sleep"), dark = true)
        Canvas(Modifier.fillMaxWidth().aspectRatio(Dark.W / Dark.H * 1.6f).clipToBounds()) {
            val u = size.width / Dark.W
            scale(u, u, Offset.Zero) { translate(0f, -96f) { scene(h.creature, 0f, 0f) } }
        }
        Card(Modifier.padding(horizontal = 16.dp)) {
            Text(w.t("Что пугает сегодня?", "What frightens you tonight?"), style = T.Heading)
            if (fears.isEmpty()) Text(w.t("Ничего? Так не бывает.", "Nothing? There's always something."), style = T.Small, modifier = Modifier.padding(vertical = 8.dp))
            for (f in fears) Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(f, style = T.Voice, modifier = Modifier.weight(1f))
                if (f in extra) Text("✕", style = T.Body.copy(color = C.muted), modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { extra.remove(f) }.padding(horizontal = 8.dp))
                else Text(w.t("из дневника", "from today"), style = T.Small)
            }
            Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Field(text, { text = it }, w.t("ещё один страх…", "one more fear…"), Modifier.weight(1f), onDone = { add(text) })
                Box(Modifier.padding(start = 8.dp).width(48.dp).height(46.dp).clip(RoundedCornerShape(10.dp)).background(C.ink).clickable { add(text) }, contentAlignment = Alignment.Center) {
                    Text("+", style = T.Heading.copy(color = C.paper))
                }
            }
            Flow(Modifier.padding(top = 10.dp)) {
                for (s in listOf(w.t("дедлайн", "a deadline"), w.t("звук за стеной", "a noise behind the wall"), w.t("завтрашний разговор", "tomorrow's talk"), w.t("что-то со здоровьем", "something about my health"), w.t("темнота в коридоре", "the dark hallway")))
                    Chip(s, s in fears) { add(s) }
            }
        }
        Column(Modifier.padding(16.dp)) {
            Btn(w.t("Гасим свет", "Lights out"), Modifier.fillMaxWidth(), Kind.PAPER) {
                val list = fears.ifEmpty { listOf(w.t("темнота", "the dark")) }
                c.write { repo.sleep(list) }
            }
            Text(w.t("${w.name(h.creature)} съест всё это и до утра будет сидеть под кроватью. Экран станет ночником: только два глаза в темноте. Утром — доклад.",
                "${w.name(h.creature)} eats all of it and sits under the bed till morning. The screen becomes a night light: just two eyes in the dark. A report in the morning."),
                style = T.Small.copy(color = C.chalkMuted), modifier = Modifier.padding(top = 10.dp))
        }
    }
}

/** Lights out: black, two eyes, a clock. The screen stays on as a night light. */
@Composable
internal fun NightView(h: Home, n: Night) {
    val w = h.w
    val c = LocalContext.current.container
    val t by rememberSeconds()
    val view = LocalView.current
    DisposableEffect(Unit) { view.keepScreenOn = true; onDispose { view.keepScreenOn = false } }
    var line by remember { mutableStateOf<String?>(w.ate(n.fearList().size)) }
    var seq by remember { mutableIntStateOf(0) }
    var i by remember { mutableIntStateOf(0) }
    LaunchedEffect(seq) { delay(3200); line = null }
    Box(Modifier.fillMaxSize().background(C.night)) {
        Column(Modifier.fillMaxWidth().align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Canvas(Modifier.fillMaxWidth().aspectRatio(Dark.W / Dark.H).pointerInput(Unit) {
                detectTapGestures { line = w.night(i++); seq++ }
            }) {
                val u = size.width / Dark.W
                scale(u, u, Offset.Zero) { scene(h.creature, blinkAt(t, 4.3f), t) }
            }
            Text(line ?: "", style = T.Voice.copy(color = C.chalkMuted, textAlign = TextAlign.Center), modifier = Modifier.height(28.dp))
            val clock = Instant.ofEpochMilli(h.now).atZone(h.zone).format(DateTimeFormatter.ofPattern("HH:mm"))
            Text(w.t("${w.name(h.creature)} сторожит · $clock", "${w.name(h.creature)} keeps watch · $clock"), style = T.Small.copy(color = C.muted))
        }
        Box(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 22.dp)
            .clip(RoundedCornerShape(20.dp)).border(1.dp, C.nightLine, RoundedCornerShape(20.dp)).clickable { c.write { repo.morning() } }
            .padding(horizontal = 18.dp, vertical = 8.dp)) {
            Text(w.t("☀ наступило утро", "☀ it's morning"), style = T.Small.copy(color = C.muted))
        }
    }
}

/** The morning report: who came in the night, and the cores of the fears, much smaller now. */
@Composable
internal fun Morning(h: Home, n: Night) {
    val w = h.w
    val c = LocalContext.current.container
    val fears = n.fearList()
    // Mid-sentence the fears start with a small letter: «приходили: завтра к стоматологу».
    val inline = fears.map { f -> if (f.length > 1 && f[1].isLowerCase()) f.replaceFirstChar { it.lowercase() } else f }
    Column(Modifier.verticalScroll(rememberScrollState())) {
        TopBar(w.t("Утро", "Morning"), w.t("доклад из-под кровати", "a report from under the bed"), dark = true)
        Card(Modifier.padding(horizontal = 16.dp)) {
            Text(w.t("Ночью приходили: ", "In the night came: ") + w.and(inline) + ".", style = T.Voice.copy(fontSize = 19.sp))
            Text(if (fears.size > 1) w.t("Я съел всех. Вот огрызки, забирай:", "I ate them all. Here are the cores:") else w.t("Я его съел. Вот огрызок, забирай:", "I ate it. Here's the core:"),
                style = T.Voice.copy(fontSize = 19.sp), modifier = Modifier.padding(top = 6.dp))
            Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                for (f in fears) {
                    Text(f, style = T.Voice.copy(color = C.muted, textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough))
                    Text(w.stub(f), style = T.Small, modifier = Modifier.padding(bottom = 6.dp))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(C.line.copy(alpha = .6f)))
                }
            }
            Text(w.t("Я спать. Разбуди вечером.", "I'm off to sleep. Wake me in the evening."), style = T.Voice.copy(fontSize = 19.sp), modifier = Modifier.padding(top = 12.dp))
        }
        Column(Modifier.padding(16.dp)) {
            Btn(w.t("Спасибо, ${w.name(h.creature)}", "Thank you, ${w.name(h.creature)}"), Modifier.fillMaxWidth(), Kind.CHALK) {
                c.write { settings.update { it.copy(readNight = n.id) } }
            }
        }
    }
}

