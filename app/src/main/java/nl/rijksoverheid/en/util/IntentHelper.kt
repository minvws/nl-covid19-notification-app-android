/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.fragment.app.Fragment
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import nl.rijksoverheid.en.navigation.navigateCatchingErrors

object IntentHelper {

    /**
     * Open Play store activity with a fallback to the webpage if the activity couldn't be found
     */
    fun openPlayStore(context: Context, applicationId: String) = with(context) {
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("market://details?id=$applicationId")
        ).addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            startActivity(intent)
        } catch (ex: ActivityNotFoundException) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("http://play.google.com/store/apps/details?id=$applicationId")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    /**
     * Open phone call intent or navigate to activity not found direction when not supported
     */
    fun openPhoneCallIntent(
        fragment: Fragment,
        phoneNumber: String,
        activityNotFoundDirection: NavDirections
    ) = with(fragment) {
        try {
            startActivity(
                Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:$phoneNumber")
                }
            )
        } catch (e: ActivityNotFoundException) {
            findNavController().navigateCatchingErrors(activityNotFoundDirection)
        }
    }
}
