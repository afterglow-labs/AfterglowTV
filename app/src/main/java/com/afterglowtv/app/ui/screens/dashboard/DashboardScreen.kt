package com.afterglowtv.app.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import com.afterglowtv.app.R
import com.afterglowtv.app.navigation.StartupDestination
import com.afterglowtv.app.navigation.Routes
import com.afterglowtv.app.store.StorePolicy
import com.afterglowtv.app.store.StorePolicySnapshot
import com.afterglowtv.app.ui.components.SearchInput
import com.afterglowtv.app.ui.components.shell.AppNavigationChrome
import com.afterglowtv.app.ui.components.shell.AppScreenScaffold
import com.afterglowtv.app.ui.design.AppColors
import com.afterglowtv.app.ui.design.AppPalette
import com.afterglowtv.app.ui.design.GlowSpec
import com.afterglowtv.app.ui.design.afterglow
import com.afterglowtv.app.ui.design.AppColors.Brand as Primary
import com.afterglowtv.app.ui.design.AppColors.SurfaceElevated as SurfaceElevated
import com.afterglowtv.app.ui.design.AppColors.SurfaceEmphasis as SurfaceHighlight
import com.afterglowtv.app.ui.design.AppColors.TextPrimary as TextPrimary
import com.afterglowtv.app.ui.design.AppColors.TextSecondary as OnSurfaceDim
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.BorderStroke
import com.afterglowtv.app.ui.interaction.TvClickableSurface
import com.afterglowtv.domain.model.ActiveLiveSource
import com.afterglowtv.domain.model.ActiveLiveSourceOption
import com.afterglowtv.domain.model.ProviderStatus
import com.afterglowtv.domain.model.ProviderType

@Composable
fun DashboardScreen(
    onNavigate: (String) -> Unit,
    onAddProvider: () -> Unit,
    currentRoute: String,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val startupDestination by viewModel.startupDestination.collectAsStateWithLifecycle()
    val developerModeEnabled by viewModel.developerModeEnabled.collectAsStateWithLifecycle()
    val remoteDpadChannelZapping by viewModel.remoteDpadChannelZapping.collectAsStateWithLifecycle()
    val remoteDpadInvertChannelZapping by viewModel.remoteDpadInvertChannelZapping.collectAsStateWithLifecycle()
    val remoteShowInfoOnZap by viewModel.remoteShowInfoOnZap.collectAsStateWithLifecycle()
    val preventStandbyDuringPlayback by viewModel.preventStandbyDuringPlayback.collectAsStateWithLifecycle()
    val autoPlayNextEpisode by viewModel.autoPlayNextEpisode.collectAsStateWithLifecycle()
    val backgroundGradientsEnabled by viewModel.backgroundGradientsEnabled.collectAsStateWithLifecycle()
    val themePaletteId by viewModel.themePaletteId.collectAsStateWithLifecycle()
    val showLiveSourceSwitcher by viewModel.showLiveSourceSwitcher.collectAsStateWithLifecycle()
    val showAllChannelsCategory by viewModel.showAllChannelsCategory.collectAsStateWithLifecycle()
    val showRecentChannelsCategory by viewModel.showRecentChannelsCategory.collectAsStateWithLifecycle()
    val provider = uiState.provider
    val snackbarHostState = remember { SnackbarHostState() }
    val policy = StorePolicy.currentFor(developerModeEnabled)

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.userMessageShown()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AppScreenScaffold(
            currentRoute = currentRoute,
            onNavigate = onNavigate,
            title = stringResource(R.string.nav_home),
            subtitle = provider?.name?.takeIf { uiState.showProviderChrome },
            navigationChrome = AppNavigationChrome.TopBar,
            compactHeader = true,
            showScreenHeader = false
        ) {
            if (shouldShowDashboardLoadingState(uiState)) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Primary)
                }
                return@AppScreenScaffold
            }

            Box(modifier = Modifier.fillMaxSize()) {
                HomeCommandHub(
                    policy = policy,
                    developerModeEnabled = developerModeEnabled,
                    activeProviderName = uiState.provider?.name,
                    providerHealth = uiState.providerHealth,
                    activeSource = uiState.activeLiveSource,
                    sourceOptions = uiState.liveSourceOptions,
                    startupDestination = startupDestination,
                    remoteDpadChannelZapping = remoteDpadChannelZapping,
                    remoteDpadInvertChannelZapping = remoteDpadInvertChannelZapping,
                    remoteShowInfoOnZap = remoteShowInfoOnZap,
                    preventStandbyDuringPlayback = preventStandbyDuringPlayback,
                    autoPlayNextEpisode = autoPlayNextEpisode,
                    backgroundGradientsEnabled = backgroundGradientsEnabled,
                    themePaletteId = themePaletteId,
                    themePalettes = AppPalette.ALL,
                    showLiveSourceSwitcher = showLiveSourceSwitcher,
                    showAllChannelsCategory = showAllChannelsCategory,
                    showRecentChannelsCategory = showRecentChannelsCategory,
                    onNavigate = onNavigate,
                    onAddProvider = onAddProvider,
                    onSourceSelected = viewModel::switchLiveSource,
                    onThemeSelected = viewModel::setThemePalette,
                    onStartupDestinationChange = viewModel::setStartupDestination,
                    onRemoteDpadChannelZappingChange = viewModel::setRemoteDpadChannelZapping,
                    onRemoteDpadInvertChannelZappingChange = viewModel::setRemoteDpadInvertChannelZapping,
                    onRemoteShowInfoOnZapChange = viewModel::setRemoteShowInfoOnZap,
                    onPreventStandbyDuringPlaybackChange = viewModel::setPreventStandbyDuringPlayback,
                    onAutoPlayNextEpisodeChange = viewModel::setAutoPlayNextEpisode,
                    onBackgroundGradientsEnabledChange = viewModel::setBackgroundGradientsEnabled,
                    onShowLiveSourceSwitcherChange = viewModel::setShowLiveSourceSwitcher,
                    onShowAllChannelsCategoryChange = viewModel::setShowAllChannelsCategory,
                    onShowRecentChannelsCategoryChange = viewModel::setShowRecentChannelsCategory,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }
}

internal fun shouldShowDashboardLoadingState(uiState: DashboardUiState): Boolean =
    uiState.provider == null && uiState.isLoading

