package com.afterglowtv.app.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.afterglowtv.app.R
import com.afterglowtv.app.store.StorePolicy
import com.afterglowtv.app.ui.components.TvEmptyState
import com.afterglowtv.app.ui.interaction.TvClickableSurface
import com.afterglowtv.app.ui.theme.OnSurfaceDim
import com.afterglowtv.app.ui.theme.Primary
import com.afterglowtv.domain.model.Provider
import com.afterglowtv.domain.model.ActiveLiveSource
import com.afterglowtv.domain.model.ProviderM3uPlaylistKind
import com.afterglowtv.domain.model.ProviderType

internal enum class ProviderSettingsCategory {
    LIVE_TV,
    VOD
}

internal fun LazyListScope.providerSection(
    uiState: SettingsUiState,
    providerCategory: ProviderSettingsCategory,
    onAddProvider: (ProviderM3uPlaylistKind?) -> Unit,
    onEditProvider: (Provider) -> Unit,
    onNavigateToParentalControl: (Long) -> Unit,
    viewModel: SettingsViewModel,
    providerState: SettingsProviderSectionState
) {
    if (uiState.providers.isEmpty()) {
        item {
            TvEmptyState(
                title = stringResource(R.string.settings_no_providers),
                subtitle = stringResource(R.string.settings_no_providers_subtitle),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 8.dp)
            )
        }
    } else {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                if (providerCategory == ProviderSettingsCategory.LIVE_TV) {
                    ProviderSettingsContainer(
                        title = "Live TV",
                        emptyTitle = "No Live TV providers",
                        emptySubtitle = "Add a playlist or provider for Live TV.",
                        providers = uiState.providers.filter { it.isLiveProviderCandidate() },
                        preferredProviderId = uiState.activeProviderId,
                        uiState = uiState,
                        providerState = providerState,
                        viewModel = viewModel,
                        onEditProvider = onEditProvider,
                        onNavigateToParentalControl = onNavigateToParentalControl
                    )
                } else {
                    ProviderSettingsContainer(
                        title = "VOD",
                        emptyTitle = "No VOD providers",
                        emptySubtitle = "Add a VOD playlist to use in the VOD container.",
                        providers = uiState.providers.filter { it.isVodProvider() },
                        preferredProviderId = (uiState.activeVodSource as? ActiveLiveSource.ProviderSource)?.providerId,
                        uiState = uiState,
                        providerState = providerState,
                        viewModel = viewModel,
                        onEditProvider = onEditProvider,
                        onNavigateToParentalControl = onNavigateToParentalControl
                    )
                }

                if (providerCategory == ProviderSettingsCategory.LIVE_TV && StorePolicy.current.showAdvancedSourceTypes) {
                    CombinedM3uProfilesCard(
                        profiles = uiState.combinedProfiles,
                        availableProviders = uiState.availableM3uProviders,
                        selectedProfileId = providerState.selectedCombinedProfileId,
                        activeLiveSource = uiState.activeLiveSource,
                        onSelectProfile = { providerState.selectedCombinedProfileId = it },
                        onCreateProfile = { providerState.showCreateCombinedDialog = true },
                        onActivateProfile = { profileId -> viewModel.setActiveCombinedProfile(profileId) },
                        onDeleteProfile = { profileId ->
                            if (providerState.selectedCombinedProfileId == profileId) {
                                providerState.selectedCombinedProfileId = null
                            }
                            viewModel.deleteCombinedProfile(profileId)
                        },
                        onRenameProfile = { profileId ->
                            providerState.selectedCombinedProfileId = profileId
                            providerState.showRenameCombinedDialog = true
                        },
                        onAddProvider = { profileId ->
                            providerState.selectedCombinedProfileId = profileId
                            providerState.showAddCombinedMemberDialog = true
                        },
                        onRemoveProvider = { profileId, providerId ->
                            viewModel.removeProviderFromCombinedProfile(profileId, providerId)
                        },
                        onToggleProviderEnabled = { profileId, providerId, enabled ->
                            viewModel.setCombinedProviderEnabled(profileId, providerId, enabled)
                        },
                        onMoveProvider = { profileId, providerId, moveUp ->
                            viewModel.moveCombinedProvider(profileId, providerId, moveUp)
                        }
                    )
                }
            }
        }
    }

    item {
        val providerKind = if (providerCategory == ProviderSettingsCategory.VOD) {
            ProviderM3uPlaylistKind.VOD
        } else {
            null
        }
        TvClickableSurface(
            onClick = { onAddProvider(providerKind) },
            shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = Primary.copy(alpha = 0.15f),
                focusedContainerColor = Primary.copy(alpha = 0.3f)
            ),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.settings_add_provider),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Primary
                )
            }
        }
    }
}

