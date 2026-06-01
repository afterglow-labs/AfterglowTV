package com.afterglowtv.app.navigation

import java.io.Serializable
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

sealed class ExternalDestination : Serializable {
    data object Home : ExternalDestination()

    data class ProviderSetup(
        val providerId: Long? = null,
        val importUri: String? = null
    ) : ExternalDestination()

    fun toRoute(): String = when (this) {
        Home -> Routes.HOME
        is ProviderSetup -> Routes.providerSetup(providerId = providerId, importUri = importUri)
    }

    companion object {
        fun fromLegacyRoute(route: String): ExternalDestination? {
            val normalizedRoute = route.trim()
            if (normalizedRoute.isEmpty()) return null

            return when {
                normalizedRoute == Routes.HOME -> Home
                normalizedRoute.startsWith(Routes.PROVIDER_SETUP.substringBefore('?')) -> {
                    val queryParameters = normalizedRoute.queryParameters()
                    val providerId = queryParameters["providerId"]
                        ?.toLongOrNull()
                        ?.takeIf { it >= 0L }
                    val importUri = queryParameters["importUri"]
                        ?.takeIf { it.isNotBlank() }
                    ProviderSetup(providerId = providerId, importUri = importUri)
                }

                else -> null
            }
        }
    }
}

private fun String.queryParameters(): Map<String, String> {
    val query = substringAfter('?', missingDelimiterValue = "")
    if (query.isBlank()) return emptyMap()
    return query.split('&')
        .mapNotNull { entry ->
            val key = entry.substringBefore('=', missingDelimiterValue = "").takeIf { it.isNotBlank() }
                ?: return@mapNotNull null
            val rawValue = entry.substringAfter('=', missingDelimiterValue = "")
            key to URLDecoder.decode(rawValue, StandardCharsets.UTF_8)
        }
        .toMap()
}

sealed interface ExternalNavigationRequest {
    data class Search(val query: String) : ExternalNavigationRequest
    data class Player(val request: PlayerNavigationRequest) : ExternalNavigationRequest
    data class Destination(val destination: ExternalDestination) : ExternalNavigationRequest
    data class ImportM3u(val uri: String) : ExternalNavigationRequest
    data class ImportBackup(val uri: String) : ExternalNavigationRequest
}