@Composable
private fun HomeCommandHub(
    policy: StorePolicySnapshot,
    developerModeEnabled: Boolean,
    activeProviderName: String?,
    providerHealth: DashboardProviderHealth,
    activeSource: ActiveLiveSource?,
    sourceOptions: List<ActiveLiveSourceOption>,
    startupDestination: StartupDestination,
    remoteDpadChannelZapping: Boolean,
    remoteDpadInvertChannelZapping: Boolean,
    remoteShowInfoOnZap: Boolean,
    preventStandbyDuringPlayback: Boolean,
    autoPlayNextEpisode: Boolean,
    backgroundGradientsEnabled: Boolean,
    themePaletteId: String,
    themePalettes: List<AppPalette>,
    showLiveSourceSwitcher: Boolean,
    showAllChannelsCategory: Boolean,
    showRecentChannelsCategory: Boolean,
    onNavigate: (String) -> Unit,
    onAddProvider: () -> Unit,
    onSourceSelected: (ActiveLiveSource) -> Unit,
    onThemeSelected: (AppPalette) -> Unit,
    onStartupDestinationChange: (StartupDestination) -> Unit,
    onRemoteDpadChannelZappingChange: (Boolean) -> Unit,
    onRemoteDpadInvertChannelZappingChange: (Boolean) -> Unit,
    onRemoteShowInfoOnZapChange: (Boolean) -> Unit,
    onPreventStandbyDuringPlaybackChange: (Boolean) -> Unit,
    onAutoPlayNextEpisodeChange: (Boolean) -> Unit,
    onBackgroundGradientsEnabledChange: (Boolean) -> Unit,
    onShowLiveSourceSwitcherChange: (Boolean) -> Unit,
    onShowAllChannelsCategoryChange: (Boolean) -> Unit,
    onShowRecentChannelsCategoryChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val watchCards = buildList {
        add(HomeHubCardModel("Live TV", "Channels", Routes.LIVE_TV, Icons.Default.PlayArrow, Color(0xFF60A5FA)))
        add(HomeHubCardModel("TV Guide", "EPG grid", Routes.EPG, Icons.Default.Info, Color(0xFF5EEAD4)))
        if (!policy.guideOnlyReviewSurface) {
            add(HomeHubCardModel("VOD", "On demand", Routes.VOD_CONTAINER, Icons.Default.Star, Color(0xFFFFA64D)))
            add(HomeHubCardModel("Library", "Personal media", Routes.LOCAL_MEDIA, Icons.Default.Menu, Color(0xFFB4F06B)))
        }
    }
    val searchEnabled = !policy.guideOnlyReviewSurface
    val appearanceCards = listOf(
        HomeHubCardModel("Themes", "Colors", Routes.THEMES, Icons.Default.Star, Color(0xFFFF77FF)),
        HomeHubCardModel("Glow", "Focus light", Routes.GLOW_SETTINGS, Icons.Default.Info, Color(0xFF5EEAD4)),
        HomeHubCardModel("Customize", "Shapes", Routes.STYLE_CUSTOMIZER, Icons.Default.Settings, Color(0xFFFF7A38))
    )
    val startupOptions = StartupDestination.visibleEntries(developerModeEnabled)
        .filter {
            it == StartupDestination.HOME ||
                it == StartupDestination.LIVE_TV ||
                it == StartupDestination.IPTV_GUIDE ||
                it == StartupDestination.SETTINGS
        }
    val visibleStartupDestination = StartupDestination.visibleOrDefault(startupDestination, developerModeEnabled)

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val layoutMode = if (maxHeight < 540.dp || maxWidth < 800.dp) {
            HomeDashboardLayoutMode.CompactGrid
        } else {
            HomeDashboardLayoutMode.SpaciousGrid
        }
        val compactHome = layoutMode != HomeDashboardLayoutMode.SpaciousGrid
        val outerHorizontalPadding = if (compactHome) 12.dp else 24.dp
        val outerVerticalPadding = if (compactHome) 6.dp else 12.dp
        val gap = if (compactHome) 8.dp else 12.dp

        when (layoutMode) {
            HomeDashboardLayoutMode.ShortWide -> {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = outerHorizontalPadding, vertical = outerVerticalPadding),
                    horizontalArrangement = Arrangement.spacedBy(gap)
                ) {
                    HomeWatchWindow(
                        cards = watchCards,
                        searchEnabled = searchEnabled,
                        onNavigate = onNavigate,
                        layoutMode = layoutMode,
                        modifier = Modifier.weight(1.08f)
                    )
                    Column(
                        modifier = Modifier
                            .weight(0.92f)
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(gap)
                    ) {
                        HomeSourcesShortcutWindow(
                            activeProviderName = activeProviderName,
                            providerHealth = providerHealth,
                            activeSource = activeSource,
                            sourceOptions = sourceOptions,
                            onAddProvider = onAddProvider,
                            onOpenSettings = { onNavigate(Routes.SETTINGS) },
                            onSourceSelected = onSourceSelected,
                            layoutMode = layoutMode,
                            modifier = Modifier.weight(0.82f)
                        )
                        HomeAppearanceWindow(
                            cards = appearanceCards,
                            themePalettes = themePalettes,
                            activePaletteId = themePaletteId,
                            onNavigate = onNavigate,
                            onThemeSelected = onThemeSelected,
                            layoutMode = layoutMode,
                            modifier = Modifier.weight(1.18f)
                        )
                    }
                    HomeQuickWindow(
                        activeProviderName = activeProviderName,
                        providerHealth = providerHealth,
                        activeSource = activeSource,
                        sourceOptions = sourceOptions,
                        selectedDestination = visibleStartupDestination,
                        startupOptions = startupOptions,
                        remoteDpadChannelZapping = remoteDpadChannelZapping,
                        remoteDpadInvertChannelZapping = remoteDpadInvertChannelZapping,
                        remoteShowInfoOnZap = remoteShowInfoOnZap,
                        preventStandbyDuringPlayback = preventStandbyDuringPlayback,
                        autoPlayNextEpisode = autoPlayNextEpisode,
                        backgroundGradientsEnabled = backgroundGradientsEnabled,
                        showLiveSourceSwitcher = showLiveSourceSwitcher,
                        showAllChannelsCategory = showAllChannelsCategory,
                        showRecentChannelsCategory = showRecentChannelsCategory,
                        onDestinationSelected = onStartupDestinationChange,
                        onAddProvider = onAddProvider,
                        onOpenSourceSettings = { onNavigate(Routes.SETTINGS) },
                        onSourceSelected = onSourceSelected,
                        onRemoteDpadChannelZappingChange = onRemoteDpadChannelZappingChange,
                        onRemoteDpadInvertChannelZappingChange = onRemoteDpadInvertChannelZappingChange,
                        onRemoteShowInfoOnZapChange = onRemoteShowInfoOnZapChange,
                        onPreventStandbyDuringPlaybackChange = onPreventStandbyDuringPlaybackChange,
                        onAutoPlayNextEpisodeChange = onAutoPlayNextEpisodeChange,
                        onBackgroundGradientsEnabledChange = onBackgroundGradientsEnabledChange,
                        onShowLiveSourceSwitcherChange = onShowLiveSourceSwitcherChange,
                        onShowAllChannelsCategoryChange = onShowAllChannelsCategoryChange,
                        onShowRecentChannelsCategoryChange = onShowRecentChannelsCategoryChange,
                        layoutMode = layoutMode,
                        modifier = Modifier.weight(1.22f)
                    )
                }
            }

            HomeDashboardLayoutMode.NarrowCompact -> {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = outerHorizontalPadding, vertical = outerVerticalPadding),
                    horizontalArrangement = Arrangement.spacedBy(gap)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(gap)
                    ) {
                        HomeWatchWindow(
                            cards = watchCards,
                            searchEnabled = searchEnabled,
                            onNavigate = onNavigate,
                            layoutMode = layoutMode,
                            modifier = Modifier.weight(1.28f)
                        )
                        HomeSourcesShortcutWindow(
                            activeProviderName = activeProviderName,
                            providerHealth = providerHealth,
                            activeSource = activeSource,
                            sourceOptions = sourceOptions,
                            onAddProvider = onAddProvider,
                            onOpenSettings = { onNavigate(Routes.SETTINGS) },
                            onSourceSelected = onSourceSelected,
                            layoutMode = layoutMode,
                            modifier = Modifier.weight(0.72f)
                        )
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(gap)
                    ) {
                        HomeQuickWindow(
                            activeProviderName = activeProviderName,
                            providerHealth = providerHealth,
                            activeSource = activeSource,
                            sourceOptions = sourceOptions,
                            selectedDestination = visibleStartupDestination,
                            startupOptions = startupOptions,
                            remoteDpadChannelZapping = remoteDpadChannelZapping,
                            remoteDpadInvertChannelZapping = remoteDpadInvertChannelZapping,
                            remoteShowInfoOnZap = remoteShowInfoOnZap,
                            preventStandbyDuringPlayback = preventStandbyDuringPlayback,
                            autoPlayNextEpisode = autoPlayNextEpisode,
                            backgroundGradientsEnabled = backgroundGradientsEnabled,
                            showLiveSourceSwitcher = showLiveSourceSwitcher,
                            showAllChannelsCategory = showAllChannelsCategory,
                            showRecentChannelsCategory = showRecentChannelsCategory,
                            onDestinationSelected = onStartupDestinationChange,
                            onAddProvider = onAddProvider,
                            onOpenSourceSettings = { onNavigate(Routes.SETTINGS) },
                            onSourceSelected = onSourceSelected,
                            onRemoteDpadChannelZappingChange = onRemoteDpadChannelZappingChange,
                            onRemoteDpadInvertChannelZappingChange = onRemoteDpadInvertChannelZappingChange,
                            onRemoteShowInfoOnZapChange = onRemoteShowInfoOnZapChange,
                            onPreventStandbyDuringPlaybackChange = onPreventStandbyDuringPlaybackChange,
                            onAutoPlayNextEpisodeChange = onAutoPlayNextEpisodeChange,
                            onBackgroundGradientsEnabledChange = onBackgroundGradientsEnabledChange,
                            onShowLiveSourceSwitcherChange = onShowLiveSourceSwitcherChange,
                            onShowAllChannelsCategoryChange = onShowAllChannelsCategoryChange,
                            onShowRecentChannelsCategoryChange = onShowRecentChannelsCategoryChange,
                            layoutMode = layoutMode,
                            modifier = Modifier.weight(1.2f)
                        )
                        HomeAppearanceWindow(
                            cards = appearanceCards,
                            themePalettes = themePalettes,
                            activePaletteId = themePaletteId,
                            onNavigate = onNavigate,
                            onThemeSelected = onThemeSelected,
                            layoutMode = layoutMode,
                            modifier = Modifier.weight(0.8f)
                        )
                    }
                }
            }

            HomeDashboardLayoutMode.CompactGrid,
            HomeDashboardLayoutMode.SpaciousGrid -> {
                val wordmarkHeight = if (compactHome) 70.dp else 92.dp
                val watchToolbarHeight = if (compactHome) 116.dp else 144.dp
                val centeredBodyHeight = ((maxHeight - outerVerticalPadding * 2 - watchToolbarHeight - wordmarkHeight - gap * 2) * 0.92f)
                    .coerceAtMost(if (compactHome) 340.dp else 430.dp)
                    .coerceAtLeast(if (compactHome) 260.dp else 330.dp)

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = outerHorizontalPadding, vertical = outerVerticalPadding),
                    verticalArrangement = Arrangement.spacedBy(gap)
                ) {
                    HomeWatchToolbarRow(
                        cards = watchCards,
                        searchEnabled = searchEnabled,
                        onNavigate = onNavigate,
                        layoutMode = layoutMode,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(watchToolbarHeight)
                    )
                    HomeDashboardWordmark(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(wordmarkHeight)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .fillMaxWidth()
                                .height(centeredBodyHeight),
                            horizontalArrangement = Arrangement.spacedBy(gap)
                        ) {
                            HomeAppearanceWindow(
                                cards = appearanceCards,
                                themePalettes = themePalettes,
                                activePaletteId = themePaletteId,
                                onNavigate = onNavigate,
                                onThemeSelected = onThemeSelected,
                                layoutMode = layoutMode,
                                modifier = Modifier.weight(0.9f)
                            )
                            HomeQuickWindow(
                                activeProviderName = activeProviderName,
                                providerHealth = providerHealth,
                                activeSource = activeSource,
                                sourceOptions = sourceOptions,
                                selectedDestination = visibleStartupDestination,
                                startupOptions = startupOptions,
                                remoteDpadChannelZapping = remoteDpadChannelZapping,
                                remoteDpadInvertChannelZapping = remoteDpadInvertChannelZapping,
                                remoteShowInfoOnZap = remoteShowInfoOnZap,
                                preventStandbyDuringPlayback = preventStandbyDuringPlayback,
                                autoPlayNextEpisode = autoPlayNextEpisode,
                                backgroundGradientsEnabled = backgroundGradientsEnabled,
                                showLiveSourceSwitcher = showLiveSourceSwitcher,
                                showAllChannelsCategory = showAllChannelsCategory,
                                showRecentChannelsCategory = showRecentChannelsCategory,
                                onDestinationSelected = onStartupDestinationChange,
                                onAddProvider = onAddProvider,
                                onOpenSourceSettings = { onNavigate(Routes.SETTINGS) },
                                onSourceSelected = onSourceSelected,
                                onRemoteDpadChannelZappingChange = onRemoteDpadChannelZappingChange,
                                onRemoteDpadInvertChannelZappingChange = onRemoteDpadInvertChannelZappingChange,
                                onRemoteShowInfoOnZapChange = onRemoteShowInfoOnZapChange,
                                onPreventStandbyDuringPlaybackChange = onPreventStandbyDuringPlaybackChange,
                                onAutoPlayNextEpisodeChange = onAutoPlayNextEpisodeChange,
                                onBackgroundGradientsEnabledChange = onBackgroundGradientsEnabledChange,
                                onShowLiveSourceSwitcherChange = onShowLiveSourceSwitcherChange,
                                onShowAllChannelsCategoryChange = onShowAllChannelsCategoryChange,
                                onShowRecentChannelsCategoryChange = onShowRecentChannelsCategoryChange,
                                layoutMode = layoutMode,
                                modifier = Modifier.weight(0.9f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeDashboardWordmark(
    modifier: Modifier = Modifier
) {
    val orange = Color(0xFFFF8A18)
    val pink = Color(0xFFFF2D8D)
    val purple = Color(0xFFB437FF)
    Box(
        modifier = modifier.afterglow(
            specs = listOf(
                GlowSpec(orange, 18.dp, 0.20f),
                GlowSpec(pink, 34.dp, 0.18f),
                GlowSpec(purple, 58.dp, 0.12f),
            ),
            shape = RoundedCornerShape(999.dp)
        ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = buildAnnotatedString {
                withStyle(
                    SpanStyle(
                        brush = Brush.linearGradient(
                            0.00f to orange,
                            0.52f to pink,
                            1.00f to purple,
                            start = Offset.Zero,
                            end = Offset(760f, 0f),
                        )
                    )
                ) {
                    append("Afterglow Labs")
                }
            },
            style = MaterialTheme.typography.displaySmall.copy(
                fontFamily = HomeVoxRoundWideBold,
                fontWeight = FontWeight.Bold,
                fontSize = 42.sp,
                lineHeight = 48.sp,
                shadow = Shadow(
                    color = pink.copy(alpha = 0.36f),
                    offset = Offset.Zero,
                    blurRadius = 14f
                )
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(0.86f)
        )
    }
}

private enum class HomeDashboardLayoutMode {
    SpaciousGrid,
    CompactGrid,
    ShortWide,
    NarrowCompact
}

private data class HomeHubCardModel(
    val title: String,
    val subtitle: String,
    val route: String?,
    val icon: ImageVector,
    val accent: Color
)

internal enum class DashboardSourceStatus {
    ACTIVE,
    READY,
    UNAVAILABLE
}

internal data class DashboardSourceRowModel(
    val source: ActiveLiveSource,
    val title: String,
    val subtitle: String,
    val status: DashboardSourceStatus,
    val canActivate: Boolean
)

internal fun buildDashboardSourceRows(
    activeSource: ActiveLiveSource?,
    options: List<ActiveLiveSourceOption>,
    maxRows: Int
): List<DashboardSourceRowModel> =
    options
        .distinctBy { it.source }
        .sortedWith(
            compareByDescending<ActiveLiveSourceOption> { it.source == activeSource }
                .thenByDescending { it.isEnabled }
        )
        .take(maxRows.coerceAtLeast(0))
        .map { option ->
            val isActive = option.source == activeSource
            DashboardSourceRowModel(
                source = option.source,
                title = option.title.asDashboardSourceTitle(),
                subtitle = option.subtitle.orEmpty(),
                status = when {
                    isActive -> DashboardSourceStatus.ACTIVE
                    option.isEnabled -> DashboardSourceStatus.READY
                    else -> DashboardSourceStatus.UNAVAILABLE
                },
                canActivate = option.isEnabled && !isActive
            )
        }

private fun String.asDashboardSourceTitle(): String =
    if (equals("Free, Authorized Public M3U Playlist", ignoreCase = true)) {
        "Demo Playlist"
    } else {
        this
    }

private val HomeLabsPurple = Color(0xFF6F35D8)
private val HomeNeonOrange = Color(0xFFFF6A00)
private val HomeVoxRoundWideBold = FontFamily(
    Font(R.font.vox_round_wide_bold, FontWeight.Bold)
)

private val BrightHomeThemeIds = setOf(
    "afterglow_labs",
    "afterglow_gray_light",
    "afterglow_light_1",
    "afterglow_light_2",
    "afterglow_light_3",
    "afterglow_light_4",
    "rachels_sunrise",
)

private val isAfterglowLabsTheme: Boolean
    get() = AppColors.palette.id == "afterglow_labs"

private val isBrightHomeTheme: Boolean
    get() = AppColors.palette.id in BrightHomeThemeIds

private val homeOutlineColor: Color
    get() = if (isAfterglowLabsTheme) HomeLabsPurple else AppColors.Outline

private val homeFocusOutlineColor: Color
    get() = HomeNeonOrange

private val homeWindowFill: Color
    get() = if (isBrightHomeTheme) AppColors.SurfaceElevated else SurfaceElevated.copy(alpha = 0.96f)

private fun homeActionFill(accent: Color, selected: Boolean = false): Color =
    if (isBrightHomeTheme) {
        AppColors.Surface
    } else {
        accent.copy(alpha = if (selected) 0.30f else 0.22f)
    }

private fun homeActionBorderColor(accent: Color, selected: Boolean = false): Color =
    if (isBrightHomeTheme) {
        homeOutlineColor.copy(alpha = if (selected) 0.92f else 0.74f)
    } else {
        accent.copy(alpha = if (selected) 0.66f else 0.40f)
    }

private fun Modifier.homeActiveGlow(shape: RoundedCornerShape, active: Boolean = false): Modifier =
    if (active) {
        this.afterglow(
            specs = listOf(
                GlowSpec(HomeNeonOrange, 8.dp, 0.28f),
                GlowSpec(HomeNeonOrange, 18.dp, 0.14f),
            ),
            shape = shape,
        )
    } else {
        this
    }

@Composable
private fun HomeWindow(
    title: String,
    subtitle: String,
    accent: Color,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val outline = homeOutlineColor
    val shape = RoundedCornerShape(if (compact) 16.dp else 20.dp)
    Surface(
        modifier = modifier.fillMaxSize(),
        shape = shape,
        colors = SurfaceDefaults.colors(containerColor = homeWindowFill),
        border = Border(BorderStroke(if (isBrightHomeTheme || isAfterglowLabsTheme) 2.dp else 1.dp, outline.copy(alpha = if (isBrightHomeTheme || isAfterglowLabsTheme) 0.82f else 0.38f)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (isBrightHomeTheme) {
                        Modifier
                    } else {
                        Modifier.background(
                            Brush.linearGradient(
                                listOf(
                                    accent.copy(alpha = 0.08f),
                                    SurfaceElevated.copy(alpha = 0.12f),
                                    Color.Transparent
                                )
                            )
                        )
                    }
                )
                .padding(horizontal = if (compact) 12.dp else 16.dp, vertical = if (compact) 10.dp else 14.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 7.dp else 10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = if (compact) {
                        MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                    } else {
                        MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    },
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!compact) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceDim,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            content()
        }
    }
}

@Composable
private fun HomeWatchWindow(
    cards: List<HomeHubCardModel>,
    searchEnabled: Boolean,
    onNavigate: (String) -> Unit,
    layoutMode: HomeDashboardLayoutMode,
    modifier: Modifier = Modifier
) {
    val compact = layoutMode != HomeDashboardLayoutMode.SpaciousGrid
    HomeWindow(
        title = "Watch",
        subtitle = "Fast routes into playback.",
        accent = Color(0xFF60A5FA),
        modifier = modifier,
        compact = compact
    ) {
        val watchActions = cards.take(4)
        val actionGap = if (compact) 8.dp else 10.dp
        val circleSize = if (compact) 76.dp else 104.dp
        val actionColumns = if (watchActions.size <= 2) 1 else 2
        val actionGridWidth = circleSize * actionColumns + actionGap * (actionColumns - 1)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .width(actionGridWidth)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(
                    space = actionGap,
                    alignment = Alignment.CenterVertically
                )
            ) {
                watchActions.chunked(actionColumns).forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(actionGap),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        row.forEach { card ->
                            HomeWatchAction(
                                model = card,
                                modifier = Modifier.size(circleSize),
                                prominent = false,
                                compact = compact,
                                circular = true,
                                onClick = { card.route?.let(onNavigate) }
                            )
                        }
                    }
                }
            }
            if (searchEnabled) {
                HomeDashboardSearchPanel(
                    compact = compact,
                    modifier = Modifier
                        .weight(1f)
                        .height(if (compact) 120.dp else 142.dp),
                    onSubmit = { query -> onNavigate(Routes.search(query)) }
                )
            }
        }
    }
}

@Composable
private fun HomeWatchToolbarRow(
    cards: List<HomeHubCardModel>,
    searchEnabled: Boolean,
    onNavigate: (String) -> Unit,
    layoutMode: HomeDashboardLayoutMode,
    modifier: Modifier = Modifier
) {
    val compact = layoutMode != HomeDashboardLayoutMode.SpaciousGrid
    HomeWindow(
        title = "Watch",
        subtitle = "Fast routes into playback.",
        accent = Color(0xFF60A5FA),
        modifier = modifier,
        compact = compact
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 14.dp)
        ) {
            cards.take(4).forEach { card ->
                HomeWatchToolbarButton(
                    model = card,
                    compact = compact,
                    modifier = Modifier.width(if (compact) 72.dp else 88.dp),
                    onClick = { card.route?.let(onNavigate) }
                )
            }
            if (searchEnabled) {
                HomeDashboardSearchPanel(
                    compact = compact,
                    modifier = Modifier
                        .weight(1f)
                        .height(if (compact) 74.dp else 86.dp),
                    onSubmit = { query -> onNavigate(Routes.search(query)) }
                )
            } else {
                Box(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun HomeWatchToolbarButton(
    model: HomeHubCardModel,
    compact: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val circleSize = if (compact) 54.dp else 64.dp
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        HomeWatchAction(
            model = model,
            modifier = Modifier.size(circleSize),
            prominent = false,
            compact = compact,
            circular = true,
            showCircularLabel = false,
            onClick = onClick
        )
        Text(
            text = model.title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = if (compact) 10.sp else 11.sp,
                lineHeight = if (compact) 11.sp else 13.sp
            ),
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 5.dp)
        )
    }
}

@Composable
private fun HomeDashboardSearchPanel(
    compact: Boolean,
    modifier: Modifier = Modifier,
    onSubmit: (String) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    val submit = {
        val trimmed = query.trim()
        if (trimmed.isNotBlank()) {
            onSubmit(trimmed)
        }
    }
    val shape = RoundedCornerShape(14.dp)
    Surface(
        modifier = modifier
            .homeActiveGlow(shape, active = false),
        shape = shape,
        colors = SurfaceDefaults.colors(
            containerColor = if (isBrightHomeTheme) AppColors.Surface else AppColors.Surface.copy(alpha = 0.64f)
        ),
        border = Border(
            BorderStroke(
                if (isBrightHomeTheme || isAfterglowLabsTheme) 2.dp else 1.dp,
                homeActionBorderColor(Color(0xFFFF77FF))
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (compact) 10.dp else 14.dp, vertical = if (compact) 8.dp else 10.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 10.dp, Alignment.CenterVertically)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                IconBadge(
                    icon = Icons.Default.Search,
                    accent = Color(0xFFFF77FF),
                    size = if (compact) 24.dp else 30.dp
                )
                Text(
                    text = "Search",
                    style = if (compact) {
                        MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            lineHeight = 16.sp
                        )
                    } else {
                        MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp,
                            lineHeight = 20.sp
                        )
                    },
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            SearchInput(
                value = query,
                onValueChange = { query = it },
                placeholder = "Type here",
                imeAction = ImeAction.Search,
                onSearch = submit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (compact) 48.dp else 58.dp)
            )
        }
    }
}

