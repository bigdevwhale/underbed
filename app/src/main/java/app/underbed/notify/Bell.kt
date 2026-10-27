package app.underbed.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.underbed.MainActivity
import app.underbed.R
import app.underbed.container
import app.underbed.domain.day
import app.underbed.localized
import app.underbed.ru
import app.underbed.ui.Words
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/** One quiet evening knock from under the bed: the watch, if it hasn't started tonight. */
object Bell {
    private const val CHANNEL = "watch"
    private const val WORK = "watch"
    const val OPEN = "open"

    fun channel(c: Context) {
        val ch = NotificationChannel(CHANNEL, c.localized().let { if (it.ru) "Стук из-под кровати" else "A knock from under the bed" }, NotificationManager.IMPORTANCE_DEFAULT)
        c.getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    /** The next time the clock reaches [hour] after [now]. */
    fun next(hour: Int, now: Long, zone: ZoneId): Long {
        val today = LocalDate.ofEpochDay(now.day(zone))
        val t = today.atTime(hour, 0).atZone(zone).toInstant().toEpochMilli()
        return if (t > now) t else today.plusDays(1).atTime(hour, 0).atZone(zone).toInstant().toEpochMilli()
    }

    suspend fun plan(c: Context) {
        val app = c.applicationContext
        val st = app.container.settings.now()
        val wm = WorkManager.getInstance(app)
        if (!st.onboarded || st.watchHour < 0) { wm.cancelUniqueWork(WORK); return }
        val now = System.currentTimeMillis()
        val req = OneTimeWorkRequestBuilder<BellWorker>().setInitialDelay(next(st.watchHour, now, ZoneId.systemDefault()) - now, TimeUnit.MILLISECONDS).build()
        wm.enqueueUniqueWork(WORK, ExistingWorkPolicy.REPLACE, req)
    }

    fun post(c: Context, title: String, text: String) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(c, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val pi = PendingIntent.getActivity(c, 1, Intent(c, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP).putExtra(OPEN, "watch"), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val n = NotificationCompat.Builder(c, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_eyes)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(c).notify(1, n)
    }
}

class BellWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val c = ctx.container
        val st = c.settings.now()
        val s = c.repo.refresh()
        val zone = ZoneId.systemDefault()
        val today = System.currentTimeMillis().day(zone)
        val started = s.nights.any { it.start.day(zone) == today }
        if (st.onboarded && !started) {
            val w = Words(ctx.localized().ru, st.gender)
            Bell.post(ctx, w.t("Тук-тук из-под кровати", "Knock-knock from under the bed"), w.t("${w.name(st.creature)} готов к дозору. Что пугает сегодня?", "${w.name(st.creature)} is ready for the watch. What frightens you tonight?"))
        }
        Bell.plan(ctx)
        return Result.success()
    }
}
