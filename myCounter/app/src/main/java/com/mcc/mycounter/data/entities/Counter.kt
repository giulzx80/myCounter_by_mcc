package com.mcc.mycounter.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Periodicità del contatore. Determina il "periodo corrente" per le statistiche
 * e la regola di consolidamento automatico:
 *  - DAILY: ogni nuovo giorno l'app chiede di consolidare il giorno precedente.
 *  - WEEKLY: ogni nuova settimana (a partire dal lunedì) l'app chiede di
 *    consolidare la settimana precedente.
 *  - MONTHLY/YEARLY: nessun prompt automatico, solo segnalazione visuale del periodo.
 */
enum class Periodicity {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY;

    companion object {
        fun fromName(s: String?): Periodicity =
            entries.firstOrNull { it.name == s } ?: DAILY
    }
}

/**
 * Tipo di obiettivo. Definisce la SEMANTICA dell'obiettivo per ogni counter:
 *  - NONE   → nessun obiettivo (target = 0); nessun messaggio premio/avviso.
 *  - TARGET → obiettivo da raggiungere (es. bicchieri d'acqua, passi). Più
 *             ti avvicini = meglio. Raggiungere/superare = SUCCESSO.
 *  - LIMIT  → tetto massimo da non superare (es. caffè, sigarette, calorie).
 *             Restare sotto = SUCCESSO. Superare = FAILURE.
 */
enum class GoalType {
    NONE,
    TARGET,
    LIMIT;

    companion object {
        fun fromName(s: String?): GoalType =
            entries.firstOrNull { it.name == s } ?: TARGET
    }
}

/**
 * Stato corrente del counter rispetto all'obiettivo, calcolato da
 * [Counter.computeGoalState]. Usato per scegliere colori, icone, messaggi.
 */
enum class GoalState {
    /** nessun obiettivo configurato → niente badge */
    NONE,
    /** in progresso, sotto la soglia di "hot zone" (<80%) */
    PROGRESS,
    /** zona calda: 80%-99% del target (warning per LIMIT, incoraggiamento per TARGET) */
    HOT_ZONE,
    /** outcome positivo: TARGET raggiunto OPPURE LIMIT non sforato (≤ target) */
    SUCCESS,
    /** outcome negativo: LIMIT sforato (> target). Solo per LIMIT. */
    FAILURE
}

/**
 * Entità "Counter" = istanza di contatore configurata dall'utente.
 *
 * @param name nome del contatore (es. "Conta Sigarette")
 * @param step quantità sommata/sottratta a ogni TAP (es. 1)
 * @param startValue valore iniziale (default 0)
 * @param currentValue valore corrente (mutato dai TAP)
 * @param reverse se true il TAP DECREMENTA invece di incrementare
 * @param dailyTarget obiettivo (per il periodo). 0 = nessun target.
 * @param costPerTap costo per ogni tap. 0 = non visualizzato.
 * @param tapColorArgb colore ARGB del pulsante principale
 * @param tapImageUri URI dell'immagine personalizzata sul pulsante (può essere null)
 * @param periodicity periodicità (giornaliera/mensile/annuale)
 * @param createdAt timestamp ms creazione
 * @param updatedAt timestamp ms ultimo update
 * @param lastTapAt timestamp ms ultimo TAP (per la logica "nuovo giorno")
 * @param lastConsolidatedAt timestamp dell'ultimo consolidamento (per abilitare il Reset)
 */