@Composable
private fun HomeSourcesShortcutWindow(
    activeProviderName: String?,
    providerHealth: DashboardProviderHealth,
    activeSource: ActiveLiveSource?,
    sourceOptions: List<ActiveLiveSourceOption>,
    onAddProvider: () -> Unit,
    onOpenSettings: () -> Unit,
    onSourceSelected: (ActiveLiveSource) -> Unit,
    layoutMode: HomeDashboardLayoutMode,
    modifier: Modifier = Modifier
) {
    val compact = layoutMode != HomeDashboardLayoutMode.SpaciousGrid
    val rows = buildDashboardSourceRows(
        activeSource = activeSource,
        options = sourceOptions,
        maxRows = if (compact) 4 else 6
    )
    HomeWindow(
        title = "Sources",
        subtitle = "Providers and playlists.",
        accent = Color(0xFFFFD166),
        modifier = modifier,
        compact = compact
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .width(if (compact) 156.dp else 222.dp)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(
                    space = if (compact) 8.dp else 10.dp,
                    alignment = Alignment.CenterVertically
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 10.dp)
                ) {
                    HomeSourceCommandAction(
                        title = "Add Source",
                        icon = Icons.Default.Add,
                        accent = Color(0xFFFFD166),
                        modifier = Modifier
                            .weight(1f)
                            .height(if (compact) 58.dp else 72.dp),
                        compact = compact,
                        centered = true,
                        onClick = onAddProvider
                    )
                    HomeSourceCommandAction(
                        title = "Add VOD",
                        icon = Icons.Default.Star,
                        accent = Color(0xFFFF7A38),
                        modifier = Modifier
                            .weight(1f)
                            .height(if (compact) 58.dp else 72.dp),
                        compact = compact,
                        centered = true,
                        onClick = onAddProvider
                    )
                }
                HomeSourceCommandAction(
                    title = "Settings",
                    icon = Icons.Default.Menu,
                    accent = Color(0xFFB4F06B),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (compact) 50.dp else 60.dp),
                    compact = compact,
                    onClick = onOpenSettings
                )
            }
            if (rows.isEmpty()) {
                HomeSourceStatusPanel(
                    title = activeProviderName ?: "No playlist connected",
                    subtitle = if (activeProviderName != null) {
                        "${providerTypeLabel(providerHealth.type)} - ${providerStatusLabel(providerHealth.status)}"
                    } else {
                        "Add a playlist or provider"
                    },
                    compact = compact,
                    modifier = Modifier.weight(1f)
                )
            } else {
                HomeSourceTileGrid(
                    rows = rows,
                    compact = compact,
                    modifier = Modifier.weight(1f),
                    onSourceSelected = onSourceSelected
                )
            }
        }
    }
}

