package one.xcorp.swipepicker.time

import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class LocalTime {

    val millis: Long

    constructor() {
        val date = Calendar.getInstance()

        millis = TimeUnit.HOURS.toMillis(date.get(Calendar.HOUR_OF_DAY).toLong()) +
                TimeUnit.MINUTES.toMillis(date.get(Calendar.MINUTE).toLong()) +
                TimeUnit.SECONDS.toMillis(date.get(Calendar.SECOND).toLong()) +
                date.get(Calendar.MILLISECOND).toLong()
    }

    constructor(millis: Long) {
        this.millis = millis
    }

    constructor(hour: Int = 0, minute: Int = 0, second: Int = 0, millisecond: Int = 0) {
        millis = TimeUnit.HOURS.toMillis(hour.toLong()) +
                TimeUnit.MINUTES.toMillis(minute.toLong()) +
                TimeUnit.SECONDS.toMillis(second.toLong()) +
                millisecond.toLong()
    }

    fun toDate(): Date = Date(millis)

    fun toString(pattern: String): String {
        timeFormat.applyPattern(pattern)
        return timeFormat.format(toDate())
    }

    companion object {

        private val timeFormat = SimpleDateFormat("", Locale.US)
                .apply { timeZone = TimeZone.getTimeZone("UTC") }

        fun parse(value: String, pattern: String): LocalTime? {
            timeFormat.applyPattern(pattern)

            return try {
                LocalTime(timeFormat.parse(value).time)
            } catch (e: Exception) {
                null
            }
        }
    }
}