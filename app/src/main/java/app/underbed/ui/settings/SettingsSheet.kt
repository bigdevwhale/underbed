package app.underbed.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import app.underbed.BuildConfig
import app.underbed.container
import app.underbed.data.Gender
import app.underbed.data.Settings
import app.underbed.notify.Bell
import app.underbed.ui.Home
import app.underbed.ui.Words
import app.underbed.ui.components.Card
import app.underbed.ui.components.Chip
import app.underbed.ui.components.Flow
import app.underbed.ui.components.Gap
import app.underbed.ui.components.Sheet
import app.underbed.ui.onboarding.CreatureGrid
import app.underbed.ui.theme.C
import app.underbed.ui.theme.T

/** The StillCraft bot: deep links file a bug or an idea for this app, or follow its releases. */
object Community {
    private const val BOT = "https://t.me/ArcaneWhisperBot?start="
    const val SLUG = "underbed"
    const val BUG = "${BOT}bug_$SLUG"
    const val IDEA = "${BOT}wish_$SLUG"
    const val FOLLOW = "${BOT}f_$SLUG"
    const val CHAT = "https://t.me/+Q4gFEDk8O842OGVi"
}

@Composable
fun SettingsSheet(h: Home, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val c = ctx.container
    val w = h.w
    val st = h.settings
    fun save(f: (Settings) -> Settings) = c.write { settings.update(f); Bell.plan(ctx) }

    Sheet(w.t("Настройки", "Settings"), onClose, w.t("Закрыть", "Close")) {
        Card {
            Text(w.t("Язык", "Language"), style = T.Heading)
            Flow(Modifier.padding(top = 8.dp)) {
                Chip("Русский", h.ru) { AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("ru")) }
                Chip("English", !h.ru) { AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en")) }
            }
            if (h.ru) {
                Text("Как к тебе обращаться", style = T.BodyB, modifier = Modifier.padding(top = 12.dp))
                Flow(Modifier.padding(top = 6.dp)) {
                    Chip("испугался", st.gender == Gender.HE) { save { it.copy(gender = Gender.HE) } }
                    Chip("испугалась", st.gender == Gender.SHE) { save { it.copy(gender = Gender.SHE) } }
                }
            }
        }
        Gap(10)
        Card {
            Text(w.t("Кто под кроватью", "Who's under the bed"), style = T.Heading)
            Text(w.t("Можно переселить другого. Могилы и всё съеденное останутся.", "You can move someone else in. The graves and everything eaten stay."), style = T.Small)
            Box(Modifier.padding(top = 10.dp).clip(RoundedCornerShape(10.dp)).background(C.night).padding(8.dp)) {
                CreatureGrid(w, st.creature) { cr -> save { it.copy(creature = cr) } }
            }
        }
        Gap(10)
        Card {
            Text(w.t("Вечерний дозор", "The evening watch"), style = T.Heading)
            Text(w.t("Одно тихое напоминание: что пугает сегодня? Только если дозор ещё не начат.", "One quiet reminder: what frightens you tonight? Only if the watch hasn't started."), style = T.Small)
            Flow(Modifier.padding(top = 8.dp)) {
                for (hr in listOf(21, 22, 23)) Chip("$hr:00", st.watchHour == hr) { save { it.copy(watchHour = hr) } }
                Chip(w.t("не напоминать", "no reminder"), st.watchHour < 0) { save { it.copy(watchHour = -1) } }
            }
        }
        Gap(10)
        Card(onClick = { c.write { settings.update { it.copy(onboarded = false) } }; onClose() }) {
            Text(w.t("Как это устроено", "How it works"), style = T.Heading)
            Text(w.t("Заглянуть под кровать ещё раз ›", "Look under the bed again ›"), style = T.Small.copy(color = C.wax))
        }
        Gap(10)
        CommunityLinks(w)
        Gap(10)
        Card {
            Text(w.t("О «Буке»", "About Underbed"), style = T.Heading)
            Text(w.t("Питомец, который ест страх. Ужастики и книги ужасов — его еда, тревожные новости — фастфуд, а свои страхи он сторожит по ночам. Интернет нужен только для поиска фильмов и книг: Wikidata, Фантлаб, Open Library. Без рекламы и без аккаунта; всё хранится на телефоне.",
                "A pet that eats fear. Horror films and books are its food, anxious news is fast food, and your own fears it guards at night. The internet is used only to look up films and books: Wikidata, Fantlab, Open Library. No ads, no account; everything stays on the phone."), style = T.Small)
            Text(w.t("Версия ${BuildConfig.VERSION_NAME} · шрифты Old Standard TT и Neucha (OFL) · исходники на GitHub", "Version ${BuildConfig.VERSION_NAME} · Old Standard TT and Neucha fonts (OFL) · source on GitHub"),
                style = T.Small.copy(color = C.wax),
                modifier = Modifier.padding(top = 8.dp).clickable { runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/bigdevwhale/underbed"))) } })
        }
        Gap(20)
    }
}

/** Report a bug, suggest an idea, follow updates, join the chat; each opens Telegram. */
@Composable
private fun CommunityLinks(w: Words) {
    val ctx = LocalContext.current
    fun open(url: String) { runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
    Card {
        Text(w.t("Сообщество", "Community"), style = T.Heading)
        Text(w.t("Приложение делается вместе с теми, кто им пользуется. Всё идёт через бота StillCraft в Telegram.", "The app is made together with the people who use it. It all goes through the StillCraft bot on Telegram."),
            style = T.Small, modifier = Modifier.padding(bottom = 4.dp))
        for ((icon, label, sub, url) in listOf(
            Link("🐞", w.t("Сообщить о баге", "Report a bug"), w.t("что сломалось и как повторить", "what broke and how to repeat it"), Community.BUG),
            Link("💡", w.t("Предложить идею", "Suggest an idea"), w.t("чудище, страх, функция", "a creature, a fear, a feature"), Community.IDEA),
            Link("🔔", w.t("Подписаться на обновления", "Follow updates"), w.t("бот напишет, когда выйдет версия", "the bot writes when a version is out"), Community.FOLLOW),
            Link("💬", w.t("Чат StillCraft", "StillCraft chat"), w.t("все приложения и люди", "all the apps and people"), Community.CHAT),
        )) {
            Row(Modifier.fillMaxWidth().clickable { open(url) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(icon, style = T.Heading, modifier = Modifier.width(34.dp))
                Column(Modifier.weight(1f)) {
                    Text(label, style = T.BodyB)
                    Text(sub, style = T.Small)
                }
                Text("›", style = T.Heading.copy(color = C.wax))
            }
        }
    }
}

private data class Link(val icon: String, val label: String, val sub: String, val url: String)
