package jp.awt.clock.data

data class Alarm(
    val id: Long = 0,
    val hour: Int,
    val minute: Int,
    val label: String = "",
    val enabled: Boolean = true,
    val repeatMask: Int = 0,
    val gradualVolume: Boolean = true,
    val vibrate: Boolean = true,
) {
    fun repeatsOn(dayIndex: Int): Boolean = repeatMask and (1 shl dayIndex) != 0

    val timeText: String
        get() = "%02d:%02d".format(hour, minute)

    val displayLabel: String
        get() = label.ifBlank { "アラーム" }
}

val dayLabels = listOf("月", "火", "水", "木", "金", "土", "日")

fun repeatSummary(mask: Int): String = when (mask) {
    0 -> "一度だけ"
    0b0011111 -> "平日"
    0b1100000 -> "週末"
    0b1111111 -> "毎日"
    else -> dayLabels.filterIndexed { index, _ -> mask and (1 shl index) != 0 }.joinToString("・")
}

