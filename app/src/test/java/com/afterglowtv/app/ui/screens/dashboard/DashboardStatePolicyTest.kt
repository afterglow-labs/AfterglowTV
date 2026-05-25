package com.afterglowtv.app.ui.screens.dashboard

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DashboardStatePolicyTest {
    @Test
    fun `empty provider prompt is hidden while dashboard is still loading`() {
        val loadingState = DashboardUiState(provider = null, isLoading = true)
        val settledEmptyState = DashboardUiState(provider = null, isLoading = false)

        assertThat(shouldShowDashboardLoadingState(loadingState)).isTrue()
        assertThat(shouldShowDashboardEmptyState(loadingState)).isFalse()
        assertThat(shouldShowDashboardLoadingState(settledEmptyState)).isFalse()
        assertThat(shouldShowDashboardEmptyState(settledEmptyState)).isTrue()
    }
}
