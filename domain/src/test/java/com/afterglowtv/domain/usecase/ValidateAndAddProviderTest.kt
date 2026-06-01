package com.afterglowtv.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.afterglowtv.domain.manager.ProviderSetupInputValidator
import com.afterglowtv.domain.model.Program
import com.afterglowtv.domain.manager.ValidatedM3uProviderInput
import com.afterglowtv.domain.manager.ValidatedStalkerProviderInput
import com.afterglowtv.domain.manager.ValidatedXtreamProviderInput
import com.afterglowtv.domain.model.ProviderEpgSyncMode
import com.afterglowtv.domain.model.Provider
import com.afterglowtv.domain.model.ProviderM3uPlaylistKind
import com.afterglowtv.domain.model.ProviderSavedWithSyncErrorException
import com.afterglowtv.domain.model.ProviderStatus
import com.afterglowtv.domain.model.ProviderType
import com.afterglowtv.domain.model.ProviderXtreamLiveSyncMode
import com.afterglowtv.domain.model.Result
import com.afterglowtv.domain.model.EpgSource
import com.afterglowtv.domain.model.ProviderEpgSourceAssignment
import com.afterglowtv.domain.repository.EpgSourceRepository
import com.afterglowtv.domain.repository.ProviderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ValidateAndAddProviderTest {

    @Test
    fun `validateXtreamInput returns null when input is valid without calling repository`() {
        val repository = FakeProviderRepository()
        val useCase = ValidateAndAddProvider(
            providerSetupInputValidator = FakeProviderSetupInputValidator(),
            providerRepository = repository,
            epgSourceRepository = FakeEpgSourceRepository()
        )

        val error = useCase.validateXtreamInput(
            XtreamProviderSetupCommand(serverUrl = "https://example.com", username = "alice", password = "secret", name = "Provider")
        )

        assertThat(error).isNull()
        assertThat(repository.lastXtreamCall).isNull()
    }

    @Test
    fun `validateXtreamInput returns ValidationError without calling repository`() {
        val repository = FakeProviderRepository()
        val useCase = ValidateAndAddProvider(
            providerSetupInputValidator = FakeProviderSetupInputValidator(
                xtreamResult = Result.error("Please enter server URL")
            ),
            providerRepository = repository,
            epgSourceRepository = FakeEpgSourceRepository()
        )

        val error = useCase.validateXtreamInput(
            XtreamProviderSetupCommand(serverUrl = "", username = "alice", password = "secret", name = "Provider")
        )

        assertThat(error).isInstanceOf(ValidateAndAddProviderResult.ValidationError::class.java)
        assertThat((error as ValidateAndAddProviderResult.ValidationError).message).isEqualTo("Please enter server URL")
        assertThat(repository.lastXtreamCall).isNull()
    }

    @Test
    fun `validateM3uInput returns null when input is valid without calling repository`() {
        val repository = FakeProviderRepository()
        val useCase = ValidateAndAddProvider(
            providerSetupInputValidator = FakeProviderSetupInputValidator(),
            providerRepository = repository,
            epgSourceRepository = FakeEpgSourceRepository()
        )

        val error = useCase.validateM3uInput(
            M3uProviderSetupCommand(url = "https://example.com/list.m3u", name = "Playlist")
        )

        assertThat(error).isNull()
        assertThat(repository.lastM3uCall).isNull()
    }

    @Test
    fun `validateM3uInput returns ValidationError without calling repository`() {
        val repository = FakeProviderRepository()
        val useCase = ValidateAndAddProvider(
            providerSetupInputValidator = FakeProviderSetupInputValidator(
                m3uResult = Result.error("Please enter M3U URL")
            ),
            providerRepository = repository,
            epgSourceRepository = FakeEpgSourceRepository()
        )

        val error = useCase.validateM3uInput(
            M3uProviderSetupCommand(url = "", name = "Playlist")
        )

        assertThat(error).isInstanceOf(ValidateAndAddProviderResult.ValidationError::class.java)
        assertThat(repository.lastM3uCall).isNull()
    }

    @Test
    fun `validateStalkerInput returns null when input is valid without calling repository`() {
        val repository = FakeProviderRepository()
        val useCase = ValidateAndAddProvider(
            providerSetupInputValidator = FakeProviderSetupInputValidator(),
            providerRepository = repository,
            epgSourceRepository = FakeEpgSourceRepository()
        )

        val error = useCase.validateStalkerInput(
            StalkerProviderSetupCommand(
                portalUrl = "https://portal.example.com",
                macAddress = "00:1A:79:12:34:56",
                name = "MAG"
            )
        )

        assertThat(error).isNull()
        assertThat(repository.lastStalkerCall).isNull()
    }

    @Test
    fun `validateStalkerInput returns ValidationError without calling repository`() {
        val repository = FakeProviderRepository()
        val useCase = ValidateAndAddProvider(
            providerSetupInputValidator = FakeProviderSetupInputValidator(
                stalkerResult = Result.error("Please enter portal URL")
            ),
            providerRepository = repository,
            epgSourceRepository = FakeEpgSourceRepository()
        )

        val error = useCase.validateStalkerInput(
            StalkerProviderSetupCommand(portalUrl = "", macAddress = "00:1A:79:12:34:56", name = "MAG")
        )

        assertThat(error).isInstanceOf(ValidateAndAddProviderResult.ValidationError::class.java)
        assertThat(repository.lastStalkerCall).isNull()
    }

    @Test
    fun returns_validation_error_without_calling_repository_for_xtream() = runTest {
        val repository = FakeProviderRepository()
        val useCase = ValidateAndAddProvider(
            providerSetupInputValidator = FakeProviderSetupInputValidator(
                xtreamResult = Result.error("Please enter server URL")
            ),
            providerRepository = repository,
            epgSourceRepository = FakeEpgSourceRepository()
        )

        val result = useCase.loginXtream(
            XtreamProviderSetupCommand(
                serverUrl = "",
                username = "user",
                password = "secret",
                name = "Provider"
            )
        )

        assertThat(result).isInstanceOf(ValidateAndAddProviderResult.ValidationError::class.java)
        assertThat((result as ValidateAndAddProviderResult.ValidationError).message).isEqualTo("Please enter server URL")
        assertThat(repository.lastXtreamCall).isNull()
    }

    @Test
    fun delegates_normalized_xtream_input_to_repository() = runTest {
        val repository = FakeProviderRepository()
        val useCase = ValidateAndAddProvider(
            providerSetupInputValidator = FakeProviderSetupInputValidator(
                xtreamResult = Result.success(
                    ValidatedXtreamProviderInput(
                        serverUrl = "https://example.com",
                        username = "alice",
                        password = "normalized-secret",
                        name = "Premium",
                        httpUserAgent = "AfterglowTVTest/1.0",
                        httpHeaders = "Referer: https://example.com"
                    )
                )
            ),
            providerRepository = repository,
            epgSourceRepository = FakeEpgSourceRepository()
        )

        val result = useCase.loginXtream(
            XtreamProviderSetupCommand(
                serverUrl = " https://example.com ",
                username = " alice ",
                password = "secret\u0000",
                name = " Premium ",
                httpUserAgent = " AfterglowTVTest/1.0 ",
                httpHeaders = " Referer: https://example.com ",
                xtreamFastSyncEnabled = true,
                epgSyncMode = ProviderEpgSyncMode.BACKGROUND,
                existingProviderId = 7L
            )
        )

        assertThat(result).isInstanceOf(ValidateAndAddProviderResult.Success::class.java)
        assertThat(repository.lastXtreamCall).isEqualTo(
            XtreamCall(
                serverUrl = "https://example.com",
                username = "alice",
                password = "normalized-secret",
                name = "Premium",
                httpUserAgent = "AfterglowTVTest/1.0",
                httpHeaders = "Referer: https://example.com",
                xtreamFastSyncEnabled = true,
                epgSyncMode = ProviderEpgSyncMode.BACKGROUND,
                xtreamLiveSyncMode = ProviderXtreamLiveSyncMode.AUTO,
                id = 7L
            )
        )
    }

    @Test
    fun allows_blank_xtream_password_when_editing_existing_provider() = runTest {
        val repository = FakeProviderRepository()
        val useCase = ValidateAndAddProvider(
            providerSetupInputValidator = FakeProviderSetupInputValidator(
                xtreamResult = Result.success(
                    ValidatedXtreamProviderInput(
                        serverUrl = "https://example.com",
                        username = "alice",
                        password = "",
                        name = "Premium",
                        httpUserAgent = "",
                        httpHeaders = ""
                    )
                )
            ),
            providerRepository = repository,
            epgSourceRepository = FakeEpgSourceRepository()
        )

        val result = useCase.loginXtream(
            XtreamProviderSetupCommand(
                serverUrl = "https://example.com",
                username = "alice",
                password = "",
                name = "Premium",
                existingProviderId = 7L
            )
        )

        assertThat(result).isInstanceOf(ValidateAndAddProviderResult.Success::class.java)
        assertThat(repository.lastXtreamCall?.password).isEmpty()
    }

    @Test
    fun maps_saved_provider_sync_failure_to_saved_with_warning() = runTest {
        val repository = FakeProviderRepository().apply {
            xtreamResult = Result.error(
                "Provider login succeeded, but initial sync failed. The provider was saved and can be retried from Settings: timeout",
                ProviderSavedWithSyncErrorException(
                    provider = provider(id = 7L, name = "Premium", type = ProviderType.XTREAM_CODES).copy(status = ProviderStatus.ERROR),
                    message = "Provider login succeeded, but initial sync failed. The provider was saved and can be retried from Settings: timeout"
                )
            )
        }
        val useCase = ValidateAndAddProvider(
            providerSetupInputValidator = FakeProviderSetupInputValidator(),
            providerRepository = repository,
            epgSourceRepository = FakeEpgSourceRepository()
        )

        val result = useCase.loginXtream(
            XtreamProviderSetupCommand(
                serverUrl = "https://example.com",
                username = "alice",
                password = "secret",
                name = "Premium"
            )
        )

        assertThat(result).isInstanceOf(ValidateAndAddProviderResult.SavedWithWarning::class.java)
        result as ValidateAndAddProviderResult.SavedWithWarning
        assertThat(result.provider.id).isEqualTo(7L)
        assertThat(result.provider.status).isEqualTo(ProviderStatus.ERROR)
        assertThat(result.warning).contains("initial sync failed")
    }

    @Test
    fun delegates_validated_m3u_input_to_repository() = runTest {
        val repository = FakeProviderRepository()
        val useCase = ValidateAndAddProvider(
            providerSetupInputValidator = FakeProviderSetupInputValidator(
                m3uResult = Result.success(
                    ValidatedM3uProviderInput(
                        url = "file://playlist.m3u",
                        name = "Local Playlist",
                        httpUserAgent = "PlaylistAgent/2.0",
                        httpHeaders = "Referer: https://example.com"
                    )
                )
            ),
            providerRepository = repository,
            epgSourceRepository = FakeEpgSourceRepository()
        )

        val result = useCase.addM3u(
            M3uProviderSetupCommand(
                url = "file://playlist.m3u",
                name = "Local Playlist",
                httpUserAgent = "PlaylistAgent/2.0",
                httpHeaders = "Referer: https://example.com",
                epgSyncMode = ProviderEpgSyncMode.SKIP,
                existingProviderId = 11L
            )
        )

        assertThat(result).isInstanceOf(ValidateAndAddProviderResult.Success::class.java)
        assertThat(repository.lastM3uCall).isEqualTo(
            M3uCall(
                url = "file://playlist.m3u",
                name = "Local Playlist",
                httpUserAgent = "PlaylistAgent/2.0",
                httpHeaders = "Referer: https://example.com",
                epgSyncMode = ProviderEpgSyncMode.SKIP,
                m3uVodClassificationEnabled = false,
                m3uPlaylistKind = ProviderM3uPlaylistKind.LIVE,
                id = 11L
            )
        )
    }

    @Test
    fun auto_converts_xtream_playlist_url_to_xtream_login() = runTest {
        val repository = FakeProviderRepository()
        val useCase = ValidateAndAddProvider(
            providerSetupInputValidator = FakeProviderSetupInputValidator(
                m3uResult = Result.success(
                    ValidatedM3uProviderInput(
                        url = "http://extapk2302.shop:8080/get.php?username=Hakan1605&password=wg9daUwzfV&type=m3u_plus",
                        name = "Imported Playlist",
                        httpUserAgent = "PlaylistAgent/2.0",
                        httpHeaders = "Referer: https://example.com"
                    )
                )
            ),
            providerRepository = repository,
            epgSourceRepository = FakeEpgSourceRepository()
        )

        val result = useCase.addM3u(
            M3uProviderSetupCommand(
                url = "http://extapk2302.shop:8080/get.php?username=Hakan1605&password=wg9daUwzfV&type=m3u_plus",
                name = "Imported Playlist",
                epgSyncMode = ProviderEpgSyncMode.BACKGROUND,
                existingProviderId = 19L
            )
        )

        assertThat(result).isInstanceOf(ValidateAndAddProviderResult.Success::class.java)
        assertThat(repository.lastM3uCall).isNull()
        assertThat(repository.lastXtreamCall).isEqualTo(
            XtreamCall(
                serverUrl = "http://extapk2302.shop:8080",
                username = "Hakan1605",
                password = "wg9daUwzfV",
                name = "Imported Playlist",
                httpUserAgent = "PlaylistAgent/2.0",
                httpHeaders = "Referer: https://example.com",
                xtreamFastSyncEnabled = true,
                epgSyncMode = ProviderEpgSyncMode.BACKGROUND,
                xtreamLiveSyncMode = ProviderXtreamLiveSyncMode.AUTO,
                id = 19L
            )
        )
    }

    @Test
    fun vod_playlist_url_stays_m3u_even_when_it_looks_like_xtream() = runTest {
        val repository = FakeProviderRepository()
        val useCase = ValidateAndAddProvider(
            providerSetupInputValidator = FakeProviderSetupInputValidator(
                m3uResult = Result.success(
                    ValidatedM3uProviderInput(
                        url = "http://extapk2302.shop:8080/get.php?username=Hakan1605&password=wg9daUwzfV&type=m3u_plus",
                        name = "Imported VOD Playlist",
                        httpUserAgent = "",
                        httpHeaders = ""
                    )
                )
            ),
            providerRepository = repository,
            epgSourceRepository = FakeEpgSourceRepository()
        )

        val result = useCase.addM3u(
            M3uProviderSetupCommand(
                url = "http://extapk2302.shop:8080/get.php?username=Hakan1605&password=wg9daUwzfV&type=m3u_plus",
                name = "Imported VOD Playlist",
                m3uPlaylistKind = ProviderM3uPlaylistKind.VOD
            )
        )

        assertThat(result).isInstanceOf(ValidateAndAddProviderResult.Success::class.java)
        assertThat(repository.lastXtreamCall).isNull()
        assertThat(repository.lastM3uCall?.m3uVodClassificationEnabled).isTrue()
        assertThat(repository.lastM3uCall?.m3uPlaylistKind).isEqualTo(ProviderM3uPlaylistKind.VOD)
    }

    @Test
    fun rejects_xtream_playlist_url_with_embedded_credentials_in_authority() = runTest {
        val repository = FakeProviderRepository()
        val useCase = ValidateAndAddProvider(
            providerSetupInputValidator = FakeProviderSetupInputValidator(
                m3uResult = Result.success(
                    ValidatedM3uProviderInput(
                        url = "http://tvappapk@extapk2302.shop:8080/get.php?username=Hakan1605&password=wg9daUwzfV&type=m3u_plus",
                        name = "Imported Playlist",
                        httpUserAgent = "",
                        httpHeaders = ""
                    )
                )
            ),
            providerRepository = repository,
            epgSourceRepository = FakeEpgSourceRepository()
        )

        val result = useCase.addM3u(
            M3uProviderSetupCommand(
                url = "http://tvappapk@extapk2302.shop:8080/get.php?username=Hakan1605&password=wg9daUwzfV&type=m3u_plus",
                name = "Imported Playlist"
            )
        )

        assertThat(result).isInstanceOf(ValidateAndAddProviderResult.ValidationError::class.java)
        assertThat((result as ValidateAndAddProviderResult.ValidationError).message)
            .isEqualTo("Playlist sources must not include embedded credentials in the URL authority.")
        assertThat(repository.lastXtreamCall).isNull()
    }

    @Test
    fun rejects_xtream_playlist_with_oversized_decoded_password() = runTest {
        val repository = FakeProviderRepository()
        val oversizedPassword = "p".repeat(257)
        val encodedPassword = java.net.URLEncoder.encode(oversizedPassword, java.nio.charset.StandardCharsets.UTF_8.name())
        val useCase = ValidateAndAddProvider(
            providerSetupInputValidator = FakeProviderSetupInputValidator(
                m3uResult = Result.success(
                    ValidatedM3uProviderInput(
                        url = "https://example.com/get.php?username=user&password=$encodedPassword&type=m3u_plus",
                        name = "Imported Playlist",
                        httpUserAgent = "",
                        httpHeaders = ""
                    )
                )
            ),
            providerRepository = repository,
            epgSourceRepository = FakeEpgSourceRepository()
        )

        val result = useCase.addM3u(
            M3uProviderSetupCommand(
                url = "https://example.com/get.php?username=user&password=$encodedPassword&type=m3u_plus",
                name = "Imported Playlist"
            )
        )

        assertThat(result).isInstanceOf(ValidateAndAddProviderResult.ValidationError::class.java)
        assertThat((result as ValidateAndAddProviderResult.ValidationError).message)
            .isEqualTo("Playlist password is too long.")
        assertThat(repository.lastXtreamCall).isNull()
        assertThat(repository.lastM3uCall).isNull()
    }

    @Test
    fun delegates_validated_stalker_input_to_repository() = runTest {
        val repository = FakeProviderRepository()
        val useCase = ValidateAndAddProvider(
            providerSetupInputValidator = FakeProviderSetupInputValidator(
                stalkerResult = Result.success(
                    ValidatedStalkerProviderInput(
                        portalUrl = "https://portal.example.com",
                        macAddress = "00:1A:79:12:34:56",
                        name = "MAG",
                        deviceProfile = "MAG250",
                        timezone = "UTC",
                        locale = "en"
                    )
                )
            ),
            providerRepository = repository,
            epgSourceRepository = FakeEpgSourceRepository()
        )

        val result = useCase.loginStalker(
            StalkerProviderSetupCommand(
                portalUrl = " https://portal.example.com ",
                macAddress = "00-1a-79-12-34-56",
                name = " MAG ",
                deviceProfile = " MAG250 ",
                timezone = " UTC ",
                locale = " en ",
                epgSyncMode = ProviderEpgSyncMode.BACKGROUND,
                existingProviderId = 21L
            )
        )

        assertThat(result).isInstanceOf(ValidateAndAddProviderResult.Success::class.java)
        assertThat(repository.lastStalkerCall).isEqualTo(
            StalkerCall(
                portalUrl = "https://portal.example.com",
                macAddress = "00:1A:79:12:34:56",
                name = "MAG",
                deviceProfile = "MAG250",
                timezone = "UTC",
                locale = "en",
                epgSyncMode = ProviderEpgSyncMode.BACKGROUND,
                id = 21L
            )
        )
    }
}

