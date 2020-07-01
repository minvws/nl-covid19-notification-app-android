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
import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import androidx.annotation.StringRes
import androidx.core.app.SharedElementCallback
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.transition.TransitionInflater
import nl.rijksoverheid.en.BaseFragment
import nl.rijksoverheid.en.R
import nl.rijksoverheid.en.databinding.FragmentExplanationBinding

class ExplanationFragment : BaseFragment(R.layout.fragment_explanation) {
    data class ViewState(
        @StringRes val headline: Int,
        @StringRes val description: Int,
        @DrawableRes val illustration: Int,
        @RawRes val animation: Int,
        val isExample: Boolean
    )

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
                        val appbar = sharedElements.firstOrNull { it.id == R.id.appbar }
                        // hide the appbar from the previous fragment to make the appbar from this fragment
                        // fade in.
                        appbar?.visibility = View.INVISIBLE
                    }
                }

                override fun onSharedElementEnd(
                    sharedElementNames: MutableList<String>,
                    sharedElements: MutableList<View>,
                    sharedElementSnapshots: MutableList<View>?
                ) {
                    val appbar = sharedElements.firstOrNull { it.id == R.id.appbar }
                    appbar?.visibility = View.VISIBLE
                }
            })
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentExplanationBinding.bind(view)

        binding.viewState = ViewState(
            headline = args.title,
            description = args.description,
            illustration = args.illustration,
            animation = args.animation,
            isExample = args.isExample
        )

        binding.nextButtonClickListener = View.OnClickListener {
            findNavController().navigate(
                ExplanationFragmentDirections.actionNext(), FragmentNavigatorExtras(
                    binding.appbar to binding.appbar.transitionName
                )
            )
        }

        // Don't use enter transitions for the first screen in the onboarding graph
        val navController = findNavController()
        if (navController.currentDestination?.id == navController.currentDestination?.parent?.startDestination) {
            enterTransition = null
            binding.toolbar.navigationIcon = null
            activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner) {
                activity?.finish()
            }
        }
    }
}
