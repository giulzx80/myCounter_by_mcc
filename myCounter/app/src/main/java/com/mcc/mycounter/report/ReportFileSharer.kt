package com.mcc.mycounter.report

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File

/**
 * Utility per salvare un PDF nei Download del telefono e per condividerlo
 * via il sistema di sharing di Android.
 *
 * - Save: usa MediaStore (Android 10+) → nessun permesso runtime richiesto.
 *   Su versioni precedenti scrive direttamente in Downloads (nostra minSdk = 26
 *   richiede WRITE_EXTERNAL_STORAGE su API 28-, ma di norma il device usato è 29+).
 *
 * - Share: passa il file al sistema via FileProvider con permesso temporaneo
 *   di lettura. L'utente vede il "share sheet" del telefono e sceglie l'app
 *   (WhatsApp, Drive, Mail, ecc.).
 */
object ReportFileSharer {

    /**
     * Copia il [file] PDF nella cartella Downloads del telefono.
     * Restituisce true in caso di successo.
     */
    fun saveToDownloads(context: Context, file: File): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: return false
                resolver.openOutputStream(uri)?.use { out ->
                    file.inputStream().use { it.copyTo(out) }
                } ?: return false
                true
            } else {
                @Suppress("DEPRECATION")
                val downloads = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS
                )
                if (!downloads.exists()) downloads.mkdirs()
                val target = File(downloads, file.name)
                file.copyTo(target, overwrite = true)
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Apre il sistema di condivisione di Android con il PDF allegato.
     */
    fun share(context: Context, file: File) {
        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Report myCounter")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(sendIntent, "Condividi report").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