private class FakeProviderSetupInputValidator(
    private val xtreamResult: Result<ValidatedXtreamProviderInput> = Result.success(
        ValidatedXtreamProviderInput(
            serverUrl = "https://example.com",
            username = "user",
            password = "secret",
            name = "Provider",
            httpUserAgent = "",
            httpHeaders = ""
        )
    ),
    private val m3uResult: Result<ValidatedM3uProviderInput> = Result.success(
        ValidatedM3uProviderInput(
            url = "https://example.com/playlist.m3u",
            name = "Playlist",
            httpUserAgent = "",
            httpHeaders = ""
        )
    ),
    private val stalkerResult: Result<ValidatedStalkerProviderInput> = Result.success(
        ValidatedStalkerProviderInput(
            portalUrl = "https://portal.example.com",
            macAddress = "00:1A:79:12:34:56",
            name = "Provider",
            deviceProfile = "MAG250",
            timezone = "UTC",
            locale = "en"
        )
    )
) : ProviderSetupInputValidator {
    override fun validateXtream(
        serverUrl: String,
        username: String,
        password: String,
        allowBlankPassword: Boolean,
        name: String,
        httpUserAgent: String,
        httpHeaders: String
    ): Result<ValidatedXtreamProviderInput> = xtreamResult

    override fun validateM3u(
        url: String,
        name: String,
        httpUserAgent: String,
        httpHeaders: String
    ): Result<ValidatedM3uProviderInput> = m3uResult

    override fun validateStalker(
        portalUrl: String,
        macAddress: String,
        name: String,
        deviceProfile: String,
        timezone: String,
        locale: String
    ): Result<ValidatedStalkerProviderInput> = stalkerResult
}

