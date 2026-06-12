package com.afterglowtv.app.ui.components.shell

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.afterglowtv.app.R
import com.afterglowtv.app.navigation.Routes
import com.afterglowtv.app.store.StorePolicy
import com.afterglowtv.app.store.StorePolicySnapshot
import com.afterglowtv.app.ui.design.AppColors
import com.afterglowtv.app.ui.design.AppMotion
import com.afterglowtv.app.ui.design.FocusSpec
import com.afterglowtv.app.ui.design.LocalAppShapes
import com.afterglowtv.app.ui.design.LocalAppSpacing
import com.afterglowtv.app.ui.design.requestFocusSafely
import com.afterglowtv.app.ui.interaction.mouseClickable
import com.afterglowtv.app.ui.interaction.rememberTvInteractionSounds

enum class AppNavigationChrome {
    Rail,
    TopBar
}

private val TopNavFontFamily = FontFamily(
    Font(R.font.vox_round, FontWeight.Normal)
)

@Composable
fun AppScreenScaffold(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    navigationChrome: AppNavigationChrome = AppNavigationChrome.Rail,
    topBarVisible: Boolean = true,
    compactHeader: Boolean = false,
    showScreenHeader: Boolean = true,
    header: (@Composable ColumnScope.() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(),
    /** When true, the scaffold renders edge-to-edge — no safe-area padding,
     *  no horizontal/vertical chrome insets. Used by the EPG which wants
     *  every pixel for the program grid. Caller's [content] is expected to
     *  handle its own insets if any are needed. */
    fullBleed: Boolean = false,
    /** Auto-hidden top bars should never be visible without owning focus. */
    focusTopBarOnShow: Boolean = fullBleed,
    content: @Composable ColumnScope.() -> Unit
) {
    val spacing = LocalAppSpacing.current
    val safeArea = com.afterglowtv.app.ui.design.LocalSafeArea.current

    Box(modifier = modifier.fillMaxSize()) {
        AfterglowBackdrop(modifier = Modifier.fillMaxSize())

        Box(
            modifier = Modifier
                .fillMaxSize()
                .let { if (fullBleed) it else it.padding(safeArea) }
        ) {
            if (navigationChrome == AppNavigationChrome.Rail) {
                Row(modifier = Modifier.fillMaxSize()) {
                    DestinationRail(
                        currentRoute = currentRoute,
                        onNavigate = onNavigate,
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(spacing.railWidth)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                start = spacing.lg,
                                end = spacing.screenGutter,
                                top = spacing.safeTop,
                                bottom = spacing.safeBottom
                            )
                    ) {
                        if (showScreenHeader) {
                            AppScreenHeader(
                                title = title,
                                subtitle = subtitle,
                                modifier = Modifier.fillMaxWidth(),
                                compact = compactHeader
                            )
                            if (header != null) {
                                Spacer(modifier = Modifier.height(spacing.lg))
                                header()
                            }
                            Spacer(modifier = Modifier.height(spacing.lg))
                        } else if (header != null) {
                            header()
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(contentPadding)
                        ) {
                            content()
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .let {
                            if (fullBleed) it
                            else it.padding(horizontal = 14.dp, vertical = 10.dp)
                        }
                ) {
                    if (topBarVisible) {
                        TopNavigationBar(
                            currentRoute = currentRoute,
                            onNavigate = onNavigate,
                            requestFocusOnShow = focusTopBarOnShow,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                    if (showScreenHeader) {
                        AppScreenHeader(
                            title = title,
                            subtitle = subtitle,
                            modifier = Modifier.fillMaxWidth(),
                            compact = true
                        )
                        if (header != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            header()
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    } else if (header != null) {
                        header()
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(contentPadding)
                    ) {
                        content()
                    }
                }
            }
        }
    }
}

@Composable
fun AppScreenHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    compact: Boolean = false
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (!eyebrow.isNullOrBlank()) {
            Text(
                text = eyebrow,
                style = MaterialTheme.typography.labelMedium,
                color = AppColors.Brand
            )
        }
        Text(
            text = title,
            style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.displaySmall,
            color = AppColors.TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyLarge,
                color = AppColors.TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
private fun TopNavigationBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    requestFocusOnShow: Boolean = false,
    viewModel: AppShellViewModel = hiltViewModel()
) {
    val showAdultTab by viewModel.showAdultTab.collectAsStateWithLifecycle()
    val developerModeEnabled by viewModel.developerModeEnabled.collectAsStateWithLifecycle()
    val policy = StorePolicy.currentFor(developerModeEnabled)
    val items = remember(showAdultTab, policy) { buildDestinationItems(showAdultTab, policy) }
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    var showExitConfirmation by remember { mutableStateOf(false) }

    val focusRequesters = remember { mutableMapOf<String, FocusRequester>() }
    val closeFocusRequester = remember { FocusRequester() }
    val settingsFocusRequester = focusRequesters.getOrPut(Routes.SETTINGS) { FocusRequester() }
    var closeFocusArmed by remember { mutableStateOf(false) }
    var requestCloseFocus by remember { mutableStateOf(false) }

    LaunchedEffect(requestCloseFocus) {
        if (requestCloseFocus) {
            closeFocusRequester.requestFocus()
            requestCloseFocus = false
        }
    }

    LaunchedEffect(requestFocusOnShow, currentRoute, items) {
        if (requestFocusOnShow) {
            activeTopNavigationFocusRequester(
                items = items,
                currentRoute = currentRoute,
                focusRequesters = focusRequesters
            ).requestFocusSafely(
                tag = "AppShell",
                target = "Top navigation item"
            )
        }
    }

    if (showExitConfirmation) {
        ConfirmCloseAppDialog(
            onDismiss = { showExitConfirmation = false },
            onConfirm = {
                showExitConfirmation = false
                activity?.finishAndRemoveTask()
            }
        )
    }
    
    Surface(
        modifier = modifier.focusProperties {
            onEnter = {
                activeTopNavigationFocusRequester(
                    items = items,
                    currentRoute = currentRoute,
                    focusRequesters = focusRequesters
                )
            }
        },
        shape = RoundedCornerShape(28.dp),
        colors = SurfaceDefaults.colors(containerColor = AppColors.Surface.copy(alpha = 0.9f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(118.dp)
                .padding(horizontal = 22.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AfterglowBrandStrip(
                    wordmark = "Afterglow TV",
                    tagline = "",
                    modifier = Modifier.weight(1f),
                    logoSize = 46.dp,
                    showBrandName = false
                )
                TopAppCloseButton(
                    focusRequester = closeFocusRequester,
                    fallbackFocusRequester = settingsFocusRequester,
                    isFocusAllowed = { closeFocusArmed },
                    onRejectedFocus = { closeFocusArmed = false },
                    onFocusExit = { closeFocusArmed = false },
                    onClick = { showExitConfirmation = true }
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
                    .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val requester = focusRequesters.getOrPut(item.route) { FocusRequester() }
                    val isSettingsItem = item.route == Routes.SETTINGS
                    TopNavigationButton(
                        label = stringResource(item.labelRes),
                        icon = item.icon,
                        selected = currentRoute.startsWith(item.route),
                        focusRequester = requester,
                        modifier = Modifier
                            .focusProperties {
                                if (isSettingsItem) {
                                    up = closeFocusRequester
                                    right = closeFocusRequester
                                } else {
                                    up = FocusRequester.Cancel
                                }
                            },
                        onDirectionalKey = if (isSettingsItem) {
                            { key ->
                                if (key == Key.DirectionRight || key == Key.DirectionUp) {
                                    closeFocusArmed = true
                                    requestCloseFocus = true
                                    true
                                } else {
                                    false
                                }
                            }
                        } else {
                            null
                        },
                        onClick = {
                            if (!currentRoute.startsWith(item.route)) {
                                onNavigate(item.route)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ConfirmCloseAppDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Close Afterglow TV?",
                style = MaterialTheme.typography.titleMedium,
                color = AppColors.TextPrimary
            )
        },
        text = {
            Text(
                text = "Playback and browsing will stop.",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TextSecondary
            )
        },
        confirmButton = {
            androidx.compose.material3.Button(
                onClick = onConfirm,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD94A32),
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "Close",
                    color = Color.White
                )
            }
        },
        dismissButton = {
            androidx.compose.material3.Button(
                onClick = onDismiss,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = AppColors.SurfaceEmphasis,
                    contentColor = AppColors.TextPrimary
                )
            ) {
                Text(
                    text = "Cancel",
                    color = AppColors.TextPrimary
                )
            }
        },
        containerColor = AppColors.SurfaceElevated.copy(alpha = 1f),
        titleContentColor = AppColors.TextPrimary,
        textContentColor = AppColors.TextSecondary
    )
}

@Composable
private fun TopAppCloseButton(
    onClick: () -> Unit,
    focusRequester: FocusRequester,
    fallbackFocusRequester: FocusRequester,
    isFocusAllowed: () -> Boolean,
    onRejectedFocus: () -> Unit,
    onFocusExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sounds = rememberTvInteractionSounds()
    Surface(
        onClick = {
            sounds.playSelect()
            onClick()
        },
        modifier = modifier
            .focusRequester(focusRequester)
            .focusProperties {
                left = fallbackFocusRequester
                down = fallbackFocusRequester
                right = FocusRequester.Cancel
                up = FocusRequester.Cancel
            }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionLeft,
                    Key.DirectionDown -> {
                        fallbackFocusRequester.requestFocus()
                        true
                    }
                    Key.DirectionRight,
                    Key.DirectionUp -> true
                    else -> false
                }
            }
            .onFocusChanged { state ->
                if (state.isFocused && !isFocusAllowed()) {
                    onRejectedFocus()
                    fallbackFocusRequester.requestFocus()
                } else if (!state.isFocused) {
                    onFocusExit()
                }
            }
            .mouseClickable(
                focusRequester = focusRequester,
                onClick = {
                    sounds.playSelect()
                    onClick()
                }
        ),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(999.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color(0xFF6F2118),
            focusedContainerColor = Color(0xFFC74724)
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(FocusSpec.BorderWidth, Color(0xFFFF8A2A)),
                shape = RoundedCornerShape(999.dp)
            )
        )
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_afterglow_exit_power),
            contentDescription = "Close app",
            tint = Color.Unspecified,
            modifier = Modifier
                .padding(10.dp)
                .size(20.dp)
        )
    }
}

@Composable
private fun TopNavigationButton(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
    onDirectionalKey: ((Key) -> Boolean)? = null,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val sounds = rememberTvInteractionSounds()
    val scale by animateFloatAsState(
        targetValue = if (isFocused) FocusSpec.FocusedScale else 1f,
        animationSpec = AppMotion.FocusSpec,
        label = "topNavScale"
    )

    Surface(
        onClick = {
            sounds.playSelect()
            onClick()
        },
        modifier = modifier
            .focusRequester(focusRequester)
            .then(
                if (onDirectionalKey != null) {
                    Modifier.onPreviewKeyEvent { event ->
                        event.type == KeyEventType.KeyDown && onDirectionalKey(event.key)
                    }
                } else {
                    Modifier
                }
            )
            .mouseClickable(
                focusRequester = focusRequester,
                onClick = {
                    sounds.playSelect()
                    onClick()
                }
            )
            .zIndex(if (isFocused) 1f else 0f) // Keep focused button on top
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .onFocusChanged {
                if (it.isFocused && !isFocused) {
                    sounds.playNavigate()
                }
                isFocused = it.isFocused
            },
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(14.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) AppColors.BrandMuted else Color.Transparent,
            focusedContainerColor = AppColors.SurfaceEmphasis
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(FocusSpec.BorderWidth, AppColors.Focus),
                shape = RoundedCornerShape(999.dp)
            )
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) AppColors.Brand else AppColors.TextSecondary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = TopNavFontFamily,
                    fontWeight = FontWeight.Normal
                ),
                color = if (selected) AppColors.TextPrimary else AppColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun AppHeroHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    footer: (@Composable ColumnScope.() -> Unit)? = null
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        colors = SurfaceDefaults.colors(containerColor = AppColors.SurfaceElevated)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .then(
                    if (AppColors.backgroundGradientsEnabled) {
                        Modifier.background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    AppColors.Canvas,
                                    AppColors.Surface,
                                    AppColors.SurfaceEmphasis,
                                )
                            )
                        )
                    } else {
                        Modifier.background(AppColors.SurfaceElevated)
                    }
                )
                .padding(32.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                AppScreenHeader(
                    title = title,
                    subtitle = subtitle,
                    eyebrow = eyebrow
                )
                if (actions != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        content = actions
                    )
                }
                if (footer != null) {
                    footer()
                }
            }
        }
    }
}

