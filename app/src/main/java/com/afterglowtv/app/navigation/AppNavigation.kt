package com.afterglowtv.app.navigation

import android.net.Uri
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewModelScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.afterglowtv.app.R
import com.afterglowtv.app.store.StorePolicy
import com.afterglowtv.app.store.StorePolicySnapshot
import com.afterglowtv.app.store.amazon.AmazonAppstoreBridge
import com.afterglowtv.app.ui.model.isArchivePlayable
import com.afterglowtv.domain.model.Channel
import com.afterglowtv.domain.model.Episode
import com.afterglowtv.domain.model.LocalMediaItem
import com.afterglowtv.domain.model.Movie
import com.afterglowtv.domain.model.ProviderM3uPlaylistKind
import com.afterglowtv.domain.model.VirtualCategoryIds
import com.afterglowtv.domain.repository.ChannelRepository
import com.afterglowtv.app.ui.screens.dashboard.DashboardScreen
import com.afterglowtv.app.ui.components.dialogs.AmazonPremiumPurchaseDialog
import com.afterglowtv.app.ui.screens.multiview.MultiViewScreen
import com.afterglowtv.app.ui.screens.home.HomeScreen
import com.afterglowtv.app.ui.screens.local.LocalMediaScreen
import com.afterglowtv.app.ui.screens.player.PlayerScreen
import com.afterglowtv.app.ui.screens.settings.SettingsScreen
import com.afterglowtv.app.ui.screens.vod.VodScreen
import com.afterglowtv.app.ui.screens.welcome.WelcomeScreen
import com.afterglowtv.app.MainActivity
import com.afterglowtv.data.preferences.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.Serializable
import javax.inject.Inject
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


private const val PLAYER_REQUEST_KEY = "player_request"

private val NoticeFontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_bold, FontWeight.Bold)
)

data class PlayerNavigationRequest(
    val streamUrl: String,
    val title: String,
    val channelId: String? = null,
    val internalId: Long = -1L,
    val categoryId: Long? = null,
    val providerId: Long? = null,
    val isVirtual: Boolean = false,
    val combinedProfileId: Long? = null,
    val combinedSourceFilterProviderId: Long? = null,
    val contentType: String = "LIVE",
    val artworkUrl: String? = null,
    val archiveStartMs: Long? = null,
    val archiveEndMs: Long? = null,
    val archiveTitle: String? = null,
    val returnRoute: String? = null,
    val seriesId: Long? = null,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val episodeId: Long? = null,
    val startPositionMs: Long? = null
) : Serializable

object Routes {
    const val HOME = "home"
    const val LIVE_TV = "live_tv"
    const val LIVE_TV_DESTINATION = "live_tv?categoryId={categoryId}"
    const val LOCAL_MEDIA = "local_media"
    const val VOD_CONTAINER = "vod_container"
    const val ADULT = "adult"
    const val EPG = "epg"
    const val EPG_DESTINATION = "epg?categoryId={categoryId}&anchorTime={anchorTime}&favoritesOnly={favoritesOnly}"
    const val SETTINGS = "settings"
    const val THEMES = "themes"
    const val GLOW_SETTINGS = "glow_settings"
    const val STYLE_CUSTOMIZER = "style_customizer"
    const val SETTINGS_DESTINATION = "settings?backupUri={backupUri}&addProviderKind={addProviderKind}&playlistUri={playlistUri}"
    const val PLAYER = "player"
    const val SEARCH = "search"
    const val SEARCH_DESTINATION = "search?query={query}"
    const val WELCOME = "welcome"
    const val PARENTAL_CONTROL_GROUPS = "parental_control_groups/{providerId}"
    const val MULTI_VIEW = "multi_view"

    fun liveTv(categoryId: Long? = null) = if (categoryId == null) LIVE_TV else "$LIVE_TV?categoryId=$categoryId"
    fun epg(categoryId: Long? = null, anchorTime: Long? = null, favoritesOnly: Boolean? = null): String {
        val resolvedCategoryId = categoryId ?: -1L
        val resolvedAnchorTime = anchorTime ?: -1L
        val resolvedFavoritesOnly = favoritesOnly ?: false
        return "$EPG?categoryId=$resolvedCategoryId&anchorTime=$resolvedAnchorTime&favoritesOnly=$resolvedFavoritesOnly"
    }