private data class XtreamCall(
    val serverUrl: String,
    val username: String,
    val password: String,
    val name: String,
    val httpUserAgent: String,
    val httpHeaders: String,
    val xtreamFastSyncEnabled: Boolean,
    val epgSyncMode: ProviderEpgSyncMode,
    val xtreamLiveSyncMode: ProviderXtreamLiveSyncMode,
    val id: Long?
)

private data class M3uCall(
    val url: String,
    val name: String,
    val httpUserAgent: String,
    val httpHeaders: String,
    val epgSyncMode: ProviderEpgSyncMode,
    val m3uVodClassificationEnabled: Boolean,
    val m3uPlaylistKind: ProviderM3uPlaylistKind,
    val id: Long?
)

private data class StalkerCall(
    val portalUrl: String,
    val macAddress: String,
    val name: String,
    val deviceProfile: String,
    val timezone: String,
    val locale: String,
    val epgSyncMode: ProviderEpgSyncMode,
    val id: Long?
)

private class FakeProviderRepository : ProviderRepository {
    var lastXtreamCall: XtreamCall? = null
    var lastM3uCall: M3uCall? = null
    var lastStalkerCall: StalkerCall? = null
    var xtreamResult: Result<Provider>? = null
    var m3uResult: Result<Provider>? = null
    var stalkerResult: Result<Provider>? = null

