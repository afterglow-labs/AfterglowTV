package com.afterglowtv.app.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.afterglowtv.app.ui.design.AppColors
import com.afterglowtv.app.ui.design.AppShapeSet
import com.afterglowtv.app.ui.design.AppStyles
import com.afterglowtv.app.ui.design.GlowSpec
import com.afterglowtv.app.ui.design.afterglow
import com.afterglowtv.data.preferences.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class StyleCustomizerViewModel @Inject constructor(
    private val preferences: PreferencesRepository,
) : ViewModel() {
    private val _shapeSetValue = MutableStateFlow(AppStyles.value)
    val shapeSetValue: StateFlow<AppShapeSet> = _shapeSetValue.asStateFlow()

    private fun publish() {
        _shapeSetValue.value = AppStyles.value
    }

    fun selectButton(style: AppShapeSet.ButtonStyle) {
        AppStyles.setButton(style)
        publish()
        viewModelScope.launch { preferences.setStyleButton(style.name) }
    }

    fun selectEpgCell(style: AppShapeSet.EpgCellStyle) {
        AppStyles.setEpgCell(style)
        publish()
        viewModelScope.launch { preferences.setStyleEpgCell(style.name) }
    }

    fun selectEpgLiveCell(style: AppShapeSet.EpgLiveCellStyle) {
        AppStyles.setEpgLiveCell(style)
        publish()
        viewModelScope.launch { preferences.setStyleEpgLiveCell(style.name) }
    }

    fun selectTextField(style: AppShapeSet.TextFieldStyle) {
        AppStyles.setTextField(style)
        publish()
        viewModelScope.launch { preferences.setStyleTextField(style.name) }
    }

    fun selectChannelRow(style: AppShapeSet.ChannelRowStyle) {
        AppStyles.setChannelRow(style)
        publish()
        viewModelScope.launch { preferences.setStyleChannelRow(style.name) }
    }

    fun selectPill(style: AppShapeSet.PillStyle) {
        AppStyles.setPill(style)
        publish()
        viewModelScope.launch { preferences.setStylePill(style.name) }
    }

    fun selectFocus(style: AppShapeSet.FocusStyle) {
        AppStyles.setFocus(style)
        publish()
        viewModelScope.launch { preferences.setStyleFocus(style.name) }
    }

    fun selectProgress(style: AppShapeSet.ProgressStyle) {
        AppStyles.setProgress(style)
        publish()
        viewModelScope.launch { preferences.setStyleProgress(style.name) }
    }
}

@Composable
fun StyleCustomizerScreen(
    onBack: () -> Unit,
    viewModel: StyleCustomizerViewModel = hiltViewModel(),
) {
    val active by viewModel.shapeSetValue.collectAsState()

    SettingsToolDialog(
        title = "Customize",
        subtitle = "Pick a shape for every piece. Changes are instant — no restart, no save button.",
        onDismiss = onBack,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                AxisPicker(
                    title = "Buttons",
                    options = AppShapeSet.ButtonStyle.entries,
                    selected = active.button,
                    onSelect = viewModel::selectButton,
                )
            }
            item {
                AxisPicker(
                    title = "EPG cells",
                    options = AppShapeSet.EpgCellStyle.entries,
                    selected = active.epgCell,
                    onSelect = viewModel::selectEpgCell,
                )
            }
            item {
                AxisPicker(
                    title = "EPG live cell",
                    options = AppShapeSet.EpgLiveCellStyle.entries,
                    selected = active.epgLiveCell,
                    onSelect = viewModel::selectEpgLiveCell,
                )
            }
            item {
                AxisPicker(
                    title = "Text fields",
                    options = AppShapeSet.TextFieldStyle.entries,
                    selected = active.textField,
                    onSelect = viewModel::selectTextField,
                )
            }
            item {
                AxisPicker(
                    title = "Channel rows",
                    options = AppShapeSet.ChannelRowStyle.entries,
                    selected = active.channelRow,
                    onSelect = viewModel::selectChannelRow,
                )
            }
            item {
                AxisPicker(
                    title = "Pills",
                    options = AppShapeSet.PillStyle.entries,
                    selected = active.pill,
                    onSelect = viewModel::selectPill,
                )
            }
            item {
                AxisPicker(
                    title = "Focus",
                    options = AppShapeSet.FocusStyle.entries,
                    selected = active.focus,
                    onSelect = viewModel::selectFocus,
                )
            }
            item {
                AxisPicker(
                    title = "Progress",
                    options = AppShapeSet.ProgressStyle.entries,
                    selected = active.progress,
                    onSelect = viewModel::selectProgress,
                )
            }
        }
    }
}

@Composable
private fun <T : Enum<T>> AxisPicker(
    title: String,
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = TextUnit(20f, TextUnitType.Sp),
            ),
            color = AppColors.TextPrimary,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(options) { option ->
                StyleChip(
                    label = humanize(option.name),
                    isSelected = option == selected,
                    onClick = { onSelect(option) },
                )
            }
        }
    }
}

@Composable
private fun StyleChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(999.dp)
    Box(
        modifier = Modifier
            .let { base ->
                if (isSelected) base.afterglow(
                    specs = listOf(GlowSpec(AppColors.TiviAccent, 14.dp, 0.6f)),
                    shape = shape,
                ) else base
            }
            .clip(shape)
            .background(
                if (isSelected) AppColors.TiviAccent.copy(alpha = 0.20f)
                else AppColors.TiviSurfaceBase
            )
            .border(
                BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) AppColors.TiviAccent else AppColors.TiviSurfaceAccent,
                ),
                shape,
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            ),
            color = if (isSelected) AppColors.TextPrimary else AppColors.TextSecondary,
        )
    }
}

private fun humanize(enumName: String): String =
    enumName.split('_').joinToString(" ") { word ->
        word.lowercase().replaceFirstChar { it.uppercase() }
    }
