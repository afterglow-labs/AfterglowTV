package com.afterglowtv.app.store

import android.util.Log
import com.afterglowtv.app.BuildConfig
import com.afterglowtv.data.preferences.PreferencesRepository
import com.afterglowtv.domain.model.ActiveLiveSource
import com.afterglowtv.domain.model.EpgSource
import com.afterglowtv.domain.model.Provider
import com.afterglowtv.domain.model.ProviderEpgSyncMode
import com.afterglowtv.domain.model.ProviderM3uPlaylistKind
import com.afterglowtv.domain.model.ProviderSourceSlot
import com.afterglowtv.domain.model.ProviderStatus
import com.afterglowtv.domain.model.ProviderType
import com.afterglowtv.domain.model.Result
import com.afterglowtv.domain.repository.EpgSourceRepository
import com.afterglowtv.domain.repository.ProviderRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class DevModePlaylistPresetSeeder @Inject constructor(
    private val providerRepository: ProviderRepository,
    private val epgSourceRepository: EpgSourceRepository,
    private val preferencesRepository: PreferencesRepository
) {
    suspend fun seedIfNeeded(rawSpecs: String = BuildConfig.DEV_MODE_PLAYLIST_PRESET_SPECS) {
        val presets = parseDevModePlaylistPresetSpecs(rawSpecs)
        if (presets.isEmpty()) return

        val fingerprint = presets.seedFingerprint()
        if (preferencesRepository.devModePlaylistPresetSeedFingerprint.first() == fingerprint) return

        var seededCleanly = true
        presets.forEach { preset ->
            runCatching { seedPreset(preset) }
                .onFailure { error ->
                    seededCleanly = false
                    Log.w(TAG, "Unable to seed developer playlist preset ${preset.name}", error)
                }
        }

        if (seededCleanly) {
            preferencesRepository.setDevModePlaylistPresetSeedFingerprint(fingerprint)
        }
    }

    private suspend fun seedPreset(preset: DevModePlaylistPreset) {
        var providers = providerRepository.getProviders().first()
        val existingProvider = providers.firstOrNull { it.matchesPreset(preset) }
        val providerId = if (existingProvider == null) {
            when (val result = providerRepository.addProvider(preset.toProvider())) {
                is Result.Success -> result.data
                is Result.Error -> {
                    val retryProvider = providerRepository.getProviders().first()
                        .firstOrNull { it.matchesPreset(preset) }
                    retryProvider?.id ?: error(result.message)
                }
                Result.Loading -> error("Unexpected loading state while seeding ${preset.name}")
            }
        } else {
            updatePresetProvider(existingProvider, preset)
            existingProvider.id
        }

        attachEpgSource(providerId, preset)
        if (preferencesRepository.activeSource(preset.sourceSlot).first() == null) {
            preferencesRepository.setActiveSource(
                preset.sourceSlot,
                ActiveLiveSource.ProviderSource(providerId)
            )
        }
    }

    private suspend fun updatePresetProvider(provider: Provider, preset: DevModePlaylistPreset) {
        providerRepository.updateProvider(
            provider.copy(
                name = preset.name,
                serverUrl = preset.playlistUrl,
                m3uUrl = preset.playlistUrl,
                epgUrl = preset.epgUrl,
                epgSyncMode = ProviderEpgSyncMode.BACKGROUND,
                m3uVodClassificationEnabled = preset.playlistKind == ProviderM3uPlaylistKind.VOD,
                m3uPlaylistKind = preset.playlistKind,
                status = if (provider.status == ProviderStatus.UNKNOWN) ProviderStatus.PARTIAL else provider.status
            )
        )
    }

    private suspend fun attachEpgSource(providerId: Long, preset: DevModePlaylistPreset) {
        if (preset.epgUrl.isBlank()) return

        val source = existingEpgSource(preset.epgUrl)
            ?: when (val result = epgSourceRepository.addSource("${preset.name} EPG", preset.epgUrl)) {
                is Result.Success -> result.data
                is Result.Error -> existingEpgSource(preset.epgUrl)
                Result.Loading -> null
            }
            ?: return

        epgSourceRepository.assignSourceToProvider(
            providerId = providerId,
            epgSourceId = source.id,
            priority = 1
        )
    }

    private suspend fun existingEpgSource(url: String): EpgSource? =
        epgSourceRepository.getAllSources().first().firstOrNull { it.url == url }

    private fun Provider.matchesPreset(preset: DevModePlaylistPreset): Boolean =
        type == ProviderType.M3U &&
            (m3uUrl == preset.playlistUrl || serverUrl == preset.playlistUrl)

    private fun DevModePlaylistPreset.toProvider(): Provider =
        Provider(
            name = name,
            type = ProviderType.M3U,
            serverUrl = playlistUrl,
            m3uUrl = playlistUrl,
            epgUrl = epgUrl,
            isActive = false,
            epgSyncMode = ProviderEpgSyncMode.BACKGROUND,
            m3uVodClassificationEnabled = playlistKind == ProviderM3uPlaylistKind.VOD,
            m3uPlaylistKind = playlistKind,
            status = ProviderStatus.PARTIAL
        )

    private companion object {
        private const val TAG = "DevPlaylistSeeder"
    }
}

internal data class DevModePlaylistPreset(
    val name: String,
    val playlistKind: ProviderM3uPlaylistKind,
    val sourceSlot: ProviderSourceSlot,
    val playlistUrl: String,
    val epgUrl: String
)

internal fun parseDevModePlaylistPresetSpecs(rawSpecs: String): List<DevModePlaylistPreset> =
    rawSpecs.split('|')
        .mapNotNull { rawSpec ->
            val parts = rawSpec.split("::")
            if (parts.size !in 4..5) return@mapNotNull null
            val name = parts[0].trim()
            val playlistKind = runCatching {
                ProviderM3uPlaylistKind.valueOf(parts[1].trim())
            }.getOrNull() ?: return@mapNotNull null
            val sourceSlot = runCatching {
                ProviderSourceSlot.valueOf(parts[2].trim())
            }.getOrNull() ?: return@mapNotNull null
            val playlistUrl = parts[3].trim()
            val epgUrl = parts.getOrNull(4)?.trim().orEmpty()
            DevModePlaylistPreset(
                name = name,
                playlistKind = playlistKind,
                sourceSlot = sourceSlot,
                playlistUrl = playlistUrl,
                epgUrl = epgUrl
            ).takeIf { name.isNotBlank() && playlistUrl.isNotBlank() }
        }

private fun List<DevModePlaylistPreset>.seedFingerprint(): String =
    joinToString("|") { preset ->
        listOf(
            preset.name,
            preset.playlistKind.name,
            preset.sourceSlot.name,
            preset.playlistUrl,
            preset.epgUrl
        ).joinToString("::")
    }.hashCode().toString()
