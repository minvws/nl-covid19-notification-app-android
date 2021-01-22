/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.util

import android.os.CountDownTimer
import nl.rijksoverheid.en.settings.Settings
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class PausedStateTimer(
    pausedState: Settings.PausedState.Paused,
    private val onTickAndFinish: () -> Unit
) : CountDownTimer(
    LocalDateTime.now().until(pausedState.pausedUntil, ChronoUnit.MILLIS),
    5000
) {
    var isRunning = false
    fun startTimer() {
        if (!isRunning) {
            start()
            isRunning = true
        }
    }
    fun cancelTimer() {
        cancel()
        isRunning = false
    }

    override fun onTick(p0: Long) {
        onTickAndFinish.invoke()
    }
    override fun onFinish() {
        onTickAndFinish.invoke()
    }
}
