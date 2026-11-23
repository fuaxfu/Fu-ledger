package com.example.sms

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Telephony
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.BirrLedgerApp
import com.example.MainActivity
import com.example.R
import com.example.data.model.TransactionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class SmsBroadcastReceiver : BroadcastReceiver() {

  override fun onReceive(context: Context, intent: Intent) {
    if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

    val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
    if (messages.isEmpty()) return

    val groupedBySender = linkedMapOf<String, Pair<StringBuilder, Long>>()
    for (sms in messages) {
      val sender = sms.displayOriginatingAddress ?: sms.originatingAddress ?: ""
      val timestamp = sms.timestampMillis.takeIf { it > 0L } ?: System.currentTimeMillis()
      val entry = groupedBySender.getOrPut(sender) { StringBuilder() to timestamp }
      entry.first.append(sms.messageBody.orEmpty())
    }

    val pendingResult = goAsync()
    val app = context.applicationContext as? BirrLedgerApp
    if (app == null) {
      pendingResult.finish()
      return
    }

    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
      try {
        val repo = app.repository
        val cbeKeywords = repo.cbeSenderKeywords.first()
        val mpesaKeywords = repo.mpesaSenderKeywords.first()
        val notifyEnabled = repo.notificationsEnabled.first()

        for ((sender, pair) in groupedBySender) {
          val (bodyBuilder, timestamp) = pair
          val parsed = BankSmsParser.parseSms(
            sender = sender,
            body = bodyBuilder.toString(),
            timestampMillis = timestamp,
            cbeKeywords = cbeKeywords,
            mpesaKeywords = mpesaKeywords,
            allowBodyFallback = false,
          )
          if (parsed != null) {
            val inserted = repo.ingestParsedSms(parsed)
            repo.pruneExpired()
            if (inserted && notifyEnabled) {
              postLocalNotification(context, parsed)
            }
          }
        }
      } finally {
        pendingResult.finish()
      }
    }
  }

  private fun postLocalNotification(context: Context, parsed: ParsedSmsResult) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val granted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS,
      ) == PackageManager.PERMISSION_GRANTED
      if (!granted) return
    }

    val tx = parsed.transaction
    val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
      minimumFractionDigits = 2
      maximumFractionDigits = 2
    }
    val sign = if (tx.type == TransactionType.EXPENSE) "-" else "+"
    val title = "${tx.source.shortBadge} ${tx.type.label} Logged"
    val bodyText = "$sign${tx.currency} ${formatter.format(tx.amount)} • ${tx.counterparty} (${tx.category.displayName})"

    val launchIntent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val pendingIntent = PendingIntent.getActivity(
      context,
      tx.smsSignature.hashCode(),
      launchIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val notification = NotificationCompat.Builder(context, BirrLedgerApp.CHANNEL_TRANSACTIONS)
      .setSmallIcon(R.mipmap.ic_launcher_round)
      .setContentTitle(title)
      .setContentText(bodyText)
      .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
      .setPriority(NotificationCompat.PRIORITY_DEFAULT)
      .setAutoCancel(true)
      .setContentIntent(pendingIntent)
      .build()

    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    manager?.notify(tx.smsSignature.hashCode(), notification)
  }
}
