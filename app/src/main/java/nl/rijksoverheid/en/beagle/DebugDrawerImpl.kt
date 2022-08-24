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
import com.pandulapeter.beagle.common.configuration.Placement
import com.pandulapeter.beagle.common.configuration.Text
import com.pandulapeter.beagle.common.contracts.BeagleListItemContract
import com.pandulapeter.beagle.modules.DeviceInfoModule
import com.pandulapeter.beagle.modules.DividerModule
import com.pandulapeter.beagle.modules.HeaderModule
import com.pandulapeter.beagle.modules.KeylineOverlaySwitchModule
import com.pandulapeter.beagle.modules.MultipleSelectionListModule
import com.pandulapeter.beagle.modules.PaddingModule
import com.pandulapeter.beagle.modules.SingleSelectionListModule
import com.pandulapeter.beagle.modules.SwitchModule
import com.pandulapeter.beagle.modules.TextModule
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import nl.rijksoverheid.en.BuildConfig
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.api.model.FeatureFlag
import nl.rijksoverheid.en.api.model.FeatureFlagOption
import nl.rijksoverheid.en.factory.RepositoryFactory.createExposureNotificationsRepository
import nl.rijksoverheid.en.factory.RepositoryFactory.createLabTestRepository
import nl.rijksoverheid.en.labtest.LabTestRepository
import java.time.format.DateTimeFormatter

private const val TEST_NOTIFICATION_EXPOSURE_DAYS_AGO_ID = "testNotificationExposureDaysAgo"
private const val PREVIOUSLY_KNOWN_EXPOSURE_DATE_ID = "previouslyKnownExposureDate"
private const val KEY_SHARING_HEADER_ID = "keySharingHeader"

class DebugDrawerImpl : DebugDrawer {

    override var useDefaultGuidance: Boolean = false
        private set

    private var useDebugFeatureFlagSetting: Boolean = false
    override var useDebugFeatureFlags = { useDebugFeatureFlagSetting }

    override var testExposureDaysAgo: Int = 5
        private set

    private var debugFeatureFlags: List<FeatureFlag> = emptyList()
    override var getDebugFeatureFlags = { debugFeatureFlags }

    override fun initialize(application: Application) {
        val labTestRepository = createLabTestRepository(application)

        Beagle.initialize(application)
        setBeagleModules(application, labTestRepository)
        observePreviousExposureDate(application)
    }

    private fun setBeagleModules(context: Context, labTestRepository: LabTestRepository) {
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
            SwitchModule(
                text = "Use debug featureFlags",
                onValueChanged = {
                    useDebugFeatureFlagSetting = it
                }
            ),
            MultipleSelectionListModule(
                title = "Debug feature flags",
                items = FeatureFlagOption.values().map { FeatureFlagGroupOption(it) },
                onSelectionChanged = { featureFlagGroupOptions ->
                    debugFeatureFlags = featureFlagGroupOptions.map {
                        FeatureFlag(it.featureFlagOption.id, true)
                    }
                },
                initiallySelectedItemIds = emptySet()
            ),
            TextModule("Exposures", TextModule.Type.SECTION_HEADER),
            SingleSelectionListModule(
                title = "Test notification ExposureDaysAgo",
                items = (0..20).map { value -> RadioGroupOption(value.toString(), value) },
                initiallySelectedItemId = testExposureDaysAgo.toString(),
                onSelectionChanged = {
                    if (it != null) {
                        testExposureDaysAgo = it.value
                    }
                },
                id = TEST_NOTIFICATION_EXPOSURE_DAYS_AGO_ID
            ),
            TextModule("Key sharing", TextModule.Type.SECTION_HEADER, id = KEY_SHARING_HEADER_ID),
            TextModule("Clear key data", TextModule.Type.BUTTON) {
                MainScope().launch {
                    labTestRepository.clearKeyData()
                }
            },
            DividerModule(),
            TextModule("Other", TextModule.Type.SECTION_HEADER),
            KeylineOverlaySwitchModule(),
            DeviceInfoModule()
        )
    }

    private fun observePreviousExposureDate(context: Context) = MainScope().launch {
        val exposureNotificationsRepository = createExposureNotificationsRepository(context)
        exposureNotificationsRepository.previouslyKnownExposureDate()
            .collect { previouslyKnownExposureDate ->
                Beagle.remove(PREVIOUSLY_KNOWN_EXPOSURE_DATE_ID)
                Beagle.add(
                    TextModule(
                        "Previous exposure date: ${
                        previouslyKnownExposureDate?.format(DateTimeFormatter.ISO_LOCAL_DATE) ?: "none"
                        }",
                        TextModule.Type.NORMAL,
                        id = PREVIOUSLY_KNOWN_EXPOSURE_DATE_ID
                    ),
                    placement = Placement.Below(TEST_NOTIFICATION_EXPOSURE_DAYS_AGO_ID)
                )
            }
    }

    data class RadioGroupOption(
        override val id: String,
        val value: Int
    ) : BeagleListItemContract {
        override val title: Text = Text.CharSequence(value.toString())
    }

    data class FeatureFlagGroupOption(
        val featureFlagOption: FeatureFlagOption
    ) : BeagleListItemContract {
        override val title: Text = Text.CharSequence(featureFlagOption.id)
        override val id: String = featureFlagOption.id
    }
}
