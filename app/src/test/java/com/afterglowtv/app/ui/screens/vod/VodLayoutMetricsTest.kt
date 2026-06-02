package com.afterglowtv.app.ui.screens.vod

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class VodLayoutMetricsTest {
    @Test
    fun `vod shelf tiles stay compact`() {
        assertThat(VodLayoutMetrics.ShelfRowHeight.value).isAtMost(60f)
        assertThat(VodLayoutMetrics.ShelfHeaderWidth.value).isAtMost(92f)
        assertThat(VodLayoutMetrics.ShelfTileWidth.value).isAtMost(200f)
        assertThat(VodLayoutMetrics.TilePadding.value).isAtMost(5f)
    }

    @Test
    fun `vod grid tiles stay compact`() {
        assertThat(VodLayoutMetrics.GridTileHeight.value).isAtMost(60f)
        assertThat(VodLayoutMetrics.GridMinTileWidth.value).isAtMost(220f)
    }
}
