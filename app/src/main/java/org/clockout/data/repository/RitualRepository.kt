package org.clockout.data.repository

import org.clockout.domain.repository.ProgressRepository
import org.clockout.domain.repository.RitualLogRepository
import org.clockout.domain.repository.SettingsRepository
import org.clockout.domain.repository.SoundRepository

/** Transitional: the one implementation still serves every narrow repository. */
interface RitualRepository : SettingsRepository, SoundRepository, RitualLogRepository, ProgressRepository