    override fun getProviders(): Flow<List<Provider>> = flowOf(emptyList())

    override fun getActiveProvider(): Flow<Provider?> = flowOf(null)

    override suspend fun getProvider(id: Long): Provider? = null

    override suspend fun addProvider(provider: Provider): Result<Long> = error("Not used in test")

    override suspend fun updateProvider(provider: Provider): Result<Unit> = error("Not used in test")

    override suspend fun deleteProvider(id: Long): Result<Unit> = error("Not used in test")

    override suspend fun setActiveProvider(id: Long): Result<Unit> = error("Not used in test")

    override suspend fun loginXtream(
        serverUrl: String,
        username: String,
        password: String,
        name: String,
        httpUserAgent: String,
        httpHeaders: String,
        xtreamFastSyncEnabled: Boolean,
        epgSyncMode: ProviderEpgSyncMode,
        xtreamLiveSyncMode: ProviderXtreamLiveSyncMode,
        onProgress: ((String) -> Unit)?,
        id: Long?
    ): Result<Provider> {
        lastXtreamCall = XtreamCall(serverUrl, username, password, name, httpUserAgent, httpHeaders, xtreamFastSyncEnabled, epgSyncMode, xtreamLiveSyncMode, id)
        return xtreamResult ?: Result.success(provider(id = id ?: 1L, name = name, type = ProviderType.XTREAM_CODES))
    }

