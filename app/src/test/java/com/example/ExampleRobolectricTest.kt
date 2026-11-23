package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.UserPreferencesRepository
import com.example.data.repository.LedgerRepository
import com.example.sms.BankSmsParser
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun appNameMatchesBirrLedger() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("BirrLedger", appName)
  }

  @Test
  fun retentionPolicyKeepsFullYearAndErasesOlderRecords() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    val repo = LedgerRepository(db.transactionDao(), UserPreferencesRepository(context))

    val now = 1_760_000_000_000L
    val recentTime = now - TimeUnit.DAYS.toMillis(30)
    val expiredTime = now - TimeUnit.DAYS.toMillis(366)

    val recentSms = BankSmsParser.parseSms(
      sender = "CBE",
      body = "Your Account 1***1234 has been Credited with ETB 5,000.00. Current Balance is ETB 10,000.00. Ref: FTRECENT1",
      timestampMillis = recentTime,
    )!!

    val oldSms = BankSmsParser.parseSms(
      sender = "MPESA",
      body = "OLDREF123 Confirmed. ETB 200.00 sent to Shop on 01/01/25. New M-PESA balance is ETB 800.00.",
      timestampMillis = expiredTime,
    )!!

    // Insert old item directly to simulate a record that aged past 365 days
    db.transactionDao().insertTransactionIgnoreDuplicate(oldSms.transaction)
    repo.ingestParsedSms(recentSms, nowMillis = now)

    val pruned = repo.pruneExpired(nowMillis = now)
    assertEquals(1, pruned)

    val remaining = repo.transactions.first()
    assertEquals(1, remaining.size)
    assertEquals("FTRECENT1", remaining.first().referenceCode)

    db.close()
  }
}
