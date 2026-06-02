package com.afterglowtv.domain.usecase

import com.afterglowtv.domain.manager.ProviderSetupInputValidator
import com.afterglowtv.domain.model.Provider
import com.afterglowtv.domain.model.ProviderEpgSyncMode
import com.afterglowtv.domain.model.ProviderM3uPlaylistKind
import com.afterglowtv.domain.model.ProviderXtreamLiveSyncMode
import com.afterglowtv.domain.model.ProviderSavedWithSyncErrorException
import com.afterglowtv.domain.model.Result
import com.afterglowtv.domain.repository.EpgSourceRepository
import com.afterglowtv.domain.repository.ProviderRepository
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject

data class XtreamProviderSetupCommand(
    val serverUrl: String,
    val username: String,
    val password: String,
    val name: String,
    val httpUserAgent: String = "",
    val httpHeaders: String = "",
    val xtreamFastSyncEnabled: Boolean = true,
    val epgSyncMode: ProviderEpgSyncMode = ProviderEpgSyncMode.BACKGROUND,
    val xtreamLiveSyncMode: ProviderXtreamLiveSyncMode = ProviderXtreamLiveSyncMode.AUTO,
    val existingProviderId: Long? = null
)

data class M3uProviderSetupCommand(
    val url: String,
    val name: String,
    val httpUserAgent: String = "",
    val httpHeaders: String = "",
    val epgSyncMode: ProviderEpgSyncMode = ProviderEpgSyncMode.BACKGROUND,
    val m3uVodClassificationEnabled: Boolean = false,
    val m3uPlaylistKind: ProviderM3uPlaylistKind = ProviderM3uPlaylistKind.LIVE,
    val existingProviderId: Long? = null,
    /** Optional XMLTV / EPG URL to attach to the newly-created provider. */
    val epgUrl: String? = null,
    val allowXtreamPlaylistAutoDetection: Boolean = true
)

data class StalkerProviderSetupCommand(
    val portalUrl: String,
    val macAddress: String,
    val name: String,
    val deviceProfile: String = "",
    val timezone: String = "",
    val locale: String = "",
    val epgSyncMode: ProviderEpgSyncMode = ProviderEpgSyncMode.BACKGROUND,
    val existingProviderId: Long? = null
)

sealed class ValidateAndAddProviderResult {
    data class Success(val provider: Provider) : ValidateAndAddProviderResult()
    data class SavedWithWarning(val provider: Provider, val warning: String) : ValidateAndAddProviderResult()
    data class ValidationError(val message: String) : ValidateAndAddProviderResult()
    data class Error(val message: String, val exception: Throwable? = null) : ValidateAndAddProviderResult()
}

