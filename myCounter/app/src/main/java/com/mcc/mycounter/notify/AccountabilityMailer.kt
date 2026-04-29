package com.mcc.mycounter.notify

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.mcc.mycounter.MyCounterApplication
import com.mcc.mycounter.R
import com.mcc.mycounter.data.entities.Achievement
import com.mcc.mycounter.data.entities.Counter
import com.mcc.mycounter.data.entities.GoalType
import com.mcc.mycounter.report.ReportGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Step D — Accountability via email.
 *
 * Quando un counter ha [Counter.accountabilityEmail] valorizzata, dopo ogni
 * consolidamento prepariamo:
 *  1. Un PDF di riepilogo del periodo (riusa [ReportGenerator]).
 *  2. Una NOTIFICA con azione "Invia mail al coach": al tap apre il client
 *     mail di sistema con destinatario, oggetto, corpo e PDF allegato già
 *     pronti — basta premere "Invia".
 *
 * Niente backend, niente account: l'utente conferma l'invio.
 */
object AccountabilityMailer {

    private const val MAIL_NOTIF_BASE = 400_000

    /**
     * Se [counter.accountabilityEmail] è valorizzata, prepara il PDF in
     * background e mostra una notifica con tap → apertura client mail.
     */
    fun sendIfConfigured(context: Context, counter: Counter, achievement: Achievement) {
        val emails = counter.accountabilityEmails()
        if (emails.isEmpty()) return

        GlobalScope.launch(Dispatchers.IO) {
            val app = context.applicationContext as MyCounterApplication
            val taps = runCatching { app.repository.allTaps(counter.id) }.getOrDefault(emptyList())
            val cons = runCatching { app.repository.allConsolidations(counter.id) }.getOrDefault(emptyList())
            val pdf = runCatching { ReportGenerator(context).generate(counter, taps, cons) }.getOrNull()
                ?: return@launch
            withContext(Dispatchers.Main) {
                showMailNotification(context, counter, achievement, emails, pdf)
            }
        }
    }

    private fun showMailNotification(
        context: Context,
        counter: Counter,
        achievement: Achievement,
        toEmails: List<String>,
        pdf: java.io.File
    ) {
        NotificationHelper.ensureChannel(context)
        val authority = "${context.packageName}.fileprovider"
        val pdfUri: Uri = FileProvider.getUriForFile(context, authority, pdf)

        // NB: rinominato da `type` per non fare shadow al `Intent.type` dentro l'apply{}
        val goalKind = counter.goalTypeEnum()
        val outcomeLabel = when {
            achievement.outcomeEnum() == Achievement.Outcome.SUCCESS && goalKind == GoalType.LIMIT ->
                "limite rispettato"
            achievement.outcomeEnum() == Achievement.Outcome.SUCCESS && goalKind == GoalType.TARGET ->
                "obiettivo centrato"
            achievement.outcomeEnum() == Achievement.Outcome.FAILURE && goalKind == GoalType.LIMIT ->
                "limite superato"
            achievement.outcomeEnum() == Achievement.Outcome.FAILURE && goalKind == GoalType.TARGET ->
                "obiettivo mancato"
            else -> "riepilogo periodo"
        }

        val subject = "myCounter — ${counter.name}: $outcomeLabel"
        val body = buildString {
            append("Riepilogo del periodo per il counter \"${counter.name}\".\n\n")
            append("Esito: $outcomeLabel\n")
            append("Valore finale: ${achievement.finalValue} (target: ${achievement.targetValue})\n")
            if (achievement.streak > 1) append("Streak attuale: ${achievement.streak}\n")
            append("\nPDF dettagliato in allegato.\n\n— Inviato da myCounter by MCC")
        }

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_EMAIL, toEmails.toTypedArray())
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            putExtra(Intent.EXTRA_STREAM, pdfUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val recipientsLabel = when {
            toEmails.size == 1 -> toEmails.first()
            else -> "${toEmails.size} destinatari"
        }
        val chooser = Intent.createChooser(sendIntent, "Invia riepilogo a $recipientsLabel").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val notifId = MAIL_NOTIF_BASE + counter.id.toInt()
        val pi = PendingIntent.getActivity(
            context, notifId, chooser,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("📧 Riepilogo da inviare a $recipientsLabel")
            .setContentText("${counter.name}: $outcomeLabel — tap per inviare")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Periodo concluso. Tap su questa notifica per aprire la mail già " +
                            "pronta con il PDF allegato."
                )
            )
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        val mgr = ContextCompat.getSystemService(context, android.app.NotificationManager::class.java)
            ?: return
        runCatching { mgr.notify(notifId, n) }
    }
}
