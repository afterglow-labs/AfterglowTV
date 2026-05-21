package com.afterglowtv.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LocalMediaScheduleTest {
    @Test
    fun pseudoLivePlaybackStartsAtElapsedProgramTime() {
        val program = localProgram(
            startTimeMs = TWO_PM_MS,
            endTimeMs = THREE_PM_MS,
            mediaDurationMs = ONE_HOUR_MS
        )

        val playback = LocalMediaPlaybackResolver.resolvePseudoLivePlayback(
            program = program,
            nowMs = TWO_PM_MS + 37 * MINUTE_MS
        )

        assertThat(playback?.startPositionMs).isEqualTo(37 * MINUTE_MS)
    }

    @Test
    fun pseudoLivePlaybackReturnsNullOutsideSlot() {
        val program = localProgram(
            startTimeMs = TWO_PM_MS,
            endTimeMs = THREE_PM_MS,
            mediaDurationMs = ONE_HOUR_MS
        )

        assertThat(
            LocalMediaPlaybackResolver.resolvePseudoLivePlayback(
                program = program,
                nowMs = TWO_PM_MS - 1
            )
        ).isNull()
        assertThat(
            LocalMediaPlaybackResolver.resolvePseudoLivePlayback(
                program = program,
                nowMs = THREE_PM_MS
            )
        ).isNull()
    }

    @Test
    fun pseudoLivePlaybackClampsToMediaDurationWhenSlotIsLongerThanFile() {
        val program = localProgram(
            startTimeMs = TWO_PM_MS,
            endTimeMs = THREE_PM_MS,
            mediaDurationMs = 22 * MINUTE_MS
        )

        val playback = LocalMediaPlaybackResolver.resolvePseudoLivePlayback(
            program = program,
            nowMs = TWO_PM_MS + 37 * MINUTE_MS
        )

        assertThat(playback?.startPositionMs).isEqualTo(22 * MINUTE_MS - 1_000L)
    }

    @Test
    fun sequentialSchedulerBuildsChannelSlotsFromLibraryOrder() {
        val items = listOf(
            localItem(id = 10, title = "Pilot", durationMs = 42 * MINUTE_MS),
            localItem(id = 11, title = "Wendigo", durationMs = 41 * MINUTE_MS)
        )

        val programs = LocalMediaScheduler.buildSequentialPrograms(
            channelId = 5,
            mediaItems = items,
            windowStartMs = TWO_PM_MS
        )

        assertThat(programs).hasSize(2)
        assertThat(programs[0].mediaItemId).isEqualTo(10)
        assertThat(programs[0].startTimeMs).isEqualTo(TWO_PM_MS)
        assertThat(programs[0].endTimeMs).isEqualTo(TWO_PM_MS + 42 * MINUTE_MS)
        assertThat(programs[1].mediaItemId).isEqualTo(11)
        assertThat(programs[1].startTimeMs).isEqualTo(TWO_PM_MS + 42 * MINUTE_MS)
        assertThat(programs[1].endTimeMs).isEqualTo(TWO_PM_MS + 83 * MINUTE_MS)
    }

    private fun localProgram(
        startTimeMs: Long,
        endTimeMs: Long,
        mediaDurationMs: Long
    ) = LocalMediaProgram(
        id = 1,
        channelId = 1,
        mediaItemId = 1,
        title = "Supernatural",
        startTimeMs = startTimeMs,
        endTimeMs = endTimeMs,
        mediaDurationMs = mediaDurationMs
    )

    private fun localItem(id: Long, title: String, durationMs: Long) = LocalMediaItem(
        id = id,
        libraryId = 1,
        uri = "content://media/$id",
        displayName = "$title.mkv",
        title = title,
        mediaKind = LocalMediaKind.EPISODE,
        durationMs = durationMs
    )

    private companion object {
        const val MINUTE_MS = 60_000L
        const val ONE_HOUR_MS = 60 * MINUTE_MS
        const val TWO_PM_MS = 14 * ONE_HOUR_MS
        const val THREE_PM_MS = 15 * ONE_HOUR_MS
    }
}
