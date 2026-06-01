package com.afterglowtv.app.player

import com.google.common.truth.Truth.assertThat
import com.afterglowtv.domain.model.Channel
import com.afterglowtv.domain.model.ContentType
import com.afterglowtv.domain.model.StreamInfo
import com.afterglowtv.player.PlayerEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain

@OptIn(ExperimentalCoroutinesApi::class)
class LivePreviewHandoffManagerTest {
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `matching pending preview can be consumed by fullscreen`() = runTest(testDispatcher) {
        val manager = LivePreviewHandoffManager()
        val engine = mock<PlayerEngine>()
        val channel = channel(id = 42L, providerId = 7L)
        val streamInfo = StreamInfo(url = "https://example.com/live.m3u8", title = channel.name)

        manager.registerPreviewSession(channel, streamInfo, engine)

        assertThat(manager.beginFullscreenHandoff(channel.id, engine)).isTrue()

        val session = manager.consumeFullscreenHandoff(channel.id, channel.providerId)

        assertThat(session).isNotNull()
        assertThat(session?.engine).isSameInstanceAs(engine)
        assertThat(session?.contentType).isEqualTo(ContentType.LIVE)
        assertThat(manager.consumeFullscreenHandoff(channel.id, channel.providerId)).isNull()
        verifyNoInteractions(engine)
    }

    @Test
    fun `movie preview only hands off to movie playback`() = runTest(testDispatcher) {
        val manager = LivePreviewHandoffManager()
        val engine = mock<PlayerEngine>()
        val movieId = 77L
        val providerId = 12L
        val streamInfo = StreamInfo(url = "https://example.com/movie.mp4", title = "Movie")

        manager.registerPreviewSession(
            contentId = movieId,
            providerId = providerId,
            contentType = ContentType.MOVIE,
            streamInfo = streamInfo,
            engine = engine
        )

        assertThat(manager.beginFullscreenHandoff(movieId, engine)).isTrue()
        assertThat(
            manager.consumeFullscreenHandoff(
                contentId = movieId,
                providerId = providerId,
                contentType = ContentType.LIVE
            )
        ).isNull()

        val session = manager.consumeFullscreenHandoff(
            contentId = movieId,
            providerId = providerId,
            contentType = ContentType.MOVIE
        )

        assertThat(session?.engine).isSameInstanceAs(engine)
        assertThat(session?.contentType).isEqualTo(ContentType.MOVIE)
        verifyNoInteractions(engine)
    }

    @Test
    fun `pending preview is released when fullscreen never claims it`() = runTest(testDispatcher) {
        val manager = LivePreviewHandoffManager()
        val engine = mock<PlayerEngine>()
        val channel = channel(id = 99L, providerId = 5L)
        val streamInfo = StreamInfo(url = "https://example.com/preview.ts", title = channel.name)

        manager.registerPreviewSession(channel, streamInfo, engine)
        assertThat(manager.beginFullscreenHandoff(channel.id, engine)).isTrue()

        advanceTimeBy(15_001L)
        testDispatcher.scheduler.runCurrent()

        assertThat(manager.consumeFullscreenHandoff(channel.id, channel.providerId)).isNull()
        verify(engine).release()
    }

    private fun channel(id: Long, providerId: Long): Channel = Channel(
        id = id,
        name = "Preview $id",
        streamUrl = "stream://$id",
        categoryId = 1L,
        providerId = providerId
    )
}
