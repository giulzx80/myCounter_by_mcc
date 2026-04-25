package com.mcc.mycounter.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Helper per forzare l'update di tutte le istanze del widget.
 * Chiamato dopo ogni TAP nella TapScreen e dal repository.
 */
object WidgetUpdater {
    fun requestUpdateAll(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                CounterWidget().updateAll(context)
            } catch (_: Throwable) {
                // Se non c'è nessun widget piazzato, ignora.
            }
        }
    }

    suspend fun listWidgetIds(context: Context): List<Int> {
        val mgr = GlanceAppWidgetManager(context)
        return mgr.getGlanceIds(CounterWidget::class.java).map { id ->
            mgr.getAppWidgetId(id)
        }
    }
}
