package com.afterglowtv.app.ui.screens.dashboard

import com.afterglowtv.domain.model.ActiveLiveSource
import com.afterglowtv.domain.model.ActiveLiveSourceOption
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DashboardStatePolicyTest {
    @Test
    fun `dashboard only shows loading while provider state is unsettled`() {
        val loadingState = DashboardUiState(provider = null, isLoading = true)
        val settledEmptyState = DashboardUiState(provider = null, isLoading = false)

        assertThat(shouldShowDashboardLoadingState(loadingState)).isTrue()
        assertThat(shouldShowDashboardLoadingState(settledEmptyState)).isFalse()
    }

    @Test
    fun `dashboard source rows keep active source visible and expose activation state`() {
        val playlist = ActiveLiveSource.ProviderSource(10L)
        val combined = ActiveLiveSource.CombinedM3uSource(20L)
        val unavailable = ActiveLiveSource.CombinedM3uSource(30L)

        val rows = buildDashboardSourceRows(
            activeSource = combined,
            options = listOf(
                ActiveLiveSourceOption(
                    source = playlist,
                    title = "Public Playlist",
                    subtitle = "M3U Provider",
                    isEnabled = true
                ),
                ActiveLiveSourceOption(
                    source = combined,
                    title = "Family Mix",
                    subtitle = "Combined M3U",
                    isEnabled = true
                ),
                ActiveLiveSourceOption(
                    source = unavailable,
                    title = "Empty Mix",
                    subtitle = "Combined M3U",
                    isEnabled = false
                )
            ),
            maxRows = 3
        )

        assertThat(rows.map { it.title }).containsExactly("Family Mix", "Public Playlist", "Empty Mix").inOrder()
        assertThat(rows.map { it.status }).containsExactly(
            DashboardSourceStatus.ACTIVE,
            DashboardSourceStatus.READY,
            DashboardSourceStatus.UNAVAILABLE
        ).inOrder()
        assertThat(rows.map { it.canActivate }).containsExactly(false, true, false).inOrder()
    }
}