@Composable
private fun HomeSourceTileGrid(
    rows: List<DashboardSourceRowModel>,
    compact: Boolean,
    modifier: Modifier = Modifier,
    onSourceSelected: (ActiveLiveSource) -> Unit
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val gap = if (compact) 10.dp else 14.dp
        val tileHeight = if (compact) 42.dp else 52.dp
        val rowCount = (rows.size + 1) / 2

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(
                space = if (compact) 8.dp else 10.dp,
                alignment = Alignment.CenterVertically
            )
        ) {
            repeat(rowCount) { rowIndex ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(tileHeight),
                    horizontalArrangement = Arrangement.spacedBy(gap)
                ) {
                    repeat(2) { columnIndex ->
                        val sourceIndex = rowIndex * 2 + columnIndex
                        if (sourceIndex < rows.size) {
                            val row = rows[sourceIndex]
                            HomeSourceRow(
                                model = row,
                                compact = compact,
                                circular = false,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                onClick = { onSourceSelected(row.source) }
                            )
                        } else {
                            Box(
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeSourceStatusPanel(
    title: String,
    subtitle: String,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(14.dp)
    Surface(
        modifier = modifier.fillMaxSize(),
        shape = shape,
        colors = SurfaceDefaults.colors(
            containerColor = if (isBrightHomeTheme) AppColors.Surface else AppColors.Surface.copy(alpha = 0.62f)
        ),
        border = Border(
            BorderStroke(
                if (isBrightHomeTheme || isAfterglowLabsTheme) 2.dp else 1.dp,
                homeActionBorderColor(Color(0xFFFFD166))
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (compact) 10.dp else 14.dp, vertical = if (compact) 8.dp else 12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = if (compact) {
                    MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                } else {
                    MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                },
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = if (compact) 10.sp else 11.sp,
                    lineHeight = if (compact) 12.sp else 14.sp
                ),
                color = OnSurfaceDim,
                maxLines = if (compact) 2 else 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun HomeSourceRow(
    model: DashboardSourceRowModel,
    compact: Boolean,
    circular: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val selected = model.status == DashboardSourceStatus.ACTIVE
    val accent = when (model.status) {
        DashboardSourceStatus.ACTIVE -> HomeNeonOrange
        DashboardSourceStatus.READY -> Color(0xFF5EEAD4)
        DashboardSourceStatus.UNAVAILABLE -> OnSurfaceDim
    }
    val shape = RoundedCornerShape(if (circular) 999.dp else if (selected) 10.dp else 8.dp)
    val borderColor = when (model.status) {
        DashboardSourceStatus.ACTIVE -> HomeNeonOrange
        DashboardSourceStatus.READY -> accent.copy(alpha = 0.88f)
        DashboardSourceStatus.UNAVAILABLE -> OnSurfaceDim.copy(alpha = 0.44f)
    }
    TvClickableSurface(
        onClick = { if (model.canActivate) onClick() },
        enabled = model.status != DashboardSourceStatus.UNAVAILABLE,
        modifier = modifier
            .fillMaxSize()
            .homeActiveGlow(shape, active = selected),
        shape = ClickableSurfaceDefaults.shape(shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isBrightHomeTheme) {
                AppColors.Surface.copy(alpha = 0.72f)
            } else if (selected) {
                HomeNeonOrange.copy(alpha = 0.07f)
            } else {
                Color.Transparent
            },
            focusedContainerColor = if (isBrightHomeTheme) {
                AppColors.Surface
            } else {
                AppColors.Surface.copy(alpha = 0.18f)
            },
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = BorderStroke(
                    if (selected) 3.dp else 2.dp,
                    borderColor
                ),
                shape = shape
            ),
            focusedBorder = Border(
                border = BorderStroke(3.dp, homeFocusOutlineColor),
                shape = shape
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (circular) 10.dp else if (compact) 8.dp else 10.dp, vertical = if (circular) 10.dp else if (compact) 7.dp else 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = model.title,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = if (compact) 11.sp else 12.sp,
                    lineHeight = if (compact) 12.sp else 14.sp
                ),
                color = TextPrimary.copy(alpha = if (model.status == DashboardSourceStatus.UNAVAILABLE) 0.68f else 1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Text(
                text = sourceStatusLabel(model.status),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = if (compact) 9.sp else 10.sp,
                    lineHeight = if (compact) 10.sp else 12.sp
                ),
                color = borderColor.copy(alpha = if (model.status == DashboardSourceStatus.UNAVAILABLE) 0.58f else 0.92f),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun HomeSourceStatusBadge(
    status: DashboardSourceStatus,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    val accent = when (status) {
        DashboardSourceStatus.ACTIVE -> Primary
        DashboardSourceStatus.READY -> Color(0xFF5EEAD4)
        DashboardSourceStatus.UNAVAILABLE -> OnSurfaceDim
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(accent.copy(alpha = if (status == DashboardSourceStatus.UNAVAILABLE) 0.14f else 0.18f))
            .padding(horizontal = if (compact) 6.dp else 7.dp, vertical = if (compact) 2.dp else 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = sourceStatusLabel(status),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = if (compact) 9.sp else 10.sp,
                lineHeight = if (compact) 10.sp else 12.sp
            ),
            color = accent,
            maxLines = 1
        )
    }
}

@Composable
private fun HomeSourceCommandAction(
    title: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    centered: Boolean = false,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(8.dp)
    TvClickableSurface(
        onClick = onClick,
        modifier = modifier
            .fillMaxSize()
            .homeActiveGlow(shape, active = isAfterglowLabsTheme && !compact),
        shape = ClickableSurfaceDefaults.shape(shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isBrightHomeTheme) AppColors.Surface.copy(alpha = 0.72f) else Color.Transparent,
            focusedContainerColor = if (isBrightHomeTheme) AppColors.Surface else AppColors.Surface.copy(alpha = 0.18f)
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = BorderStroke(
                    2.dp,
                    accent.copy(alpha = if (isBrightHomeTheme) 0.82f else 0.72f)
                ),
                shape = shape
            ),
            focusedBorder = Border(
                border = BorderStroke(3.dp, homeFocusOutlineColor),
                shape = shape
            )
        )
    ) {
        if (centered) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = if (compact) 6.dp else 8.dp, vertical = if (compact) 5.dp else 7.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                IconBadge(icon = icon, accent = accent, size = if (compact) 20.dp else 24.dp)
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = if (compact) 10.sp else 12.sp,
                        lineHeight = if (compact) 11.sp else 14.sp
                    ),
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = if (compact) 10.dp else 12.dp, vertical = if (compact) 6.dp else 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp)
            ) {
                IconBadge(icon = icon, accent = accent, size = if (compact) 22.dp else 26.dp)
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = if (compact) 12.sp else 14.sp,
                        lineHeight = if (compact) 14.sp else 16.sp
                    ),
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun sourceStatusLabel(status: DashboardSourceStatus): String = when (status) {
    DashboardSourceStatus.ACTIVE -> "Active"
    DashboardSourceStatus.READY -> "Ready"
    DashboardSourceStatus.UNAVAILABLE -> "Offline"
}

private fun providerStatusLabel(status: ProviderStatus): String = when (status) {
    ProviderStatus.ACTIVE -> "Active"
    ProviderStatus.PARTIAL -> "Importing"
    ProviderStatus.EXPIRED -> "Expired"
    ProviderStatus.DISABLED -> "Disabled"
    ProviderStatus.ERROR -> "Error"
    ProviderStatus.UNKNOWN -> "Unknown"
}

private fun providerTypeLabel(type: ProviderType): String = when (type) {
    ProviderType.XTREAM_CODES -> "Xtream"
    ProviderType.M3U -> "M3U"
    ProviderType.STALKER_PORTAL -> "Stalker"
}

@Composable
private fun HomeAppearanceWindow(
    cards: List<HomeHubCardModel>,
    themePalettes: List<AppPalette>,
    activePaletteId: String,
    onNavigate: (String) -> Unit,
    onThemeSelected: (AppPalette) -> Unit,
    layoutMode: HomeDashboardLayoutMode,
    modifier: Modifier = Modifier
) {
    val compact = layoutMode != HomeDashboardLayoutMode.SpaciousGrid
    HomeWindow(
        title = "Appearance",
        subtitle = "Visual shortcuts.",
        accent = Color(0xFFFF77FF),
        modifier = modifier,
        compact = compact
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(if (compact) 0.72f else 0.82f),
                horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp)
            ) {
                cards.drop(1).forEach { card ->
                    HomeSmallTextAction(
                        title = card.title,
                        subtitle = card.subtitle,
                        icon = card.icon,
                        accent = card.accent,
                        modifier = Modifier.weight(1f),
                        compact = compact,
                        onClick = { card.route?.let(onNavigate) }
                    )
                }
            }
            HomeThemeFeatureAction(
                model = cards[0],
                palettes = themePalettes,
                activePaletteId = activePaletteId,
                compact = compact,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(if (compact) 1.28f else 1.18f),
                onOpenThemes = { cards[0].route?.let(onNavigate) },
                onThemeSelected = onThemeSelected
            )
        }
    }
}

@Composable
private fun HomeQuickWindow(
    activeProviderName: String?,
    providerHealth: DashboardProviderHealth,
    activeSource: ActiveLiveSource?,
    sourceOptions: List<ActiveLiveSourceOption>,
    selectedDestination: StartupDestination,
    startupOptions: List<StartupDestination>,
    remoteDpadChannelZapping: Boolean,
    remoteDpadInvertChannelZapping: Boolean,
    remoteShowInfoOnZap: Boolean,
    preventStandbyDuringPlayback: Boolean,
    autoPlayNextEpisode: Boolean,
    backgroundGradientsEnabled: Boolean,
    showLiveSourceSwitcher: Boolean,
    showAllChannelsCategory: Boolean,
    showRecentChannelsCategory: Boolean,
    onDestinationSelected: (StartupDestination) -> Unit,
    onAddProvider: () -> Unit,
    onOpenSourceSettings: () -> Unit,
    onSourceSelected: (ActiveLiveSource) -> Unit,
    onRemoteDpadChannelZappingChange: (Boolean) -> Unit,
    onRemoteDpadInvertChannelZappingChange: (Boolean) -> Unit,
    onRemoteShowInfoOnZapChange: (Boolean) -> Unit,
    onPreventStandbyDuringPlaybackChange: (Boolean) -> Unit,
    onAutoPlayNextEpisodeChange: (Boolean) -> Unit,
    onBackgroundGradientsEnabledChange: (Boolean) -> Unit,
    onShowLiveSourceSwitcherChange: (Boolean) -> Unit,
    onShowAllChannelsCategoryChange: (Boolean) -> Unit,
    onShowRecentChannelsCategoryChange: (Boolean) -> Unit,
    layoutMode: HomeDashboardLayoutMode,
    modifier: Modifier = Modifier
) {
    val accent = Color(0xFFFF7A38)
    val outline = homeOutlineColor
    val compact = layoutMode != HomeDashboardLayoutMode.SpaciousGrid
    val shape = RoundedCornerShape(if (compact) 16.dp else 20.dp)
    Surface(
        modifier = modifier.fillMaxSize(),
        shape = shape,
        colors = SurfaceDefaults.colors(containerColor = homeWindowFill),
        border = Border(BorderStroke(if (isBrightHomeTheme || isAfterglowLabsTheme) 2.dp else 1.dp, outline.copy(alpha = if (isBrightHomeTheme || isAfterglowLabsTheme) 0.82f else 0.38f)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (isBrightHomeTheme) {
                        Modifier
                    } else {
                        Modifier.background(
                            Brush.linearGradient(
                                listOf(
                                    accent.copy(alpha = 0.08f),
                                    SurfaceElevated.copy(alpha = 0.12f),
                                    Color.Transparent
                                )
                            )
                        )
                    }
                )
                .padding(horizontal = if (compact) 12.dp else 16.dp, vertical = if (compact) 10.dp else 12.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 7.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Quick Settings",
                    style = if (compact) {
                        MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                    } else {
                        MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    },
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (compact) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    HomeSourcesInlineSection(
                        activeProviderName = activeProviderName,
                        providerHealth = providerHealth,
                        activeSource = activeSource,
                        sourceOptions = sourceOptions,
                        compact = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(104.dp),
                        onAddProvider = onAddProvider,
                        onOpenSettings = onOpenSourceSettings,
                        onSourceSelected = onSourceSelected
                    )
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        HomeStartupOptionsColumn(
                            selectedDestination = selectedDestination,
                            startupOptions = startupOptions,
                            compact = true,
                            modifier = Modifier.weight(1f),
                            onDestinationSelected = onDestinationSelected
                        )
                        HomeLiveBrowsingOptionsColumn(
                            showLiveSourceSwitcher = showLiveSourceSwitcher,
                            showAllChannelsCategory = showAllChannelsCategory,
                            showRecentChannelsCategory = showRecentChannelsCategory,
                            compact = true,
                            modifier = Modifier.weight(1f),
                            onShowLiveSourceSwitcherChange = onShowLiveSourceSwitcherChange,
                            onShowAllChannelsCategoryChange = onShowAllChannelsCategoryChange,
                            onShowRecentChannelsCategoryChange = onShowRecentChannelsCategoryChange
                        )
                    }
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        HomeRemoteOptionsColumn(
                            remoteDpadChannelZapping = remoteDpadChannelZapping,
                            remoteDpadInvertChannelZapping = remoteDpadInvertChannelZapping,
                            remoteShowInfoOnZap = remoteShowInfoOnZap,
                            compact = true,
                            modifier = Modifier.weight(1f),
                            onRemoteDpadChannelZappingChange = onRemoteDpadChannelZappingChange,
                            onRemoteDpadInvertChannelZappingChange = onRemoteDpadInvertChannelZappingChange,
                            onRemoteShowInfoOnZapChange = onRemoteShowInfoOnZapChange
                        )
                        HomePlaybackOptionsColumn(
                            preventStandbyDuringPlayback = preventStandbyDuringPlayback,
                            autoPlayNextEpisode = autoPlayNextEpisode,
                            backgroundGradientsEnabled = backgroundGradientsEnabled,
                            compact = true,
                            modifier = Modifier.weight(1f),
                            onPreventStandbyDuringPlaybackChange = onPreventStandbyDuringPlaybackChange,
                            onAutoPlayNextEpisodeChange = onAutoPlayNextEpisodeChange,
                            onBackgroundGradientsEnabledChange = onBackgroundGradientsEnabledChange
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HomeSourcesInlineSection(
                        activeProviderName = activeProviderName,
                        providerHealth = providerHealth,
                        activeSource = activeSource,
                        sourceOptions = sourceOptions,
                        compact = false,
                        modifier = Modifier
                            .weight(1.05f)
                            .fillMaxHeight(),
                        onAddProvider = onAddProvider,
                        onOpenSettings = onOpenSourceSettings,
                        onSourceSelected = onSourceSelected
                    )
                    Column(
                        modifier = Modifier.weight(1.55f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            HomeStartupOptionsColumn(
                                selectedDestination = selectedDestination,
                                startupOptions = startupOptions,
                                compact = false,
                                modifier = Modifier.weight(0.95f),
                                onDestinationSelected = onDestinationSelected
                            )
                            HomeLiveBrowsingOptionsColumn(
                                showLiveSourceSwitcher = showLiveSourceSwitcher,
                                showAllChannelsCategory = showAllChannelsCategory,
                                showRecentChannelsCategory = showRecentChannelsCategory,
                                compact = false,
                                modifier = Modifier.weight(1f),
                                onShowLiveSourceSwitcherChange = onShowLiveSourceSwitcherChange,
                                onShowAllChannelsCategoryChange = onShowAllChannelsCategoryChange,
                                onShowRecentChannelsCategoryChange = onShowRecentChannelsCategoryChange
                            )
                        }
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            HomeRemoteOptionsColumn(
                                remoteDpadChannelZapping = remoteDpadChannelZapping,
                                remoteDpadInvertChannelZapping = remoteDpadInvertChannelZapping,
                                remoteShowInfoOnZap = remoteShowInfoOnZap,
                                compact = false,
                                modifier = Modifier.weight(1.08f),
                                onRemoteDpadChannelZappingChange = onRemoteDpadChannelZappingChange,
                                onRemoteDpadInvertChannelZappingChange = onRemoteDpadInvertChannelZappingChange,
                                onRemoteShowInfoOnZapChange = onRemoteShowInfoOnZapChange
                            )
                            HomePlaybackOptionsColumn(
                                preventStandbyDuringPlayback = preventStandbyDuringPlayback,
                                autoPlayNextEpisode = autoPlayNextEpisode,
                                backgroundGradientsEnabled = backgroundGradientsEnabled,
                                compact = false,
                                modifier = Modifier.weight(1f),
                                onPreventStandbyDuringPlaybackChange = onPreventStandbyDuringPlaybackChange,
                                onAutoPlayNextEpisodeChange = onAutoPlayNextEpisodeChange,
                                onBackgroundGradientsEnabledChange = onBackgroundGradientsEnabledChange
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeSourcesInlineSection(
    activeProviderName: String?,
    providerHealth: DashboardProviderHealth,
    activeSource: ActiveLiveSource?,
    sourceOptions: List<ActiveLiveSourceOption>,
    compact: Boolean,
    modifier: Modifier = Modifier,
    onAddProvider: () -> Unit,
    onOpenSettings: () -> Unit,
    onSourceSelected: (ActiveLiveSource) -> Unit
) {
    val rows = buildDashboardSourceRows(
        activeSource = activeSource,
        options = sourceOptions,
        maxRows = if (compact) 4 else 6
    )
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.width(if (compact) 148.dp else 184.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp)
            ) {
                HomeSourceCommandAction(
                    title = "Add Source",
                    icon = Icons.Default.Add,
                    accent = Color(0xFFFFD166),
                    modifier = Modifier
                        .weight(1f)
                        .height(if (compact) 44.dp else 54.dp),
                    compact = true,
                    centered = true,
                    onClick = onAddProvider
                )
                HomeSourceCommandAction(
                    title = "Add VOD",
                    icon = Icons.Default.Star,
                    accent = Color(0xFFFF7A38),
                    modifier = Modifier
                        .weight(1f)
                        .height(if (compact) 44.dp else 54.dp),
                    compact = true,
                    centered = true,
                    onClick = onAddProvider
                )
            }
            HomeSourceCommandAction(
                title = "Settings",
                icon = Icons.Default.Menu,
                accent = Color(0xFFB4F06B),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (compact) 38.dp else 46.dp),
                compact = true,
                onClick = onOpenSettings
            )
        }
        if (rows.isEmpty()) {
            HomeSourceStatusPanel(
                title = activeProviderName ?: "No playlist connected",
                subtitle = if (activeProviderName != null) {
                    "${providerTypeLabel(providerHealth.type)} - ${providerStatusLabel(providerHealth.status)}"
                } else {
                    "Add a playlist or provider"
                },
                compact = true,
                modifier = Modifier.weight(1f)
            )
        } else {
            HomeSourceTileGrid(
                rows = rows,
                compact = true,
                modifier = Modifier.weight(1f),
                onSourceSelected = onSourceSelected
            )
        }
    }
}

@Composable
private fun HomeStartupOptionsColumn(
    selectedDestination: StartupDestination,
    startupOptions: List<StartupDestination>,
    compact: Boolean,
    modifier: Modifier = Modifier,
    onDestinationSelected: (StartupDestination) -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (compact) 5.dp else 6.dp)
    ) {
        HomeQuickSectionLabel(text = "App start page", compact = compact)
        startupOptions.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(if (compact) 5.dp else 6.dp)
            ) {
                row.forEach { destination ->
                    HomeStartupChip(
                        destination = destination,
                        selected = destination == selectedDestination,
                        compact = compact,
                        modifier = Modifier.weight(1f),
                        onClick = { onDestinationSelected(destination) }
                    )
                }
                if (row.size == 1) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun HomeLiveBrowsingOptionsColumn(
    showLiveSourceSwitcher: Boolean,
    showAllChannelsCategory: Boolean,
    showRecentChannelsCategory: Boolean,
    compact: Boolean,
    modifier: Modifier = Modifier,
    onShowLiveSourceSwitcherChange: (Boolean) -> Unit,
    onShowAllChannelsCategoryChange: (Boolean) -> Unit,
    onShowRecentChannelsCategoryChange: (Boolean) -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (compact) 3.dp else 4.dp)
    ) {
        HomeQuickSectionLabel(text = "Live browsing", compact = compact)
        HomeMiniToggle(
            title = "Source switcher",
            checked = showLiveSourceSwitcher,
            compact = compact,
            onCheckedChange = onShowLiveSourceSwitcherChange
        )
        HomeMiniToggle(
            title = "All channels",
            checked = showAllChannelsCategory,
            compact = compact,
            onCheckedChange = onShowAllChannelsCategoryChange
        )
        HomeMiniToggle(
            title = "Recent row",
            checked = showRecentChannelsCategory,
            compact = compact,
            onCheckedChange = onShowRecentChannelsCategoryChange
        )
    }
}

@Composable
private fun HomeRemoteOptionsColumn(
    remoteDpadChannelZapping: Boolean,
    remoteDpadInvertChannelZapping: Boolean,
    remoteShowInfoOnZap: Boolean,
    compact: Boolean,
    modifier: Modifier = Modifier,
    onRemoteDpadChannelZappingChange: (Boolean) -> Unit,
    onRemoteDpadInvertChannelZappingChange: (Boolean) -> Unit,
    onRemoteShowInfoOnZapChange: (Boolean) -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (compact) 3.dp else 4.dp)
    ) {
        HomeQuickSectionLabel(text = "Remote control", compact = compact)
        HomeMiniToggle(
            title = "D-pad channel +/-",
            checked = remoteDpadChannelZapping,
            compact = compact,
            onCheckedChange = onRemoteDpadChannelZappingChange
        )
        HomeMiniToggle(
            title = "Reverse channel order",
            checked = remoteDpadInvertChannelZapping,
            enabled = remoteDpadChannelZapping,
            compact = compact,
            onCheckedChange = onRemoteDpadInvertChannelZappingChange
        )
        HomeMiniToggle(
            title = "Show info while changing",
            checked = remoteShowInfoOnZap,
            compact = compact,
            onCheckedChange = onRemoteShowInfoOnZapChange
        )
    }
}

@Composable
private fun HomePlaybackOptionsColumn(
    preventStandbyDuringPlayback: Boolean,
    autoPlayNextEpisode: Boolean,
    backgroundGradientsEnabled: Boolean,
    compact: Boolean,
    modifier: Modifier = Modifier,
    onPreventStandbyDuringPlaybackChange: (Boolean) -> Unit,
    onAutoPlayNextEpisodeChange: (Boolean) -> Unit,
    onBackgroundGradientsEnabledChange: (Boolean) -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (compact) 3.dp else 4.dp)
    ) {
        HomeQuickSectionLabel(text = "Playback & look", compact = compact)
        HomeMiniToggle(
            title = "Keep awake",
            checked = preventStandbyDuringPlayback,
            compact = compact,
            onCheckedChange = onPreventStandbyDuringPlaybackChange
        )
        HomeMiniToggle(
            title = "Autoplay next",
            checked = autoPlayNextEpisode,
            compact = compact,
            onCheckedChange = onAutoPlayNextEpisodeChange
        )
        HomeMiniToggle(
            title = "Gradients",
            checked = backgroundGradientsEnabled,
            compact = compact,
            onCheckedChange = onBackgroundGradientsEnabledChange
        )
    }
}

