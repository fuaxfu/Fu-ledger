package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BirrLedgerApp
import com.example.data.analytics.AnalyticsEngine
import com.example.data.analytics.AnalyticsPeriod
import com.example.data.analytics.AnalyticsSnapshot
import com.example.data.analytics.MonthFilterOption
import com.example.data.local.AppColorTheme
import com.example.data.local.ThemeMode
import com.example.data.local.UserPreferencesRepository
import com.example.data.model.AccountBalanceEntity
import com.example.data.model.ExpenseCategory
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionSource
import com.example.data.model.TransactionType
import com.example.sms.BankSmsParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LedgerUiState(
  val transactions: List<TransactionEntity> = emptyList(),
  val filteredLedgerTransactions: List<TransactionEntity> = emptyList(),
  val ledgerMonthIncome: Double = 0.0,
  val ledgerMonthExpense: Double = 0.0,
  val balances: List<AccountBalanceEntity> = emptyList(),
  val rollingMonths: List<MonthFilterOption> = AnalyticsEngine.buildRolling12Months(),
  val selectedLedgerMonth: MonthFilterOption? = null,
  val selectedAnalyticsMonth: MonthFilterOption? = null,
  val overviewPeriod: AnalyticsPeriod = AnalyticsPeriod.DAYS_30,
  val overviewAnalytics: AnalyticsSnapshot = AnalyticsEngine.compute(emptyList(), AnalyticsPeriod.DAYS_30),
  val analyticsPeriod: AnalyticsPeriod = AnalyticsPeriod.YEAR_1,
  val analyticsSourceFilter: TransactionSource? = null,
  val analyticsSnapshot: AnalyticsSnapshot = AnalyticsEngine.compute(emptyList(), AnalyticsPeriod.YEAR_1),
  val searchQuery: String = "",
  val ledgerTypeFilter: TransactionType? = null,
  val ledgerSourceFilter: TransactionSource? = null,
  val ledgerCategoryFilter: ExpenseCategory? = null,
  val hasSmsPermissions: Boolean = false,
  val isSyncingSms: Boolean = false,
  val lastSyncMillis: Long = 0L,
  val lastPrunedCount: Int = 0,
  val totalRecordCount: Int = 0,
  val oldestRecordMillis: Long? = null,
  val cbeKeywords: Set<String> = emptySet(),
  val mpesaKeywords: Set<String> = emptySet(),
  val notificationsEnabled: Boolean = true,
  val biometricLockEnabled: Boolean = true,
  val lockOnBackground: Boolean = true,
  val isAppUnlocked: Boolean = false,
  val biometricError: String? = null,
  val hideBalancesByDefault: Boolean = false,
  val isBalancesMasked: Boolean = false,
  val showDecimals: Boolean = true,
  val preferredCurrency: String = "ETB",
  val autoSyncOnLaunch: Boolean = true,
  val themeMode: ThemeMode = ThemeMode.SYSTEM,
  val colorTheme: AppColorTheme = AppColorTheme.EMERALD_FOREST,
  val statusBannerMessage: String? = null,
)

private data class FilterPart1(
  val overviewPeriod: AnalyticsPeriod,
  val analyticsPeriod: AnalyticsPeriod,
  val analyticsSource: TransactionSource?,
  val search: String,
  val selectedAnalyticsMonth: MonthFilterOption?,
)

private data class FilterPart2(
  val type: TransactionType?,
  val source: TransactionSource?,
  val category: ExpenseCategory?,
  val selectedLedgerMonth: MonthFilterOption?,
)

private data class FilterParams(
  val overviewPeriod: AnalyticsPeriod,
  val analyticsPeriod: AnalyticsPeriod,
  val analyticsSource: TransactionSource?,
  val searchQuery: String,
  val selectedAnalyticsMonth: MonthFilterOption?,
  val ledgerType: TransactionType?,
  val ledgerSource: TransactionSource?,
  val ledgerCategory: ExpenseCategory?,
  val selectedLedgerMonth: MonthFilterOption?,
)

private data class VaultMetaPart1(
  val hasSmsPermissions: Boolean,
  val isSyncingSms: Boolean,
  val lastSyncMillis: Long,
  val lastPrunedCount: Int,
  val totalCount: Int,
)

private data class VaultMetaPart2(
  val oldestMillis: Long?,
  val cbeKeywords: Set<String>,
  val mpesaKeywords: Set<String>,
  val notificationsEnabled: Boolean,
  val statusBanner: String?,
)

