package com.mcc.mycounter.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Singolo evento di TAP (incremento o decremento).
 * Costituisce lo "Storico" del contatore.
 *
 * @param counterId id del [Counter] di appartenenza
 * @param timestamp ms quando il tap è avvenuto
 * @param delta variazione applicata (può essere negativa se reverse)
 * @param valueBefore valore prima del tap
 * @param valueAfter valore dopo il tap
 * @param note nota interna opzionale (es. "rimborso", "revert"...)
 * @param fromWidget true se generato dal widget (utile per analisi)
 */
@Entity(
    tableName = "tap_events",
    foreignKeys = [
        ForeignKey(
            entity = Counter::class,
            parentColumns = ["id"],
            childColumns = ["counterId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("counterId"), Index("timestamp")]
)
data class TapEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val counterId: Long,
    val timestamp: Long,
    val delta: Int,
    val valueBefore: Long,
    val valueAfter: Long,
    val note: String? = null,
    val fromWidget: Boolean = false,
    /**
     * Per modalità Conta Tempo: durata della sessione cronometrata in MILLISECONDI
     * (timestamp - sessionStartedAt). Null per i tap "classici".
     */
    val durationMs: Long? = null,
    /**
     * Per modalità Conta Tempo: istante (epoch ms) di inizio della sessione.
     * `timestamp` rappresenta invece l'istante di stop. Null per i tap classici.
     */
    val sessionStartedAt: Long? = null
) {
    /** True se questo evento è una sessione cronometrata. */
    fun isTimerSession(): Boolean = durationMs != null && sessionStartedAt != null
}
