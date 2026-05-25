package com.afterglowtv.app.ui.screens.player

import com.google.common.truth.Truth.assertThat
import com.afterglowtv.domain.model.ContentType
import kotlinx.coroutines.test.runTest
import org.junit.Test

class PlayerZapActionsTest {

    @Test
    fun `appendNumericChannelDigit keeps up to six digits`() {
        var buffer = ""

        (1..6).forEach { digit ->
            buffer = appendNumericChannelDigit(buffer, digit)
        }

        assertThat(buffer).isEqualTo("123456")
    }

    @Test
    fun `appendNumericChannelDigit restarts after sixth digit`() {
        var buffer = ""

        (1..6).forEach { digit ->
            buffer = appendNumericChannelDigit(buffer, digit)
        }
        buffer = appendNumericChannelDigit(buffer, 7)

        assertThat(buffer).isEqualTo("7")
    }

    @Test
    fun `linear live channel zapping is allowed during catch up playback`() {
        assertThat(
            isLinearLiveChannelZapAllowed(
                currentContentType = ContentType.LIVE,
                isCatchUpPlayback = true,
                hasChannels = true
            )
        ).isTrue()
    }

    @Test
    fun `linear live channel zapping requires live content with channels`() {
        assertThat(
            isLinearLiveChannelZapAllowed(
                currentContentType = ContentType.LIVE,
                isCatchUpPlayback = false,
                hasChannels = true
            )
        ).isTrue()
        assertThat(
            isLinearLiveChannelZapAllowed(
                currentContentType = ContentType.MOVIE,
                isCatchUpPlayback = false,
                hasChannels = true
            )
        ).isFalse()
        assertThat(
            isLinearLiveChannelZapAllowed(
                currentContentType = ContentType.LIVE,
                isCatchUpPlayback = false,
                hasChannels = false
            )
        ).isFalse()
    }

    @Test
    fun `remote dpad up advances by default`() {
        assertThat(remoteDpadZapDelta(RemoteDpadVerticalDirection.UP, inverted = false)).isEqualTo(1)
    }

    @Test
    fun `remote dpad down goes backward by default`() {
        assertThat(remoteDpadZapDelta(RemoteDpadVerticalDirection.DOWN, inverted = false)).isEqualTo(-1)
    }

    @Test
    fun `remote dpad zapping can be inverted`() {
        assertThat(remoteDpadZapDelta(RemoteDpadVerticalDirection.UP, inverted = true)).isEqualTo(-1)
        assertThat(remoteDpadZapDelta(RemoteDpadVerticalDirection.DOWN, inverted = true)).isEqualTo(1)
    }

    @Test
    fun `withScopedScrubbingMode disables scrubbing after success`() = runTest {
        val states = mutableListOf<Boolean>()

        withScopedScrubbingMode(states::add) {
            "done"
        }

        assertThat(states).containsExactly(true, false).inOrder()
    }

    @Test
    fun `withScopedScrubbingMode disables scrubbing after failure`() = runTest {
        val states = mutableListOf<Boolean>()

        runCatching {
            withScopedScrubbingMode(states::add) {
                error("boom")
            }
        }

        assertThat(states).containsExactly(true, false).inOrder()
    }
}
