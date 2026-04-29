package com.mcc.mycounter.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mcc.mycounter.MyCounterApplication

/**
 * Factory condivisa per tutti i ViewModel dell'app.
 * Preleva le dipendenze dal [MyCounterApplication].
 */
class ViewModelFactory(private val app: MyCounterApplication) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(CounterViewModel::class.java) ->
                CounterViewModel(app, app.repository, app.settingsManager) as T
            modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
                SettingsViewModel(app.settingsManager) as T
            else -> throw IllegalArgumentException("ViewModel sconosciuto: ${modelClass.name}")
        }
    }
}
