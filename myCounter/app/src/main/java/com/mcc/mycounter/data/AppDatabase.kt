package com.mcc.mycounter.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mcc.mycounter.data.dao.ConsolidationDao
import com.mcc.mycounter.data.dao.CounterDao
import com.mcc.mycounter.data.dao.TapEventDao
import com.mcc.mycounter.data.entities.Consolidation
import com.mcc.mycounter.data.entities.Counter
import com.mcc.mycounter.data.entities.TapEvent

/**
 * Database Room dell'app. Singleton ottenuto via [getInstance].
 *
 * Versione schema:
 *  - v1: counters, tap_events, consolidations
 *  - v2: aggiunti campi per la modalità "Conta Tempo":
 *        counters.timeMode, counters.runningStartedAt
 *        tap_events.durationMs, tap_events.sessionStartedAt
 */
@Database(
    entities = [Counter::class, TapEvent::class, Consolidation::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun counterDao(): CounterDao
    abstract fun tapEventDao(): TapEventDao
    abstract fun consolidationDao(): ConsolidationDao

    companion object {
        private const val DB_NAME = "mycounter.db"

        /** v1 → v2: aggiunge i campi per la modalità Conta Tempo. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE counters ADD COLUMN timeMode INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE counters ADD COLUMN runningStartedAt INTEGER")
                db.execSQL("ALTER TABLE tap_events ADD COLUMN durationMs INTEGER")
                db.execSQL("ALTER TABLE tap_events ADD COLUMN sessionStartedAt INTEGER")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME
                )
                    .addMigrations(MIGRATION_1_2)
                    // In dev, se non c'è una migration valida ricostruiamo il DB
                    // invece di crashare.
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
