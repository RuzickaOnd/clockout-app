package org.clockout.data.repository

import java.time.LocalDate

/**
 * Rows seeded from ritual_logs carry whatever digits the system locale wrote them in - Persian
 * or Arabic-Indic ones included - which ISO parsing rejects, so they are read back as ASCII.
 * A row that still does not parse is skipped rather than taking the home screen down with it.
 */
internal fun parsePracticeDay(date: String): LocalDate? {
    val ascii = date.map { char -> Character.digit(char, 10).takeIf { it >= 0 }?.digitToChar() ?: char }
        .joinToString("")
    return runCatching { LocalDate.parse(ascii) }.getOrNull()
}
