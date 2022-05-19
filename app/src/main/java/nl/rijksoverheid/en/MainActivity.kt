/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en

import android.app.Activity
import android.os.Bundle
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import dev.chrisbanes.insetter.applyInsetter
import nl.rijksoverheid.en.applifecycle.AppLifecycleManager
import nl.rijksoverheid.en.applifecycle.AppLifecycleViewModel
import nl.rijksoverheid.en.applifecycle.AppUpdateRequiredFragmentDirections
import nl.rijksoverheid.en.applifecycle.EndOfLifeFragmentDirections
import nl.rijksoverheid.en.applifecycle.NoInternetFragmentDirections
import nl.rijksoverheid.en.databinding.ActivityMainBinding
import nl.rijksoverheid.en.databinding.ActivityMainBinding.inflate
import nl.rijksoverheid.en.debug.DebugNotification
import nl.rijksoverheid.en.job.RemindExposureNotificationWorker
import nl.rijksoverheid.en.lifecyle.EventObserver
import nl.rijksoverheid.en.navigation.isInitialised
import nl.rijksoverheid.en.notifier.NotificationsRepository
import timber.log.Timber

private const val TAG_GENERIC_ERROR = "generic_error"

class MainActivity : AppCompatActivity() {
    private val viewModel: ExposureNotificationsViewModel by viewModels()
    private val appLifecycleViewModel: AppLifecycleViewModel by viewModels()

    private lateinit var binding: ActivityMainBinding

    private val requestConsent =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) viewModel.requestEnableNotifications()
        }

    private val navController get() = findNavController(R.id.nav_host_fragment)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        splashScreen.setKeepOnScreenCondition {
            appLifecycleViewModel.splashScreenKeepOnScreenCondition
        }

        binding = inflate(layoutInflater)
        setContentView(binding.root)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding.root.applyInsetter {
            type(navigationBars = true) {
                padding()
            }
        }

        viewModel.notificationsResult.observe(
            this,
            EventObserver {
                when (it) {
                    is ExposureNotificationsViewModel.NotificationsStatusResult.ConsentRequired -> {
                        requestConsent.launch(
                            IntentSenderRequest.Builder(it.intent.intentSender).build()
                        )
                    }
                    is ExposureNotificationsViewModel.NotificationsStatusResult.Unavailable,
                    is ExposureNotificationsViewModel.NotificationsStatusResult.UnknownError -> {
                        if (supportFragmentManager.findFragmentByTag(TAG_GENERIC_ERROR) == null) {
                            ExposureNotificationApiNotAvailableDialogFragment().show(
                                supportFragmentManager,
                                TAG_GENERIC_ERROR
                            )
                        }
                    }
                }
            }
        )

        appLifecycleViewModel.appLifecycleStatus.observe(this) {
            when (it) {
                is AppLifecycleViewModel.AppLifecycleStatus.Update ->
                    handleUpdateState(it.update)
                is AppLifecycleViewModel.AppLifecycleStatus.EndOfLife ->
                    handleEndOfLifeState()
                is AppLifecycleViewModel.AppLifecycleStatus.UnableToFetchAppConfig ->
                    handleUnableToFetchAppConfig()
                else -> {
                    if (!navController.isInitialised())
                        inflateNavGraph()
                }
            }
        }

        if (BuildConfig.FEATURE_DEBUG_NOTIFICATION) {
            DebugNotification(this).show()
        }
    }

    private fun handleUpdateState(update: AppLifecycleManager.UpdateState) {
        if (update is AppLifecycleManager.UpdateState.InAppUpdate) {
            if (!navController.isInitialised())
                inflateNavGraph()

            update.appUpdateManager.startUpdateFlow(
                update.appUpdateInfo,
                this, AppUpdateOptions.defaultOptions(AppUpdateType.IMMEDIATE)
            ).addOnCompleteListener { task ->
                Timber.d("App update result: ${task.result}")
                if (task.result != Activity.RESULT_OK) {
                    finish()
                }
            }
        } else {
            if (!navController.isInitialised())
                inflateNavGraph(R.id.nav_app_update_required)
            else {
                val installerPackageName =
                    (update as AppLifecycleManager.UpdateState.UpdateRequired).installerPackageName
                navController.navigate(
                    AppUpdateRequiredFragmentDirections.actionAppUpdateRequired(
                        installerPackageName
                    )
                )
            }
        }
    }

    private fun inflateNavGraph(startDestinationId: Int? = null) {
        val graph = navController.navInflater.inflate(R.navigation.nav_main)
        startDestinationId?.let { graph.setStartDestination(it) }
        navController.graph = graph
    }

    private fun handleEndOfLifeState() {
        viewModel.disableExposureNotifications()
        if (!navController.isInitialised())
            inflateNavGraph(R.id.nav_end_of_life)
        else
            navController.navigate(EndOfLifeFragmentDirections.actionEndOfLife())
    }

    private fun handleUnableToFetchAppConfig() {
        if (!navController.isInitialised())
            inflateNavGraph(R.id.nav_no_internet)
        else
            navController.navigate(
                NoInternetFragmentDirections.actionNoInternet()
            )
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        if (BuildConfig.FEATURE_SECURE_SCREEN) {
            navController.addOnDestinationChangedListener(
                SecureScreenNavigationListener(
                    window,
                    R.id.nav_status,
                    R.id.nav_post_notification,
                    R.id.nav_remove_exposed_message_dialog
                )
            )
        }
    }

    override fun onStart() {
        super.onStart()
        NotificationsRepository(this).clearAppInactiveNotification()
        RemindExposureNotificationWorker.cancel(this)
    }

    override fun onResume() {
        super.onResume()
        appLifecycleViewModel.checkAppLifecycleStatus()
    }

    override fun getDefaultViewModelProviderFactory(): ViewModelProvider.Factory {
        return ViewModelFactory(applicationContext)
    }
}
