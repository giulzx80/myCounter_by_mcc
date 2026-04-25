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
