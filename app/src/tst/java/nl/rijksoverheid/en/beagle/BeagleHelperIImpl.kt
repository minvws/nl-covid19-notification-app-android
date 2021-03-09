/*
 *  Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */

package nl.rijksoverheid.en.beagle

import android.app.Application
import com.pandulapeter.beagle.Beagle
import com.pandulapeter.beagle.modules.DeviceInfoModule
import com.pandulapeter.beagle.modules.DividerModule
import com.pandulapeter.beagle.modules.HeaderModule
import com.pandulapeter.beagle.modules.KeylineOverlaySwitchModule
import com.pandulapeter.beagle.modules.PaddingModule
import com.pandulapeter.beagle.modules.SwitchModule
import com.pandulapeter.beagle.modules.TextModule
import nl.rijksoverheid.en.BuildConfig
import nl.rijksoverheid.en.R

object BeagleHelperImpl : BeagleHelper {

    override var useDefaultGuidance: Boolean = false
        private set

    override fun initialize(application: Application) {
        Beagle.initialize(application)
        Beagle.set(
            HeaderModule(
                title = application.getString(R.string.app_name),
                subtitle = BuildConfig.APPLICATION_ID,
                text = "${BuildConfig.BUILD_TYPE} v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
            ),
            PaddingModule(),
            TextModule("General", TextModule.Type.SECTION_HEADER),
            SwitchModule(
                text = "Use local guidance",
                onValueChanged = {
                    useDefaultGuidance = it
                }
            ),
            DividerModule(),
            TextModule("Other", TextModule.Type.SECTION_HEADER),
            KeylineOverlaySwitchModule(),
            DeviceInfoModule(),
        )
    }
}