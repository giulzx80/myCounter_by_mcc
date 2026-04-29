package com.mcc.mycounter.notify

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.mcc.mycounter.MyCounterApplication
import com.mcc.mycounter.data.entities.GoalType
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Worker che gira UNA volta al giorno (programmato a ~22:00) e per ogni
 * counter con obiettivo TARGET/LIMIT mostra una notifica di reminder.
 *
 * Si registra una sola volta in [MyCounterApplication.onCreate] (idempotente:
 * KEEP non sostituisce un work già esistente).
 */
class DailyReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as MyCounterApplication
        val counters = runCatching { app.repository.getAll() }.getOrNull().orEmpty()
        counters.forEach { c ->
            val type = c.goalTypeEnum()
            if (type != GoalType.NONE && c.dailyTarget > 0) {
                NotificationHelper.showReminder(applicationContext, c)
            }
        }
        return Result.success()
    }

    companion object {
        private const val UNIQUE_NAME = "mycounter_daily_reminder"
        /** Ora di scatto del reminder giornaliero (22:00). */
        private const val HOUR_OF_DAY = 22

        /** Programma il worker in modo idempotente. */
        fun schedule(context: Context) {
            val initialDelay = computeInitialDelayMillis(HOUR_OF_DAY)
            val request = PeriodicWorkRequestBuilder<DailyReminderWorker>(
                1, TimeUnit.DAYS
            )
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        /** Cancella il reminder. */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_NAME)
        }

        /** Millisecondi da ora fino al prossimo orario [hour]:00 nel giorno corrente o seguente. */
        private fun computeInitialDelayMillis(hour: Int): Long {
            val now = System.currentTimeMillis()
            val cal = Calendar.getInstance().apply {
                timeInMillis = now
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (cal.timeInMillis <= now) {
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
            return cal.timeInMillis - now
        }
    }
}