    override suspend fun validateM3u(
        url: String,
        name: String,
        httpUserAgent: String,
        httpHeaders: String,
        epgSyncMode: ProviderEpgSyncMode,
        m3uVodClassificationEnabled: Boolean,
        m3uPlaylistKind: ProviderM3uPlaylistKind,
        onProgress: ((String) -> Unit)?,
        id: Long?
    ): Result<Provider> {
        lastM3uCall = M3uCall(url, name, httpUserAgent, httpHeaders, epgSyncMode, m3uVodClassificationEnabled, m3uPlaylistKind, id)
        return m3uResult ?: Result.success(provider(id = id ?: 2L, name = name, type = ProviderType.M3U, m3uUrl = url, m3uPlaylistKind = m3uPlaylistKind))
    }

    override suspend fun loginStalker(
        portalUrl: String,
        macAddress: String,
        name: String,
        deviceProfile: String,
        timezone: String,
        locale: String,
        epgSyncMode: ProviderEpgSyncMode,
        onProgress: ((String) -> Unit)?,
        id: Long?
    ): Result<Provider> {
        lastStalkerCall = StalkerCall(
            portalUrl = portalUrl,
            macAddress = macAddress,
            name = name,
            deviceProfile = deviceProfile,
            timezone = timezone,
            locale = locale,
            epgSyncMode = epgSyncMode,
            id = id
        )
        return stalkerResult ?: Result.success(
            provider(id = id ?: 3L, name = name, type = ProviderType.STALKER_PORTAL).copy(
                serverUrl = portalUrl,
                stalkerMacAddress = macAddress,
                stalkerDeviceProfile = deviceProfile,
                stalkerDeviceTimezone = timezone,
                stalkerDeviceLocale = locale
            )
        )
    }

