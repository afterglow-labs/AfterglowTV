package com.afterglowtv.app.navigation

import com.afterglowtv.app.ui.model.adultGuideCategoryId
import com.afterglowtv.domain.model.Category
import com.afterglowtv.domain.model.ContentType
import com.afterglowtv.domain.model.VirtualCategoryIds
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AdultGuideNavigationTest {

    @Test
    fun `adult guide playback uses selected category when present`() {
        val category = Category(
            id = adultGuideCategoryId("blondes"),
            name = "Blondes",
            type = ContentType.LIVE,
            isVirtual = true,
            isAdult = true
        )

        assertThat(adultGuidePlaybackCategoryId(category)).isEqualTo(category.id)
    }

    @Test
    fun `adult guide playback falls back to all adult category`() {
        assertThat(adultGuidePlaybackCategoryId(null)).isEqualTo(VirtualCategoryIds.ADULT_GUIDE)
    }
}
