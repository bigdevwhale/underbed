package app.underbed.ui.onboarding

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import app.underbed.container
import app.underbed.data.Gender
import app.underbed.data.Settings
import app.underbed.domain.Creature
import app.underbed.ui.Words
import app.underbed.ui.art.Dark
import app.underbed.ui.art.Dark.scene
import app.underbed.ui.art.petTile
import app.underbed.ui.components.Btn
import app.underbed.ui.components.Chip
import app.underbed.ui.components.Flow
import app.underbed.ui.components.Kind
import app.underbed.ui.components.blinkAt
import app.underbed.ui.components.night
import app.underbed.ui.components.rememberSeconds
import app.underbed.ui.theme.C
import app.underbed.ui.theme.T

/** Two pages: something lives under your bed; pick who. */
@Composable
fun Onboarding(w: Words, settings: Settings, startPage: Int = 0) {
    var page by remember { mutableIntStateOf(startPage) }
    if (page == 0) Welcome(w) { page = 1 } else Pick(w, settings)
}

@Composable
private fun Welcome(w: Words, next: () -> Unit) {
    val t by rememberSeconds()
    Column(Modifier.fillMaxSize().night().statusBarsPadding().navigationBarsPadding().padding(horizontal = 22.dp).verticalScroll(rememberScrollState())) {
        Canvas(Modifier.fillMaxWidth().padding(top = 30.dp).aspectRatio(Dark.W / Dark.H)) {
            val u = size.width / Dark.W
            scale(u, u, Offset.Zero) { scene(Creature.BUKA, blinkAt(t, 3.6f), t) }
        }
        Text(w.t("Под кроватью кто-то есть", "Something lives under your bed"), style = T.Title.copy(color = C.chalk, textAlign = TextAlign.Center), modifier = Modifier.fillMaxWidth().padding(top = 14.dp))
        Text(
            w.t("В детстве ты его боял{ся|ась}. Теперь его можно кормить. Он ест страх: ужастики, книги ужасов, тревожные новости, твои страхи и то, что случилось в тёмном подъезде.",
                "As a child you were afraid of it. Now you can feed it. It eats fear: horror films, horror books, anxious news, your own fears, and the thing in the dark stairwell."),
            style = T.Body.copy(color = C.chalk, textAlign = TextAlign.Center), modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        )
        Column(Modifier.padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (line in listOf(
                w.t("☠  Ужастики и книги сытные — от них он растёт, а на погосте появляются могилки.", "☠  Films and books fill it up: it grows, and they get graves in the graveyard."),
                w.t("✉  Новости — фастфуд. Больше трёх в день не съест: скажет «иди погуляй».", "✉  News is fast food. Over three a day and it tells you to go for a walk."),
                w.t("☾  Твои страхи он забирает на ночь и сторожит, а утром отдаёт огрызки.", "☾  Your own fears it takes for the night and keeps watch; in the morning you get the cores."),
            )) Text(line, style = T.Small.copy(color = C.chalkMuted, fontSize = 15.sp, lineHeight = 21.sp))
        }
        Btn(w.t("Заглянуть под кровать", "Look under the bed"), Modifier.fillMaxWidth().padding(top = 26.dp, bottom = 20.dp), Kind.PAPER, onClick = next)
    }
}

@Composable
private fun Pick(w: Words, settings: Settings) {
    val ctx = LocalContext.current
    val c = ctx.container
    var pick by remember { mutableStateOf(settings.creature) }
    var gender by remember { mutableStateOf(settings.gender) }
    fun finish() { val p = pick; val g = gender; c.write { this.settings.update { it.copy(onboarded = true, creature = p, gender = g ?: it.gender) } } }
    // The evening knock needs leave to post; asked once, as the pet moves in.
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { finish() }
    Column(Modifier.fillMaxSize().night().statusBarsPadding().navigationBarsPadding().padding(horizontal = 16.dp).verticalScroll(rememberScrollState())) {
        Text(w.t("Кто живёт под твоей кроватью?", "Who lives under your bed?"), style = T.Title.copy(color = C.chalk, textAlign = TextAlign.Center, fontSize = 25.sp), modifier = Modifier.fillMaxWidth().padding(top = 20.dp))
        Text(w.t("У каждого свой вкус на страх. Сменить можно потом в настройках.", "Each has its own taste in fear. You can change it later in settings."), style = T.Small.copy(color = C.chalkMuted, textAlign = TextAlign.Center), modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 12.dp))
        CreatureGrid(w, pick) { pick = it }
        if (w.ru) {
            Text("Как к тебе обращаться", style = T.Label.copy(color = C.chalkMuted), modifier = Modifier.padding(top = 16.dp, bottom = 6.dp))
            Flow {
                Chip("испугался", gender == Gender.HE, dark = true) { gender = Gender.HE }
                Chip("испугалась", gender == Gender.SHE, dark = true) { gender = Gender.SHE }
            }
        }
        Btn(w.t("Взять к себе", "Take it in"), Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 20.dp), Kind.PAPER) {
            if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                ask.launch(Manifest.permission.POST_NOTIFICATIONS)
            else finish()
        }
    }
}

/** The seven creatures in tiles, two to a row. */
@Composable
fun CreatureGrid(w: Words, pick: Creature, onPick: (Creature) -> Unit) {
    val all = Creature.entries
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        for (row in all.chunked(2)) Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            for (cr in row) {
                val on = cr == pick
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).border(if (on) 2.dp else 1.5.dp, if (on) C.glow else C.nightLine, RoundedCornerShape(12.dp))
                        .clickable { onPick(cr) }.padding(bottom = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(Modifier.fillMaxWidth().height(104.dp)) { Canvas(Modifier.fillMaxSize()) { petTile(cr) } }
                    Text(w.name(cr), style = T.BodyB.copy(color = C.chalk, fontSize = 17.sp), modifier = Modifier.padding(top = 6.dp))
                    Text(w.diet(cr), style = T.Small.copy(color = C.chalkMuted, fontSize = 12.sp, textAlign = TextAlign.Center), modifier = Modifier.padding(horizontal = 6.dp))
                }
            }
        }
    }
}
