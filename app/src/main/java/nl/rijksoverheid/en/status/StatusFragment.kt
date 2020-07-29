/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.status

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import nl.rijksoverheid.en.BaseFragment
import nl.rijksoverheid.en.ExposureNotificationsViewModel
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.databinding.FragmentStatusBinding
import nl.rijksoverheid.en.lifecyle.EventObserver

class StatusFragment @JvmOverloads constructor(
    factoryProducer: (() -> ViewModelProvider.Factory)? = null
) : BaseFragment(R.layout.fragment_status, factoryProducer) {
    private val statusViewModel: StatusViewModel by viewModels()
    private val viewModel: ExposureNotificationsViewModel by activityViewModels()

    private val section = StatusSection()
    private val adapter = GroupAdapter<GroupieViewHolder>().apply { add(section) }

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
                StatusActionItem.About -> findNavController().navigate(
                    StatusFragmentDirections.actionAbout()
                )
                StatusActionItem.Share -> share()
                StatusActionItem.GenericNotification -> findNavController().navigate(
                    StatusFragmentDirections.actionGenericNotification()
                )
                StatusActionItem.RequestTest -> findNavController().navigate(
                    StatusFragmentDirections.actionRequestTest()
                )
                StatusActionItem.LabTest -> findNavController().navigate(
                    StatusFragmentDirections.actionLabTest()
                )
            }
        }

        viewModel.notificationState.observe(viewLifecycleOwner) {
            if (it is ExposureNotificationsViewModel.NotificationsState.Unavailable) {
                Toast.makeText(context, R.string.error_api_not_available, Toast.LENGTH_LONG)
                    .show()
            }
        }

        statusViewModel.requestEnableNotifications.observe(viewLifecycleOwner, EventObserver {
            viewModel.requestEnableNotifications()
        })

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
                    secondaryAction = ::showRemoveNotificationConfirmationDialog
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
    }

    private fun resetAndRequestEnableNotifications() {
        viewModel.requestEnableNotificationsForcingConsent()
    }

    private fun navigateToPostNotification(epochDay: Long) =
        findNavController().navigate(StatusFragmentDirections.actionPostNotification(epochDay))

    private fun navigateToNotificationSettings() {
        val intent = Intent()
        intent.action = "android.settings.APP_NOTIFICATION_SETTINGS"
        intent.putExtra("app_package", requireContext().packageName)
        intent.putExtra("app_uid", requireContext().applicationInfo.uid)
        intent.putExtra("android.provider.extra.APP_PACKAGE", requireContext().packageName)
        startActivity(intent)
    }

    private fun showRemoveNotificationConfirmationDialog() {
        findNavController().navigate(StatusFragmentDirections.actionRemoveExposedMessage())
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