private data class SecurityPrefs(
  val biometricLockEnabled: Boolean,
  val lockOnBackground: Boolean,
  val isAppUnlocked: Boolean,
  val biometricError: String?,
)

private data class AppearancePrefs(
  val hideBalancesByDefault: Boolean,
  val sessionMaskOverride: Boolean?,
  val showDecimals: Boolean,
  val preferredCurrency: String,
  val autoSyncOnLaunch: Boolean,
)

private data class ThemePrefs(
  val themeMode: ThemeMode,
  val colorTheme: AppColorTheme,
)

private data class SettingsBundle(
  val security: SecurityPrefs,
  val appearance: AppearancePrefs,
  val theme: ThemePrefs,
)

class LedgerViewModel(application: Application) : AndroidViewModel(application) {

  private val app = application as BirrLedgerApp
  private val repository = app.repository
  private val smsScanner = app.smsScanner

  private val rollingMonths = AnalyticsEngine.buildRolling12Months()

  private val overviewPeriodFlow = MutableStateFlow(AnalyticsPeriod.DAYS_30)
  private val analyticsPeriodFlow = MutableStateFlow(AnalyticsPeriod.YEAR_1)
  private val analyticsSourceFlow = MutableStateFlow<TransactionSource?>(null)
  private val selectedAnalyticsMonthFlow = MutableStateFlow<MonthFilterOption?>(null)
  private val searchQueryFlow = MutableStateFlow("")
  private val ledgerTypeFlow = MutableStateFlow<TransactionType?>(null)
  private val ledgerSourceFlow = MutableStateFlow<TransactionSource?>(null)
  private val ledgerCategoryFlow = MutableStateFlow<ExpenseCategory?>(null)
  private val selectedLedgerMonthFlow = MutableStateFlow<MonthFilterOption?>(null)

  private val hasSmsPermissionsFlow = MutableStateFlow(smsScanner.hasSmsPermissions())
  private val isSyncingSmsFlow = MutableStateFlow(false)
  private val statusMessageFlow = MutableStateFlow<String?>(null)

  private val isAppUnlockedFlow = MutableStateFlow(false)
  private val biometricErrorFlow = MutableStateFlow<String?>(null)
  private val sessionBalanceMaskOverrideFlow = MutableStateFlow<Boolean?>(null)

  init {
    viewModelScope.launch {
      val lockEnabled = repository.biometricLockEnabled.first()
      if (!lockEnabled) {
        isAppUnlockedFlow.value = true
      }
      val autoSync = repository.autoSyncOnLaunch.first()
      if (autoSync && smsScanner.hasSmsPermissions()) {
        syncSmsInbox()
      }
    }
  }

  private val filterParamsFlow = combine(
    combine(
      overviewPeriodFlow,
      analyticsPeriodFlow,
      analyticsSourceFlow,
      searchQueryFlow,
      selectedAnalyticsMonthFlow,
    ) { overviewPeriod, analyticsPeriod, analyticsSource, search, analyticsMonth ->
      FilterPart1(overviewPeriod, analyticsPeriod, analyticsSource, search, analyticsMonth)
    },
    combine(
      ledgerTypeFlow,
      ledgerSourceFlow,
      ledgerCategoryFlow,
      selectedLedgerMonthFlow,
    ) { type, source, category, ledgerMonth ->
      FilterPart2(type, source, category, ledgerMonth)
    },
  ) { p1, p2 ->
    FilterParams(
      overviewPeriod = p1.overviewPeriod,
      analyticsPeriod = p1.analyticsPeriod,
      analyticsSource = p1.analyticsSource,
      searchQuery = p1.search,
      selectedAnalyticsMonth = p1.selectedAnalyticsMonth,
      ledgerType = p2.type,
      ledgerSource = p2.source,
      ledgerCategory = p2.category,
      selectedLedgerMonth = p2.selectedLedgerMonth,
    )
  }

  private val vaultMetaFlow = combine(
    combine(
      hasSmsPermissionsFlow,
      isSyncingSmsFlow,
      repository.lastSyncMillis,
      repository.lastPrunedCount,
      repository.transactionCount,
    ) { hasPerms, isSyncing, lastSync, pruned, count ->
      VaultMetaPart1(hasPerms, isSyncing, lastSync, pruned, count)
    },
    combine(
      repository.oldestTimestamp,
      repository.cbeSenderKeywords,
      repository.mpesaSenderKeywords,
      repository.notificationsEnabled,
      statusMessageFlow,
    ) { oldest, cbe, mpesa, notify, banner ->
      VaultMetaPart2(oldest, cbe, mpesa, notify, banner)
    },
  ) { p1, p2 ->
    p1 to p2
  }

