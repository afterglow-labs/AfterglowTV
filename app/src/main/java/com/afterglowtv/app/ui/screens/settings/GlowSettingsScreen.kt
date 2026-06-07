package com.afterglowtv.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.focusable
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afterglowtv.app.ui.design.AppColors
import com.afterglowtv.app.ui.design.GlowSerialization
import com.afterglowtv.app.ui.design.GlowSpec
import com.afterglowtv.app.ui.design.Glows
import com.afterglowtv.app.ui.design.afterglow
import com.afterglowtv.data.preferences.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Persists Glow customizations on every slider tick / swatch tap. */
@HiltViewModel
class GlowSettingsViewModel @Inject constructor(
    private val preferences: PreferencesRepository,
) : ViewModel() {
    fun saveIntensity(value: Float) {
        viewModelScope.launch { preferences.setGlowIntensity(value) }
    }
    fun saveFocus(specs: List<GlowSpec>) {
        viewModelScope.launch { preferences.setGlowFocusSpecs(GlowSerialization.serialize(specs)) }
    }
    fun saveLive(specs: List<GlowSpec>) {
        viewModelScope.launch { preferences.setGlowLiveSpecs(GlowSerialization.serialize(specs)) }
    }
    fun saveAmbient(specs: List<GlowSpec>) {
        viewModelScope.launch { preferences.setGlowAmbientSpecs(GlowSerialization.serialize(specs)) }
    }
}

/**
 * Glow customization. Live preview tile reacts to every slider tick — same
 * pattern as the YouTube tweak. Per-role: color (preset chips), radius
 * (sliders), opacity (sliders). Plus a master intensity multiplier that
 * scales everything together (set to 0 to disable all glows). All changes
 * persist via [GlowSettingsViewModel] -> [PreferencesRepository].
 */
@Composable
fun GlowSettingsScreen(
    onBack: () -> Unit = {},
    viewModel: GlowSettingsViewModel = hiltViewModel(),
) {
    val backgroundGradientsEnabled = AppColors.backgroundGradientsEnabled

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (backgroundGradientsEnabled) {
                        Modifier.background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                listOf(
                                    AppColors.TiviSurfaceDeep,
                                    AppColors.TiviSurfaceBase,
                                    AppColors.TiviSurfaceCool,
                                ),
                            )
                        )
                    } else {
                        Modifier.background(AppColors.TiviSurfaceDeep)
                    }
                )
        )
        if (backgroundGradientsEnabled) {
            Box(
                modifier = Modifier.fillMaxSize().background(
                    androidx.compose.ui.graphics.Brush.radialGradient(
                        colors = listOf(
                            AppColors.TiviAccent.copy(alpha = AppColors.palette.glowAlpha(0.24f)),
                            AppColors.TiviAccent.copy(alpha = 0f),
                        ),
                        center = androidx.compose.ui.geometry.Offset(2400f, -200f),
                        radius = 1400f,
                    )
                )
            )
            Box(
                modifier = Modifier.fillMaxSize().background(
                    androidx.compose.ui.graphics.Brush.radialGradient(
                        colors = listOf(
                            AppColors.EpgNowLine.copy(alpha = AppColors.palette.glowAlpha(0.16f)),
                            AppColors.EpgNowLine.copy(alpha = 0f),
                        ),
                        center = androidx.compose.ui.geometry.Offset(300f, 1900f),
                        radius = 1100f,
                    )
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            GlowHeader()

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                item { MasterIntensityCard(onPersist = viewModel::saveIntensity) }

                item {
                    GlowRoleCard(
                        label = "Focus halo",
                        description = "The glow on focused rows, cards, and pills.",
                        specs = Glows.focus,
                        onChange = {
                            Glows.overrideFocus(it)
                            viewModel.saveFocus(it)
                        },
                    )
                }
                item {
                    GlowRoleCard(
                        label = "Live & now-line",
                        description = "Pulse around the LIVE pill, recording indicator, and EPG now-line.",
                        specs = Glows.live,
                        onChange = {
                            Glows.overrideLive(it)
                            viewModel.saveLive(it)
                        },
                    )
                }
                item {
                    GlowRoleCard(
                        label = "Ambient",
                        description = "Subtle halo on cards, posters, and hero blocks.",
                        specs = Glows.ambient,
                        onChange = {
                            Glows.overrideAmbient(it)
                            viewModel.saveAmbient(it)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun GlowHeader() {
    Row(
        modifier = Modifier.padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(
                id = com.afterglowtv.app.R.drawable.afterglow_logo
            ),
            contentDescription = "AfterglowTV",
            modifier = Modifier
                .size(56.dp)
                .afterglow(
                    specs = listOf(
                        GlowSpec(AppColors.TiviAccent, 16.dp, 0.55f),
                        GlowSpec(AppColors.EpgNowLine, 28.dp, 0.30f),
                    ),
                    shape = RoundedCornerShape(14.dp),
                )
                .clip(RoundedCornerShape(14.dp)),
        )
        Column {
            Text(
                text = "Glow",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = androidx.compose.ui.unit.TextUnit(36f, androidx.compose.ui.unit.TextUnitType.Sp),
                ),
                color = AppColors.TextPrimary,
            )
            Text(
                text = "Color · radius · opacity · intensity. Per role. Stacked layers.",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.TiviAccentLight,
            )
        }
    }
}

@Composable
private fun MasterIntensityCard(onPersist: (Float) -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(AppColors.TiviSurfaceBase)
            .border(1.dp, AppColors.TiviSurfaceCool, RoundedCornerShape(4.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Master intensity",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = AppColors.TextPrimary,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "%.2fx".format(Glows.intensity),
                style = MaterialTheme.typography.titleMedium,
                color = AppColors.TiviAccentLight,
            )
        }
        Text(
            text = "Multiplies every glow's opacity. 0 disables glow entirely; 2 doubles every halo.",
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextTertiary,
        )
        DpadSlider(
            value = Glows.intensity,
            onValueChange = {
                Glows.applyIntensity(it)
                onPersist(it)
            },
            valueRange = 0f..2f,
            step = 0.1f,
            colors = sliderColors(),
        )
    }
}

@Composable
private fun GlowRoleCard(
    label: String,
    description: String,
    specs: List<GlowSpec>,
    onChange: (List<GlowSpec>) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(AppColors.TiviSurfaceBase)
            .border(1.dp, AppColors.TiviSurfaceCool, RoundedCornerShape(4.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = AppColors.TextPrimary,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextTertiary,
                )
            }
            GlowPreviewTile(specs)
        }

        specs.forEachIndexed { index, spec ->
            GlowLayerControls(
                layerIndex = index + 1,
                spec = spec,
                onChange = { updated ->
                    onChange(specs.toMutableList().also { it[index] = updated })
                },
            )
        }

        if (specs.size < 3) {
            Text(
                text = "+ Add layer",
                modifier = Modifier
                    .clip(RoundedCornerShape(2.dp))
                    .background(AppColors.TiviAccentMuted)
                    .clickable {
                        val next = specs + GlowSpec(AppColors.TiviAccent, 16.dp, 0.35f)
                        onChange(next)
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium,
                color = AppColors.TiviAccentLight,
            )
        }
    }
}

@Composable
private fun GlowPreviewTile(specs: List<GlowSpec>) {
    val shape = RoundedCornerShape(4.dp)
    Box(
        modifier = Modifier
            .size(96.dp, 56.dp)
            .afterglow(specs, shape)
            .clip(shape)
            .background(AppColors.TiviSurfaceCool)
            .border(1.dp, AppColors.TiviAccent.copy(alpha = 0.5f), shape),
    )
}

@Composable
private fun GlowLayerControls(
    layerIndex: Int,
    spec: GlowSpec,
    onChange: (GlowSpec) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(2.dp))
            .background(AppColors.TiviSurfaceCool.copy(alpha = 0.5f))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Layer $layerIndex",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = AppColors.TiviAccentLight,
                modifier = Modifier.weight(1f),
            )
            ColorSwatches(
                selected = spec.color,
                onPick = { onChange(spec.copy(color = it)) },
            )
        }

        // Radius slider
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Radius",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextSecondary,
                modifier = Modifier.width(72.dp),
            )
            DpadSlider(
                value = spec.radius.value,
                onValueChange = { onChange(spec.copy(radius = it.dp)) },
                valueRange = 0f..48f,
                step = 2f,
                colors = sliderColors(),
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "%.0f dp".format(spec.radius.value),
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TiviAccentLight,
                modifier = Modifier.width(56.dp),
            )
        }

        // Opacity slider
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Opacity",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextSecondary,
                modifier = Modifier.width(72.dp),
            )
            DpadSlider(
                value = spec.opacity,
                onValueChange = { onChange(spec.copy(opacity = it)) },
                valueRange = 0f..1f,
                step = 0.05f,
                colors = sliderColors(),
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "%.0f%%".format(spec.opacity * 100f),
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TiviAccentLight,
                modifier = Modifier.width(56.dp),
            )
        }
    }
}

