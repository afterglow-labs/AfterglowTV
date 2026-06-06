package com.afterglowtv.app.ui.screens.welcome

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.afterglowtv.app.R
import com.afterglowtv.app.ui.components.shell.AfterglowHero
import com.afterglowtv.app.ui.design.AppColors
import com.afterglowtv.app.ui.design.GlowSpec
import com.afterglowtv.app.ui.design.afterglow
import com.afterglowtv.domain.repository.ProviderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@HiltViewModel
class WelcomeViewModel @Inject constructor(
    private val providerRepository: ProviderRepository
) : ViewModel() {

    private val _hasProviders = MutableStateFlow<Boolean?>(null)
    val hasProviders: StateFlow<Boolean?> = _hasProviders.asStateFlow()

    init {
        viewModelScope.launch {
            providerRepository.getProviders()
                .map { it.isNotEmpty() }
                .collect { _hasProviders.value = it }
        }
    }
}

@Composable
fun WelcomeScreen(
    onNavigateToHome: () -> Unit,
    viewModel: WelcomeViewModel = hiltViewModel()
) {
    val hasProviders by viewModel.hasProviders.collectAsStateWithLifecycle()

    LaunchedEffect(hasProviders) {
        if (hasProviders != null) {
            onNavigateToHome()
        }
    }

    // Branded loading splash — usually flashes for ~100 ms while the provider
    // count loads, but worth making the first thing users see feel like the
    // product they downloaded.
    AfterglowHero(
        wordmark = "TV",
        logoSize = 120.dp,
    ) {
        Spacer(Modifier.height(28.dp))
        CircularProgressIndicator(
            color = AppColors.TiviAccent,
            strokeWidth = 3.dp,
            modifier = Modifier.size(36.dp),
        )
    }
}
