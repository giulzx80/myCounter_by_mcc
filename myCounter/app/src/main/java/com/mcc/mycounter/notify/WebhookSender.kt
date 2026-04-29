package com.mcc.mycounter.notify

import android.util.Log
import com.mcc.mycounter.MyCounterApplication
import com.mcc.mycounter.data.entities.Achievement
import com.mcc.mycounter.data.entities.Counter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Step E — Webhook integrazione globale.
 *
 * Quando attivato dalle Impostazioni, dopo OGNI consolidamento
 * (manuale o automatico) l'app fa un `POST application/json` all'URL
 * configurato con un payload che descrive counter + esito + streak.
 *
 * Header opzionali:
 *  - `X-myCounter-Signature: hex(HMAC-SHA256(body, secret))` se l'utente
 *    ha impostato un secret (consente al server di verificare l'autenticità).
 *  - `User-Agent: myCounter-by-MCC/1.0`
 *
 * Best-effort: nessun retry, niente coda persistente. Se il server è offline
 * il payload è perso (lo storico e il consolidamento restano comunque
 * registrati nel DB locale, quindi il webhook è "informativo").
 *
 * Esempio di payload (formato stabile):
 * ```
 * {
 *   "event": "consolidation",
 *   "timestamp": 1719753600000,
 *   "counter": {
 *     "id": 1,
 *     "name": "Conta Caffè",
 *     "goalType": "LIMIT",         // TARGET / LIMIT / NONE
 *     "periodicity": "DAILY",       // DAILY / WEEKLY / MONTHLY / YEARLY
 *     "timeMode": false,
 *     "startValue": 0,
 *     "target": 3,
 *     "finalValue": 4,
 *     "totalCost": 4.80,
 *     "currency": "EUR"
 *   },
 *   "achievement": {
 *     "outcome": "FAILURE",         // SUCCESS / FAILURE / NEUTRAL
 *     "streak": 0,
 *     "consolidationId": 42,
 *     "periodEndedAt": 1719753600000
 *   }
 * }
 * ```
 */
object WebhookSender {

    private const val TAG = "WebhookSender"
    private const val UA = "myCounter-by-MCC/1.0"
    private const val TIMEOUT_MS = 5_000

    /**
     * Coroutine scope a livello processo: il webhook è "fire and forget" e
     * non deve essere legato al ciclo di vita di una specifica schermata.
     */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Se il webhook è abilitato nelle impostazioni e ha un URL valido, invia
     * il payload. Restituisce immediatamente: l'invio reale avviene in
     * background.
     */
    fun sendIfEnabled(app: MyCounterApplication, counter: Counter, achievement: Achievement) {
        scope.launch {
            val settings = runCatching {
                app.settingsManager.settingsFlow.first()
            }.getOrNull() ?: return@launch
            if (!settings.webhookEnabled) return@launch
            val url = settings.webhookUrl.trim()
            if (url.isEmpty() || !url.startsWith("http", ignoreCase = true)) return@launch

            val payload = buildPayload(counter, achievement).toString()
            val signature = settings.webhookSecret.takeIf { it.isNotEmpty() }
                ?.let { hmacSha256Hex(it, payload) }
            val ok = postJson(url, payload, signature)
            Log.d(TAG, "POST $url → ${if (ok) "OK" else "FAIL"}")
        }
    }

    /** Test manuale dalla schermata Impostazioni. Ritorna true se il server risponde 2xx. */
    suspend fun sendTestPing(url: String, secret: String): Boolean = withContext(Dispatchers.IO) {
        if (url.isBlank() || !url.startsWith("http", ignoreCase = true)) return@withContext false
        val payload = JSONObject().apply {
            put("event", "test")
            put("timestamp", System.currentTimeMillis())
            put("note", "Ping di test da myCounter Impostazioni")
        }.toString()
        val signature = secret.takeIf { it.isNotEmpty() }?.let { hmacSha256Hex(it, payload) }
        postJson(url, payload, signature)
    }

    private fun buildPayload(counter: Counter, achievement: Achievement): JSONObject {
        val counterJson = JSONObject().apply {
            put("id", counter.id)
            put("name", counter.name)
            // Tag logico opzionale: utile per il routing server-side
            // (Zapier/n8n/IFTTT possono filtrare/branchare in base a questo).
            counter.webhookTag?.takeIf { it.isNotBlank() }?.let { put("tag", it) }
            put("goalType", counter.goalType)
            put("periodicity", counter.periodicity)
            put("timeMode", counter.timeMode)
            put("startValue", counter.startValue)
            put("target", counter.dailyTarget)
            put("finalValue", achievement.finalValue)
            put("totalCost", counter.totalCost())
            put("currency", "EUR")
            // Tutti gli indirizzi accountability configurati per questo counter
            val emails = counter.accountabilityEmails()
            if (emails.isNotEmpty()) {
                put("accountabilityEmails", org.json.JSONArray(emails))
            }
        }
        val achievementJson = JSONObject().apply {
            put("outcome", achievement.outcome)
            put("streak", achievement.streak)
            put("consolidationId", achievement.consolidationId)
            put("periodEndedAt", achievement.periodEndedAt)
        }
        return JSONObject().apply {
            put("event", "consolidation")
            put("timestamp", System.currentTimeMillis())
            put("counter", counterJson)
            put("achievement", achievementJson)
        }
    }

    private fun postJson(url: String, body: String, signatureHex: String?): Boolean {
        val u = runCatching { URL(url) }.getOrNull() ?: return false
        val conn = (runCatching { u.openConnection() as HttpURLConnection }.getOrNull()) ?: return false
        return try {
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.connectTimeout = TIMEOUT_MS
            conn.readTimeout = TIMEOUT_MS
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            conn.setRequestProperty("User-Agent", UA)
            if (signatureHex != null) {
                conn.setRequestProperty("X-myCounter-Signature", signatureHex)
            }
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            code in 200..299
        } catch (e: Exception) {
            Log.w(TAG, "Errore webhook POST: ${e.message}")
            false
        } finally {
            runCatching { conn.disconnect() }
        }
    }

    /** HMAC-SHA256 in hex lowercase. */
    private fun hmacSha256Hex(key: String, message: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        val bytes = mac.doFinal(message.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
