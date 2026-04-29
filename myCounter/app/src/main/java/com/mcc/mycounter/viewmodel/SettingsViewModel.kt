package com.mcc.mycounter.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mcc.mycounter.data.preferences.SettingsManager
import com.mcc.mycounter.data.preferences.UserSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel per la schermata Impostazioni e per fornire il tema all'app.
 */
class SettingsViewModel(private val manager: SettingsManager) : ViewModel() {

    val settings: StateFlow<UserSettings> = manager.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = UserSettings()
    )

    fun updateTheme(mode: String, palette: String) = viewModelScope.launch {
        manager.updateTheme(mode, palette)
    }

    fun updateHaptic(enabled: Boolean) = viewModelScope.launch {
        manager.updateHaptic(enabled)
    }

    fun updateConfirmDecrement(enabled: Boolean) = viewModelScope.launch {
        manager.updateConfirmDecrement(enabled)
    }

    fun updateAccountabilityEmailEnabled(enabled: Boolean) = viewModelScope.launch {
        manager.updateAccountabilityEmailEnabled(enabled)
    }

    fun updateWebhook(enabled: Boolean, url: String, secret: String) = viewModelScope.launch {
        manager.updateWebhook(enabled, url.trim(), secret.trim())
    }

    fun reset() = viewModelScope.launch { manager.reset() }
}