@Composable
private fun ColorSwatches(selected: Color, onPick: (Color) -> Unit) {
    val palette = listOf(
        AppColors.TiviAccent,
        AppColors.TiviAccentLight,
        AppColors.EpgNowLine,
        AppColors.Live,
        AppColors.Warning,
        AppColors.Info,
        Color(0xFFFFFFFF),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        palette.forEach { c ->
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(c)
                    .border(
                        width = if (c == selected) 2.dp else 0.5.dp,
                        color = if (c == selected) AppColors.TextPrimary else AppColors.TextTertiary,
                        shape = CircleShape,
                    )
                    .clickable { onPick(c) },
            )
        }
    }
}

@Composable
private fun sliderColors() = SliderDefaults.colors(
    thumbColor = AppColors.TiviAccent,
    activeTrackColor = AppColors.TiviAccent,
    inactiveTrackColor = AppColors.TiviSurfaceAccent,
)

/**
 * D-pad-friendly slider for Fire TV / Android TV. Wraps a Material [Slider]
 * in a focusable [Box] that intercepts DPAD_LEFT / DPAD_RIGHT and steps the
 * value by [step]. Without this wrapper, Fire TV remotes can FOCUS a Material
 * slider but can't ADJUST it — the standard slider responds only to gesture
 * drags / keyboard arrows (which Fire TV remotes don't emit).
 *
 * A subtle accent-colored border appears on the wrapper when focused so the
 * user can see which slider they're currently controlling.
 */
@Composable
private fun DpadSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    step: Float,
    colors: SliderColors,
    modifier: Modifier = Modifier,
) {
    var isFocused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) AppColors.TiviAccent else Color.Transparent,
                shape = shape,
            )
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .focusable()
            .onFocusChanged { isFocused = it.isFocused }
            .onPreviewKeyEvent { event ->
                if (event.nativeKeyEvent.action != android.view.KeyEvent.ACTION_DOWN) {
                    return@onPreviewKeyEvent false
                }
                val newValue = when (event.nativeKeyEvent.keyCode) {
                    android.view.KeyEvent.KEYCODE_DPAD_LEFT -> value - step
                    android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> value + step
                    else -> return@onPreviewKeyEvent false
                }
                onValueChange(newValue.coerceIn(valueRange.start, valueRange.endInclusive))
                true
            },
    ) {
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = colors,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
