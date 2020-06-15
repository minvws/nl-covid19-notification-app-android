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
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import nl.rijksoverheid.en.BaseFragment
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.databinding.FragmentListBinding
import java.time.Clock
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter

class PostNotificationFragment(
    private val clock: Clock = Clock.systemDefaultZone()
) : BaseFragment(R.layout.fragment_list) {
    private val args: PostNotificationFragmentArgs by navArgs()

    private val adapter = GroupAdapter<GroupieViewHolder>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val exposureDate = LocalDate.ofEpochDay(args.epochDayOfLastExposure)

        val daysSince = Period.between(exposureDate, LocalDate.now(clock)).days
        val daysSinceString =
            requireContext().resources.getQuantityString(R.plurals.days, daysSince, daysSince)

        val phoneNumber = getString(R.string.phone_number)
        adapter.add(
            PostNotificationSection(
                onCallClicked = {
                    startActivity(Intent(Intent.ACTION_DIAL).apply {
                        data = Uri.parse("tel:$phoneNumber")
                    })
                },
                daysSince = daysSinceString,
                date = DateTimeFormatter.ofPattern("EEEE d MMMM").format(exposureDate),
                phoneNumber = phoneNumber
            )
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentListBinding.bind(view)

        binding.toolbar.apply {
            setTitle(R.string.post_notification_toolbar_title)
            setNavigationIcon(R.drawable.ic_close)
            setNavigationOnClickListener { findNavController().popBackStack() }
        }
        binding.content.adapter = adapter
    }
}
