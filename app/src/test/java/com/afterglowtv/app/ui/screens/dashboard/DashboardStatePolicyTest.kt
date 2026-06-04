package com.afterglowtv.app.ui.screens.dashboard

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
}
