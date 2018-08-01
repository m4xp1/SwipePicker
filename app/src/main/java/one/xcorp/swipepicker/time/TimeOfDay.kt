package one.xcorp.swipepicker.time

data class TimeOfDay(val type: Type, val start: Long, val end: Long) {

    fun duration() = end - start

    enum class Type { DAWN, DAY, SUNSET, NIGHT }
}