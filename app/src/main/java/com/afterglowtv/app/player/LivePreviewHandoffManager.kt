package com.afterglowtv.app.player

import com.afterglowtv.domain.model.Channel
import com.afterglowtv.domain.model.ContentType
import com.afterglowtv.domain.model.StreamInfo
import com.afterglowtv.player.PlayerEngine
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Singleton
class LivePreviewHandoffManager @Inject constructor() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var pendingReleaseJob: Job? = null
    private var session: LivePreviewHandoffSession? = null

    fun registerPreviewSession(
        channel: Channel,
        streamInfo: StreamInfo,
        engine: PlayerEngine
    ) {
        registerPreviewSession(
            contentId = channel.id,
            providerId = channel.providerId,
            contentType = ContentType.LIVE,
            streamInfo = streamInfo,
            engine = engine
        )
    }

    fun registerPreviewSession(
        contentId: Long,
        providerId: Long,
        contentType: ContentType,
        streamInfo: StreamInfo,
        engine: PlayerEngine
    ) {
        val previous = session
        pendingReleaseJob?.cancel()
        session = LivePreviewHandoffSession(
            engine = engine,
            contentId = contentId,
            providerId = providerId,
            contentType = contentType,
            streamInfo = streamInfo,
            pendingFullscreen = false
        )
        if (previous != null && previous.engine !== engine) {
            previous.engine.release()
        }
    }

    fun beginFullscreenHandoff(contentId: Long, engine: PlayerEngine?): Boolean {
        val current = session ?: return false
        if (engine == null || current.engine !== engine || current.contentId != contentId) return false
        pendingReleaseJob?.cancel()
        session = current.copy(
            pendingFullscreen = true,
            updatedAtMs = System.currentTimeMillis()
        )
        pendingReleaseJob = scope.launch {
            delay(PENDING_FULLSCREEN_TIMEOUT_MS)
            val stale = session
            if (stale?.pendingFullscreen == true) {
                session = null
                stale.engine.release()
            }
        }
        return true
    }

    fun consumeFullscreenHandoff(channelId: Long, providerId: Long?): LivePreviewHandoffSession? {
        return consumeFullscreenHandoff(
            contentId = channelId,
            providerId = providerId,
            contentType = ContentType.LIVE
        )
    }

    fun consumeFullscreenHandoff(
        contentId: Long,
        providerId: Long?,
        contentType: ContentType
    ): LivePreviewHandoffSession? {
        val current = session ?: return null
        if (!current.pendingFullscreen) return null
        if (current.contentId != contentId) return null
        if (providerId != null && providerId > 0L && current.providerId != providerId) return null
        if (current.contentType != contentType) return null
        pendingReleaseJob?.cancel()
        session = null
        return current
    }

    fun clear(engine: PlayerEngine?) {
        val current = session ?: return
        if (engine != null && current.engine !== engine) return
        pendingReleaseJob?.cancel()
        session = null
    }

    data class LivePreviewHandoffSession(
        val engine: PlayerEngine,
        val contentId: Long,
        val providerId: Long,
        val contentType: ContentType,
        val streamInfo: StreamInfo,
        val pendingFullscreen: Boolean,
        val updatedAtMs: Long = System.currentTimeMillis()
    )

    private companion object {
        const val PENDING_FULLSCREEN_TIMEOUT_MS = 15_000L
    }
}
