package com.mcc.mycounter.report

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.mcc.mycounter.data.entities.Consolidation
import com.mcc.mycounter.data.entities.Counter
import com.mcc.mycounter.data.entities.Periodicity
import com.mcc.mycounter.data.entities.TapEvent
import com.mcc.mycounter.ui.util.formatCurrency
import com.mcc.mycounter.ui.util.formatDate
import com.mcc.mycounter.ui.util.formatDateTime
import com.mcc.mycounter.ui.util.formatDuration
import com.mcc.mycounter.ui.util.formatDurationMs
import com.mcc.mycounter.ui.util.formatThousand
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Generatore di report PDF per un singolo contatore.
 *
 * Usa l'API nativa Android [PdfDocument] (zero dipendenze esterne).
 * Il PDF generato viene salvato nella cache directory dell'app, così è subito
 * disponibile per la condivisione via [androidx.core.content.FileProvider].
 *
 * Layout:
 *  - Titolo + data
 *  - Anagrafica del contatore
 *  - Statistiche periodo corrente
 *  - Storico consolidamenti (ultimi 20)
 *  - Storico tap (ultimi 100, dal più recente)
 *  - Footer
 *
 * Multi-pagina automatico: quando lo spazio sulla pagina si esaurisce viene
 * aperta una nuova pagina A4.
 */
class ReportGenerator(private val context: Context) {

    // Dimensione pagina A4 in punti PostScript (1 pt = 1/72 inch).
    private val pageWidth = 595
    private val pageHeight = 842
    private val marginX = 40f
    private val marginY = 50f
    // Brand color del PDF di report — grigio scuro DexHub (palette Default).
    private val brand = AndroidColor.parseColor("#1F2933")

    private val titlePaint = Paint().apply {
        color = AndroidColor.BLACK
        textSize = 20f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        isAntiAlias = true
    }
    private val sectionPaint = Paint().apply {
        color = brand
        textSize = 15f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        isAntiAlias = true
    }
    private val bodyPaint = Paint().apply {
        color = AndroidColor.DKGRAY
        textSize = 11f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        isAntiAlias = true
    }
    private val grayPaint = Paint().apply {
        color = AndroidColor.GRAY
        textSize = 10f
        isAntiAlias = true
    }
    private val rulePaint = Paint().apply {
        color = AndroidColor.LTGRAY
        strokeWidth = 1f
    }

