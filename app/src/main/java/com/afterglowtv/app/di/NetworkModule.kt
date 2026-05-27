package com.afterglowtv.app.di

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import com.afterglowtv.app.BuildConfig
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.afterglowtv.data.remote.NetworkTimeoutConfig
import com.afterglowtv.data.remote.http.DefaultUserAgentInterceptor
import com.afterglowtv.data.remote.http.buildAppRequestProfile
import com.afterglowtv.data.remote.http.buildAppUserAgent
import com.afterglowtv.data.remote.stalker.OkHttpStalkerApiService
import com.afterglowtv.data.remote.stalker.StalkerApiService
import com.afterglowtv.data.remote.xtream.XtreamApiService
import com.afterglowtv.data.remote.xtream.OkHttpXtreamApiService
import com.afterglowtv.data.remote.xtream.XtreamUrlFactory
import com.afterglowtv.data.parser.XmltvParser
import com.afterglowtv.player.Media3PlayerEngine
import com.afterglowtv.player.PlayerEngine
import com.afterglowtv.player.adaptive.AdaptiveBufferController
import com.afterglowtv.player.adaptive.AdaptivePlaybackRecorder
import com.afterglowtv.player.adaptive.ConnectionPrewarmer
import com.afterglowtv.player.adaptive.NetworkClassDetector
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.io.File
import java.util.concurrent.TimeUnit.SECONDS
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(
        @ApplicationContext context: Context
    ): OkHttpClient {
        val appUserAgent = buildAppUserAgent(BuildConfig.VERSION_NAME)
        val isDebuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        val loggingLevel = if (isDebuggable) {
            HttpLoggingInterceptor.Level.BASIC
        } else {
            HttpLoggingInterceptor.Level.NONE
        }

        val httpLogger = HttpLoggingInterceptor { message ->
            Log.d("OkHttp", XtreamUrlFactory.sanitizeLogMessage(message))
        }.apply {
            level = loggingLevel
        }

        return OkHttpClient.Builder()
            .cache(
                Cache(
                    directory = httpCacheDirectory(context.applicationInfo.dataDir),
                    maxSize = 256L * 1024 * 1024
                )
            )
            // Explicitly prefer HTTP/2. OkHttp negotiates via ALPN — IPTV
            // origins that support H2 get multiplexed segment fetches over
            // a single connection (no head-of-line blocking when one segment
            // is slow). Origins that don't support H2 fall back to HTTP/1.1.
            .protocols(listOf(okhttp3.Protocol.HTTP_2, okhttp3.Protocol.HTTP_1_1))
            // App-level DNS cache wraps the system resolver. The system
            // cache TTL is ~30s on most Android builds, which is shorter
            // than a typical IPTV session — every channel switch beyond
            // that window pays a fresh DNS round-trip. Our 5-minute TTL
            // turns that into a no-op for the steady-state user.
            .dns(CachingDns())
            .connectTimeout(NetworkTimeoutConfig.CONNECT_TIMEOUT_SECONDS, SECONDS)
            .readTimeout(NetworkTimeoutConfig.READ_TIMEOUT_SECONDS, SECONDS)
            .writeTimeout(NetworkTimeoutConfig.WRITE_TIMEOUT_SECONDS, SECONDS)
            .addInterceptor(DefaultUserAgentInterceptor(appUserAgent))
            .addInterceptor(httpLogger)
            .followRedirects(true)
            .followSslRedirects(true)
            // 20 idle / 10-min keep-alive — sized for Multi-View and parallel
            // EPG / catalog fetches that all hit the same IPTV host. The default
            // (5/5min) churns connections on every channel switch; reusing
            // pooled connections cuts handshake latency by 100-300 ms.
            .connectionPool(okhttp3.ConnectionPool(20, 10, java.util.concurrent.TimeUnit.MINUTES))
            .dispatcher(okhttp3.Dispatcher().apply {
                maxRequests = 64
                // Per-host cap raised to 16 — Multi-View can open up to 9
                // streams in parallel plus catalog/EPG fetches.
                maxRequestsPerHost = 16
            })
            .retryOnConnectionFailure(true)
            .build()
    }

    @Provides
    @Singleton
    fun provideXtreamApiService(okHttpClient: OkHttpClient, xtreamJson: Json): XtreamApiService =
        OkHttpXtreamApiService(
            client = okHttpClient,
            json = xtreamJson,
            defaultRequestProfile = buildAppRequestProfile(BuildConfig.VERSION_NAME, ownerTag = "app/xtream")
        )

    @Provides
    @Singleton
    fun provideStalkerApiService(okHttpClient: OkHttpClient, xtreamJson: Json): StalkerApiService =
        OkHttpStalkerApiService(okHttpClient, xtreamJson)

    @Provides
    @Singleton
    fun provideXtreamJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideXmltvParser(): XmltvParser = XmltvParser()

    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder().create()

    @Provides
    @Singleton
    @MainPlayerEngine
    fun provideMainPlayerEngine(
        @ApplicationContext context: Context,
        okHttpClient: OkHttpClient,
        playbackCompatibilityRepository: com.afterglowtv.domain.repository.PlaybackCompatibilityRepository,
        adaptiveRecorder: AdaptivePlaybackRecorder,
        networkClassDetector: NetworkClassDetector,
        connectionPrewarmer: ConnectionPrewarmer,
        adaptiveBufferController: AdaptiveBufferController,
        smbMediaSourceResolver: com.afterglowtv.domain.model.SmbMediaSourceResolver,
    ): PlayerEngine = Media3PlayerEngine(
        context = context,
        okHttpClient = okHttpClient,
        playbackCompatibilityRepository = playbackCompatibilityRepository,
        adaptiveRecorder = adaptiveRecorder,
        networkClassDetector = networkClassDetector,
        connectionPrewarmer = connectionPrewarmer,
        adaptiveBufferController = adaptiveBufferController,
        smbMediaSourceResolver = smbMediaSourceResolver,
    )

    /**
     * Factory binding for preview and multiview playback.
     * Each Provider.get() call returns a fresh engine instance.
     */
    @Provides
    @AuxiliaryPlayerEngine
    fun provideAuxiliaryPlayerEngine(
        @ApplicationContext context: Context,
        okHttpClient: OkHttpClient,
        playbackCompatibilityRepository: com.afterglowtv.domain.repository.PlaybackCompatibilityRepository,
        adaptiveRecorder: AdaptivePlaybackRecorder,
        networkClassDetector: NetworkClassDetector,
        connectionPrewarmer: ConnectionPrewarmer,
        adaptiveBufferController: AdaptiveBufferController,
        smbMediaSourceResolver: com.afterglowtv.domain.model.SmbMediaSourceResolver,
    ): PlayerEngine = Media3PlayerEngine(
        context = context,
        okHttpClient = okHttpClient,
        playbackCompatibilityRepository = playbackCompatibilityRepository,
        adaptiveRecorder = adaptiveRecorder,
        networkClassDetector = networkClassDetector,
        connectionPrewarmer = connectionPrewarmer,
        adaptiveBufferController = adaptiveBufferController,
        smbMediaSourceResolver = smbMediaSourceResolver,
    ).apply {
        enableMediaSession = false
        bypassAudioFocus = true
    }
}

internal fun httpCacheDirectory(dataDir: String): File =
    File(File(dataDir, "cache"), "afterglowtv_http_cache")
