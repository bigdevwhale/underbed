package app.underbed.ui.bed

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.underbed.domain.Creature
import app.underbed.domain.Genre
import app.underbed.domain.Meal
import app.underbed.domain.Moon
import app.underbed.domain.Rules
import app.underbed.ui.Home
import app.underbed.ui.Nav
import app.underbed.ui.Overlay
import app.underbed.ui.Words
import app.underbed.ui.art.Bed
import app.underbed.ui.art.Bed.scene
import app.underbed.ui.art.Look
import app.underbed.ui.art.Mouth
import app.underbed.ui.art.ink
import app.underbed.ui.art.moon
import app.underbed.ui.art.mouthY
import app.underbed.ui.art.pet
import app.underbed.ui.components.Btn
import app.underbed.ui.components.TopBar
import app.underbed.ui.components.blinkAt
import app.underbed.ui.components.dots
import app.underbed.ui.components.rememberSeconds
import app.underbed.ui.theme.C
import app.underbed.ui.theme.T
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** The first thing the pet says when the screen opens: the hour, the day, the moon, or its belly. */
fun greeting(h: Home): String {
    val w = h.w
    return when {
        Moon.oxHour(h.now, h.zone) -> w.ox()
        Moon.halloween(h.now, h.zone) -> w.halloween()
        Moon.friday13(h.now, h.zone) -> w.friday13()
        Moon.full(h.now) && (h.creature == Creature.WOLF || h.now.hashCode() % 2 == 0) -> w.fullMoon(h.creature)
        h.asleep -> w.asleep()
        h.sat >= 95 -> w.stuffed()
        h.sat < 15 -> w.hungry()
        else -> w.hello(h.creature)
    }
}

@Composable
fun BedScreen(h: Home, nav: Nav) {
    val w = h.w
    val t by rememberSeconds()
    val scope = rememberCoroutineScope()
    var mouth by remember { mutableStateOf<Mouth?>(null) }
    var bubble by remember { mutableStateOf<String?>(greeting(h)) }
    var bubbleSeq by remember { mutableIntStateOf(0) }
    var pokes by remember { mutableIntStateOf((h.now / 1000 % 6).toInt()) }
    val morsel = remember { Animatable(0f) }
    val jump = remember { Animatable(0f) }
    fun say(s: String) { bubble = s; bubbleSeq++ }

    LaunchedEffect(bubbleSeq) { if (bubble != null) { delay(4800); bubble = null } }
    LaunchedEffect(nav.reaction?.seq) {
        val r = nav.reaction ?: return@LaunchedEffect
        nav.reaction = null
        if (r.refused) {
            mouth = Mouth.COVERED; say(r.text); delay(5200); mouth = null
        } else {
            mouth = Mouth.OPEN
            morsel.snapTo(0f)
            morsel.animateTo(1f, tween(750, easing = LinearEasing))
            morsel.snapTo(0f)
            mouth = null
            say(r.text)
        }
    }

    Column(Modifier.verticalScroll(rememberScrollState())) {
        TopBar(w.name(h.creature), w.who(h.creature)) {
            MoonTag(h)
            Icon(Icons.Outlined.Settings, w.t("Настройки", "Settings"), tint = C.ink2,
                modifier = Modifier.padding(start = 6.dp).clip(RoundedCornerShape(20.dp)).clickable { nav.push(Overlay.Settings) }.padding(6.dp).size(24.dp))
        }
        Box(Modifier.fillMaxWidth().border(1.5.dp, C.ink)) {
            Canvas(
                Modifier.fillMaxWidth().aspectRatio(Bed.W / Bed.H).pointerInput(h.creature) {
                    detectTapGestures { p ->
                        val u = size.width / Bed.W
                        val d = Offset(p.x / u, p.y / u) - Bed.PET
                        if (d.x * d.x / (80f * 80f) + d.y * d.y / (90f * 90f) <= 1f) {
                            scope.launch { jump.snapTo(0f); jump.animateTo(1f, tween(650)) }
                            say(w.poke(pokes++))
                        }
                    }
                },
            ) {
                val u = size.width / Bed.W
                scale(u, u, Offset.Zero) {
                    scene(t)
                    val face = mouth ?: if (h.asleep) Mouth.SLEEP else Mouth.GRIN
                    val lift = sin(jump.value * PI.toFloat()) * 40f
                    val look = Look(
                        mouth = face, sat = h.sat / 100f, blink = if (face == Mouth.SLEEP) 0f else blinkAt(t), traits = h.s.traits(),
                        breathe = sin(t * 1.85f), float = if (h.creature == Creature.GHOST) (1 - cos(t * 1.57f)) * 4f else 0f,
                    )
                    pet(h.creature, Bed.PET - Offset(0f, lift), 1f, look)
                    if (morsel.value > 0f) morsel(morsel.value, Bed.PET + Offset(0f, mouthY(h.creature)))
                }
            }
            bubble?.let { b ->
                Box(Modifier.align(Alignment.TopCenter).padding(top = 84.dp, start = 30.dp, end = 30.dp)) {
                    Text(b, style = T.Voice.copy(textAlign = TextAlign.Center), modifier = Modifier.widthIn(max = 280.dp).clip(RoundedCornerShape(14.dp)).background(C.paper).border(1.5.dp, C.ink, RoundedCornerShape(14.dp)).padding(horizontal = 12.dp, vertical = 8.dp))
                }
            }
        }
        Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(w.t("Сытость", "Fed"), style = T.Label)
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f).height(14.dp).clip(RoundedCornerShape(8.dp)).border(1.5.dp, C.ink, RoundedCornerShape(8.dp))) {
                    Canvas(Modifier.fillMaxWidth(h.sat / 100f).height(14.dp)) {
                        var x = -size.height
                        while (x < size.width) { drawLine(C.ink, Offset(x, size.height), Offset(x + size.height, 0f), 1.6f); x += 4.dp.toPx() }
                    }
                }
                Text(w.satiety(h.sat), style = T.Label, modifier = Modifier.padding(start = 10.dp).widthIn(min = 70.dp), textAlign = TextAlign.End)
            }
            Btn(w.t("Покормить страхом", "Feed it a fear"), Modifier.fillMaxWidth().padding(top = 14.dp)) { nav.push(Overlay.Feed) }
            growth(h)?.let { Text(it, style = T.Small, modifier = Modifier.padding(top = 12.dp)) }
            Text(w.t("Сегодня съедено", "Eaten today"), style = T.Label, modifier = Modifier.padding(top = 18.dp))
            Box(Modifier.fillMaxWidth().padding(top = 4.dp).height(1.dp).background(C.ink))
            val today = h.s.on(h.today, h.zone).sortedByDescending { it.at }
            if (today.isEmpty()) Text(if (h.asleep) w.t("Пусто. Бука спит в шкафу, весь в нафталине.", "Nothing. It's asleep in the wardrobe, smelling of mothballs.") else w.t("Пока пусто. Из-под кровати смотрят два глаза.", "Nothing yet. Two eyes are watching from under the bed."),
                style = T.Small, modifier = Modifier.padding(vertical = 10.dp))
            for (m in today) MealRow(w, m)
            Text(w.t("Ужастики и книги сытные: от них растут и оставляют могилку. Новости — фастфуд: больше ${Rules.NEWS_A_DAY} в день не ест. Свои страхи забирает на ночь в дозор.",
                "Films and books are filling: they make it grow and get a grave. News is fast food: no more than ${Rules.NEWS_A_DAY} a day. Your own fears it takes for the night watch."),
                style = T.Small.copy(color = C.muted), modifier = Modifier.padding(top = 14.dp))
            Spacer(Modifier.height(10.dp))
        }
    }
}

