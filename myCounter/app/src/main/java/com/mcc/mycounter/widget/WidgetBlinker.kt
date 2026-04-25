package com.mcc.mycounter.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import com.mcc.mycounter.MyCounterApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Coroutine "battery-friendly" che, quando attiva, toggla a intervalli regolari
 * lo stato [CounterWidget.Keys.BLINK_ON] di TUTTE le istanze del widget e forza
 * un re-render: visivamente l'icona STOP nei widget Conta Tempo "pulsa".
 *
 * Si avvia quando l'utente preme PLAY nel widget (in [TapWidgetAction]) e si
 * ferma quando preme STOP. Il loop verifica anche lo stato del DB ad ogni
 * iterazione: se per qualche motivo il counter non è più "running" (es. STOP
 * dato dall'app), si auto-spegne.
 *
 * Nota: la coroutine vive nello scope dell'app (process-scope). Se il processo
 * viene killato dal sistema, il blink si interrompe ma il timer rimane (è
 * persistito in DB). Alla riapertura il blink riparte solo al prossimo PLAY.
 */
object WidgetBlinker {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile private var job: Job? = null

    /** Intervallo del toggle (ms). 1000ms = blink "lento" e percepibile. */
    private const val INTERVAL_MS = 1000L

    /** Avvia il blink se non è già attivo. */
    fun start(context: Context, counterIdHint: Long = 0L) {
        if (job?.isActive == true) return
        val app = context.applicationContext
        job = scope.launch {
            var on = true
            try {
                while (isActive) {
                    // Verifica che ci sia ancora almeno un timer Conta Tempo running
                    // per il counter; se non c'è, esci.
                    val app2 = app as MyCounterApplication
                    val anyRunning = if (counterIdHint > 0) {
                        app2.repository.getById(counterIdHint)?.runningStartedAt != null
                    } else {
                        app2.repository.getAll().any {
                            it.timeMode && it.runningStartedAt != null
                        }
                    }
                    if (!anyRunning) break

                    // Aggiorna lo stato BLINK_ON di TUTTI i widget piazzati
                    val mgr = GlanceAppWidgetManager(app)
                    val ids = runCatching {
                        mgr.getGlanceIds(CounterWidget::class.java)
                    }.getOrDefault(emptyList())
                    ids.forEach { glanceId ->
                        runCatching {
                            updateAppWidgetState(app, glanceId) { prefs ->
                                prefs[CounterWidget.Keys.BLINK_ON] = on
                            }
                        }
                    }
                    // Re-render globale
                    runCatching { CounterWidget().updateAll(app) }

                    on = !on
                    delay(INTERVAL_MS)
                }
            } finally {
                // Spegni: lascia BLINK_ON = true (icona piena) come stato finale.
                val mgr = GlanceAppWidgetManager(app)
                val ids = runCatching {
                    mgr.getGlanceIds(CounterWidget::class.java)
                }.getOrDefault(emptyList())
                ids.forEach { glanceId ->
                    runCatching {
                        updateAppWidgetState(app, glanceId) { prefs ->
                            prefs[CounterWidget.Keys.BLINK_ON] = true
                        }
                    }
                }
                runCatching { CounterWidget().updateAll(app) }
            }
        }
    }

    /** Ferma immediatamente il blink. */
    fun stop() {
        job?.cancel()
        job = null
    }
}
