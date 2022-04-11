/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.util

import android.content.Context
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import androidx.core.content.ContextCompat

object Accessibility {

    /**
     * Returns the AccessibilityManager if available and enabled.
     *
     * @param context Context reference
     *
     * @return AccessibilityManager object, or null
     */
    private fun accessibilityManager(context: Context?): AccessibilityManager? {
        if (context != null) {
            val service = ContextCompat.getSystemService(context, AccessibilityManager::class.java)
            if (service is AccessibilityManager && service.isEnabled) {
                return service
            }
        }
        return null
    }

    /**
     * Checks whether touch exploration is active
     *
     * @param context Context reference
     */
    fun touchExploration(context: Context?): Boolean {
        return accessibilityManager(context)?.isTouchExplorationEnabled ?: false
    }

    fun announcementForScreenReader(context: Context, description: String) {
        if (!touchExploration(context)) {
            return
        }

        val event = AccessibilityEvent.obtain()
        event.packageName = context.packageName
        event.eventType = AccessibilityEvent.TYPE_ANNOUNCEMENT
        event.text.add(description)
        accessibilityManager(context)?.sendAccessibilityEvent(event)
    }
}
