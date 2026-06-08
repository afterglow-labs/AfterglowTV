package com.afterglowtv.app.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.afterglowtv.app.ui.design.AppColors
import com.afterglowtv.app.ui.design.AppPalette
import com.afterglowtv.app.ui.design.FocusSpec
import com.afterglowtv.app.ui.design.GlowSpec
import com.afterglowtv.app.ui.design.afterglow
import com.afterglowtv.app.ui.interaction.TvClickableSurface
import com.afterglowtv.data.preferences.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThemePickerViewModel @Inject constructor(
    private val preferences: PreferencesRepository,
) : ViewModel() {
    val activePaletteId = preferences.themePalette.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        "afterglow_sunset",
    )
    val backgroundGradientsEnabled = preferences.backgroundGradientsEnabled.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        true,
    )

    fun select(palette: AppPalette) {
        viewModelScope.launch {
            preferences.setThemePalette(palette.id)
            AppColors.applyPalette(palette)
        }
    }

    fun setBackgroundGradientsEnabled(enabled: Boolean) {
        AppColors.applyBackgroundGradientsEnabled(enabled)
        viewModelScope.launch { preferences.setBackgroundGradientsEnabled(enabled) }
    }
}

@Composable
fun ThemePickerScreen(
    onBack: () -> Unit,
    viewModel: ThemePickerViewModel = hiltViewModel(),
) {
    val activeId by viewModel.activePaletteId.collectAsState()
    val backgroundGradientsEnabled by viewModel.backgroundGradientsEnabled.collectAsState()
    val palettes = AppPalette.ALL
    val initialIndex = palettes.indexOfFirst { it.id == activeId }.coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)

    LaunchedEffect(activeId, palettes) {
        val index = palettes.indexOfFirst { it.id == activeId }
        if (index >= 0) {
            listState.animateScrollToItem(index)
        }
    }

    SettingsToolDialog(
        title = "Themes",
        subtitle = "Pick a vibe. Everything reflows instantly — no restart, no fuss.",
        onDismiss = onBack,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
        ) {
            GradientPreferenceCard(
                enabled = backgroundGradientsEnabled,
                onEnabledChange = viewModel::setBackgroundGradientsEnabled,
                modifier = Modifier.padding(bottom = 14.dp),
            )

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 32.dp),
            ) {
                items(palettes, key = { it.id }) { palette ->
                    PaletteCard(
                        palette = palette,
                        isSelected = palette.id == activeId,
                        onSelect = { viewModel.select(palette) },
                    )
                }
            }
        }
    }
}

@Composable
private fun GradientPreferenceCard(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(10.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val glow = when {
        isFocused -> listOf(
            GlowSpec(Color.White, 16.dp, 0.58f),
            GlowSpec(AppColors.TiviAccentLight, 26.dp, 0.28f),
        )
        enabled -> listOf(
            GlowSpec(AppColors.TiviAccent, 14.dp, 0.45f),
        )
        else -> emptyList()
    }

    TvClickableSurface(
        onClick = { onEnabledChange(!enabled) },
        interactionSource = interactionSource,
        modifier = modifier
            .fillMaxWidth()
            .then(if (glow.isNotEmpty()) Modifier.afterglow(glow, shape) else Modifier),
        shape = ClickableSurfaceDefaults.shape(shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = AppColors.SurfaceElevated.copy(alpha = 0.88f),
            contentColor = AppColors.TextPrimary,
            focusedContainerColor = AppColors.SurfaceAccent.copy(alpha = 0.96f),
            focusedContentColor = AppColors.TextPrimary,
            pressedContainerColor = AppColors.SurfaceAccent,
            pressedContentColor = AppColors.TextPrimary,
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = BorderStroke(1.dp, AppColors.Outline.copy(alpha = 0.70f)),
                shape = shape,
            ),
            focusedBorder = Border(
                border = BorderStroke(FocusSpec.BorderWidth + 1.dp, Color.White),
                shape = shape,
            ),
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.012f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(width = 84.dp, height = 48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                AppColors.TiviSurfaceDeep,
                                AppColors.TiviSurfaceCool,
                                AppColors.TiviAccent,
                                AppColors.EpgNowLine,
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(260f, 130f),
                        ),
                    )
                    .border(1.dp, AppColors.Divider, RoundedCornerShape(8.dp)),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Background gradients",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = AppColors.TextPrimary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (enabled) {
                        "Blend the selected theme colors across app backgrounds"
                    } else {
                        "Keep app backgrounds solid; theme cards still show gradients"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextSecondary,
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = onEnabledChange,
            )
        }
    }
}

@Composable
private fun PaletteCard(
    palette: AppPalette,
    isSelected: Boolean,
    onSelect: () -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val focusGlow = when {
        isFocused -> listOf(
            GlowSpec(Color.White, 18.dp, 0.70f),
            GlowSpec(palette.accentLight, 30.dp, 0.35f),
        )
        isSelected -> listOf(
            GlowSpec(palette.accent, 16.dp, 0.70f),
            GlowSpec(palette.accent, 32.dp, 0.35f),
        )
        else -> emptyList()
    }

    TvClickableSurface(
        onClick = onSelect,
        interactionSource = interactionSource,
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (focusGlow.isNotEmpty()) {
                    Modifier.afterglow(focusGlow, shape)
                } else {
                    Modifier
                }
            ),
        shape = ClickableSurfaceDefaults.shape(shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = palette.textPrimary,
            focusedContainerColor = Color.Transparent,
            focusedContentColor = palette.textPrimary,
            pressedContainerColor = Color.Transparent,
            pressedContentColor = palette.textPrimary,
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) palette.accent else palette.outline.copy(alpha = 0.85f),
                ),
                shape = shape,
            ),
            focusedBorder = Border(
                border = BorderStroke(FocusSpec.BorderWidth + 1.dp, Color.White),
                shape = shape,
            ),
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.012f),
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.linearGradient(
                        colors = themeSelectionGradient(palette),
                        start = Offset(0f, 0f),
                        end = Offset(1600f, 520f),
                    )
                ),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                themePreviewSwatches(palette).forEachIndexed { index, color ->
                    Swatch(color, size = if (index == 2) 52.dp else 40.dp, isFeatured = index == 2)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = palette.displayName,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = palette.textPrimary,
                    )
                    if (isSelected) {
                        Spacer(Modifier.width(12.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(palette.accent)
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = "ACTIVE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = TextUnit(1f, TextUnitType.Sp),
                                ),
                                color = palette.surfaceDeep,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = palette.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun Swatch(
    color: androidx.compose.ui.graphics.Color,
    size: androidx.compose.ui.unit.Dp,
    isFeatured: Boolean = false,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp))
            .background(color)
            .border(
                width = if (isFeatured) 2.dp else 1.dp,
                color = AppColors.Divider,
                shape = RoundedCornerShape(8.dp),
            ),
    )
}
