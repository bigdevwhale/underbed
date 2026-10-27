package app.underbed.data

import app.underbed.AppContainer
import app.underbed.domain.Food
import app.underbed.domain.Genre
import app.underbed.domain.Spot
import app.underbed.localized
import app.underbed.ru
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random

/**
 * The demo build's past: about six weeks of fear played through the real repository with a
 * wound-back clock, so the graveyard is full, a top hat and twig antlers have grown, and last night's
 * report waits for the morning. The welcome pages still come first, as on a real first launch.
 */
object DemoSeed {
    private val NEWS = listOf(
        "Опять пишут про новый вирус" to "Another new virus in the headlines", "Курс снова скачет" to "The currency is jumping again",
        "Сокращения по всей отрасли" to "Layoffs across the industry", "Жара побила рекорд" to "The heat broke another record",
        "Аренда опять подорожала" to "Rents went up again", "Где-то снова трясло" to "Another earthquake somewhere",
        "Пробки на мосту до ночи" to "The bridge jammed till night", "Учёные нашли новую угрозу" to "Scientists found a new threat",
    )
    private val OWN = listOf(
        "Завтра к стоматологу" to "Dentist tomorrow", "Разговор с начальником" to "A talk with the boss",
        "Анализы придут в пятницу" to "Test results due Friday", "Выступать на собрании" to "Speaking at the meeting",
        "Не успею к дедлайну" to "I won't make the deadline", "Позвонить маме про врача" to "Calling Mum about the doctor",
        "Платёж по кредиту" to "The loan payment", "Новая работа в понедельник" to "New job on Monday",
    )
    private val CASES = listOf(
        "Тёмный подъезд, лампочка не горит" to "Dark stairwell, the bulb was out", "Шорох на даче ночью" to "A rustle at the cottage at night",
        "Лифт застрял между этажами" to "The lift got stuck between floors", "Кто-то шёл следом до самого дома" to "Someone walked behind me all the way home",
    )
    private val EPITAPHS = listOf(
        "Работал без выходных" to "Worked without a day off", "Перезвонила через семь дней" to "Called back in seven days",
        "Не смотри" to "Don't look", "Кто-то из нас не тот" to "One of us isn't who we seem", "Шёл медленно, но всегда успевал" to "Walked slowly, always arrived",
        "Светло, а всё равно страшно" to "Broad daylight, still scary", "Зря зашёл в телепорт" to "Shouldn't have stepped in", "Тень была длиннее хозяина" to "The shadow outgrew its owner",
        "Писал письма до последнего" to "Wrote letters to the very end", "Спит, но видит сны" to "Sleeps, but dreams",
    )
    /** This person likes gothic and folk most, so a top hat and twig antlers have grown. */
    private val TASTE = listOf(Genre.GOTHIC, Genre.GOTHIC, Genre.FOLK, Genre.FOLK, Genre.GOTHIC, Genre.FOLK)

    suspend fun seedIfEmpty(c: AppContainer) {
        val repo = c.repo
        if (repo.refresh().meals.isNotEmpty()) return
        val all = c.horror.titles
        if (all.isEmpty()) return
        repo.hold = true
        val ru = c.app.localized().ru
        fun pick(p: Pair<String, String>) = if (ru) p.first else p.second
        val zone = ZoneId.systemDefault()
        repo.zone = zone
        val rnd = Random(42)
        val today = LocalDate.now(zone)
        val now = System.currentTimeMillis()
        fun at(d: LocalDate, h: Int, m: Int) = d.atTime(h, m).atZone(zone).toInstant().toEpochMilli()
        val used = HashSet<String>()
        fun title(food: Food, g: Genre?) = all.filter { it.food == food && (g == null || it.genre == g) && it.en !in used }.ifEmpty { all.filter { it.food == food } }
            .let { it[rnd.nextInt(it.size)] }.also { used += it.en }
        val ids = ArrayList<Long>()

        var d = today.minusDays(44)
        while (d.isBefore(today)) {
            val day = d
            if (rnd.nextFloat() < .28f) { d = d.plusDays(1); continue }
            // An evening of horror: a film most days, sometimes a book.
            if (rnd.nextFloat() < .8f) {
                val food = if (rnd.nextFloat() < .7f) Food.FILM else Food.BOOK
                val g = if (rnd.nextFloat() < .55f) TASTE[rnd.nextInt(TASTE.size)] else null
                val t = title(food, g)
                repo.clock = { at(day, 21, rnd.nextInt(60)) }
                val spots = Spot.entries.shuffled(rnd).take(rnd.nextInt(3)).toSet()
                val film = food == Food.FILM && rnd.nextFloat() < .3f
                val jumps = if (film) rnd.nextInt(7) else null
                ids += repo.feed(food, t.name(ru), 2 + rnd.nextInt(4), t.maker(ru), t.year, t.genre, spots, jumps, jumps?.let { 20 + rnd.nextInt(70) }).second
            }
            repeat(rnd.nextInt(4)) { i ->
                repo.clock = { at(day, 8 + i * 3, rnd.nextInt(60)) }
                repo.feed(Food.NEWS, pick(NEWS[rnd.nextInt(NEWS.size)]), 1 + rnd.nextInt(3))
            }
            if (rnd.nextFloat() < .12f) { repo.clock = { at(day, 19, 40) }; repo.feed(Food.NEWS, pick(NEWS[rnd.nextInt(NEWS.size)]), 2) }
            val own = if (rnd.nextFloat() < .4f) pick(OWN[rnd.nextInt(OWN.size)]).also { repo.clock = { at(day, 17, 10) }; repo.feed(Food.OWN, it, 2 + rnd.nextInt(3)) } else null
            if (rnd.nextFloat() < .15f) { repo.clock = { at(day, 22, 5) }; repo.feed(Food.CASE, pick(CASES[rnd.nextInt(CASES.size)]), 3 + rnd.nextInt(3), spots = setOf(Spot.NAPE)) }
            if (own != null || rnd.nextFloat() < .2f) {
                repo.clock = { at(day, 23, 30) }
                repo.sleep(listOfNotNull(own, if (rnd.nextFloat() < .5f) pick(listOf("звук за стеной" to "a noise behind the wall", "темнота в коридоре" to "the dark hallway")[rnd.nextInt(2)]) else null).ifEmpty { listOf(pick("дедлайн" to "a deadline")) })
                repo.clock = { at(day.plusDays(1), 7, 20) }
                repo.morning()
            }
            d = d.plusDays(1)
        }
        // Epitaphs on some of the stones.
        for ((i, id) in ids.shuffled(rnd).take(EPITAPHS.size).withIndex()) repo.epitaph(id, pick(EPITAPHS[i]))
        // Last night's fears, eaten; the report waits. And this morning's news and fear.
        val yesterday = today.minusDays(1)
        repo.clock = { at(yesterday, 18, 0) }
        repo.feed(Food.OWN, pick(OWN[0]), 4)
        repo.clock = { at(yesterday, 23, 40) }
        repo.sleep(listOf(pick(OWN[0]), pick("звук за стеной" to "a noise behind the wall"), pick("что-то со здоровьем" to "something about my health")))
        repo.clock = { minOf(at(today, 7, 15), now - 60_000) }
        repo.morning()
        repo.clock = { minOf(at(today, 9, 10), now - 30_000) }
        repo.feed(Food.NEWS, pick(NEWS[0]), 2)
        repo.clock = { minOf(at(today, 10, 5), now - 20_000) }
        repo.feed(Food.OWN, pick(OWN[1]), 3)
        repo.clock = System::currentTimeMillis
        repo.hold = false
        repo.refresh()
    }
}
