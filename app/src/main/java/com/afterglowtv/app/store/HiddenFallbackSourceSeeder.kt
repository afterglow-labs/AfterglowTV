package com.afterglowtv.app.store

import android.content.Context
import android.util.Log
import com.afterglowtv.data.preferences.PreferencesRepository
import com.afterglowtv.domain.model.ActiveLiveSource
import com.afterglowtv.domain.model.ProviderEpgSyncMode
import com.afterglowtv.domain.model.Provider
import com.afterglowtv.domain.model.ProviderSourceSlot
import com.afterglowtv.domain.repository.ProviderRepository
import com.afterglowtv.domain.usecase.M3uProviderSetupCommand
import com.afterglowtv.domain.usecase.ValidateAndAddProvider
import com.afterglowtv.domain.usecase.ValidateAndAddProviderResult
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class HiddenFallbackSourceSeeder @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val providerRepository: ProviderRepository,
    private val preferencesRepository: PreferencesRepository,
    private val validateAndAddProvider: ValidateAndAddProvider
) {
    suspend fun seedIfNeeded(policy: StorePolicySnapshot = StorePolicy.current) {
        if (!policy.enableHiddenFallbackSource) return
        var providers = providerRepository.getProviders().first()

        var liveFallbackProviderIdToActivate: Long? = null
        policy.hiddenFallbackSources.forEach { spec ->
            val fallbackUrl = prepareHiddenFallbackPlaylistFile(spec) ?: return@forEach
            providers = providerRepository.getProviders().first()
            val existingFallback = providers.firstOrNull { provider ->
                provider.m3uUrl.contains(spec.providerFileName) ||
                    provider.serverUrl.contains(spec.providerFileName)
            }
            val providerId = existingFallback?.id ?: if (policy.shouldEnsureHiddenFallback(providers)) {
                when (
                    val result = validateAndAddProvider.addM3u(
                        M3uProviderSetupCommand(
                            url = fallbackUrl,
                            name = spec.providerName,
                            epgSyncMode = ProviderEpgSyncMode.SKIP,
                            m3uVodClassificationEnabled = spec.m3uVodClassificationEnabled,
                            existingProviderId = existingFallback?.id,
                            allowXtreamPlaylistAutoDetection = false
                        )
                    )
                ) {
                    is ValidateAndAddProviderResult.Success -> result.provider.id
                    is ValidateAndAddProviderResult.SavedWithWarning -> result.provider.id
                    else -> null
                }
            } else {
                null
            }

            if (providerId != null) {
                if (existingFallback != null) {
                    providerRepository.updateProvider(
                        existingFallback.copy(
                            epgUrl = "",
                            epgSyncMode = ProviderEpgSyncMode.SKIP
                        )
                    )
                    providerRepository.refreshProviderData(
                        providerId = providerId,
                        force = true,
                        epgSyncModeOverride = ProviderEpgSyncMode.SKIP
                    )
                }
                providers = providerRepository.getProviders().first()
                val currentSource = preferencesRepository.activeSource(spec.sourceSlot).first()
                if (shouldUseHiddenFallbackSourceForSlot(policy, providers, currentSource, providerId)) {
                    preferencesRepository.setActiveSource(
                        spec.sourceSlot,
                        ActiveLiveSource.ProviderSource(providerId)
                    )
                    if (spec.sourceSlot == ProviderSourceSlot.LIVE) {
                        liveFallbackProviderIdToActivate = providerId
                    }
                }
            }
        }

        liveFallbackProviderIdToActivate?.let {
            providerRepository.setActiveProvider(it)
            preferencesRepository.setLastActiveProviderId(it)
        }
    }

    private fun prepareHiddenFallbackPlaylistFile(spec: HiddenFallbackSourceSpec): String? {
        val outputDirectory = File(context.filesDir, StorePolicySnapshot.HIDDEN_FALLBACK_DIRECTORY)
        val outputFile = File(outputDirectory, spec.providerFileName)
        val tempFile = File(outputDirectory, "${spec.providerFileName}.tmp")

        return try {
            outputDirectory.mkdirs()
            context.assets.open(spec.assetPath).use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            if (!tempFile.renameTo(outputFile)) {
                tempFile.copyTo(outputFile, overwrite = true)
                tempFile.delete()
            }
            outputFile.toURI().toString()
        } catch (exception: IOException) {
            Log.w(
                TAG,
                "Unable to prepare hidden fallback playlist ${spec.assetPath}",
                exception
            )
            null
        }
    }

    private companion object {
        private const val TAG = "HiddenFallbackSeeder"
    }
}

internal fun shouldUseHiddenFallbackSourceForSlot(
    policy: StorePolicySnapshot,
    providers: List<Provider>,
    currentSource: ActiveLiveSource?,
    fallbackProviderId: Long
): Boolean {
    val onlyBundledSources = providers.isNotEmpty() && providers.all(policy::isHiddenFallbackProvider)
    if (!policy.enableHiddenFallbackSource || !onlyBundledSources) return false
    return currentSource != ActiveLiveSource.ProviderSource(fallbackProviderId)
}
