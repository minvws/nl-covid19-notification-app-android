/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.util

import android.os.CountDownTimer
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class SimpleCountdownTimer(
    val countDownTo: LocalDateTime,
    countDownInterval: Long = 5000L,
    private val onTickAndFinish: () -> Unit
) : CountDownTimer(
    LocalDateTime.now().until(countDownTo, ChronoUnit.MILLIS),
    countDownInterval
) {
    private var isRunning = false
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