  private val settingsBundleFlow = combine(
    combine(
      repository.biometricLockEnabled,
      repository.lockOnBackground,
      isAppUnlockedFlow,
      biometricErrorFlow,
    ) { lockEnabled, lockBg, unlocked, bioErr ->
      SecurityPrefs(lockEnabled, lockBg, unlocked, bioErr)
    },
    combine(
      repository.hideBalancesByDefault,
      sessionBalanceMaskOverrideFlow,
      repository.showDecimals,
      repository.preferredCurrency,
      repository.autoSyncOnLaunch,
    ) { hideDefault, maskOverride, decimals, currency, autoSync ->
      AppearancePrefs(hideDefault, maskOverride, decimals, currency, autoSync)
    },
    combine(repository.themeMode, repository.colorTheme) { mode, color ->
      ThemePrefs(mode, color)
    },
  ) { sec, appPrefs, theme ->
    SettingsBundle(sec, appPrefs, theme)
  }

  val uiState: StateFlow<LedgerUiState> = combine(
    repository.transactions,
    repository.balances,
    filterParamsFlow,
    vaultMetaFlow,
    settingsBundleFlow,
  ) { transactions, balances, filters, vaultPair, settings ->
    val (meta1, meta2) = vaultPair
    val query = filters.searchQuery.trim().lowercase()

    val ledgerMonthTx = if (filters.selectedLedgerMonth != null) {
      transactions.filter {
        it.timestampMillis in filters.selectedLedgerMonth.startMillis until filters.selectedLedgerMonth.endMillis
      }
    } else {
      transactions
    }

    val filteredLedger = ledgerMonthTx.filter { tx ->
      val matchesQuery = query.isEmpty() ||
        tx.counterparty.lowercase().contains(query) ||
        tx.category.displayName.lowercase().contains(query) ||
        (tx.referenceCode?.lowercase()?.contains(query) == true) ||
        (tx.note?.lowercase()?.contains(query) == true)
      val matchesType = filters.ledgerType == null || tx.type == filters.ledgerType
      val matchesSource = filters.ledgerSource == null || tx.source == filters.ledgerSource
      val matchesCategory = filters.ledgerCategory == null || tx.category == filters.ledgerCategory
      matchesQuery && matchesType && matchesSource && matchesCategory
    }

    val ledgerMonthIncome = filteredLedger
      .filter { it.type == TransactionType.INCOME }
      .sumOf { it.amount }
    val ledgerMonthExpense = filteredLedger
      .filter { it.type == TransactionType.EXPENSE }
      .sumOf { it.amount }

    val effectiveUnlocked = !settings.security.biometricLockEnabled || settings.security.isAppUnlocked
    val effectiveMasked = settings.appearance.sessionMaskOverride ?: settings.appearance.hideBalancesByDefault
    val prefCurr = settings.appearance.preferredCurrency

    LedgerUiState(
      transactions = transactions,
      filteredLedgerTransactions = filteredLedger,
      ledgerMonthIncome = ledgerMonthIncome,
      ledgerMonthExpense = ledgerMonthExpense,
      balances = balances,
      rollingMonths = rollingMonths,
      selectedLedgerMonth = filters.selectedLedgerMonth,
      selectedAnalyticsMonth = filters.selectedAnalyticsMonth,
      overviewPeriod = filters.overviewPeriod,
      overviewAnalytics = AnalyticsEngine.compute(
        allTransactions = transactions,
        period = filters.overviewPeriod,
        preferredCurrency = prefCurr,
      ),
      analyticsPeriod = filters.analyticsPeriod,
      analyticsSourceFilter = filters.analyticsSource,
      analyticsSnapshot = AnalyticsEngine.compute(
        allTransactions = transactions,
        period = filters.analyticsPeriod,
        sourceFilter = filters.analyticsSource,
        selectedMonth = filters.selectedAnalyticsMonth,
        preferredCurrency = prefCurr,
      ),
      searchQuery = filters.searchQuery,
      ledgerTypeFilter = filters.ledgerType,
      ledgerSourceFilter = filters.ledgerSource,
      ledgerCategoryFilter = filters.ledgerCategory,
      hasSmsPermissions = meta1.hasSmsPermissions,
      isSyncingSms = meta1.isSyncingSms,
      lastSyncMillis = meta1.lastSyncMillis,
      lastPrunedCount = meta1.lastPrunedCount,
      totalRecordCount = meta1.totalCount,
      oldestRecordMillis = meta2.oldestMillis,
      cbeKeywords = meta2.cbeKeywords,
      mpesaKeywords = meta2.mpesaKeywords,
      notificationsEnabled = meta2.notificationsEnabled,
      biometricLockEnabled = settings.security.biometricLockEnabled,
      lockOnBackground = settings.security.lockOnBackground,
      isAppUnlocked = effectiveUnlocked,
      biometricError = settings.security.biometricError,
      hideBalancesByDefault = settings.appearance.hideBalancesByDefault,
      isBalancesMasked = effectiveMasked,
      showDecimals = settings.appearance.showDecimals,
      preferredCurrency = prefCurr,
      autoSyncOnLaunch = settings.appearance.autoSyncOnLaunch,
      themeMode = settings.theme.themeMode,
      colorTheme = settings.theme.colorTheme,
      statusBannerMessage = meta2.statusBanner,
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5_000),
    initialValue = LedgerUiState(),
  )

