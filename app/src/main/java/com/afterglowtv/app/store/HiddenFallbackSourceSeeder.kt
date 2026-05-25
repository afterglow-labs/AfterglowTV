package com.afterglowtv.app.store

import com.afterglowtv.domain.model.ProviderEpgSyncMode
import com.afterglowtv.domain.repository.ProviderRepository
import com.afterglowtv.domain.usecase.M3uProviderSetupCommand
import com.afterglowtv.domain.usecase.ValidateAndAddProvider
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class HiddenFallbackSourceSeeder @Inject constructor(
    private val providerRepository: ProviderRepository,
    private val validateAndAddProvider: ValidateAndAddProvider
) {
    suspend fun seedIfNeeded(policy: StorePolicySnapshot = StorePolicy.current) {
        if (!policy.enableHiddenFallbackSource) return
        val fallbackUrl = policy.hiddenFallbackPlaylistUrl?.takeIf { it.isNotBlank() } ?: return
        val providers = providerRepository.getProviders().first()
        if (!policy.shouldEnsureHiddenFallback(providers)) return

        val existingFallback = providers.firstOrNull(policy::isHiddenFallbackProvider)
        if (existingFallback != null) {
            if (existingFallback.isActive) {
                return
            }
        }

        validateAndAddProvider.addM3u(
            M3uProviderSetupCommand(
                url = fallbackUrl,
                name = HIDDEN_FALLBACK_SOURCE_NAME,
                epgSyncMode = ProviderEpgSyncMode.BACKGROUND,
                m3uVodClassificationEnabled = false,
                existingProviderId = existingFallback?.id,
                allowXtreamPlaylistAutoDetection = false
            )
        )
    }

    private companion object {
        private const val HIDDEN_FALLBACK_SOURCE_NAME = "Afterglow TV"
    }
}
