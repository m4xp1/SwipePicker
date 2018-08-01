package one.xcorp.swipepicker.time

import one.xcorp.swipepicker.time.TimeOfDay.Type.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class LocalTime {

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

    fun getTimeOfDay(dawnStart: Long, dawnDuration: Long,
                     sunsetStart: Long, sunsetDuration: Long): TimeOfDay {
        val dawnEnd = dawnStart + dawnDuration
        val sunsetEnd = sunsetStart + sunsetDuration

        return when (millis) {
            in dawnStart..dawnEnd -> TimeOfDay(DAWN, dawnStart, dawnEnd)
            in dawnEnd..sunsetStart -> TimeOfDay(DAY, dawnEnd, sunsetStart)
            in sunsetStart..sunsetEnd -> TimeOfDay(SUNSET, sunsetStart, sunsetEnd)
            else -> TimeOfDay(NIGHT, sunsetEnd, dawnStart)
        }
    }

    fun toString(pattern: String): String {
        timeFormat.applyPattern(pattern)
        return timeFormat.format(toDate())
    }
}