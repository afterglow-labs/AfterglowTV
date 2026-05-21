package com.afterglowtv.app.ui.model

enum class VodViewMode(val storageValue: String) {
    SHELVES("shelves"),
    GRID("grid"),
    GUIDE("guide");

    companion object {
        fun fromStorage(value: String?): VodViewMode {
            val normalized = value?.trim()?.lowercase()
            return when (normalized) {
                "modern" -> SHELVES
                "classic" -> GRID
                else -> entries.firstOrNull { it.storageValue.equals(value, ignoreCase = true) } ?: SHELVES
            }
        }
    }
}
