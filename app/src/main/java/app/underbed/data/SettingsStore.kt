package app.underbed.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.underbed.domain.Creature
import app.underbed.domain.Genre
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")

enum class Gender { HE, SHE }

/** A film night under way: what's on, since when, and the minutes the person jumped. */
data class Show(val title: String, val maker: String, val year: Int?, val genre: Genre, val start: Long, val jumps: List<Int>)

data class Settings(
    val onboarded: Boolean = false,
    val creature: Creature = Creature.BUKA,
    /** How Russian words address the person: «испугался» or «испугалась». */
    val gender: Gender? = null,
    /** The evening reminder of the night watch; -1 for none. */
    val watchHour: Int = 22,
    /** The last night whose morning report has been read. */
    val readNight: Long = 0,
    val show: Show? = null,
)

class SettingsStore(context: Context) {
    private val store = context.applicationContext.dataStore

    val settings: Flow<Settings> = store.data.map { it.read() }

    private fun Preferences.read() = Settings(
        onboarded = this[ONBOARDED] ?: false,
        creature = Creature.entries.firstOrNull { it.name == this[CREATURE] } ?: Creature.BUKA,
        gender = Gender.entries.firstOrNull { it.name == this[GENDER] },
        watchHour = this[WATCH_HOUR] ?: 22,
        readNight = this[READ_NIGHT] ?: 0,
        show = this[SHOW_TITLE]?.takeIf { it.isNotEmpty() }?.let { t ->
            Show(
                t, this[SHOW_MAKER] ?: "", this[SHOW_YEAR]?.takeIf { it > 0 }, Genre.of(this[SHOW_GENRE] ?: ""), this[SHOW_START] ?: 0,
                (this[SHOW_JUMPS] ?: "").split(',').mapNotNull { it.toIntOrNull() },
            )
        },
    )

    suspend fun now(): Settings = settings.first()

    suspend fun update(change: (Settings) -> Settings) {
        store.edit {
            val s = change(it.read())
            it[ONBOARDED] = s.onboarded
            it[CREATURE] = s.creature.name
            it[GENDER] = s.gender?.name ?: ""
            it[WATCH_HOUR] = s.watchHour
            it[READ_NIGHT] = s.readNight
            val sh = s.show
            it[SHOW_TITLE] = sh?.title ?: ""
            it[SHOW_MAKER] = sh?.maker ?: ""
            it[SHOW_YEAR] = sh?.year ?: 0
            it[SHOW_GENRE] = sh?.genre?.name ?: ""
            it[SHOW_START] = sh?.start ?: 0
            it[SHOW_JUMPS] = sh?.jumps?.joinToString(",") ?: ""
        }
    }

    private companion object {
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val CREATURE = stringPreferencesKey("creature")
        val GENDER = stringPreferencesKey("gender")
        val WATCH_HOUR = intPreferencesKey("watch_hour")
        val READ_NIGHT = longPreferencesKey("read_night")
        val SHOW_TITLE = stringPreferencesKey("show_title")
        val SHOW_MAKER = stringPreferencesKey("show_maker")
        val SHOW_YEAR = intPreferencesKey("show_year")
        val SHOW_GENRE = stringPreferencesKey("show_genre")
        val SHOW_START = longPreferencesKey("show_start")
        val SHOW_JUMPS = stringPreferencesKey("show_jumps")
    }
}