@Composable
private fun ProviderSettingsContainer(
    title: String,
    emptyTitle: String,
    emptySubtitle: String,
    providers: List<Provider>,
    preferredProviderId: Long?,
    uiState: SettingsUiState,
    providerState: SettingsProviderSectionState,
    viewModel: SettingsViewModel,
    onEditProvider: (Provider) -> Unit,
    onNavigateToParentalControl: (Long) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = Primary
        )
        if (providers.isEmpty()) {
            TvEmptyState(
                title = emptyTitle,
                subtitle = emptySubtitle,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            var selectedProviderId by rememberSaveable(providers.map { it.id }, preferredProviderId) {
                mutableStateOf(preferredProviderId?.takeIf { id -> providers.any { it.id == id } } ?: providers.first().id)
            }
            LaunchedEffect(providers, preferredProviderId) {
                val availableIds = providers.map { it.id }.toSet()
                if (selectedProviderId !in availableIds) {
                    selectedProviderId = preferredProviderId?.takeIf { it in availableIds } ?: providers.first().id
                }
            }
            val selectedProvider = providers.firstOrNull { it.id == selectedProviderId } ?: providers.first()

            Text(
                text = stringResource(R.string.settings_provider_selector_hint),
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceDim
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 4.dp)
            ) {
                items(providers, key = { it.id }) { provider ->
                    ProviderSelectorTab(
                        provider = provider,
                        isSelected = provider.id == selectedProvider.id,
                        activeLabels = provider.activeRoleLabels(uiState),
                        onClick = { selectedProviderId = provider.id }
                    )
                }
            }

            ProviderSettingsCard(
                provider = selectedProvider,
                activeLabels = selectedProvider.activeRoleLabels(uiState),
                isSyncing = uiState.isSyncing,
                xtreamLiveOnboardingPhase = uiState.xtreamLiveOnboardingPhaseByProvider[selectedProvider.id],
                xtreamLiveOnboarding = uiState.xtreamLiveOnboardingByProvider[selectedProvider.id],
                xtreamIndexSectionStatuses = uiState.xtreamIndexSectionStatusByProvider[selectedProvider.id].orEmpty(),
                diagnostics = uiState.diagnosticsByProvider[selectedProvider.id],
                databaseMaintenance = uiState.databaseMaintenance,
                syncWarnings = uiState.syncWarningsByProvider[selectedProvider.id].orEmpty(),
                onRetryWarningAction = { action -> viewModel.retryWarningAction(selectedProvider.id, action) },
                onConnect = { viewModel.setActiveProvider(selectedProvider.id) },
                onRefresh = {
                    providerState.pendingSyncProviderId = selectedProvider.id
                    providerState.customSyncSelections = selectedProvider.defaultCustomSyncSelections()
                    providerState.showProviderSyncDialog = true
                },
                onDelete = { providerState.pendingDeleteProviderId = selectedProvider.id },
                onEdit = { onEditProvider(selectedProvider) },
                onParentalControl = { onNavigateToParentalControl(selectedProvider.id) },
                onToggleM3uVodClassification = { enabled ->
                    viewModel.setM3uVodClassificationEnabled(selectedProvider.id, enabled)
                },
                onRefreshM3uClassification = {
                    viewModel.refreshProviderClassification(selectedProvider.id)
                }
            )
        }
    }
}

internal enum class ProviderActiveRole {
    LIVE,
    VOD,
    ADULT,
    ADULT_VOD
}

internal fun providerActiveRoles(
    provider: Provider,
    activeProviderId: Long?,
    activeLiveSource: ActiveLiveSource?,
    activeVodSource: ActiveLiveSource?,
    activeAdultLiveSource: ActiveLiveSource? = null,
    activeAdultVodSource: ActiveLiveSource? = null
): Set<ProviderActiveRole> = buildSet {
    val liveProviderId = (activeLiveSource as? ActiveLiveSource.ProviderSource)?.providerId
    if (provider.id == liveProviderId || provider.id == activeProviderId && provider.isLiveProviderCandidate()) {
        add(ProviderActiveRole.LIVE)
    }
    val vodProviderId = (activeVodSource as? ActiveLiveSource.ProviderSource)?.providerId
    if (provider.id == vodProviderId) {
        add(ProviderActiveRole.VOD)
    }
    val adultLiveProviderId = (activeAdultLiveSource as? ActiveLiveSource.ProviderSource)?.providerId
    if (provider.id == adultLiveProviderId) {
        add(ProviderActiveRole.ADULT)
    }
    val adultVodProviderId = (activeAdultVodSource as? ActiveLiveSource.ProviderSource)?.providerId
    if (provider.id == adultVodProviderId) {
        add(ProviderActiveRole.ADULT_VOD)
    }
}

private fun Provider.activeRoleLabels(uiState: SettingsUiState): List<String> =
    providerActiveRoles(
        provider = this,
        activeProviderId = uiState.activeProviderId,
        activeLiveSource = uiState.activeLiveSource,
        activeVodSource = uiState.activeVodSource,
        activeAdultLiveSource = uiState.activeAdultLiveSource,
        activeAdultVodSource = uiState.activeAdultVodSource
    ).map { role ->
        when (role) {
            ProviderActiveRole.LIVE -> "Live Active"
            ProviderActiveRole.VOD -> "VOD Active"
            ProviderActiveRole.ADULT -> "Adult Active"
            ProviderActiveRole.ADULT_VOD -> "Adult VOD Active"
        }
    }

private fun Provider.isLiveProviderCandidate(): Boolean =
    type != ProviderType.M3U || m3uPlaylistKind != ProviderM3uPlaylistKind.VOD

private fun Provider.isVodProvider(): Boolean =
    type == ProviderType.M3U && m3uPlaylistKind == ProviderM3uPlaylistKind.VOD
