package org.clockout.testing

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import org.clockout.AppContainer
import org.clockout.data.local.RitualPreferences
import org.clockout.data.local.db.ClockoutDatabase
import org.junit.rules.TemporaryFolder
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDate

/**
 * A container over an in-memory database and a DataStore of its own, so tests never share the
 * process-wide store. Today is read once: a context-driven date flow under runTest would never end.
 */
fun testAppContainer(
    folder: TemporaryFolder,
    soundFileExists: (String) -> Boolean = { true },
    context: Context = ApplicationProvider.getApplicationContext()
): AppContainer = AppContainer(
    context = context,
    preferences = RitualPreferences(
        PreferenceDataStoreFactory.create {
            folder.newFile("test.preferences_pb").apply { delete() }
        }
    ),
    database = Room.inMemoryDatabaseBuilder(context, ClockoutDatabase::class.java).build(),
    todayFlow = flowOf(LocalDate.now()),
    soundFileExists = soundFileExists
)