@Composable
private fun HomeQuickSectionLabel(
    text: String,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = if (compact) {
            MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 9.sp,
                lineHeight = 11.sp
            )
        } else {
            MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
        },
        color = OnSurfaceDim,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.fillMaxWidth(),
        textAlign = TextAlign.Center
    )
}

@Composable
private fun HomeThemeFeatureAction(
    model: HomeHubCardModel,
    palettes: List<AppPalette>,
    activePaletteId: String,
    compact: Boolean,
    modifier: Modifier = Modifier,
    onOpenThemes: () -> Unit,
    onThemeSelected: (AppPalette) -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val outline = homeOutlineColor
    Surface(
        modifier = modifier.fillMaxSize(),
        shape = shape,
        colors = SurfaceDefaults.colors(
            containerColor = if (isBrightHomeTheme) AppColors.Surface else AppColors.Surface.copy(alpha = 0.70f)
        ),
        border = Border(
            BorderStroke(
                if (isBrightHomeTheme || isAfterglowLabsTheme) 2.dp else 1.dp,
                if (isAfterglowLabsTheme) outline.copy(alpha = 0.84f) else homeActionBorderColor(model.accent)
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (compact) 12.dp else 14.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 7.dp else 9.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        text = model.title,
                        style = if (compact) {
                            MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                        } else {
                            MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                        },
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = model.subtitle,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = if (compact) 10.sp else 11.sp,
                            lineHeight = if (compact) 12.sp else 14.sp
                        ),
                        color = OnSurfaceDim,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                HomeOpenThemesButton(
                    compact = compact,
                    onClick = onOpenThemes
                )
            }
            HomeThemeSwatchGrid(
                palettes = palettes,
                activePaletteId = activePaletteId,
                compact = compact,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                onThemeSelected = onThemeSelected
            )
        }
    }
}

