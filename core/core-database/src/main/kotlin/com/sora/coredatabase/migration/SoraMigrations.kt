package com.sora.coredatabase.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Schema migrations.
 *
 * Destructive fallback is deliberately NOT enabled: a user's library, match
 * confirmations and read positions represent real manual effort, and wiping
 * them on a schema change would be a data-loss bug. Every version bump needs
 * a migration here plus a test in SoraMigrationTest.
 */
object SoraMigrations {

    /**
     * 1 -> 2: adds `library_entries.lastConfirmedChapter`.
     *
     * Stores the chapter number the user confirmed when completing a VOLUME
     * unit, so the next volume's completion dialog can pre-fill an estimate
     * rather than asking cold (brief: "persist whatever chapter number the
     * user confirms so future syncs for the same series have a reference
     * point").
     *
     * Nullable with no default: existing rows legitimately have no confirmed
     * chapter yet, and null distinguishes that from a real value of 0.
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE `library_entries` ADD COLUMN `lastConfirmedChapter` REAL",
            )
        }
    }

    /** Every migration, in order, for Room's builder. */
    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2)
}
