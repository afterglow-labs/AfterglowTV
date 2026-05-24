package com.afterglowtv.app.ui.components.shell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afterglowtv.data.preferences.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
internal class AppShellViewModel @Inject constructor(
    preferencesRepository: PreferencesRepository
) : ViewModel() {
    val showAdultGuideTab: StateFlow<Boolean> = preferencesRepository.showAdultGuideTab.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = true
    )
}
