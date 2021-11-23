/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.util.ext

import androidx.fragment.app.Fragment
import androidx.transition.TransitionInflater
import nl.rijksoverheid.en.R

fun Fragment.setSlideTransition() {
    enterTransition = TransitionInflater.from(requireContext()).inflateTransition(R.transition.slide_end)
    exitTransition = TransitionInflater.from(requireContext()).inflateTransition(R.transition.slide_start)

    sharedElementEnterTransition =
        TransitionInflater.from(requireContext()).inflateTransition(R.transition.move_fade)
    sharedElementReturnTransition = sharedElementEnterTransition
}

fun Fragment.setSlideTransitionWithoutReturnTransition() {
    enterTransition = TransitionInflater.from(requireContext()).inflateTransition(R.transition.slide_end)
    sharedElementEnterTransition =
        TransitionInflater.from(requireContext()).inflateTransition(R.transition.move_fade)
    returnTransition = null
    sharedElementReturnTransition = null
}

fun Fragment.setExitSlideTransition() {
    exitTransition = TransitionInflater.from(requireContext()).inflateTransition(R.transition.slide_start)
}
