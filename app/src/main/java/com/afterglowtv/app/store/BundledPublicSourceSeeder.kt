package com.afterglowtv.app.store

import android.content.Context
import android.util.Log
import com.afterglowtv.data.preferences.PreferencesRepository
import com.afterglowtv.domain.model.ActiveLiveSource
import com.afterglowtv.domain.model.EpgSource
import com.afterglowtv.domain.model.Provider
import com.afterglowtv.domain.model.ProviderEpgSyncMode
import com.afterglowtv.domain.model.ProviderSourceSlot
import com.afterglowtv.domain.model.Result
import com.afterglowtv.domain.repository.EpgSourceRepository
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
class BundledPublicSourceSeeder @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val providerRepository: ProviderRepository,
    private val epgSourceRepository: EpgSourceRepository,
    private val preferencesRepository: PreferencesRepository,
    private val validateAndAddProvider: ValidateAndAddProvider
) {
    suspend fun seedIfNeeded(policy: StorePolicySnapshot = StorePolicy.current) {
        if (!policy.enableBundledPublicSource) return

        var providers = providerRepository.getProviders().first()
        val seededOnce = preferencesRepository.bundledPublicSourceSeeded.first()
        val existingBundledProvider = providers.firstOrNull(policy::isBundledPublicSourceProvider)

        if (existingBundledProvider == null && seededOnce) return
        if (existingBundledProvider == null && providers.isNotEmpty()) {
            preferencesRepository.setBundledPublicSourceSeeded(true)
            return
        }

        var liveProviderIdToActivate: Long? = null
        policy.bundledPublicSources.forEach { spec ->
            val preparedPlaylist = prepareBundledPlaylistFile(spec) ?: return@forEach
            val bundledGuideUrl = prepareBundledGuideFile(spec, preparedPlaylist)
            val playlistUrl = spec.playlistUrl.ifBlank { preparedPlaylist.file.toURI().toString() }
            val guideUrl = spec.guideUrl.ifBlank { bundledGuideUrl.orEmpty() }.ifBlank { null }
            providers = providerRepository.getProviders().first()
            val existingProvider = providers.firstOrNull(policy::isBundledPublicSourceProvider)

            val providerId = existingProvider?.id ?: if (policy.shouldSeedBundledPublicSource(providers, seededOnce)) {
                when (
                    val result = validateAndAddProvider.addM3u(
                        M3uProviderSetupCommand(
                            url = playlistUrl,
                            name = spec.providerName,
                            epgSyncMode = ProviderEpgSyncMode.SKIP,
                            m3uVodClassificationEnabled = spec.m3uVodClassificationEnabled,
                            existingProviderId = null,
                            epgUrl = guideUrl,
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
                updateBundledProviderMetadata(providerId, spec, playlistUrl, guideUrl)
                attachGuideSource(providerId, spec, guideUrl, bundledGuideUrl)
                preferencesRepository.setBundledPublicSourceSeeded(true)

                providers = providerRepository.getProviders().first()
                val currentSource = preferencesRepository.activeSource(spec.sourceSlot).first()
                if (shouldUseBundledPublicSourceForSlot(providers, currentSource, providerId)) {
                    preferencesRepository.setActiveSource(
                        spec.sourceSlot,
                        ActiveLiveSource.ProviderSource(providerId)
                    )
                    if (spec.sourceSlot == ProviderSourceSlot.LIVE) {
                        liveProviderIdToActivate = providerId
                    }
                }
            }
        }

        liveProviderIdToActivate?.let {
            providerRepository.setActiveProvider(it)
            preferencesRepository.setLastActiveProviderId(it)
        }
    }

    private suspend fun updateBundledProviderMetadata(
        providerId: Long,
        spec: BundledPublicSourceSpec,
        playlistUrl: String,
        guideUrl: String?
    ) {
        val provider = providerRepository.getProvider(providerId) ?: return
        providerRepository.updateProvider(
            provider.copy(
                name = spec.providerName,
                serverUrl = playlistUrl,
                m3uUrl = playlistUrl,
                epgUrl = guideUrl.orEmpty(),
                epgSyncMode = ProviderEpgSyncMode.SKIP,
                m3uVodClassificationEnabled = spec.m3uVodClassificationEnabled
            )
        )
    }

    private suspend fun attachGuideSource(
        providerId: Long,
        spec: BundledPublicSourceSpec,
        guideUrl: String?,
        bundledGuideUrl: String?
    ) {
        if (guideUrl.isNullOrBlank()) return
        val sourceName = "${spec.providerName} Guide"
        val legacySource = existingGuideSourceOrNull(bundledGuideUrl)
            ?.takeIf { it.url != guideUrl }
        val source = existingGuideSource(guideUrl)
            ?: when (
            val result = epgSourceRepository.addSource(
                name = sourceName,
                url = guideUrl
            )
        ) {
            is Result.Success -> result.data
            else -> existingGuideSource(guideUrl)
        } ?: return

        epgSourceRepository.assignSourceToProvider(
            providerId = providerId,
            epgSourceId = source.id,
            priority = 1
        )
        runCatching { epgSourceRepository.refreshSource(source.id) }
            .onFailure { error -> Log.w(TAG, "Unable to refresh bundled public guide", error) }
        runCatching { epgSourceRepository.resolveForProvider(providerId) }
            .onFailure { error -> Log.w(TAG, "Unable to resolve bundled public guide", error) }
        legacySource?.let { sourceToDelete ->
            runCatching { epgSourceRepository.deleteSource(sourceToDelete.id) }
                .onFailure { error -> Log.w(TAG, "Unable to remove legacy bundled public guide", error) }
        }
    }

    private suspend fun existingGuideSource(guideUrl: String): EpgSource? =
        epgSourceRepository.getAllSources().first().firstOrNull { it.url == guideUrl }

    private suspend fun existingGuideSourceOrNull(guideUrl: String?): EpgSource? =
        guideUrl?.takeIf { it.isNotBlank() }?.let { existingGuideSource(it) }

    private fun prepareBundledPlaylistFile(spec: BundledPublicSourceSpec): PreparedBundledPlaylist? {
        val outputDirectory = File(context.filesDir, StorePolicySnapshot.BUNDLED_PUBLIC_SOURCE_DIRECTORY)
        val outputFile = File(outputDirectory, spec.playlistFileName)
        val tempFile = File(outputDirectory, "${spec.playlistFileName}.tmp")

        return try {
            outputDirectory.mkdirs()
            context.assets.open(spec.playlistAssetPath).use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            if (!tempFile.renameTo(outputFile)) {
                tempFile.copyTo(outputFile, overwrite = true)
                tempFile.delete()
            }
            PreparedBundledPlaylist(outputFile)
        } catch (exception: IOException) {
            Log.w(
                TAG,
                "Unable to prepare bundled public playlist ${spec.playlistAssetPath}",
                exception
            )
            null
        }
    }

    private fun prepareBundledGuideFile(
        spec: BundledPublicSourceSpec,
        playlist: PreparedBundledPlaylist
    ): String? {
        val guideFile = File(playlist.file.parentFile, spec.guideFileName)
        return try {
            BundledPublicGuideGenerator.writeGuide(
                playlistFile = playlist.file,
                outputFile = guideFile
            )
            guideFile.toURI().toString()
        } catch (exception: Exception) {
            Log.w(TAG, "Unable to generate bundled public guide ${spec.guideFileName}", exception)
            null
        }
    }

    private companion object {
        private const val TAG = "BundledPublicSeeder"
    }
}

private data class PreparedBundledPlaylist(
    val file: File
)

internal fun shouldUseBundledPublicSourceForSlot(
    providers: List<Provider>,
    currentSource: ActiveLiveSource?,
    bundledProviderId: Long
): Boolean {
    if (providers.singleOrNull()?.id != bundledProviderId) return false
    return currentSource != ActiveLiveSource.ProviderSource(bundledProviderId)
}
