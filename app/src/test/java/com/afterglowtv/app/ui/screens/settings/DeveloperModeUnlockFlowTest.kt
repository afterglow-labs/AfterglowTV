package com.afterglowtv.app.ui.screens.settings

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DeveloperModeUnlockFlowTest {
    @Test
    fun `version row tap threshold opens developer dialog while developer mode is disabled`() {
        var result = DeveloperModeTapResult(tapCount = 0, showPasswordDialog = false)

        repeat(7) {
            result = nextDeveloperModeTapResult(
                developerModeEnabled = false,
                currentTapCount = result.tapCount
            )
        }

        assertThat(result).isEqualTo(
            DeveloperModeTapResult(
                tapCount = 0,
                showPasswordDialog = true,
                targetDeveloperModeEnabled = true
            )
        )
    }

    @Test
    fun `version row tap threshold opens developer dialog while developer mode is enabled`() {
        var result = DeveloperModeTapResult(tapCount = 0, showPasswordDialog = false)

        repeat(7) {
            result = nextDeveloperModeTapResult(
                developerModeEnabled = true,
                currentTapCount = result.tapCount
            )
        }

        assertThat(result).isEqualTo(
            DeveloperModeTapResult(
                tapCount = 0,
                showPasswordDialog = true,
                targetDeveloperModeEnabled = false
            )
        )
    }

    @Test
    fun `developer password toggles current developer mode state`() {
        assertThat(developerModeStateAfterValidPassword(currentDeveloperModeEnabled = false)).isTrue()
        assertThat(developerModeStateAfterValidPassword(currentDeveloperModeEnabled = true)).isFalse()
    }
}
