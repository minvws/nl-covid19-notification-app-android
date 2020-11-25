/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.status

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import nl.rijksoverheid.en.BaseFragment
import nl.rijksoverheid.en.ExposureNotificationsViewModel
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.databinding.FragmentStatusBinding
import nl.rijksoverheid.en.navigation.navigateCatchingErrors
import nl.rijksoverheid.en.util.formatExposureDate
import nl.rijksoverheid.en.util.isIgnoringBatteryOptimizations
import nl.rijksoverheid.en.util.requestDisableBatteryOptimizations
import timber.log.Timber

private const val RC_DISABLE_BATTERY_OPTIMIZATIONS = 1

class StatusFragment @JvmOverloads constructor(
    factoryProducer: (() -> ViewModelProvider.Factory)? = null
) : BaseFragment(R.layout.fragment_status, factoryProducer) {
    private val statusViewModel: StatusViewModel by viewModels()
    private val viewModel: ExposureNotificationsViewModel by activityViewModels()

    private lateinit var section: StatusSection
    private val adapter = GroupAdapter<GroupieViewHolder>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        section = StatusSection()
        adapter.add(section)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!statusViewModel.isPlayServicesUpToDate()) {
            findNavController().navigate(StatusFragmentDirections.actionUpdatePlayServices())
        } else if (!statusViewModel.hasCompletedOnboarding()) {
            findNavController().navigate(StatusFragmentDirections.actionOnboarding())
        }

        val binding = FragmentStatusBinding.bind(view)
        binding.content.adapter = adapter
        adapter.setOnItemClickListener { item, _ ->
            when (item) {
                StatusActionItem.About -> findNavController().navigateCatchingErrors(
                    StatusFragmentDirections.actionAbout()
                )
                StatusActionItem.Share -> share()
                StatusActionItem.GenericNotification -> findNavController().navigateCatchingErrors(
                    StatusFragmentDirections.actionGenericNotification()
                )
                StatusActionItem.RequestTest -> findNavController().navigateCatchingErrors(
                    StatusFragmentDirections.actionRequestTest(statusViewModel.exposureDetected)
                )
                StatusActionItem.LabTest -> findNavController().navigateCatchingErrors(
                    StatusFragmentDirections.actionLabTest()
                )
                StatusActionItem.Settings -> findNavController().navigateCatchingErrors(
                    StatusFragmentDirections.actionSettings()
                )
            }
        }

        viewModel.notificationState.observe(viewLifecycleOwner) {
            if (it is ExposureNotificationsViewModel.NotificationsState.Unavailable) {
                Toast.makeText(context, R.string.error_api_not_available, Toast.LENGTH_LONG)
                    .show()
            }
        }

        statusViewModel.headerState.observe(viewLifecycleOwner) {
            when (it) {
                StatusViewModel.HeaderState.Active -> section.updateHeader(
                    headerState = it
                )
                StatusViewModel.HeaderState.Disabled -> section.updateHeader(
                    headerState = it,
                    primaryAction = ::resetAndRequestEnableNotifications
                )
                is StatusViewModel.HeaderState.Exposed -> section.updateHeader(
                    headerState = it,
                    primaryAction = { navigateToPostNotification(it.date.toEpochDay()) },
                    secondaryAction = {
                        showRemoveNotificationConfirmationDialog(
                            it.date.formatExposureDate(requireContext())
                        )
                    }
                )
            }
        }
        statusViewModel.errorState.observe(viewLifecycleOwner) {
            when (it) {
                StatusViewModel.ErrorState.None -> section.updateErrorState(it)
                StatusViewModel.ErrorState.SyncIssues -> section.updateErrorState(it) { statusViewModel.resetErrorState() }
                StatusViewModel.ErrorState.NotificationsDisabled -> section.updateErrorState(it) { navigateToNotificationSettings() }
                is StatusViewModel.ErrorState.ConsentRequired -> section.updateErrorState(it) { resetAndRequestEnableNotifications() }
            }
        }

        statusViewModel.hasSeenLatestTerms.observe(viewLifecycleOwner) {
            section.infoItem = if (!it)
                StatusInfoItem(
                    R.string.status_info_interop_announcement_message,
                    actionMoreInfo = {
                        val uri = Uri.parse(getString(R.string.interop_url, getString(R.string.app_language)))
                        CustomTabsIntent.Builder().build().launchUrl(requireContext(), uri)
                    },
                    actionClose = {
                        statusViewModel.setHasSeenLatestTerms()
                    }
                )
            else null
        }
    }

    override fun onResume() {
        super.onResume()
        if (!requireContext().isIgnoringBatteryOptimizations()) {
            section.showBatteryOptimisationsError {
                requestDisableBatteryOptimizations(
                    RC_DISABLE_BATTERY_OPTIMIZATIONS
                )
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_DISABLE_BATTERY_OPTIMIZATIONS) {
            if (requireContext().isIgnoringBatteryOptimizations()) {
                section.removeBatteryOptimisationsError()
            }
        }
    }

    private fun resetAndRequestEnableNotifications() {
        if (viewModel.locationPreconditionSatisfied) {
            viewModel.requestEnableNotificationsForcingConsent()
        } else {
            findNavController().navigateCatchingErrors(StatusFragmentDirections.actionEnableLocationServices())
        }
    }

    private fun navigateToPostNotification(epochDay: Long) =
        findNavController().navigate(StatusFragmentDirections.actionPostNotification(epochDay))

    private fun navigateToNotificationSettings() {
        try {
            val intent = Intent()
            intent.action = "android.settings.APP_NOTIFICATION_SETTINGS"
            intent.putExtra("app_package", requireContext().packageName)
            intent.putExtra("app_uid", requireContext().applicationInfo.uid)
            intent.putExtra("android.provider.extra.APP_PACKAGE", requireContext().packageName)
            startActivity(intent)
        } catch (ex: Exception) {
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.addCategory(Intent.CATEGORY_DEFAULT)
                intent.data = Uri.parse("package:${requireContext().packageName}")
                startActivity(intent)
            } catch (ex: ActivityNotFoundException) {
                Timber.e("Could not open app settings")
            }
        }
    }

    private fun showRemoveNotificationConfirmationDialog(formattedDate: String) {
        findNavController().navigate(
            StatusFragmentDirections.actionRemoveExposedMessage(formattedDate)
        )
        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<Boolean>(
            RemoveExposedMessageDialogFragment.REMOVE_EXPOSED_MESSAGE_RESULT
        )?.observe(viewLifecycleOwner) {
            if (it) {
                statusViewModel.removeExposure()
            }
        }
    }

    private fun share() {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(
                Intent.EXTRA_TEXT,
                getString(R.string.share_content, getString(R.string.share_url))
            )
            type = "text/plain"
        }

        val shareIntent = Intent.createChooser(sendIntent, getString(R.string.share_title))
        startActivity(shareIntent)
    }
}
