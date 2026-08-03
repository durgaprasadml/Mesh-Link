package com.meshlink.ui.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe, cached date formatting utility to eliminate object allocations
 * (SimpleDateFormat, Date, Calendar) during Jetpack Compose composition and scrolling.
 */
object DateTimeUtils {

    private val threadLocalTimeFormatHHMM = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat {
            return SimpleDateFormat("HH:mm", Locale.getDefault())
        }
    }

    private val threadLocalTimeFormatHHMMSS = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat {
            return SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        }
    }

    private val threadLocalTimeFormat12Hour = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat {
            return SimpleDateFormat("h:mm a", Locale.getDefault())
        }
    }

    private val threadLocalDateFormatChatRow = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat {
            return SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
        }
    }

    private val threadLocalDateFormatMonthDay = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat {
            return SimpleDateFormat("MMMM d", Locale.getDefault())
        }
    }

    private val threadLocalDateFormatMonthDayYear = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat {
            return SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
        }
    }

    private val reusableDate = object : ThreadLocal<Date>() {
        override fun initialValue(): Date = Date()
    }

    private val formattedTimeCache = ConcurrentHashMap<Long, String>()
    private const val MAX_CACHE_SIZE = 1000

    fun formatTimeHHMM(timeInMillis: Long): String {
        if (timeInMillis <= 0L) return ""
        val cached = formattedTimeCache[timeInMillis]
        if (cached != null) return cached

        val date = reusableDate.get() ?: Date()
        date.time = timeInMillis
        val formatted = threadLocalTimeFormatHHMM.get()?.format(date) ?: ""
        
        if (formattedTimeCache.size < MAX_CACHE_SIZE) {
            formattedTimeCache[timeInMillis] = formatted
        }
        return formatted
    }

    fun formatTimeHHMMSS(timeInMillis: Long): String {
        if (timeInMillis <= 0L) return ""
        val date = reusableDate.get() ?: Date()
        date.time = timeInMillis
        return threadLocalTimeFormatHHMMSS.get()?.format(date) ?: ""
    }

    fun formatTime12Hour(timeInMillis: Long): String {
        if (timeInMillis <= 0L) return ""
        val date = reusableDate.get() ?: Date()
        date.time = timeInMillis
        return threadLocalTimeFormat12Hour.get()?.format(date) ?: ""
    }

    fun formatChatRowDate(timeInMillis: Long): String {
        if (timeInMillis <= 0L) return ""
        val date = reusableDate.get() ?: Date()
        date.time = timeInMillis
        return threadLocalDateFormatChatRow.get()?.format(date) ?: ""
    }

    fun formatDateSeparator(timeInMillis: Long): String {
        if (timeInMillis <= 0L) return ""
        val currentCalendar = Calendar.getInstance()
        currentCalendar.timeInMillis = timeInMillis

        val today = Calendar.getInstance()
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

        val isToday = currentCalendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                currentCalendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)

        if (isToday) return "Today"

        val isYesterday = currentCalendar.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) &&
                currentCalendar.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR)

        if (isYesterday) return "Yesterday"

        val date = reusableDate.get() ?: Date()
        date.time = timeInMillis

        return if (currentCalendar.get(Calendar.YEAR) == today.get(Calendar.YEAR)) {
            threadLocalDateFormatMonthDay.get()?.format(date) ?: ""
        } else {
            threadLocalDateFormatMonthDayYear.get()?.format(date) ?: ""
        }
    }

    fun shouldShowDateSeparator(currentTimestamp: Long, previousTimestamp: Long?): Boolean {
        if (previousTimestamp == null) return true

        val currentCalendar = Calendar.getInstance().apply { timeInMillis = currentTimestamp }
        val previousCalendar = Calendar.getInstance().apply { timeInMillis = previousTimestamp }

        return currentCalendar.get(Calendar.YEAR) != previousCalendar.get(Calendar.YEAR) ||
                currentCalendar.get(Calendar.DAY_OF_YEAR) != previousCalendar.get(Calendar.DAY_OF_YEAR)
    }
}
