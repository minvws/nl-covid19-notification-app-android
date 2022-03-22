/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.status

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.coroutineScope
import androidx.navigation.fragment.findNavController
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import nl.rijksoverheid.en.BaseFragment
import nl.rijksoverheid.en.ExposureNotificationsViewModel
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.api.model.DashboardItem
import nl.rijksoverheid.en.databinding.FragmentStatusBinding
import nl.rijksoverheid.en.navigation.navigateCatchingErrors
import nl.rijksoverheid.en.status.StatusSection.NotificationAction
import nl.rijksoverheid.en.status.StatusViewModel.NotificationState
import nl.rijksoverheid.en.util.formatExposureDate
import nl.rijksoverheid.en.util.isIgnoringBatteryOptimizations
import nl.rijksoverheid.en.util.launchDisableBatteryOptimizationsRequest
import timber.log.Timber
import java.time.LocalDate

/**
 * Fragment used as landing page which provides status information
 * and links to different features of the app.
 */
class StatusFragment @JvmOverloads constructor(
    factoryProducer: (() -> ViewModelProvider.Factory)? = null
) : BaseFragment(R.layout.fragment_status, factoryProducer) {
    private val statusViewModel: StatusViewModel by viewModels()
    private val viewModel: ExposureNotificationsViewModel by activityViewModels()

    private lateinit var section: StatusSection
    private val adapter = GroupAdapter<GroupieViewHolder>()

    private val requestBluetoothConnectPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        }
    }

    private val disableBatteryOptimizationsResultRegistration =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            statusViewModel.isIgnoringBatteryOptimizations.value =
                requireContext().isIgnoringBatteryOptimizations()
        }

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
                StatusActionItem.Share -> findNavController().navigateCatchingErrors(
                    StatusFragmentDirections.actionShare()
                )
                StatusActionItem.GenericNotification -> findNavController().navigateCatchingErrors(
                    StatusFragmentDirections.actionGenericNotification()
                )
                StatusActionItem.LabTest -> navigateToSharingKeys()
                StatusActionItem.RequestTest -> requestTest()
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

        statusViewModel.headerState.observe(viewLifecycleOwner, ::updateHeaderState)
        statusViewModel.notificationState.observe(viewLifecycleOwner, ::updateNotificationState)
        statusViewModel.lastKeysProcessed.observe(viewLifecycleOwner) {
            section.lastKeysProcessed = it
        }
        statusViewModel.dashboardData.observe(viewLifecycleOwner) { dashboardData ->
            dashboardData.data?.let {
                section.updateDashboardData(it, ::navigateToDashboardItem)
            }
        }
        statusViewModel.exposureNotificationApiUpdateRequired.observe(viewLifecycleOwner) { requireAnUpdate ->
            if (requireAnUpdate)
                findNavController().navigateCatchingErrors(StatusFragmentDirections.actionUpdatePlayServices())
        }
    }

    override fun onResume() {
        super.onResume()
        statusViewModel.isIgnoringBatteryOptimizations.value = requireContext().isIgnoringBatteryOptimizations()
        section.refreshStateContent()
        statusViewModel.updateDashboardData()
    }

    private fun updateHeaderState(headerState: StatusViewModel.HeaderState) {
        when (headerState) {
            StatusViewModel.HeaderState.Active -> section.updateHeader(
                headerState = headerState
            )
            is StatusViewModel.HeaderState.BluetoothDisabled -> section.updateHeader(
                headerState = headerState,
                primaryAction = ::requestEnableBluetooth
            )
            is StatusViewModel.HeaderState.LocationDisabled -> section.updateHeader(
                headerState = headerState,
                primaryAction = ::requestEnableLocationServices
            )
            is StatusViewModel.HeaderState.Disabled -> section.updateHeader(
                headerState = headerState,
                primaryAction = ::resetAndRequestEnableNotifications
            )
            is StatusViewModel.HeaderState.SyncIssues -> section.updateHeader(
                headerState = headerState,
                primaryAction = statusViewModel::resetErrorState
            )
            is StatusViewModel.HeaderState.SyncIssuesWifiOnly -> section.updateHeader(
                headerState = headerState,
                primaryAction = ::navigateToInternetRequiredFragment
            )
            is StatusViewModel.HeaderState.Paused -> {
                section.updateHeader(
                    headerState = headerState,
                    primaryAction = ::resetAndRequestEnableNotifications
                )
            }
            is StatusViewModel.HeaderState.Exposed -> {
                section.updateHeader(
                    headerState = headerState,
                    primaryAction = {
                        navigateToPostNotification(
                            headerState.date,
                            headerState.notificationReceivedDate
                        )
                    },
                    secondaryAction = {
                        showRemoveNotificationConfirmationDialog(
                            headerState.date.formatExposureDate(requireContext())
                        )
                    }
                )
            }
        }
    }

    private fun updateNotificationState(notificationState: List<NotificationState>) {
        section.updateNotifications(notificationState) { state: NotificationState, action: NotificationAction ->
            when (state) {
                is NotificationState.Paused -> resetAndRequestEnableNotifications()
                is NotificationState.ExposureOver14DaysAgo -> {
                    when (action) {
                        NotificationAction.Primary -> {
                            showRemoveNotificationConfirmationDialog(
                                state.exposureDate.formatExposureDate(requireContext())
                            )
                        }
                        NotificationAction.Secondary -> {
                            navigateToPostNotification(
                                state.exposureDate,
                                state.notificationReceivedDate
                            )
                        }
                    }
                }
                NotificationState.BatteryOptimizationEnabled ->
                    disableBatteryOptimizationsResultRegistration.launchDisableBatteryOptimizationsRequest()
                NotificationState.Error.BluetoothDisabled -> requestEnableBluetooth()
                NotificationState.Error.ConsentRequired -> resetAndRequestEnableNotifications()
                NotificationState.Error.LocationDisabled -> requestEnableLocationServices()
                NotificationState.Error.NotificationsDisabled -> navigateToNotificationSettings()
                NotificationState.Error.SyncIssues -> statusViewModel.resetErrorState()
                NotificationState.Error.SyncIssuesWifiOnly -> navigateToInternetRequiredFragment()
            }
        }
    }

    private fun requestTest() {
        viewLifecycleOwner.lifecycle.coroutineScope.launchWhenResumed {
            val appointmentInfo = statusViewModel.getAppointmentInfo(requireContext())
            findNavController().navigateCatchingErrors(
                StatusFragmentDirections.actionRequestTest(appointmentInfo.phoneNumber, appointmentInfo.website)
            )
        }
    }

    private fun resetAndRequestEnableNotifications() {
        viewModel.requestEnableNotificationsForcingConsent()
    }

    private fun requestEnableLocationServices() {
        findNavController().navigateCatchingErrors(StatusFragmentDirections.actionEnableLocationServices())
    }

    private fun requestEnableBluetooth() {
        // Android 12 requires BLUETOOTH_CONNECT permission for BluetoothAdapter.ACTION_REQUEST_ENABLE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val bluetoothConnectPermission = ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.BLUETOOTH_CONNECT
            )
            when {
                bluetoothConnectPermission == PackageManager.PERMISSION_GRANTED -> {
                    // Permission granted so we can start BluetoothAdapter.ACTION_REQUEST_ENABLE intent
                    startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                }
                shouldShowRequestPermissionRationale(Manifest.permission.BLUETOOTH_CONNECT) -> {
                    // Permission was denied so open bluetooth settings and let the user turn on bluetooth by themself
                    val intentOpenBluetoothSettings = Intent()
                    intentOpenBluetoothSettings.action = Settings.ACTION_BLUETOOTH_SETTINGS
                    startActivity(intentOpenBluetoothSettings)
                }
                else -> {
                    // Request BLUETOOTH_CONNECT permission
                    requestBluetoothConnectPermission.launch(Manifest.permission.BLUETOOTH_CONNECT)
                }
            }
        } else {
            startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        }
    }

    private fun navigateToInternetRequiredFragment() {
        findNavController().navigateCatchingErrors(StatusFragmentDirections.actionNavInternetRequired())
    }

    private fun navigateToSharingKeys() {
        viewLifecycleOwner.lifecycle.coroutineScope.launchWhenResumed {
            if (statusViewModel.hasIndependentKeySharing())
                findNavController().navigateCatchingErrors(StatusFragmentDirections.actionKeySharingOptions())
            else
                findNavController().navigateCatchingErrors(StatusFragmentDirections.actionLabTest())
        }
    }

    private fun navigateToPostNotification(
        lastExposureLocalDate: LocalDate,
        notificationReceivedLocalDate: LocalDate?
    ) =
        findNavController().navigateCatchingErrors(
            StatusFragmentDirections.actionPostNotification(
                lastExposureLocalDate.toString(),
                notificationReceivedLocalDate?.toString()
            )
        )

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

    private fun navigateToDashboardItem(dashboardItem: DashboardItem) {
        findNavController().navigateCatchingErrors(
            StatusFragmentDirections.actionDashboard(dashboardItem.reference)
        )
    }

    private fun showRemoveNotificationConfirmationDialog(formattedDate: String) {
        try {
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
        } catch (ex: IllegalArgumentException) {
            Timber.w(ex, "Error while navigating")
        }
    }
}
