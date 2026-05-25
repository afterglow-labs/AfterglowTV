package com.afterglowtv.app.navigation

import com.afterglowtv.domain.model.Category
import com.afterglowtv.domain.model.VirtualCategoryIds

internal fun adultGuidePlaybackCategoryId(category: Category?): Long =
    category?.id ?: VirtualCategoryIds.ADULT_GUIDE
