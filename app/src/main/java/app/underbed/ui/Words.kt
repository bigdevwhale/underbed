package app.underbed.ui

import app.underbed.data.Gender
import app.underbed.domain.Creature
import app.underbed.domain.Food
import app.underbed.domain.Genre
import app.underbed.domain.Spot
import java.time.LocalDate
import kotlin.math.abs

/** The words that come from data rather than resources, in both languages, and everything the pet says. */
class Words(val ru: Boolean, val gender: Gender?) {
    fun t(r: String, e: String) = if (ru) g(r) else e

    /** Picks the form in Russian {he|she} marks; without a known gender both show, as «испугался(ась)». */
    fun g(s: String): String = MARK.replace(s) { m ->
        val (he, she) = m.destructured
        when (gender) {
            Gender.HE -> he
            Gender.SHE -> she
            null -> if (she.startsWith(he)) "$he(${she.removePrefix(he)})" else if (he.isEmpty()) "($she)" else "$he/$she"
        }
    }

    fun plural(n: Int, one: String, few: String, many: String): String {
        val a = abs(n) % 100; val b = a % 10
        return when { a in 11..19 -> many; b == 1 -> one; b in 2..4 -> few; else -> many }
    }

    fun n(n: Int, one: String, few: String, many: String, en1: String, enN: String) = if (ru) "$n ${plural(n, one, few, many)}" else "$n ${if (n == 1) en1 else enN}"

    fun times(n: Int) = n(n, "раз", "раза", "раз", "time", "times")

    fun name(c: Creature) = when (c) {
        Creature.BUKA -> t("Бука", "Bogey")
        Creature.GHOST -> t("Призрак", "Ghost")
        Creature.ZOMBIE -> t("Зомби", "Zombie")
        Creature.VAMPIRE -> t("Вампир", "Vampire")
        Creature.WOLF -> t("Оборотень", "Werewolf")
        Creature.DOLL -> t("Кукла", "Doll")
        Creature.KIKI -> t("Кикимора", "Kikimora")
    }

    /** «от Буки», «от Вампира». */
    fun nameFrom(c: Creature) = if (!ru) name(c) else when (c) {
        Creature.BUKA -> "Буки"; Creature.GHOST -> "Призрака"; Creature.ZOMBIE -> "Зомби"; Creature.VAMPIRE -> "Вампира"
        Creature.WOLF -> "Оборотня"; Creature.DOLL -> "Куклы"; Creature.KIKI -> "Кикиморы"
    }

    fun who(c: Creature) = when (c) {
        Creature.BUKA -> t("классический подкроватник", "the classic one from under the bed")
        Creature.GHOST -> t("живёт в простыне", "lives in a bedsheet")
        Creature.ZOMBIE -> t("немного разваливается", "falls apart a little")
        Creature.VAMPIRE -> t("не отражается в зеркале", "has no reflection")
        Creature.WOLF -> t("воет на настоящую луну", "howls at the real moon")
        Creature.DOLL -> t("поворачивает голову, когда не смотришь", "turns its head when you look away")
        Creature.KIKI -> t("обрастает мхом", "grows moss")
    }

    fun diet(c: Creature) = when (c) {
        Creature.BUKA -> t("ест всё подряд", "eats anything")
        Creature.GHOST -> t("тишина, скрипы, готика", "silence, creaks, gothic")
        Creature.ZOMBIE -> t("боди-хоррор, врачи", "body horror, doctors")
        Creature.VAMPIRE -> t("готика, старые дома", "gothic, old houses")
        Creature.WOLF -> t("слэшеры, потеря контроля", "slashers, losing control")
        Creature.DOLL -> t("психологический ужас", "psychological horror")
        Creature.KIKI -> t("фолк-хоррор, лес, болото", "folk horror, woods, bogs")
    }

    fun genre(g: Genre) = when (g) {
        Genre.GOTHIC -> t("Готика", "Gothic")
        Genre.SLASHER -> t("Слэшер", "Slasher")
        Genre.FOLK -> t("Фолк", "Folk")
        Genre.COSMIC -> t("Чудища", "Monsters")
        Genre.JHORROR -> t("Японский", "J-horror")
        Genre.PSYCHO -> t("Психологический", "Psychological")
        Genre.BODY -> t("Боди-хоррор", "Body horror")
        Genre.OTHER -> t("Прочее", "Other")
    }

