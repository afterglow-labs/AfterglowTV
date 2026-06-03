package com.afterglowtv.app.naming

import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Test

class AdultSurfaceNamingContractTest {
    @Test
    fun `adult surfaces use adult naming instead of guide naming`() {
        val sourceRoots = listOf(
            File("src/main"),
            File("src/amazon"),
            File("../domain/src/main"),
            File("../data/src/main")
        )
        val forbiddenNames = listOf(
            "XXX" + " Guide",
            "XXX" + " VOD",
            "XXX" + "_GUIDE",
            "xxx" + "_vod",
            "Adult" + "Guide",
            "adult" + "Guide",
            "Adult" + "Vod" + "Guide",
            "canShow" + "Adult" + "Vod" + "Guide",
            "adult" + "_guide",
            "adult" + " guide",
            "Adult" + " guide",
            "Xtream" + " Codes"
        )

        val offenders = sourceRoots
            .flatMap { root ->
                root.walkTopDown()
                    .filter { file -> file.isFile && file.extension in setOf("kt", "xml") }
                    .toList()
            }
            .flatMap { file ->
                val text = file.readText()
                forbiddenNames
                    .filter(text::contains)
                    .map { forbidden -> "${file.relativeTo(File(".")).path}: $forbidden" }
            }

        assertThat(offenders).isEmpty()
    }

    @Test
    fun `resources expose adult and adult vod names`() {
        val mainStrings = File("src/main/res/values/strings.xml").readText()

        assertThat(mainStrings).contains("""<string name="nav_adult">Adult</string>""")
        assertThat(mainStrings).contains("""<string name="adult_vod_title">Adult VOD</string>""")
    }
}
