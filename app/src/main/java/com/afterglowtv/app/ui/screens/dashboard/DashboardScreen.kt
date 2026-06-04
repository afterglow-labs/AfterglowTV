package com.afterglowtv.app.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.afterglowtv.app.R
import com.afterglowtv.app.device.rememberIsTelevisionDevice
import com.afterglowtv.app.ui.components.ChannelLogoBadge
import com.afterglowtv.app.navigation.StartupDestination
import com.afterglowtv.app.navigation.Routes
import com.afterglowtv.app.store.StorePolicy
import com.afterglowtv.app.store.StorePolicySnapshot
import com.afterglowtv.app.ui.components.CategoryRow
import com.afterglowtv.app.ui.components.ChannelCard
import com.afterglowtv.app.ui.components.ContinueWatchingRow
import com.afterglowtv.app.ui.components.rememberCrossfadeImageModel
import com.afterglowtv.app.ui.components.shell.AfterglowBrandStrip
import com.afterglowtv.app.ui.components.shell.AppNavigationChrome
import com.afterglowtv.app.ui.components.shell.AppHeroHeader
import com.afterglowtv.app.ui.components.shell.AppScreenScaffold
import com.afterglowtv.app.ui.components.shell.StatusPill
import com.afterglowtv.app.ui.design.AppColors
import com.afterglowtv.app.ui.time.LocalAppTimeFormat
import com.afterglowtv.app.ui.time.createDateTimeFormat
import com.afterglowtv.app.ui.design.AppColors.Brand as Primary
import com.afterglowtv.app.ui.design.AppColors.Focus as FocusBorder
import com.afterglowtv.app.ui.design.AppColors.SurfaceElevated as SurfaceElevated
import com.afterglowtv.app.ui.design.AppColors.SurfaceEmphasis as SurfaceHighlight
import com.afterglowtv.app.ui.design.AppColors.TextPrimary as OnBackground
import com.afterglowtv.app.ui.design.AppColors.TextPrimary as TextPrimary
import com.afterglowtv.app.ui.design.AppColors.TextTertiary as OnSurfaceDim
import com.afterglowtv.app.ui.design.AppColors.TextTertiary as TextTertiary
import com.afterglowtv.domain.model.Channel
import com.afterglowtv.domain.model.PlaybackHistory
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.BorderStroke
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.afterglowtv.app.ui.interaction.TvClickableSurface
import com.afterglowtv.app.ui.interaction.TvButton
import com.afterglowtv.app.ui.interaction.TvIconButton

