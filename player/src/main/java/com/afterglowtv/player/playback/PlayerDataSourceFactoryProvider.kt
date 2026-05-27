package com.afterglowtv.player.playback

import android.content.Context
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import com.afterglowtv.domain.model.NoopSmbMediaSourceResolver
import com.afterglowtv.domain.model.SmbMediaSourceResolver
import com.afterglowtv.domain.model.StreamInfo
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient

class PlayerDataSourceFactoryProvider(
    private val context: Context,
    private val baseClient: OkHttpClient,
    private val smbMediaSourceResolver: SmbMediaSourceResolver = NoopSmbMediaSourceResolver
) {
    private val clientsByProfile = ConcurrentHashMap<PlayerTimeoutProfile, OkHttpClient>()

    fun createFactory(
        streamInfo: StreamInfo,
        resolvedStreamType: ResolvedStreamType,
        preload: Boolean = false
    ): Pair<PlayerTimeoutProfile, DataSource.Factory> {
        val profile = PlayerTimeoutProfile.resolve(streamInfo, resolvedStreamType, preload)
        val headers = streamInfo.headers
        val client = clientsByProfile.computeIfAbsent(profile) {
            baseClient.newBuilder()
                .connectTimeout(profile.connectTimeoutMs, TimeUnit.MILLISECONDS)
                .readTimeout(profile.readTimeoutMs, TimeUnit.MILLISECONDS)
                .writeTimeout(profile.writeTimeoutMs, TimeUnit.MILLISECONDS)
                .build()
        }
        val upstreamFactory = OkHttpDataSource.Factory(client).apply {
            streamInfo.userAgent?.takeIf { it.isNotBlank() }?.let(::setUserAgent)
            if (headers.isNotEmpty()) {
                setDefaultRequestProperties(headers)
            }
        }
        val defaultFactory = DefaultDataSource.Factory(context, upstreamFactory)
        val factory = SmbAwareDataSourceFactory(defaultFactory, smbMediaSourceResolver)
        return profile to factory
    }
}
