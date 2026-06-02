package com.afterglowtv.app.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.compose.foundation.lazy.items
import com.afterglowtv.app.ui.design.FocusSpec
import com.afterglowtv.app.ui.interaction.TvClickableSurface
import com.afterglowtv.app.ui.theme.OnSurfaceDim
import com.afterglowtv.app.ui.theme.Primary

internal fun LazyListScope.epgSourcesSection(
    uiState: SettingsUiState,
    viewModel: SettingsViewModel
) {
    val epgSources = uiState.epgSources
    val providers = uiState.providers

    item {
        Text(
            text = "EPG Sources",
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFF66BB6A),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "Add external XMLTV EPG sources and assign them to providers. External sources are matched to channels by ID or name and override provider-native EPG data.",
            style = MaterialTheme.typography.bodySmall,
            color = OnSurfaceDim,
            modifier = Modifier.padding(bottom = 16.dp)
        )
    }

    item {
        AddEpgSourceCard(viewModel = viewModel)
    }

    if (epgSources.isEmpty()) {
        item {
            Text(
                text = "No external EPG sources configured. Add a source above to get started.",
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceDim,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }
    } else {
        items(epgSources, key = { source -> "epg-source-${source.id}" }) { source ->
            EpgSourceCard(
                source = source,
                isRefreshing = source.id in uiState.refreshingEpgSourceIds,
                pendingDelete = uiState.epgPendingDeleteSourceId == source.id,
                onToggleEnabled = { enabled -> viewModel.toggleEpgSourceEnabled(source.id, enabled) },
                onRefresh = { viewModel.refreshEpgSource(source.id) },
                onSetPendingDelete = { pending ->
                    viewModel.setPendingDeleteEpgSource(if (pending) source.id else null)
                },
                onDelete = { viewModel.deleteEpgSource(source.id) }
            )
        }
    }

    if (providers.isNotEmpty() && epgSources.isNotEmpty()) {
        item {
            Text(
                text = "Provider Assignments",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF66BB6A),
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
            )
            Text(
                text = "Assign EPG sources to providers. Channels will be matched automatically by ID or name.",
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceDim,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        items(providers, key = { provider -> "epg-provider-${provider.id}" }) { provider ->
            val assignments = uiState.epgSourceAssignments[provider.id].orEmpty()
            val resolutionSummary = uiState.epgResolutionSummaries[provider.id]
            val assignedSourceIds = assignments.map { it.epgSourceId }.toSet()
            val unassignedSources = epgSources.filter { it.id !in assignedSourceIds }

            LaunchedEffect(provider.id) {
                viewModel.loadEpgAssignments(provider.id)
            }

            ProviderEpgAssignmentsCard(
                providerName = provider.name,
                assignments = assignments,
                resolutionSummary = resolutionSummary,
                unassignedSources = unassignedSources,
                onMoveUp = { epgSourceId -> viewModel.moveEpgSourceAssignmentUp(provider.id, epgSourceId) },
                onMoveDown = { epgSourceId -> viewModel.moveEpgSourceAssignmentDown(provider.id, epgSourceId) },
                onRemove = { epgSourceId -> viewModel.unassignEpgSourceFromProvider(provider.id, epgSourceId) },
                onAssign = { epgSourceId -> viewModel.assignEpgSourceToProvider(provider.id, epgSourceId) }
            )
        }
    }
}

