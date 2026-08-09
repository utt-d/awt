package jp.awt.clock.ui

import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimeInputValidationTest {
    @Test
    fun clockKeypadDisablesDigitsThatCannotFormAValidTime() {
        assertTrue(canAppendClockDigit("0730", digitEntryActive = false, digit = 2))
        assertFalse(canAppendClockDigit("0730", digitEntryActive = false, digit = 3))

        assertTrue(canAppendClockDigit("2", digitEntryActive = true, digit = 3))
        assertFalse(canAppendClockDigit("2", digitEntryActive = true, digit = 4))

        assertTrue(canAppendClockDigit("23", digitEntryActive = true, digit = 5))
        assertFalse(canAppendClockDigit("23", digitEntryActive = true, digit = 6))
        assertTrue(canAppendClockDigit("235", digitEntryActive = true, digit = 9))
    }

    @Test
    fun clockParserAcceptsOnlyCompleteCivilTimes() {
        assertEquals(23 to 59, parseClockDigits("2359"))
        assertEquals(0 to 0, parseClockDigits("0000"))
        assertNull(parseClockDigits("2400"))
        assertNull(parseClockDigits("1260"))
        assertNull(parseClockDigits("123"))
    }

    @Test
    fun durationKeypadLimitsMinuteAndSecondTens() {
        assertTrue(canAppendDurationDigit("99", digitEntryActive = true, digit = 5))
        assertFalse(canAppendDurationDigit("99", digitEntryActive = true, digit = 6))
        assertTrue(canAppendDurationDigit("995", digitEntryActive = true, digit = 9))

        assertTrue(canAppendDurationDigit("9959", digitEntryActive = true, digit = 5))
        assertFalse(canAppendDurationDigit("9959", digitEntryActive = true, digit = 6))
        assertTrue(canAppendDurationDigit("99595", digitEntryActive = true, digit = 9))
    }

    @Test
    fun durationParserAcceptsUpToNinetyNineHours() {
        assertEquals(Triple(99, 59, 59), parseDurationDigits("995959"))
        assertEquals(Triple(0, 0, 1), parseDurationDigits("000001"))
        assertNull(parseDurationDigits("006000"))
        assertNull(parseDurationDigits("000060"))
    }

    @Test
    fun morphingDigitMasksUseStableSevenSegmentGeometry() {
        assertEquals(0b1111111, digitSegmentMask('8'))
        assertEquals((1 shl 1) or (1 shl 2), digitSegmentMask('1'))
        assertEquals(1 shl 6, digitSegmentMask('–'))
        assertEquals(0, digitSegmentMask('x'))
    }

    @Test
    fun arabicNumeralTemplatesCoverEveryDigitInsideTheNormalizedCanvas() {
        ('0'..'9').forEach { digit ->
            val points = arabicDigitTemplate(digit)
            assertTrue("$digit must have a drawable path", points.size >= 6)
            assertTrue(
                "$digit must remain inside the normalized canvas",
                points.all { it.x in 0f..1f && it.y in 0f..1f },
            )
        }
    }

    @Test
    fun everyArabicNumeralContainsRealCurvatureRatherThanOnlyStraightSegments() {
        ('0'..'9').forEach { digit ->
            val points = arabicDigitTemplate(digit)
            val hasCurvedSamples = points.windowed(3).any { (a, b, c) ->
                val firstX = b.x - a.x
                val firstY = b.y - a.y
                val secondX = c.x - b.x
                val secondY = c.y - b.y
                abs(firstX * secondY - firstY * secondX) > 0.00001f
            }
            assertTrue("$digit must contain sampled Bézier curvature", hasCurvedSamples)
        }
    }

    @Test
    fun arabicNumeralsResampleToEqualPointCountsForContinuousMorphing() {
        val zero = resampleDigitPolyline(arabicDigitTemplate('0'))
        val eight = resampleDigitPolyline(arabicDigitTemplate('8'))
        assertEquals(56, zero.size)
        assertEquals(zero.size, eight.size)
        assertEquals(arabicDigitTemplate('0').first(), zero.first())
        assertEquals(arabicDigitTemplate('8').last(), eight.last())
    }

    @Test
    fun contourAlignmentPreservesWindingInsteadOfTwistingTheMorph() {
        val source = listOf(
            Offset(0.1f, 0.1f),
            Offset(0.9f, 0.1f),
            Offset(0.9f, 0.9f),
            Offset(0.1f, 0.9f),
        )
        val reversedTarget = listOf(
            Offset(0.2f, 0.8f),
            Offset(0.8f, 0.8f),
            Offset(0.8f, 0.2f),
            Offset(0.2f, 0.2f),
        )
        val aligned = alignedDigitContour(source, reversedTarget)

        assertTrue(digitContourArea(source) * digitContourArea(aligned) > 0f)
        assertEquals(source.size, aligned.size)
    }

    @Test
    fun appearanceProfilesUpdateOnlyTheSelectedScreen() {
        val original = AppearanceProfiles()
        val measureSettings = original.measure.copy(
            theme = AwtThemeId.Ember,
            customTextArgb = 0xFF44AAEE.toInt(),
            reduceMotion = true,
        )
        val updated = original.updated(AppearanceScreen.Measure, measureSettings)

        assertEquals(original.alarms, updated.alarms)
        assertEquals(original.clock, updated.clock)
        assertEquals(measureSettings, updated.measure)
    }

    @Test
    fun arabicFourKeepsAClosedCounterWideCrossbarAndDescendingStem() {
        val four = arabicDigitTemplate('4')
        val start = four.first()
        assertTrue(four.count { abs(it.x - start.x) < 0.01f && abs(it.y - start.y) < 0.01f } >= 2)
        assertTrue(four.any { it.x < 0.25f && it.y in 0.55f..0.70f })
        assertTrue(four.any { it.x > 0.78f && it.y in 0.55f..0.70f })
        assertTrue(four.any { it.x in 0.64f..0.76f && it.y > 0.92f })
    }

    @Test
    fun timerRingProgressIsClampedAndMonotonic() {
        assertEquals(0f, timerRingProgress(10_000L, 10_000L), 0.0001f)
        assertEquals(0.5f, timerRingProgress(5_000L, 10_000L), 0.0001f)
        assertEquals(1f, timerRingProgress(0L, 10_000L), 0.0001f)
        assertEquals(0f, timerRingProgress(1_000L, 0L), 0.0001f)
        assertEquals(1f, timerRingProgress(-500L, 10_000L), 0.0001f)
    }
}
