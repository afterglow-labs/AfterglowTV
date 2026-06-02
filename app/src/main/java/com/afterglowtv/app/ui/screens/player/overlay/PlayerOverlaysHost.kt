package com.afterglowtv.app.ui.screens.player.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type

/**
 * Drop-in AfterglowTV overlay host. Stacks (in Z order, low → high):
 *
 *  1. Optional 40% black scrim — dims the underlying player when [state.livePanelVisible].
 *  2. [LivePanelOverlay] — left-anchored two-column channel picker.
 *  3. [InfoOsdBottom]    — bottom-third now/next info OSD.
 *  4. [QuickSettingsPanel] — right-anchored quick-settings.
 *
 * The host is invisible (zero composition cost) when nothing is showing. The
 * D-pad dispatcher on the host's modifier maps:
 *
 *  - LEFT / CENTER → show live panel
 *  - UP / MENU / INFO → show info OSD
 *  - RIGHT → show quick-settings
 *  - BACK → dismiss the topmost visible overlay
 *
 * Wire from PlayerScreen with a single call:
 *
 *     PlayerOverlaysHost(
 *         state = playerOverlayState,
 *         actions = playerOverlayActions,
 *         modifier = Modifier.fillMaxSize(),
 *     )
 *
 * keeping all state and action wiring in the host's two data containers so
 * `PlayerScreen.kt` does not need to know about the four overlay composables
 * individually.
 */
data class PlayerOverlayState(
    val livePanelVisible: Boolean = false,
    val panelCategories: List<LivePanelCategory> = emptyList(),
    val selectedCategoryId: String? = null,
    val panelChannels: List<LivePanelChannelRowState> = emptyList(),
    val currentChannelIndex: Int? = null,
    val infoOsdVisible: Boolean = false,
    val infoOsd: InfoOsdState? = null,
    val quickSettingsVisible: Boolean = false,
    val quickSettings: List<QuickSettingItem> = emptyList(),
)

data class PlayerOverlayActions(
    val onCategorySelected: (LivePanelCategory) -> Unit = {},
    val onChannelSelected: (LivePanelChannelRowState) -> Unit = {},
    val onQuickSettingClick: (QuickSettingItem) -> Unit = {},
    val onShowLivePanel: () -> Unit = {},
    val onShowInfoOsd: () -> Unit = {},
    val onShowQuickSettings: () -> Unit = {},
    val onDismissLivePanel: () -> Unit = {},
    val onDismissInfoOsd: () -> Unit = {},
    val onDismissQuickSettings: () -> Unit = {},
)

@Composable
fun PlayerOverlaysHost(
    state: PlayerOverlayState,
    actions: PlayerOverlayActions,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionLeft, Key.DirectionCenter -> {
                        if (!state.livePanelVisible) {
                            actions.onShowLivePanel(); true
                        } else false
                    }
                    Key.DirectionUp, Key.Menu, Key.Info -> {
                        if (!state.infoOsdVisible) {
                            actions.onShowInfoOsd(); true
                        } else false
                    }
                    Key.DirectionRight -> {
                        if (!state.quickSettingsVisible) {
                            actions.onShowQuickSettings(); true
                        } else false
                    }
                    Key.Back -> when {
                        state.livePanelVisible -> { actions.onDismissLivePanel(); true }
                        state.quickSettingsVisible -> { actions.onDismissQuickSettings(); true }
                        state.infoOsdVisible -> { actions.onDismissInfoOsd(); true }
                        else -> false
                    }
                    else -> false
                }
            },
    ) {
        AnimatedVisibility(
            visible = state.livePanelVisible,
            enter = fadeIn(tween(160)),
            exit = fadeOut(tween(120)),
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)))
        }

        LivePanelOverlay(
            visible = state.livePanelVisible,
            categories = state.panelCategories,
            selectedCategoryId = state.selectedCategoryId,
            channels = state.panelChannels,
            currentChannelIndex = state.currentChannelIndex,
            onCategorySelected = actions.onCategorySelected,
            onChannelSelected = actions.onChannelSelected,
            onDismiss = actions.onDismissLivePanel,
        )

        InfoOsdBottom(
            state = state.infoOsd,
            visible = state.infoOsdVisible,
            onDismiss = actions.onDismissInfoOsd,
        )

        QuickSettingsPanel(
            visible = state.quickSettingsVisible,
            items = state.quickSettings,
            onItemClick = actions.onQuickSettingClick,
            onDismiss = actions.onDismissQuickSettings,
        )
    }
}
