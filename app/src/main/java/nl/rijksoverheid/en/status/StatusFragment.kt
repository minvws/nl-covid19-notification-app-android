/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.status

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import nl.rijksoverheid.en.BaseFragment
import nl.rijksoverheid.en.ExposureNotificationsViewModel
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.databinding.FragmentStatusBinding
import nl.rijksoverheid.en.job.DownloadDiagnosisKeysWorker
import nl.rijksoverheid.en.lifecyle.EventObserver
import timber.log.Timber

private const val RC_REQUEST_CONSENT = 1

class StatusFragment : BaseFragment(R.layout.fragment_status) {
    private val statusViewModel: StatusViewModel by viewModels()
    private val viewModel: ExposureNotificationsViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!statusViewModel.hasCompletedOnboarding()) {
            findNavController().navigate(StatusFragmentDirections.actionOnboarding())
        }

        val binding = FragmentStatusBinding.bind(view)

        binding.toolbar.inflateMenu(R.menu.status)
        binding.toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.menu_status_report -> viewModel.requestUploadTemporaryKeys()
                R.id.menu_status_check -> DownloadDiagnosisKeysWorker.queue(requireContext())
            }
            true
        }

        viewModel.notificationState.observe(viewLifecycleOwner) {
            when (it) {
                ExposureNotificationsViewModel.NotificationsState.Enabled -> { /* all is fine */
                }
                ExposureNotificationsViewModel.NotificationsState.Disabled -> { /* TODO() */
                }
                ExposureNotificationsViewModel.NotificationsState.Unavailable -> showApiUnavailableError()
            }
        }

        viewModel.exportTemporaryKeysResult.observe(viewLifecycleOwner, EventObserver {
            when (it) {
                is ExposureNotificationsViewModel.ExportKeysResult.RequestConsent -> {
                    try {
                        requireActivity().startIntentSenderFromFragment(
                            this,
                            it.resolution.intentSender,
                            RC_REQUEST_CONSENT,
                            null,
                            0,
                            0,
                            0,
                            null
                        )
                    } catch (ex: IntentSender.SendIntentException) {
                        Timber.e(ex, "Error requesting consent")
                    }
                }
                ExposureNotificationsViewModel.ExportKeysResult.Success -> Toast.makeText(
                    requireContext(),
                    R.string.status_upload_success,
                    Toast.LENGTH_LONG
                ).show()
                ExposureNotificationsViewModel.ExportKeysResult.Error -> Toast.makeText(
                    requireContext(),
                    R.string.status_upload_failure,
                    Toast.LENGTH_LONG
                ).show()
            }
        })

        viewModel.exposureDetected.observe(viewLifecycleOwner) {
            Timber.d("Exposure = $it")
            val drawableRes =
                if (it) R.drawable.ic_status_exposure else R.drawable.ic_status_no_exposure
            val headline =
                if (it) R.string.status_exposure_detected_headline else R.string.status_no_exposure_detected_headline
            binding.status.setCompoundDrawablesRelativeWithIntrinsicBounds(
                null,
                AppCompatResources.getDrawable(requireContext(), drawableRes),
                null,
                null
            )
            binding.status.setText(headline)
            binding.resetStatus.visibility = if (it) View.VISIBLE else View.INVISIBLE
        }

        binding.resetStatus.setOnClickListener {
            viewModel.resetExposures()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_REQUEST_CONSENT && resultCode == Activity.RESULT_OK) {
            viewModel.requestUploadTemporaryKeys()
        }
    }

    private fun showApiUnavailableError() {
        // TODO
    }
}