    /** «Растёт на готике», «grows on gothic». */
    fun genreOn(g: Genre) = when (g) {
        Genre.GOTHIC -> t("готике", "gothic")
        Genre.SLASHER -> t("слэшерах", "slashers")
        Genre.FOLK -> t("фолке", "folk horror")
        Genre.COSMIC -> t("чудищах", "monsters")
        Genre.JHORROR -> t("японском ужасе", "J-horror")
        Genre.PSYCHO -> t("психологическом ужасе", "psychological horror")
        Genre.BODY -> t("боди-хорроре", "body horror")
        Genre.OTHER -> t("всяком", "all sorts")
    }

    /** What grows on the pet from a kind of horror. */
    fun trait(g: Genre) = when (g) {
        Genre.GOTHIC -> t("цилиндр", "a top hat")
        Genre.SLASHER -> t("шрам от когтей", "a claw scar")
        Genre.FOLK -> t("рожки из веток", "twig antlers")
        Genre.COSMIC -> t("щупальца", "tentacles")
        Genre.JHORROR -> t("длинные мокрые волосы", "long wet hair")
        Genre.PSYCHO -> t("трещина", "a crack")
        Genre.BODY -> t("швы", "stitches")
        Genre.OTHER -> ""
    }

    fun food(f: Food) = when (f) {
        Food.FILM -> t("Фильм", "Film")
        Food.BOOK -> t("Книга", "Book")
        Food.NEWS -> t("Новость", "News")
        Food.OWN -> t("Свой страх", "My own fear")
        Food.CASE -> t("Случай", "Something that happened")
    }

    fun glyph(f: Food) = when (f) { Food.FILM -> "☠"; Food.BOOK -> "❦"; Food.NEWS -> "✉"; Food.OWN -> "☁"; Food.CASE -> "✧" }

    fun spot(s: Spot) = when (s) {
        Spot.NAPE -> t("затылок", "nape")
        Spot.BACK -> t("спина", "back")
        Spot.BELLY -> t("живот", "belly")
        Spot.HANDS -> t("руки", "hands")
        Spot.HEELS -> t("пятки", "heels")
        Spot.THROAT -> t("горло", "throat")
    }

    fun level(n: Int) = when (n) {
        1 -> t("чуть-чуть", "a little")
        2 -> t("мурашки", "goosebumps")
        3 -> t("волосы дыбом", "hair on end")
        4 -> t("не спал{|а} ночь", "a sleepless night")
        5 -> t("до сих пор боюсь", "still scared")
        else -> ""
    }

    fun satiety(n: Int) = when {
        n >= 95 -> t("объелся", "stuffed")
        n >= 70 -> t("сыт", "full")
        n >= 35 -> t("перекусил", "had a bite")
        n > 0 -> t("голоден", "hungry")
        else -> t("очень голоден", "starving")
    }

    // ——— what the pet says ———

    fun hello(c: Creature) = when (c) {
        Creature.BUKA -> t("Принёс{|ла} что-нибудь страшненькое?", "Brought me something scary?")
        Creature.GHOST -> t("…ш-ш-ш. Тихо. Я тут.", "…shhh. Quiet. I'm here.")
        Creature.ZOMBIE -> t("Мозги мне не нужны. Мне бы страху.", "I don't want brains. I want fear.")
        Creature.VAMPIRE -> t("Добрый вечер. Чем угощаете?", "Good evening. What are we having?")
        Creature.WOLF -> t("Р-р-р. Голодный я.", "Grrr. I'm hungry.")
        Creature.DOLL -> t("Поиграем? Расскажи что-нибудь страшное.", "Shall we play? Tell me something scary.")
        Creature.KIKI -> t("Чего притих{|ла}? Боишься чего?", "Why so quiet? Scared of something?")
    }

