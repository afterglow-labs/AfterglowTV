package com.afterglowtv.app.navigation

import com.afterglowtv.domain.model.Category
import com.afterglowtv.domain.model.VirtualCategoryIds

internal fun adultPlaybackCategoryId(category: Category?): Long =
    category?.id ?: VirtualCategoryIds.ADULT
