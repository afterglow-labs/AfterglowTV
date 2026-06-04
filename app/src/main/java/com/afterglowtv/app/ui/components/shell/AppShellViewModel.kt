package com.afterglowtv.app.ui.components.shell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afterglowtv.app.store.StorePolicy
import com.afterglowtv.data.preferences.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
internal class AppShellViewModel @Inject constructor(
    preferencesRepository: PreferencesRepository
) : ViewModel() {
    val developerModeEnabled: StateFlow<Boolean> = preferencesRepository.developerModeEnabled
        .map(StorePolicy::effectiveDeveloperModeEnabled)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false
        )

    val showAdultTab: StateFlow<Boolean> = combine(
        preferencesRepository.developerModeEnabled,
        preferencesRepository.showAdultTab
    ) { developerModeEnabled, showAdultTab ->
        StorePolicy.current.showAdultSurfaces &&
            StorePolicy.effectiveDeveloperModeEnabled(developerModeEnabled) &&
            showAdultTab
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = false
    )
}
