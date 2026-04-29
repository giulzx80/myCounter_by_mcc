package com.mcc.mycounter.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mcc.mycounter.data.dao.AchievementDao
import com.mcc.mycounter.data.dao.ConsolidationDao
import com.mcc.mycounter.data.dao.CounterDao
import com.mcc.mycounter.data.dao.TapEventDao
import com.mcc.mycounter.data.entities.Achievement
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
 *  - v3: tipo di obiettivo + accountability + tabella achievements:
 *        counters.goalType (TARGET/LIMIT/NONE)
 *        counters.accountabilityEmail
 *        nuova tabella achievements
 *  - v4: tag webhook per-counter (counters.webhookTag, opzionale)
 */
@Database(
    entities = [Counter::class, TapEvent::class, Consolidation::class, Achievement::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun counterDao(): CounterDao
    abstract fun tapEventDao(): TapEventDao
    abstract fun consolidationDao(): ConsolidationDao
    abstract fun achievementDao(): AchievementDao

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

        /**
         * v2 → v3: tipo di obiettivo, accountability, tabella achievements.
         * I counter esistenti diventano automaticamente di tipo TARGET (= comportamento
         * pre-esistente, "raggiungi l'obiettivo").
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE counters ADD COLUMN goalType TEXT NOT NULL DEFAULT 'TARGET'")
                db.execSQL("ALTER TABLE counters ADD COLUMN accountabilityEmail TEXT")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `achievements` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `counterId` INTEGER NOT NULL,
                        `consolidationId` INTEGER NOT NULL,
                        `periodEndedAt` INTEGER NOT NULL,
                        `outcome` TEXT NOT NULL,
                        `goalType` TEXT NOT NULL,
                        `finalValue` INTEGER NOT NULL,
                        `targetValue` INTEGER NOT NULL,
                        `streak` INTEGER NOT NULL DEFAULT 0,
                        `createdAt` INTEGER NOT NULL,
                        FOREIGN KEY(`counterId`) REFERENCES `counters`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`consolidationId`) REFERENCES `consolidations`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_achievements_counterId` ON `achievements` (`counterId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_achievements_periodEndedAt` ON `achievements` (`periodEndedAt`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_achievements_consolidationId` ON `achievements` (`consolidationId`)")
            }
        }

        /**
         * v3 → v4: aggiunge `webhookTag` ai counter (opzionale) per il routing
         * server-side dei payload del webhook globale.
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE counters ADD COLUMN webhookTag TEXT")
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    // In dev, se non c'è una migration valida ricostruiamo il DB
                    // invece di crashare.
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
