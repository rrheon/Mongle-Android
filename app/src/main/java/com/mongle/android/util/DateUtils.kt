package com.mongle.android.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Parse ISO 8601 date string from server (handles both with and without milliseconds) */
fun parseISO8601(dateStr: String?): Date {
    if (dateStr == null) return Date()
    val formats = arrayOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ssXXX"
    )
    for (fmt in formats) {
        try {
            val sdf = SimpleDateFormat(fmt, Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            return sdf.parse(dateStr) ?: continue
        } catch (_: Exception) { continue }
    }
    return Date()
}
