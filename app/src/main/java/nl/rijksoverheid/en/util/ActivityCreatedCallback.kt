/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.util

import android.app.Activity
import android.app.Application
import android.os.Bundle

/**
 * Simplified ActivityLifecycleCallbacks that only triggers a callback on activity created
 * @param callback method for onActivityCreated
 */
class ActivityCreatedCallback(private val callback: (Activity) -> Unit) :
    Application.ActivityLifecycleCallbacks {

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        callback.invoke(activity)
    }

    override fun onActivityStarted(activity: Activity) {
        // Ignore onActivityStarted callback
    }

    override fun onActivityResumed(activity: Activity) {
        // Ignore onActivityResumed callback
    }

    override fun onActivityPaused(activity: Activity) {
        // Ignore onActivityPaused callback
    }

    override fun onActivityStopped(activity: Activity) {
        // Ignore onActivityStopped callback
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        // Ignore onActivitySaveInstanceState callback
    }

    override fun onActivityDestroyed(activity: Activity) {
        // Ignore onActivityDestroyed callback
    }
}