    override suspend fun refreshProviderData(
        providerId: Long,
        force: Boolean,
        movieFastSyncOverride: Boolean?,
        epgSyncModeOverride: ProviderEpgSyncMode?,
        onProgress: ((String) -> Unit)?
    ): Result<Unit> = error("Not used in test")

    override suspend fun getProgramsForLiveStream(
        providerId: Long,
        streamId: Long,
        epgChannelId: String?,
        limit: Int
    ): Result<List<Program>> = error("Not used in test")

    override suspend fun buildCatchUpUrl(providerId: Long, streamId: Long, start: Long, end: Long): String? = null

    fun provider(
        id: Long,
        name: String,
        type: ProviderType,
        m3uUrl: String = "",
        m3uPlaylistKind: ProviderM3uPlaylistKind = ProviderM3uPlaylistKind.LIVE
    ) = Provider(
        id = id,
        name = name,
        type = type,
        serverUrl = if (type == ProviderType.M3U) m3uUrl else "https://example.com",
        username = if (type == ProviderType.XTREAM_CODES) "user" else "",
        m3uUrl = m3uUrl,
        m3uPlaylistKind = m3uPlaylistKind,
        status = ProviderStatus.ACTIVE
    )
}

/**
 * Minimal in-memory [EpgSourceRepository] used by [ValidateAndAddProviderTest].
 * Records add + assign calls so tests can assert that the M3U + EPG flow attached
 * the EPG source to the newly-added provider when an `epgUrl` was supplied.
 */
