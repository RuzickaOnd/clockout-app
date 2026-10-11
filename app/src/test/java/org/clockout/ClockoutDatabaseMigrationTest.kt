package org.clockout

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.clockout.data.local.db.ClockoutDatabase
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ClockoutDatabaseMigrationTest {

    private val databaseName = "migration-test.db"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        ClockoutDatabase::class.java
    )

    @Test
    fun `migration 1 to 2 seeds practice days from the ritual history`() {
        helper.createDatabase(databaseName, 1).apply {
            execSQL(
                "INSERT INTO ritual_logs(date, question_text, selected_answer, mood_tag, timestamp) " +
                    "VALUES ('2026-10-05', 'Q', NULL, 'CALM', 1), ('2026-10-06', 'Q', 'A', 'TENSE', 2)"
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(databaseName, 2, true, ClockoutDatabase.MIGRATION_1_2)

        val dates = migrated.query("SELECT date FROM practice_days ORDER BY date").use { cursor ->
            buildList { while (cursor.moveToNext()) add(cursor.getString(0)) }
        }
        assertEquals(listOf("2026-10-05", "2026-10-06"), dates)
        val logCount = migrated.query("SELECT COUNT(*) FROM ritual_logs").use { it.moveToFirst(); it.getInt(0) }
        assertEquals(2, logCount)
    }
}
