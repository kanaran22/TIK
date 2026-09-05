package com.geotask.app.data

import androidx.room.migration.Migration

/**
 * Every schema change from version 3 onward needs a migration here, or the app will
 * crash on launch for anyone with an older database (which is the point — a silent
 * wipe of someone's tasks and streak history is worse than a loud failure).
 *
 * How to add one:
 *  1. Change the entity, bump `version` in [TaskDatabase].
 *  2. Build once — Room writes `app/schemas/<version>.json`. Diff it against the
 *     previous version's file to see exactly what SQL you need.
 *  3. Add a `Migration(from, to)` below and include it in [ALL_MIGRATIONS].
 *
 * Adding a nullable column is usually a one-liner, e.g.:
 *
 *     private val MIGRATION_3_4 = Migration(3, 4) { db ->
 *         db.execSQL("ALTER TABLE tasks ADD COLUMN priority INTEGER")
 *     }
 *
 * Anything that drops or retypes a column needs the create-copy-drop-rename dance,
 * because SQLite can't alter a column in place.
 */
val ALL_MIGRATIONS: Array<Migration> = arrayOf()
