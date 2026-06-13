package com.afterglowtv.app

import android.app.SearchManager
import android.content.Intent
import android.app.PictureInPictureParams
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Build
import android.os.StrictMode
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.afterglowtv.app.cast.CastManager
import com.afterglowtv.app.cast.CastRouteChooserActivity
import com.afterglowtv.app.backup.BackupFileBridge
import com.afterglowtv.app.device.isTelevisionDevice
import com.afterglowtv.app.di.MainPlayerEngine
import com.afterglowtv.app.localization.resolveAppLocale
import com.afterglowtv.app.navigation.AppNavigation
import com.afterglowtv.app.navigation.ExternalDestination
import com.afterglowtv.app.navigation.ExternalNavigationRequest
import com.afterglowtv.app.navigation.PlayerNavigationRequest
import com.afterglowtv.app.player.PlayerNowPlayingStore
import com.afterglowtv.app.store.amazon.AmazonAppstoreBridge
import com.afterglowtv.app.tv.LauncherRecommendationsManager
import com.afterglowtv.app.tv.WatchNextManager
import com.afterglowtv.app.tvinput.TvInputChannelSyncManager
import com.afterglowtv.app.ui.theme.AfterglowTVTheme
import com.afterglowtv.app.ui.time.LocalAppTimeFormat
import com.afterglowtv.player.PlaybackState
import com.afterglowtv.player.PlayerEngine
import dagger.hilt.android.AndroidEntryPoint

