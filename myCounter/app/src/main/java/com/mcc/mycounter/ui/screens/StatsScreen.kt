package com.mcc.mycounter.ui.screens

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.mcc.mycounter.MyCounterApplication
import com.mcc.mycounter.data.entities.TapEvent
import com.mcc.mycounter.report.ReportFileSharer
import com.mcc.mycounter.report.ReportGenerator
import com.mcc.mycounter.ui.components.GradientBackground
import com.mcc.mycounter.ui.components.PrimaryActionButton
import com.mcc.mycounter.ui.components.SectionCard
import com.mcc.mycounter.ui.util.formatCurrency
import com.mcc.mycounter.ui.util.formatDuration
import com.mcc.mycounter.ui.util.formatThousand
import com.mcc.mycounter.viewmodel.CounterViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.rememberCoroutineScope
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Schermata Statistiche: andamento del counter raggruppato per giorno/mese/anno
 * (in base al granularity scelto), totale costi e progresso vs obiettivo.
 *
 * Usa MPAndroidChart via AndroidView (interop Compose).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StatsScreen(
    counterViewModel: CounterViewModel,
    onBack: () -> Unit,
    onOpenHistory: () -> Unit
) {
    val state by counterViewModel.uiState.collectAsState()
    val counter = state.selected

    val context = androidx.compose.ui.platform.LocalContext.current
    val app = remember(context) { context.applicationContext as MyCounterApplication }
    val scope = rememberCoroutineScope()

    var granularity by remember { mutableStateOf(Granularity.DAILY) }
    var allTaps by remember(counter?.id, state.history) { mutableStateOf<List<TapEvent>>(emptyList()) }
    LaunchedEffect(counter?.id, state.history) {
        val id = counter?.id ?: return@LaunchedEffect
        allTaps = app.repository.allTaps(id)
    }

    // Stato del dialog "Report"
    var generatedReport by remember { mutableStateOf<File?>(null) }
    var generatingReport by remember { mutableStateOf(false) }
    var reportMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Statistiche${counter?.let { " · " + it.name } ?: ""}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = onOpenHistory) {
                        Icon(Icons.Rounded.History, contentDescription = "Storico")
                    }
                }
            )
        }
    ) { inner ->
        GradientBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(Modifier.height(8.dp))

                if (counter == null) {
                    Text(
                        "Nessun contatore selezionato.",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    return@Column
                }

                // Selettore granularità
                SectionCard {
                    Text("Granularità", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Granularity.HOURLY to "Orario (oggi)",
                            Granularity.DAILY to "Giornaliero",
                            Granularity.WEEKLY to "Settimanale",
                            Granularity.MONTHLY to "Mensile",
                            Granularity.YEARLY to "Annuale"
                        ).forEach { (g, label) ->
                            FilterChip(
                                selected = granularity == g,
                                onClick = { granularity = g },
                                label = { Text(label) }
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Suggerimento: nel grafico puoi fare pinch-to-zoom e " +
                                "trascinare orizzontalmente per esplorare i dati nel dettaglio.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                // KPI principali — adattato in base alla modalità del counter.
                SectionCard {
                    Text("Riepilogo", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))

                    if (counter.timeMode) {
                        // Conta Tempo: mostra tempi (HH:MM:SS), numero sessioni, tempo medio.
                        val sessions = allTaps.filter { it.durationMs != null }
                        val sessionsCount = sessions.size
                        val totalSec = counter.currentValue
                        val avgSec = if (sessionsCount > 0)
                            (sessions.sumOf { (it.durationMs ?: 0L) } / 1000L) / sessionsCount
                        else 0L
                        val longestSec = (sessions.maxOfOrNull { it.durationMs ?: 0L } ?: 0L) / 1000L

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Kpi("Tempo accumulato", totalSec.formatDuration())
                            Kpi("Sessioni", sessionsCount.toString())
                            if (counter.dailyTarget > 0) {
                                val pct = ((counter.currentValue.toDouble() / counter.dailyTarget) * 100)
                                    .coerceAtLeast(0.0).coerceAtMost(999.0)
                                Kpi("Obiettivo", "%.0f%%".format(pct))
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Kpi("Sessione media", avgSec.formatDuration())
                            Kpi("Sessione max", longestSec.formatDuration())
                            if (counter.dailyTarget > 0) {
                                Kpi("Target",
                                    (counter.dailyTarget).formatDuration())
                            }
                        }
                        // Tempo cronometrato OGGI (somma delle sessioni di oggi)
                        val todayStart = todayStartMs()
                        val todaySec = sessions
                            .filter { it.timestamp >= todayStart }
                            .sumOf { (it.durationMs ?: 0L) } / 1000L
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Tempo cronometrato oggi: ${todaySec.formatDuration()}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (counter.costPerTap > 0) {
                            Text(
                                "Totale costi periodo corrente: ${counter.totalCost().formatCurrency()} " +
                                        "(€/min ${counter.costPerTap.formatCurrency()})",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    } else {
                        // Counter classico (numerico)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Kpi("Valore", counter.currentValue.formatThousand())
                            Kpi("Tap totali", allTaps.size.toString())
                            if (counter.dailyTarget > 0) {
                                val pct = ((counter.currentValue.toDouble() / counter.dailyTarget) * 100)
                                    .coerceAtLeast(0.0).coerceAtMost(999.0)
                                Kpi("Obiettivo", "%.0f%%".format(pct))
                            }
                        }
                        if (counter.costPerTap > 0) {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Totale costi periodo corrente: ${counter.totalCost().formatCurrency()}",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }

                // Pulsante Report PDF
                PrimaryActionButton(
                    text = if (generatingReport) "Generazione in corso…" else "Report PDF",
                    enabled = !generatingReport,
                    onClick = {
                        generatingReport = true
                        scope.launch {
                            val file = withContext(Dispatchers.IO) {
                                val cons = app.repository.allConsolidations(counter.id)
                                ReportGenerator(context).generate(counter, allTaps, cons)
                            }
                            generatingReport = false
                            generatedReport = file
                        }
                    }
                )

                // Bar chart aggregato. Per Conta Tempo: bar = MINUTI cronometrati nel
                // bucket. Per counter classico: bar = numero di tap nel bucket.
                val aggregated = aggregate(allTaps, granularity, counter.timeMode)
                val unitLabel = if (counter.timeMode) "Minuti" else "Tap"
                SectionCard {
                    Text(
                        when (granularity) {
                            Granularity.HOURLY -> "$unitLabel per ora (oggi)"
                            Granularity.DAILY -> "$unitLabel per giorno"
                            Granularity.WEEKLY -> "$unitLabel per settimana"
                            Granularity.MONTHLY -> "$unitLabel per mese"
                            Granularity.YEARLY -> "$unitLabel per anno"
                        },
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    if (aggregated.isEmpty()) {
                        Text(
                            "Nessun dato per questa granularità.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    } else {
                        AndroidView(
                            modifier = Modifier.fillMaxWidth().height(260.dp),
                            factory = { ctx ->
                                BarChart(ctx).apply {
                                    description = Description().apply { text = "" }
                                    setDrawGridBackground(false)
                                    setNoDataText("Nessun dato")
                                    legend.isEnabled = false
                                    axisRight.isEnabled = false
                                    // Interazioni: pinch zoom + scroll orizzontale +
                                    // doppio tap per zoom rapido.
                                    setPinchZoom(true)
                                    setScaleEnabled(true)
                                    setDoubleTapToZoomEnabled(true)
                                    isDragEnabled = true
                                    isHighlightPerTapEnabled = true
                                }
                            },
                            update = { chart ->
                                val labels = aggregated.map { it.first }
                                val entries = aggregated.mapIndexed { i, p ->
                                    BarEntry(i.toFloat(), p.second.toFloat())
                                }
                                val ds = BarDataSet(entries, unitLabel).apply {
                                    color = counter.tapColorArgb
                                    valueTextSize = 10f
                                }
                                chart.data = BarData(ds).apply { barWidth = 0.7f }
                                chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
                                chart.xAxis.granularity = 1f
                                chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
                                chart.xAxis.labelRotationAngle = -45f
                                chart.xAxis.textColor = AndroidColor.GRAY
                                chart.axisLeft.textColor = AndroidColor.GRAY

                                // Visibilità iniziale "raccolta" così la pinch-zoom serva
                                // davvero per espandere: per HOURLY mostriamo tutte le 24
                                // ore, per le altre mostriamo le ultime ~10 barre e
                                // l'utente può scrollare/zoomare per le precedenti.
                                val maxVisible = if (granularity == Granularity.HOURLY) 24f else 10f
                                chart.setVisibleXRangeMaximum(maxVisible)
                                // Scrolla all'estremo destro (più recente) per default
                                chart.moveViewToX(entries.lastOrNull()?.x ?: 0f)
                                chart.invalidate()
                            }
                        )
                    }
                }

                // Line chart cumulato. In Conta Tempo l'asse Y è in MINUTI per
                // una lettura più naturale (anziché secondi).
                val cumulativeLabel = if (counter.timeMode)
                    "Tempo cumulato (minuti)" else "Valore counter"
                SectionCard {
                    Text("Andamento cumulato", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    if (allTaps.isEmpty()) {
                        Text(
                            "Effettua almeno un TAP per vedere l'andamento.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    } else {
                        AndroidView(
                            modifier = Modifier.fillMaxWidth().height(260.dp),
                            factory = { ctx ->
                                LineChart(ctx).apply {
                                    description = Description().apply { text = "" }
                                    setDrawGridBackground(false)
                                    setNoDataText("Nessun dato")
                                    axisRight.isEnabled = false
                                }
                            },
                            update = { chart ->
                                val sorted = allTaps.sortedBy { it.timestamp }
                                val entries = sorted.mapIndexed { idx, ev ->
                                    val y = if (counter.timeMode)
                                        ev.valueAfter / 60f       // secondi → minuti
                                    else
                                        ev.valueAfter.toFloat()
                                    Entry(idx.toFloat(), y)
                                }
                                val ds = LineDataSet(entries, cumulativeLabel).apply {
                                    color = counter.tapColorArgb
                                    setDrawCircles(false)
                                    lineWidth = 2.5f
                                    setDrawFilled(true)
                                    fillAlpha = 60
                                    fillColor = counter.tapColorArgb
                                }
                                chart.data = LineData(ds)
                                chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
                                chart.xAxis.textColor = AndroidColor.GRAY
                                chart.axisLeft.textColor = AndroidColor.GRAY
                                chart.invalidate()
                            }
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }

    // Dialog "Report PDF generato": offre Save (Downloads) e Condividi (sheet sistema).
    val report = generatedReport
    if (report != null) {
        AlertDialog(
            onDismissRequest = { generatedReport = null },
            icon = {
                Icon(
                    Icons.Rounded.PictureAsPdf,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text("Report PDF generato") },
            text = {
                Column {
                    Text(
                        "File: ${report.name}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Salvalo nella cartella Download del telefono oppure condividilo " +
                                "tramite WhatsApp, mail, Drive, ecc.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    reportMessage?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val ok = ReportFileSharer.saveToDownloads(context, report)
                    reportMessage = if (ok) "✓ Salvato in Download/${report.name}"
                    else "Errore nel salvataggio."
                }) {
                    Icon(Icons.Rounded.Save, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Salva")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        ReportFileSharer.share(context, report)
                    }) {
                        Icon(Icons.Rounded.Share, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Condividi")
                    }
                    TextButton(onClick = {
                        generatedReport = null
                        reportMessage = null
                    }) { Text("Chiudi") }
                }
            }
        )
    }
}

@Composable
private fun Kpi(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
        Text(value, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
    }
}

private enum class Granularity { HOURLY, DAILY, WEEKLY, MONTHLY, YEARLY }

/** Inizio (epoch ms) del giorno corrente, per filtrare i tap di "oggi". */
private fun todayStartMs(): Long {
    val cal = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
}

/**
 * Raggruppa i tap per ora/giorno/settimana/mese/anno e ritorna coppie
 * (label, valore).
 *
 * Il valore è:
 *  - in modalità Conta Tempo: MINUTI cronometrati nel bucket
 *    (somma delle durationMs / 60000).
 *  - in modalità classica: somma dei delta (cioè numero netto di tap, anche
 *    negativo per i reverse).
 *
 * Buckets:
 *  - HOURLY:  24 bar fisse "00".."23" relative al giorno di OGGI.
 *  - DAILY:   un bar per ogni giorno con dati, label "dd/MM".
 *  - WEEKLY:  settimane lun-dom, label "sett. dd/MM".
 *  - MONTHLY: label "MM/yy".
 *  - YEARLY:  label "yyyy".
 */
private fun aggregate(
    taps: List<TapEvent>,
    gran: Granularity,
    timeMode: Boolean = false
): List<Pair<String, Long>> {
    if (gran == Granularity.HOURLY) return aggregateHourlyToday(taps, timeMode)
    if (taps.isEmpty()) return emptyList()
    val fmt = when (gran) {
        Granularity.DAILY -> SimpleDateFormat("dd/MM", Locale.ITALY)
        Granularity.WEEKLY -> SimpleDateFormat("'sett.' dd/MM", Locale.ITALY)
        Granularity.MONTHLY -> SimpleDateFormat("MM/yy", Locale.ITALY)
        Granularity.YEARLY -> SimpleDateFormat("yyyy", Locale.ITALY)
        Granularity.HOURLY -> SimpleDateFormat("HH", Locale.ITALY) // unreachable
    }
    val cal = Calendar.getInstance().apply { firstDayOfWeek = Calendar.MONDAY }
    val map = LinkedHashMap<String, Long>()
    taps.sortedBy { it.timestamp }.forEach { ev ->
        cal.timeInMillis = ev.timestamp
        when (gran) {
            Granularity.DAILY -> {
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
            }
            Granularity.WEEKLY -> {
                cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
            }
            Granularity.MONTHLY -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
            }
            Granularity.YEARLY -> {
                cal.set(Calendar.DAY_OF_YEAR, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
            }
            Granularity.HOURLY -> { /* gestito in aggregateHourlyToday */ }
        }
        val key = fmt.format(Date(cal.timeInMillis))
        val contribution: Long = if (timeMode) {
            // Conta Tempo: minuti = durationMs / 60_000 (arrotondamento per difetto)
            (ev.durationMs ?: 0L) / 60_000L
        } else ev.delta.toLong()
        map[key] = (map[key] ?: 0L) + contribution
    }
    return map.entries.map { it.key to it.value }
}

/**
 * Aggrega i tap di OGGI per ora del giorno. Ritorna SEMPRE 24 entries (00..23)
 * anche con valore 0, così il chart mostra il pattern completo della giornata.
 *
 * In modalità Conta Tempo l'unità è MINUTI cronometrati per ora. Per le sessioni
 * che iniziano in un'ora e finiscono in un'altra, attribuiamo l'intera durata
 * all'ora di FINE per semplicità (in ottica esercizio "tempo sprecato",
 * la sessione compare quando si stoppa).
 */
private fun aggregateHourlyToday(
    taps: List<TapEvent>,
    timeMode: Boolean
): List<Pair<String, Long>> {
    val cal = Calendar.getInstance()
    val startOfDay = (cal.clone() as Calendar).apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val startOfTomorrow = startOfDay + 24L * 60 * 60 * 1000

    val buckets = LongArray(24)
    taps.forEach { ev ->
        if (ev.timestamp in startOfDay until startOfTomorrow) {
            cal.timeInMillis = ev.timestamp
            val h = cal.get(Calendar.HOUR_OF_DAY)
            buckets[h] += if (timeMode) (ev.durationMs ?: 0L) / 60_000L
            else ev.delta.toLong()
        }
    }
    return (0..23).map { h -> "%02d".format(h) to buckets[h] }
}