    fun poke(i: Int) = listOf(
        t("Щекотно.", "That tickles."),
        t("Не тыкай, я в темноте лучше выгляжу.", "Don't poke. I look better in the dark."),
        t("Я под кроватью живу, а не в музее.", "I live under a bed, not in a museum."),
        t("Мур… то есть р-р-р.", "Purr… I mean, grrr."),
        t("Ещё раз ткнёшь — схвачу за пятку.", "Poke me again and I'll grab your heel."),
        t("Я тут с тех пор, как ты был{|а} маленьк{им|ой}.", "I've been here since you were little."),
    )[Math.floorMod(i, 6)]

    fun hungry() = t("Есть хочу… Принеси хоть одну страшилку.", "I'm hungry… Bring me just one scary thing.")
    fun asleep() = t("Хр-р… Я заснул в шкафу. Покорми — проснусь.", "Zzz… I fell asleep in the wardrobe. Feed me and I'll wake.")
    fun stuffed() = t("Ох. Больше не лезет. Иди погуляй.", "Oof. Not another bite. Go for a walk.")
    fun ox() = t("Час Быка. Слушай: на чердаке никого нет. Почти.", "The witching hour. Listen: there's nobody in the attic. Almost.")
    fun fullMoon(c: Creature) = if (c == Creature.WOLF) t("Луна! А-у-у-у-у!", "The moon! Ah-woooo!") else t("Сегодня полнолуние. Оборотни не спят — и я тоже.", "Full moon tonight. Werewolves are up, and so am I.")
    fun halloween() = t("Сегодня мой праздник! Я схожу по домам и принесу тебе конфету.", "It's my holiday! I'll go door to door and bring you a sweet.")
    fun friday13() = t("Пятница, тринадцатое. Самый вкусный день в году.", "Friday the thirteenth. The tastiest day of the year.")

    fun fed(level: Int, love: Boolean, notScary: Boolean, book: Boolean): String {
        if (notScary) return t("Это разве страшно? Ну ладно, на десерт.", "Is that scary? Fine, for dessert.")
        val bite = when (level) {
            1 -> t("Хрум.", "Crunch.")
            2 -> t("Ням-ням.", "Yum.")
            3 -> t("Ммм, с хрустом.", "Mmm, crispy.")
            4 -> t("Сытно! Аж шерсть дыбом.", "Filling! My fur's on end.")
            else -> t("Вот это страх! Объедение.", "Now that's fear! Delicious.")
        }
        val tail = if (book) t(" Бумажный страх — самый долгоиграющий.", " Paper fear lasts the longest.") else t(" Закопаю на погосте.", " I'll bury it in the graveyard.")
        return (if (love) t("Моё любимое! ", "My favourite! ") else "") + bite + tail
    }

    fun news(eatenBefore: Int) = when (eatenBefore) {
        0 -> t("Хрусть. Солёненько.", "Crunch. Salty.")
        1 -> t("Ещё одна? Ну ладно… жирновато.", "Another one? Fine… a bit greasy.")
        else -> t("Ик. Изжога. Это последняя на сегодня.", "Hic. Heartburn. That's the last one today.")
    }

    fun refused() = t("Я сыт. Иди погуляй, я посторожу.", "I'm full. Go for a walk, I'll keep watch.")
    fun own() = t("Давай сюда. Заберу на ночь, утром вернёшь себе огрызок.", "Hand it over. I'll keep it for the night; in the morning you get the core back.")
    fun case() = t("Настоящий, живой страх. Свежачок!", "A real, live fear. Fresh!")

    fun shout(i: Int) = listOf(
        t("А-а-а!", "Aaah!"), t("Ой!", "Eek!"), t("Я не испугался, это ты!", "I wasn't scared, you were!"),
        t("Мама!", "Mummy!"), t("Ух-х…", "Whoa…"), t("Держи меня!", "Hold me!"),
    )[Math.floorMod(i, 6)]

