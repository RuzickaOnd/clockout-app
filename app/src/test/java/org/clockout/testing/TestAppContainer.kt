package org.clockout.testing

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.clockout.AppContainer
import org.clockout.DefaultAppContainer
import org.clockout.data.local.RitualPreferences
import org.clockout.data.local.db.ClockoutDatabase
import org.junit.rules.TemporaryFolder
import java.time.LocalDate

/**
 * The production graph over stores a test owns. Production code reaches DataStore and Room only
 * through the repositories; a test also needs the stores themselves - to seed them, to read them
 * back and to close the database - so this keeps its own references to them. Today is read once:
 * a context-driven date flow under runTest would never end.
 */
class TestAppContainer(
    context: Context = ApplicationProvider.getApplicationContext(),
    val preferences: RitualPreferences,
    val database: ClockoutDatabase,
    todayFlow: Flow<LocalDate> = flowOf(LocalDate.now()),
    soundFileExists: (String) -> Boolean = { true }
) : AppContainer by DefaultAppContainer(context, preferences, database, todayFlow, soundFileExists)

/** A container over an in-memory database and a DataStore of its own, so tests never share the process-wide store. */
fun testAppContainer(
    folder: TemporaryFolder,
    soundFileExists: (String) -> Boolean = { true },
    context: Context = ApplicationProvider.getApplicationContext()
): TestAppContainer = TestAppContainer(
    context = context,
    preferences = RitualPreferences(
        PreferenceDataStoreFactory.create {
            folder.newFile("test.preferences_pb").apply { delete() }
        }
    ),
    database = Room.inMemoryDatabaseBuilder(context, ClockoutDatabase::class.java).build(),
    soundFileExists = soundFileExists
)