@Entity(tableName = "counters")
data class Counter(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val step: Int = 1,
    val startValue: Long = 0,
    /**
     * Valore corrente del contatore.
     *  - In modalità classica:  conta dei TAP (numero intero).
     *  - In modalità Conta Tempo: somma cumulata in SECONDI delle sessioni cronometrate.
     */
    val currentValue: Long = 0,
    val reverse: Boolean = false,
    /**
     * Obiettivo del periodo.
     *  - In modalità classica:  numero di tap.
     *  - In modalità Conta Tempo: numero di SECONDI (l'editor accetta minuti, qui vengono x60).
     */
    val dailyTarget: Long = 0,
    /**
     * Costo associato.
     *  - In modalità classica:  costo per ogni TAP.
     *  - In modalità Conta Tempo: costo per MINUTO.
     */
    val costPerTap: Double = 0.0,
    // Default tap color: grigio scuro DexHub (#52525B), coerente con la
    // palette "Default" del tema. L'utente può cambiarlo dal CounterEditor.
    val tapColorArgb: Int = 0xFF52525B.toInt(),
    val tapImageUri: String? = null,
    val periodicity: String = Periodicity.DAILY.name,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastTapAt: Long? = null,
    val lastConsolidatedAt: Long? = null,
    /** Se true il TAP funziona come start/stop di un timer (modalità "Conta Tempo"). */
    val timeMode: Boolean = false,
    /** Quando non null, c'è un timer in esecuzione: indica l'istante di start. */
    val runningStartedAt: Long? = null,
    /**
     * Tipo di obiettivo: NONE / TARGET (raggiungere) / LIMIT (non superare).
     * Default TARGET per retro-compatibilità con i counter creati prima
     * dell'introduzione del tipo obiettivo.
     */
    val goalType: String = GoalType.TARGET.name,
    /**
     * Email degli "accountability partner": una o più, separate da virgola
     * (es. "coach@x.com, mamma@y.com"). Quando è valorizzata, a fine periodo
     * l'app prepara una bozza-mail con il riepilogo da inviare a TUTTI gli
     * indirizzi in TO.
     */
    val accountabilityEmail: String? = null,
    /**
     * Tag logico opzionale per il webhook globale: viene inserito nel payload
     * (campo `counter.tag`) così che il server downstream possa instradare/
     * filtrare gli eventi senza dover decodificare nome o id. Esempio: "vices",
     * "health", "work". Lascia vuoto per non includerlo nel payload.
     */
    val webhookTag: String? = null
) {

    /**
     * Lista pulita di email accountability (split su `,`, `;`, spazio o newline).
     * Filtra entries vuote e fa trim. Vuota se [accountabilityEmail] è null/blank.
     */
    fun accountabilityEmails(): List<String> =
        accountabilityEmail
            ?.split(',', ';', ' ', '\n', '\r', '\t')
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: emptyList()

    /** Tipo di obiettivo del counter, decoded dall'enum string [goalType]. */
    fun goalTypeEnum(): GoalType = GoalType.fromName(goalType)

    /** Quanto manca all'obiettivo. Negativo se l'obiettivo è già stato superato. */
    fun remainingToTarget(): Long =
        if (dailyTarget <= 0) Long.MAX_VALUE else (dailyTarget - currentValue)

    /**
     * Stato corrente del counter rispetto all'obiettivo.
     * Vedi [GoalState] per i 5 valori possibili.
     */
    fun computeGoalState(): GoalState {
        val type = goalTypeEnum()
        if (type == GoalType.NONE || dailyTarget <= 0) return GoalState.NONE
        val ratio = currentValue.toDouble() / dailyTarget
        return when (type) {
            GoalType.TARGET -> when {
                currentValue >= dailyTarget -> GoalState.SUCCESS
                ratio >= 0.8 -> GoalState.HOT_ZONE
                else -> GoalState.PROGRESS
            }
            GoalType.LIMIT -> when {
                currentValue > dailyTarget -> GoalState.FAILURE
                ratio >= 0.8 -> GoalState.HOT_ZONE
                else -> GoalState.PROGRESS
            }
            GoalType.NONE -> GoalState.NONE
        }
    }

    /**
     * True se è il momento di mostrare un feedback visivo "caldo" (border
     * giallo/animato sul TAP). Vale sia per TARGET (vicino al traguardo)
     * sia per LIMIT (vicino al limite).
     */
    fun isInHotZone(): Boolean = computeGoalState() == GoalState.HOT_ZONE

    /**
     * True se il counter è in stato POSITIVO ora:
     *  - TARGET: traguardo raggiunto/superato
     *  - LIMIT:  ancora dentro il limite (anche al 99%)
     * Per UI/badge.
     */
    fun isPositiveOutcome(): Boolean = when (computeGoalState()) {
        GoalState.SUCCESS -> true
        else -> false
    }

    /**
     * True se il counter è in stato NEGATIVO ora:
     *  - LIMIT sforato. (TARGET non raggiunto durante il periodo non è ancora un fail.)
     */
    fun isNegativeOutcome(): Boolean = computeGoalState() == GoalState.FAILURE

    /** Backward-compat: in passato significava "TARGET raggiunto". */
    @Deprecated("Usa computeGoalState() / isPositiveOutcome()", ReplaceWith("isPositiveOutcome()"))
    fun isTargetReached(): Boolean = isPositiveOutcome()

    /**
     * Costo totale corrente.
     *  - Classico: currentValue (tap) * costPerTap.
     *  - Conta Tempo: (currentValue / 60) [minuti] * costPerTap [costo per minuto].
     */
    fun totalCost(): Double =
        if (timeMode) (currentValue / 60.0) * costPerTap
        else currentValue * costPerTap

    /** True se il timer del Conta Tempo è in esecuzione. */
    fun isTimerRunning(): Boolean = timeMode && runningStartedAt != null
}
