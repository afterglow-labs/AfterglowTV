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
    fun `vod shelf section labels do not waste row width`() {
        assertThat(VodLayoutMetrics.ShelfHeaderWidth.value).isAtMost(68f)
    }

    @Test
    fun `vod grid tiles stay compact`() {
        assertThat(VodLayoutMetrics.GridTileHeight.value).isAtMost(60f)
        assertThat(VodLayoutMetrics.GridMinTileWidth.value).isAtMost(220f)
    }

    @Test
    fun `vod preview belongs in a compact header slot`() {
        assertThat(VodLayoutMetrics.HeaderPreviewWidth.value).isAtMost(224f)
        assertThat(VodLayoutMetrics.HeaderPreviewHeight.value).isAtMost(96f)
    }

    @Test
    fun `vod controls header leaves room for content rows`() {
        assertThat(VodLayoutMetrics.ContentHeaderHeight.value).isAtMost(60f)
    }
}
