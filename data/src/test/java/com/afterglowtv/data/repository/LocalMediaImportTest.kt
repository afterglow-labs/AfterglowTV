package com.afterglowtv.data.repository

import com.afterglowtv.domain.model.LocalMediaKind
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LocalMediaImportTest {
    @Test
    fun cleansReleaseNoiseFromLocalMediaTitle() {
        val metadata = LocalMediaImport.describeFile(
            displayName = "Supernatural.S01E02.Wendigo.1080p.WEB-DL.x264.mkv",
            mimeType = "video/x-matroska"
        )

        assertThat(metadata.title).isEqualTo("Supernatural S01E02 Wendigo")
        assertThat(metadata.mediaKind).isEqualTo(LocalMediaKind.EPISODE)
        assertThat(metadata.seasonNumber).isEqualTo(1)
        assertThat(metadata.episodeNumber).isEqualTo(2)
    }

    @Test
    fun treatsCommonVideoExtensionsAsImportableWhenMimeTypeIsMissing() {
        assertThat(LocalMediaImport.isImportableVideo("Movie Name.mp4", null)).isTrue()
        assertThat(LocalMediaImport.isImportableVideo("playlist.m3u", null)).isFalse()
    }
}
