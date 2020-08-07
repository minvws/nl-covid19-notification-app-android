/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import com.google.android.play.core.install.model.AppUpdateType
import nl.rijksoverheid.en.applifecycle.AppLifecycleViewModel
import nl.rijksoverheid.en.applifecycle.EndOfLifeFragmentDirections
import nl.rijksoverheid.en.debug.DebugNotification
import nl.rijksoverheid.en.job.RemindExposureNotificationWorker
import nl.rijksoverheid.en.lifecyle.EventObserver
import nl.rijksoverheid.en.notifier.NotificationsRepository

private const val RC_REQUEST_CONSENT = 1
private const val RC_UPDATE_APP = 2
private const val TAG_GENERIC_ERROR = "generic_error"

class MainActivity : AppCompatActivity() {
    private val viewModel: ExposureNotificationsViewModel by viewModels()
    private val appLifecycleViewModel: AppLifecycleViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE

        viewModel.notificationsResult.observe(
            this,
            EventObserver {
                when (it) {
                    is ExposureNotificationsViewModel.NotificationsStatusResult.ConsentRequired -> {
                        startIntentSenderForResult(
                            it.intent.intentSender,
                            RC_REQUEST_CONSENT,
                            null,
                            0, 0, 0
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

        appLifecycleViewModel.updateEvent.observe(
            this,
            EventObserver {
                when (it) {
                    is AppLifecycleViewModel.AppLifecycleStatus.Update -> {
                        it.update.appUpdateManager.startUpdateFlowForResult(
                            it.update.appUpdateInfo,
                            AppUpdateType.IMMEDIATE,
                            this,
                            RC_UPDATE_APP
                        )
                    }
                    AppLifecycleViewModel.AppLifecycleStatus.EndOfLife -> findNavController(R.id.nav_host_fragment).navigate(
                        EndOfLifeFragmentDirections.actionEndOfLife()
                    )
                }
            }
        )

        if (BuildConfig.FEATURE_DEBUG_NOTIFICATION) {
            DebugNotification(this).show()
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        findNavController(R.id.nav_host_fragment).addOnDestinationChangedListener(
            SecureScreenNavigationListener(window, R.id.nav_status, R.id.nav_post_notification)
        )
    }

    override fun onStart() {
        super.onStart()
        NotificationsRepository(this).clearAppInactiveNotification()
        RemindExposureNotificationWorker.cancel(this)
    }

    override fun onResume() {
        super.onResume()
        appLifecycleViewModel.checkForForcedAppUpdate()
    }

    override fun getDefaultViewModelProviderFactory(): ViewModelProvider.Factory {
        return ViewModelFactory(applicationContext)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_REQUEST_CONSENT && resultCode == Activity.RESULT_OK) {
            viewModel.requestEnableNotifications()
        }
        // If user canceled the forced update, do not allow them to use the app
        if (requestCode == RC_UPDATE_APP && resultCode != Activity.RESULT_OK) {
            finish()
        }
    }
}
