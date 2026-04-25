package com.mcc.mycounter.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.mcc.mycounter.MainActivity
import com.mcc.mycounter.MyCounterApplication
import com.mcc.mycounter.R
import kotlinx.coroutines.flow.first

/**
 * Widget Glance collegato a UN singolo contatore (binding per-widget).
 *
 * Layout (top → bottom, centrato):
 *   1. Nome contatore in una "pill" colorata
 *   2. Pulsante TAP (Box+clickable) che mostra il VALORE corrente
 *   3. Icona ingranaggio per aprire le Statistiche
 *
 * Sfondo: trasparente. Più widget possono coesistere ognuno con counter diversi.
 *
 * IMPORTANTE: dopo qualsiasi modifica al CounterWidget, **rimuovere e
 * riaggiungere il widget dalla home**. Android caches le RemoteViews esistenti
 * del widget per istanza: senza la rimozione + ri-pinning il vecchio widget
 * potrebbe non riprendere la nuova logica di click.
 */
class CounterWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as MyCounterApplication

        val appWidgetId = try {
            GlanceAppWidgetManager(context).getAppWidgetId(id)
        } catch (_: Throwable) { -1 }

        val boundCounterId = if (appWidgetId > 0) WidgetBindings.getBinding(context, appWidgetId) else null
        val fallbackId = app.settingsManager.settingsFlow.first().selectedCounterId
        val counterId = boundCounterId ?: fallbackId

        val counter = if (counterId > 0) app.repository.getById(counterId) else null
        updateAppWidgetState(context, id) { prefs ->
            prefs[Keys.COUNTER_ID] = counter?.id ?: 0L
            prefs[Keys.COUNTER_NAME] = counter?.name ?: "myCounter"
            prefs[Keys.COUNTER_VALUE] = counter?.currentValue ?: 0L
            prefs[Keys.COUNTER_STEP] = counter?.step ?: 1
            prefs[Keys.COUNTER_REVERSE] = counter?.reverse ?: false
            prefs[Keys.COUNTER_COLOR] = counter?.tapColorArgb ?: 0xFF6750A4.toInt()
            prefs[Keys.COUNTER_TIME_MODE] = counter?.timeMode ?: false
            prefs[Keys.COUNTER_RUNNING] = counter?.runningStartedAt != null
            // Default 0 = nessun flash della sessione appena conclusa.
            // Il flash viene impostato dalla TapWidgetAction quando si stoppa il timer.
            if (prefs[Keys.LAST_SESSION_FLASH_MS] == null) {
                prefs[Keys.LAST_SESSION_FLASH_MS] = 0L
            }
            // Default true = icona stop "piena". Il blink la toggla a intervalli.
            if (prefs[Keys.BLINK_ON] == null) {
                prefs[Keys.BLINK_ON] = true
            }
        }

        provideContent {
            GlanceTheme {
                val ctxName = currentState(Keys.COUNTER_NAME) ?: "myCounter"
                val ctxValue = currentState(Keys.COUNTER_VALUE) ?: 0L
                val ctxStep = currentState(Keys.COUNTER_STEP) ?: 1
                val ctxReverse = currentState(Keys.COUNTER_REVERSE) ?: false
                val ctxColor = currentState(Keys.COUNTER_COLOR) ?: 0xFF6750A4.toInt()
                val ctxId = currentState(Keys.COUNTER_ID) ?: 0L
                val ctxTimeMode = currentState(Keys.COUNTER_TIME_MODE) ?: false
                val ctxRunning = currentState(Keys.COUNTER_RUNNING) ?: false
                val ctxFlashMs = currentState(Keys.LAST_SESSION_FLASH_MS) ?: 0L
                val ctxBlinkOn = currentState(Keys.BLINK_ON) ?: true

                CounterWidgetContent(
                    context = context,
                    counterId = ctxId,
                    counterName = ctxName,
                    counterValue = ctxValue,
                    counterStep = ctxStep,
                    counterReverse = ctxReverse,
                    counterColorArgb = ctxColor,
                    timeMode = ctxTimeMode,
                    timerRunning = ctxRunning,
                    lastSessionFlashMs = ctxFlashMs,
                    blinkOn = ctxBlinkOn
                )
            }
        }
    }

    @Composable
    private fun CounterWidgetContent(
        context: Context,
        counterId: Long,
        counterName: String,
        counterValue: Long,
        counterStep: Int,
        counterReverse: Boolean,
        counterColorArgb: Int,
        timeMode: Boolean,
        timerRunning: Boolean,
        lastSessionFlashMs: Long,
        blinkOn: Boolean
    ) {
        val tapColor = androidx.compose.ui.graphics.Color(counterColorArgb)
        val white = androidx.compose.ui.graphics.Color.White

        // Deep-link Stats (back → home telefono via fromWidget=1).
        val openStatsIntent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("mycounter://counter/$counterId?screen=stats&fromWidget=1")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            setClass(context.applicationContext, MainActivity::class.java)
        }

        // Action: ricalcolata ad ogni recomposition con il counterId corrente.
        val tapAction = actionRunCallback<TapWidgetAction>(
            actionParametersOf(CounterIdKey to counterId)
        )

        // Sfondo trasparente: NIENTE .background sul Column root.
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Pill con il nome del contatore.
            Text(
                text = counterName,
                modifier = GlanceModifier
                    .background(ColorProvider(day = tapColor, night = tapColor))
                    .cornerRadius(12.dp)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = ColorProvider(day = white, night = white)
                ),
                maxLines = 1
            )
            Spacer(modifier = GlanceModifier.height(8.dp))

            // 2. Pulsante TAP: Box clickable + contenuto centrale.
            //    - Counter classico: Text col valore numerico.
            //    - Counter Conta Tempo: icona Play (timer fermo) o Stop (timer in esecuzione).
            //    Quando il timer è in esecuzione, il Box "pulsa" alternando tra il
            //    colore pieno e una variante più chiara.
            //    Ordine modifier: size → clickable → cornerRadius → background.
            val pulseColor = if (timerRunning && !blinkOn) {
                // Variante MOLTO più chiara del colore principale: differenza
                // visibile a colpo d'occhio durante la pulsazione.
                androidx.compose.ui.graphics.Color(
                    red = (tapColor.red + (1f - tapColor.red) * 0.75f).coerceIn(0f, 1f),
                    green = (tapColor.green + (1f - tapColor.green) * 0.75f).coerceIn(0f, 1f),
                    blue = (tapColor.blue + (1f - tapColor.blue) * 0.75f).coerceIn(0f, 1f),
                    alpha = 1f
                )
            } else tapColor
            Box(
                modifier = GlanceModifier
                    .size(80.dp)
                    .clickable(tapAction)
                    .cornerRadius(40.dp)
                    .background(ColorProvider(day = pulseColor, night = pulseColor)),
                contentAlignment = Alignment.Center
            ) {
                if (timeMode) {
                    when {
                        // Flash post-stop: per ~3 secondi mostra la durata della
                        // sessione appena conclusa al posto del Play.
                        !timerRunning && lastSessionFlashMs > 0 -> {
                            val totalSec = lastSessionFlashMs / 1000L
                            val hours = totalSec / 3600
                            val minutes = (totalSec % 3600) / 60
                            val seconds = totalSec % 60
                            val label = if (hours > 0)
                                "%d:%02d:%02d".format(hours, minutes, seconds)
                            else
                                "%02d:%02d".format(minutes, seconds)
                            Text(
                                text = label,
                                style = TextStyle(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = ColorProvider(day = white, night = white)
                                ),
                                maxLines = 1
                            )
                        }
                        else -> {
                            // Quando il timer è running, alterniamo due drawable per
                            // l'icona STOP (pieno / semi-trasparente). In combinazione
                            // con la pulsazione del background, l'effetto "alive" è
                            // chiaramente percepibile.
                            val iconRes = when {
                                !timerRunning -> R.drawable.ic_widget_play
                                blinkOn -> R.drawable.ic_widget_stop
                                else -> R.drawable.ic_widget_stop_dim
                            }
                            Image(
                                provider = ImageProvider(iconRes),
                                contentDescription = if (timerRunning) "Stop timer" else "Avvia timer",
                                modifier = GlanceModifier.size(40.dp)
                            )
                        }
                    }
                } else {
                    Text(
                        text = counterValue.toString(),
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = ColorProvider(day = white, night = white)
                        ),
                        maxLines = 1
                    )
                }
            }
            Spacer(modifier = GlanceModifier.height(6.dp))

            // 3. Icona ingranaggio → apre Statistiche.
            Image(
                provider = ImageProvider(R.drawable.ic_widget_stats),
                contentDescription = "Apri Statistiche",
                modifier = GlanceModifier
                    .size(28.dp)
                    .clickable(actionStartActivity(openStatsIntent))
            )
        }
    }

    object Keys {
        val COUNTER_ID = longPreferencesKey("w_counter_id")
        val COUNTER_NAME = stringPreferencesKey("w_counter_name")
        val COUNTER_VALUE = longPreferencesKey("w_counter_value")
        val COUNTER_STEP = intPreferencesKey("w_counter_step")
        val COUNTER_REVERSE = booleanPreferencesKey("w_counter_reverse")
        val COUNTER_COLOR = intPreferencesKey("w_counter_color")
        // Per la modalità Conta Tempo: il widget mostra Play/Stop al posto del numero.
        val COUNTER_TIME_MODE = booleanPreferencesKey("w_counter_time_mode")
        val COUNTER_RUNNING = booleanPreferencesKey("w_counter_running")
        /**
         * Quando il timer viene appena STOPPATO (nel widget), per ~3 secondi
         * mostriamo qui la durata della sessione conclusa (in millisecondi);
         * dopo torna 0 e il widget rimostra l'icona Play.
         */
        val LAST_SESSION_FLASH_MS = longPreferencesKey("w_last_session_flash_ms")
        /**
         * Toggle del "blink" dell'icona STOP mentre il timer è in esecuzione.
         * Aggiornato periodicamente da [WidgetBlinker].
         */
        val BLINK_ON = booleanPreferencesKey("w_blink_on")
    }

    companion object {
        val CounterIdKey: ActionParameters.Key<Long> = ActionParameters.Key("counterId")
    }
}
