/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.util

import nl.rijksoverheid.en.lifecyle.Event

sealed class Resource<T>(
    val data: T? = null,
    val error: Event<ErrorType>? = null
) {

    inline fun <R> map(crossinline transform: (T) -> R): Resource<R> {
        return when (this) {
            is Success -> Success(transform(data!!))
            is Loading -> Loading(data?.let(transform))
            is Error -> Error(
                error!!.peekContent(),
                data?.let(transform)
            )
        }
    }

    class Success<T>(data: T) : Resource<T>(data)
    class Loading<T>(data: T? = null) : Resource<T>(data)
    class Error<T>(errorType: ErrorType, data: T? = null) : Resource<T>(data, Event(errorType))
}
