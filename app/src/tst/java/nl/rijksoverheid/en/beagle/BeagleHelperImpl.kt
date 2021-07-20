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
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import nl.rijksoverheid.en.BuildConfig
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.api.model.FeatureFlag
import nl.rijksoverheid.en.api.model.FeatureFlagOption
import nl.rijksoverheid.en.factory.createExposureNotificationsRepository
import nl.rijksoverheid.en.factory.createLabTestRepository
import nl.rijksoverheid.en.labtest.LabTestRepository
import nl.rijksoverheid.en.util.formatDateTime
import java.time.format.DateTimeFormatter

object BeagleHelperImpl : BeagleHelper {

    override var useDefaultGuidance: Boolean = false
        private set

    private var useDebugFeatureFlagSetting: Boolean = false
    override var useDebugFeatureFlags = { useDebugFeatureFlagSetting }

    override var testExposureDaysAgo: Int = 5
        private set

    private var debugFeatureFlags: List<FeatureFlag> = emptyList()
    override var getDebugFeatureFlags = { debugFeatureFlags }

    private const val testNotificationExposureDaysAgoId = "testNotificationExposureDaysAgo"
    private const val previouslyKnownExposureDateId = "previouslyKnownExposureDate"
    private const val keySharingHeaderId = "keySharingHeader"
    private const val cachedRegistrationCodeId = "cachedRegistrationCode"
    private const val cachedRegistrationExpirationId = "cachedRegistrationExpiration"

    override fun initialize(application: Application) {
        Beagle.initialize(application)
        setBeagleModules(application)
        observePreviousExposureDate(application)

        val labTestRepository = createLabTestRepository(application)
        observeRegistrationCode(labTestRepository)
        observeRegistrationKeyExpirationDate(application, labTestRepository)
    }

    private fun setBeagleModules(context: Context) {
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
                    if (it != null)
                        testExposureDaysAgo = it.value
                },
                id = testNotificationExposureDaysAgoId
            ),
            TextModule("Key sharing", TextModule.Type.SECTION_HEADER, id = keySharingHeaderId),
            DividerModule(),
            TextModule("Other", TextModule.Type.SECTION_HEADER),
            KeylineOverlaySwitchModule(),
            DeviceInfoModule(),
        )
    }

    private fun observePreviousExposureDate(context: Context) = MainScope().launch {
        val exposureNotificationsRepository = createExposureNotificationsRepository(context)
        exposureNotificationsRepository.previouslyKnownExposureDate()
            .collect { previouslyKnownExposureDate ->
                Beagle.remove(previouslyKnownExposureDateId)
                Beagle.add(
                    TextModule(
                        "Previous exposure date: ${
                        previouslyKnownExposureDate?.format(DateTimeFormatter.ISO_LOCAL_DATE) ?: "none"
                        }",
                        TextModule.Type.NORMAL, id = previouslyKnownExposureDateId
                    ),
                    placement = Placement.Below(testNotificationExposureDaysAgoId)
                )
            }
    }

    private fun observeRegistrationCode(labTestRepository: LabTestRepository) = MainScope().launch {
        labTestRepository.getCachedRegistrationCodeFlow()
            .collect { registrationKey ->
                Beagle.remove(cachedRegistrationCodeId)
                Beagle.add(
                    TextModule(
                        "Cached code: ${
                        registrationKey ?: "none"
                        }",
                        TextModule.Type.NORMAL, id = cachedRegistrationCodeId
                    ),
                    placement = Placement.Below(keySharingHeaderId)
                )
            }
    }

    private fun observeRegistrationKeyExpirationDate(
        context: Context,
        labTestRepository: LabTestRepository
    ) = MainScope().launch {
        labTestRepository.getCachedRegistrationExpirationFlow()
            .collect { expirationDate ->
                Beagle.remove(cachedRegistrationExpirationId)
                Beagle.add(
                    TextModule(
                        "Expiration date: ${
                        expirationDate?.formatDateTime(context) ?: "none"
                        }",
                        TextModule.Type.NORMAL, id = cachedRegistrationExpirationId
                    ),
                    placement = Placement.Below(cachedRegistrationCodeId)
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