  fun unlockApp() {
    isAppUnlockedFlow.value = true
    biometricErrorFlow.value = null
  }

  fun setBiometricError(msg: String?) {
    biometricErrorFlow.value = msg
  }

  fun lockAppNow() {
    if (uiState.value.biometricLockEnabled) {
      isAppUnlockedFlow.value = false
      biometricErrorFlow.value = null
    }
  }

  fun onAppStopped() {
    if (uiState.value.biometricLockEnabled && uiState.value.lockOnBackground) {
      isAppUnlockedFlow.value = false
      biometricErrorFlow.value = null
    }
  }

  fun toggleSessionBalanceMask() {
    val current = uiState.value.isBalancesMasked
    sessionBalanceMaskOverrideFlow.value = !current
  }

  fun refreshPermissionState(autoSyncIfGranted: Boolean = false) {
    val granted = smsScanner.hasSmsPermissions()
    hasSmsPermissionsFlow.value = granted
    if (granted && autoSyncIfGranted) {
      syncSmsInbox()
    }
  }

  fun syncSmsInbox() {
    if (isSyncingSmsFlow.value) return
    viewModelScope.launch {
      isSyncingSmsFlow.value = true
      val result = smsScanner.scanInboxLastYear()
      isSyncingSmsFlow.value = false
      result.fold(
        onSuccess = { outcome ->
          statusMessageFlow.value = when {
            outcome.insertedCount > 0 && outcome.prunedCount > 0 ->
              "Synced ${outcome.insertedCount} new transactions • Pruned ${outcome.prunedCount} expired (>1 yr)"
            outcome.insertedCount > 0 ->
              "Synced ${outcome.insertedCount} new bank SMS transactions"
            else ->
              "Inbox up to date • 365-day local history verified"
          }
        },
        onFailure = {
          statusMessageFlow.value = "Grant SMS permission to sync CBE and M-Pesa inbox messages"
        },
      )
    }
  }

  fun parseAndIngestSmsText(senderInput: String, bodyInput: String): Boolean {
    val cbe = uiState.value.cbeKeywords.ifEmpty { UserPreferencesRepository.DEFAULT_CBE_SENDERS }
    val mpesa = uiState.value.mpesaKeywords.ifEmpty { UserPreferencesRepository.DEFAULT_MPESA_SENDERS }
    val parsed = BankSmsParser.parseSms(
      sender = senderInput,
      body = bodyInput,
      timestampMillis = System.currentTimeMillis(),
      cbeKeywords = cbe,
      mpesaKeywords = mpesa,
      allowBodyFallback = true,
    ) ?: return false

    viewModelScope.launch {
      val inserted = repository.ingestParsedSms(parsed)
      repository.pruneExpired()
      statusMessageFlow.value = if (inserted) {
        "${parsed.transaction.source.shortBadge} ${parsed.transaction.type.label} logged locally"
      } else {
        "Duplicate SMS reference already recorded"
      }
    }
    return true
  }

  fun addManualEntry(
    type: TransactionType,
    amount: Double,
    currency: String,
    category: ExpenseCategory,
    counterparty: String,
    note: String?,
  ) {
    viewModelScope.launch {
      repository.addManualTransaction(
        type = type,
        amount = amount,
        currency = currency,
        category = category,
        counterparty = counterparty,
        note = note,
      )
      statusMessageFlow.value = "${type.label} saved to local vault"
    }
  }

