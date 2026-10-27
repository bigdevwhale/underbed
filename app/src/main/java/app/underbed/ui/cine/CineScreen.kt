package app.underbed.ui.cine

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.underbed.BuildConfig
import app.underbed.container
import app.underbed.data.Found
import app.underbed.data.Search
import app.underbed.data.Show
import app.underbed.domain.Food
import app.underbed.domain.Genre
import app.underbed.ui.Home
import app.underbed.ui.Nav
import app.underbed.ui.Tab
import app.underbed.ui.art.Couch
import app.underbed.ui.art.Couch.scene
import app.underbed.ui.art.Look
import app.underbed.ui.art.Mouth
import app.underbed.ui.components.Btn
import app.underbed.ui.components.Card
import app.underbed.ui.components.Field
import app.underbed.ui.components.Kind
import app.underbed.ui.components.TopBar
import app.underbed.ui.components.blinkAt
import app.underbed.ui.components.dots
import app.underbed.ui.components.rememberSeconds
import app.underbed.ui.theme.C
import app.underbed.ui.theme.T
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.ceil
import kotlin.math.sqrt

@Composable
fun CineScreen(h: Home, nav: Nav, review: Long? = null) {
    var reviewId by remember { mutableStateOf(review) }
    val show = h.settings.show
    val done = reviewId?.let { h.s.meal(it) }
    when {
        done != null -> Review(h, done.id, onYard = { reviewId = null; nav.tab(Tab.YARD) }) { reviewId = null }
        show == null -> Setup(h)
        else -> Running(h, show) { reviewId = it }
    }
}

@Composable
private fun SceneBox(h: Home, look: Look, jump: Float) {
    val t by rememberSeconds()
    Canvas(Modifier.fillMaxWidth().aspectRatio(Couch.W / Couch.H)) {
        val u = size.width / Couch.W
        scale(u, u, Offset.Zero) { scene(h.creature, Look(look.mouth, look.sat, blinkAt(t), look.traits, kotlin.math.sin(t * 1.85f)), t, jump) }
    }
}

@Composable
private fun Setup(h: Home) {
    val w = h.w
    val c = LocalContext.current.container
    var text by remember { mutableStateOf("") }
    var pick by remember { mutableStateOf<Found?>(null) }
    var found by remember { mutableStateOf<List<Found>>(emptyList()) }
    LaunchedEffect(text, pick) {
        found = emptyList()
        if (pick != null || text.trim().length < 2) return@LaunchedEffect
        val local = h.horror.find(text, Food.FILM, 5).map { Found(it.name(h.ru), it.maker(h.ru), it.year, it.genre, true) }
        found = local
        delay(450)
        val seen = local.map { it.title.lowercase() }.toHashSet()
        found = (local + Search.search(Food.FILM, text, h.ru).orEmpty().filter { seen.add(it.title.lowercase()) }).take(8)
    }
    Column(Modifier.verticalScroll(rememberScrollState())) {
        TopBar(w.t("Кино-ночь", "Film night"), w.t("смотрим вместе", "watching together"), dark = true)
        SceneBox(h, Look(sat = h.sat / 100f, traits = h.s.traits()), 0f)
        Card(Modifier.padding(16.dp)) {
            Text(w.t("Что смотрим?", "What are we watching?"), style = T.Heading)
            Field(text, { text = it; pick = null }, w.t("Название ужастика", "The horror film's title"), Modifier.padding(top = 10.dp))
            if (pick == null) for (f in found) Column(Modifier.fillMaxWidth().clickable { text = f.title; pick = f }.padding(vertical = 7.dp)) {
                Text(f.title, style = T.BodyB)
                Text(listOfNotNull(f.maker.ifBlank { null }, f.year?.toString()).joinToString(" · "), style = T.Small)
            }
            Text(w.t("${w.name(h.creature)} сядет рядом и будет считать, сколько раз ты подпрыгнешь. Держи телефон в руке — он сам почувствует, как ты вздрогнешь. Или жми на кнопку.",
                "${w.name(h.creature)} sits beside you and counts how many times you jump. Keep the phone in your hand and it feels you start. Or press the button."),
                style = T.Small, modifier = Modifier.padding(top = 10.dp))
        }
        Btn(w.t("Начать сеанс", "Start the film"), Modifier.fillMaxWidth().padding(horizontal = 16.dp), Kind.PAPER, enabled = text.isNotBlank()) {
            val p = pick
            val title = text.trim()
            val g = p?.genre ?: h.horror.match(title, Food.FILM)?.genre ?: Genre.OTHER
            c.write { settings.update { it.copy(show = Show(title, p?.maker ?: "", p?.year, g, System.currentTimeMillis(), emptyList())) } }
        }
    }
}

