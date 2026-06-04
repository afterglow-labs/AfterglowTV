package com.afterglowtv.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.afterglowtv.app.R
import com.afterglowtv.app.ui.components.dialogs.PremiumDialog
import com.afterglowtv.app.ui.components.dialogs.PremiumDialogActionButton
import com.afterglowtv.app.ui.components.dialogs.PremiumDialogFooterButton
import com.afterglowtv.app.ui.interaction.TvButton
import com.afterglowtv.app.ui.interaction.TvClickableSurface
import com.afterglowtv.app.ui.interaction.mouseClickable
import com.afterglowtv.app.ui.theme.OnBackground
import com.afterglowtv.app.ui.theme.OnSurface
import com.afterglowtv.app.ui.theme.OnSurfaceDim
import com.afterglowtv.app.ui.theme.Primary

@Composable
internal fun SettingsSectionHeader(
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = Primary
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = OnSurfaceDim
        )
    }
}

@Composable
internal fun SettingsRow(label: String, value: String) {
    val focusRequester = remember { FocusRequester() }
    TvClickableSurface(
        onClick = {},
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = Primary.copy(alpha = 0.15f)
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .mouseClickable(
                focusRequester = focusRequester,
                onClick = {}
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = OnSurface)
            Text(text = value, style = MaterialTheme.typography.bodyMedium, color = OnBackground)
        }
    }
}

@Composable
internal fun ClickableSettingsRow(
    label: String,
    value: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    indent: Dp = 0.dp
) {
    val focusRequester = remember { FocusRequester() }
    TvClickableSurface(
        onClick = { if (enabled) onClick() },
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = if (enabled) Primary.copy(alpha = 0.15f) else Color.Transparent
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .mouseClickable(
                focusRequester = focusRequester,
                onClick = { if (enabled) onClick() }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp + indent, end = 8.dp, top = 12.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) OnSurface else OnSurfaceDim
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) Primary else OnSurfaceDim
            )
        }
    }
}

@Composable
internal fun SwitchSettingsRow(
    label: String,
    value: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    indent: Dp = 0.dp
) {
    val focusRequester = remember { FocusRequester() }
    TvClickableSurface(
        onClick = { if (enabled) onCheckedChange(!checked) },
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = if (enabled) Primary.copy(alpha = 0.15f) else Color.Transparent
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .mouseClickable(
                focusRequester = focusRequester,
                onClick = { if (enabled) onCheckedChange(!checked) }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp + indent, end = 8.dp, top = 12.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (enabled) OnSurface else OnSurfaceDim
                )
                Text(text = value, style = MaterialTheme.typography.bodySmall, color = OnSurfaceDim)
            }
            Switch(
                checked = checked,
                onCheckedChange = { if (enabled) onCheckedChange(it) },
                enabled = enabled
            )
        }
    }
}

@Composable
internal fun LiveTvQuickFiltersDialog(
    filters: List<String>,
    onDismiss: () -> Unit,
    onAddFilter: (String) -> Unit,
    onRemoveFilter: (String) -> Unit
) {
    var pendingFilter by rememberSaveable { mutableStateOf("") }

    PremiumDialog(
        title = stringResource(R.string.settings_live_tv_quick_filters_dialog_title),
        subtitle = stringResource(R.string.settings_live_tv_quick_filters_dialog_subtitle),
        onDismissRequest = onDismiss,
        widthFraction = 0.5f,
        content = {
            EpgSourceTextField(
                value = pendingFilter,
                onValueChange = { pendingFilter = it },
                placeholder = stringResource(R.string.settings_live_tv_quick_filters_placeholder)
            )
            PremiumDialogActionButton(
                label = stringResource(R.string.settings_live_tv_quick_filters_add),
                enabled = pendingFilter.isNotBlank(),
                onClick = {
                    onAddFilter(pendingFilter)
                    pendingFilter = ""
                }
            )
            if (filters.isEmpty()) {
                Text(
                    text = stringResource(R.string.settings_live_tv_quick_filters_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceDim
                )
            } else {
                Text(
                    text = stringResource(R.string.settings_live_tv_quick_filters_saved),
                    style = MaterialTheme.typography.labelMedium,
                    color = OnSurfaceDim
                )
                LazyColumn(
                    modifier = Modifier.heightIn(max = 260.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filters, key = { it }) { filter ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = filter,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = OnSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                TvButton(onClick = { onRemoveFilter(filter) }) {
                                    Text(stringResource(R.string.settings_live_tv_quick_filters_remove))
                                }
                            }
                        }
                    }
                }
            }
        },
        footer = {
            PremiumDialogFooterButton(
                label = stringResource(R.string.settings_cancel),
                onClick = onDismiss
            )
        }
    )
}

internal fun formatLiveTvQuickFiltersValue(filters: List<String>, context: android.content.Context): String {
    if (filters.isEmpty()) {
        return context.getString(R.string.settings_live_tv_quick_filters_none)
    }
    return context.resources.getQuantityString(
        R.plurals.settings_live_tv_quick_filters_count,
        filters.size,
        filters.size
    )
}

@Composable
internal fun SettingsNavItem(
    label: String,
    badgeIcon: ImageVector,
    accentColor: Color,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    indent: Dp = 0.dp,
    compact: Boolean = false,
    onClick: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val badgeSize = if (compact) 24.dp else 28.dp
    val iconSize = if (compact) 15.dp else 18.dp
    val verticalPadding = if (compact) 8.dp else 12.dp
    val barHeight = if (compact) 16.dp else 22.dp
    TvClickableSurface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(0.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isSelected) Primary.copy(alpha = 0.11f) else Color.Transparent,
            focusedContainerColor = Primary.copy(alpha = 0.22f)
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .mouseClickable(focusRequester = focusRequester, onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 10.dp + indent, end = 10.dp, top = verticalPadding, bottom = verticalPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                Modifier
                    .width(3.dp)
                    .height(barHeight)
                    .background(
                        color = if (isSelected) Primary else Color.Transparent,
                        shape = RoundedCornerShape(2.dp)
                    )
            )
            Box(
                Modifier
                    .size(badgeSize)
                    .background(accentColor.copy(alpha = 0.18f), RoundedCornerShape(7.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = badgeIcon,
                    contentDescription = null,
                    modifier = Modifier.size(iconSize),
                    tint = accentColor,
                )
            }
            Text(
                text = label,
                style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
                color = if (isSelected) Primary else OnBackground,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

@Composable
internal fun SettingsNavHeader(
    label: String,
    badgeIcon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 10.dp, end = 10.dp, top = 12.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            Modifier
                .width(3.dp)
                .height(22.dp)
                .background(Color.Transparent, RoundedCornerShape(2.dp))
        )
        Box(
            Modifier
                .size(28.dp)
                .background(accentColor.copy(alpha = 0.18f), RoundedCornerShape(7.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = badgeIcon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = accentColor,
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = OnBackground,
            fontWeight = FontWeight.SemiBold
        )
    }
}
