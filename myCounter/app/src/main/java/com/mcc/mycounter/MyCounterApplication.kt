package com.mcc.mycounter

import android.app.Application
import com.mcc.mycounter.data.AppDatabase
import com.mcc.mycounter.data.entities.Counter
import com.mcc.mycounter.data.entities.Periodicity
import com.mcc.mycounter.data.preferences.SettingsManager
import com.mcc.mycounter.data.repository.CounterRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Application root: crea e conserva le dipendenze singleton usate dal resto dell'app
 * (database, repository, settings).
 *
 * Struttura volutamente "manuale" (senza Hilt/Koin) per ridurre al minimo le dipendenze
 * - identica filosofia di MyVote_by_mcc.
 */
class MyCounterApplication : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val repository: CounterRepository by lazy { CounterRepository(database) }
    val settingsManager: SettingsManager by lazy { SettingsManager(this) }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        seedSampleDataIfEmpty()
        resumeBlinkerIfTimerRunning()
        autoConsolidateAtStartup()
        // Notifiche
        com.mcc.mycounter.notify.NotificationHelper.ensureChannel(this)
        com.mcc.mycounter.notify.DailyReminderWorker.schedule(this)
    }

    /**
     * All'avvio dell'app, controlla TUTTI i counter DAILY/WEEKLY e se è
     * passato il confine di periodo dall'ultimo consolidamento esegue il
     * consolidamento silenzioso (snapshot + reset). Per ogni counter
     * consolidato: notifica esito + eventuale bozza-mail.
     */
    private fun autoConsolidateAtStartup() {
        appScope.launch {
            // Itera manualmente così possiamo intercettare ogni consolidamento.
            val toCheck = repository.getAll()
            var anyReset = false
            toCheck.forEach { c ->
                val pair = repository.autoConsolidateIfNeededWithAchievement(c.id)
                if (pair != null) {
                    anyReset = true
                    val freshCounter = repository.getById(c.id) ?: c
                    com.mcc.mycounter.notify.NotificationHelper.showOutcome(
                        this@MyCounterApplication, freshCounter, pair.second
                    )
                    // Email accountability — solo se attiva da impostazioni
                    val s = settingsManager.settingsFlow.first()
                    if (s.accountabilityEmailEnabled) {
                        com.mcc.mycounter.notify.AccountabilityMailer.sendIfConfigured(
                            this@MyCounterApplication, freshCounter, pair.second
                        )
                    }
                    // Webhook (Step E) — sempre se attivo nelle impostazioni
                    com.mcc.mycounter.notify.WebhookSender.sendIfEnabled(
                        this@MyCounterApplication, freshCounter, pair.second
                    )
                }
            }
            if (anyReset) {
                com.mcc.mycounter.widget.WidgetUpdater.requestUpdateAll(this@MyCounterApplication)
            }
        }
    }

    /**
     * Se al riavvio dell'app esiste già almeno un counter Conta Tempo con timer
     * running (es. il processo è stato killato in background mentre il timer
     * era attivo), riprende il blink dell'icona STOP nei widget.
     */
    private fun resumeBlinkerIfTimerRunning() {
        appScope.launch {
            val anyRunning = repository.getAll().any {
                it.timeMode && it.runningStartedAt != null
            }
            if (anyRunning) {
                com.mcc.mycounter.widget.WidgetBlinker.start(this@MyCounterApplication)
            }
        }
    }

    /**
     * Al primo avvio (DB vuoto) inserisce un counter di esempio per dare immediato
     * contesto all'utente. Viene fatto in IO scope per non bloccare il main.
     */
    private fun seedSampleDataIfEmpty() {
        appScope.launch {
            if (repository.count() == 0) {
                repository.upsert(
                    Counter(
                        name = "Conta Sigarette",
                        step = 1,
                        startValue = 0,
                        currentValue = 0,
                        reverse = false,
                        dailyTarget = 10,
                        costPerTap = 0.30,
                        tapColorArgb = 0xFFE65100.toInt(),
                        periodicity = Periodicity.DAILY.name
                    )
                )
                repository.upsert(
                    Counter(
                        name = "Caffè",
                        step = 1,
                        startValue = 0,
                        currentValue = 0,
                        reverse = false,
                        dailyTarget = 4,
                        costPerTap = 1.20,
                        tapColorArgb = 0xFF6D4C41.toInt(),
                        periodicity = Periodicity.DAILY.name
                    )
                )
            }
        }
    }
}
