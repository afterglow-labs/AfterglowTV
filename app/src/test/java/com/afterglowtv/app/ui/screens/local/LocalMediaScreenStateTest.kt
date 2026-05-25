package com.afterglowtv.app.ui.screens.local

import com.afterglowtv.domain.model.LocalMediaItem
import com.afterglowtv.domain.model.LocalMediaKind
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LocalMediaScreenStateTest {

    @Test
    fun `xxx section is hidden until developer mode is unlocked`() {
        assertThat(visibleLocalMediaSections(developerModeEnabled = false)).containsExactly(
            LocalMediaSection.ALL,
            LocalMediaSection.MOVIES,
            LocalMediaSection.TV_SHOWS,
            LocalMediaSection.OTHER
        ).inOrder()

        assertThat(visibleLocalMediaSections(developerModeEnabled = true)).contains(
            LocalMediaSection.XXX
        )
    }

    @Test
    fun `adult local media items are hidden until developer mode is unlocked`() {
        val normal = localItem(title = "Vacation", mediaKind = LocalMediaKind.UNKNOWN)
        val adult = localItem(title = "XXX Private Clip", mediaKind = LocalMediaKind.UNKNOWN)

        assertThat(
            visibleLocalMediaItems(
                items = listOf(normal, adult),
                section = LocalMediaSection.ALL,
                developerModeEnabled = false
            )
        ).containsExactly(normal)

        assertThat(
            visibleLocalMediaItems(
                items = listOf(normal, adult),
                section = LocalMediaSection.ALL,
                developerModeEnabled = true
            )
        ).containsExactly(normal, adult).inOrder()
    }

    private fun localItem(
        title: String,
        mediaKind: LocalMediaKind
    ): LocalMediaItem = LocalMediaItem(
        id = title.hashCode().toLong(),
        libraryId = 1L,
        uri = "content://local/$title",
        displayName = title,
        title = title,
        mediaKind = mediaKind
    )
}
