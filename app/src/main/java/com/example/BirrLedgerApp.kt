package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.data.local.AppDatabase
import com.example.data.local.UserPreferencesRepository
import com.example.data.repository.LedgerRepository
import com.example.sms.SmsInboxScanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BirrLedgerApp : Application() {

  companion object {
    const val CHANNEL_TRANSACTIONS = "birrledger_transactions"
  }

  private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
  val preferencesRepository: UserPreferencesRepository by lazy { UserPreferencesRepository(this) }
  val repository: LedgerRepository by lazy {
    LedgerRepository(database.transactionDao(), preferencesRepository)
  }
  val smsScanner: SmsInboxScanner by lazy { SmsInboxScanner(this, repository) }

  override fun onCreate() {
    super.onCreate()
    createNotificationChannel()
    // Enforce 365-day local retention on every app launch
    appScope.launch {
      repository.pruneExpired()
    }
  }

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        CHANNEL_TRANSACTIONS,
        getString(R.string.notification_channel_name),
        NotificationManager.IMPORTANCE_DEFAULT,
      ).apply {
        description = getString(R.string.notification_channel_desc)
      }
      val manager = getSystemService(NotificationManager::class.java)
      manager?.createNotificationChannel(channel)
    }
  }
}
