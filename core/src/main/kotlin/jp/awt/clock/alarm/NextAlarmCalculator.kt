package jp.awt.clock.alarm

import jp.awt.clock.data.Alarm
import java.time.LocalTime
import java.time.ZonedDateTime

object NextAlarmCalculator {
    fun next(alarm: Alarm, now: ZonedDateTime = ZonedDateTime.now()): ZonedDateTime {
        for (offset in 0..7) {
            val date = now.toLocalDate().plusDays(offset.toLong())
            if (alarm.repeatMask != 0) {
                val bit = date.dayOfWeek.value - 1
                if (!alarm.repeatsOn(bit)) continue
            }

            val candidate = ZonedDateTime.of(
                date,
                LocalTime.of(alarm.hour, alarm.minute),
                now.zone,
            )
            if (candidate.isAfter(now)) return candidate
        }

        // A valid repeating mask always resolves above. This also safely handles a corrupt mask.
        return ZonedDateTime.of(
            now.toLocalDate().plusDays(1),
            LocalTime.of(alarm.hour, alarm.minute),
            now.zone,
        )
    }
}

