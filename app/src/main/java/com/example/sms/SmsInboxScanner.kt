package com.example.sms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.core.content.ContextCompat
import com.example.data.repository.LedgerRepository
import com.example.data.repository.SyncBatchOutcome
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class SmsInboxScanner(
  private val context: Context,
  private val repository: LedgerRepository,
) {

  fun hasSmsPermissions(): Boolean {
    val readGranted = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.READ_SMS,
    ) == PackageManager.PERMISSION_GRANTED
    val receiveGranted = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.RECEIVE_SMS,
    ) == PackageManager.PERMISSION_GRANTED
    return readGranted && receiveGranted
  }

  suspend fun scanInboxLastYear(): Result<SyncBatchOutcome> = withContext(Dispatchers.IO) {
    val readGranted = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.READ_SMS,
    ) == PackageManager.PERMISSION_GRANTED
    if (!readGranted) {
      return@withContext Result.failure(SecurityException("READ_SMS permission not granted"))
    }

    runCatching {
      val now = System.currentTimeMillis()
      val cutoff = LedgerRepository.oneYearCutoffMillis(now)
      val cbeKeywords = repository.cbeSenderKeywords.first()
      val mpesaKeywords = repository.mpesaSenderKeywords.first()

      val parsedResults = mutableListOf<ParsedSmsResult>()
      val projection = arrayOf(
        Telephony.Sms.ADDRESS,
        Telephony.Sms.BODY,
        Telephony.Sms.DATE,
      )
      val selection = "${Telephony.Sms.DATE} >= ?"
      val selectionArgs = arrayOf(cutoff.toString())
      val sortOrder = "${Telephony.Sms.DATE} DESC"

      context.contentResolver.query(
        Telephony.Sms.Inbox.CONTENT_URI,
        projection,
        selection,
        selectionArgs,
        sortOrder,
      )?.use { cursor ->
        val addrIdx = cursor.getColumnIndex(Telephony.Sms.ADDRESS)
        val bodyIdx = cursor.getColumnIndex(Telephony.Sms.BODY)
        val dateIdx = cursor.getColumnIndex(Telephony.Sms.DATE)

        while (cursor.moveToNext()) {
          val address = if (addrIdx >= 0) cursor.getString(addrIdx).orEmpty() else ""
          val body = if (bodyIdx >= 0) cursor.getString(bodyIdx).orEmpty() else ""
          val date = if (dateIdx >= 0) cursor.getLong(dateIdx) else now

          val parsed = BankSmsParser.parseSms(
            sender = address,
            body = body,
            timestampMillis = date,
            cbeKeywords = cbeKeywords,
            mpesaKeywords = mpesaKeywords,
            allowBodyFallback = false,
          )
          if (parsed != null) {
            parsedResults.add(parsed)
          }
        }
      }

      repository.ingestParsedBatch(parsedResults, nowMillis = now)
    }
  }
}
