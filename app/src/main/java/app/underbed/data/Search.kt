package app.underbed.data

import app.underbed.domain.Food
import app.underbed.domain.Genre
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** A film or a book found online; [genre] is null when the catalogue says nothing frightening about it. */
data class Found(val title: String, val maker: String, val year: Int?, val genre: Genre?, val known: Boolean)

/**
 * Looking a title up in open catalogues, free and keyless. Films come from Wikidata, with their
 * genres. Books come from Fantlab for queries in Cyrillic (it knows Russian titles and translations)
 * and from Open Library for the rest, each filling in for the other, with Google Books when neither
 * finds anything. This is the only thing that goes online, and only while someone searches.
 */
object Search {
    private const val AGENT = "Underbed/1.0 (https://github.com/bigdevwhale/underbed)"

    private val FILMS = setOf("Q11424", "Q202866", "Q24869", "Q506240", "Q93204", "Q17517379", "Q226730")

    /** Kinds of Fantlab works that are books one reads, not poems, articles or reviews. */
    private val FANTLAB_BOOKS = setOf("novel", "story", "collection", "epic", "tale", "anthology", "graphicnovel", "comix", "shortstory", "novelette")

    /** Colouring books and notebooks that share a word with a horror title. */
    private val JUNK = listOf("coloring", "colouring", "notebook", "journal", "planner", "sticker", "activity book")

