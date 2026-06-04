package com.afterglowtv.app.store

import com.google.common.truth.Truth.assertThat
import java.io.File
import java.time.Instant
import org.junit.Test

class BundledPublicGuideGeneratorTest {
    @Test
    fun `generator writes plausible rolling xmltv for bundled public playlist`() {
        val playlist = File("src/amazon/assets/public_sources/playlist_usa.m3u8")
        val guide = kotlin.io.path.createTempFile("afterglow-public-guide", ".xml").toFile()

        try {
            val channelCount = BundledPublicGuideGenerator.writeGuide(
                playlistFile = playlist,
                outputFile = guide,
                nowMs = Instant.parse("2026-06-04T12:00:00Z").toEpochMilli()
            )
            val xml = guide.readText()

            assertThat(channelCount).isAtLeast(10)
            assertThat(xml).contains("""<channel id="30ATVClassicMovies.us">""")
            assertThat(xml).contains("""<channel id="AccuWeatherNOW.us@SD">""")
            assertThat(xml).contains("""<programme start="202606""")
            assertThat(xml).contains("""<category>Movies</category>""")
            assertThat(xml).contains("""<category>Weather</category>""")
            assertThat(xml).contains("""<category>Public Access</category>""")
            assertThat(xml).contains("Classic")
            assertThat(xml).contains("Forecast")
        } finally {
            guide.delete()
        }
    }
}
