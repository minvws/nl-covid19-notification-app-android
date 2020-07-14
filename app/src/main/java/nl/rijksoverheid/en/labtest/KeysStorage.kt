/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.labtest

import android.content.SharedPreferences
import android.util.Base64
import androidx.core.content.edit
import com.google.android.gms.nearby.exposurenotification.TemporaryExposureKey
import okio.Buffer

/**
 * Wrapper around (secure) shared preferences that stores [TemporaryExposureKey]s for upload
 * @param storageKey the key in the [prefs] to use
 * @param prefs the shared preferences to use for storage
 */
class KeysStorage(private val storageKey: String, private val prefs: SharedPreferences) {

    fun storeKeys(keys: List<TemporaryExposureKey>) {
        prefs.edit {
            putStringSet(storageKey, keys.map { encodeKey(it) }.toSet())
        }
    }

    fun getKeys(): List<TemporaryExposureKey> {
        val keys = prefs.getStringSet(storageKey, emptySet()) ?: emptySet()
        return keys.map { decodeKey(it) }
    }

    fun clearKeys() {
        prefs.edit {
            remove(storageKey)
        }
    }

    private fun decodeKey(encoded: String): TemporaryExposureKey {
        val data = Base64.decode(encoded, Base64.NO_PADDING or Base64.NO_WRAP)
        val buffer = Buffer()
        buffer.write(data)
        return TemporaryExposureKey.TemporaryExposureKeyBuilder()
            .setRollingStartIntervalNumber(buffer.readInt())
            .setRollingPeriod(buffer.readInt())
            .setKeyData(buffer.readByteArray()).build()
    }

    private fun encodeKey(key: TemporaryExposureKey): String {
        val buffer = Buffer()
        buffer.writeInt(key.rollingStartIntervalNumber)
        buffer.writeInt(key.rollingPeriod)
        buffer.write(key.keyData)
        return Base64.encodeToString(buffer.readByteArray(), Base64.NO_WRAP or Base64.NO_PADDING)
    }
}
