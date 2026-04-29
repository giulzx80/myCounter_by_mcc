package com.mcc.mycounter.data.repository

import com.mcc.mycounter.data.AppDatabase
import com.mcc.mycounter.data.entities.Achievement
import com.mcc.mycounter.data.entities.Consolidation
import com.mcc.mycounter.data.entities.Counter
import com.mcc.mycounter.data.entities.GoalState
import com.mcc.mycounter.data.entities.GoalType
import com.mcc.mycounter.data.entities.Periodicity
import com.mcc.mycounter.data.entities.TapEvent
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

/**
 * Repository centrale dell'app: tutte le operazioni di scrittura/lettura
 * passano da qui. Mantiene la logica di:
 *  - applicazione del TAP (incremento/decremento + storico)
 *  - consolidamento (snapshot + reset valore)
 *  - reset (cancellazione storico/statistiche, solo dopo consolidamento)
 *  - rilevazione "nuovo giorno" per la periodicità DAILY
 */
class CounterRepository(private val db: AppDatabase) {

    private val counterDao get() = db.counterDao()
    private val tapDao get() = db.tapEventDao()
    private val consolidationDao get() = db.consolidationDao()
    private val achievementDao get() = db.achievementDao()

    // ---- Counter CRUD ----

    fun observeAll(): Flow<List<Counter>> = counterDao.observeAll()
    fun observeCounter(id: Long): Flow<Counter?> = counterDao.observeById(id)
    suspend fun getById(id: Long): Counter? = counterDao.getById(id)
    suspend fun getAll(): List<Counter> = counterDao.getAll()
    suspend fun count(): Int = counterDao.count()

    suspend fun upsert(counter: Counter): Long {
        val now = System.currentTimeMillis()
        return if (counter.id == 0L) {
            counterDao.insert(counter.copy(createdAt = now, updatedAt = now))
        } else {
            counterDao.update(counter.copy(updatedAt = now))
            counter.id
        }
    }

    suspend fun delete(id: Long) = counterDao.deleteById(id)

    suspend fun duplicate(id: Long): Long? {
        val src = counterDao.getById(id) ?: return null
        val copy = src.copy(
            id = 0,
            name = "${src.name} (copia)",
            currentValue = src.startValue,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            lastTapAt = null,
            lastConsolidatedAt = null
        )
        return counterDao.insert(copy)
    }

    // ---- Tap ----

    /**
     * Applica un TAP al contatore.
     *
     * Comportamento:
     *  - **Counter classico**: aggiunge `step` (o sottrae se `reverse=true`),
     *    registra l'evento nello storico, aggiorna il valore in DB.
     *  - **Counter Conta Tempo**: il primo tap AVVIA il timer (salva
     *    `runningStartedAt`), il successivo tap STOPPA il timer, calcola la
     *    durata in secondi, la accumula a `currentValue` e la registra come
     *    sessione nello storico (con `durationMs` e `sessionStartedAt`).
     *
     * @return il counter aggiornato, oppure null se non esiste.
     */
    suspend fun applyTap(counterId: Long, fromWidget: Boolean = false): Counter? {
        val counter = counterDao.getById(counterId) ?: return null

        return if (counter.timeMode) {
            applyTimeTapInternal(counter, fromWidget)
        } else {
            applyClassicTap(counter, fromWidget)
        }
    }

    private suspend fun applyClassicTap(counter: Counter, fromWidget: Boolean): Counter? {
        val delta = if (counter.reverse) -counter.step else counter.step
        val before = counter.currentValue
        val after = before + delta
        val now = System.currentTimeMillis()

        tapDao.insert(
            TapEvent(
                counterId = counter.id,
                timestamp = now,
                delta = delta,
                valueBefore = before,
                valueAfter = after,
                fromWidget = fromWidget
            )
        )
        counterDao.updateValue(counter.id, after, now)
        return counterDao.getById(counter.id)
    }

