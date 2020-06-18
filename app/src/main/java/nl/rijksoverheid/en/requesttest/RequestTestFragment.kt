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
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import nl.rijksoverheid.en.BaseFragment
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.databinding.FragmentListBinding

class RequestTestFragment : BaseFragment(R.layout.fragment_list) {
    private val adapter = GroupAdapter<GroupieViewHolder>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val phoneNumber = getString(R.string.phone_number)
        adapter.add(
            RequestTestSection(
                onCallClicked = {
                    startActivity(Intent(Intent.ACTION_DIAL).apply {
                        data = Uri.parse("tel:$phoneNumber")
                    })
                },
                phoneNumber = phoneNumber
            )
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentListBinding.bind(view)

        binding.toolbar.apply {
            setTitle(R.string.request_test_toolbar_title)
            setNavigationOnClickListener { activity?.onBackPressedDispatcher?.onBackPressed() }
        }
        binding.content.adapter = adapter
    }
}
