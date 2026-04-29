package com.mcc.mycounter.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import com.mcc.mycounter.MyCounterApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Action callback eseguito quando l'utente preme il pulsante TAP nel widget.
 *
 * Risoluzione del counterId (in ordine di priorità):
 *   1. Binding per-widget salvato dalla [WidgetConfigActivity]
 *   2. Parametro passato dall'UI (counterId mostrato al momento del tap)
 *   3. Counter selezionato globalmente nelle preferenze (ultimo fallback)
 *
 * Sequenza:
 *   1. Persiste il TAP al repository (Dispatchers.IO).
 *   2. Aggiorna il widget state — Glance reactivity tramite currentState().
 *   3. Forza il re-render via broadcast standard di Android (path nativo,
 *      bypassa qualsiasi cache di Glance).
 *   4. update()/updateAll() di Glance per coprire altre istanze.
 *
 * Caso speciale **Conta Tempo, STOP del timer**: dopo aver registrato la
 * sessione, il widget mostra per ~3 secondi la durata appena conclusa al
 * posto dell'icona Play. Trascorsi i 3 secondi, lo state viene ripulito e
 * il widget torna a mostrare l'icona Play.
 */
class TapWidgetAction : ActionCallback {

    companion object {
        /** Quanto rimane visibile la durata della sessione appena conclusa nel widget. */
        private const val POST_STOP_FLASH_MS = 3_000L
    }

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val app = context.applicationContext as MyCounterApplication

        val appWidgetId = try {
            GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
        } catch (_: Throwable) { -1 }

        val boundId = if (appWidgetId > 0) WidgetBindings.getBinding(context, appWidgetId) else null
        val paramId = parameters[CounterWidget.CounterIdKey] ?: 0L
        val fallbackSelected = app.settingsManager.settingsFlow.first().selectedCounterId

        val counterId = when {
            boundId != null && boundId > 0 -> boundId
            paramId > 0 -> paramId
            fallbackSelected > 0 -> fallbackSelected
            else -> 0L
        }
        if (counterId <= 0) return

        // Auto-consolidamento PRIMA del tap: se è cambiato il giorno (DAILY) o
        // la settimana (WEEKLY), azzera silenziosamente. Così il TAP fatto al
        // mattino dopo mezzanotte parte da 0 invece di sommarsi al totale di ieri.
        withContext(Dispatchers.IO) {
            app.repository.autoConsolidateIfNeeded(counterId)
        }

        // Stato PRIMA del tap: serve per capire se questo tap è uno STOP del timer.
        val before = withContext(Dispatchers.IO) { app.repository.getById(counterId) }
        val wasRunning = before?.timeMode == true && before.runningStartedAt != null

        // 1. Persiste il TAP — la suspend ritorna a write completata.
        val updated = withContext(Dispatchers.IO) {
            app.repository.applyTap(counterId, fromWidget = true)
        } ?: return

        // 1.b Hot-zone notification (transizione a HOT_ZONE o FAILURE)
        runCatching {
            val wasHot = before?.isInHotZone() == true ||
                    before?.computeGoalState() == com.mcc.mycounter.data.entities.GoalState.FAILURE
            val isHotNow = updated.isInHotZone() ||
                    updated.computeGoalState() == com.mcc.mycounter.data.entities.GoalState.FAILURE
            if (!wasHot && isHotNow) {
                com.mcc.mycounter.notify.NotificationHelper.showHotZone(context, updated)
            }
        }

        val isStopTransition = wasRunning && updated.runningStartedAt == null && updated.timeMode
        val isStartTransition = !wasRunning && updated.runningStartedAt != null && updated.timeMode

        // Avvia/Ferma il blinker dell'icona STOP nei widget.
        when {
            isStartTransition -> WidgetBlinker.start(context, updated.id)
            isStopTransition -> WidgetBlinker.stop()
        }

        // Per il flash della durata: prendiamo l'ultima sessione (la più recente).
        val flashMs: Long = if (isStopTransition) {
            val recent = withContext(Dispatchers.IO) {
                app.repository.allTaps(updated.id)
                    .filter { it.durationMs != null }
                    .maxByOrNull { it.timestamp }
            }
            recent?.durationMs ?: 0L
        } else 0L

        // 2. Aggiorna il widget state per Glance reactivity (currentState()).
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[CounterWidget.Keys.COUNTER_ID] = updated.id
            prefs[CounterWidget.Keys.COUNTER_NAME] = updated.name
            prefs[CounterWidget.Keys.COUNTER_VALUE] = updated.currentValue
            prefs[CounterWidget.Keys.COUNTER_STEP] = updated.step
            prefs[CounterWidget.Keys.COUNTER_REVERSE] = updated.reverse
            prefs[CounterWidget.Keys.COUNTER_COLOR] = updated.tapColorArgb
            prefs[CounterWidget.Keys.COUNTER_TIME_MODE] = updated.timeMode
            prefs[CounterWidget.Keys.COUNTER_RUNNING] = updated.runningStartedAt != null
            prefs[CounterWidget.Keys.LAST_SESSION_FLASH_MS] = flashMs
        }

        // 3. Forza il re-render via broadcast standard di Android.
        forceWidgetUpdate(context)

        // 4. Belt-and-suspenders: API di Glance.
        runCatching {
            val widget = CounterWidget()
            widget.update(context, glanceId)
            widget.updateAll(context)
        }

        // 5. Caso STOP timer: dopo 3s pulisce il flash e re-renderizza così
        //    il widget torna a mostrare l'icona Play.
        if (isStopTransition && flashMs > 0L) {
            delay(POST_STOP_FLASH_MS)
            updateAppWidgetState(context, glanceId) { prefs ->
                prefs[CounterWidget.Keys.LAST_SESSION_FLASH_MS] = 0L
            }
            forceWidgetUpdate(context)
            runCatching {
                val widget = CounterWidget()
                widget.update(context, glanceId)
                widget.updateAll(context)
            }
        }
    }

    private fun forceWidgetUpdate(context: Context) {
        val mgr = AppWidgetManager.getInstance(context)
        val component = ComponentName(context, CounterWidgetReceiver::class.java)
        val ids = mgr.getAppWidgetIds(component)
        if (ids.isNotEmpty()) {
            val updateIntent = Intent(context, CounterWidgetReceiver::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(updateIntent)
        }
    }
}
