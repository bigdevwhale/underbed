package app.underbed

import androidx.test.core.app.ApplicationProvider
import app.underbed.data.BedDb
import app.underbed.data.Fed
import app.underbed.data.Repo
import app.underbed.domain.Food
import app.underbed.domain.Genre
import app.underbed.domain.Horror
import app.underbed.domain.Moon
import app.underbed.domain.Rules
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LogicTest {
    private val utc = ZoneId.of("UTC")
    private fun repo() = Repo(BedDb.memory(ApplicationProvider.getApplicationContext())).apply { zone = utc }

    @Test fun newsIsFastFood() = runBlocking {
        val r = repo()
        val t0 = 1_700_000_000_000L
        r.clock = { t0 }
        repeat(Rules.NEWS_A_DAY) { assertEquals(Fed.EATEN, r.feed(Food.NEWS, "n$it", 2).first) }
        assertEquals(Fed.REFUSED, r.feed(Food.NEWS, "one too many", 2).first)
        // A film still goes down.
        assertEquals(Fed.EATEN, r.feed(Food.FILM, "Сияние", 4, genre = Genre.GOTHIC).first)
        val s = r.refresh()
        assertEquals(Rules.NEWS_A_DAY, s.newsEaten(t0 / 86_400_000, utc))
        assertEquals(1, s.graves.size)
        // The next day the limit starts over.
        r.clock = { t0 + 86_400_000L }
        assertEquals(Fed.EATEN, r.feed(Food.NEWS, "tomorrow", 1).first)
    }

    @Test fun traitsAndSleep() = runBlocking {
        val r = repo()
        var t = 1_700_000_000_000L
        r.clock = { t }
        repeat(Rules.TRAIT_AT) { r.feed(Food.FILM, "g$it", 3, genre = Genre.GOTHIC); t += 600_000 }
        repeat(Rules.TRAIT_AT - 1) { r.feed(Food.BOOK, "f$it", 3, genre = Genre.FOLK); t += 600_000 }
        val s = r.refresh()
        assertEquals(listOf(Genre.GOTHIC), s.traits())
        assertTrue(!s.asleep(t))
        assertTrue(s.asleep(t + (Rules.SLEEP_DAYS + 1) * 86_400_000L))
        assertEquals(100, s.satiety(1_700_000_000_000L / 86_400_000, utc))
    }

    @Test fun ownFearsGoToTheNight() = runBlocking {
        val r = repo()
        var t = 1_700_000_000_000L
        r.clock = { t }
        r.feed(Food.OWN, "Стоматолог", 3)
        val day = t / 86_400_000
        assertEquals(listOf("Стоматолог"), r.refresh().ownUnwatched(day, utc))
        t += 60_000
        r.sleep(listOf("Стоматолог", "темнота"))
        assertEquals(emptyList<String>(), r.refresh().ownUnwatched(day, utc))
        assertTrue(r.refresh().openNight != null)
        r.morning()
        assertNull(r.refresh().openNight)
    }

    @Test fun genresFromCatalogueWords() {
        assertEquals(Genre.SLASHER, Genre.read(listOf("slasher film", "horror film")))
        assertEquals(Genre.FOLK, Genre.read(listOf("Folk horror")))
        assertEquals(Genre.JHORROR, Genre.read(listOf("Japanese horror film")))
        assertEquals(Genre.GOTHIC, Genre.read(listOf("Fiction", "Vampires", "Horror tales")))
        assertEquals(Genre.OTHER, Genre.read(listOf("horror film")))
        assertNull(Genre.read(listOf("romantic comedy", "drama")))
    }

    @Test fun moon() {
        // 2024-04-08 18:21 UTC was a new moon (the total solar eclipse); 2024-04-23 23:49 UTC a full one.
        val p = Moon.phase(1712600460000L)
        assertTrue("new $p", p < .02 || p > .98)
        assertTrue(Moon.full(1713916140000L))
    }

    @Test fun catalogue() {
        val h = Horror.load { File("src/main/assets/horror.tsv").inputStream() }
        assertTrue(h.titles.size > 250)
        assertTrue(h.titles.all { it.ru.isNotBlank() && it.en.isNotBlank() && it.year in 1800..2030 })
        assertEquals("Сияние", h.find("сиян", Food.FILM).first().ru)
        assertTrue(h.find("кинг", Food.BOOK).isNotEmpty())
        assertTrue(h.find("Dracula", Food.BOOK).any { it.ru.contains("Дракула") })
    }
}
