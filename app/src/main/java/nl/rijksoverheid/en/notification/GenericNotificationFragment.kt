/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.notification

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import nl.rijksoverheid.en.BaseFragment
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.databinding.FragmentListWithButtonBinding
import nl.rijksoverheid.en.util.forceLtr

class GenericNotificationFragment : BaseFragment(R.layout.fragment_list_with_button) {
    private val adapter = GroupAdapter<GroupieViewHolder>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter.add(GenericNotificationSection(phoneNumber = getString(R.string.phone_number).forceLtr()))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentListWithButtonBinding.bind(view)

        binding.toolbar.apply {
            setTitle(R.string.generic_notification_toolbar_title)
        }
        binding.content.adapter = adapter
        binding.button.apply {
            setText(R.string.post_notification_button)
            setOnClickListener {
                startActivity(Intent(Intent.ACTION_DIAL).apply {
                    val phoneNumber = getString(R.string.phone_number)
                    data = Uri.parse("tel:$phoneNumber")
                })
            }
        }
    }
}
