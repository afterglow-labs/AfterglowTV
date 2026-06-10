package com.afterglowtv.app.ui.screens.epg

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EpgStateCache @Inject constructor() {
    @Volatile
    private var cached: EpgUiState? = null
    @Volatile
    private var cachedBaseSnapshot: GuideBaseSnapshot? = null

    fun snapshot(): EpgUiState? = cached
    fun baseSnapshot(): GuideBaseSnapshot? = cachedBaseSnapshot

    fun remember(state: EpgUiState) {
        cached = state.copy(
            isInitialLoading = false,
            isRefreshing = false,
            recordingMessage = null,
            pendingRecordingConflict = null,
            previewChannelId = null,
            previewPlayerEngine = null,
            isPreviewLoading = false,
            previewErrorMessage = null
        )
    }

    fun rememberBaseSnapshot(snapshot: GuideBaseSnapshot) {
        cachedBaseSnapshot = snapshot
    }

    fun clear() {
        cached = null
        cachedBaseSnapshot = null
    }
}
