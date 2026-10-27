package app.underbed.domain

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.ZoneId

/** Who lives under the bed. Each one has its eyes, its favourite fears, and a dark or a pale body. */
enum class Creature(val eye: Long, val loves: Set<Genre>, val dark: Boolean) {
    BUKA(0xFFF2C14E, emptySet(), true),
    GHOST(0xFFF6E7B8, setOf(Genre.GOTHIC, Genre.JHORROR), false),
    ZOMBIE(0xFFC9D46A, setOf(Genre.BODY, Genre.SLASHER), false),
    VAMPIRE(0xFFE0523F, setOf(Genre.GOTHIC), false),
    WOLF(0xFFF0A23A, setOf(Genre.SLASHER, Genre.FOLK), true),
    DOLL(0xFFF2C14E, setOf(Genre.PSYCHO, Genre.JHORROR), false),
    KIKI(0xFFA8D66A, setOf(Genre.FOLK), true),
}

/** The kind of horror a film or a book is; it chooses the stone on the graveyard and what grows on the pet. */
enum class Genre {
    GOTHIC, SLASHER, FOLK, COSMIC, JHORROR, PSYCHO, BODY, OTHER;

    companion object {
        fun of(key: String): Genre = entries.firstOrNull { it.name.equals(key.trim(), ignoreCase = true) } ?: OTHER

        /**
         * Reads a genre out of catalogue words (Wikidata genres, Open Library subjects): the most
         * particular kind first, plain "horror" last. Null when nothing in them is frightening.
         */
        fun read(words: List<String>): Genre? {
            val w = words.map { it.lowercase() }
            fun has(vararg k: String) = w.any { s -> k.any { it in s } }
            return when {
                has("slasher", "serial killer", "home invasion") -> SLASHER
                has("folk horror", "witchcraft", "witches", "pagan", "folklore") -> FOLK
                has("body horror", "zombie", "infection", "cannibal") -> BODY
                has("japanese horror", "j-horror", "korean horror", "asian horror", "onryō", "yūrei", "kaidan") -> JHORROR
                has("psychological horror", "psychological thriller") -> PSYCHO
                has("gothic", "vampire", "haunted house", "ghost") -> GOTHIC
                has("science fiction horror", "monster", "lovecraft", "cosmic horror", "creature", "kaiju", "cthulhu") -> COSMIC
                has("horror", "terror", "ужас") -> OTHER
                else -> null
            }
        }
    }
}

/** What the pet can be fed. */
enum class Food {
    FILM, BOOK, NEWS, OWN, CASE;

    /** Films and books are the fear made up on purpose: they fill up and get a grave. */
    val fiction get() = this == FILM || this == BOOK
}

/** Where the fear was felt. */
enum class Spot { NAPE, BACK, BELLY, HANDS, HEELS, THROAT }

/** One portion of fear. Films and books that were eaten lie on the graveyard. */
@Entity(tableName = "meal")
data class Meal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val at: Long,
    val food: Food,
    val text: String,
    /** The director or the author. */
    val maker: String = "",
    val year: Int? = null,
    val genre: Genre = Genre.OTHER,
    /** How frightening it was, 1 to 5, from the goosebump meter. */
    val level: Int,
    /** [Spot] names, comma separated. */
    val spots: String = "",
    /** False for news the pet would not eat: one too many for the day. */
    val eaten: Boolean = true,
    val epitaph: String = "",
    /** From a film night: how many times the person jumped, and the minute of most jumps. */
    val jumps: Int? = null,
    val topMinute: Int? = null,
) {
    val grave get() = food.fiction && eaten
    fun spotList(): List<Spot> = spots.split(',').mapNotNull { s -> Spot.entries.firstOrNull { it.name == s } }
}

/** A night on watch: the fears named before sleep, one per line. */
@Entity(tableName = "night")
data class Night(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val start: Long,
    val end: Long? = null,
    val fears: String,
) {
    fun fearList() = fears.lines().map { it.trim() }.filter { it.isNotEmpty() }
}

fun Long.day(zone: ZoneId): Long = Instant.ofEpochMilli(this).atZone(zone).toLocalDate().toEpochDay()

object Rules {
    /** News is fast food: after this many in a day the pet covers its mouth. */
    const val NEWS_A_DAY = 3
    /** Films and books of one kind it takes for that kind to start growing on the pet. */
    const val TRAIT_AT = 5
    /** Days without fear after which the pet falls asleep in the wardrobe. */
    const val SLEEP_DAYS = 3

    /** How much a meal fills, out of a day's 100. */
    fun bite(m: Meal): Int = if (!m.eaten) 0 else when (m.food) {
        Food.FILM -> 8 + 6 * m.level
        Food.BOOK -> 14 + 6 * m.level
        Food.OWN -> 10 + 3 * m.level
        Food.CASE -> 8 + 4 * m.level
        Food.NEWS -> 6
    }
}

/** Everything in the database at one moment. */
class State(val meals: List<Meal>, val nights: List<Night>) {
    val graves: List<Meal> = meals.filter { it.grave }

    fun meal(id: Long) = meals.firstOrNull { it.id == id }

    fun on(day: Long, zone: ZoneId) = meals.filter { it.at.day(zone) == day }

    fun satiety(day: Long, zone: ZoneId): Int = on(day, zone).sumOf { Rules.bite(it) }.coerceAtMost(100)

    fun newsEaten(day: Long, zone: ZoneId) = on(day, zone).count { it.food == Food.NEWS && it.eaten }

    /** The night under way, if the light is out. */
    val openNight: Night? get() = nights.lastOrNull { it.end == null }
    val lastNight: Night? get() = nights.filter { it.end != null }.maxByOrNull { it.end!! }

    /** No fear for days: the pet sleeps in the wardrobe until it is fed. */
    fun asleep(now: Long): Boolean {
        val last = meals.filter { it.eaten }.maxOfOrNull { it.at } ?: return false
        return now - last > Rules.SLEEP_DAYS * 86_400_000L
    }

    fun genres(): Map<Genre, Int> = graves.groupingBy { it.genre }.eachCount()

    /** Up to two kinds of horror fed often enough to grow on the pet, most fed first. */
    fun traits(): List<Genre> = genres().filter { it.key != Genre.OTHER && it.value >= Rules.TRAIT_AT }
        .entries.sortedByDescending { it.value }.take(2).map { it.key }

    /** Own fears of the day that haven't gone into a night since they were named. */
    fun ownUnwatched(day: Long, zone: ZoneId): List<String> =
        on(day, zone).filter { m -> m.food == Food.OWN && nights.none { n -> n.start >= m.at && m.text in n.fearList() } }.map { it.text }.distinct()
}