/** What has grown on the pet, or how close the next thing is. */
private fun growth(h: Home): String? {
    val w = h.w
    val tr = h.s.traits()
    if (tr.isNotEmpty()) return w.t("Отросло: ", "Grown: ") + tr.joinToString(", ") { w.trait(it) } + w.t(" — растёт на ", " — it grows on ") + w.and(tr.map { w.genreOn(it) }) + "."
    val (g, n) = h.s.genres().filter { it.key != Genre.OTHER }.maxByOrNull { it.value }?.toPair() ?: return null
    val left = Rules.TRAIT_AT - n
    return w.t("Ещё $left из жанра «${w.genre(g)}» — и что-нибудь отрастёт.", "$left more of ${w.genreOn(g)} and something will grow.")
}

@Composable
private fun MealRow(w: Words, m: Meal) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(w.glyph(m.food), style = T.Body, modifier = Modifier.width(20.dp), textAlign = TextAlign.Center)
        Text(m.text, style = T.Body.copy(color = if (m.eaten) C.ink else C.muted, textDecoration = if (m.eaten) null else androidx.compose.ui.text.style.TextDecoration.LineThrough), modifier = Modifier.weight(1f))
        Text(if (m.eaten) dots(m.level) else w.t("не стал есть", "wouldn't eat"), style = if (m.eaten) T.Small.copy(color = C.ink) else T.Small)
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(C.line.copy(alpha = .6f)))
}

@Composable
private fun MoonTag(h: Home) {
    val ph = Moon.phase(h.now)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(20.dp)) { val r = size.minDimension / 2; moon(center, r - 1f, ph) }
        Text(" " + h.w.moon(ph), style = T.Small)
    }
}

/** A frightened little ink blot flying into the open mouth, [p] 0..1. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.morsel(p: Float, to: Offset) {
    val from = Offset(Bed.W / 2, Bed.H + 10f)
    val x = from.x + (to.x - from.x) * p
    val y = from.y + (to.y - from.y) * p - sin(p * PI.toFloat()) * 90f
    val s = 1.3f - .8f * p
    translate(x, y) {
        scale(s, s, Offset.Zero) {
            rotate(p * 360f, Offset.Zero) {
                ink("M-10 -3C-12 -10 -2 -13 3 -10C10 -12 13 -2 10 3C12 10 2 13 -3 10C-10 12 -13 2 -10 -3Z")
                drawCircle(C.paper, 3f, Offset(-4f, -2f)); drawCircle(C.paper, 3f, Offset(4f, -2f))
                drawCircle(C.ink, 1.2f, Offset(-4f, -1.5f)); drawCircle(C.ink, 1.2f, Offset(4f, -1.5f))
                drawOval(C.paper, Offset(-2f, 2.4f), androidx.compose.ui.geometry.Size(4f, 5.2f))
            }
        }
    }
}
