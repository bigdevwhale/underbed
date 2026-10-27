package app.underbed.domain

import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import kotlin.math.cos

/** The real moon over the graveyard, and the nights that are special under the bed. */
object Moon {
    private const val SYNODIC = 29.530588853
    /** A known new moon: 6 January 2000, 18:14 UTC. */
    private const val NEW_2000 = 947182440000L

    /** Where the moon is in its month: 0 new, 0.5 full. */
    fun phase(at: Long): Double {
        var a = ((at - NEW_2000) / 86_400_000.0) % SYNODIC
        if (a < 0) a += SYNODIC
        return a / SYNODIC
    }

    /** The lit part of the disc, 0 to 1. */
    fun light(at: Long) = (1 - cos(2 * Math.PI * phase(at))) / 2

    fun full(at: Long) = phase(at).let { it > .466 && it < .534 }
    fun new(at: Long) = phase(at).let { it < .034 || it > .966 }

    /** 3 a.m., the hour of the ox. */
    fun oxHour(at: Long, zone: ZoneId) = Instant.ofEpochMilli(at).atZone(zone).hour == 3

    fun halloween(at: Long, zone: ZoneId) = Instant.ofEpochMilli(at).atZone(zone).let { it.monthValue == 10 && it.dayOfMonth == 31 }

    fun friday13(at: Long, zone: ZoneId) = Instant.ofEpochMilli(at).atZone(zone).let { it.dayOfMonth == 13 && it.dayOfWeek == DayOfWeek.FRIDAY }
}
