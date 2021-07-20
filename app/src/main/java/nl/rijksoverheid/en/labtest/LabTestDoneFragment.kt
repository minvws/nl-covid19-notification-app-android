/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.labtest

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.coroutineScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import nl.rijksoverheid.en.BaseFragment
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.databinding.FragmentListBinding
import nl.rijksoverheid.en.util.setSlideTransitionWithoutReturnTransition

class LabTestDoneFragment : BaseFragment(R.layout.fragment_list) {
    private val args: LabTestDoneFragmentArgs by navArgs()
    private val adapter = GroupAdapter<GroupieViewHolder>()

    private val labTestDoneViewModel: LabTestDoneViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setSlideTransitionWithoutReturnTransition()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentListBinding.bind(view)

        binding.toolbar.apply {
            setTitle(R.string.lab_test_done_toolbar_title)
            setNavigationIcon(R.drawable.ic_close)
            setNavigationContentDescription(R.string.cd_close)
        }

        viewLifecycleOwner.lifecycle.coroutineScope.launchWhenResumed {
            val hasIndependentKeySharing = labTestDoneViewModel.hasIndependentKeySharing()
            adapter.add(
                LabTestDoneSection(args.generatedKey, hasIndependentKeySharing) {
                    findNavController().popBackStack()
                }
            )
        }

        binding.content.adapter = adapter
    }
}