    fun flavor(c: Creature) = when (c) {
        Creature.BUKA -> t("Хрустящий страх, беру добавки.", "Crunchy fear, I'll have seconds.")
        Creature.GHOST -> t("Я тоже прятался под простынёй.", "I hid under my sheet too.")
        Creature.ZOMBIE -> t("Мозги не тронуты, страх съеден.", "Brains untouched, fear eaten.")
        Creature.VAMPIRE -> t("Изысканно. Хотя крови маловато.", "Exquisite. A little short on blood.")
        Creature.WOLF -> t("Р-р-р! Ещё такой хочу.", "Grrr! I want another like it.")
        Creature.DOLL -> t("Мне понравилось, как они кричали.", "I liked the way they screamed.")
        Creature.KIKI -> t("Лес был ненастоящий, зато страх свежий.", "The woods were fake, but the fear was fresh.")
    }

    fun review(c: Creature, jumps: Int, top: Int?, love: Boolean): List<String> = listOf(
        if (jumps == 0) t("Ни разу не подпрыгнул{|а}! Ты сам{|а}, что ли, чудище?", "Not a single jump! Are you a monster yourself?")
        else t("Ты подпрыгнул{|а} ${times(jumps)}.", "You jumped ${times(jumps)}.") +
            (top?.let { t(" Сильнее всего на $it-й минуте, я чуть попкорн не уронил.", " Most of all at minute $it; I nearly dropped the popcorn.") } ?: ""),
        (if (love) t("Мой любимый жанр. ", "My favourite kind. ") else "") + flavor(c),
    )

    fun night(i: Int) = listOf(
        t("Я тут.", "I'm here."), t("Спи, я смотрю.", "Sleep, I'm watching."),
        t("Под кроватью чисто.", "All clear under the bed."), t("Это просто холодильник.", "It's just the fridge."),
        t("Шорох? Это я. Свой.", "A rustle? That's me. A friend."),
    )[Math.floorMod(i, 5)]

    fun ate(n: Int) = if (ru) "Ням. ${n(n, "страх", "страха", "страхов", "", "")}. Спи." else "Yum. $n ${if (n == 1) "fear" else "fears"}. Sleep."

    private val big = listOf(t("со шкаф", "a wardrobe"), t("с медведя", "a bear"), t("с тучу", "a storm cloud"), t("с дом", "a house"), t("со слона", "an elephant"), t("с трамвай", "a tram"))
    private val small = listOf(t("с горошину", "a pea"), t("с напёрсток", "a thimble"), t("с пуговицу", "a button"), t("с семечку", "a sunflower seed"), t("со спичечный коробок", "a matchbox"), t("с изюминку", "a raisin"))

    /** How a fear shrank overnight: the same fear always shrinks the same way. */
    fun stub(fear: String): String {
        val h = fear.hashCode()
        val b = big[Math.floorMod(h, big.size)]
        val s = small[Math.floorMod(h / 7, small.size)]
        return t("был размером $b, теперь $s", "it was the size of $b, now it's $s")
    }

    fun and(items: List<String>) = when (items.size) {
        0 -> ""
        1 -> items[0]
        else -> items.dropLast(1).joinToString(", ") + t(" и ", " and ") + items.last()
    }

    fun date(d: LocalDate) = if (ru) "${d.dayOfMonth} ${MONTHS_GEN[d.monthValue - 1]}" else "${MONTHS_EN[d.monthValue - 1]} ${d.dayOfMonth}"
    fun dateY(d: LocalDate) = if (ru) "${d.dayOfMonth} ${MONTHS_GEN[d.monthValue - 1]} ${d.year}" else "${MONTHS_EN[d.monthValue - 1]} ${d.dayOfMonth}, ${d.year}"

    fun moon(phase: Double): String = when {
        phase < .034 || phase > .966 -> t("новолуние", "new moon")
        phase > .466 && phase < .534 -> t("полнолуние", "full moon")
        abs(phase - .25) < .034 -> t("первая четверть", "first quarter")
        abs(phase - .75) < .034 -> t("последняя четверть", "last quarter")
        phase < .5 -> t("луна растёт", "waxing moon")
        else -> t("луна убывает", "waning moon")
    }

    private companion object {
        val MARK = Regex("\\{([^|{}]*)\\|([^|{}]*)\\}")
        val MONTHS_GEN = listOf("января", "февраля", "марта", "апреля", "мая", "июня", "июля", "августа", "сентября", "октября", "ноября", "декабря")
        val MONTHS_EN = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
    }
}
