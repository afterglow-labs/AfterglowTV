package com.afterglowtv.app.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.afterglowtv.app.ui.components.shell.AppNavigationChrome
import com.afterglowtv.app.ui.components.shell.AppScreenScaffold
import com.afterglowtv.app.ui.design.AppColors
import com.afterglowtv.app.ui.design.AppColors.Brand as Primary
import com.afterglowtv.app.ui.design.AppColors.Focus as FocusBorder
import com.afterglowtv.app.ui.design.AppColors.SurfaceElevated as SurfaceElevated
import com.afterglowtv.app.ui.design.AppColors.SurfaceEmphasis as SurfaceHighlight
import com.afterglowtv.app.ui.design.AppColors.TextPrimary as TextPrimary
import com.afterglowtv.app.ui.design.AppColors.TextSecondary as OnSurfaceDim
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.BorderStroke
import com.afterglowtv.app.ui.interaction.TvClickableSurface

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
            add(HomeHubCardModel("Search", "Provider catalog", Routes.SEARCH, Icons.Default.Search, Color(0xFFFF77FF)))
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
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HomeWatchWindow(
                cards = watchCards,
                onNavigate = onNavigate,
                modifier = Modifier.weight(1f)
            )
            HomeSourcesShortcutWindow(
                onAddProvider = onAddProvider,
                onOpenSettings = { onNavigate(Routes.SETTINGS) },
                modifier = Modifier.weight(1f)
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
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            cards.take(2).forEach { card ->
                HomeWatchAction(
                    model = card,
                    modifier = Modifier.weight(1f),
                    prominent = true,
                    onClick = { card.route?.let(onNavigate) }
                )
            }
        }
        if (cards.size > 2) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                cards.drop(2).take(3).forEach { card ->
                    HomeWatchAction(
                        model = card,
                        modifier = Modifier.weight(1f),
                        prominent = false,
                        onClick = { card.route?.let(onNavigate) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeSourcesShortcutWindow(
    onAddProvider: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    HomeWindow(
        title = "Sources",
        subtitle = "Providers and playlists.",
        accent = Color(0xFFFFD166),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HomeSmallTextAction(
                title = "Add Source",
                subtitle = "Playlists",
                icon = Icons.Default.Settings,
                accent = Color(0xFFFFD166),
                modifier = Modifier.weight(1f),
                onClick = onAddProvider
            )
            HomeSmallTextAction(
                title = "Settings",
                subtitle = "Providers",
                icon = Icons.Default.Menu,
                accent = Color(0xFFB4F06B),
                modifier = Modifier.weight(1f),
                onClick = onOpenSettings
            )
        }
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
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                cards.drop(1).forEach { card ->
                    HomeSmallTextAction(
                        title = card.title,
                        subtitle = card.subtitle,
                        icon = card.icon,
                        accent = card.accent,
                        modifier = Modifier.weight(1f),
                        compact = true,
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
                    text = "Quick Settings",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = TextPrimary,
                    maxLines = 1
                )
            }
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier.weight(0.84f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "App start page",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = OnSurfaceDim,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Remote control",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = OnSurfaceDim,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
private fun HomeWatchAction(
    model: HomeHubCardModel,
    modifier: Modifier = Modifier,
    prominent: Boolean,
    onClick: () -> Unit
) {
    TvClickableSurface(
        onClick = onClick,
        modifier = modifier.fillMaxSize(),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(14.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = model.accent.copy(alpha = 0.14f),
            focusedContainerColor = SurfaceHighlight
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = BorderStroke(1.dp, model.accent.copy(alpha = 0.28f)),
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
                .padding(horizontal = if (prominent) 14.dp else 12.dp, vertical = if (prominent) 10.dp else 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (prominent) 12.dp else 9.dp)
        ) {
            IconBadge(
                icon = model.icon,
                accent = model.accent,
                size = if (prominent) 34.dp else 28.dp
            )
            Text(
                text = model.title,
                style = if (prominent) {
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
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val title = when (destination) {
        StartupDestination.HOME -> "Home"
        StartupDestination.LIVE_TV -> "Live TV"
        StartupDestination.IPTV_GUIDE -> "TV Guide"
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