    private suspend fun applyTimeTapInternal(counter: Counter, fromWidget: Boolean): Counter? {
        val now = System.currentTimeMillis()

        if (counter.runningStartedAt == null) {
            // START: marca l'inizio della sessione, nessun evento storico finché non si STOPpa.
            counterDao.update(
                counter.copy(
                    runningStartedAt = now,
                    lastTapAt = now,
                    updatedAt = now
                )
            )
        } else {
            // STOP: calcola durata, accumula in currentValue (in secondi), salva sessione.
            val startedAt = counter.runningStartedAt
            val durationMs = (now - startedAt).coerceAtLeast(0L)
            val durationSec = durationMs / 1000L
            val before = counter.currentValue
            val after = before + durationSec

            tapDao.insert(
                TapEvent(
                    counterId = counter.id,
                    timestamp = now,
                    delta = durationSec.toInt().coerceAtLeast(0),
                    valueBefore = before,
                    valueAfter = after,
                    fromWidget = fromWidget,
                    durationMs = durationMs,
                    sessionStartedAt = startedAt
                )
            )
            counterDao.update(
                counter.copy(
                    currentValue = after,
                    runningStartedAt = null,
                    lastTapAt = now,
                    updatedAt = now
                )
            )
        }
        return counterDao.getById(counter.id)
    }

    /**
     * Annulla un timer Conta Tempo in esecuzione senza registrare la sessione.
     * Utile come "discard" se l'utente vuole cestinare un timer partito per errore.
     */
    suspend fun cancelRunningTimer(counterId: Long): Counter? {
        val counter = counterDao.getById(counterId) ?: return null
        if (!counter.timeMode || counter.runningStartedAt == null) return counter
        counterDao.update(counter.copy(runningStartedAt = null, updatedAt = System.currentTimeMillis()))
        return counterDao.getById(counterId)
    }

    /** Tap manuale "inverso" (utile come undo o decremento esplicito). */
    suspend fun applyManualDelta(counterId: Long, delta: Int, note: String? = null): Counter? {
        val counter = counterDao.getById(counterId) ?: return null
        val before = counter.currentValue
        val after = before + delta
        val now = System.currentTimeMillis()
        tapDao.insert(
            TapEvent(
                counterId = counterId,
                timestamp = now,
                delta = delta,
                valueBefore = before,
                valueAfter = after,
                note = note
            )
        )
        counterDao.updateValue(counterId, after, now)
        return counterDao.getById(counterId)
    }

    // ---- Storico / Statistiche ----

    fun observeTapHistory(counterId: Long): Flow<List<TapEvent>> =
        tapDao.observeForCounter(counterId)

    fun observeConsolidations(counterId: Long): Flow<List<Consolidation>> =
        consolidationDao.observeForCounter(counterId)

    suspend fun lastConsolidation(counterId: Long): Consolidation? =
        consolidationDao.lastForCounter(counterId)

    suspend fun tapsInRange(counterId: Long, fromMs: Long): List<TapEvent> =
        tapDao.getInRange(counterId, fromMs)

    suspend fun allTaps(counterId: Long): List<TapEvent> =
        tapDao.getAllForCounter(counterId)

    suspend fun allConsolidations(counterId: Long): List<Consolidation> =
        consolidationDao.getAllForCounter(counterId)

    // ---- Consolida ----

    /**
     * Consolida il periodo corrente: salva uno snapshot, calcola
     * l'esito (Achievement) rispetto all'obiettivo, azzera il valore.
     *
     * Restituisce la Consolidation creata (oppure null se il counter non esiste).
     */
    suspend fun consolidate(counterId: Long): Consolidation? {
        val counter = counterDao.getById(counterId) ?: return null
        val now = System.currentTimeMillis()
        val periodStart = counter.lastConsolidatedAt ?: counter.createdAt
        val taps = tapDao.countForCounter(counterId)

        val consolidation = Consolidation(
            counterId = counterId,
            closedAt = now,
            finalValue = counter.currentValue,
            tapsCount = taps,
            totalCost = counter.totalCost(),
            targetReached = counter.isPositiveOutcome(),
            periodStartedAt = periodStart,
            periodEndedAt = now,
            periodicity = counter.periodicity
        )
        val consolidationId = consolidationDao.insert(consolidation)

        // Calcola e salva l'achievement collegato.
        createAchievementForConsolidation(counter, consolidationId, now)

        counterDao.applyConsolidationReset(counterId, counter.startValue, now)
        return consolidation.copy(id = consolidationId)
    }

