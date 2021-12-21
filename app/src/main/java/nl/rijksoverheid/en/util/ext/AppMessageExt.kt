/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.util.ext

import nl.rijksoverheid.en.api.model.AppMessage
import java.util.Random

fun AppMessage.shouldScheduleBasedOnProbability(): Boolean {
    return Random().nextFloat() < probability
}
