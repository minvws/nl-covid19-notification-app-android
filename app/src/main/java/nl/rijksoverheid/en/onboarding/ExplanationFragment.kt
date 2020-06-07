/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.onboarding

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.transition.TransitionInflater
import nl.rijksoverheid.en.BaseFragment
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.databinding.FragmentExplanationBinding

class ExplanationFragment : BaseFragment(R.layout.fragment_explanation) {

    private val args: ExplanationFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enterTransition = TransitionInflater.from(context).inflateTransition(android.R.transition.slide_right)
        exitTransition = TransitionInflater.from(context).inflateTransition(android.R.transition.slide_left)

        val sharedTransition = TransitionInflater.from(context).inflateTransition(android.R.transition.move)
        sharedElementEnterTransition = sharedTransition
        sharedElementReturnTransition = sharedTransition
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentExplanationBinding.bind(view)

        binding.headline.setText(args.title)
        binding.description.setText(args.description)
        binding.illustration.setImageResource(args.illustration)
        binding.illustration.contentDescription = getString(args.illustrationContentDescription)
        binding.next.setOnClickListener {
            val extras = FragmentNavigatorExtras(
                binding.next to binding.next.transitionName
            )
            findNavController().navigate(R.id.action_next, null, null, extras)
        }
    }
}
