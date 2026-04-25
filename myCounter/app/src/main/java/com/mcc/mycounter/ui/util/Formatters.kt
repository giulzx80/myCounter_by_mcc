package com.mcc.mycounter.ui.util

import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date
import java.util.Locale

/** Formatta un Long con separatore migliaia in stile italiano. */
fun Long.formatThousand(): String =
    NumberFormat.getIntegerInstance(Locale.ITALIAN).format(this)

/** Formatta un Double in valuta (€). */
fun Double.formatCurrency(): String =
    NumberFormat.getCurrencyInstance(Locale.ITALY).format(this)

/** Formatta un Float "corto" come fa MyVote (es. 7 invece di 7.0). */
fun Float.formatShort(): String =
    if (this % 1f == 0f) this.toInt().toString() else "%.2f".format(Locale.ITALY, this)

/** Formatta un timestamp epoch ms come data+ora locale corta. */
fun Long.formatDateTime(): String {
    val df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.ITALY)
    return df.format(Date(this))
}

/** Formatta un timestamp epoch ms come solo data corta. */
fun Long.formatDate(): String {
    val df = DateFormat.getDateInstance(DateFormat.SHORT, Locale.ITALY)
    return df.format(Date(this))
}

/**
 * Formatta una durata in secondi come "HH:MM:SS" (o "MM:SS" se < 1h).
 */
fun Long.formatDuration(): String {
    val totalSec = if (this < 0) 0L else this
    val hours = totalSec / 3600
    val minutes = (totalSec % 3600) / 60
    val seconds = totalSec % 60
    return if (hours > 0)
        "%d:%02d:%02d".format(hours, minutes, seconds)
    else
        "%02d:%02d".format(minutes, seconds)
}

/** Formatta una durata in millisecondi come HH:MM:SS / MM:SS. */
fun Long.formatDurationMs(): String = (this / 1000L).formatDuration()