    /**
     * Crea un [Achievement] per il consolidamento appena fatto, calcolando
     * outcome e streak. Pubblico così le notifiche/email possono leggerlo.
     */
    suspend fun createAchievementForConsolidation(
        counter: Counter,
        consolidationId: Long,
        endedAt: Long
    ): Achievement {
        val goalTypeEnum = counter.goalTypeEnum()
        val outcome: Achievement.Outcome = when {
            goalTypeEnum == GoalType.NONE || counter.dailyTarget <= 0 -> Achievement.Outcome.NEUTRAL
            goalTypeEnum == GoalType.TARGET ->
                if (counter.currentValue >= counter.dailyTarget) Achievement.Outcome.SUCCESS
                else Achievement.Outcome.FAILURE
            goalTypeEnum == GoalType.LIMIT ->
                if (counter.currentValue <= counter.dailyTarget) Achievement.Outcome.SUCCESS
                else Achievement.Outcome.FAILURE
            else -> Achievement.Outcome.NEUTRAL
        }

        // Streak corrente: se SUCCESS, +1 alla streak precedente; altrimenti reset a 0.
        val previous = achievementDao.lastForCounter(counter.id)
        val previousStreak = previous?.streak ?: 0
        val newStreak = when (outcome) {
            Achievement.Outcome.SUCCESS -> previousStreak + 1
            Achievement.Outcome.FAILURE -> 0
            Achievement.Outcome.NEUTRAL -> previousStreak  // mantieni
        }

        val ach = Achievement(
            counterId = counter.id,
            consolidationId = consolidationId,
            periodEndedAt = endedAt,
            outcome = outcome.name,
            goalType = counter.goalType,
            finalValue = counter.currentValue,
            targetValue = counter.dailyTarget,
            streak = newStreak
        )
        val id = achievementDao.insert(ach)
        return ach.copy(id = id)
    }

    fun observeAchievements(counterId: Long): Flow<List<Achievement>> =
        achievementDao.observeForCounter(counterId)

    suspend fun lastAchievement(counterId: Long): Achievement? =
        achievementDao.lastForCounter(counterId)

    suspend fun lastNAchievements(counterId: Long, n: Int): List<Achievement> =
        achievementDao.lastNForCounter(counterId, n)

    /**
     * Consolida e ritorna sia la Consolidation appena creata sia l'Achievement
     * collegato. Usato dai chiamanti che vogliono mostrare una notifica/email
     * con l'esito subito dopo il consolidamento.
     */
    suspend fun consolidateWithAchievement(counterId: Long): Pair<Consolidation, Achievement>? {
        val cons = consolidate(counterId) ?: return null
        val ach = achievementDao.lastForCounter(counterId) ?: return null
        return cons to ach
    }

