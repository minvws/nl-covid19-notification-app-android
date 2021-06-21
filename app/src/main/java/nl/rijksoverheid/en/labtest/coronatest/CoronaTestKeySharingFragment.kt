/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.labtest.coronatest

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.browser.customtabs.CustomTabsIntent
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.coroutineScope
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import nl.rijksoverheid.en.BaseFragment
import nl.rijksoverheid.en.ExposureNotificationsViewModel
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.about.FAQItemId
import nl.rijksoverheid.en.databinding.FragmentListBinding
import nl.rijksoverheid.en.labtest.LabTestViewModel
import nl.rijksoverheid.en.lifecyle.EventObserver
import nl.rijksoverheid.en.navigation.navigateCatchingErrors
import nl.rijksoverheid.en.util.IllustrationSpaceDecoration
import timber.log.Timber

class CoronaTestKeySharingFragment : BaseFragment(R.layout.fragment_list) {
    private val labViewModel: LabTestViewModel by viewModels()
    private val viewModel: ExposureNotificationsViewModel by activityViewModels()
    private val section = CoronaTestKeySharingSection(
        retry = { labViewModel.retry() },
        requestConsent = { viewModel.requestEnableNotifications() },
        uploadKeys = { labViewModel.upload() },
        openCoronaTestWebsite = ::openShareKeyUrl,
        copy = ::copyToClipboard,
        finish = ::finishCoronaTestKeySharing
    )
    private val adapter = GroupAdapter<GroupieViewHolder>().apply { add(section) }

    private val requestUploadConsent =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) labViewModel.upload()
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentListBinding.bind(view)

        binding.toolbar.setTitle(R.string.lab_test_generic_toolbar_title)
        binding.content.addItemDecoration(IllustrationSpaceDecoration())
        binding.content.adapter = adapter

        adapter.setOnItemClickListener { _, _ ->
            findNavController().navigateCatchingErrors(
                CoronaTestKeySharingFragmentDirections.actionHowItWorks(FAQItemId.UPLOAD_KEYS),
                FragmentNavigatorExtras(binding.appbar to binding.appbar.transitionName)
            )
        }

        labViewModel.keyState.observe(viewLifecycleOwner) { keyState ->
            section.update(keyState)
            // binding.button.isEnabled =
            //    getContinueButtonEnabledState(keyState, section.notificationsState)
        }
        viewModel.notificationState.observe(viewLifecycleOwner) { notificationsState ->
            section.update(notificationsState)
            // binding.button.isEnabled =
            //    getContinueButtonEnabledState(section.keyState, notificationsState)
        }

        labViewModel.uploadResult.observe(
            viewLifecycleOwner,
            EventObserver {
                if (it is LabTestViewModel.UploadResult.RequestConsent)
                    requestConsent(it.resolution.intentSender)

                section.update(it)
            }
        )
    }

    override fun onStart() {
        super.onStart()
        labViewModel.retry()
    }

    private fun finishCoronaTestKeySharing() {
        labViewModel.usedKey?.let { key ->
            findNavController().navigateCatchingErrors(
                CoronaTestKeySharingFragmentDirections.actionLabTestDone(key)
            )
        }
    }

    private fun requestConsent(intentSender: IntentSender) {
        try {
            requestUploadConsent.launch(IntentSenderRequest.Builder(intentSender).build())
        } catch (ex: Exception) {
            Timber.e(ex, "Error requesting consent")
        }
    }

    private fun openShareKeyUrl() {
        viewLifecycleOwner.lifecycle.coroutineScope.launchWhenResumed {
            labViewModel.getShareKeyUrl().let {
                val url = Uri.parse(labViewModel.getShareKeyUrl())
                CustomTabsIntent.Builder().build().launchUrl(requireContext(), url)
            }
        }
    }

    private fun copyToClipboard(key: String) {
        view?.let {
            val clipboard =
                it.context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    ?: return
            val clip =
                ClipData.newPlainText(getString(R.string.lab_test_copy_key_to_clipboard), key)
            clipboard.setPrimaryClip(clip)
            Snackbar.make(it, R.string.lab_test_copy_key_to_clipboard, Snackbar.LENGTH_LONG).show()
        }
    }
}
