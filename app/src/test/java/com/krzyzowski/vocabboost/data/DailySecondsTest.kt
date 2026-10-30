package com.krzyzowski.vocabboost.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class DailySecondsTest {
    private val warsaw = ZoneId.of("Europe/Warsaw")
    private val nov2 = LocalDate.parse("2026-11-02")
    private val nov3 = LocalDate.parse("2026-11-03")

    @Test
    fun `time over local midnight is split between the two days and the new day starts from zero`() {
        // Warsaw is UTC+1 in November: local midnight is 23:00Z
        val before = DailySeconds.NONE.add(
            Instant.parse("2026-11-02T10:00:00Z"),
            Instant.parse("2026-11-02T10:00:20Z"),
            warsaw,
        )

        val after = before.add(Instant.parse("2026-11-02T22:59:30Z"), Instant.parse("2026-11-02T23:00:30Z"), warsaw)

        assertEquals(50, after.secondsOn(nov2))
        assertEquals(30, after.secondsOn(nov3))
        assertEquals(0, before.secondsOn(nov3))
    }

    @Test
    fun `a day is the local day, not the UTC day`() {
        // 23:30Z on 2 November is already 00:30 on 3 November in Warsaw
        val seconds = DailySeconds.NONE.add(
            Instant.parse("2026-11-02T23:30:00Z"),
            Instant.parse("2026-11-02T23:40:00Z"),
            warsaw,
        )

        assertEquals(0, seconds.secondsOn(nov2))
        assertEquals(600, seconds.secondsOn(nov3))
    }

    @Test
    fun `several short slices on one day add up`() {
        val start = Instant.parse("2026-11-02T10:00:00Z")

        val seconds = (0 until 3).fold(DailySeconds.NONE) { total, slice ->
            total.add(start.plusSeconds(slice * 15L), start.plusSeconds(slice * 15L + 15), warsaw)
        }

        assertEquals(45, seconds.secondsOn(nov2))
    }

    @Test
    fun `a clock moved backwards adds nothing`() {
        val noon = Instant.parse("2026-11-02T12:00:00Z")
        val before = DailySeconds.NONE.add(noon, noon.plusSeconds(60), warsaw)

        val backwards = before.add(noon.plusSeconds(60), noon.minusSeconds(3600), warsaw)
        val standingStill = before.add(noon, noon, warsaw)

        assertEquals(60, backwards.secondsOn(nov2))
        assertEquals(60, standingStill.secondsOn(nov2))
    }

    @Test
    fun `a flush that lands a few hundred milliseconds off still counts the nearest whole second`() {
        val start = Instant.parse("2026-11-02T10:00:00Z")

        val slightlyShort = DailySeconds.NONE.add(start, start.plusMillis(14_600), warsaw)
        val slightlyLong = DailySeconds.NONE.add(start, start.plusMillis(15_400), warsaw)

        assertEquals(15, slightlyShort.secondsOn(nov2))
        assertEquals(15, slightlyLong.secondsOn(nov2))
    }

    @Test
    fun `the day the clocks go forward holds 23 hours at most`() {
        val localMidnight = Instant.parse("2026-03-28T23:00:00Z")
        val nextLocalMidnight = Instant.parse("2026-03-29T22:00:00Z")

        val seconds = DailySeconds.NONE.add(localMidnight, nextLocalMidnight, warsaw)

        assertEquals(23 * 3600, seconds.secondsOn(LocalDate.parse("2026-03-29")))
        assertEquals(0, seconds.secondsOn(LocalDate.parse("2026-03-30")))
    }

    @Test
    fun `days older than a week are dropped and today and yesterday are kept`() {
        val old = DailySeconds.NONE.add(
            Instant.parse("2026-10-01T10:00:00Z"),
            Instant.parse("2026-10-01T10:01:00Z"),
            warsaw,
        )
        val yesterday = old.add(Instant.parse("2026-11-01T10:00:00Z"), Instant.parse("2026-11-01T10:02:00Z"), warsaw)

        val seconds = yesterday.add(
            Instant.parse("2026-11-02T10:00:00Z"),
            Instant.parse("2026-11-02T10:03:00Z"),
            warsaw,
        )

        assertEquals("2026-11-01=120,2026-11-02=180", seconds.encode())
    }

    @Test
    fun `saved time reads back and a malformed entry loses only itself`() {
        val seconds = DailySeconds.decode("2026-11-02=90,garbage,2026-11-03=abc,not-a-date=5,2026-11-04=30")

        assertEquals(90, seconds.secondsOn(nov2))
        assertEquals(0, seconds.secondsOn(nov3))
        assertEquals(30, seconds.secondsOn(LocalDate.parse("2026-11-04")))
        assertEquals("2026-11-02=90,2026-11-04=30", seconds.encode())
    }

    @Test
    fun `nothing saved reads as zero seconds on every day`() {
        val seconds = DailySeconds.decode("")

        assertEquals(0, seconds.secondsOn(nov2))
        assertEquals("", seconds.encode())
    }
}
