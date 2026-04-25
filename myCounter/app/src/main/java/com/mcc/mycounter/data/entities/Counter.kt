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
    val tapColorArgb: Int = 0xFF6750A4.toInt(),
    val tapImageUri: String? = null,
    val periodicity: String = Periodicity.DAILY.name,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastTapAt: Long? = null,
    val lastConsolidatedAt: Long? = null,
    /** Se true il TAP funziona come start/stop di un timer (modalità "Conta Tempo"). */
    val timeMode: Boolean = false,
    /** Quando non null, c'è un timer in esecuzione: indica l'istante di start. */
    val runningStartedAt: Long? = null
) {
    /** Quanto manca all'obiettivo. Negativo se l'obiettivo è già stato superato. */
    fun remainingToTarget(): Long =
        if (dailyTarget <= 0) Long.MAX_VALUE else (dailyTarget - currentValue)

    /** True se siamo nella "zona calda" (>= 80% dell'obiettivo, target > 0). */
    fun isInHotZone(): Boolean =
        dailyTarget > 0 && currentValue.toDouble() >= dailyTarget * 0.8

    /** True se l'obiettivo è raggiunto o superato. */
    fun isTargetReached(): Boolean =
        dailyTarget > 0 && currentValue >= dailyTarget

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
