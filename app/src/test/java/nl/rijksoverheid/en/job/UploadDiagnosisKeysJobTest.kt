/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.job

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import kotlinx.coroutines.runBlocking
import nl.rijksoverheid.en.labtest.LabTestRepository
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [Build.VERSION_CODES.O_MR1])
class UploadDiagnosisKeysJobTest {
    private lateinit var context: Context
    private val instant = Instant.parse("2020-06-20T10:15:30.00Z")
    private val clock = Clock.fixed(instant, ZoneId.of("Europe/Amsterdam"))

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `job schedules for next update according to value in the Initial state`() {
        val delay = AtomicLong(0)
        val worker =
            TestListenableWorkerBuilder<UploadDiagnosisKeysJob>(context).setWorkerFactory(object :
                WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters
                ): ListenableWorker? {
                    return UploadDiagnosisKeysJob(appContext, workerParameters, suspend {
                        LabTestRepository.UploadDiagnosticKeysResult.Initial(
                            LocalDateTime.now(clock).plusHours(2)
                        )
                    }, { delay.set(it) }, clock)
                }
            }).build()

        runBlocking {
            val result = (worker as CoroutineWorker).doWork()
            assertEquals(ListenableWorker.Result.success(), result)
        }

        assertEquals(TimeUnit.MILLISECONDS.convert(2, TimeUnit.HOURS), delay.get())
    }

    @Test
    fun `job schedules for next hour if the value in the Initial state is within less of an hour`() {
        val delay = AtomicLong(0)
        val worker =
            TestListenableWorkerBuilder<UploadDiagnosisKeysJob>(context).setWorkerFactory(object :
                WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters
                ): ListenableWorker? {
                    return UploadDiagnosisKeysJob(appContext, workerParameters, suspend {
                        LabTestRepository.UploadDiagnosticKeysResult.Initial(
                            LocalDateTime.now(clock).minusDays(2)
                        )
                    }, { delay.set(it) }, clock)
                }
            }).build()

        runBlocking {
            val result = (worker as CoroutineWorker).doWork()
            assertEquals(ListenableWorker.Result.success(), result)
        }

        assertEquals(TimeUnit.MILLISECONDS.convert(1, TimeUnit.HOURS), delay.get())
    }
}
