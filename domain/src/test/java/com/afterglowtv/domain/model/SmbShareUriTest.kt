package com.afterglowtv.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SmbShareUriTest {
    @Test
    fun parseSmbUriWithNestedPath() {
        val reference = SmbShareUri.parse("smb://qnap.local/Multimedia/Movies/Action")

        assertThat(reference).isEqualTo(
            SmbShareReference(
                host = "qnap.local",
                port = 445,
                shareName = "Multimedia",
                path = "Movies/Action"
            )
        )
    }

    @Test
    fun parseUncPath() {
        val reference = SmbShareUri.parse("\\\\192.168.1.20\\Media\\TV Shows")

        assertThat(reference).isEqualTo(
            SmbShareReference(
                host = "192.168.1.20",
                port = 445,
                shareName = "Media",
                path = "TV Shows"
            )
        )
    }

    @Test
    fun buildCanonicalRootEncodesPathSegments() {
        val uri = SmbShareUri.build(
            host = "nas.local",
            port = 1445,
            shareName = "Media Share",
            path = "Movies/Sci Fi"
        )

        assertThat(uri).isEqualTo("smb://nas.local:1445/Media%20Share/Movies/Sci%20Fi")
        assertThat(SmbShareUri.parse(uri)).isEqualTo(
            SmbShareReference(
                host = "nas.local",
                port = 1445,
                shareName = "Media Share",
                path = "Movies/Sci Fi"
            )
        )
    }

    @Test
    fun fromConfigNormalizesBackslashPath() {
        val reference = SmbShareUri.fromConfig(
            SmbShareConfig(
                host = " qnap ",
                shareName = " Multimedia ",
                path = "\\Movies\\New"
            )
        )

        assertThat(reference).isEqualTo(
            SmbShareReference(
                host = "qnap",
                port = 445,
                shareName = "Multimedia",
                path = "Movies/New"
            )
        )
    }
}
