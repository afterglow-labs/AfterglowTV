package com.afterglowtv.domain.model

enum class GuideNoDataTextMode {
    CHANNEL_NAME,
    GENERIC,
    CUSTOM,
    BLANK;

    companion object {
        fun fromStorage(value: String?): GuideNoDataTextMode =
            entries.firstOrNull { it.name == value } ?: CHANNEL_NAME
    }
}
