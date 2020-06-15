/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.onboarding

import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.core.app.SharedElementCallback
import androidx.core.view.isVisible
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

        enterTransition = TransitionInflater.from(context).inflateTransition(R.transition.slide_end)
        exitTransition =
            TransitionInflater.from(context).inflateTransition(R.transition.slide_start)

        sharedElementEnterTransition =
            TransitionInflater.from(context).inflateTransition(R.transition.move_fade)
        sharedElementReturnTransition = sharedElementEnterTransition

        if (args.fromFirstPage) {
            setEnterSharedElementCallback(object : SharedElementCallback() {
                override fun onSharedElementStart(
                    sharedElementNames: MutableList<String>,
                    sharedElements: MutableList<View>,
                    sharedElementSnapshots: MutableList<View>?
                ) {
                    if (sharedElements.isNotEmpty()) {
                        val toolbar = sharedElements.firstOrNull { it.id == R.id.toolbar }
                        // hide the toolbar from the previous fragment to make the toolbar from this fragment
                        // fade in.
                        toolbar?.visibility = View.INVISIBLE
                    }
                }

                override fun onSharedElementEnd(
                    sharedElementNames: MutableList<String>,
                    sharedElements: MutableList<View>,
                    sharedElementSnapshots: MutableList<View>?
                ) {
                    val toolbar = sharedElements.firstOrNull { it.id == R.id.toolbar }
                    toolbar?.visibility = View.VISIBLE
                }
            })
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentExplanationBinding.bind(view)

        binding.headline.setText(args.title)
        binding.description.setText(args.description)
        binding.illustration.setImageResource(args.illustration)
        binding.example.isVisible = args.isExample
        binding.toolbar.setNavigationOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }

        val navController = findNavController()

        // Don't use enter transitions for the first screen in the onboarding graph
        if (navController.currentDestination?.id == navController.currentDestination?.parent?.startDestination) {
            enterTransition = null
            binding.toolbar.navigationIcon = null
            activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner) {
                activity?.finish()
            }
        }

        binding.next.setOnClickListener {
            navController.navigate(
                ExplanationFragmentDirections.actionNext(), FragmentNavigatorExtras(
                    binding.toolbar to binding.toolbar.transitionName
                )
            )
        }
    }
}
