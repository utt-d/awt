package jp.awt.clock.alarm

import jp.awt.clock.data.Alarm
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class NextAlarmCalculatorTest {
    private val zone = ZoneId.of("Asia/Tokyo")

    @Test
    fun oneShotTodayWhenTimeIsStillAhead() {
        val now = ZonedDateTime.of(2026, 7, 23, 7, 30, 0, 0, zone)
        val next = NextAlarmCalculator.next(Alarm(hour = 8, minute = 0), now)
        assertEquals(23, next.dayOfMonth)
        assertEquals(8, next.hour)
    }

    @Test
    fun oneShotMovesToTomorrowWhenTimePassed() {
        val now = ZonedDateTime.of(2026, 7, 23, 8, 1, 0, 0, zone)
        val next = NextAlarmCalculator.next(Alarm(hour = 8, minute = 0), now)
        assertEquals(24, next.dayOfMonth)
    }

    @Test
    fun repeatingAlarmUsesSelectedWeekday() {
        val thursday = ZonedDateTime.of(2026, 7, 23, 9, 0, 0, 0, zone)
        val mondayOnly = Alarm(hour = 8, minute = 0, repeatMask = 1)
        val next = NextAlarmCalculator.next(mondayOnly, thursday)
        assertEquals(27, next.dayOfMonth)
    }

    @Test
    fun sameMinuteDoesNotScheduleInThePast() {
        val exact = ZonedDateTime.of(2026, 7, 23, 8, 0, 0, 0, zone)
        val next = NextAlarmCalculator.next(Alarm(hour = 8, minute = 0), exact)
        assertEquals(24, next.dayOfMonth)
    }
}

