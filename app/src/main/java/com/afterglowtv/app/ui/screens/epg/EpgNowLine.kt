package com.afterglowtv.app.ui.screens.epg

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.afterglowtv.app.ui.design.AppColors
import com.afterglowtv.app.ui.design.LocalAppSpacing
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * 2 dp red vertical "now" line drawn over an EPG grid. Recomputes its X
 * position every 60 s. Matches AfterglowTV:
 *   - color #F44336 (Material Red 500)
 *   - stroke 2 dp
 *
 * @param windowStart  Instant aligned to the leading edge of the EPG grid.
 * @param slotsVisible Number of 30-minute slots spanned by the EPG grid.
 */
@Composable
fun EpgNowLine(
    windowStart: Instant,
    slotsVisible: Int,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalAppSpacing.current
    var nowMillis by remember { mutableLongStateOf(Instant.now().toEpochMilli()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            nowMillis = Instant.now().toEpochMilli()
        }
    }
    val totalMinutes = (slotsVisible.coerceAtLeast(1)) * 30
    val elapsedMinutes = ChronoUnit.MINUTES.between(windowStart, Instant.ofEpochMilli(nowMillis))
        .coerceAtLeast(0).coerceAtMost(totalMinutes.toLong())
    val fraction = elapsedMinutes.toFloat() / totalMinutes.toFloat()

    Canvas(
        modifier = modifier
            .fillMaxHeight()
            .width(spacing.epgSlotWidth * slotsVisible),
    ) {
        val x = size.width * fraction
        drawLine(
            color = AppColors.EpgNowLine,
            start = Offset(x = x, y = 0f),
            end = Offset(x = x, y = size.height),
            strokeWidth = spacing.nowLineWidth.toPx(),
        )
    }
}
