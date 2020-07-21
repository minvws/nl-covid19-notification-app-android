/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.notification

import android.os.Bundle
import android.view.View
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import nl.rijksoverheid.en.BaseFragment
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.databinding.FragmentListBinding

class UploadKeysFailedNotificationFragment : BaseFragment(R.layout.fragment_list) {
    private val adapter = GroupAdapter<GroupieViewHolder>().apply {
        add(UploadKeysFailedNotificationSection())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentListBinding.bind(view)

        binding.toolbar.apply {
            setTitle(R.string.upload_keys_failed_notification_toolbar_title)
            setNavigationIcon(R.drawable.ic_close)
            setNavigationContentDescription(R.string.cd_close)
        }
        binding.content.adapter = adapter
    }
}
