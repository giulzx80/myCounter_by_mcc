package com.mcc.mycounter.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.mcc.mycounter.MainActivity
import com.mcc.mycounter.R
import com.mcc.mycounter.data.entities.Achievement
import com.mcc.mycounter.data.entities.Counter
import com.mcc.mycounter.data.entities.GoalType

/**
 * Helper centrale per le notifiche dell'app:
 *  - canale unico "myCounter alerts"
 *  - notifica HOT ZONE (avviso o incoraggiamento dopo TAP)
 *  - notifica ESITO CONSOLIDAMENTO (premio o avviso a fine periodo)
 *  - notifica REMINDER GIORNALIERO (programmata via WorkManager)
 *
 * Nota: su Android 13+ serve il permesso POST_NOTIFICATIONS, gestito a runtime
 * dalla MainActivity al primo avvio.
 */
object NotificationHelper {

    const val CHANNEL_ID = "mycounter_alerts"
    private const val CHANNEL_NAME = "myCounter — avvisi"
    private const val CHANNEL_DESC = "Hot zone, esito periodo, reminder giornalieri."

    /** ID base; per ogni counter usiamo CHANNEL_ID_BASE + counter.id per non sovrapporre. */
    private const val NOTIF_ID_HOTZONE_BASE = 100_000
    private const val NOTIF_ID_OUTCOME_BASE = 200_000
    private const val NOTIF_ID_REMINDER_BASE = 300_000

    /** Crea il canale di notifica una volta sola. Idempotente. */
    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = ContextCompat.getSystemService(context, NotificationManager::class.java) ?: return
            if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = CHANNEL_DESC
                }
                mgr.createNotificationChannel(channel)
            }
        }
    }

    /**
     * Notifica "hot zone" inviata dopo un TAP che fa entrare il counter
     * nella zona 80%-99% del proprio target.
     */
    fun showHotZone(context: Context, counter: Counter) {
        ensureChannel(context)
        val type = counter.goalTypeEnum()
        val (title, body) = when (type) {
            GoalType.LIMIT -> "${counter.name}: stai per superare il limite" to
                    "Sei a ${counter.currentValue}/${counter.dailyTarget}. Resisti!"
            GoalType.TARGET -> "${counter.name}: ci sei quasi!" to
                    "${counter.currentValue}/${counter.dailyTarget} — ancora ${counter.dailyTarget - counter.currentValue}"
            GoalType.NONE -> return
        }
        notify(context, NOTIF_ID_HOTZONE_BASE + counter.id.toInt(), title, body, counter.id)
    }

    /**
     * Notifica esito periodo: chiamata subito dopo il consolidamento (manuale
     * o automatico). Mostra premio o avviso secondo l'achievement appena creato.
     */
    fun showOutcome(context: Context, counter: Counter, achievement: Achievement) {
        ensureChannel(context)
        val type = counter.goalTypeEnum()
        val outcome = achievement.outcomeEnum()
        val (title, body) = when {
            outcome == Achievement.Outcome.NEUTRAL -> return  // niente da segnalare
            type == GoalType.LIMIT && outcome == Achievement.Outcome.SUCCESS ->
                "🏆 ${counter.name}: limite rispettato" to
                        "Periodo concluso a ${achievement.finalValue}/${achievement.targetValue}." +
                        (if (achievement.streak > 1) " Streak: ${achievement.streak} di fila!" else "")
            type == GoalType.LIMIT && outcome == Achievement.Outcome.FAILURE ->
                "⚠️ ${counter.name}: limite superato" to
                        "Hai sforato di ${achievement.finalValue - achievement.targetValue}. Riproviamo!"
            type == GoalType.TARGET && outcome == Achievement.Outcome.SUCCESS ->
                "🏆 ${counter.name}: obiettivo centrato" to
                        "${achievement.finalValue}/${achievement.targetValue} raggiunto!" +
                        (if (achievement.streak > 1) " Streak: ${achievement.streak} di fila!" else "")
            type == GoalType.TARGET && outcome == Achievement.Outcome.FAILURE ->
                "💪 ${counter.name}: obiettivo mancato per poco" to
                        "Mancato per ${achievement.targetValue - achievement.finalValue}. Domani si riparte."
            else -> return
        }
        notify(context, NOTIF_ID_OUTCOME_BASE + counter.id.toInt(), title, body, counter.id)
    }

    /** Reminder giornaliero (via WorkManager). */
    fun showReminder(context: Context, counter: Counter) {
        ensureChannel(context)
        val type = counter.goalTypeEnum()
        val (title, body) = when (type) {
            GoalType.TARGET -> {
                if (counter.currentValue >= counter.dailyTarget) return
                "${counter.name}: non mollare" to
                        "Mancano ${counter.dailyTarget - counter.currentValue} per il tuo obiettivo di oggi"
            }
            GoalType.LIMIT -> {
                if (counter.currentValue > counter.dailyTarget) return
                "${counter.name}: tutto sotto controllo" to
                        "Sei a ${counter.currentValue}/${counter.dailyTarget}. Resisti fino a mezzanotte!"
            }
            GoalType.NONE -> return
        }
        notify(context, NOTIF_ID_REMINDER_BASE + counter.id.toInt(), title, body, counter.id)
    }

    /** Costruzione + post della notifica con tap → apre l'app sulla TapScreen del counter. */
    private fun notify(context: Context, id: Int, title: String, body: String, counterId: Long) {
        val openIntent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("mycounter://counter/$counterId?screen=tap")
            setClass(context.applicationContext, MainActivity::class.java)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            context, id, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        val mgr = ContextCompat.getSystemService(context, NotificationManager::class.java) ?: return
        runCatching { mgr.notify(id, n) }
    }
}
