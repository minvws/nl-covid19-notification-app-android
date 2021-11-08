/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.lifecyle

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

fun LifecycleOwner.asFlow() = callbackFlow {
    val observer = object : DefaultLifecycleObserver {
        override fun onCreate(owner: LifecycleOwner) {
            trySend(owner.lifecycle.currentState)
        }

        override fun onStart(owner: LifecycleOwner) {
            trySend(owner.lifecycle.currentState)
        }

        override fun onResume(owner: LifecycleOwner) {
            trySend(owner.lifecycle.currentState)
        }

        override fun onPause(owner: LifecycleOwner) {
            trySend(owner.lifecycle.currentState)
        }

        override fun onStop(owner: LifecycleOwner) {
            trySend(owner.lifecycle.currentState)
        }

        override fun onDestroy(owner: LifecycleOwner) {
            trySend(owner.lifecycle.currentState)
        }
    }

    lifecycle.addObserver(observer)

    awaitClose {
        lifecycle.removeObserver(observer)
    }
}
