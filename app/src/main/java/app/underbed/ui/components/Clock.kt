package app.underbed.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import app.underbed.BuildConfig
import kotlin.math.PI
import kotlin.math.sin

/** Seconds since the screen appeared, every frame: the candle, the breath, the blink. Still in screenshot builds. */
@Composable
fun rememberSeconds(): State<Float> {
    val t = remember { mutableFloatStateOf(if (BuildConfig.SHOTS) 2.2f else 0f) }
    if (!BuildConfig.SHOTS) LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (true) withFrameNanos { t.floatValue = (it - start) / 1e9f }
    }
    return t
}

/** 0 with the eyes open, up to 1 for a quick blink every few seconds. */
fun blinkAt(t: Float, every: Float = 5.5f): Float {
    val p = t % every
    val d = .22f
    return if (p > every - d) sin(((p - (every - d)) / d) * PI.toFloat()) else 0f
}
