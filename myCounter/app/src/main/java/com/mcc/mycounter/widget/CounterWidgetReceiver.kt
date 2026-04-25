package com.mcc.mycounter.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * AppWidget receiver standard di Glance. Una sola istanza dichiarata nel manifest
 * con metadata @xml/counter_widget_info. Quando il widget viene rimosso, ripuliamo
 * il binding (widgetId → counterId) salvato in DataStore.
 */
class CounterWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CounterWidget()

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        CoroutineScope(Dispatchers.IO).launch {
            WidgetBindings.removeBindings(context, appWidgetIds)
        }
    }
}
