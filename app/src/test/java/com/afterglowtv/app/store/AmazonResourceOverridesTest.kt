package com.afterglowtv.app.store

import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Test

class AmazonResourceOverridesTest {
    @Test
    fun `amazon build description says FireOS`() {
        val amazonStrings = File("src/amazon/res/values/strings.xml").readText()

        assertThat(amazonStrings).contains(
            """<string name="settings_build_desc">AfterglowTV for FireOS</string>"""
        )
    }
}
