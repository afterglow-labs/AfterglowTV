package com.afterglowtv.app.ui.screens.epg

import com.google.common.truth.Truth.assertThat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Test

class GuideDateTimeTest {

    @Test
    fun `guide prime time anchor uses selected local day`() {
        val zoneId = ZoneId.of("America/New_York")
        val anchor = LocalDateTime.of(2026, 5, 2, 10, 0)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()

        val primeTime = guidePrimeTimeAnchor(anchor, EpgViewModel.PRIME_TIME_HOUR, zoneId)
        val localPrimeTime = Instant.ofEpochMilli(primeTime).atZone(zoneId).toLocalDateTime()

        assertThat(localPrimeTime).isEqualTo(LocalDateTime.of(2026, 5, 2, 20, 0))
    }

    @Test
    fun `jump guide anchor to day preserves local time across dst transition`() {
        val zoneId = ZoneId.of("America/New_York")
        val anchor = LocalDateTime.of(2026, 3, 7, 23, 30)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
        val targetDayStart = LocalDate.of(2026, 3, 8)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()

        val jumpedAnchor = jumpGuideAnchorToDay(anchor, targetDayStart, zoneId)
        val localJumpedAnchor = Instant.ofEpochMilli(jumpedAnchor).atZone(zoneId).toLocalDateTime()

        assertThat(localJumpedAnchor).isEqualTo(LocalDateTime.of(2026, 3, 8, 23, 30))
    }

    @Test
    fun `shift guide day start advances by local calendar day across dst`() {
        val zoneId = ZoneId.of("America/New_York")
        val dayStart = LocalDate.of(2026, 3, 8)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()

        val shiftedDayStart = shiftGuideDayStart(dayStart, 1, zoneId)
        val shiftedLocalDate = Instant.ofEpochMilli(shiftedDayStart).atZone(zoneId).toLocalDate()

        assertThat(shiftedLocalDate).isEqualTo(LocalDate.of(2026, 3, 9))
    }

    @Test
    fun `guide window start snaps to previous half hour after lookback`() {
        val zoneId = ZoneId.of("America/Chicago")
        val anchor = LocalDateTime.of(2026, 6, 7, 1, 14, 23)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()

        val windowStart = guideWindowStartForAnchor(anchor, zoneId = zoneId)
        val localWindowStart = Instant.ofEpochMilli(windowStart).atZone(zoneId).toLocalDateTime()

        assertThat(localWindowStart).isEqualTo(LocalDateTime.of(2026, 6, 7, 0, 0))
    }

    @Test
    fun `guide window start preserves clean half hour slots`() {
        val zoneId = ZoneId.of("America/Chicago")
        val anchor = LocalDateTime.of(2026, 6, 7, 1, 44, 59)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()

        val windowStart = guideWindowStartForAnchor(anchor, zoneId = zoneId)
        val localWindowStart = Instant.ofEpochMilli(windowStart).atZone(zoneId).toLocalDateTime()

        assertThat(localWindowStart).isEqualTo(LocalDateTime.of(2026, 6, 7, 0, 30))
    }
}
