package com.mcc.mycounter.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Snapshot di chiusura periodo. Generato dalla funzione "Consolida" e
 * dal consolidamento automatico (per la periodicità DAILY).
 *
 * Una volta consolidato, il contatore viene azzerato (currentValue = startValue)
 * e le statistiche ripartono dal periodo successivo. Lo storico resta
 * comunque consultabile.
 *
 * @param counterId id del [Counter] consolidato
 * @param closedAt timestamp ms del consolidamento
 * @param finalValue valore finale al consolidamento
 * @param tapsCount numero di tap effettuati nel periodo
 * @param totalCost costo totale del periodo (finalValue * costPerTap calcolato all'epoca)
 * @param targetReached true se l'obiettivo era stato raggiunto al momento del consolidamento
 * @param periodStartedAt inizio del periodo consolidato
 * @param periodEndedAt fine del periodo consolidato (= closedAt)
 * @param periodicity periodicità del periodo consolidato
 */
@Entity(
    tableName = "consolidations",
    foreignKeys = [
        ForeignKey(
            entity = Counter::class,
            parentColumns = ["id"],
            childColumns = ["counterId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("counterId"), Index("closedAt")]
)
data class Consolidation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val counterId: Long,
    val closedAt: Long,
    val finalValue: Long,
    val tapsCount: Int,
    val totalCost: Double,
    val targetReached: Boolean,
    val periodStartedAt: Long,
    val periodEndedAt: Long,
    val periodicity: String
)
