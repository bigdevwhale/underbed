package app.underbed

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import app.underbed.data.DemoSeed
import app.underbed.data.Gender
import app.underbed.data.Show
import app.underbed.domain.Food
import app.underbed.domain.Genre
import app.underbed.domain.Night
import app.underbed.ui.Home
import app.underbed.ui.Nav
import app.underbed.ui.Words
import app.underbed.ui.bed.BedScreen
import app.underbed.ui.bed.FeedSheet
import app.underbed.ui.cine.Review
import app.underbed.ui.cine.Running
import app.underbed.ui.components.LocalToast
import app.underbed.ui.components.Toaster
import app.underbed.ui.components.night
import app.underbed.ui.components.paper
import app.underbed.ui.onboarding.Onboarding
import app.underbed.ui.settings.SettingsSheet
import app.underbed.ui.theme.BedTheme
import app.underbed.ui.watch.Morning
import app.underbed.ui.watch.NightView
import app.underbed.ui.watch.Setup
import app.underbed.ui.yard.GraveSheet
import app.underbed.ui.yard.YardScreen
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Every main screen over the demo history, drawn to PNGs (run with `-Prender`), English and Russian. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
abstract class ScreensBase {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    abstract val lang: String
    private val out by lazy { File(System.getProperty("render.out") ?: "build/art", "screens-$lang").apply { mkdirs() } }

    private fun home(): Home = runBlocking {
        val app = ApplicationProvider.getApplicationContext<UnderbedApp>()
        val c = app.container
        DemoSeed.seedIfEmpty(c)
        val s = c.repo.refresh()
        val st = c.settings.now()
        // Evening, after the day's last meal, so the pet greets rather than talks about the hour.
        val evening = java.time.LocalDate.now().atTime(20, 0).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        Home(s, st, Words(lang == "ru", Gender.HE), maxOf(evening, s.meals.maxOf { it.at } + 60_000), c.horror)
    }

    private fun nav() = Nav(mutableStateListOf()) {}

    private fun shot(name: String, dark: Boolean = false, content: @Composable (Home) -> Unit) {
        val h = home()
        rule.mainClock.autoAdvance = false
        rule.setContent {
            BedTheme {
                CompositionLocalProvider(LocalToast provides Toaster()) {
                    Box(Modifier.fillMaxSize().then(if (dark) Modifier.night() else Modifier.paper())) { content(h) }
                }
            }
        }
        rule.mainClock.advanceTimeBy(2_500)
        rule.runOnUiThread {
            val view = rule.activity.window.decorView
            val image = android.graphics.Bitmap.createBitmap(view.width, view.height, android.graphics.Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(image))
            File(out, "$name.png").outputStream().use { image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    private fun film(h: Home) = h.s.graves.filter { it.food == Food.FILM && it.epitaph.isNotEmpty() }.maxBy { it.at }

    @Test fun welcome() = shot("0-welcome", true) { Onboarding(it.w, it.settings.copy(onboarded = false)) }
    @Test fun pick() = shot("1-pick", true) { Onboarding(it.w, it.settings.copy(onboarded = false), startPage = 1) }
    @Test fun bed() = shot("2-bed") { BedScreen(it, nav()) }
    @Test fun feed() = shot("3-feed") { h ->
        BedScreen(h, nav())
        FeedSheet(h, nav(), Food.BOOK, h.w.t("Призрак дома на холме", "The Haunting of Hill House"), 4)
    }
    @Test fun yard() = shot("4-yard") { YardScreen(it, nav()) }
    @Test fun grave() = shot("5-grave") { h -> YardScreen(h, nav()); GraveSheet(h, film(h).id, nav()) }
    @Test fun watch() = shot("6-watch", true) { Setup(it) }
    @Test fun night() = shot("7-night", true) { h -> NightView(h, Night(1, h.now, null, "a\nb\nc")) }
    @Test fun morning() = shot("8-morning", true) { h -> Morning(h, h.s.lastNight!!) }
    @Test fun cine() = shot("9-cine", true) { h ->
        Running(h, Show(h.w.t("Сияние", "The Shining"), "Stanley Kubrick", 1980, Genre.GOTHIC, System.currentTimeMillis() - 47 * 60_000, listOf(12, 31, 31, 44))) {}
    }
    @Test fun review() = shot("a-review", true) { h -> Review(h, h.s.graves.filter { it.jumps != null }.maxBy { it.at }.id, {}, {}) }
    @Test fun settings() = shot("x-settings") { h -> BedScreen(h, nav()); SettingsSheet(h) {} }
}

class RenderArtScreensEn : ScreensBase() {
    override val lang = "en"
}

@Config(qualifiers = "ru-w411dp-h891dp-xxhdpi")
class RenderArtScreensRu : ScreensBase() {
    override val lang = "ru"
}
