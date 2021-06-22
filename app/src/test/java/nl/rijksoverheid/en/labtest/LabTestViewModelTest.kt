/*
 * Copyright (c) 2020 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 */
package nl.rijksoverheid.en.labtest

import android.app.PendingIntent
import kotlinx.coroutines.runBlocking
import nl.rijksoverheid.en.config.AppConfigManager
import nl.rijksoverheid.en.util.observeForTesting
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LabTestViewModelTest {

    @Mock
    private lateinit var labTestRepository: LabTestRepository

    @Mock
    private lateinit var appConfigManager: AppConfigManager

    private lateinit var closeable: AutoCloseable

    @Before
    fun openMocks() {
        closeable = MockitoAnnotations.openMocks(this)
    }

    @After
    fun releaseMocks() {
        closeable.close()
    }

    @Test
    fun `retry triggers a registerForUpload and updates keyState`() {
        val key = "TEST-KEY"
        val labTestViewModelTest = LabTestViewModel(labTestRepository, appConfigManager)

        runBlocking {
            Mockito.`when`(labTestRepository.registerForUpload())
                .thenReturn(RegistrationResult.Success(key))

            labTestViewModelTest.retry()

            labTestViewModelTest.keyState.observeForTesting {
                Assert.assertEquals(
                    listOf(LabTestViewModel.KeyState.Loading, LabTestViewModel.KeyState.Success(key)),
                    it.values
                )
            }
        }
    }

    @Test
    fun `usedKey is used for uploadResult`() {
        val key = "TEST-KEY"
        val labTestViewModelTest = LabTestViewModel(labTestRepository, appConfigManager)

        runBlocking {
            Mockito.`when`(labTestRepository.registerForUpload())
                .thenReturn(RegistrationResult.Success(key))
            Mockito.`when`(labTestRepository.requestUploadDiagnosticKeys())
                .thenReturn(LabTestRepository.RequestUploadDiagnosisKeysResult.Success)

            labTestViewModelTest.retry()

            labTestViewModelTest.keyState.observeForTesting {
                labTestViewModelTest.upload()
            }

            labTestViewModelTest.uploadResult.observeForTesting {
                Assert.assertEquals(
                    LabTestViewModel.UploadResult.Success(key),
                    it.values.first().getContentIfNotHandled()
                )
            }
        }
    }

    @Test
    fun `Error uploadResult from requestUploadDiagnosticKeys`() {
        val labTestViewModelTest = LabTestViewModel(labTestRepository, appConfigManager)

        runBlocking {
            Mockito.`when`(labTestRepository.requestUploadDiagnosticKeys())
                .thenReturn(LabTestRepository.RequestUploadDiagnosisKeysResult.UnknownError)

            labTestViewModelTest.upload()

            labTestViewModelTest.uploadResult.observeForTesting {
                Assert.assertEquals(
                    LabTestViewModel.UploadResult.Error,
                    it.values.first().getContentIfNotHandled()
                )
            }
        }
    }

    @Test
    fun `RequireConsent resolution intent used in uploadResult`() {
        val pendingIntent: PendingIntent = mock(PendingIntent::class.java)
        val labTestViewModelTest = LabTestViewModel(labTestRepository, appConfigManager)

        runBlocking {
            Mockito.`when`(labTestRepository.requestUploadDiagnosticKeys())
                .thenReturn(LabTestRepository.RequestUploadDiagnosisKeysResult.RequireConsent(pendingIntent))

            labTestViewModelTest.upload()

            labTestViewModelTest.uploadResult.observeForTesting {
                Assert.assertEquals(
                    LabTestViewModel.UploadResult.RequestConsent(pendingIntent),
                    it.values.first().getContentIfNotHandled()
                )
            }
        }
    }

    @Test
    fun `LabTestViewModel KeyState Success has correct key and displayKey for code with dashes`() {
        val code = "TE-ST-KE"
        val successKeyState = LabTestViewModel.KeyState.Success(code)
        Assert.assertEquals("TE-ST-KE", successKeyState.displayKey)
        Assert.assertEquals("TESTKE", successKeyState.key)
    }

    @Test
    fun `LabTestViewModel KeyState Success has correct key and displayKey for code without dashes`() {
        val code = "TESTKEY"
        val successKeyState = LabTestViewModel.KeyState.Success(code)
        Assert.assertEquals("TES-TK-EY", successKeyState.displayKey)
        Assert.assertEquals("TESTKEY", successKeyState.key)
    }
}