    /** Null when nothing could be reached at all. */
    suspend fun search(food: Food, query: String, ru: Boolean): List<Found>? = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.length < 2) return@withContext emptyList()
        when (food) {
            Food.FILM -> wikidata(q, ru)
            Food.BOOK -> books(q)
            else -> emptyList()
        }
    }

    private fun get(url: String): String? = try {
        val c = URL(url).openConnection() as HttpURLConnection
        c.setRequestProperty("User-Agent", AGENT)
        c.connectTimeout = 8_000
        c.readTimeout = 10_000
        try {
            if (c.responseCode in 200..299) c.inputStream.bufferedReader().readText() else null
        } finally {
            c.disconnect()
        }
    } catch (_: Exception) {
        null
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    private fun year(s: String?): Int? = s?.let { Regex("""(\d{4})""").find(it)?.value?.toIntOrNull() }

    private suspend fun books(q: String): List<Found>? = coroutineScope {
        val cyrillic = q.any { it in 'Ѐ'..'ӿ' }
        val fan = async { fantlab(q) }
        val open = async { openLibrary(q) }
        val first = if (cyrillic) fan.await() else open.await()
        val second = if (cyrillic) open.await() else fan.await()
        val found = (first.orEmpty() + second.orEmpty().take(if (cyrillic) 4 else 6))
            .filterNot { f -> JUNK.any { it in f.title.lowercase() } }
            .distinctBy { it.title.lowercase().trim() to it.maker.lowercase().trim() }
        if (found.isNotEmpty()) return@coroutineScope found
        val google = google(q)
        when {
            !google.isNullOrEmpty() -> google.filterNot { f -> JUNK.any { it in f.title.lowercase() } }
            first == null && second == null && google == null -> null
            else -> emptyList()
        }
    }

    private fun fantlab(q: String): List<Found>? {
        val body = get("https://api.fantlab.ru/search-works?q=${enc(q)}&page=1&onlymatches=1") ?: return null
        val arr = runCatching { JSONArray(body) }.getOrNull() ?: return null
        return (0 until arr.length()).map { arr.getJSONObject(it) }
            .filter { it.optString("name_eng") in FANTLAB_BOOKS }
            .sortedByDescending { if (it.optInt("markcount") > 0) 1 else 0 }
            .take(10)
            .map { w ->
                Found(
                    title = w.optString("rusname").ifBlank { w.optString("name") }.trim(),
                    maker = w.optString("all_autor_rusname").ifBlank { w.optString("all_autor_name") }.trim(),
                    year = w.optInt("year", 0).takeIf { it > 0 },
                    genre = null,
                    known = false,
                )
            }.filter { it.title.isNotBlank() }
    }

    private fun openLibrary(q: String): List<Found>? {
        val body = get("https://openlibrary.org/search.json?q=${enc(q)}&limit=12&fields=title,author_name,first_publish_year,subject") ?: return null
        val docs = runCatching { JSONObject(body).getJSONArray("docs") }.getOrNull() ?: return null
        return (0 until docs.length()).map { docs.getJSONObject(it) }.map { d ->
            val subjects = d.optJSONArray("subject")?.let { a -> (0 until minOf(a.length(), 60)).map { a.optString(it) } }.orEmpty()
            Found(
                title = d.optString("title"),
                maker = d.optJSONArray("author_name")?.optString(0).orEmpty(),
                year = d.optInt("first_publish_year", 0).takeIf { it > 0 },
                genre = Genre.read(subjects),
                known = subjects.isNotEmpty(),
            )
        }.filter { it.title.isNotBlank() }
    }

    private fun google(q: String): List<Found>? {
        val body = get("https://www.googleapis.com/books/v1/volumes?q=${enc(q)}&maxResults=12&printType=books") ?: return null
        val items = runCatching { JSONObject(body).optJSONArray("items") }.getOrNull() ?: return emptyList()
        return (0 until items.length()).map { items.getJSONObject(it).getJSONObject("volumeInfo") }.map { v ->
            val cats = v.optJSONArray("categories")?.let { a -> (0 until a.length()).map { a.optString(it) } }.orEmpty()
            Found(
                title = v.optString("title"),
                maker = v.optJSONArray("authors")?.optString(0).orEmpty(),
                year = year(v.optString("publishedDate")),
                genre = Genre.read(cats),
                known = cats.isNotEmpty(),
            )
        }.filter { it.title.isNotBlank() }
    }

    private fun wikidata(q: String, ru: Boolean): List<Found>? {
        val lang = if (ru) "ru" else "en"
        val body = get("https://www.wikidata.org/w/api.php?action=wbsearchentities&search=${enc(q)}&language=$lang&uselang=$lang&type=item&limit=20&format=json")
            ?: return null
        val ids = JSONObject(body).optJSONArray("search")?.let { a -> (0 until a.length()).map { a.getJSONObject(it).getString("id") } }.orEmpty()
        if (ids.isEmpty()) return emptyList()
        val ents = get("https://www.wikidata.org/w/api.php?action=wbgetentities&ids=${ids.joinToString("|")}&props=claims|labels&languages=ru|en&format=json")
            ?.let { JSONObject(it).optJSONObject("entities") } ?: return null
        class Raw(val label: String, val year: Int?, val maker: String?, val genres: List<String>)
        val raws = ids.mapNotNull { id ->
            val e = ents.optJSONObject(id) ?: return@mapNotNull null
            val claims = e.optJSONObject("claims") ?: return@mapNotNull null
            if (values(claims, "P31").none { it in FILMS }) return@mapNotNull null
            val labels = e.optJSONObject("labels")
            val label = labels?.optJSONObject(lang)?.optString("value") ?: labels?.optJSONObject("en")?.optString("value")
                ?: labels?.optJSONObject("ru")?.optString("value") ?: return@mapNotNull null
            val time = claims.optJSONArray("P577")?.let { first(it) }?.optString("time")
            Raw(label, year(time), values(claims, "P57").firstOrNull(), values(claims, "P136"))
        }.take(8)
        // Directors' names and genres' English words, in one more request.
        val more = (raws.mapNotNull { it.maker } + raws.flatMap { it.genres }).distinct().take(50)
        val names = if (more.isEmpty()) emptyMap() else get("https://www.wikidata.org/w/api.php?action=wbgetentities&ids=${more.joinToString("|")}&props=labels&languages=$lang|en&format=json")
            ?.let { JSONObject(it).optJSONObject("entities") }
            ?.let { es ->
                more.associateWith { id ->
                    val l = es.optJSONObject(id)?.optJSONObject("labels")
                    (l?.optJSONObject(lang) ?: l?.optJSONObject("en"))?.optString("value").orEmpty() to l?.optJSONObject("en")?.optString("value").orEmpty()
                }
            }.orEmpty()
        return raws.map { r ->
            Found(
                title = r.label,
                maker = r.maker?.let { names[it]?.first }.orEmpty(),
                year = r.year,
                genre = Genre.read(r.genres.mapNotNull { names[it]?.second }),
                known = r.genres.isNotEmpty(),
            )
        }
    }

    private fun first(claims: JSONArray): JSONObject? = claims.optJSONObject(0)?.optJSONObject("mainsnak")?.optJSONObject("datavalue")?.optJSONObject("value")

    private fun values(claims: JSONObject, prop: String): List<String> {
        val a = claims.optJSONArray(prop) ?: return emptyList()
        return (0 until a.length()).mapNotNull { a.optJSONObject(it)?.optJSONObject("mainsnak")?.optJSONObject("datavalue")?.optJSONObject("value")?.optString("id") }
    }
}
