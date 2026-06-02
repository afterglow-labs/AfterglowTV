package com.afterglowtv.data.local

import androidx.room.TypeConverter
import com.afterglowtv.domain.model.ContentType
import com.afterglowtv.domain.model.LocalMediaKind
import com.afterglowtv.domain.model.LocalMediaLibrarySourceType
import com.afterglowtv.domain.model.ProviderEpgSyncMode
import com.afterglowtv.domain.model.ProviderM3uPlaylistKind
import com.afterglowtv.domain.model.ProviderStatus
import com.afterglowtv.domain.model.ProviderType
import com.afterglowtv.domain.model.ProviderXtreamLiveSyncMode
import java.util.logging.Logger

class RoomEnumConverters {
    private companion object {
        val logger: Logger = Logger.getLogger(RoomEnumConverters::class.java.name)
    }

    @TypeConverter
    fun fromProviderType(value: ProviderType?): String? = value?.name

    @TypeConverter
    fun toProviderType(value: String?): ProviderType? =
        enumValueOrDefault(value, ProviderType.M3U, providerTypeAliases())

    @TypeConverter
    fun fromProviderStatus(value: ProviderStatus?): String? = value?.name

    @TypeConverter
    fun toProviderStatus(value: String?): ProviderStatus? = enumValueOrDefault(value, ProviderStatus.UNKNOWN)

    @TypeConverter
    fun fromProviderEpgSyncMode(value: ProviderEpgSyncMode?): String? = value?.name

    @TypeConverter
    fun toProviderEpgSyncMode(value: String?): ProviderEpgSyncMode? =
        enumValueOrDefault(value, ProviderEpgSyncMode.SKIP, providerEpgSyncModeAliases())

    @TypeConverter
    fun fromProviderXtreamLiveSyncMode(value: ProviderXtreamLiveSyncMode?): String? = value?.name

    @TypeConverter
    fun toProviderXtreamLiveSyncMode(value: String?): ProviderXtreamLiveSyncMode? =
        enumValueOrDefault(value, ProviderXtreamLiveSyncMode.AUTO, providerXtreamLiveSyncModeAliases())

    @TypeConverter
    fun fromProviderM3uPlaylistKind(value: ProviderM3uPlaylistKind?): String? = value?.name

    @TypeConverter
    fun toProviderM3uPlaylistKind(value: String?): ProviderM3uPlaylistKind? =
        enumValueOrDefault(value, ProviderM3uPlaylistKind.LIVE, providerM3uPlaylistKindAliases())

    @TypeConverter
    fun fromContentType(value: ContentType?): String? = value?.name

    @TypeConverter
    fun toContentType(value: String?): ContentType? =
        enumValueOrDefault(value, ContentType.LIVE, contentTypeAliases())

    @TypeConverter
    fun fromLocalMediaKind(value: LocalMediaKind?): String? = value?.name

    @TypeConverter
    fun toLocalMediaKind(value: String?): LocalMediaKind? =
        enumValueOrDefault(value, LocalMediaKind.UNKNOWN, localMediaKindAliases())

    @TypeConverter
    fun fromLocalMediaLibrarySourceType(value: LocalMediaLibrarySourceType?): String? = value?.name

    @TypeConverter
    fun toLocalMediaLibrarySourceType(value: String?): LocalMediaLibrarySourceType? =
        enumValueOrDefault(value, LocalMediaLibrarySourceType.DOCUMENT_TREE, localMediaLibrarySourceTypeAliases())

    private inline fun <reified T : Enum<T>> enumValueOrDefault(
        value: String?,
        defaultValue: T,
        aliases: Map<String, T> = emptyMap()
    ): T? {
        if (value == null) return null
        val normalizedValue = value.trim()
        aliases[normalizedValue.uppercase()]?.let { return it }
        return enumValues<T>().firstOrNull { candidate ->
            candidate.name.equals(normalizedValue, ignoreCase = true)
        } ?: defaultValue.also {
            logger.warning(
                "Unknown ${T::class.java.simpleName} value '$value'; defaulting to ${defaultValue.name}"
            )
        }
    }

    private fun providerTypeAliases(): Map<String, ProviderType> = mapOf(
        "XTREAM" to ProviderType.XTREAM_CODES,
        "XTREAM_CODES_API" to ProviderType.XTREAM_CODES,
        "STALKER" to ProviderType.STALKER_PORTAL,
        "STB" to ProviderType.STALKER_PORTAL,
        "PLAYLIST" to ProviderType.M3U
    )

    private fun providerEpgSyncModeAliases(): Map<String, ProviderEpgSyncMode> = mapOf(
        "DISABLED" to ProviderEpgSyncMode.SKIP,
        "OFF" to ProviderEpgSyncMode.SKIP,
        "FOREGROUND" to ProviderEpgSyncMode.UPFRONT
    )

    private fun providerXtreamLiveSyncModeAliases(): Map<String, ProviderXtreamLiveSyncMode> = mapOf(
        "SEGMENTED" to ProviderXtreamLiveSyncMode.CATEGORY_BY_CATEGORY,
        "CATEGORY" to ProviderXtreamLiveSyncMode.CATEGORY_BY_CATEGORY,
        "CATEGORIES" to ProviderXtreamLiveSyncMode.CATEGORY_BY_CATEGORY,
        "FULL" to ProviderXtreamLiveSyncMode.STREAM_ALL,
        "FULL_CATALOG" to ProviderXtreamLiveSyncMode.STREAM_ALL,
        "STREAM" to ProviderXtreamLiveSyncMode.STREAM_ALL
    )

    private fun providerM3uPlaylistKindAliases(): Map<String, ProviderM3uPlaylistKind> = mapOf(
        "TV" to ProviderM3uPlaylistKind.LIVE,
        "LIVE_TV" to ProviderM3uPlaylistKind.LIVE,
        "MOVIE" to ProviderM3uPlaylistKind.VOD,
        "MOVIES" to ProviderM3uPlaylistKind.VOD,
        "ON_DEMAND" to ProviderM3uPlaylistKind.VOD
    )

    private fun contentTypeAliases(): Map<String, ContentType> = mapOf(
        "EPISODE" to ContentType.SERIES_EPISODE,
        "SHOW" to ContentType.SERIES,
        "VOD" to ContentType.MOVIE,
        "CHANNEL" to ContentType.LIVE
    )

    private fun localMediaKindAliases(): Map<String, LocalMediaKind> = mapOf(
        "SERIES_EPISODE" to LocalMediaKind.EPISODE,
        "TV" to LocalMediaKind.EPISODE,
        "VOD" to LocalMediaKind.MOVIE
    )

    private fun localMediaLibrarySourceTypeAliases(): Map<String, LocalMediaLibrarySourceType> = mapOf(
        "FOLDER" to LocalMediaLibrarySourceType.DOCUMENT_TREE,
        "SAF" to LocalMediaLibrarySourceType.DOCUMENT_TREE,
        "NETWORK" to LocalMediaLibrarySourceType.SMB,
        "NETWORK_SHARE" to LocalMediaLibrarySourceType.SMB,
        "CIFS" to LocalMediaLibrarySourceType.SMB
    )
}
