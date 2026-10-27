package app.underbed.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import app.underbed.R
import app.underbed.container
import app.underbed.data.Settings
import app.underbed.domain.Horror
import app.underbed.domain.State
import app.underbed.domain.day
import app.underbed.notify.Bell
import app.underbed.ru
import app.underbed.ui.art.pen
import app.underbed.ui.bed.BedScreen
import app.underbed.ui.bed.FeedSheet
import app.underbed.ui.cine.CineScreen
import app.underbed.ui.components.LocalToast
import app.underbed.ui.components.ToastHost
import app.underbed.ui.components.Toaster
import app.underbed.ui.components.night
import app.underbed.ui.components.paper
import app.underbed.ui.onboarding.Onboarding
import app.underbed.ui.settings.SettingsSheet
import app.underbed.ui.theme.C
import app.underbed.ui.theme.T
import app.underbed.ui.watch.WatchScreen
import app.underbed.ui.yard.GraveSheet
import app.underbed.ui.yard.YardScreen
import kotlinx.coroutines.delay
import java.time.ZoneId

/** Everything a screen reads, at one moment. */
class Home(val s: State, val settings: Settings, val w: Words, val now: Long, val horror: Horror) {
    val ru get() = w.ru
    val zone: ZoneId = ZoneId.systemDefault()
    val today: Long get() = now.day(zone)
    val creature get() = settings.creature
    val sat get() = s.satiety(today, zone)
    val asleep get() = s.asleep(now)
}

enum class Tab { BED, YARD, WATCH, CINE }

sealed interface Overlay {
    data object Feed : Overlay
    data class Grave(val id: Long) : Overlay
    data object Settings : Overlay
}

/** What the pet just said about its food; [seq] makes the same words play again. */
class Reaction(val text: String, val refused: Boolean, val seq: Long = System.nanoTime())

/** How screens open sheets, switch tabs and let the pet answer a feeding. */
class Nav(val overlays: SnapshotStateList<Overlay>, val tab: (Tab) -> Unit) {
    var reaction by mutableStateOf<Reaction?>(null)
    /** The last reaction the pet has acted out, so coming back to the tab doesn't replay it. */
    var played = 0L
    fun push(o: Overlay) = overlays.add(o)
    fun pop() { if (overlays.isNotEmpty()) overlays.removeAt(overlays.lastIndex) }
}

@Composable
fun BedRoot(settings: Settings) {
    val ctx = LocalContext.current
    val c = ctx.container
    val state by c.repo.state.collectAsStateWithLifecycle()
    val words = remember(ctx.ru, settings.gender) { Words(ctx.ru, settings.gender) }
    val toaster = remember { Toaster() }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    if (!settings.onboarded) {
        CompositionLocalProvider(LocalToast provides toaster) { Box(Modifier.fillMaxSize()) { Onboarding(words, settings); ToastHost(toaster) } }
        return
    }
    val life = LocalLifecycleOwner.current
    LaunchedEffect(Unit) {
        life.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            c.repo.refresh()
            Bell.plan(ctx)
            while (true) { now = System.currentTimeMillis(); delay(30_000) }
        }
    }
    val s = state ?: run { Box(Modifier.fillMaxSize().night()); return }
    val home = Home(s, settings, words, now, c.horror)

    // The evening bell opens straight onto the watch.
    val opened = remember { (ctx as? Activity)?.intent?.getStringExtra(Bell.OPEN) }
    var tab by remember { mutableStateOf(if (opened == "watch") Tab.WATCH else Tab.BED) }
    val overlays = remember { mutableStateListOf<Overlay>() }
    val nav = remember { Nav(overlays) { tab = it } }
    BackHandler(enabled = overlays.isEmpty() && tab != Tab.BED) { tab = Tab.BED }
    val dark = tab == Tab.WATCH || tab == Tab.CINE
    // Dark icons in the status bar over paper, light ones over the night.
    val view = LocalView.current
    LaunchedEffect(dark) {
        (ctx as? Activity)?.window?.let { WindowCompat.getInsetsController(it, view).isAppearanceLightStatusBars = !dark }
    }

    CompositionLocalProvider(LocalToast provides toaster) {
        Box(Modifier.fillMaxSize().then(if (dark) Modifier.night() else Modifier.paper())) {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    when (tab) {
                        Tab.BED -> BedScreen(home, nav)
                        Tab.YARD -> YardScreen(home, nav)
                        Tab.WATCH -> WatchScreen(home, nav)
                        Tab.CINE -> CineScreen(home, nav)
                    }
                }
                // The night on watch has the whole screen to itself.
                if (!(tab == Tab.WATCH && s.openNight != null)) TabBar(tab, dark) { tab = it }
            }
            when (val o = overlays.lastOrNull()) {
                Overlay.Feed -> FeedSheet(home, nav)
                is Overlay.Grave -> GraveSheet(home, o.id, nav)
                Overlay.Settings -> SettingsSheet(home) { nav.pop() }
                null -> Unit
            }
            ToastHost(toaster)
        }
    }
}

private val ICONS = mapOf(
    Tab.BED to "M2 8h20M2 8v12M22 8v12M2 11h20",
    Tab.YARD to "M6 21V10a6 6 0 0 1 12 0v11M3 21h18M12 9v6M9.5 11.5h5",
    Tab.WATCH to "M2 12s4-6 10-6 10 6 10 6-4 6-10 6S2 12 2 12zM12 9v6",
    Tab.CINE to "M3 5h18v12H3zM8 21h8M12 17v4M10 9l5 2-5 2z",
)

@Composable
private fun TabBar(tab: Tab, dark: Boolean, onTab: (Tab) -> Unit) {
    val items = listOf(Tab.BED to R.string.tab_bed, Tab.YARD to R.string.tab_yard, Tab.WATCH to R.string.tab_watch, Tab.CINE to R.string.tab_cine)
    Column(Modifier.fillMaxWidth().background(if (dark) C.night else C.paper)) {
        Box(Modifier.fillMaxWidth().height(1.5.dp).background(if (dark) C.nightLine else C.ink))
        Row(Modifier.fillMaxWidth().navigationBarsPadding()) {
            for ((t, label) in items) {
                val sel = t == tab
                val color = when { dark && sel -> C.chalk; dark -> C.muted; sel -> C.ink; else -> C.ink2.copy(alpha = .75f) }
                Column(Modifier.weight(1f).clickable { onTab(t) }.padding(top = 9.dp, bottom = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Canvas(Modifier.size(24.dp)) {
                        val k = size.width / 24f
                        scale(k, k, Offset.Zero) {
                            pen(ICONS.getValue(t), 1.6f, color)
                            if (t == Tab.BED) { drawCircle(color, 1.3f, Offset(9f, 16f)); drawCircle(color, 1.3f, Offset(14f, 16f)) }
                        }
                    }
                    Text(stringResource(label), style = T.Body.copy(color = color, fontSize = 12.sp, lineHeight = 15.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal))
                }
            }
        }
    }
}
