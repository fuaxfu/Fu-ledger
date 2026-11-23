package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "birr_ledger_prefs")

enum class ThemeMode(val label: String) {
  SYSTEM("System"),
  LIGHT("Light"),
  DARK("Dark"),
}

enum class AppColorTheme(val label: String, val previewHex: Long) {
  EMERALD_FOREST("Emerald Forest", 0xFF0B4F43),
  ROYAL_SAPPHIRE("Royal Sapphire", 0xFF1D4ED8),
  OBSIDIAN_GOLD("Obsidian Gold", 0xFFB45309),
  CRIMSON_SLATE("Crimson Slate", 0xFF9F1239),
  DYNAMIC_SYSTEM("Material You", 0xFF4F46E5),
}

class UserPreferencesRepository(private val context: Context) {

  companion object {
    val DEFAULT_CBE_SENDERS = setOf("CBE", "COMMERCIAL", "CBEBIRR")
    val DEFAULT_MPESA_SENDERS = setOf("MPESA", "M-PESA", "SAFARICOM")

    private val KEY_CBE_SENDERS = stringSetPreferencesKey("cbe_sender_keywords")
    private val KEY_MPESA_SENDERS = stringSetPreferencesKey("mpesa_sender_keywords")
    private val KEY_NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
    private val KEY_LAST_SYNC_MILLIS = longPreferencesKey("last_sync_millis")
    private val KEY_LAST_PRUNED_COUNT = intPreferencesKey("last_pruned_count")

    // Security & Accessories
    private val KEY_BIOMETRIC_LOCK = booleanPreferencesKey("biometric_lock_enabled")
    private val KEY_LOCK_ON_BACKGROUND = booleanPreferencesKey("lock_on_background")
    private val KEY_HIDE_BALANCES = booleanPreferencesKey("hide_balances_by_default")
    private val KEY_SHOW_DECIMALS = booleanPreferencesKey("show_decimals")
    private val KEY_PREFERRED_CURRENCY = stringPreferencesKey("preferred_currency")
    private val KEY_AUTO_SYNC_ON_LAUNCH = booleanPreferencesKey("auto_sync_on_launch")
    private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
    private val KEY_COLOR_THEME = stringPreferencesKey("color_theme")
  }

  val cbeSenderKeywords: Flow<Set<String>> = context.dataStore.data.map { prefs ->
    prefs[KEY_CBE_SENDERS]?.takeIf { it.isNotEmpty() } ?: DEFAULT_CBE_SENDERS
  }

  val mpesaSenderKeywords: Flow<Set<String>> = context.dataStore.data.map { prefs ->
    prefs[KEY_MPESA_SENDERS]?.takeIf { it.isNotEmpty() } ?: DEFAULT_MPESA_SENDERS
  }

  val notificationsEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
    prefs[KEY_NOTIFICATIONS] ?: true
  }

  val lastSyncMillis: Flow<Long> = context.dataStore.data.map { prefs ->
    prefs[KEY_LAST_SYNC_MILLIS] ?: 0L
  }

  val lastPrunedCount: Flow<Int> = context.dataStore.data.map { prefs ->
    prefs[KEY_LAST_PRUNED_COUNT] ?: 0
  }

  val biometricLockEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
    prefs[KEY_BIOMETRIC_LOCK] ?: true
  }

  val lockOnBackground: Flow<Boolean> = context.dataStore.data.map { prefs ->
    prefs[KEY_LOCK_ON_BACKGROUND] ?: true
  }

  val hideBalancesByDefault: Flow<Boolean> = context.dataStore.data.map { prefs ->
    prefs[KEY_HIDE_BALANCES] ?: false
  }

  val showDecimals: Flow<Boolean> = context.dataStore.data.map { prefs ->
    prefs[KEY_SHOW_DECIMALS] ?: true
  }

  val preferredCurrency: Flow<String> = context.dataStore.data.map { prefs ->
    prefs[KEY_PREFERRED_CURRENCY] ?: "ETB"
  }

  val autoSyncOnLaunch: Flow<Boolean> = context.dataStore.data.map { prefs ->
    prefs[KEY_AUTO_SYNC_ON_LAUNCH] ?: true
  }

  val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
    val raw = prefs[KEY_THEME_MODE]
    ThemeMode.entries.firstOrNull { it.name == raw } ?: ThemeMode.SYSTEM
  }

  val colorTheme: Flow<AppColorTheme> = context.dataStore.data.map { prefs ->
    val raw = prefs[KEY_COLOR_THEME]
    AppColorTheme.entries.firstOrNull { it.name == raw } ?: AppColorTheme.EMERALD_FOREST
  }

  suspend fun addCbeKeyword(keyword: String) {
    val clean = keyword.trim().uppercase()
    if (clean.isBlank()) return
    context.dataStore.edit { prefs ->
      val current = prefs[KEY_CBE_SENDERS] ?: DEFAULT_CBE_SENDERS
      prefs[KEY_CBE_SENDERS] = current + clean
    }
  }

  suspend fun addMpesaKeyword(keyword: String) {
    val clean = keyword.trim().uppercase()
    if (clean.isBlank()) return
    context.dataStore.edit { prefs ->
      val current = prefs[KEY_MPESA_SENDERS] ?: DEFAULT_MPESA_SENDERS
      prefs[KEY_MPESA_SENDERS] = current + clean
    }
  }

  suspend fun resetSenderKeywords() {
    context.dataStore.edit { prefs ->
      prefs[KEY_CBE_SENDERS] = DEFAULT_CBE_SENDERS
      prefs[KEY_MPESA_SENDERS] = DEFAULT_MPESA_SENDERS
    }
  }

  suspend fun setNotificationsEnabled(enabled: Boolean) {
    context.dataStore.edit { prefs -> prefs[KEY_NOTIFICATIONS] = enabled }
  }

  suspend fun setBiometricLockEnabled(enabled: Boolean) {
    context.dataStore.edit { prefs -> prefs[KEY_BIOMETRIC_LOCK] = enabled }
  }

  suspend fun setLockOnBackground(enabled: Boolean) {
    context.dataStore.edit { prefs -> prefs[KEY_LOCK_ON_BACKGROUND] = enabled }
  }

  suspend fun setHideBalancesByDefault(hide: Boolean) {
    context.dataStore.edit { prefs -> prefs[KEY_HIDE_BALANCES] = hide }
  }

  suspend fun setShowDecimals(show: Boolean) {
    context.dataStore.edit { prefs -> prefs[KEY_SHOW_DECIMALS] = show }
  }

  suspend fun setPreferredCurrency(currency: String) {
    context.dataStore.edit { prefs -> prefs[KEY_PREFERRED_CURRENCY] = currency }
  }

  suspend fun setAutoSyncOnLaunch(enabled: Boolean) {
    context.dataStore.edit { prefs -> prefs[KEY_AUTO_SYNC_ON_LAUNCH] = enabled }
  }

  suspend fun setThemeMode(mode: ThemeMode) {
    context.dataStore.edit { prefs -> prefs[KEY_THEME_MODE] = mode.name }
  }

  suspend fun setColorTheme(theme: AppColorTheme) {
    context.dataStore.edit { prefs -> prefs[KEY_COLOR_THEME] = theme.name }
  }

  suspend fun recordSyncResult(syncMillis: Long, prunedCount: Int) {
    context.dataStore.edit { prefs ->
      prefs[KEY_LAST_SYNC_MILLIS] = syncMillis
      prefs[KEY_LAST_PRUNED_COUNT] = prunedCount
    }
  }
}
