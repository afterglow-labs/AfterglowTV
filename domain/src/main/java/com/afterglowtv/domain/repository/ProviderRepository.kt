package com.afterglowtv.domain.repository

import com.afterglowtv.domain.model.Program
import com.afterglowtv.domain.model.Provider
import com.afterglowtv.domain.model.ProviderEpgSyncMode
import com.afterglowtv.domain.model.ProviderM3uPlaylistKind
import com.afterglowtv.domain.model.ProviderXtreamLiveSyncMode
import com.afterglowtv.domain.model.Result
import kotlinx.coroutines.flow.Flow

data class LiveStreamProgramRequest(
    val streamId: Long,
    val epgChannelId: String? = null
)

interface ProviderRepository {
    fun getProviders(): Flow<List<Provider>>
    fun getActiveProvider(): Flow<Provider?>
    suspend fun getProvider(id: Long): Provider?
    suspend fun addProvider(provider: Provider): Result<Long>
    suspend fun updateProvider(provider: Provider): Result<Unit>
    suspend fun deleteProvider(id: Long): Result<Unit>
    suspend fun setActiveProvider(id: Long): Result<Unit>
    suspend fun loginXtream(serverUrl: String, username: String, password: String, name: String, httpUserAgent: String = "", httpHeaders: String = "", xtreamFastSyncEnabled: Boolean, epgSyncMode: ProviderEpgSyncMode = ProviderEpgSyncMode.BACKGROUND, xtreamLiveSyncMode: ProviderXtreamLiveSyncMode = ProviderXtreamLiveSyncMode.AUTO, onProgress: ((String) -> Unit)? = null, id: Long? = null): Result<Provider>
    suspend fun validateM3u(url: String, name: String, httpUserAgent: String = "", httpHeaders: String = "", epgSyncMode: ProviderEpgSyncMode = ProviderEpgSyncMode.BACKGROUND, m3uVodClassificationEnabled: Boolean = false, m3uPlaylistKind: ProviderM3uPlaylistKind = ProviderM3uPlaylistKind.LIVE, onProgress: ((String) -> Unit)? = null, id: Long? = null): Result<Provider>
    suspend fun loginStalker(
        portalUrl: String,
        macAddress: String,
        name: String,
        deviceProfile: String = "",
        timezone: String = "",
        locale: String = "",
        epgSyncMode: ProviderEpgSyncMode = ProviderEpgSyncMode.BACKGROUND,
        onProgress: ((String) -> Unit)? = null,
        id: Long? = null
    ): Result<Provider>
    suspend fun refreshProviderData(
        providerId: Long,
        force: Boolean = false,
        movieFastSyncOverride: Boolean? = null,
        epgSyncModeOverride: ProviderEpgSyncMode? = null,
        onProgress: ((String) -> Unit)? = null
    ): Result<Unit>
    suspend fun getProgramsForLiveStream(
        providerId: Long,
        streamId: Long,
        epgChannelId: String? = null,
        limit: Int = 12
    ): Result<List<Program>>
    suspend fun getProgramsForLiveStreams(
        providerId: Long,
        requests: List<LiveStreamProgramRequest>,
        limit: Int = 12
    ): Map<LiveStreamProgramRequest, Result<List<Program>>> =
        requests.distinct().associateWith { request ->
            getProgramsForLiveStream(
                providerId = providerId,
                streamId = request.streamId,
                epgChannelId = request.epgChannelId,
                limit = limit
            )
        }
    suspend fun buildCatchUpUrl(providerId: Long, streamId: Long, start: Long, end: Long): String?
    suspend fun buildCatchUpUrls(providerId: Long, streamId: Long, start: Long, end: Long): List<String> =
        listOfNotNull(buildCatchUpUrl(providerId, streamId, start, end))
}