@Composable
fun DashboardScreen(
    onNavigate: (String) -> Unit,
    onAddProvider: () -> Unit,
    onRecentChannelClick: (Channel, Long?) -> Unit,
    onFavoriteChannelClick: (Channel, Long?) -> Unit,
    onPlaybackHistoryClick: (PlaybackHistory) -> Unit,
    currentRoute: String,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val startupDestination by viewModel.startupDestination.collectAsStateWithLifecycle()
    val developerModeEnabled by viewModel.developerModeEnabled.collectAsStateWithLifecycle()
    val remoteDpadChannelZapping by viewModel.remoteDpadChannelZapping.collectAsStateWithLifecycle()
    val remoteDpadInvertChannelZapping by viewModel.remoteDpadInvertChannelZapping.collectAsStateWithLifecycle()
    val remoteShowInfoOnZap by viewModel.remoteShowInfoOnZap.collectAsStateWithLifecycle()
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
                    providerName = provider?.name?.takeIf { uiState.showProviderChrome },
                    policy = policy,
                    developerModeEnabled = developerModeEnabled,
                    startupDestination = startupDestination,
                    remoteDpadChannelZapping = remoteDpadChannelZapping,
                    remoteDpadInvertChannelZapping = remoteDpadInvertChannelZapping,
                    remoteShowInfoOnZap = remoteShowInfoOnZap,
                    onNavigate = onNavigate,
                    onAddProvider = onAddProvider,
                    onStartupDestinationChange = viewModel::setStartupDestination,
                    onRemoteDpadChannelZappingChange = viewModel::setRemoteDpadChannelZapping,
                    onRemoteDpadInvertChannelZappingChange = viewModel::setRemoteDpadInvertChannelZapping,
                    onRemoteShowInfoOnZapChange = viewModel::setRemoteShowInfoOnZap,
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
    providerName: String?,
    policy: StorePolicySnapshot,
    developerModeEnabled: Boolean,
    startupDestination: StartupDestination,
    remoteDpadChannelZapping: Boolean,
    remoteDpadInvertChannelZapping: Boolean,
    remoteShowInfoOnZap: Boolean,
    onNavigate: (String) -> Unit,
    onAddProvider: () -> Unit,
    onStartupDestinationChange: (StartupDestination) -> Unit,
    onRemoteDpadChannelZappingChange: (Boolean) -> Unit,
    onRemoteDpadInvertChannelZappingChange: (Boolean) -> Unit,
    onRemoteShowInfoOnZapChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val watchCards = buildList {
        add(HomeHubCardModel("Live TV", "Channels", Routes.LIVE_TV, Icons.Default.PlayArrow, Color(0xFF60A5FA)))
        add(HomeHubCardModel("TV Guide", "EPG grid", Routes.EPG, Icons.Default.Info, Color(0xFF5EEAD4)))
        if (!policy.guideOnlyReviewSurface) {
            add(HomeHubCardModel("VOD", "On demand", Routes.VOD_CONTAINER, Icons.Default.Star, Color(0xFFFFA64D)))
            add(HomeHubCardModel("Library", "Personal media", Routes.LOCAL_MEDIA, Icons.Default.Menu, Color(0xFFB4F06B)))
            add(HomeHubCardModel("Search", "Find anything", Routes.SEARCH, Icons.Default.Search, Color(0xFFFF77FF)))
        }
    }
    val appearanceCards = listOf(
        HomeHubCardModel("Themes", "Colors", Routes.THEMES, Icons.Default.Star, Color(0xFFFF77FF)),
        HomeHubCardModel("Glow", "Focus light", Routes.GLOW_SETTINGS, Icons.Default.Info, Color(0xFF5EEAD4)),
        HomeHubCardModel("Customize", "Shapes", Routes.STYLE_CUSTOMIZER, Icons.Default.Settings, Color(0xFFFF7A38))
    )
    val startupOptions = StartupDestination.visibleEntries(developerModeEnabled)
        .filter { it == StartupDestination.HOME || it == StartupDestination.LIVE_TV || it == StartupDestination.IPTV_GUIDE }
    val visibleStartupDestination = StartupDestination.visibleOrDefault(startupDestination, developerModeEnabled)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AfterglowBrandStrip(
            wordmark = "Jump back in",
            tagline = providerName?.let { "Connected to $it." } ?: "Set up sources or jump straight into the guide.",
            modifier = Modifier.fillMaxWidth(),
            logoSize = 40.dp
        )
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HomeWatchWindow(
                cards = watchCards,
                onNavigate = onNavigate,
                modifier = Modifier.weight(1.18f)
            )
            HomeSourcesWindow(
                providerName = providerName,
                onAddProvider = onAddProvider,
                onOpenSettings = { onNavigate(Routes.SETTINGS) },
                modifier = Modifier.weight(0.82f)
            )
        }
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HomeAppearanceWindow(
                cards = appearanceCards,
                onNavigate = onNavigate,
                modifier = Modifier.weight(1f)
            )
            HomeQuickWindow(
                selectedDestination = visibleStartupDestination,
                startupOptions = startupOptions,
                remoteDpadChannelZapping = remoteDpadChannelZapping,
                remoteDpadInvertChannelZapping = remoteDpadInvertChannelZapping,
                remoteShowInfoOnZap = remoteShowInfoOnZap,
                onDestinationSelected = onStartupDestinationChange,
                onRemoteDpadChannelZappingChange = onRemoteDpadChannelZappingChange,
                onRemoteDpadInvertChannelZappingChange = onRemoteDpadInvertChannelZappingChange,
                onRemoteShowInfoOnZapChange = onRemoteShowInfoOnZapChange,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private data class HomeHubCardModel(
    val title: String,
    val subtitle: String,
    val route: String?,
    val icon: ImageVector,
    val accent: Color
)

@Composable
private fun HomeWindow(
    title: String,
    subtitle: String,
    accent: Color,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        shape = RoundedCornerShape(20.dp),
        colors = SurfaceDefaults.colors(containerColor = SurfaceElevated.copy(alpha = 0.86f)),
        border = Border(BorderStroke(1.dp, accent.copy(alpha = 0.24f)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(
                            accent.copy(alpha = 0.10f),
                            SurfaceElevated.copy(alpha = 0.18f),
                            Color.Transparent
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceDim,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            content()
        }
    }
}

@Composable
private fun HomeWatchWindow(
    cards: List<HomeHubCardModel>,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    HomeWindow(
        title = "Watch",
        subtitle = "Fast routes into playback.",
        accent = Color(0xFF60A5FA),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            cards.take(2).forEach { card ->
                HomeLargeAction(
                    model = card,
                    modifier = Modifier.weight(1f),
                    onClick = { card.route?.let(onNavigate) }
                )
            }
        }
        if (cards.size > 2) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                cards.drop(2).take(3).forEach { card ->
                    HomeStripAction(
                        model = card,
                        modifier = Modifier.weight(1f),
                        onClick = { card.route?.let(onNavigate) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeSourcesWindow(
    providerName: String?,
    onAddProvider: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    HomeWindow(
        title = "Sources",
        subtitle = providerName ?: "Playlists and setup.",
        accent = Color(0xFFFFD166),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.height(72.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HomeSmallTextAction(
                title = if (providerName == null) "Add Playlist" else "Sources",
                subtitle = if (providerName == null) "URL or file" else "Manage playlists",
                icon = Icons.Default.Settings,
                accent = Color(0xFFFFD166),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                onClick = onAddProvider
            )
            HomeSmallTextAction(
                title = "Settings",
                subtitle = "Providers",
                icon = Icons.Default.Menu,
                accent = Color(0xFFB4F06B),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                onClick = onOpenSettings
            )
        }
        HomeSourceStatusText(
            title = providerName ?: "No playlist selected",
            subtitle = if (providerName == null) "Add a public or personal playlist to fill the guide." else "Ready for guide and playback.",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun HomeSourceStatusText(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = OnSurfaceDim,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun HomeAppearanceWindow(
    cards: List<HomeHubCardModel>,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    HomeWindow(
        title = "Appearance",
        subtitle = "Visual shortcuts.",
        accent = Color(0xFFFF77FF),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HomeThemeFeatureAction(
                model = cards[0],
                modifier = Modifier.weight(1.08f),
                onClick = { cards[0].route?.let(onNavigate) }
            )
            Column(
                modifier = Modifier.weight(0.92f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                cards.drop(1).forEach { card ->
                    HomeSmallTextAction(
                        title = card.title,
                        subtitle = card.subtitle,
                        icon = card.icon,
                        accent = card.accent,
                        modifier = Modifier.weight(1f),
                        onClick = { card.route?.let(onNavigate) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeQuickWindow(
    selectedDestination: StartupDestination,
    startupOptions: List<StartupDestination>,
    remoteDpadChannelZapping: Boolean,
    remoteDpadInvertChannelZapping: Boolean,
    remoteShowInfoOnZap: Boolean,
    onDestinationSelected: (StartupDestination) -> Unit,
    onRemoteDpadChannelZappingChange: (Boolean) -> Unit,
    onRemoteDpadInvertChannelZappingChange: (Boolean) -> Unit,
    onRemoteShowInfoOnZapChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = Color(0xFFFF7A38)
    Surface(
        modifier = modifier.fillMaxSize(),
        shape = RoundedCornerShape(20.dp),
        colors = SurfaceDefaults.colors(containerColor = SurfaceElevated.copy(alpha = 0.86f)),
        border = Border(BorderStroke(1.dp, accent.copy(alpha = 0.24f)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(
                            accent.copy(alpha = 0.10f),
                            SurfaceElevated.copy(alpha = 0.18f),
                            Color.Transparent
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Quick",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = TextPrimary,
                    maxLines = 1
                )
                Text(
                    text = "Startup and remote behavior",
                    style = MaterialTheme.typography.labelMedium,
                    color = OnSurfaceDim,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier.weight(0.84f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Start on",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = OnSurfaceDim,
                        maxLines = 1
                    )
                    startupOptions.forEach { destination ->
                        HomeStartupChip(
                            destination = destination,
                            selected = destination == selectedDestination,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { onDestinationSelected(destination) }
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1.16f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Remote",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = OnSurfaceDim,
                        maxLines = 1
                    )
                    HomeMiniToggle(
                        title = "D-pad channel +/-",
                        checked = remoteDpadChannelZapping,
                        onCheckedChange = onRemoteDpadChannelZappingChange
                    )
                    HomeMiniToggle(
                        title = "Reverse channel order",
                        checked = remoteDpadInvertChannelZapping,
                        enabled = remoteDpadChannelZapping,
                        onCheckedChange = onRemoteDpadInvertChannelZappingChange
                    )
                    HomeMiniToggle(
                        title = "Show info while changing",
                        checked = remoteShowInfoOnZap,
                        onCheckedChange = onRemoteShowInfoOnZapChange
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeLargeAction(
    model: HomeHubCardModel,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    TvClickableSurface(
        onClick = onClick,
        modifier = modifier.fillMaxSize(),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(16.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = model.accent.copy(alpha = 0.14f),
            focusedContainerColor = SurfaceHighlight
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = BorderStroke(1.dp, model.accent.copy(alpha = 0.36f)),
                shape = RoundedCornerShape(16.dp)
            ),
            focusedBorder = Border(
                border = BorderStroke(2.dp, FocusBorder),
                shape = RoundedCornerShape(16.dp)
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            IconBadge(icon = model.icon, accent = model.accent, size = 34.dp)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = model.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = model.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceDim,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun HomeStripAction(
    model: HomeHubCardModel,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    HomeSmallTextAction(
        title = model.title,
        subtitle = model.subtitle,
        icon = model.icon,
        accent = model.accent,
        modifier = modifier.height(52.dp),
        onClick = onClick
    )
}

@Composable
private fun HomeThemeFeatureAction(
    model: HomeHubCardModel,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    TvClickableSurface(
        onClick = onClick,
        modifier = modifier.fillMaxSize(),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(16.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = model.accent.copy(alpha = 0.14f),
            focusedContainerColor = SurfaceHighlight
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = BorderStroke(1.dp, model.accent.copy(alpha = 0.34f)),
                shape = RoundedCornerShape(16.dp)
            ),
            focusedBorder = Border(
                border = BorderStroke(2.dp, FocusBorder),
                shape = RoundedCornerShape(16.dp)
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(Primary, Color(0xFF5EEAD4), Color(0xFFFF77FF), Color(0xFFFFD166)).forEach { swatch ->
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(swatch)
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = model.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = model.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceDim,
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
    onClick: () -> Unit
) {
    TvClickableSurface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(14.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = AppColors.Surface.copy(alpha = 0.42f),
            focusedContainerColor = SurfaceHighlight
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = BorderStroke(1.dp, accent.copy(alpha = 0.22f)),
                shape = RoundedCornerShape(14.dp)
            ),
            focusedBorder = Border(
                border = BorderStroke(2.dp, FocusBorder),
                shape = RoundedCornerShape(14.dp)
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconBadge(icon = icon, accent = accent, size = 30.dp)
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurfaceDim,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun HomeStartupChip(
    destination: StartupDestination,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val title = when (destination) {
        StartupDestination.HOME -> "Home"
        StartupDestination.LIVE_TV -> "Live"
        StartupDestination.IPTV_GUIDE -> "Guide"
        else -> stringResource(destination.labelResId)
    }
    val accent = if (selected) Primary else Color(0xFF7DD3FC)
    TvClickableSurface(
        onClick = onClick,
        modifier = modifier.height(29.dp),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = accent.copy(alpha = if (selected) 0.22f else 0.10f),
            focusedContainerColor = SurfaceHighlight
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = BorderStroke(if (selected) 2.dp else 1.dp, accent.copy(alpha = if (selected) 0.65f else 0.24f)),
                shape = RoundedCornerShape(12.dp)
            ),
            focusedBorder = Border(
                border = BorderStroke(2.dp, FocusBorder),
                shape = RoundedCornerShape(12.dp)
            )
        )
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
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
    onCheckedChange: (Boolean) -> Unit
) {
    val accent = if (checked) Primary else Color(0xFF7DD3FC)
    TvClickableSurface(
        onClick = { if (enabled) onCheckedChange(!checked) },
        modifier = modifier
            .fillMaxWidth()
            .height(29.dp),
        enabled = enabled,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(11.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = accent.copy(alpha = if (checked) 0.18f else 0.07f),
            focusedContainerColor = SurfaceHighlight
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = BorderStroke(1.dp, accent.copy(alpha = if (checked) 0.50f else 0.18f)),
                shape = RoundedCornerShape(11.dp)
            ),
            focusedBorder = Border(
                border = BorderStroke(2.dp, FocusBorder),
                shape = RoundedCornerShape(11.dp)
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(accent.copy(alpha = if (enabled) 1f else 0.35f))
            )
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = TextPrimary.copy(alpha = if (enabled) 1f else 0.48f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (checked) "On" else "Off",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
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

@Composable
private fun DashboardHero(
    providerName: String,
    feature: DashboardFeature,
    stats: DashboardStats,
    onOpenLiveTv: () -> Unit,
    onOpenGuide: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSavedLibrary: () -> Unit,
    onFeatureAction: () -> Unit
) {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val isTelevisionDevice = rememberIsTelevisionDevice()
    val heroHeight = when {
        screenWidth < 700.dp -> 176.dp
        !isTelevisionDevice && screenWidth < 1280.dp -> 196.dp
        else -> 220.dp
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        if (!feature.artworkUrl.isNullOrBlank()) {
            AsyncImage(
                model = rememberCrossfadeImageModel(feature.artworkUrl),
                contentDescription = feature.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(heroHeight)
                    .clip(RoundedCornerShape(28.dp))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(heroHeight)
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.88f),
                                Color.Black.copy(alpha = 0.72f),
                                Color.Black.copy(alpha = 0.34f)
                            )
                        )
                    )
            )
        }

        AppHeroHeader(
            eyebrow = providerName,
            title = feature.title.ifBlank { stringResource(R.string.dashboard_title) },
            subtitle = feature.summary.ifBlank { stringResource(R.string.dashboard_subtitle, providerName) },
            modifier = Modifier
                .fillMaxWidth()
                .height(heroHeight),
            footer = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusPill(label = stringResource(R.string.nav_live_tv), containerColor = AppColors.BrandMuted)
                        StatusPill(label = stringResource(R.string.nav_epg), containerColor = AppColors.SurfaceEmphasis)
                        StatusPill(label = stringResource(R.string.favorites_title), containerColor = AppColors.Warning, contentColor = Color.Black)
                    }
                    DashboardStatRow(stats = stats)
                }
            },
            actions = {
                DashboardActionButton(label = stringResource(R.string.nav_live_tv), onClick = onOpenLiveTv)
                DashboardActionButton(label = stringResource(R.string.nav_epg), onClick = onOpenGuide)
                DashboardActionButton(label = stringResource(R.string.dashboard_search_library), onClick = onOpenSearch)
                DashboardActionButton(label = stringResource(R.string.favorites_title), onClick = onOpenSavedLibrary)
                if (feature.actionLabel.isNotBlank()) {
                    DashboardActionButton(
                        label = feature.actionLabel,
                        onClick = onFeatureAction
                    )
                }
            }
        )
    }
}

@Composable
private fun DashboardStatRow(
    stats: DashboardStats
) {
    val statItems = listOf(
        stringResource(R.string.dashboard_stat_live, stats.liveChannelCount),
        stringResource(R.string.dashboard_stat_favorites, stats.favoriteChannelCount),
        stringResource(R.string.dashboard_stat_recent, stats.recentChannelCount),
        stringResource(R.string.dashboard_stat_resume, stats.continueWatchingCount)
    )

    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(statItems, key = { it }) { statLabel ->
            Surface(
                shape = RoundedCornerShape(999.dp),
                colors = SurfaceDefaults.colors(
                    containerColor = AppColors.Surface.copy(alpha = 0.64f)
                )
            ) {
                Text(
                    text = statLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun DashboardShortcutRow(
    title: String,
    subtitle: String,
    shortcuts: List<DashboardLiveShortcut>,
    onShortcutClick: (DashboardLiveShortcut) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceDim
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(shortcuts, key = { "${it.type}:${it.categoryId}:${it.label}" }) { shortcut ->
                DashboardShortcutCard(
                    shortcut = shortcut,
                    onClick = { onShortcutClick(shortcut) }
                )
            }
        }
    }
}

@Composable
private fun DashboardShortcutCard(
    shortcut: DashboardLiveShortcut,
    onClick: () -> Unit
) {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val isTelevisionDevice = rememberIsTelevisionDevice()
    val cardWidth = when {
        screenWidth < 700.dp -> 148.dp
        !isTelevisionDevice && screenWidth < 1280.dp -> 160.dp
        else -> 170.dp
    }
    val accentColor = when (shortcut.type) {
        DashboardShortcutType.FAVORITES -> Color(0xFFFFC857)
        DashboardShortcutType.RECENT -> Color(0xFF4FD1C5)
        DashboardShortcutType.LAST_GROUP -> Color(0xFF60A5FA)
        DashboardShortcutType.CUSTOM_GROUP -> Primary
    }

    TvClickableSurface(
        onClick = onClick,
        modifier = Modifier
            .width(cardWidth)
            .height(76.dp),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(16.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = SurfaceElevated,
            focusedContainerColor = SurfaceHighlight
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.28f)),
                shape = RoundedCornerShape(16.dp)
            ),
            focusedBorder = Border(
                border = BorderStroke(2.dp, FocusBorder),
                shape = RoundedCornerShape(16.dp)
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(accentColor)
                )
                Text(
                    text = shortcut.label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = shortcut.detail,
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceDim,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DashboardActionButton(
    label: String,
    onClick: () -> Unit
) {
    TvButton(
        onClick = onClick,
        colors = ButtonDefaults.colors(
            containerColor = Primary.copy(alpha = 0.18f),
            focusedContainerColor = Primary.copy(alpha = 0.32f),
            contentColor = TextPrimary
        )
    ) {
        Text(text = label)
    }
}

@Composable
private fun DashboardProviderHealthCard(
    providerName: String,
    health: DashboardProviderHealth,
    onOpenDiagnostics: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val appTimeFormat = LocalAppTimeFormat.current
    val dateTimeFormat = remember(appTimeFormat) { appTimeFormat.createDateTimeFormat() }
    val syncLabel = remember(health.lastSyncedAt, dateTimeFormat) {
        if (health.lastSyncedAt <= 0L) {
            context.getString(R.string.dashboard_provider_no_sync)
        } else {
            context.getString(R.string.dashboard_provider_synced_at, dateTimeFormat.format(Date(health.lastSyncedAt)))
        }
    }
    val expiryLabel = remember(health.expirationDate) {
        health.expirationDate?.let {
            val format = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            context.getString(R.string.dashboard_provider_expires_at, format.format(Date(it)))
        } ?: context.getString(R.string.dashboard_provider_no_expiry)
    }
    val statusLabel = when (health.status) {
        com.afterglowtv.domain.model.ProviderStatus.ACTIVE -> stringResource(R.string.settings_status_active)
        com.afterglowtv.domain.model.ProviderStatus.PARTIAL -> stringResource(R.string.settings_status_partial)
        com.afterglowtv.domain.model.ProviderStatus.ERROR -> stringResource(R.string.settings_status_error)
        com.afterglowtv.domain.model.ProviderStatus.EXPIRED -> stringResource(R.string.settings_status_expired)
        com.afterglowtv.domain.model.ProviderStatus.DISABLED -> stringResource(R.string.settings_status_disabled)
        com.afterglowtv.domain.model.ProviderStatus.UNKNOWN -> stringResource(R.string.settings_status_unknown)
    }
    val sourceLabel = when (health.type) {
        com.afterglowtv.domain.model.ProviderType.XTREAM_CODES -> stringResource(R.string.dashboard_provider_xtream)
        com.afterglowtv.domain.model.ProviderType.M3U -> stringResource(R.string.dashboard_provider_m3u)
        com.afterglowtv.domain.model.ProviderType.STALKER_PORTAL -> "Portal/MAG Login"
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 4.dp),
        shape = RoundedCornerShape(22.dp),
        colors = SurfaceDefaults.colors(containerColor = SurfaceHighlight)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = stringResource(R.string.dashboard_provider_health_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Text(
                    text = providerName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = OnSurfaceDim
                )
                Text(
                    text = "$syncLabel | $expiryLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceDim
                )
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    DashboardHealthPill(
                        label = statusLabel,
                        value = stringResource(R.string.dashboard_provider_status)
                    )
                }
                item {
                    DashboardHealthPill(
                        label = sourceLabel,
                        value = stringResource(R.string.dashboard_provider_source)
                    )
                }
                item {
                    DashboardHealthPill(
                        label = health.maxConnections.toString(),
                        value = stringResource(R.string.dashboard_provider_connections)
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 0.dp),
            horizontalArrangement = Arrangement.End
        ) {
            DashboardActionButton(
                label = stringResource(R.string.dashboard_warning_review),
                onClick = onOpenDiagnostics
            )
        }
    }
}

@Composable
private fun DashboardHealthPill(
    label: String,
    value: String
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        colors = SurfaceDefaults.colors(containerColor = Color.White.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall,
                color = OnSurfaceDim
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = TextPrimary
            )
        }
    }
}

@Composable
private fun DashboardProviderWarningCard(
    warnings: List<String>,
    onOpenSettings: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = SurfaceDefaults.colors(containerColor = SurfaceElevated)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.dashboard_warning_title),
                style = MaterialTheme.typography.titleMedium,
                color = Primary
            )
            Text(
                text = warnings.take(3).joinToString(" | "),
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceDim
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DashboardActionButton(
                    label = stringResource(R.string.dashboard_warning_review),
                    onClick = onOpenSettings
                )
            }
        }
    }
}

@Composable
private fun DashboardUpdateCard(
    notice: DashboardUpdateNotice,
    onOpenSettings: () -> Unit,
    onInstallUpdate: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = SurfaceDefaults.colors(containerColor = Primary.copy(alpha = 0.16f)),
        border = Border(BorderStroke(1.dp, Primary.copy(alpha = 0.45f)))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.dashboard_update_title, notice.latestVersionName),
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Text(
                text = stringResource(
                    if (notice.installReady) {
                        R.string.dashboard_update_install_ready
                    } else {
                        R.string.dashboard_update_available
                    }
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceDim
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DashboardActionButton(
                    label = stringResource(
                        if (notice.installReady) {
                            R.string.dashboard_update_open_installer
                        } else {
                            R.string.dashboard_update_open_settings
                        }
                    ),
                    onClick = {
                        if (notice.installReady) {
                            onInstallUpdate()
                        } else {
                            onOpenSettings()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun EmptyDashboard(
    onAddProvider: () -> Unit,
    onOpenSettings: () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val isTelevisionDevice = rememberIsTelevisionDevice()
        val contentModifier = if (maxWidth < 900.dp) {
            Modifier.fillMaxWidth(0.9f)
        } else if (!isTelevisionDevice && maxWidth < 1280.dp) {
            Modifier.fillMaxWidth(0.76f)
        } else {
            Modifier.width(720.dp)
        }

        Surface(
            shape = RoundedCornerShape(28.dp),
            colors = SurfaceDefaults.colors(
                containerColor = SurfaceHighlight
            )
        ) {
            Column(
                modifier = contentModifier
                    .padding(horizontal = 32.dp, vertical = 28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = stringResource(R.string.dashboard_empty_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = OnBackground
                )
                Text(
                    text = stringResource(R.string.dashboard_empty_body),
                    style = MaterialTheme.typography.titleMedium,
                    color = OnSurfaceDim
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TvButton(onClick = onAddProvider) {
                        Text(stringResource(R.string.settings_add_provider))
                    }
                    TvButton(
                        onClick = onOpenSettings,
                        colors = ButtonDefaults.colors(
                            containerColor = SurfaceElevated,
                            focusedContainerColor = Primary.copy(alpha = 0.24f),
                            contentColor = TextPrimary
                        )
                    ) {
                        Text(stringResource(R.string.nav_settings))
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberDashboardSections(
    uiState: DashboardUiState
): List<DashboardHomeSection> {
    return remember(
        uiState.feature.actionType,
        uiState.liveShortcuts,
        uiState.favoriteChannels,
        uiState.recentChannels,
        uiState.continueWatching
    ) {
        val preferred = listOf(
            DashboardHomeSection.FAVORITE_CHANNELS,
            DashboardHomeSection.RECENT_CHANNELS,
            DashboardHomeSection.LIVE_SHORTCUTS,
            DashboardHomeSection.CONTINUE_WATCHING
        )

        preferred.filter { section ->
            when (section) {
                DashboardHomeSection.LIVE_SHORTCUTS -> uiState.liveShortcuts.isNotEmpty()
                DashboardHomeSection.FAVORITE_CHANNELS -> uiState.favoriteChannels.isNotEmpty()
                DashboardHomeSection.RECENT_CHANNELS -> uiState.recentChannels.isNotEmpty()
                DashboardHomeSection.CONTINUE_WATCHING -> uiState.continueWatching.isNotEmpty()
            }
        }
    }
}

private enum class DashboardHomeSection {
    LIVE_SHORTCUTS,
    FAVORITE_CHANNELS,
    RECENT_CHANNELS,
    CONTINUE_WATCHING
}

@Composable
private fun FavoriteChannelsRow(
    title: String,
    channels: List<Channel>,
    onSeeAll: () -> Unit,
    onChannelClick: (Channel) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            TvClickableSurface(
                onClick = onSeeAll,
                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(999.dp)),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = Primary.copy(alpha = 0.12f),
                    focusedContainerColor = Primary.copy(alpha = 0.22f),
                    contentColor = TextTertiary
                )
            ) {
                Text(
                    text = stringResource(R.string.category_see_all),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(channels, key = { it.id }) { channel ->
                FavoriteChannelLogoCard(
                    channel = channel,
                    onClick = { onChannelClick(channel) }
                )
            }
        }
    }
}

@Composable
private fun FavoriteChannelLogoCard(
    channel: Channel,
    onClick: () -> Unit
) {
    TvClickableSurface(
        onClick = onClick,
        modifier = Modifier.width(86.dp),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(18.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = SurfaceElevated,
            focusedContainerColor = SurfaceHighlight
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(2.dp, FocusBorder),
                shape = RoundedCornerShape(18.dp)
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(999.dp))
            ) {
                ChannelLogoBadge(
                    channelName = channel.name,
                    logoUrl = channel.logoUrl,
                    shape = RoundedCornerShape(999.dp),
                    backgroundColor = AppColors.SurfaceEmphasis,
                    contentPadding = PaddingValues(8.dp),
                    textStyle = MaterialTheme.typography.labelLarge,
                    textColor = TextPrimary,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Text(
                text = channel.name,
                style = MaterialTheme.typography.bodySmall,
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
