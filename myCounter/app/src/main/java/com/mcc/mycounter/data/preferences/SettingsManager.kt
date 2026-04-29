package com.mcc.mycounter.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Estensione: DataStore collegato al Context, scope file "settings"
private val Context.dataStore by preferencesDataStore(name = "settings")

/**
 * Modello delle impostazioni utente. Immutable — ogni modifica genera una nuova istanza.
 *
 * @param themeMode "system" / "light" / "dark"
 * @param paletteName palette globale (default / sunset / ocean / forest)
 * @param hapticFeedback true se ogni TAP fa una piccola vibrazione
 * @param confirmDecrement true se chiede conferma quando il valore scende sotto 0
 * @param selectedCounterId id del counter aperto in TapScreen all'avvio (-1 = nessuno)
 */
data class UserSettings(
    val themeMode: String = "system",
    val paletteName: String = "default",
    val hapticFeedback: Boolean = true,
    val confirmDecrement: Boolean = false,
    val selectedCounterId: Long = -1L,
    /**
     * Master switch per l'invio della bozza-mail "accountability" a fine periodo
     * (per i counter che hanno [com.mcc.mycounter.data.entities.Counter.accountabilityEmail]
     * valorizzato). Default ON.
     */
    val accountabilityEmailEnabled: Boolean = true,
    /**
     * Webhook globale: se true, dopo OGNI consolidamento l'app fa un POST JSON
     * all'URL configurato. Default OFF.
     */
    val webhookEnabled: Boolean = false,
    /** URL completo (https://...) dell'endpoint webhook. */
    val webhookUrl: String = "",
    /**
     * Secret opzionale: se valorizzato l'app firma il body con HMAC-SHA256 e
     * lo invia nell'header `X-myCounter-Signature` per consentire la
     * verifica server-side dell'autenticità.
     */
    val webhookSecret: String = ""
)

/**
 * Gestore centralizzato delle impostazioni, basato su DataStore Preferences.
 * Stessa filosofia di MyVote_by_mcc.SettingsManager.
 */
class SettingsManager(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val PALETTE = stringPreferencesKey("palette")
        val HAPTIC = booleanPreferencesKey("haptic_feedback")
        val CONFIRM_DEC = booleanPreferencesKey("confirm_decrement")
        val SELECTED_COUNTER = longPreferencesKey("selected_counter_id")
        val ACCOUNTABILITY_EMAIL_ENABLED = booleanPreferencesKey("accountability_email_enabled")
        val WEBHOOK_ENABLED = booleanPreferencesKey("webhook_enabled")
        val WEBHOOK_URL = stringPreferencesKey("webhook_url")
        val WEBHOOK_SECRET = stringPreferencesKey("webhook_secret")
    }

    val settingsFlow: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        UserSettings(
            themeMode = prefs[Keys.THEME_MODE] ?: "system",
            paletteName = prefs[Keys.PALETTE] ?: "default",
            hapticFeedback = prefs[Keys.HAPTIC] ?: true,
            confirmDecrement = prefs[Keys.CONFIRM_DEC] ?: false,
            selectedCounterId = prefs[Keys.SELECTED_COUNTER] ?: -1L,
            accountabilityEmailEnabled = prefs[Keys.ACCOUNTABILITY_EMAIL_ENABLED] ?: true,
            webhookEnabled = prefs[Keys.WEBHOOK_ENABLED] ?: false,
            webhookUrl = prefs[Keys.WEBHOOK_URL] ?: "",
            webhookSecret = prefs[Keys.WEBHOOK_SECRET] ?: ""
        )
    }

    suspend fun updateTheme(themeMode: String, paletteName: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = themeMode
            prefs[Keys.PALETTE] = paletteName
        }
    }

    suspend fun updateHaptic(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HAPTIC] = enabled }
    }

    suspend fun updateConfirmDecrement(enabled: Boolean) {
        context.dataStore.edit { it[Keys.CONFIRM_DEC] = enabled }
    }

    suspend fun updateSelectedCounter(id: Long) {
        context.dataStore.edit { it[Keys.SELECTED_COUNTER] = id }
    }

    suspend fun updateAccountabilityEmailEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.ACCOUNTABILITY_EMAIL_ENABLED] = enabled }
    }

    suspend fun updateWebhook(enabled: Boolean, url: String, secret: String) {
        context.dataStore.edit {
            it[Keys.WEBHOOK_ENABLED] = enabled
            it[Keys.WEBHOOK_URL] = url
            it[Keys.WEBHOOK_SECRET] = secret
        }
    }

    suspend fun reset() {
        context.dataStore.edit { it.clear() }
    }
}