    fun generate(
        counter: Counter,
        taps: List<TapEvent>,
        consolidations: List<Consolidation>
    ): File {
        val pdf = PdfDocument()
        var pageNumber = 1
        var page: PdfDocument.Page? = null
        var canvas: Canvas? = null
        var y = marginY

        fun newPage(): Canvas {
            page?.let { pdf.finishPage(it) }
            val info = PdfDocument.PageInfo
                .Builder(pageWidth, pageHeight, pageNumber++)
                .create()
            page = pdf.startPage(info)
            y = marginY
            return page!!.canvas
        }

        canvas = newPage()

        fun ensureSpace(needed: Float) {
            if (y + needed > pageHeight - marginY) {
                canvas = newPage()
            }
        }

        fun drawText(text: String, paint: Paint, lineHeight: Float = paint.textSize + 4f) {
            ensureSpace(lineHeight)
            canvas!!.drawText(text, marginX, y + paint.textSize, paint)
            y += lineHeight
        }

        fun rule() {
            ensureSpace(8f)
            canvas!!.drawLine(marginX, y, pageWidth - marginX, y, rulePaint)
            y += 8f
        }

        fun spacer(h: Float = 10f) {
            ensureSpace(h)
            y += h
        }

        // ---- Titolo ----
        drawText("Report myCounter", titlePaint, 26f)
        drawText("Contatore: ${counter.name}", sectionPaint, 22f)
        drawText("Generato il ${System.currentTimeMillis().formatDateTime()}", grayPaint, 18f)
        rule()

        // ---- Anagrafica ----
        drawText("Anagrafica", sectionPaint, 22f)
        drawText("Nome: ${counter.name}", bodyPaint)
        drawText(
            "Modalità: ${if (counter.timeMode) "Conta Tempo (cronometro)" else "Classica (conteggio)"}",
            bodyPaint
        )
        if (!counter.timeMode) {
            drawText("Step (incremento per tap): ${counter.step}", bodyPaint)
            drawText("Valore di partenza: ${counter.startValue.formatThousand()}", bodyPaint)
            drawText("Conteggio rovesciato: ${if (counter.reverse) "Sì" else "No"}", bodyPaint)
        }
        if (counter.dailyTarget > 0) {
            drawText(
                "Obiettivo: " + if (counter.timeMode)
                    "${counter.dailyTarget / 60L} minuti (${counter.dailyTarget.formatDuration()})"
                else
                    counter.dailyTarget.formatThousand(),
                bodyPaint
            )
        }
        if (counter.costPerTap > 0) {
            drawText(
                "Costo: ${counter.costPerTap.formatCurrency()} " +
                        if (counter.timeMode) "/ minuto" else "/ tap",
                bodyPaint
            )
        }
        val periodicityLabel = when (Periodicity.fromName(counter.periodicity)) {
            Periodicity.DAILY -> "Giornaliera"
            Periodicity.WEEKLY -> "Settimanale (consolida lunedì)"
            Periodicity.MONTHLY -> "Mensile"
            Periodicity.YEARLY -> "Annuale"
        }
        drawText("Periodicità: $periodicityLabel", bodyPaint)
        rule()

        // ---- Statistiche ----
        drawText("Statistiche periodo corrente", sectionPaint, 22f)
        if (counter.timeMode) {
            val sessions = taps.filter { it.durationMs != null }
            val totalSec = counter.currentValue
            val avgSec = if (sessions.isNotEmpty())
                (sessions.sumOf { it.durationMs ?: 0L } / 1000L) / sessions.size
            else 0L
            val maxSec = (sessions.maxOfOrNull { it.durationMs ?: 0L } ?: 0L) / 1000L
            drawText("Tempo totale accumulato: ${totalSec.formatDuration()}", bodyPaint)
            drawText("Numero sessioni: ${sessions.size}", bodyPaint)
            drawText("Sessione media: ${avgSec.formatDuration()}", bodyPaint)
            drawText("Sessione più lunga: ${maxSec.formatDuration()}", bodyPaint)
            if (counter.costPerTap > 0) {
                drawText("Costo totale: ${counter.totalCost().formatCurrency()}", bodyPaint)
            }
            if (counter.dailyTarget > 0) {
                val pct = ((counter.currentValue.toDouble() / counter.dailyTarget) * 100)
                    .coerceAtLeast(0.0)
                drawText("Progresso obiettivo: %.0f%%".format(pct), bodyPaint)
            }
        } else {
            drawText("Valore attuale: ${counter.currentValue.formatThousand()}", bodyPaint)
            drawText("Tap totali registrati: ${taps.size}", bodyPaint)
            if (counter.costPerTap > 0) {
                drawText("Costo totale: ${counter.totalCost().formatCurrency()}", bodyPaint)
            }
            if (counter.dailyTarget > 0) {
                val pct = ((counter.currentValue.toDouble() / counter.dailyTarget) * 100)
                    .coerceAtLeast(0.0)
                drawText("Progresso obiettivo: %.0f%%".format(pct), bodyPaint)
            }
        }
        rule()

        // ---- Consolidamenti ----
        if (consolidations.isNotEmpty()) {
            drawText("Storico consolidamenti (${consolidations.size} totali)", sectionPaint, 22f)
            consolidations
                .sortedByDescending { it.closedAt }
                .take(20)
                .forEach { c ->
                    val valueLabel = if (counter.timeMode)
                        "tempo=${c.finalValue.formatDuration()}"
                    else
                        "valore=${c.finalValue.formatThousand()}"
                    val tapLabel = if (counter.timeMode)
                        "sessioni=${c.tapsCount}"
                    else
                        "tap=${c.tapsCount}"
                    drawText(
                        "${c.closedAt.formatDate()} · $valueLabel, $tapLabel, " +
                                "costo=${c.totalCost.formatCurrency()}" +
                                (if (c.targetReached) " · OBIETTIVO ✓" else ""),
                        bodyPaint
                    )
                }
            rule()
        }

        // ---- Storico tap / sessioni ----
        val historyTitle = if (counter.timeMode)
            "Storico sessioni cronometrate (${taps.count { it.durationMs != null }}, ultime 100)"
        else
            "Storico tap (${taps.size} totali, ultime 100 mostrate)"
        drawText(historyTitle, sectionPaint, 22f)
        if (taps.isEmpty()) {
            drawText("Nessun tap registrato.", bodyPaint)
        } else {
            taps
                .sortedByDescending { it.timestamp }
                .take(100)
                .forEach { ev ->
                    val noteSuffix = if (!ev.note.isNullOrBlank()) " [${ev.note}]" else ""
                    val widgetSuffix = if (ev.fromWidget) " (widget)" else ""
                    if (ev.durationMs != null && ev.sessionStartedAt != null) {
                        // Sessione cronometrata: inizio → fine, durata
                        drawText(
                            "${ev.sessionStartedAt.formatDateTime()} → ${ev.timestamp.formatDateTime()} " +
                                    "· durata ${ev.durationMs.formatDurationMs()}$noteSuffix$widgetSuffix",
                            bodyPaint, lineHeight = 14f
                        )
                    } else {
                        drawText(
                            "${ev.timestamp.formatDateTime()} · " +
                                    "${ev.valueBefore.formatThousand()} → ${ev.valueAfter.formatThousand()} " +
                                    "(Δ${ev.delta})$noteSuffix$widgetSuffix",
                            bodyPaint, lineHeight = 14f
                        )
                    }
                }
        }

        // ---- Footer ----
        spacer(20f)
        rule()
        drawText("Generato da myCounter by MCC", grayPaint, 14f)

        // Chiudi l'ultima pagina
        page?.let { pdf.finishPage(it) }

        // Salva nel cache directory
        val sanitizedName = counter.name.replace(Regex("[^A-Za-z0-9_-]"), "_").take(40)
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ITALY).format(Date())
        val file = File(context.cacheDir, "report_${sanitizedName}_$timestamp.pdf")
        FileOutputStream(file).use { pdf.writeTo(it) }
        pdf.close()
        return file
    }
}
