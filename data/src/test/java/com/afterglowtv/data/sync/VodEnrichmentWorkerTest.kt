package com.afterglowtv.data.sync

import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
import org.junit.Test

class VodEnrichmentWorkerTest {
    @Test
    fun `enrichment work without active providers succeeds without doing work`() {
        val decision = decideVodEnrichmentReadiness(
            hasProviders = false,
            isLowOnMemory = true
        )

        assertThat(decision).isEqualTo(VodEnrichmentReadiness.NO_WORK)
    }

    @Test
    fun `enrichment work defers while device is low on memory`() {
        val decision = decideVodEnrichmentReadiness(
            hasProviders = true,
            isLowOnMemory = true
        )

        assertThat(decision).isEqualTo(VodEnrichmentReadiness.DEFER_LOW_MEMORY)
    }

    @Test
    fun `targeted enrichment keeps caller supplied delay`() {
        val request = VodEnrichmentWorker.createProviderRequest(
            providerId = 42L,
            initialDelaySeconds = 7L
        )

        assertThat(request.workSpec.initialDelay).isEqualTo(TimeUnit.SECONDS.toMillis(7))
    }

    @Test
    fun `targeted adult enrichment carries adult flag and clamped candidate limit`() {
        val request = VodEnrichmentWorker.createProviderRequest(
            providerId = 42L,
            initialDelaySeconds = 0L,
            candidateLimit = 999,
            adultOnly = true
        )

        assertThat(request.workSpec.input.getLong("provider_id", -1L)).isEqualTo(42L)
        assertThat(request.workSpec.input.getBoolean("adult_only", false)).isTrue()
        assertThat(request.workSpec.input.getInt("candidate_limit", -1)).isEqualTo(240)
    }

    @Test
    fun `enrichment candidate limit is clamped to worker bounds`() {
        assertThat(normalizeVodEnrichmentCandidateLimit(0)).isEqualTo(12)
        assertThat(normalizeVodEnrichmentCandidateLimit(120)).isEqualTo(120)
        assertThat(normalizeVodEnrichmentCandidateLimit(999)).isEqualTo(240)
    }

    @Test
    fun `frame artwork extraction only runs when poster is missing and stream is usable`() {
        assertThat(shouldAttemptVodFrameArtwork(posterUrl = null, streamUrl = "https://example.com/movie.mp4")).isTrue()
        assertThat(shouldAttemptVodFrameArtwork(posterUrl = "", streamUrl = "http://example.com/movie.mkv")).isTrue()
        assertThat(shouldAttemptVodFrameArtwork(posterUrl = "https://image.example/poster.jpg", streamUrl = "https://example.com/movie.mp4")).isFalse()
        assertThat(shouldAttemptVodFrameArtwork(posterUrl = null, streamUrl = "")).isFalse()
        assertThat(shouldAttemptVodFrameArtwork(posterUrl = null, streamUrl = "content://local/movie.mp4")).isFalse()
    }
}
