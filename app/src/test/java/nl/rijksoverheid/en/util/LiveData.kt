/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.util

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer

fun <T> LiveData<T>.observeForTesting(block: (ValuesObserver<T>) -> Unit) {
    val observer = ValuesObserver<T>()
    try {
        observeForever(observer)
        block(observer)
    } finally {
        removeObserver(observer)
    }
}

class ValuesObserver<T> : Observer<T> {
    val values = mutableListOf<T>()

    override fun onChanged(t: T) {
        values.add(t)
    }
}
