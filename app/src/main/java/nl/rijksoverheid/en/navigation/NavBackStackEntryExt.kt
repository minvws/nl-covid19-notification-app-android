/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
@file:Suppress("ktlint:filename")

package nl.rijksoverheid.en.navigation

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavBackStackEntry

fun <T> NavBackStackEntry.getBackStackEntryObserver(
    key: String,
    onStateChanged: (T) -> Unit
): LifecycleEventObserver {
    return LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_RESUME && savedStateHandle.contains(key)) {
            savedStateHandle.get<T>(key)?.let { onStateChanged(it) }
            savedStateHandle.remove<T>(key)
        }
    }
}