    /**
     * Reset: cancella TUTTO lo storico e tutte le consolidazioni del counter.
     * Pre-condizione: deve esistere almeno un consolidamento.
     */
    suspend fun resetHistoryAndStats(counterId: Long): Boolean {
        val last = consolidationDao.lastForCounter(counterId) ?: return false
        // Esiste almeno un consolidamento → reset OK
        tapDao.deleteAllForCounter(counterId)
        consolidationDao.deleteAllForCounter(counterId)
        achievementDao.deleteAllForCounter(counterId)
        // Lasciamo il counter intatto (valore corrente, target, costi, ecc.)
        // ma puliamo lastConsolidatedAt — il next periodo riparte da "ora".
        val counter = counterDao.getById(counterId) ?: return true
        counterDao.update(
            counter.copy(
                lastConsolidatedAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )
        // restituiamo true; `last` ci dice che c'era un consolidamento da rispettare
        @Suppress("UNUSED_VARIABLE") val unused = last
        return true
    }

    /** Verifica se il Reset è abilitato (= esiste almeno un consolidamento). */
    suspend fun canReset(counterId: Long): Boolean =
        consolidationDao.countForCounter(counterId) > 0

    // ---- Rilevazione "nuovo periodo" per il consolidamento automatico ----

    /**
     * Restituisce true se l'app dovrebbe proporre il consolidamento del periodo
     * precedente all'apertura. Vale per le periodicità che hanno il consolidamento
     * automatico:
     *  - DAILY  → prompt se l'ultimo tap è in un giorno precedente a oggi.
     *  - WEEKLY → prompt se l'ultimo tap è in una settimana precedente
     *    (settimane ISO, lunedì = primo giorno).
     */
    suspend fun shouldPromptPeriodConsolidation(counterId: Long): Boolean {
        val counter = counterDao.getById(counterId) ?: return false
        if (counter.currentValue == counter.startValue) return false
        val last = counter.lastTapAt ?: return false

        return when (Periodicity.fromName(counter.periodicity)) {
            Periodicity.DAILY -> {
                val today = startOfToday()
                last < today
            }
            Periodicity.WEEKLY -> {
                val mondayThisWeek = startOfThisWeekMonday()
                last < mondayThisWeek
            }
            else -> false
        }
    }

    /** Backward-compat alias. */
    @Deprecated(
        "Usa shouldPromptPeriodConsolidation",
        ReplaceWith("shouldPromptPeriodConsolidation(counterId)")
    )
    suspend fun shouldPromptDailyConsolidation(counterId: Long): Boolean =
        shouldPromptPeriodConsolidation(counterId)

    /**
     * AUTO-CONSOLIDAMENTO silenzioso: se il counter è DAILY/WEEKLY e il periodo
     * corrente è cambiato rispetto a quello dell'ultimo consolidamento (o
     * della creazione se non c'è stato nessun consolidamento ancora) E ci sono
     * dati da consolidare, esegue [consolidate] senza chiedere conferma.
     *
     * Viene chiamata:
     *  - all'avvio dell'app
     *  - ad ogni refresh del widget
     *  - ad ogni TAP del widget
     * così che, ad esempio, un counter "Conta Caffè" giornaliero appare a 0
     * al primo accesso del giorno successivo, senza azione manuale.
     *
     * Skippa il consolidamento se:
     *  - la periodicità è MONTHLY/YEARLY (non c'è auto-reset per quelle)
     *  - il valore corrente == startValue (niente da consolidare)
     *  - il counter è in modalità Conta Tempo CON timer in esecuzione
     *    (non vogliamo perdere la sessione attiva)
     */
    suspend fun autoConsolidateIfNeeded(counterId: Long): Consolidation? {
        val counter = counterDao.getById(counterId) ?: return null
        val periodicity = Periodicity.fromName(counter.periodicity)
        if (periodicity != Periodicity.DAILY && periodicity != Periodicity.WEEKLY) return null
        if (counter.currentValue == counter.startValue) return null
        if (counter.timeMode && counter.runningStartedAt != null) return null

        val periodBoundary = when (periodicity) {
            Periodicity.DAILY -> startOfToday()
            Periodicity.WEEKLY -> startOfThisWeekMonday()
            else -> return null
        }
        val lastRef = counter.lastConsolidatedAt ?: counter.createdAt
        if (lastRef >= periodBoundary) return null

        return consolidate(counterId)
    }

    /** Esegue [autoConsolidateIfNeeded] su TUTTI i counter. Ritorna quanti azzerati. */
    suspend fun autoConsolidateAllIfNeeded(): Int {
        var count = 0
        counterDao.getAll().forEach { c ->
            if (autoConsolidateIfNeeded(c.id) != null) count++
        }
        return count
    }

    /**
     * Come [autoConsolidateIfNeeded] ma ritorna anche l'Achievement creato.
     * Utile ai chiamanti che vogliono mostrare notifica/email subito dopo.
     */
    suspend fun autoConsolidateIfNeededWithAchievement(counterId: Long): Pair<Consolidation, Achievement>? {
        val cons = autoConsolidateIfNeeded(counterId) ?: return null
        val ach = achievementDao.lastForCounter(counterId) ?: return null
        return cons to ach
    }

    private fun startOfToday(): Long = Calendar.getInstance().apply {
        timeInMillis = System.currentTimeMillis()
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun startOfThisWeekMonday(): Long = Calendar.getInstance().apply {
        timeInMillis = System.currentTimeMillis()
        firstDayOfWeek = Calendar.MONDAY
        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