@Composable
internal fun Running(h: Home, show: Show, onEnd: (Long) -> Unit) {
    val w = h.w
    val ctx = LocalContext.current
    val c = ctx.container
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val jumpAnim = remember { Animatable(0f) }
    var shout by remember { mutableStateOf<String?>(w.t("Попкорн мой.", "The popcorn's mine.")) }
    var shouts by remember { mutableIntStateOf(0) }
    var jumps by remember(show.start) { mutableStateOf(show.jumps) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var last by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(1000) } }
    fun jump() {
        val t = System.currentTimeMillis()
        if (t - last < 900) return
        last = t
        val minute = ((t - show.start) / 60_000 + 1).toInt()
        jumps = jumps + minute
        val all = jumps
        c.write { settings.update { s -> s.copy(show = s.show?.copy(jumps = all)) } }
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        shout = w.shout(shouts++)
        scope.launch { jumpAnim.snapTo(0f); jumpAnim.animateTo(1f, tween(700)) }
    }
    val onJump by rememberUpdatedState(::jump)
    // A start felt in the hand counts as a jump.
    if (!BuildConfig.SHOTS) DisposableEffect(Unit) {
        val sm = ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val acc = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val l = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                val (x, y, z) = e.values
                if (kotlin.math.abs(sqrt(x * x + y * y + z * z) - 9.81f) > 9f) onJump()
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) = Unit
        }
        if (acc != null) sm.registerListener(l, acc, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sm.unregisterListener(l) }
    }
    Column(Modifier.verticalScroll(rememberScrollState())) {
        TopBar(show.title, w.t("сеанс идёт", "the film is on"), dark = true)
        SceneBox(h, Look(mouth = if (jumpAnim.value in .05f..0.9f) Mouth.OPEN else Mouth.GRIN, sat = h.sat / 100f, traits = h.s.traits()), jumpAnim.value)
        Text(shout ?: "", style = T.Voice.copy(color = C.chalk, textAlign = TextAlign.Center), modifier = Modifier.fillMaxWidth().padding(top = 6.dp).height(26.dp))
        Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Stat(Modifier.weight(1f), clock(now - show.start), w.t("идёт фильм", "running"))
            Stat(Modifier.weight(1f), "${jumps.size}", w.t("подпрыгнул{|а}", "jumps"))
        }
        Box(Modifier.fillMaxWidth().padding(top = 16.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier.size(170.dp).clip(CircleShape).background(C.night2).border(3.dp, C.chalk, CircleShape).clickable { jump() },
                contentAlignment = Alignment.Center,
            ) { Text(w.t("Подпрыгнул{|а}!", "I jumped!"), style = T.Heading.copy(color = C.chalk, fontSize = 22.sp)) }
        }
        Btn(w.t("Конец фильма", "The end"), Modifier.fillMaxWidth().padding(16.dp), Kind.CHALK) {
            val all = jumps
            val n = all.size
            val level = (1 + ceil(n / 2.0).toInt()).coerceAtMost(5)
            val top = all.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
            c.write {
                val (_, id) = repo.feed(Food.FILM, show.title, level, show.maker, show.year, show.genre, jumps = n, topMinute = top)
                settings.update { it.copy(show = null) }
                withContext(Dispatchers.Main) { onEnd(id) }
            }
        }
    }
}

@Composable
private fun Stat(m: Modifier, big: String, small: String) {
    Column(m, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(big, style = T.Title.copy(color = C.chalk))
        Text(small, style = T.Small.copy(color = C.chalkMuted))
    }
}

private fun clock(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%02d:%02d:%02d".format(s / 3600, s / 60 % 60, s % 60)
}

@Composable
internal fun Review(h: Home, id: Long, onYard: () -> Unit, onAgain: () -> Unit) {
    val w = h.w
    val m = h.s.meal(id) ?: return
    Column(Modifier.verticalScroll(rememberScrollState())) {
        TopBar(w.t("Рецензия", "Review"), w.t("от ", "by ") + w.nameFrom(h.creature), dark = true)
        SceneBox(h, Look(sat = h.sat / 100f, traits = h.s.traits()), 0f)
        Card(Modifier.padding(16.dp)) {
            Text(w.t("«${m.text}»", "“${m.text}”"), style = T.Heading)
            for (line in w.review(h.creature, m.jumps ?: 0, m.topMinute, h.creature.loves.contains(m.genre)))
                Text(line, style = T.Voice.copy(fontSize = 19.sp), modifier = Modifier.padding(top = 8.dp))
            Text(w.t("мурашки ", "goosebumps ") + dots(m.level) + " · " + w.genre(m.genre) + "\n" + w.t("Закопан на погосте.", "Buried in the graveyard."),
                style = T.Small, modifier = Modifier.padding(top = 10.dp))
        }
        Btn(w.t("На погост", "To the graveyard"), Modifier.fillMaxWidth().padding(horizontal = 16.dp), Kind.PAPER, onClick = onYard)
        Btn(w.t("Ещё фильм", "Another film"), Modifier.fillMaxWidth().padding(16.dp), Kind.CHALK, onClick = onAgain)
    }
}
