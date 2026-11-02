package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.CryptoVault
import com.example.domain.ReviewRating
import com.example.domain.Sm2Algorithm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Jameson", appName)
  }

  @Test
  fun `verify sm2 spaced repetition calculations`() {
    val result = Sm2Algorithm.calculateNextReview(
      currentRepetitions = 0,
      currentIntervalDays = 1,
      currentEaseFactor = 2.5f,
      rating = ReviewRating.GOOD,
      todayEpochDay = 1000L
    )
    assertEquals(1, result.repetitionCount)
    assertEquals(1, result.intervalDays)
    assertEquals(1001L, result.nextReviewEpochDay)
  }

  @Test
  fun `verify aes 256 crypto vault encryption and decryption`() {
    val secretNote = "Exam proof for Taylor series expansion"
    val encrypted = CryptoVault.encrypt(secretNote)
    assertTrue(CryptoVault.isEncrypted(encrypted))

    val decrypted = CryptoVault.decrypt(encrypted)
    assertEquals(secretNote, decrypted)
  }
}
