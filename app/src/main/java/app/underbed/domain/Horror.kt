package app.underbed.domain

import java.io.InputStream

/** A known horror film or book from the bundled catalogue. */
class Title(val food: Food, val genre: Genre, val year: Int, val ru: String, val en: String, val makerRu: String, val makerEn: String) {
    fun name(ru: Boolean) = if (ru) this.ru else en
    fun maker(ru: Boolean) = if (ru) makerRu else makerEn
}

/**
 * The bundled catalogue (assets/horror.tsv): what comes up at once while typing, and all there is
 * without the internet.
 */
class Horror(val titles: List<Title>) {
    /** Titles whose name or maker starts a word with the query, closest first. */
    fun find(query: String, food: Food, limit: Int = 8): List<Title> {
        val q = norm(query)
        if (q.length < 2) return emptyList()
        return titles.asSequence().filter { it.food == food }.mapNotNull { t ->
            val names = listOf(norm(t.ru), norm(t.en))
            val score = when {
                names.any { it == q } -> 0
                names.any { it.startsWith(q) } -> 1
                names.any { n -> n.split(' ').any { it.startsWith(q) } } -> 2
                listOf(norm(t.makerRu), norm(t.makerEn)).any { m -> m.split(' ').any { it.startsWith(q) } } -> 3
                else -> null
            }
            score?.let { it to t }
        }.sortedWith(compareBy({ it.first }, { -it.second.year })).take(limit).map { it.second }.toList()
    }

    /** The catalogue's entry for a title typed out in full, in either language. */
    fun match(text: String, food: Food): Title? {
        val q = norm(text)
        return titles.firstOrNull { it.food == food && (norm(it.ru) == q || norm(it.en) == q) }
    }

    companion object {
        fun norm(s: String) = s.lowercase().replace('ё', 'е').replace(Regex("""[^\p{L}\p{N}]+"""), " ").trim()

        fun load(open: () -> InputStream?): Horror {
            val lines = open()?.bufferedReader(Charsets.UTF_8)?.use { it.readLines() }.orEmpty()
            val titles = lines.drop(1).mapNotNull { line ->
                val f = line.split('\t')
                if (f.size < 7) return@mapNotNull null
                val food = when (f[0].trim()) { "film" -> Food.FILM; "book" -> Food.BOOK; else -> return@mapNotNull null }
                Title(food, Genre.of(f[1]), f[2].trim().toIntOrNull() ?: 0, f[3].trim(), f[4].trim(), f[5].trim(), f[6].trim())
            }
            return Horror(titles)
        }
    }
}
