package com.example.data.repository

import com.example.data.local.TransactionDao
import com.example.data.local.UserPreferencesRepository
import com.example.data.model.AccountBalanceEntity
import com.example.data.model.ExpenseCategory
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionSource
import com.example.data.model.TransactionType
import com.example.sms.ParsedSmsResult
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import java.util.concurrent.TimeUnit

class LedgerRepository(
  private val dao: TransactionDao,
  private val prefs: UserPreferencesRepository,
) {
  companion object {
    const val RETENTION_DAYS = 365L

    fun oneYearCutoffMillis(nowMillis: Long = System.currentTimeMillis()): Long =
      nowMillis - TimeUnit.DAYS.toMillis(RETENTION_DAYS)
  }

  val transactions: Flow<List<TransactionEntity>> = dao.observeAllTransactions()
  val balances: Flow<List<AccountBalanceEntity>> = dao.observeBalances()
  val transactionCount: Flow<Int> = dao.observeTransactionCount()
  val oldestTimestamp: Flow<Long?> = dao.observeOldestTimestamp()

  val cbeSenderKeywords: Flow<Set<String>> = prefs.cbeSenderKeywords
  val mpesaSenderKeywords: Flow<Set<String>> = prefs.mpesaSenderKeywords
  val notificationsEnabled: Flow<Boolean> = prefs.notificationsEnabled
  val lastSyncMillis: Flow<Long> = prefs.lastSyncMillis
  val lastPrunedCount: Flow<Int> = prefs.lastPrunedCount

  val biometricLockEnabled: Flow<Boolean> = prefs.biometricLockEnabled
  val lockOnBackground: Flow<Boolean> = prefs.lockOnBackground
  val hideBalancesByDefault: Flow<Boolean> = prefs.hideBalancesByDefault
  val showDecimals: Flow<Boolean> = prefs.showDecimals
  val preferredCurrency: Flow<String> = prefs.preferredCurrency
  val autoSyncOnLaunch: Flow<Boolean> = prefs.autoSyncOnLaunch
  val themeMode: Flow<com.example.data.local.ThemeMode> = prefs.themeMode
  val colorTheme: Flow<com.example.data.local.AppColorTheme> = prefs.colorTheme

  suspend fun ingestParsedSms(
    parsed: ParsedSmsResult,
    nowMillis: Long = System.currentTimeMillis(),
  ): Boolean {
    val cutoff = oneYearCutoffMillis(nowMillis)
    if (parsed.transaction.timestampMillis < cutoff) {
      return false
    }
    val rowId = dao.insertTransactionIgnoreDuplicate(parsed.transaction)
    syncBalanceIfNewer(parsed)
    return rowId != -1L
  }

  suspend fun ingestParsedBatch(
    results: List<ParsedSmsResult>,
    nowMillis: Long = System.currentTimeMillis(),
  ): SyncBatchOutcome {
    val cutoff = oneYearCutoffMillis(nowMillis)
    var inserted = 0
    for (item in results) {
      if (item.transaction.timestampMillis >= cutoff) {
        val rowId = dao.insertTransactionIgnoreDuplicate(item.transaction)
        if (rowId != -1L) inserted++
        syncBalanceIfNewer(item)
      }
    }
    val pruned = pruneExpired(nowMillis)
    prefs.recordSyncResult(syncMillis = nowMillis, prunedCount = pruned)
    return SyncBatchOutcome(insertedCount = inserted, prunedCount = pruned)
  }

  private suspend fun syncBalanceIfNewer(parsed: ParsedSmsResult) {
    val bal = parsed.transaction.balanceAfter ?: return
    val source = parsed.transaction.source
    if (source == TransactionSource.MANUAL) return

    val existing = dao.getBalanceForSource(source)
    if (existing == null || parsed.transaction.timestampMillis >= existing.lastSyncedMillis) {
      dao.upsertBalance(
        AccountBalanceEntity(
          source = source,
          balance = bal,
          currency = parsed.transaction.currency,
          accountLabel = parsed.accountLabel,
          lastSyncedMillis = parsed.transaction.timestampMillis,
          lastReferenceCode = parsed.transaction.referenceCode,
        ),
      )
    }
  }

  suspend fun addManualTransaction(
    type: TransactionType,
    amount: Double,
    currency: String,
    category: ExpenseCategory,
    counterparty: String,
    note: String?,
    timestampMillis: Long = System.currentTimeMillis(),
    source: TransactionSource = TransactionSource.MANUAL,
  ) {
    val entity = TransactionEntity(
      smsSignature = "MANUAL_${UUID.randomUUID()}",
      source = source,
      type = type,
      amount = amount,
      currency = currency,
      category = category,
      counterparty = counterparty.ifBlank { if (type == TransactionType.EXPENSE) "Cash Expense" else "Cash Income" },
      referenceCode = null,
      balanceAfter = null,
      timestampMillis = timestampMillis,
      note = note?.takeIf { it.isNotBlank() },
      rawSmsSnippet = null,
      isAutoLogged = false,
    )
    dao.insertTransactionIgnoreDuplicate(entity)
    pruneExpired()
  }

  suspend fun updateTransaction(transaction: TransactionEntity) {
    dao.updateTransaction(transaction)
  }

  suspend fun deleteTransaction(id: Long) {
    dao.deleteTransactionById(id)
  }

  suspend fun pruneExpired(nowMillis: Long = System.currentTimeMillis()): Int {
    val cutoff = oneYearCutoffMillis(nowMillis)
    return dao.deleteOlderThan(cutoff)
  }

  suspend fun clearAllLocalData() {
    dao.deleteAllTransactions()
    dao.deleteAllBalances()
    prefs.recordSyncResult(syncMillis = 0L, prunedCount = 0)
  }

  suspend fun addCbeKeyword(keyword: String) = prefs.addCbeKeyword(keyword)
  suspend fun addMpesaKeyword(keyword: String) = prefs.addMpesaKeyword(keyword)
  suspend fun resetSenderKeywords() = prefs.resetSenderKeywords()
  suspend fun setNotificationsEnabled(enabled: Boolean) = prefs.setNotificationsEnabled(enabled)
  suspend fun setBiometricLockEnabled(enabled: Boolean) = prefs.setBiometricLockEnabled(enabled)
  suspend fun setLockOnBackground(enabled: Boolean) = prefs.setLockOnBackground(enabled)
  suspend fun setHideBalancesByDefault(hide: Boolean) = prefs.setHideBalancesByDefault(hide)
  suspend fun setShowDecimals(show: Boolean) = prefs.setShowDecimals(show)
  suspend fun setPreferredCurrency(currency: String) = prefs.setPreferredCurrency(currency)
  suspend fun setAutoSyncOnLaunch(enabled: Boolean) = prefs.setAutoSyncOnLaunch(enabled)
  suspend fun setThemeMode(mode: com.example.data.local.ThemeMode) = prefs.setThemeMode(mode)
  suspend fun setColorTheme(theme: com.example.data.local.AppColorTheme) = prefs.setColorTheme(theme)
}

data class SyncBatchOutcome(
  val insertedCount: Int,
  val prunedCount: Int,
)
