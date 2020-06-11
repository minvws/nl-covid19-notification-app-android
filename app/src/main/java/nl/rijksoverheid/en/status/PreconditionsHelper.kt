/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.status

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

class PreconditionsHelper(context: Context, private val onPreconditionsChanged: () -> Unit) :
    DefaultLifecycleObserver {

    private val context = context.applicationContext

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            onPreconditionsChanged()
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        val filter = IntentFilter().apply {
            addAction(LocationManager.MODE_CHANGED_ACTION)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        }
        context.registerReceiver(receiver, filter)
    }

    override fun onStop(owner: LifecycleOwner) {
        context.unregisterReceiver(receiver)
        super.onStop(owner)
    }
}
