package com.mcc.mycounter.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Esito di un periodo consolidato rispetto all'obiettivo del counter.
 * Generato automaticamente al consolidamento (manuale o auto).
 *
 * Permette di calcolare la "streak" (es. "5 giorni di fila sotto il limite")
 * e di mostrare riepiloghi/notifiche premio o avviso.
 *
 * @param outcome           "SUCCESS" / "FAILURE" / "NEUTRAL"
 * @param goalType          "TARGET" / "LIMIT" / "NONE" (snapshot del tipo
 *                          di obiettivo al momento del consolidamento)
 * @param finalValue        valore finale del periodo (n. tap o secondi accumulati)
 * @param targetValue       obiettivo del periodo (snapshot)
 * @param streak            streak corrente di SUCCESS consecutivi (0 se outcome != SUCCESS)
 * @param consolidationId   ID della Consolidation collegata (relazione 1-1)
 */
@Entity(
    tableName = "achievements",
    foreignKeys = [
        ForeignKey(
            entity = Counter::class,
            parentColumns = ["id"],
            childColumns = ["counterId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Consolidation::class,
            parentColumns = ["id"],
            childColumns = ["consolidationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("counterId"), Index("periodEndedAt"), Index("consolidationId")]
)
data class Achievement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val counterId: Long,
    val consolidationId: Long,
    val periodEndedAt: Long,
    val outcome: String,        // SUCCESS / FAILURE / NEUTRAL
    val goalType: String,       // TARGET / LIMIT / NONE
    val finalValue: Long,
    val targetValue: Long,
    val streak: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
) {
    enum class Outcome { SUCCESS, FAILURE, NEUTRAL;
        companion object {
            fun fromName(s: String?): Outcome = entries.firstOrNull { it.name == s } ?: NEUTRAL
        }
    }

    fun outcomeEnum(): Outcome = Outcome.fromName(outcome)
}
