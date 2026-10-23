package com.libreta.app.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object Fmt {
    val es: Locale = Locale("es", "AR")

    fun relative(ts: Long, now: Long = System.currentTimeMillis()): String {
        val d = now - ts
        return when {
            d < 60_000 && d >= 0 -> "ahora"
            d in 0 until 3_600_000 -> "hace ${d / 60_000} min"
            d in 0 until 86_400_000 && isSameDay(ts, now) -> "hoy " + time(ts)
            isYesterday(ts, now) -> "ayer " + time(ts)
            d in 0 until 7 * 86_400_000L -> SimpleDateFormat("EEEE", es).format(Date(ts))
            sameYear(ts, now) -> SimpleDateFormat("d MMM", es).format(Date(ts))
            else -> SimpleDateFormat("d MMM yyyy", es).format(Date(ts))
        }
    }

    fun due(ts: Long, now: Long = System.currentTimeMillis()): String {
        val t = time(ts)
        return when {
            isSameDay(ts, now) -> "Hoy $t"
            isYesterday(ts, now) -> "Ayer $t"
            isSameDay(ts, now + 86_400_000L) -> "Mañana $t"
            ts > now && ts - now < 6 * 86_400_000L -> SimpleDateFormat("EEE", es).format(Date(ts)).replaceFirstChar { it.uppercase() } + " " + t
            else -> SimpleDateFormat("d MMM, HH:mm", es).format(Date(ts))
        }
    }

    fun time(ts: Long): String = SimpleDateFormat("HH:mm", es).format(Date(ts))
    fun full(ts: Long): String = SimpleDateFormat("EEEE d 'de' MMMM yyyy, HH:mm", es).format(Date(ts)).replaceFirstChar { it.uppercase() }
    fun dayHeader(ts: Long): String = SimpleDateFormat("EEEE d 'de' MMMM", es).format(Date(ts)).replaceFirstChar { it.uppercase() }
    fun monthYear(ts: Long): String = SimpleDateFormat("MMMM yyyy", es).format(Date(ts)).replaceFirstChar { it.uppercase() }
    fun short(ts: Long): String = SimpleDateFormat("dd/MM/yy", es).format(Date(ts))

    fun isSameDay(a: Long, b: Long): Boolean {
        val ca = Calendar.getInstance().apply { timeInMillis = a }
        val cb = Calendar.getInstance().apply { timeInMillis = b }
        return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR) && ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR)
    }

    private fun isYesterday(ts: Long, now: Long) = isSameDay(ts, now - 86_400_000L)
    private fun sameYear(a: Long, b: Long): Boolean =
        Calendar.getInstance().apply { timeInMillis = a }.get(Calendar.YEAR) == Calendar.getInstance().apply { timeInMillis = b }.get(Calendar.YEAR)

    fun startOfDay(ts: Long): Long = Calendar.getInstance().apply {
        timeInMillis = ts; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    fun size(bytes: Long): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024f)
        else -> "%.1f MB".format(bytes / (1024f * 1024f))
    }

    fun duration(ms: Long): String {
        val s = ms / 1000
        return "%d:%02d".format(s / 60, s % 60)
    }

    fun greeting(): String {
        val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (h) { in 5..11 -> "Buen día"; in 12..19 -> "Buenas tardes"; else -> "Buenas noches" }
    }
}
