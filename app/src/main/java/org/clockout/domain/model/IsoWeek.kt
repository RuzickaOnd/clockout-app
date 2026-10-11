package org.clockout.domain.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * The Monday that starts this date's week. Weeks run Monday to Sunday everywhere in the app - the
 * path's rhythm, the sky's constellations and the completion card all key a week by this date, so
 * they must share one definition.
 */
fun LocalDate.mondayOfWeek(): LocalDate = with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