@Composable
private fun HomeOpenThemesButton(
    compact: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(7.dp)
    TvClickableSurface(
        onClick = onClick,
        modifier = modifier
            .width(if (compact) 48.dp else 56.dp)
            .height(if (compact) 24.dp else 28.dp),
        shape = ClickableSurfaceDefaults.shape(shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = SurfaceHighlight
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        border = ClickableSurfaceDefaults.border(
            border = Border(BorderStroke(1.dp, homeActionBorderColor(Color(0xFFFF77FF))), shape = shape),
            focusedBorder = Border(BorderStroke(3.dp, homeFocusOutlineColor), shape = shape)
        )
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "More",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = if (compact) 9.sp else 10.sp,
                    lineHeight = if (compact) 10.sp else 12.sp
                ),
                color = TextPrimary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun HomeThemeSwatchGrid(
    palettes: List<AppPalette>,
    activePaletteId: String,
    compact: Boolean,
    modifier: Modifier = Modifier,
    onThemeSelected: (AppPalette) -> Unit
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val gap = if (compact) 6.dp else 8.dp
        val swatchSize = if (compact) 24.dp else 30.dp
        val columns = ((maxWidth.value + gap.value) / (swatchSize.value + gap.value))
            .toInt()
            .coerceAtLeast(4)
        val rows = ((maxHeight.value + gap.value) / (swatchSize.value + gap.value))
            .toInt()
            .coerceAtLeast(1)
        val visiblePalettes = palettes.take((columns * rows).coerceAtLeast(1))
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(gap)
        ) {
            visiblePalettes.chunked(columns).forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(gap),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    row.forEach { palette ->
                        HomeThemeSwatch(
                            palette = palette,
                            selected = palette.id == activePaletteId,
                            size = swatchSize,
                            onClick = { onThemeSelected(palette) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeThemeSwatch(
    palette: AppPalette,
    selected: Boolean,
    size: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape((size.value / 4f).dp)
    TvClickableSurface(
        onClick = onClick,
        modifier = Modifier
            .size(size)
            .homeActiveGlow(shape, active = selected),
        shape = ClickableSurfaceDefaults.shape(shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = Color.Transparent,
            pressedContainerColor = Color.Transparent
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) homeFocusOutlineColor else AppColors.Outline.copy(alpha = 0.45f)),
                shape = shape
            ),
            focusedBorder = Border(border = BorderStroke(3.dp, homeFocusOutlineColor), shape = shape)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .background(Brush.linearGradient(palette.dashboardSwatchColors()))
        )
    }
}

private fun AppPalette.dashboardSwatchColors(): List<Color> =
    listOf(
        surfaceDeep,
        surfaceBase,
        surfaceCool,
        surfaceAccent,
        accent,
        accentLight,
        nowLine,
        live
    ).distinct()

@Composable
private fun HomeWatchAction(
    model: HomeHubCardModel,
    modifier: Modifier = Modifier,
    prominent: Boolean,
    compact: Boolean = false,
    circular: Boolean = false,
    showCircularLabel: Boolean = true,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(if (circular) 999.dp else 14.dp)
    val outline = homeOutlineColor
    val iconSize = when {
        circular && !compact -> 32.dp
        circular -> 28.dp
        prominent && !compact -> 34.dp
        prominent -> 34.dp
        compact -> 24.dp
        else -> 28.dp
    }
    TvClickableSurface(
        onClick = onClick,
        modifier = modifier
            .fillMaxSize()
            .homeActiveGlow(shape, active = isAfterglowLabsTheme && prominent),
        shape = ClickableSurfaceDefaults.shape(shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isBrightHomeTheme) AppColors.Surface else AppColors.Surface.copy(alpha = 0.68f),
            focusedContainerColor = SurfaceHighlight
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = BorderStroke(
                    if (isBrightHomeTheme || isAfterglowLabsTheme) 2.dp else 1.dp,
                    if (isAfterglowLabsTheme) outline.copy(alpha = 0.78f) else homeActionBorderColor(model.accent)
                ),
                shape = shape
            ),
            focusedBorder = Border(
                border = BorderStroke(3.dp, homeFocusOutlineColor),
                shape = shape
            )
        )
    ) {
        if (circular) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                IconBadge(
                    icon = model.icon,
                    accent = model.accent,
                    size = iconSize
                )
                if (showCircularLabel) {
                    Text(
                        text = model.title,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = if (compact) 10.sp else 12.sp,
                            lineHeight = if (compact) 12.sp else 14.sp
                        ),
                        color = TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 5.dp)
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = when {
                            compact -> 9.dp
                            prominent -> 14.dp
                            else -> 12.dp
                        },
                        vertical = when {
                            compact -> 6.dp
                            prominent -> 10.dp
                            else -> 8.dp
                        }
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (compact) 7.dp else if (prominent) 12.dp else 9.dp)
            ) {
                IconBadge(
                    icon = model.icon,
                    accent = model.accent,
                    size = iconSize
                )
                Text(
                    text = model.title,
                    style = if (compact && prominent) {
                        MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp,
                            lineHeight = 22.sp
                        )
                    } else if (compact) {
                        MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            lineHeight = 17.sp
                        )
                    } else if (prominent) {
                        MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 28.sp
                        )
                    } else {
                        MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 22.sp
                        )
                    },
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun HomeSmallTextAction(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    val outline = homeOutlineColor
    val iconSize = if (compact) 20.dp else 30.dp
    val verticalPadding = if (compact) 4.dp else 10.dp
    val horizontalPadding = if (compact) 9.dp else 12.dp
    val horizontalGap = if (compact) 7.dp else 10.dp
    val titleStyle = if (compact) {
        MaterialTheme.typography.labelLarge.copy(
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            lineHeight = 15.sp
        )
    } else {
        MaterialTheme.typography.titleSmall.copy(
            fontWeight = FontWeight.SemiBold,
            lineHeight = 22.sp
        )
    }
    val subtitleStyle = if (compact) {
        MaterialTheme.typography.labelSmall.copy(
            fontSize = 9.sp,
            lineHeight = 10.sp
        )
    } else {
        MaterialTheme.typography.labelSmall.copy(
            fontSize = 11.sp,
            lineHeight = 15.sp
        )
    }
    TvClickableSurface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .homeActiveGlow(shape, active = isAfterglowLabsTheme && !compact),
        shape = ClickableSurfaceDefaults.shape(shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isBrightHomeTheme) AppColors.Surface else AppColors.Surface.copy(alpha = 0.70f),
            focusedContainerColor = SurfaceHighlight
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = BorderStroke(if (isBrightHomeTheme || isAfterglowLabsTheme) 2.dp else 1.dp, if (isAfterglowLabsTheme) outline.copy(alpha = 0.76f) else homeActionBorderColor(accent)),
                shape = shape
            ),
            focusedBorder = Border(
                border = BorderStroke(3.dp, homeFocusOutlineColor),
                shape = shape
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding, vertical = verticalPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(horizontalGap)
        ) {
            IconBadge(icon = icon, accent = accent, size = iconSize)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = title,
                    style = titleStyle,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = subtitleStyle,
                    color = OnSurfaceDim,
                    maxLines = if (compact) 2 else 1,
                    overflow = if (compact) TextOverflow.Clip else TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun HomeStartupChip(
    destination: StartupDestination,
    selected: Boolean,
    compact: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val title = when (destination) {
        StartupDestination.HOME -> "Home"
        StartupDestination.LIVE_TV -> "Live TV"
        StartupDestination.IPTV_GUIDE -> "TV Guide"
        else -> stringResource(destination.labelResId)
    }
    val accent = if (selected) HomeNeonOrange else Color(0xFF7DD3FC)
    val shape = RoundedCornerShape(7.dp)
    val outline = homeOutlineColor
    TvClickableSurface(
        onClick = onClick,
        modifier = modifier
            .height(if (compact) 25.dp else 33.dp)
            .homeActiveGlow(shape, active = selected),
        shape = ClickableSurfaceDefaults.shape(shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isBrightHomeTheme) AppColors.Surface else accent.copy(alpha = if (selected) 0.28f else 0.16f),
            focusedContainerColor = SurfaceHighlight
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = BorderStroke(
                    if (isAfterglowLabsTheme) {
                        if (selected) 4.dp else 2.dp
                    } else {
                        if (selected) 2.dp else 1.dp
                    },
                    if (isAfterglowLabsTheme) outline.copy(alpha = if (selected) 0.95f else 0.64f) else homeActionBorderColor(accent, selected)
                ),
                shape = shape
            ),
            focusedBorder = Border(
                border = BorderStroke(3.dp, homeFocusOutlineColor),
                shape = shape
            )
        )
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = title,
                style = if (compact) {
                    MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 9.sp,
                        lineHeight = 11.sp
                    )
                } else {
                    MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                },
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun HomeMiniToggle(
    title: String,
    checked: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    compact: Boolean = false,
    onCheckedChange: (Boolean) -> Unit
) {
    val accent = if (checked) HomeNeonOrange else Color(0xFF7DD3FC)
    val shape = RoundedCornerShape(7.dp)
    val outline = homeOutlineColor
    TvClickableSurface(
        onClick = { if (enabled) onCheckedChange(!checked) },
        modifier = modifier
            .fillMaxWidth()
            .height(if (compact) 25.dp else 33.dp)
            .homeActiveGlow(shape, active = checked),
        enabled = enabled,
        shape = ClickableSurfaceDefaults.shape(shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isBrightHomeTheme) AppColors.Surface else accent.copy(alpha = if (checked) 0.24f else 0.14f),
            focusedContainerColor = SurfaceHighlight
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = BorderStroke(
                    if (isAfterglowLabsTheme) {
                        if (checked) 4.dp else 2.dp
                    } else {
                        1.dp
                    },
                    if (isAfterglowLabsTheme) outline.copy(alpha = if (checked) 0.95f else 0.60f) else homeActionBorderColor(accent, checked)
                ),
                shape = shape
            ),
            focusedBorder = Border(
                border = BorderStroke(3.dp, homeFocusOutlineColor),
                shape = shape
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (compact) 7.dp else 10.dp, vertical = if (compact) 4.dp else 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (compact) 5.dp else 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(if (compact) 7.dp else 9.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accent.copy(alpha = if (enabled) 1f else 0.35f))
            )
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = if (compact) 9.sp else 11.sp,
                    lineHeight = if (compact) 11.sp else 14.sp
                ),
                color = TextPrimary.copy(alpha = if (enabled) 1f else 0.48f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (checked) "On" else "Off",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = if (compact) 9.sp else 11.sp,
                    lineHeight = if (compact) 11.sp else 14.sp
                ),
                color = OnSurfaceDim.copy(alpha = if (enabled) 1f else 0.48f),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun IconBadge(
    icon: ImageVector,
    accent: Color,
    size: androidx.compose.ui.unit.Dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape((size.value / 3f).dp))
            .background(accent.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size((size.value * 0.56f).dp)
        )
    }
}
