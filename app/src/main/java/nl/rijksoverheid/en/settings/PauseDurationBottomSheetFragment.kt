/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Section
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.databinding.FragmentBottomSheetListBinding
import java.time.LocalDateTime

const val KEY_PAUSE_DURATION_RESULT = "pause_duration_result"

class PauseDurationBottomSheetFragment : BottomSheetDialogFragment() {

    private val adapter = GroupAdapter<GroupieViewHolder>().apply {
        add(
            Section(
                listOf(
                    PauseDurationItem(1),
                    PauseDurationItem(2),
                    PauseDurationItem(4),
                    PauseDurationItem(8),
                    PauseDurationItem(12)
                )
            )
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_bottom_sheet_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentBottomSheetListBinding.bind(view)

        binding.content.adapter = adapter
        adapter.setOnItemClickListener { item, _ ->
            if (item !is PauseDurationItem)
                return@setOnItemClickListener

            val until = LocalDateTime.now().plusHours(item.pauseDurationInHours.toLong())

            findNavController().previousBackStackEntry?.savedStateHandle
                ?.set(KEY_PAUSE_DURATION_RESULT, until)
            dismiss()
        }
    }
}
