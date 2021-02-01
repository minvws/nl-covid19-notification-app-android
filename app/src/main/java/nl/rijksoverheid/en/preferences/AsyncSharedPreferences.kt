/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.preferences

import android.annotation.SuppressLint
import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async

/**
 * Interface that provides async access to [SharedPreferences]
 * @param factory factory to create the preferences
 */
class AsyncSharedPreferences(private val factory: suspend () -> SharedPreferences) {
    // create the underlying preferences lazily on the calling thread / dispatcher
    private val preferences =
        GlobalScope.async(
            context = Dispatchers.Unconfined,
            start = CoroutineStart.LAZY
        ) { factory() }

    suspend fun getString(key: String, default: String?): String? =
        preferences.await().getString(key, default)

    suspend fun getLong(key: String, default: Long) = preferences.await().getLong(key, default)
    suspend fun getStringSet(key: String, default: Set<String> = emptySet()): Set<String> =
        preferences.await().getStringSet(key, default) ?: emptySet()

    suspend fun getInt(key: String, default: Int) = preferences.await().getInt(key, default)
    suspend fun getBoolean(key: String, default: Boolean) =
        preferences.await().getBoolean(key, default)

    suspend fun contains(key: String): Boolean = preferences.await().contains(key)

    @SuppressLint("ApplySharedPref")
    suspend fun edit(commit: Boolean = false, action: SharedPreferences.Editor.() -> Unit) {
        val editor = preferences.await().edit()
        action(editor)
        if (commit) {
            editor.commit()
        } else {
            editor.apply()
        }
    }

    /**
     * Retrieve the underlying preferences
     * @return the preferences created by [factory]
     */
    suspend fun getPreferences(): SharedPreferences = preferences.await()
}