@Composable
fun AppSectionHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    actionContentColor: Color = AppColors.TextTertiary
) {
    val shapes = LocalAppShapes.current
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = if (onActionClick != null && !actionLabel.isNullOrBlank()) Modifier.weight(1f) else Modifier,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = AppColors.TextPrimary,
                modifier = Modifier.semantics { heading() }
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextTertiary
                )
            }
        }

        if (onActionClick != null && !actionLabel.isNullOrBlank()) {
            val actionFocusRequester = remember { FocusRequester() }
            Surface(
                onClick = onActionClick,
                modifier = Modifier
                    .focusRequester(actionFocusRequester)
                    .mouseClickable(
                        focusRequester = actionFocusRequester,
                        onClick = onActionClick
                    ),
                shape = ClickableSurfaceDefaults.shape(shapes.pill),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = AppColors.Brand.copy(alpha = 0.12f),
                    focusedContainerColor = AppColors.Brand.copy(alpha = 0.22f),
                    contentColor = actionContentColor
                )
            ) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
fun StatusPill(
    label: String,
    modifier: Modifier = Modifier,
    containerColor: Color = AppColors.SurfaceEmphasis,
    contentColor: Color = AppColors.TextPrimary,
    cornerRadius: Dp = 999.dp,
    horizontalPadding: Dp = 10.dp,
    verticalPadding: Dp = 4.dp
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(containerColor)
            .padding(horizontal = horizontalPadding, vertical = verticalPadding)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor
        )
    }
}