private class FakeEpgSourceRepository : EpgSourceRepository {
    val added = mutableListOf<Pair<String, String>>()
    val assignments = mutableListOf<Triple<Long, Long, Int>>()
    private var nextId = 1L

    override fun getAllSources(): Flow<List<EpgSource>> = flowOf(emptyList())
    override suspend fun getSourceById(id: Long): EpgSource? = null
    override suspend fun addSource(name: String, url: String): Result<EpgSource> {
        added += name to url
        return Result.success(EpgSource(id = nextId++, name = name, url = url))
    }
    override suspend fun updateSource(source: EpgSource): Result<Unit> = Result.success(Unit)
    override suspend fun deleteSource(id: Long) = Unit
    override suspend fun setSourceEnabled(id: Long, enabled: Boolean) = Unit
    override suspend fun getProviderIdsForSource(sourceId: Long): List<Long> = emptyList()
    override fun getAssignmentsForProvider(providerId: Long): Flow<List<ProviderEpgSourceAssignment>> = flowOf(emptyList())
    override suspend fun assignSourceToProvider(providerId: Long, epgSourceId: Long, priority: Int): Result<Unit> {
        assignments += Triple(providerId, epgSourceId, priority)
        return Result.success(Unit)
    }
    override suspend fun unassignSourceFromProvider(providerId: Long, epgSourceId: Long) = Unit
    override suspend fun updateAssignmentPriority(providerId: Long, epgSourceId: Long, priority: Int) = Unit
    override suspend fun swapAssignmentPriorities(
        providerId: Long,
        epgSourceId1: Long,
        newPriority1: Int,
        epgSourceId2: Long,
        newPriority2: Int,
    ) = Unit
    override suspend fun refreshSource(sourceId: Long): Result<Unit> = Result.success(Unit)
    override suspend fun refreshAllForProvider(providerId: Long): Result<Unit> = Result.success(Unit)
    override suspend fun resolveForProvider(
        providerId: Long,
        hiddenLiveCategoryIds: Set<Long>,
    ): com.afterglowtv.domain.model.EpgResolutionSummary =
        com.afterglowtv.domain.model.EpgResolutionSummary()
    override suspend fun getResolutionSummary(providerId: Long): com.afterglowtv.domain.model.EpgResolutionSummary =
        com.afterglowtv.domain.model.EpgResolutionSummary()
    override suspend fun getChannelMapping(providerId: Long, channelId: Long): com.afterglowtv.domain.model.ChannelEpgMapping? = null
    override suspend fun getOverrideCandidates(
        providerId: Long,
        query: String,
        limit: Int,
    ): List<com.afterglowtv.domain.model.EpgOverrideCandidate> = emptyList()
    override suspend fun applyManualOverride(
        providerId: Long,
        channelId: Long,
        epgSourceId: Long,
        xmltvChannelId: String,
    ): Result<Unit> = Result.success(Unit)
    override suspend fun clearManualOverride(providerId: Long, channelId: Long): Result<Unit> = Result.success(Unit)
    override suspend fun getResolvedProgramsForChannels(
        providerId: Long,
        channelIds: List<Long>,
        startTime: Long,
        endTime: Long,
    ): Map<String, List<Program>> = emptyMap()
}