    fun livePlayer(
        channel: Channel,
        categoryId: Long? = channel.categoryId,
        providerId: Long? = channel.providerId,
        isVirtual: Boolean = false,
        combinedProfileId: Long? = null,
        combinedSourceFilterProviderId: Long? = null,
        returnRoute: String? = null
    ): PlayerNavigationRequest {
        val effectiveCategoryId = categoryId ?: ChannelRepository.ALL_CHANNELS_ID
        return player(
            streamUrl = channel.streamUrl,
            title = channel.name,
            channelId = channel.epgChannelId,
            internalId = channel.id,
            categoryId = effectiveCategoryId,
            providerId = providerId,
            isVirtual = isVirtual,
            combinedProfileId = combinedProfileId,
            combinedSourceFilterProviderId = combinedSourceFilterProviderId,
            contentType = "LIVE",
            returnRoute = returnRoute
        )
    }

    fun moviePlayer(movie: Movie): PlayerNavigationRequest {
        return player(
            streamUrl = movie.streamUrl,
            title = movie.name,
            internalId = movie.id,
            categoryId = movie.categoryId,
            providerId = movie.providerId,
            contentType = "MOVIE",
            artworkUrl = movie.posterUrl ?: movie.backdropUrl
        )
    }

    fun episodePlayer(episode: Episode): PlayerNavigationRequest {
        return player(
            streamUrl = episode.streamUrl,
            title = "${episode.title} - S${episode.seasonNumber}E${episode.episodeNumber}",
            internalId = episode.id,
            providerId = episode.providerId,
            contentType = "SERIES_EPISODE",
            artworkUrl = episode.coverUrl,
            seriesId = episode.seriesId.takeIf { it > 0L },
            seasonNumber = episode.seasonNumber,
            episodeNumber = episode.episodeNumber,
            episodeId = episode.episodeId.takeIf { it > 0L }
        )
    }

    fun localMediaPlayer(item: LocalMediaItem): PlayerNavigationRequest {
        val isEpisode = item.mediaKind == com.afterglowtv.domain.model.LocalMediaKind.EPISODE
        return player(
            streamUrl = item.uri,
            title = item.title.ifBlank { item.displayName },
            internalId = item.id,
            providerId = 0L,
            contentType = if (isEpisode) "SERIES_EPISODE" else "MOVIE",
            artworkUrl = item.posterUri ?: item.backdropUri,
            returnRoute = LOCAL_MEDIA,
            seriesId = null,
            seasonNumber = item.seasonNumber,
            episodeNumber = item.episodeNumber,
            startPositionMs = 0L
        )
    }

    fun search(query: String? = null): String =
        if (query.isNullOrBlank()) SEARCH else "$SEARCH?query=${Uri.encode(query)}"

    fun settings(
        backupUri: String? = null,
        addProviderKind: ProviderM3uPlaylistKind? = null,
        playlistUri: String? = null
    ): String =
        if (backupUri.isNullOrBlank() && addProviderKind == null && playlistUri.isNullOrBlank()) {
            SETTINGS
        } else {
            "$SETTINGS?backupUri=${Uri.encode(backupUri.orEmpty())}" +
                "&addProviderKind=${addProviderKind?.name.orEmpty()}" +
                "&playlistUri=${Uri.encode(playlistUri.orEmpty())}"
        }