@Composable
fun AppMessageState(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
    shape: RoundedCornerShape? = null,
    containerBrush: Brush? = null,
    borderColor: Color? = null,
    titleStyle: TextStyle = MaterialTheme.typography.titleLarge,
    subtitleStyle: TextStyle = MaterialTheme.typography.bodySmall,
    titleColor: Color = AppColors.TextPrimary,
    subtitleColor: Color = AppColors.TextSecondary,
    titleTextAlign: TextAlign = TextAlign.Start,
    subtitleTextAlign: TextAlign = TextAlign.Start
) {
    val resolvedShape = shape ?: LocalAppShapes.current.large
    Surface(
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
        shape = resolvedShape,
        border = Border(
            border = BorderStroke(
                width = if (borderColor != null) 1.dp else 0.dp,
                color = borderColor ?: Color.Transparent
            ),
            shape = resolvedShape
        ),
        colors = SurfaceDefaults.colors(containerColor = AppColors.SurfaceElevated)
    ) {
        Column(
            modifier = Modifier
                .then(if (containerBrush != null) Modifier.background(containerBrush) else Modifier)
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = titleStyle,
                color = titleColor,
                textAlign = titleTextAlign,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = subtitle,
                style = subtitleStyle,
                color = subtitleColor,
                textAlign = subtitleTextAlign,
                modifier = Modifier.fillMaxWidth()
            )
            if (action != null) {
                Spacer(modifier = Modifier.height(8.dp))
                action()
            }
        }
    }
}

