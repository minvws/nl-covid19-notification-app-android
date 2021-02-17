/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.test

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.annotation.StyleRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.FragmentFactory
import androidx.fragment.app.commitNow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavHostController
import androidx.navigation.Navigation
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import dev.chrisbanes.insetter.applyInsetter

private const val KEY_THEME_RES = "theme"

class TestMainActivity : AppCompatActivity() {
    /**
     * Holds the supplied factories
     */
    private class FactoryHolderViewModel : ViewModel() {
        var viewModelFactory: ViewModelProvider.Factory? = null
        var fragmentFactory: FragmentFactory? = null
        var viewId: Int = View.generateViewId()
    }

    private val viewModel by viewModels<FactoryHolderViewModel> {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return FactoryHolderViewModel() as T
            }
        }
    }

    var viewModelFactory: ViewModelProvider.Factory?
        get() = viewModel.viewModelFactory
        set(value) {
            viewModel.viewModelFactory = value
        }

    var fragmentFactory: FragmentFactory?
        get() = viewModel.fragmentFactory
        set(value) {
            viewModel.fragmentFactory = value
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        val theme = intent.getIntExtra(KEY_THEME_RES, 0)
        if (theme != 0) {
            setTheme(theme)
        }
        viewModel.fragmentFactory?.let { supportFragmentManager.fragmentFactory = it }
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContentView(
            FragmentContainerView(this).apply {
                this.id = viewModel.viewId
                applyInsetter {
                    type(navigationBars = true) {
                        padding()
                    }
                }
            }
        )
    }

    override fun getDefaultViewModelProviderFactory(): ViewModelProvider.Factory {
        return viewModel.viewModelFactory ?: super.getDefaultViewModelProviderFactory()
    }

    companion object {
        inline fun <reified T : Fragment> launchWithFragment(
            args: Bundle? = null,
            @StyleRes theme: Int? = null,
            activityViewModelFactory: ViewModelProvider.Factory? = null,
            crossinline factory: () -> T
        ): ActivityScenario<TestMainActivity> {
            return launchWithFragment(
                T::class.java,
                args,
                theme,
                activityViewModelFactory,
                object : FragmentFactory() {
                    override fun instantiate(
                        classLoader: ClassLoader,
                        className: String
                    ): Fragment {
                        return if (className == T::class.java.name) {
                            factory()
                        } else {
                            super.instantiate(classLoader, className)
                        }
                    }
                }
            )
        }

        fun <T : Fragment> launchWithFragment(
            fragmentClass: Class<T>,
            args: Bundle?,
            @StyleRes theme: Int? = null,
            activityViewModelFactory: ViewModelProvider.Factory? = null,
            fragmentFactory: FragmentFactory? = null
        ): ActivityScenario<TestMainActivity> {
            val context = ApplicationProvider.getApplicationContext<Application>()

            return ActivityScenario.launch<TestMainActivity>(
                Intent(context, TestMainActivity::class.java).apply {
                    theme?.let {
                        putExtra(KEY_THEME_RES, it)
                    }
                }
            ).onActivity { activity ->
                activity.fragmentFactory = fragmentFactory
                activity.viewModelFactory = activityViewModelFactory
                fragmentFactory?.let { activity.supportFragmentManager.fragmentFactory = it }
                activity.supportFragmentManager.commitNow {
                    add(activity.viewModel.viewId, fragmentClass, args)
                }
            }
        }
    }
}

fun withFragment(
    fragment: Fragment,
    navController: NavHostController,
    @StyleRes theme: Int,
    activityViewModelFactory: ViewModelProvider.Factory? = null,
    block: () -> Unit
) {
    TestMainActivity.launchWithFragment(
        theme = theme,
        activityViewModelFactory = activityViewModelFactory
    ) {
        fragment.apply {
            arguments = navController.currentBackStackEntry?.arguments
        }.also { fragment ->
            fragment.viewLifecycleOwnerLiveData.observeForever {
                if (it != null) {
                    Navigation.setViewNavController(fragment.requireView(), navController)
                }
            }
        }
    }.use { block() }
}