class ValidateAndAddProvider @Inject constructor(
    private val providerSetupInputValidator: ProviderSetupInputValidator,
    private val providerRepository: ProviderRepository,
    private val epgSourceRepository: EpgSourceRepository,
) {
    companion object {
        private const val MAX_XTREAM_PLAYLIST_USERNAME_LENGTH = 128
        private const val MAX_XTREAM_PLAYLIST_PASSWORD_LENGTH = 256
    }

    /**
     * Performs local input validation only — no network calls, no persistence, no side effects.
     * Returns the [ValidateAndAddProviderResult.ValidationError] if the input is invalid, or
     * `null` if the input passed all local checks and is ready for [loginXtream].
     */
    fun validateXtreamInput(command: XtreamProviderSetupCommand): ValidateAndAddProviderResult.ValidationError? {
        return when (
            val result = providerSetupInputValidator.validateXtream(
                serverUrl = command.serverUrl,
                username = command.username,
                password = command.password,
                allowBlankPassword = command.existingProviderId != null,
                name = command.name,
                httpUserAgent = command.httpUserAgent,
                httpHeaders = command.httpHeaders
            )
        ) {
            is Result.Error -> ValidateAndAddProviderResult.ValidationError(result.message)
            else -> null
        }
    }

    /**
     * Performs local input validation only — no network calls, no persistence, no side effects.
     * Returns the [ValidateAndAddProviderResult.ValidationError] if the input is invalid, or
     * `null` if the input passed all local checks and is ready for [addM3u].
     */
    fun validateM3uInput(command: M3uProviderSetupCommand): ValidateAndAddProviderResult.ValidationError? {
        return when (
            val result = providerSetupInputValidator.validateM3u(
                url = command.url,
                name = command.name,
                httpUserAgent = command.httpUserAgent,
                httpHeaders = command.httpHeaders
            )
        ) {
            is Result.Error -> ValidateAndAddProviderResult.ValidationError(result.message)
            else -> null
        }
    }

    /**
     * Performs local input validation only — no network calls, no persistence, no side effects.
     * Returns the [ValidateAndAddProviderResult.ValidationError] if the input is invalid, or
     * `null` if the input passed all local checks and is ready for [loginStalker].
     */
    fun validateStalkerInput(command: StalkerProviderSetupCommand): ValidateAndAddProviderResult.ValidationError? {
        return when (
            val result = providerSetupInputValidator.validateStalker(
                portalUrl = command.portalUrl,
                macAddress = command.macAddress,
                name = command.name,
                deviceProfile = command.deviceProfile,
                timezone = command.timezone,
                locale = command.locale
            )
        ) {
            is Result.Error -> ValidateAndAddProviderResult.ValidationError(result.message)
            else -> null
        }
    }

    suspend fun loginXtream(
        command: XtreamProviderSetupCommand,
        onProgress: ((String) -> Unit)? = null
    ): ValidateAndAddProviderResult {
        return when (
            val validated = providerSetupInputValidator.validateXtream(
                serverUrl = command.serverUrl,
                username = command.username,
                password = command.password,
                allowBlankPassword = command.existingProviderId != null,
                name = command.name,
                httpUserAgent = command.httpUserAgent,
                httpHeaders = command.httpHeaders
            )
        ) {
            is Result.Success -> providerRepository.loginXtream(
                serverUrl = validated.data.serverUrl,
                username = validated.data.username,
                password = validated.data.password,
                name = validated.data.name,
                httpUserAgent = validated.data.httpUserAgent,
                httpHeaders = validated.data.httpHeaders,
                xtreamFastSyncEnabled = command.xtreamFastSyncEnabled,
                epgSyncMode = command.epgSyncMode,
                xtreamLiveSyncMode = command.xtreamLiveSyncMode,
                onProgress = onProgress,
                id = command.existingProviderId
            ).toUseCaseResult()

            is Result.Error -> ValidateAndAddProviderResult.ValidationError(validated.message)
            is Result.Loading -> ValidateAndAddProviderResult.Error("Unexpected loading state")
        }
    }

    suspend fun addM3u(
        command: M3uProviderSetupCommand,
        onProgress: ((String) -> Unit)? = null
    ): ValidateAndAddProviderResult {
        return when (
            val validated = providerSetupInputValidator.validateM3u(
                url = command.url,
                name = command.name,
                httpUserAgent = command.httpUserAgent,
                httpHeaders = command.httpHeaders
            )
        ) {
            is Result.Success -> {
                val validatedInput = validated.data
                val parsedXtream = if (command.allowXtreamPlaylistAutoDetection && command.m3uPlaylistKind != ProviderM3uPlaylistKind.VOD) {
                    parseXtreamPlaylistUrl(validatedInput.url)
                } else {
                    ParsedXtreamPlaylistUrlResult.NotXtreamPlaylist
                }
                when (parsedXtream) {
                    is ParsedXtreamPlaylistUrlResult.ValidationError ->
                        ValidateAndAddProviderResult.ValidationError(parsedXtream.message)

                    is ParsedXtreamPlaylistUrlResult.Success -> {
                        providerRepository.loginXtream(
                            serverUrl = parsedXtream.serverUrl,
                            username = parsedXtream.username,
                            password = parsedXtream.password,
                            name = validatedInput.name,
                            httpUserAgent = validatedInput.httpUserAgent,
                            httpHeaders = validatedInput.httpHeaders,
                            xtreamFastSyncEnabled = true,
                            epgSyncMode = command.epgSyncMode,
                            xtreamLiveSyncMode = ProviderXtreamLiveSyncMode.AUTO,
                            onProgress = onProgress,
                            id = command.existingProviderId
                        ).toUseCaseResult()
                    }

                    ParsedXtreamPlaylistUrlResult.NotXtreamPlaylist -> {
                        val mapped = providerRepository.validateM3u(
                            url = validatedInput.url,
                            name = validatedInput.name,
                            httpUserAgent = validatedInput.httpUserAgent,
                            httpHeaders = validatedInput.httpHeaders,
                            epgSyncMode = command.epgSyncMode,
                            m3uVodClassificationEnabled = command.m3uVodClassificationEnabled || command.m3uPlaylistKind == ProviderM3uPlaylistKind.VOD,
                            m3uPlaylistKind = command.m3uPlaylistKind,
                            onProgress = onProgress,
                            id = command.existingProviderId
                        ).toUseCaseResult()
                        attachOptionalEpgSource(mapped, command.epgUrl, validatedInput.name)
                        mapped
                    }
                }
            }

            is Result.Error -> ValidateAndAddProviderResult.ValidationError(validated.message)
            is Result.Loading -> ValidateAndAddProviderResult.Error("Unexpected loading state")
        }
    }

    suspend fun loginStalker(
        command: StalkerProviderSetupCommand,
        onProgress: ((String) -> Unit)? = null
    ): ValidateAndAddProviderResult {
        return when (
            val validated = providerSetupInputValidator.validateStalker(
                portalUrl = command.portalUrl,
                macAddress = command.macAddress,
                name = command.name,
                deviceProfile = command.deviceProfile,
                timezone = command.timezone,
                locale = command.locale
            )
        ) {
            is Result.Success -> providerRepository.loginStalker(
                portalUrl = validated.data.portalUrl,
                macAddress = validated.data.macAddress,
                name = validated.data.name,
                deviceProfile = validated.data.deviceProfile,
                timezone = validated.data.timezone,
                locale = validated.data.locale,
                epgSyncMode = command.epgSyncMode,
                onProgress = onProgress,
                id = command.existingProviderId
            ).toUseCaseResult()

            is Result.Error -> ValidateAndAddProviderResult.ValidationError(validated.message)
            is Result.Loading -> ValidateAndAddProviderResult.Error("Unexpected loading state")
        }
    }

    /**
     * Best-effort EPG source attachment after the underlying provider has been added.
     *
     * If the provider add did not succeed, or the user didn't supply an EPG URL, this is a no-op.
     * If the EPG add itself fails, we silently swallow — the provider was created successfully
     * and the user can retry the EPG add from Settings → TV Guide.
     */
    private suspend fun attachOptionalEpgSource(
        addResult: ValidateAndAddProviderResult,
        epgUrl: String?,
        providerName: String,
    ) {
        if (epgUrl.isNullOrBlank()) return
        val provider: Provider = when (addResult) {
            is ValidateAndAddProviderResult.Success -> addResult.provider
            is ValidateAndAddProviderResult.SavedWithWarning -> addResult.provider
            else -> return
        }
        val sourceName = if (providerName.isNotBlank()) "$providerName EPG" else "EPG for ${provider.id}"
        val added = epgSourceRepository.addSource(name = sourceName, url = epgUrl.trim())
        if (added is Result.Success) {
            epgSourceRepository.assignSourceToProvider(
                providerId = provider.id,
                epgSourceId = added.data.id,
                priority = 1,
            )
            // Fetch + ingest immediately so the guide populates without the user
            // having to manually refresh. Failures are non-fatal — the source is
            // already registered and a future provider sync will retry.
            runCatching { epgSourceRepository.refreshSource(added.data.id) }
            runCatching { epgSourceRepository.resolveForProvider(provider.id) }
        }
    }

    private fun Result<Provider>.toUseCaseResult(): ValidateAndAddProviderResult = when (this) {
        is Result.Success -> ValidateAndAddProviderResult.Success(data)
        is Result.Error -> {
            val savedWithWarning = exception as? ProviderSavedWithSyncErrorException
            if (savedWithWarning != null) {
                ValidateAndAddProviderResult.SavedWithWarning(
                    provider = savedWithWarning.provider,
                    warning = savedWithWarning.message ?: message
                )
            } else {
                ValidateAndAddProviderResult.Error(message, exception)
            }
        }
        is Result.Loading -> ValidateAndAddProviderResult.Error("Unexpected loading state")
    }

    private sealed interface ParsedXtreamPlaylistUrlResult {
        data object NotXtreamPlaylist : ParsedXtreamPlaylistUrlResult
        data class ValidationError(val message: String) : ParsedXtreamPlaylistUrlResult
        data class Success(
            val serverUrl: String,
            val username: String,
            val password: String
        ) : ParsedXtreamPlaylistUrlResult
    }

    private fun parseXtreamPlaylistUrl(url: String): ParsedXtreamPlaylistUrlResult {
        val uri = runCatching { URI(url) }.getOrNull() ?: return ParsedXtreamPlaylistUrlResult.NotXtreamPlaylist
        val scheme = uri.scheme?.lowercase() ?: return ParsedXtreamPlaylistUrlResult.NotXtreamPlaylist
        if (scheme != "http" && scheme != "https") return ParsedXtreamPlaylistUrlResult.NotXtreamPlaylist

        val normalizedPath = uri.path.orEmpty().lowercase()
        if (!normalizedPath.endsWith("/get.php")) return ParsedXtreamPlaylistUrlResult.NotXtreamPlaylist

        val query = parseQueryParameters(uri.rawQuery)
        val username = query["username"]?.takeIf { it.isNotBlank() }
            ?: return ParsedXtreamPlaylistUrlResult.NotXtreamPlaylist
        val password = query["password"]?.takeIf { it.isNotBlank() }
            ?: return ParsedXtreamPlaylistUrlResult.NotXtreamPlaylist
        val type = query["type"]?.lowercase()?.takeIf { it.isNotBlank() }
            ?: return ParsedXtreamPlaylistUrlResult.NotXtreamPlaylist
        if (type != "m3u" && type != "m3u_plus") return ParsedXtreamPlaylistUrlResult.NotXtreamPlaylist

        if (username.length > MAX_XTREAM_PLAYLIST_USERNAME_LENGTH) {
            return ParsedXtreamPlaylistUrlResult.ValidationError("Playlist username is too long.")
        }
        if (password.length > MAX_XTREAM_PLAYLIST_PASSWORD_LENGTH) {
            return ParsedXtreamPlaylistUrlResult.ValidationError("Playlist password is too long.")
        }

        if (!uri.userInfo.isNullOrBlank()) {
            return ParsedXtreamPlaylistUrlResult.ValidationError(
                "Playlist sources must not include embedded credentials in the URL authority."
            )
        }

        val host = uri.host?.takeIf { it.isNotBlank() }
            ?: return ParsedXtreamPlaylistUrlResult.ValidationError("Playlist sources must include a host.")
        val authority = buildString {
            append(host.asXtreamServerHost())
            if (uri.port != -1) {
                append(":")
                append(uri.port)
            }
        }
        return ParsedXtreamPlaylistUrlResult.Success(
            serverUrl = "$scheme://$authority",
            username = username,
            password = password
        )
    }

    private fun parseQueryParameters(rawQuery: String?): Map<String, String> {
        if (rawQuery.isNullOrBlank()) return emptyMap()
        return rawQuery.split('&')
            .mapNotNull { part ->
                val key = part.substringBefore('=', "")
                    .takeIf { it.isNotBlank() }
                    ?: return@mapNotNull null
                val value = part.substringAfter('=', "")
                decodeQueryComponent(key) to decodeQueryComponent(value)
            }
            .toMap()
    }

    private fun decodeQueryComponent(value: String): String =
        URLDecoder.decode(value, StandardCharsets.UTF_8.name())

    private fun String.asXtreamServerHost(): String =
        if (contains(':') && !startsWith("[")) "[$this]" else this
}