@Composable
fun LoadMoreCard(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shapes = LocalAppShapes.current
    val focusRequester = remember { FocusRequester() }
    Surface(
        onClick = onClick,
        modifier = modifier
            .focusRequester(focusRequester)
            .mouseClickable(
                focusRequester = focusRequester,
                onClick = onClick
            ),
        shape = ClickableSurfaceDefaults.shape(shapes.medium),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = AppColors.SurfaceElevated,
            focusedContainerColor = AppColors.SurfaceEmphasis
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(FocusSpec.BorderWidth, AppColors.Focus),
                shape = shapes.medium
            )
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = label,
                tint = AppColors.Brand,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = AppColors.TextPrimary
            )
        }
    }
}

@Composable
fun ContentMetadataStrip(
    values: List<String>,
    modifier: Modifier = Modifier
) {
    val filteredValues = values.filter { it.isNotBlank() }
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        filteredValues.forEachIndexed { index, value ->
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                color = AppColors.TextSecondary
            )
            if (index < filteredValues.lastIndex) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(AppColors.TextTertiary)
                )
            }
        }
    }
}

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
private fun DestinationRail(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AppShellViewModel = hiltViewModel()
) {
    val spacing = LocalAppSpacing.current
    val showAdultTab by viewModel.showAdultTab.collectAsStateWithLifecycle()
    val developerModeEnabled by viewModel.developerModeEnabled.collectAsStateWithLifecycle()
    val policy = StorePolicy.currentFor(developerModeEnabled)
    val items = remember(showAdultTab, policy) { buildDestinationItems(showAdultTab, policy) }
    val focusRequesters = remember { mutableMapOf<String, FocusRequester>() }

    Box(
        modifier = modifier
            .padding(start = spacing.lg, top = spacing.safeTop, bottom = spacing.safeBottom)
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        AppColors.SurfaceElevated,
                        AppColors.Surface
                    )
                )
            )
            .focusProperties {
                onEnter = {
                    val activeItem = findActiveDestinationItem(items, currentRoute)
                    focusRequesters[activeItem?.route] ?: FocusRequester.Default
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleMedium,
                color = AppColors.TextPrimary
            )
            Text(
                text = stringResource(R.string.label_tv),
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.TextTertiary
            )
            Spacer(modifier = Modifier.height(10.dp))
            items.forEach { item ->
                val requester = focusRequesters.getOrPut(item.route) { FocusRequester() }
                RailButton(
                    label = stringResource(item.labelRes),
                    icon = item.icon,
                    selected = currentRoute.startsWith(item.route),
                    modifier = Modifier.focusRequester(requester),
                    onClick = {
                        if (!currentRoute.startsWith(item.route)) {
                            onNavigate(item.route)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun RailButton(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) FocusSpec.FocusedScale else 1f,
        animationSpec = AppMotion.FocusSpec,
        label = "railButtonScale"
    )

    Surface(
        onClick = onClick,
        modifier = modifier
            .focusRequester(focusRequester)
            .mouseClickable(
                focusRequester = focusRequester,
                onClick = onClick
            )
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .onFocusChanged { isFocused = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(18.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) AppColors.BrandMuted else Color.Transparent,
            focusedContainerColor = AppColors.SurfaceEmphasis
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(FocusSpec.BorderWidth, AppColors.Focus),
                shape = RoundedCornerShape(18.dp)
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) AppColors.Brand else AppColors.TextSecondary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = if (selected) AppColors.TextPrimary else AppColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private data class DestinationItem(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector
)

private fun findActiveDestinationItem(
    items: List<DestinationItem>,
    currentRoute: String
): DestinationItem? =
    items
        .filter { currentRoute.startsWith(it.route) }
        .maxByOrNull { it.route.length }
        ?: items.firstOrNull { it.route == currentRoute }

private fun activeTopNavigationFocusRequester(
    items: List<DestinationItem>,
    currentRoute: String,
    focusRequesters: MutableMap<String, FocusRequester>
): FocusRequester {
    val activeItem = findActiveDestinationItem(items, currentRoute)
    return activeItem?.let { focusRequesters.getOrPut(it.route) { FocusRequester() } }
        ?: FocusRequester.Default
}

private fun buildDestinationItems(
    showAdultTab: Boolean = false,
    policy: StorePolicySnapshot = StorePolicy.current
): List<DestinationItem> = buildList {
    if (policy.guideOnlyReviewSurface) {
        add(DestinationItem(Routes.HOME, R.string.nav_home, Icons.Default.Home))
        add(DestinationItem(Routes.LIVE_TV, R.string.nav_live_tv, Icons.Default.PlayArrow))
        add(DestinationItem(Routes.EPG, R.string.nav_iptv_guide, Icons.Default.Info))
        add(DestinationItem(Routes.SEARCH, R.string.search_title, Icons.Default.Search))
        add(DestinationItem(Routes.SETTINGS, R.string.nav_settings, Icons.Default.Settings))
        return@buildList
    }
    add(DestinationItem(Routes.HOME, R.string.nav_home, Icons.Default.Home))
    add(DestinationItem(Routes.LIVE_TV, R.string.nav_live_tv, Icons.Default.PlayArrow))
    add(DestinationItem(Routes.EPG, R.string.nav_iptv_guide, Icons.Default.Info))
    add(DestinationItem(Routes.VOD_CONTAINER, R.string.nav_vod_container, Icons.Default.Star))
    if (showAdultTab) {
        add(DestinationItem(Routes.ADULT, R.string.nav_adult, Icons.Default.Info))
    }
    add(DestinationItem(Routes.LOCAL_MEDIA, R.string.nav_personal_guide, Icons.Default.Menu))
    add(DestinationItem(Routes.SEARCH, R.string.search_title, Icons.Default.Search))
    add(DestinationItem(Routes.SETTINGS, R.string.nav_settings, Icons.Default.Settings))
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
