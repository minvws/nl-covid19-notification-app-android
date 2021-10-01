/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.requesttest

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.navArgs
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import nl.rijksoverheid.en.BaseFragment
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.databinding.FragmentListWithTwoButtonsBinding
import nl.rijksoverheid.en.util.IntentHelper
import nl.rijksoverheid.en.util.forceLtr

/**
 * Fragment with information about how to request a Corona test.
 */
class RequestTestFragment : BaseFragment(R.layout.fragment_list_with_two_buttons) {
    private val adapter = GroupAdapter<GroupieViewHolder>()
    private val args: RequestTestFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter.add(RequestTestSection())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentListWithTwoButtonsBinding.bind(view)

        binding.toolbar.setTitle(R.string.request_test_toolbar_title)
        binding.content.adapter = adapter
        binding.button1.apply {
            val phoneNumber = args.phoneNumber
            text = getString(
                R.string.request_test_button_call,
                phoneNumber.forceLtr()
            )
            setOnClickListener {
                IntentHelper.openPhoneCallIntent(
                    this@RequestTestFragment,
                    phoneNumber,
                    RequestTestFragmentDirections.actionPhoneCallNotSupportedDialog(phoneNumber)
                )
            }
        }
        binding.button2.apply {
            setText(R.string.request_test_button_website)
            setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.test_url))))
            }
        }
    }
}