    fun player(
        streamUrl: String,
        title: String,
        channelId: String? = null,
        internalId: Long = -1L,
        categoryId: Long? = null,
        providerId: Long? = null,
        isVirtual: Boolean = false,
        combinedProfileId: Long? = null,
        combinedSourceFilterProviderId: Long? = null,
        contentType: String = "LIVE",
        artworkUrl: String? = null,
        archiveStartMs: Long? = null,
        archiveEndMs: Long? = null,
        archiveTitle: String? = null,
        returnRoute: String? = null,
        seriesId: Long? = null,
        seasonNumber: Int? = null,
        episodeNumber: Int? = null,
        episodeId: Long? = null,
        startPositionMs: Long? = null
    ): PlayerNavigationRequest {
        return PlayerNavigationRequest(
            streamUrl = streamUrl,
            title = title,
            channelId = channelId,
            internalId = internalId,
            categoryId = categoryId,
            providerId = providerId,
            isVirtual = isVirtual,
            combinedProfileId = combinedProfileId,
            combinedSourceFilterProviderId = combinedSourceFilterProviderId,
            contentType = contentType,
            artworkUrl = artworkUrl,
            archiveStartMs = archiveStartMs,
            archiveEndMs = archiveEndMs,
            archiveTitle = archiveTitle,
            returnRoute = returnRoute,
            seriesId = seriesId,
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber,
            episodeId = episodeId,
            startPositionMs = startPositionMs
        )
    }

    fun parentalControlGroups(providerId: Long) = "parental_control_groups/$providerId"
}

/** Accepts app-supported media schemes while still rejecting obviously unsafe ones. */
private fun isStreamUrlSafe(url: String?): Boolean {
    if (url.isNullOrBlank()) return false
    val scheme = url.substringBefore("://").lowercase()
    return scheme in setOf("http", "https", "rtsp", "rtmp", "rtsps", "mms", "xtream", "content", "file", "smb")
}

/** Navigate only when the current destination is fully resumed – prevents double-navigation during transitions. */
private fun NavHostController.navigateIfResumed(route: String, builder: NavOptionsBuilder.() -> Unit = {}): Boolean {
    if (currentBackStackEntry?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) != true) return false
    navigate(route, builder)
    return true
}

private fun NavHostController.navigateToPlayer(request: PlayerNavigationRequest): Boolean {
    if (currentBackStackEntry?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) != true) return false
    currentBackStackEntry?.savedStateHandle?.set(PLAYER_REQUEST_KEY, request)
    navigate(Routes.PLAYER) { launchSingleTop = true }
    return true
}

private fun NavHostController.navigateToExternalPlayer(request: PlayerNavigationRequest): Boolean {
    if (currentBackStackEntry?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) != true) return false
    currentBackStackEntry?.savedStateHandle?.set(PLAYER_REQUEST_KEY, request)
    navigate(Routes.PLAYER) { launchSingleTop = true }
    return true
}

@HiltViewModel
class AppStartupDestinationViewModel @Inject constructor(
    preferencesRepository: PreferencesRepository
) : ViewModel() {
    init {
        viewModelScope.launch {
            preferencesRepository.migrateStartupDestinationToHomeDefault()
        }
    }

    val storedDeveloperModeEnabled: StateFlow<Boolean> = preferencesRepository.developerModeEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = false
        )

    val developerModeEnabled: StateFlow<Boolean> = combine(
        storedDeveloperModeEnabled,
        AmazonAppstoreBridge.premiumEntitled
    ) { storedDeveloperModeEnabled, amazonPremiumEntitled ->
        StorePolicy.effectiveDeveloperModeEnabled(
            storedDeveloperModeEnabled = storedDeveloperModeEnabled,
            amazonPremiumEntitled = amazonPremiumEntitled
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = false
        )

    val startupDestination: StateFlow<StartupDestination> = preferencesRepository.startupDestination
        .map(StartupDestination::fromStorage)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = StartupDestination.default
        )

    val startupRoute: StateFlow<String> = combine(startupDestination, developerModeEnabled) { destination, developerModeEnabled ->
        resolveStartupRoute(destination, developerModeEnabled)
    }
        .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = resolveStartupRoute(StartupDestination.default, false)
    )
}

internal fun resolveStartupRoute(destination: StartupDestination, developerModeEnabled: Boolean): String =
    resolveStartupRoute(destination, developerModeEnabled, StorePolicy.currentFor(developerModeEnabled))

