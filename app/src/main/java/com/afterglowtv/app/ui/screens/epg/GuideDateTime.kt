package com.afterglowtv.app.ui.screens.epg

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

internal fun startOfGuideDay(timestamp: Long, zoneId: ZoneId = ZoneId.systemDefault()): Long {
    val localDate = Instant.ofEpochMilli(timestamp).atZone(zoneId).toLocalDate()
    return localDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
}

internal fun shiftGuideDayStart(
    dayStartMillis: Long,
    days: Long,
    zoneId: ZoneId = ZoneId.systemDefault()
): Long {
    val localDate = Instant.ofEpochMilli(dayStartMillis).atZone(zoneId).toLocalDate().plusDays(days)
    return localDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
}

internal fun shiftGuideAnchorByDays(
    anchorTimeMillis: Long,
    days: Long,
    zoneId: ZoneId = ZoneId.systemDefault()
): Long = Instant.ofEpochMilli(anchorTimeMillis)
    .atZone(zoneId)
    .plusDays(days)
    .toInstant()
    .toEpochMilli()

internal fun guidePrimeTimeAnchor(
    anchorTimeMillis: Long,
    primeTimeHour: Int,
    zoneId: ZoneId = ZoneId.systemDefault()
): Long {
    val localDate = Instant.ofEpochMilli(anchorTimeMillis).atZone(zoneId).toLocalDate()
    return localDate.atTime(primeTimeHour, 0).atZone(zoneId).toInstant().toEpochMilli()
}

internal fun jumpGuideAnchorToDay(
    anchorTimeMillis: Long,
    dayStartMillis: Long,
    zoneId: ZoneId = ZoneId.systemDefault()
): Long {
    val anchorDateTime = Instant.ofEpochMilli(anchorTimeMillis).atZone(zoneId)
    val targetDate = Instant.ofEpochMilli(dayStartMillis).atZone(zoneId).toLocalDate()
    return targetDate.atTime(anchorDateTime.toLocalTime()).atZone(zoneId).toInstant().toEpochMilli()
}

internal fun guideWindowStartForAnchor(
    anchorTimeMillis: Long,
    lookbackMillis: Long = EpgViewModel.LOOKBACK_MS,
    slotMinutes: Int = 30,
    zoneId: ZoneId = ZoneId.systemDefault()
): Long {
    val rawStart = Instant.ofEpochMilli(anchorTimeMillis - lookbackMillis).atZone(zoneId)
    val snappedMinute = (rawStart.minute / slotMinutes) * slotMinutes
    return rawStart
        .withMinute(snappedMinute)
        .withSecond(0)
        .withNano(0)
        .toInstant()
        .toEpochMilli()
}

internal fun dayRelativeOffset(
    dayStartMillis: Long,
    today: LocalDate,
    zoneId: ZoneId = ZoneId.systemDefault()
): Long {
    val day = Instant.ofEpochMilli(dayStartMillis).atZone(zoneId).toLocalDate()
    return day.toEpochDay() - today.toEpochDay()
}
