package com.afterglowtv.app.navigation

import java.io.Serializable

sealed class ExternalDestination : Serializable {
    data object Home : ExternalDestination()

    fun toRoute(): String = when (this) {
        Home -> Routes.HOME
    }

    companion object {
        fun fromLegacyRoute(route: String): ExternalDestination? {
            val normalizedRoute = route.trim()
            if (normalizedRoute.isEmpty()) return null

            return when {
                normalizedRoute == Routes.HOME -> Home
                else -> null
            }
        }
    }
}

sealed interface ExternalNavigationRequest {
    data class Search(val query: String) : ExternalNavigationRequest
    data class Player(val request: PlayerNavigationRequest) : ExternalNavigationRequest
    data class Destination(val destination: ExternalDestination) : ExternalNavigationRequest
    data class ImportM3u(val uri: String) : ExternalNavigationRequest
    data class ImportBackup(val uri: String) : ExternalNavigationRequest
}