  fun updateTransaction(updated: TransactionEntity) {
    viewModelScope.launch {
      repository.updateTransaction(updated)
      statusMessageFlow.value = "Transaction updated"
    }
  }

  fun deleteTransaction(id: Long) {
    viewModelScope.launch {
      repository.deleteTransaction(id)
      statusMessageFlow.value = "Transaction removed"
    }
  }

  fun pruneExpiredNow() {
    viewModelScope.launch {
      val pruned = repository.pruneExpired()
      statusMessageFlow.value = if (pruned > 0) {
        "Erased $pruned transactions older than 365 days"
      } else {
        "All stored records are within the 365-day window"
      }
    }
  }

  fun clearAllLocalData() {
    viewModelScope.launch {
      repository.clearAllLocalData()
      statusMessageFlow.value = "All local ledger records erased"
    }
  }

  fun setOverviewPeriod(period: AnalyticsPeriod) {
    overviewPeriodFlow.value = period
  }

  fun setAnalyticsPeriod(period: AnalyticsPeriod) {
    analyticsPeriodFlow.value = period
    selectedAnalyticsMonthFlow.value = null
  }

  fun selectAnalyticsMonth(month: MonthFilterOption?) {
    selectedAnalyticsMonthFlow.value = month
  }

  fun setAnalyticsSourceFilter(source: TransactionSource?) {
    analyticsSourceFlow.value = source
  }

  fun selectLedgerMonth(month: MonthFilterOption?) {
    selectedLedgerMonthFlow.value = month
  }

  fun setSearchQuery(query: String) {
    searchQueryFlow.value = query
  }

  fun setLedgerTypeFilter(type: TransactionType?) {
    ledgerTypeFlow.value = type
  }

  fun setLedgerSourceFilter(source: TransactionSource?) {
    ledgerSourceFlow.value = source
  }

  fun setLedgerCategoryFilter(category: ExpenseCategory?) {
    ledgerCategoryFlow.value = category
  }

  fun addCbeKeyword(keyword: String) {
    viewModelScope.launch {
      repository.addCbeKeyword(keyword)
      statusMessageFlow.value = "Added CBE sender keyword"
    }
  }

  fun addMpesaKeyword(keyword: String) {
    viewModelScope.launch {
      repository.addMpesaKeyword(keyword)
      statusMessageFlow.value = "Added M-Pesa sender keyword"
    }
  }

  fun resetSenderKeywords() {
    viewModelScope.launch {
      repository.resetSenderKeywords()
      statusMessageFlow.value = "Sender keywords reset to defaults"
    }
  }

  fun setNotificationsEnabled(enabled: Boolean) {
    viewModelScope.launch {
      repository.setNotificationsEnabled(enabled)
    }
  }

  fun setBiometricLockEnabled(enabled: Boolean) {
    viewModelScope.launch {
      repository.setBiometricLockEnabled(enabled)
      if (!enabled) {
        isAppUnlockedFlow.value = true
      }
      statusMessageFlow.value = if (enabled) "Biometric vault lock enabled" else "Biometric vault lock disabled"
    }
  }

  fun setLockOnBackground(enabled: Boolean) {
    viewModelScope.launch {
      repository.setLockOnBackground(enabled)
    }
  }

  fun setHideBalancesByDefault(hide: Boolean) {
    viewModelScope.launch {
      repository.setHideBalancesByDefault(hide)
      sessionBalanceMaskOverrideFlow.value = hide
    }
  }

  fun setShowDecimals(show: Boolean) {
    viewModelScope.launch {
      repository.setShowDecimals(show)
    }
  }

  fun setPreferredCurrency(currency: String) {
    viewModelScope.launch {
      repository.setPreferredCurrency(currency)
    }
  }

  fun setAutoSyncOnLaunch(enabled: Boolean) {
    viewModelScope.launch {
      repository.setAutoSyncOnLaunch(enabled)
    }
  }

  fun setThemeMode(mode: ThemeMode) {
    viewModelScope.launch {
      repository.setThemeMode(mode)
    }
  }

  fun setColorTheme(theme: AppColorTheme) {
    viewModelScope.launch {
      repository.setColorTheme(theme)
    }
  }

  fun dismissStatusBanner() {
    statusMessageFlow.update { null }
  }
}