import javax.inject.Inject
import com.afterglowtv.data.preferences.PreferencesRepository
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Density
import android.content.res.Configuration
import android.text.TextUtils
import android.view.View
import androidx.core.view.WindowCompat
import java.util.Locale
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.content.res.AssetManager
import android.content.res.Resources
import android.speech.RecognizerIntent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_PLAYER_REQUEST = "com.afterglowtv.app.extra.PLAYER_REQUEST"
        const val EXTRA_EXTERNAL_DESTINATION = "com.afterglowtv.app.extra.EXTERNAL_DESTINATION"
        const val EXTRA_EXTERNAL_ROUTE = "com.afterglowtv.app.extra.EXTERNAL_ROUTE"
        private const val MAX_PIP_ASPECT_RATIO = 2.39f
        private const val MIN_PIP_ASPECT_RATIO = 1f / MAX_PIP_ASPECT_RATIO
    }

    private data class PlayerPictureInPictureState(
        val enabled: Boolean = false,
        val isPlaying: Boolean = false,
        val aspectRatio: Rational? = null
    )

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    @Inject
    lateinit var watchNextManager: WatchNextManager

    @Inject
    lateinit var launcherRecommendationsManager: LauncherRecommendationsManager

    @Inject
    lateinit var tvInputChannelSyncManager: TvInputChannelSyncManager

    @Inject
    lateinit var castManager: CastManager

    @Inject
    @MainPlayerEngine
    lateinit var mainPlayerEngine: PlayerEngine

    @Inject
    lateinit var playerNowPlayingStore: PlayerNowPlayingStore

    private val _pictureInPictureModeFlow = MutableStateFlow(false)
    val pictureInPictureModeFlow: StateFlow<Boolean> = _pictureInPictureModeFlow.asStateFlow()

    private val _externalNavigationRequestFlow = MutableStateFlow<ExternalNavigationRequest?>(null)
    val externalNavigationRequestFlow: StateFlow<ExternalNavigationRequest?> =
        _externalNavigationRequestFlow.asStateFlow()

    private var playerPictureInPictureState = PlayerPictureInPictureState()
    private var resumePlayerAfterActivityStop = false

    override fun onCreate(savedInstanceState: Bundle?) {
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build()
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build()
            )
        }
        super.onCreate(savedInstanceState)
        // Disable legacy window-fitting so Compose receives IME insets directly.
        // This fixes keyboard-covers-input-field on API 30+ where adjustResize is
        // ignored when the theme sets windowFullscreen=true.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        _pictureInPictureModeFlow.value = isInPictureInPictureMode
        observePlayerPictureInPictureState()
        handleExternalIntent(intent)
        if (isTelevisionDevice()) {
            lifecycleScope.launch {
                watchNextManager.refreshWatchNext()
                launcherRecommendationsManager.refreshRecommendations()
                tvInputChannelSyncManager.refreshTvInputCatalog()
            }
        }
        setContent {
            val appLanguage by preferencesRepository.appLanguage.collectAsState(initial = "system")
            val appTimeFormat by preferencesRepository.appTimeFormat.collectAsState(initial = com.afterglowtv.domain.model.AppTimeFormat.SYSTEM)
            val currentContext = LocalContext.current
            
            val configuration = remember(appLanguage) {
                val locale = resolveAppLocale(
                    preferredLanguageTag = appLanguage,
                    baseConfiguration = this@MainActivity.resources.configuration
                )
                val conf = Configuration(this@MainActivity.resources.configuration)
                Locale.setDefault(locale)
                conf.setLocale(locale)
                conf.setLayoutDirection(locale)
                conf
            }
            val localizedContext = remember(configuration, currentContext) {
                val configurationContext = currentContext.createConfigurationContext(configuration)
                object : ContextWrapper(currentContext) {
                    override fun getResources(): Resources = configurationContext.resources
                    override fun getAssets(): AssetManager = configurationContext.assets
                    override fun getSystemService(name: String): Any? {
                        return if (name == Context.LAYOUT_INFLATER_SERVICE) {
                            configurationContext.getSystemService(name)
                        } else {
                            super.getSystemService(name)
                        }
                    }
                }
            }

            val layoutDirection = remember(configuration) {
                if (TextUtils.getLayoutDirectionFromLocale(configuration.locales[0]) == View.LAYOUT_DIRECTION_RTL) {
                    LayoutDirection.Rtl
                } else {
                    LayoutDirection.Ltr
                }
            }

            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalLayoutDirection provides layoutDirection,
                LocalAppTimeFormat provides appTimeFormat
            ) {
                AfterglowTVTheme {
                    AppNavigation(mainActivity = this@MainActivity)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleExternalIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        if (resumePlayerAfterActivityStop) {
            resumePlayerAfterActivityStop = false
            mainPlayerEngine.play()
        }
    }

    override fun onResume() {
        super.onResume()
        AmazonAppstoreBridge.refreshEntitlements()
    }

    override fun onStop() {
        val keepingPlaybackForPictureInPicture = supportsPictureInPicture() &&
            playerPictureInPictureState.enabled
        if (!isInPictureInPictureMode && !keepingPlaybackForPictureInPicture && mainPlayerEngine.isPlaying.value) {
            resumePlayerAfterActivityStop = true
            mainPlayerEngine.pause()
        }
        super.onStop()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        enterPlayerPictureInPictureModeIfEligible()
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        _pictureInPictureModeFlow.value = isInPictureInPictureMode
    }

    fun updatePlayerPictureInPictureState(
        enabled: Boolean,
        isPlaying: Boolean,
        videoWidth: Int,
        videoHeight: Int,
        pixelWidthHeightRatio: Float = 1f
    ) {
        if (!supportsPictureInPicture()) return
        playerPictureInPictureState = PlayerPictureInPictureState(
            enabled = enabled,
            isPlaying = isPlaying,
            aspectRatio = videoAspectRatioOrNull(videoWidth, videoHeight, pixelWidthHeightRatio)
        )
        applyPlayerPictureInPictureParams()
    }

    fun clearPlayerPictureInPictureState() {
        if (!supportsPictureInPicture()) return
        playerPictureInPictureState = PlayerPictureInPictureState()
        applyPlayerPictureInPictureParams()
    }

    fun clearExternalNavigationRequest() {
        _externalNavigationRequestFlow.value = null
    }

    fun openPlayer(request: PlayerNavigationRequest) {
        _externalNavigationRequestFlow.value = ExternalNavigationRequest.Player(request)
    }

    fun enterPlayerPictureInPictureModeFromPlayer(): Boolean {
        return enterPlayerPictureInPictureModeIfEligible(requirePlaying = false)
    }

    fun openCastRouteChooser() {
        startActivity(Intent(this, CastRouteChooserActivity::class.java))
    }

    private fun enterPlayerPictureInPictureModeIfEligible(requirePlaying: Boolean = true): Boolean {
        if (!supportsPictureInPicture() || isInPictureInPictureMode) {
            return false
        }
        val state = playerPictureInPictureState
        if (!state.enabled || (requirePlaying && !state.isPlaying)) {
            return false
        }
        val params = buildPlayerPictureInPictureParams(state)
        return runCatching {
            setPictureInPictureParams(params)
            enterPictureInPictureMode(params)
        }.getOrDefault(false)
    }

    private fun applyPlayerPictureInPictureParams() {
        if (!supportsPictureInPicture()) return
        runCatching {
            setPictureInPictureParams(buildPlayerPictureInPictureParams(playerPictureInPictureState))
        }
    }

    private fun observePlayerPictureInPictureState() {
        lifecycleScope.launch {
            combine(
                playerNowPlayingStore.state,
                mainPlayerEngine.playbackState,
                mainPlayerEngine.isPlaying,
                mainPlayerEngine.videoFormat
            ) { nowPlaying, playbackState, isPlaying, videoFormat ->
                val hasRenderablePlayback = playbackState == PlaybackState.READY ||
                    playbackState == PlaybackState.BUFFERING
                PlayerPictureInPictureState(
                    enabled = nowPlaying.active && nowPlaying.isLive && hasRenderablePlayback,
                    isPlaying = isPlaying,
                    aspectRatio = videoAspectRatioOrNull(
                        videoWidth = videoFormat.width,
                        videoHeight = videoFormat.height,
                        pixelWidthHeightRatio = videoFormat.pixelWidthHeightRatio
                    )
                )
            }.collect { state ->
                playerPictureInPictureState = state
                applyPlayerPictureInPictureParams()
            }
        }
    }

    private fun buildPlayerPictureInPictureParams(
        state: PlayerPictureInPictureState
    ): PictureInPictureParams {
        val builder = PictureInPictureParams.Builder()
        state.aspectRatio?.let { builder.setAspectRatio(it) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(state.enabled && state.isPlaying)
        }
        return builder.build()
    }

    private fun videoAspectRatioOrNull(
        videoWidth: Int,
        videoHeight: Int,
        pixelWidthHeightRatio: Float = 1f
    ): Rational? {
        if (videoWidth <= 0 || videoHeight <= 0) return null
        val safePixelRatio = pixelWidthHeightRatio.takeIf { it.isFinite() && it > 0f } ?: 1f
        val rawAspectRatio = (videoWidth * safePixelRatio) / videoHeight.toFloat()
        val clampedAspectRatio = rawAspectRatio
            .coerceIn(MIN_PIP_ASPECT_RATIO, MAX_PIP_ASPECT_RATIO)
        val numerator = (clampedAspectRatio * 10_000).toInt().coerceAtLeast(1)
        return Rational(numerator, 10_000)
    }

    private fun supportsPictureInPicture(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
    }

    private fun handleExternalIntent(intent: Intent?) {
        val request = intent?.toExternalNavigationRequest() ?: return
        _externalNavigationRequestFlow.value = request
    }

    private fun Intent.toExternalNavigationRequest(): ExternalNavigationRequest? {
        readPlayerRequestExtra()?.let { return ExternalNavigationRequest.Player(it) }
        readExternalDestinationExtra()?.let { return ExternalNavigationRequest.Destination(it) }
        getStringExtra(EXTRA_EXTERNAL_ROUTE)
            ?.let(ExternalDestination::fromLegacyRoute)
            ?.let { return ExternalNavigationRequest.Destination(it) }
        if (hasExtra(EXTRA_EXTERNAL_ROUTE)) {
            return ExternalNavigationRequest.Destination(ExternalDestination.Home)
        }
        readImportedPlaylistUri()?.let { return ExternalNavigationRequest.ImportM3u(it) }
        readImportedBackupUri()?.let { return ExternalNavigationRequest.ImportBackup(it) }

        val query = when (action) {
            Intent.ACTION_SEARCH,
            Intent.ACTION_ASSIST,
            RecognizerIntent.ACTION_VOICE_SEARCH_HANDS_FREE -> {
                getStringExtra(SearchManager.QUERY)
                    ?: getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            }

            else -> null
        }?.trim().orEmpty()

        query.takeIf { it.isNotBlank() }?.let(ExternalNavigationRequest::Search)?.let { return it }

        return if (action == Intent.ACTION_VIEW) {
            ExternalNavigationRequest.Destination(ExternalDestination.Home)
        } else {
            null
        }
    }

    private fun Intent.readImportedPlaylistUri(): String? {
        if (action != Intent.ACTION_VIEW) return null
        val targetUri = data ?: return null
        val normalizedPath = targetUri.toString().substringBefore('?').lowercase(Locale.ROOT)
        val mimeType = type?.lowercase(Locale.ROOT).orEmpty()
        val isPlaylistMime = mimeType in setOf(
            "audio/x-mpegurl",
            "audio/mpegurl",
            "application/x-mpegurl",
            "application/vnd.apple.mpegurl",
            "application/mpegurl"
        )
        val isPlaylistPath = normalizedPath.endsWith(".m3u") || normalizedPath.endsWith(".m3u8")
        if (!isPlaylistMime && !isPlaylistPath) return null
        return when (targetUri.scheme?.lowercase(Locale.ROOT)) {
            "content", "file" -> targetUri.toString()
            else -> null
        }
    }

    private fun Intent.readImportedBackupUri(): String? {
        val targetUri = when (action) {
            Intent.ACTION_VIEW -> data
            Intent.ACTION_SEND -> readStreamUriExtra()
            else -> null
        } ?: return null
        if (!isBackupJsonCandidate(targetUri)) return null
        return BackupFileBridge.copyToImportInbox(this@MainActivity, targetUri)?.toString()
            ?: targetUri.toString()
    }

    @Suppress("DEPRECATION")
    private fun Intent.readStreamUriExtra(): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            getParcelableExtra(Intent.EXTRA_STREAM) as? Uri
        } ?: clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri
    }

    private fun Intent.isBackupJsonCandidate(uri: Uri): Boolean {
        val normalizedPath = uri.toString().substringBefore('?').lowercase(Locale.ROOT)
        val mimeType = type?.lowercase(Locale.ROOT).orEmpty()
        val isJsonMime = mimeType in setOf("application/json", "text/json", "application/x-json")
        val isJsonPath = normalizedPath.endsWith(".json")
        val isGenericJsonFile = mimeType == "application/octet-stream" && isJsonPath
        if (!isJsonMime && !isJsonPath && !isGenericJsonFile) return false
        return uri.scheme?.lowercase(Locale.ROOT) in setOf("content", "file")
    }

    @Suppress("DEPRECATION")
    private fun Intent.readPlayerRequestExtra(): PlayerNavigationRequest? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getSerializableExtra(EXTRA_PLAYER_REQUEST, PlayerNavigationRequest::class.java)
        } else {
            getSerializableExtra(EXTRA_PLAYER_REQUEST) as? PlayerNavigationRequest
        }
    }

    @Suppress("DEPRECATION")
    private fun Intent.readExternalDestinationExtra(): ExternalDestination? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getSerializableExtra(EXTRA_EXTERNAL_DESTINATION, ExternalDestination::class.java)
        } else {
            getSerializableExtra(EXTRA_EXTERNAL_DESTINATION) as? ExternalDestination
        }
    }

    // Fire TV remotes have no INFO or GUIDE button. The conventional info-OSD
    // trigger on Fire TV is a long-press of the MENU (≡) button. Convert it
    // into a synthetic KEYCODE_INFO event so the existing Compose key handlers
    // can react uniformly across Android TV (INFO) and Fire TV (long-MENU).
    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent): Boolean {
        if (keyCode == android.view.KeyEvent.KEYCODE_MENU) {
            event.startTracking()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyLongPress(keyCode: Int, event: android.view.KeyEvent): Boolean {
        if (keyCode == android.view.KeyEvent.KEYCODE_MENU) {
            val down = android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_INFO)
            val up = android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_INFO)
            dispatchKeyEvent(down)
            dispatchKeyEvent(up)
            return true
        }
        return super.onKeyLongPress(keyCode, event)
    }
}
