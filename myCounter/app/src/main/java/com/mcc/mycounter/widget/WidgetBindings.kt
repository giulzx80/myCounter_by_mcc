package com.mcc.mycounter.widget

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Mappa persistente "appWidgetId → counterId" usata per dire a ogni
 * istanza di [CounterWidget] quale contatore mostrare.
 *
 * Più widget possono coesistere su una stessa home (es. uno per "Conta Sigarette"
 * e uno per "Caffè"): ognuno ha un appWidgetId diverso → counter diverso.
 *
 * Salviamo in un DataStore dedicato ("widget_bindings") per non sporcare le
 * preferenze utente principali.
 */
private val Context.widgetBindingsStore by preferencesDataStore(name = "widget_bindings")

object WidgetBindings {

    private fun key(widgetId: Int) = longPreferencesKey("widget_$widgetId")

    suspend fun setBinding(context: Context, widgetId: Int, counterId: Long) {
        context.widgetBindingsStore.edit { it[key(widgetId)] = counterId }
    }

    suspend fun getBinding(context: Context, widgetId: Int): Long? {
        return context.widgetBindingsStore.data
            .map { it[key(widgetId)] }
            .first()
    }

    suspend fun removeBindings(context: Context, widgetIds: IntArray) {
        context.widgetBindingsStore.edit { prefs ->
            widgetIds.forEach { prefs.remove(key(it)) }
        }
    }
}