internal fun resolveStartupRoute(
    destination: StartupDestination,
    developerModeEnabled: Boolean,
    policy: StorePolicySnapshot
): String =
    if (
        (policy.guideOnlyReviewSurface && !isGuideOnlyAllowedRoute(destination.route)) ||
        (destination.requiresDeveloperMode && (!developerModeEnabled || !policy.showAdultSurfaces)) ||
        (destination.route == Routes.WELCOME && !policy.showWelcomeRoute)
    ) {
        Routes.HOME
    } else {
        destination.route
    }

private fun isDeveloperLockedRoute(route: String): Boolean =
    route == Routes.ADULT

private fun isStoreLockedRoute(route: String): Boolean =
    isStoreLockedRoute(route, developerModeEnabled = false)

private fun isStoreLockedRoute(
    route: String,
    developerModeEnabled: Boolean,
    policy: StorePolicySnapshot = StorePolicy.currentFor(developerModeEnabled)
): Boolean {
    return (policy.guideOnlyReviewSurface && !isGuideOnlyAllowedRoute(route)) ||
        (route == Routes.ADULT && !policy.showAdultSurfaces)
}

private fun isGuideOnlyAllowedRoute(route: String): Boolean {
    val baseRoute = route.substringBefore('?')
    return baseRoute in setOf(
        Routes.HOME,
        Routes.LIVE_TV,
        Routes.EPG,
        Routes.PLAYER,
        Routes.SETTINGS,
        Routes.THEMES,
        Routes.GLOW_SETTINGS,
        Routes.STYLE_CUSTOMIZER
    )
}

