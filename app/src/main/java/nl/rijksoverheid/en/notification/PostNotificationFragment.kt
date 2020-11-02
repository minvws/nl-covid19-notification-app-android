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
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import nl.rijksoverheid.en.BaseFragment
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.databinding.FragmentListWithButtonBinding
import java.time.LocalDate

class PostNotificationFragment : BaseFragment(R.layout.fragment_list_with_button) {
    private val args: PostNotificationFragmentArgs by navArgs()

    private val viewModel: PostNotificationViewModel by viewModels()
    private val adapter = GroupAdapter<GroupieViewHolder>()
    private val section = PostNotificationSection().also {
        adapter.add(it)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.setExposureDate(LocalDate.ofEpochDay(args.epochDayOfLastExposure))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentListWithButtonBinding.bind(view)

        binding.toolbar.apply {
            setTitle(R.string.post_notification_toolbar_title)
            setNavigationIcon(R.drawable.ic_close)
            setNavigationContentDescription(R.string.cd_close)
        }
        binding.content.adapter = adapter

        viewModel.guidance.observe(viewLifecycleOwner) {
            section.setGuidance(it)
        }

        binding.button.apply {
            setText(R.string.post_notification_button)
            setOnClickListener {
                startActivity(
                    Intent(Intent.ACTION_DIAL).apply {
                        val phoneNumber = getString(R.string.phone_number)
                        data = Uri.parse("tel:$phoneNumber")
                    }
                )
            }
        }
    }
}
