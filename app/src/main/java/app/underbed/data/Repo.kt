package app.underbed.data

import app.underbed.domain.Food
import app.underbed.domain.Genre
import app.underbed.domain.Meal
import app.underbed.domain.Night
import app.underbed.domain.Rules
import app.underbed.domain.Spot
import app.underbed.domain.State
import app.underbed.domain.day
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.ZoneId

/** What a feeding came to: eaten, or one news too many for the day. */
enum class Fed { EATEN, REFUSED }

/** The pet's diary in Room: every portion of fear, and the nights on watch. */
class Repo(db: BedDb) {
    private val dao = db.dao()
    private val mutex = Mutex()
    private val _state = MutableStateFlow<State?>(null)
    val state: StateFlow<State?> = _state

    /** The clock; the demo seed winds it back to replay weeks. */
    var clock: () -> Long = System::currentTimeMillis
    var zone: ZoneId = ZoneId.systemDefault()

    /** While true, changes aren't shown, so the demo seed doesn't flicker through its weeks. */
    @Volatile var hold = false

    suspend fun refresh(): State = State(dao.meals(), dao.nights()).also { if (!hold) _state.value = it }

    private suspend fun <T> op(block: suspend (State, Long) -> T): T = mutex.withLock {
        val s = refresh()
        block(s, clock()).also { refresh() }
    }

    /** Feeds the pet. News past the day's limit is written down but not eaten. */
    suspend fun feed(
        food: Food, text: String, level: Int, maker: String = "", year: Int? = null, genre: Genre = Genre.OTHER,
        spots: Set<Spot> = emptySet(), jumps: Int? = null, topMinute: Int? = null,
    ): Pair<Fed, Long> = op { s, now ->
        val full = food == Food.NEWS && s.newsEaten(now.day(zone), zone) >= Rules.NEWS_A_DAY
        val id = dao.insert(
            Meal(
                at = now, food = food, text = text.trim(), maker = maker.trim(), year = year, genre = genre,
                level = level.coerceIn(1, 5), spots = spots.joinToString(",") { it.name }, eaten = !full,
                jumps = jumps, topMinute = topMinute,
            ),
        )
        (if (full) Fed.REFUSED else Fed.EATEN) to id
    }

    suspend fun epitaph(id: Long, text: String) = op { s, _ -> s.meal(id)?.let { dao.update(it.copy(epitaph = text.take(140))) } }

    suspend fun regenre(id: Long, g: Genre) = op { s, _ -> s.meal(id)?.let { dao.update(it.copy(genre = g)) } }

    suspend fun dig(id: Long) = op { _, _ -> dao.deleteMeal(id) }

    /** Lights out: the fears named for tonight go under the bed. */
    suspend fun sleep(fears: List<String>): Long = op { _, now ->
        dao.insert(Night(start = now, fears = fears.map { it.trim() }.filter { it.isNotEmpty() }.distinct().joinToString("\n")))
    }

    suspend fun morning() = op { s, now -> s.openNight?.let { dao.update(it.copy(end = now)) } }
}