@Composable
fun AppNavigation(mainActivity: MainActivity) {
    val navController = rememberNavController()
    val startupDestinationViewModel: AppStartupDestinationViewModel = hiltViewModel()
    val startupRoute = startupDestinationViewModel.startupRoute.collectAsStateWithLifecycle().value
    val storedDeveloperModeEnabled = startupDestinationViewModel.storedDeveloperModeEnabled.collectAsStateWithLifecycle().value
    val amazonPremiumEntitled = AmazonAppstoreBridge.premiumEntitled.collectAsStateWithLifecycle().value
    var nowMs by remember { mutableLongStateOf(StorePolicy.currentTimeMillis()) }
    val developerModeEnabled = StorePolicy.effectiveDeveloperModeEnabled(
        storedDeveloperModeEnabled = storedDeveloperModeEnabled,
        amazonPremiumEntitled = amazonPremiumEntitled,
        nowMs = nowMs
    )
    val policy = StorePolicy.currentFor(
        storedDeveloperModeEnabled = storedDeveloperModeEnabled,
        amazonPremiumEntitled = amazonPremiumEntitled,
        nowMs = nowMs
    )
    val currentBackStackEntry = navController.currentBackStackEntryAsState().value
    val externalNavigationRequest = mainActivity.externalNavigationRequestFlow.collectAsStateWithLifecycle().value
    var premiumPurchaseDialogDismissed by remember { mutableStateOf(false) }
    var showContentResponsibilityNotice by remember { mutableStateOf(StorePolicy.rawCurrent.amazonReviewBuild) }
    val shouldShowPremiumPurchaseOptions = StorePolicy.rawCurrent.shouldShowPremiumPurchaseOptions(
        storedDeveloperModeEnabled = storedDeveloperModeEnabled,
        amazonPremiumEntitled = amazonPremiumEntitled,
        nowMs = nowMs
    )

    LaunchedEffect(Unit) {
        while (true) {
            nowMs = StorePolicy.currentTimeMillis()
            delay(1_000L)
        }
    }

    LaunchedEffect(policy.guideOnlyReviewSurface, currentBackStackEntry?.destination?.route, developerModeEnabled) {
        val currentRoute = currentBackStackEntry?.destination?.route ?: return@LaunchedEffect
        if (isStoreLockedRoute(currentRoute, developerModeEnabled, policy)) {
            navController.navigate(Routes.HOME) {
                popUpTo(navController.graph.startDestinationId) {
                    inclusive = false
                }
                launchSingleTop = true
            }
        }
    }

    LaunchedEffect(externalNavigationRequest, currentBackStackEntry?.lifecycle?.currentState) {
        when (val request = externalNavigationRequest) {
            is ExternalNavigationRequest.Player -> {
                if (navController.navigateToExternalPlayer(request.request)) {
                    mainActivity.clearExternalNavigationRequest()
                }
            }

            is ExternalNavigationRequest.Destination -> {
                val route = request.destination.toRoute()
                if (isStoreLockedRoute(route, developerModeEnabled, policy) || (isDeveloperLockedRoute(route) && !developerModeEnabled)) {
                    mainActivity.clearExternalNavigationRequest()
                } else if (navController.navigateIfResumed(route) { launchSingleTop = true }) {
                    mainActivity.clearExternalNavigationRequest()
                }
            }

            is ExternalNavigationRequest.ImportM3u -> {
                val route = Routes.settings(
                    addProviderKind = ProviderM3uPlaylistKind.LIVE,
                    playlistUri = request.uri
                )
                if (isStoreLockedRoute(route, developerModeEnabled, policy)) {
                    mainActivity.clearExternalNavigationRequest()
                } else if (navController.navigateIfResumed(route) { launchSingleTop = true }) {
                    mainActivity.clearExternalNavigationRequest()
                }
            }

            is ExternalNavigationRequest.ImportBackup -> {
                val route = Routes.settings(backupUri = request.uri)
                if (isStoreLockedRoute(route, developerModeEnabled, policy)) {
                    mainActivity.clearExternalNavigationRequest()
                } else if (navController.navigateIfResumed(route) { launchSingleTop = true }) {
                    mainActivity.clearExternalNavigationRequest()
                }
            }

            is ExternalNavigationRequest.Search -> {
                val route = Routes.search(request.query)
                if (isStoreLockedRoute(route, developerModeEnabled, policy)) {
                    mainActivity.clearExternalNavigationRequest()
                } else if (navController.navigateIfResumed(route) { launchSingleTop = true }) {
                    mainActivity.clearExternalNavigationRequest()
                }
            }

            null -> Unit
        }
    }

    // NAV-M02/NAV-H02: Single helper replacing repeated tab lambdas without serializing
    // each tab's full UI tree into saved state on every switch.
    fun tabNavigate(route: String) {
        if (isStoreLockedRoute(route, developerModeEnabled, policy) || (isDeveloperLockedRoute(route) && !developerModeEnabled)) {
            return
        }
        val entry = navController.currentBackStackEntry ?: return
        if (!entry.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return
        val currentRoute = entry.destination.route
        if (currentRoute == route || currentRoute?.startsWith("$route?") == true) return

        navController.navigate(route) {
            popUpTo(navController.graph.startDestinationId) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Box {
        NavHost(
            navController = navController,
            startDestination = startupRoute
        ) {
        composable(Routes.WELCOME) {
            WelcomeScreen(
                onNavigateToHome = dropUnlessResumed {
                    navController.navigate(startupRoute) {
                        popUpTo(Routes.WELCOME) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HOME) {
            DashboardScreen(
                onNavigate = { route -> tabNavigate(route) },
                onAddProvider = {
                    navController.navigateIfResumed(
                        Routes.settings(addProviderKind = ProviderM3uPlaylistKind.LIVE)
                    )
                },
                currentRoute = Routes.HOME
            )
        }

        composable(
            route = Routes.LIVE_TV_DESTINATION,
            arguments = listOf(
                navArgument("categoryId") { type = NavType.LongType; defaultValue = -1L }
            )
        ) { backStackEntry ->
            val initialCategoryId = backStackEntry.arguments?.getLong("categoryId")?.takeIf { it != -1L }
            HomeScreen(
                onChannelClick = { channel, category, provider, combinedProfileId, combinedSourceFilterProviderId ->
                    navController.navigateToPlayer(
                        Routes.livePlayer(
                            channel = channel,
                            categoryId = category?.id,
                            providerId = provider?.id,
                            isVirtual = category?.isVirtual == true,
                            combinedProfileId = combinedProfileId,
                            combinedSourceFilterProviderId = combinedSourceFilterProviderId,
                            returnRoute = Routes.liveTv(category?.id)
                        )
                    )
                },
                onNavigate = { route -> tabNavigate(route) },
                currentRoute = Routes.LIVE_TV,
                initialCategoryId = initialCategoryId
            )
        }
// ... (rest of file)

        composable(Routes.LOCAL_MEDIA) {
            LocalMediaScreen(
                onPlayItem = { item ->
                    navController.navigateToPlayer(Routes.localMediaPlayer(item))
                },
                onNavigate = { route -> tabNavigate(route) },
                currentRoute = Routes.LOCAL_MEDIA
            )
        }

        composable(Routes.VOD_CONTAINER) {
            VodScreen(
                onNavigate = { route -> tabNavigate(route) },
                currentRoute = Routes.VOD_CONTAINER,
                onMovieClick = { movie ->
                    navController.navigateToPlayer(
                        Routes.moviePlayer(movie).copy(returnRoute = Routes.VOD_CONTAINER)
                    )
                }
            )
        }

        composable(Routes.ADULT) {
            if (!developerModeEnabled || !policy.showAdultSurfaces) {
                LaunchedEffect(Unit) {
                    navController.navigate(Routes.HOME) {
                        launchSingleTop = true
                    }
                }
                return@composable
            }
            HomeScreen(
                onChannelClick = { channel, category, provider, combinedProfileId, combinedSourceFilterProviderId ->
                    navController.navigateToPlayer(
                        Routes.livePlayer(
                            channel = channel,
                            categoryId = adultPlaybackCategoryId(category),
                            providerId = provider?.id ?: channel.providerId,
                            isVirtual = true,
                            combinedProfileId = combinedProfileId,
                            combinedSourceFilterProviderId = combinedSourceFilterProviderId,
                            returnRoute = Routes.ADULT
                        )
                    )
                },
                onNavigate = { route -> tabNavigate(route) },
                currentRoute = Routes.ADULT,
                adultMode = true,
                titleRes = R.string.nav_adult
            )
        }

        composable(
            route = Routes.EPG_DESTINATION,
            arguments = listOf(
                navArgument("categoryId") { type = NavType.LongType; defaultValue = -1L },
                navArgument("anchorTime") { type = NavType.LongType; defaultValue = -1L },
                navArgument("favoritesOnly") { type = NavType.BoolType; defaultValue = false }
            )
        ) { backStackEntry ->
            val epgCategoryId = backStackEntry.arguments?.getLong("categoryId")?.takeIf { it != -1L }
            val epgAnchorTime = backStackEntry.arguments?.getLong("anchorTime")?.takeIf { it != -1L }
            val epgFavoritesOnly = backStackEntry.arguments?.getBoolean("favoritesOnly") ?: false
            com.afterglowtv.app.ui.screens.epg.FullEpgScreen(
                currentRoute = Routes.EPG,
                initialCategoryId = epgCategoryId,
                initialAnchorTime = epgAnchorTime,
                initialFavoritesOnly = epgFavoritesOnly,
                onPlayChannel = { channel, categoryId, isVirtual, combinedProfileId, returnRoute ->
                    navController.navigateToPlayer(
                        Routes.livePlayer(
                            channel = channel,
                            categoryId = categoryId,
                            providerId = channel.providerId,
                            isVirtual = isVirtual,
                            combinedProfileId = combinedProfileId,
                            returnRoute = returnRoute
                        )
                    )
                },
                onPlayArchive = { channel, program, categoryId, isVirtual, combinedProfileId, returnRoute ->
                    if (!channel.isArchivePlayable(program)) {
                        return@FullEpgScreen
                    }
                    navController.navigateToPlayer(
                        Routes.player(
                            streamUrl = channel.streamUrl,
                            title = channel.name,
                            channelId = channel.epgChannelId,
                            internalId = channel.id,
                            categoryId = categoryId,
                            providerId = channel.providerId,
                            isVirtual = isVirtual,
                            combinedProfileId = combinedProfileId,
                            contentType = "LIVE",
                            archiveStartMs = program.startTime,
                            archiveEndMs = program.endTime,
                            archiveTitle = "${channel.name}: ${program.title}",
                            returnRoute = returnRoute
                        )
                    )
                },
                onNavigate = { route -> tabNavigate(route) }
            )
        }

        composable(
            route = Routes.SETTINGS_DESTINATION,
            arguments = listOf(
                navArgument("backupUri") { type = NavType.StringType; defaultValue = "" },
                navArgument("addProviderKind") { type = NavType.StringType; defaultValue = "" },
                navArgument("playlistUri") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val backupUri = backStackEntry.arguments?.getString("backupUri")?.takeIf { it.isNotBlank() }
            val addProviderKind = backStackEntry.arguments
                ?.getString("addProviderKind")
                ?.takeIf { it.isNotBlank() }
                ?.let { value -> runCatching { ProviderM3uPlaylistKind.valueOf(value) }.getOrNull() }
            val playlistUri = backStackEntry.arguments?.getString("playlistUri")?.takeIf { it.isNotBlank() }
            SettingsScreen(
                onNavigate = { route -> tabNavigate(route) },
                onNavigateToParentalControl = { providerId ->
                    navController.navigateIfResumed(Routes.parentalControlGroups(providerId))
                },
                onReturnToPlayer = {
                    navController.popBackStack(Routes.PLAYER, false)
                },
                currentRoute = Routes.SETTINGS,
                initialBackupImportUri = backupUri,
                initialAddProviderKind = addProviderKind,
                initialProviderPlaylistUri = playlistUri
            )
        }

        composable(Routes.THEMES) {
            com.afterglowtv.app.ui.screens.settings.ThemePickerScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.GLOW_SETTINGS) {
            com.afterglowtv.app.ui.screens.settings.GlowSettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.STYLE_CUSTOMIZER) {
            com.afterglowtv.app.ui.screens.settings.StyleCustomizerScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.PARENTAL_CONTROL_GROUPS,
            arguments = listOf(
                navArgument("providerId") { type = NavType.LongType }
            )
        ) {
            com.afterglowtv.app.ui.screens.settings.parental.ParentalControlGroupScreen(
                currentRoute = Routes.SETTINGS,
                onNavigate = { route -> tabNavigate(route) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.SEARCH_DESTINATION,
            arguments = listOf(
                navArgument("query") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            com.afterglowtv.app.ui.screens.search.SearchScreen(
                initialQuery = backStackEntry.arguments?.getString("query").orEmpty(),
                onChannelClick = { channel ->
                    navController.navigateToPlayer(
                        Routes.livePlayer(
                            channel = channel,
                            categoryId = channel.categoryId ?: ChannelRepository.ALL_CHANNELS_ID,
                            providerId = channel.providerId,
                            isVirtual = false,
                            returnRoute = Routes.search(backStackEntry.arguments?.getString("query").orEmpty())
                        )
                    )
                },
                onMovieClick = { movie ->
                    navController.navigateToPlayer(
                        Routes.moviePlayer(movie).copy(
                            returnRoute = Routes.search(backStackEntry.arguments?.getString("query").orEmpty())
                        )
                    )
                },
                onSeriesClick = { _ -> Unit },
                onNavigate = { route -> tabNavigate(route) },
                currentRoute = Routes.SEARCH
            )
        }

        composable(route = Routes.PLAYER) { backStackEntry ->
            val playerRequest = backStackEntry.savedStateHandle.get<PlayerNavigationRequest>(PLAYER_REQUEST_KEY)
                ?: navController.previousBackStackEntry?.savedStateHandle?.get<PlayerNavigationRequest>(PLAYER_REQUEST_KEY)?.also {
                    backStackEntry.savedStateHandle[PLAYER_REQUEST_KEY] = it
                }
            val streamUrl = if (isStreamUrlSafe(playerRequest?.streamUrl)) playerRequest?.streamUrl.orEmpty() else ""
            PlayerScreen(
                streamUrl = streamUrl,
                title = playerRequest?.title.orEmpty(),
                epgChannelId = playerRequest?.channelId,
                internalChannelId = playerRequest?.internalId ?: -1L,
                categoryId = playerRequest?.categoryId,
                providerId = playerRequest?.providerId,
                isVirtual = playerRequest?.isVirtual ?: false,
                combinedProfileId = playerRequest?.combinedProfileId,
                combinedSourceFilterProviderId = playerRequest?.combinedSourceFilterProviderId,
                contentType = playerRequest?.contentType ?: "LIVE",
                artworkUrl = playerRequest?.artworkUrl,
                archiveStartMs = playerRequest?.archiveStartMs,
                archiveEndMs = playerRequest?.archiveEndMs,
                archiveTitle = playerRequest?.archiveTitle,
                returnRoute = playerRequest?.returnRoute,
                seriesId = playerRequest?.seriesId,
                seasonNumber = playerRequest?.seasonNumber,
                episodeNumber = playerRequest?.episodeNumber,
                episodeId = playerRequest?.episodeId,
                startPositionMs = playerRequest?.startPositionMs,
                onBack = {
                    val route = playerRequest?.returnRoute
                    if (!route.isNullOrBlank() && navController.popBackStack(route, false)) {
                        Unit
                    } else if (!route.isNullOrBlank()) {
                        navController.navigate(route) {
                            popUpTo(Routes.PLAYER) { inclusive = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    } else {
                        navController.popBackStack()
                    }
                },
                onNavigate = { route ->
                    navController.navigateIfResumed(route) {
                        launchSingleTop = true
                        if (route == Routes.MULTI_VIEW) {
                            popUpTo(Routes.PLAYER) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(Routes.MULTI_VIEW) {
            MultiViewScreen(
                onBack = { navController.popBackStack() }
            )
        }
        }

        if (showContentResponsibilityNotice) {
            ContentResponsibilityNoticeDialog(
                onDismissRequest = { showContentResponsibilityNotice = false }
            )
        }

        if (shouldShowPremiumPurchaseOptions && !premiumPurchaseDialogDismissed && !showContentResponsibilityNotice) {
            AmazonPremiumPurchaseDialog(
                onDismissRequest = { premiumPurchaseDialogDismissed = true }
            )
        }
    }
}

@Composable
private fun ContentResponsibilityNoticeDialog(
    onDismissRequest: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier.width(820.dp),
        title = {
            androidx.compose.material3.Text(
                text = androidx.compose.ui.res.stringResource(R.string.content_responsibility_title),
                color = androidx.compose.ui.graphics.Color(0xFF1D1823),
                fontFamily = NoticeFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                lineHeight = 28.sp
            )
        },
        text = {
            val noticeBody = androidx.compose.ui.res.stringResource(R.string.content_responsibility_body)
            androidx.compose.material3.Text(
                text = noticeBody.withBoldLegalReferences(),
                modifier = Modifier
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
                color = androidx.compose.ui.graphics.Color(0xFF28232D),
                fontFamily = NoticeFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
        },
        confirmButton = {
            androidx.compose.material3.Button(
                onClick = onDismissRequest,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = androidx.compose.ui.graphics.Color(0xFFFF7A1A),
                    contentColor = androidx.compose.ui.graphics.Color.White
                )
            ) {
                androidx.compose.material3.Text(
                    text = androidx.compose.ui.res.stringResource(R.string.content_responsibility_confirm),
                    color = androidx.compose.ui.graphics.Color.White
                )
            }
        }
    )
}

private fun String.withBoldLegalReferences() = buildAnnotatedString {
    append(this@withBoldLegalReferences)
    listOf(
        "Afterglow TV",
        "United States Copyright Act (17 U.S.C. \u00A7\u00A7 101 et seq.)",
        "Digital Millennium Copyright Act (17 U.S.C. \u00A7 1201 et seq.)"
    ).forEach { reference ->
        var start = this@withBoldLegalReferences.indexOf(reference)
        while (start >= 0) {
            addStyle(
                style = SpanStyle(fontWeight = FontWeight.Bold),
                start = start,
                end = start + reference.length
            )
            start = this@withBoldLegalReferences.indexOf(reference, start + reference.length)
        }
    }
}
