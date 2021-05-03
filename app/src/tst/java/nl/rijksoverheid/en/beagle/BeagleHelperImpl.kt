/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.beagle

import android.app.Application
import android.content.Context
import com.pandulapeter.beagle.Beagle
import com.pandulapeter.beagle.common.configuration.Text
import com.pandulapeter.beagle.common.contracts.BeagleListItemContract
import com.pandulapeter.beagle.modules.DeviceInfoModule
import com.pandulapeter.beagle.modules.DividerModule
import com.pandulapeter.beagle.modules.HeaderModule
import com.pandulapeter.beagle.modules.KeylineOverlaySwitchModule
import com.pandulapeter.beagle.modules.PaddingModule
import com.pandulapeter.beagle.modules.SingleSelectionListModule
import com.pandulapeter.beagle.modules.SwitchModule
import com.pandulapeter.beagle.modules.TextModule
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import nl.rijksoverheid.en.BuildConfig
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.factory.createExposureNotificationsRepository
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object BeagleHelperImpl : BeagleHelper {

    override var useDefaultGuidance: Boolean = false
        private set

    override var testExposureDaysAgo: Int = 5
        private set

    override fun initialize(application: Application) {
        Beagle.initialize(application)
        val exposureNotificationsRepository = createExposureNotificationsRepository(application)
        exposureNotificationsRepository.previouslyKnownExposureDate()
            .onEach { previouslyKnownExposureDate ->
                setBeagleModules(
                    application,
                    previouslyKnownExposureDate
                )
            }.launchIn(MainScope())
    }

    private fun setBeagleModules(
        context: Context,
        previouslyKnownExposureDate: LocalDate?
    ) {
        Beagle.set(
            HeaderModule(
                title = context.getString(R.string.app_name),
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
            TextModule("Exposures", TextModule.Type.SECTION_HEADER),
            SingleSelectionListModule(
                title = "Test notification ExposureDaysAgo",
                items = (0..14).map { value -> RadioGroupOption(value.toString(), value) },
                initiallySelectedItemId = testExposureDaysAgo.toString(),
                onSelectionChanged = {
                    if (it != null)
                        testExposureDaysAgo = it.value
                }
            ),
            TextModule(
                "Previous exposure date: ${
                previouslyKnownExposureDate?.format(DateTimeFormatter.ISO_LOCAL_DATE) ?: "none"
                }",
                TextModule.Type.NORMAL
            ),
            DividerModule(),
            TextModule("Other", TextModule.Type.SECTION_HEADER),
            KeylineOverlaySwitchModule(),
            DeviceInfoModule(),
        )
    }

    data class RadioGroupOption(
        override val id: String,
        val value: Int
    ) : BeagleListItemContract {
        override val title: Text = Text.CharSequence(value.toString())
    }
}
