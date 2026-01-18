package com.asoft.artsal.photo.extensions

import android.text.format.DateUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

fun String.getStringDate(
    initialFormat: String,
    requiredFormat: String,
    locale: Locale = Locale.getDefault()
): String {
    return this.toDate(initialFormat, locale).toString(requiredFormat)
}

fun String.toDate(format: String, locale: Locale = Locale.getDefault()): Date {
    return try {
        val formatter = SimpleDateFormat(format, locale)
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        formatter.parse(this) ?: Date()
    } catch (ex: Exception) {
        Date()
    }
}

fun String.toTimeStamp(format: String, locale: Locale = Locale.getDefault()): Long? {
    return try {
        val formatter = SimpleDateFormat(format, locale)
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        (formatter.parse(this) ?: return 0L).time / 1000
    } catch (ex: Exception) {
        null
    }
}

fun Date.toTimeStamp(format: String, locale: Locale = Locale.getDefault()): Long? {
    return try {
        val dateStr = toString(format, locale)
        dateStr.toTimeStamp(format, locale)
    } catch (ex: Exception) {
        null
    }
}

fun Date.toString(format: String, locale: Locale = Locale.getDefault()): String {
    return try {
        val formatter = SimpleDateFormat(format, locale)
        formatter.format(this)
    } catch (ex: Exception) {
        ""
    }
}

//fun Long.toTimeString(format: String, locale: Locale = Locale.getDefault()): String {
//    return try {
//        val formatter = SimpleDateFormat(format, locale)
//        formatter.timeZone = TimeZone.getTimeZone(ZoneId.systemDefault())
//        return formatter.format(this * 1000)
//    } catch (ex: Exception) {
//        ""
//    }
//}

fun Long.toTimeDate(format: String, locale: Locale = Locale.getDefault()): Date {
    return try {
        val formatter = SimpleDateFormat(format, locale)
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        return formatter.format(this * 1000).toDate(format)
    } catch (ex: Exception) {
        Date()
    }
}
fun Long.toDateString(format: String = "dd/MM/yyyy"): String {
    return try {
        val date = Date(this)
        val sdf = SimpleDateFormat(format, Locale.getDefault())
        sdf.format(date)
    } catch (e: Exception) {
        "Invalid date"
    }
}



//fun Long.toTimeStampGMT(format: String, locale: Locale = Locale.getDefault()): String {
//    return try {
//        val formatter = SimpleDateFormat(format, locale)
//        formatter.timeZone = TimeZone.getTimeZone(ZoneId.systemDefault())
//        return formatter.format(this * 1000)
//    } catch (ex: Exception) {
//        ""
//    }
//}

fun isDateInCurrentWeek(date: Date?): Boolean {
    val currentCalendar = Calendar.getInstance()
    val week = currentCalendar[Calendar.WEEK_OF_YEAR]
    val year = currentCalendar[Calendar.YEAR]
    val targetCalendar = Calendar.getInstance()
    if (date != null) {
        targetCalendar.time = date
    }
    val targetWeek = targetCalendar[Calendar.WEEK_OF_YEAR]
    val targetYear = targetCalendar[Calendar.YEAR]
    return week == targetWeek && year == targetYear
}

fun isToday(whenInMillis: Long): Boolean {
    return DateUtils.isToday(whenInMillis)
}

fun Long.toTimeStampUTC(format: String, locale: Locale = Locale.getDefault()): String {
    return try {
        val formatter = SimpleDateFormat(format, locale)
        formatter.timeZone = TimeZone.getDefault()
        return formatter.format(this * 1000)
    } catch (ex: Exception) {
        ""
    }
}

