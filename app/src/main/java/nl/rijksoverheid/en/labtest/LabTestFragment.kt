/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.labtest

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.IntentSender
import android.graphics.Rect
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.TransitionInflater
import com.google.android.material.snackbar.Snackbar
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import nl.rijksoverheid.en.BaseFragment
import nl.rijksoverheid.en.ExposureNotificationsViewModel
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.about.FAQItemId
import nl.rijksoverheid.en.databinding.FragmentListWithButtonBinding
import nl.rijksoverheid.en.lifecyle.observeEvent
import nl.rijksoverheid.en.navigation.navigateCatchingErrors
import timber.log.Timber

class LabTestFragment : BaseFragment(R.layout.fragment_list_with_button) {
    private val labViewModel: LabTestViewModel by viewModels()
    private val viewModel: ExposureNotificationsViewModel by activityViewModels()
    private val section = LabTestSection(
        retry = { labViewModel.retry() },
        requestConsent = { viewModel.requestEnableNotifications() },
        copy = ::copyToClipboard
    )
    private val adapter = GroupAdapter<GroupieViewHolder>().apply { add(section) }

    private val requestUploadConsent =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) labViewModel.upload()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        exitTransition =
            TransitionInflater.from(context).inflateTransition(R.transition.slide_start)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentListWithButtonBinding.bind(view)

        binding.toolbar.apply {
            setTitle(R.string.lab_test_toolbar_title)
        }
        binding.content.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(
                outRect: Rect,
                view: View,
                parent: RecyclerView,
                state: RecyclerView.State
            ) {
                super.getItemOffsets(outRect, view, parent, state)
                if (parent.getChildAdapterPosition(view) == 0) {
                    outRect.bottom =
                        resources.getDimensionPixelOffset(R.dimen.space_below_illustration)
                }
            }
        })
        binding.content.adapter = adapter

        adapter.setOnItemClickListener { _, _ ->
            findNavController().navigateCatchingErrors(
                LabTestFragmentDirections.actionHowItWorks(FAQItemId.UPLOAD_KEYS),
                FragmentNavigatorExtras(binding.appbar to binding.appbar.transitionName)
            )
        }

        labViewModel.keyState.observe(viewLifecycleOwner) { keyState ->
            section.update(keyState)
            binding.button.isEnabled =
                getContinueButtonEnabledState(keyState, section.notificationsState)
        }
        viewModel.notificationState.observe(viewLifecycleOwner) { notificationsState ->
            section.update(notificationsState)
            binding.button.isEnabled =
                getContinueButtonEnabledState(section.keyState, notificationsState)
        }

        labViewModel.uploadResult.observeEvent(viewLifecycleOwner) {
            when (it) {
                is LabTestViewModel.UploadResult.Success -> findNavController().navigateCatchingErrors(
                    LabTestFragmentDirections.actionLabTestDone(it.usedKey),
                    FragmentNavigatorExtras(binding.appbar to binding.appbar.transitionName)
                )
                is LabTestViewModel.UploadResult.RequestConsent -> requestConsent(it.resolution.intentSender)
                LabTestViewModel.UploadResult.Error -> {
                    Toast.makeText(context, R.string.lab_test_upload_error, Toast.LENGTH_LONG)
                        .show()
                }
            }
        }

        binding.button.apply {
            setText(R.string.lab_test_button)
            setOnClickListener {
                labViewModel.upload()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        labViewModel.retry()
    }

    private fun getContinueButtonEnabledState(
        keyState: LabTestViewModel.KeyState,
        notificationsState: ExposureNotificationsViewModel.NotificationsState
    ): Boolean {

        return keyState is LabTestViewModel.KeyState.Success &&
            notificationsState in listOf(
            ExposureNotificationsViewModel.NotificationsState.Enabled,
            ExposureNotificationsViewModel.NotificationsState.BluetoothDisabled,
            ExposureNotificationsViewModel.NotificationsState.LocationPreconditionNotSatisfied
        )
    }

    private fun requestConsent(intentSender: IntentSender) {
        try {
            requestUploadConsent.launch(IntentSenderRequest.Builder(intentSender).build())
        } catch (ex: Exception) {
            Timber.e(ex, "Error requesting consent")
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
