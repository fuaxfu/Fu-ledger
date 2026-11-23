package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.model.TransactionEntity
import com.example.security.BiometricAuthHelper
import com.example.security.BiometricAvailability
import com.example.ui.components.AddManualTransactionSheet
import com.example.ui.components.ParseSmsSheet
import com.example.ui.components.TransactionDetailSheet
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.BiometricLockScreen
import com.example.ui.screens.LedgerScreen
import com.example.ui.screens.OverviewScreen
import com.example.ui.screens.PrivacyVaultScreen
import com.example.ui.theme.BirrLedgerTheme
import com.example.ui.theme.IncomeGreen
import com.example.ui.viewmodel.LedgerViewModel

object Routes {
  const val OVERVIEW = "overview"
  const val ANALYTICS = "analytics"
  const val LEDGER = "ledger"
  const val PRIVACY_VAULT = "privacy_vault"
}

private data class TopLevelDestination(
  val route: String,
  val labelRes: Int,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val testTag: String,
)

private val topLevelDestinations = listOf(
  TopLevelDestination(
    route = Routes.OVERVIEW,
    labelRes = R.string.nav_overview,
    selectedIcon = Icons.Rounded.Home,
    unselectedIcon = Icons.Outlined.Home,
    testTag = "nav_overview",
  ),
  TopLevelDestination(
    route = Routes.ANALYTICS,
    labelRes = R.string.nav_analytics,
    selectedIcon = Icons.Rounded.Analytics,
    unselectedIcon = Icons.Outlined.Analytics,
    testTag = "nav_analytics",
  ),
  TopLevelDestination(
    route = Routes.LEDGER,
    labelRes = R.string.nav_ledger,
    selectedIcon = Icons.Rounded.ReceiptLong,
    unselectedIcon = Icons.Outlined.ReceiptLong,
    testTag = "nav_ledger",
  ),
)

class MainActivity : FragmentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      BirrLedgerRoot(activity = this)
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BirrLedgerRoot(
  activity: FragmentActivity,
  viewModel: LedgerViewModel = viewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val context = LocalContext.current

  BirrLedgerTheme(
    themeMode = uiState.themeMode,
    colorTheme = uiState.colorTheme,
  ) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route ?: Routes.OVERVIEW

    val snackbarHostState = remember { SnackbarHostState() }

    var selectedTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var showManualSheet by remember { mutableStateOf(false) }
    var showParseSmsSheet by remember { mutableStateOf(false) }

    var biometricAvailability by remember {
      mutableStateOf(BiometricAuthHelper.checkAvailability(context))
    }

    val promptTitle = stringResource(R.string.biometric_prompt_title)
    val promptSubtitle = stringResource(R.string.biometric_prompt_subtitle)

    val triggerBiometricPrompt: () -> Unit = {
      biometricAvailability = BiometricAuthHelper.checkAvailability(context)
      if (biometricAvailability == BiometricAvailability.READY) {
        BiometricAuthHelper.promptUnlock(
          activity = activity,
          title = promptTitle,
          subtitle = promptSubtitle,
          onSuccess = { viewModel.unlockApp() },
          onFailureMessage = { msg -> viewModel.setBiometricError(msg) },
        )
      }
    }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
      contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { _ ->
      viewModel.refreshPermissionState(autoSyncIfGranted = true)
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
      contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
      viewModel.setNotificationsEnabled(granted)
    }

    val requestSmsPermissions: () -> Unit = {
      val perms = buildList {
        add(Manifest.permission.RECEIVE_SMS)
        add(Manifest.permission.READ_SMS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          add(Manifest.permission.POST_NOTIFICATIONS)
        }
      }.toTypedArray()
      smsPermissionLauncher.launch(perms)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
      val observer = LifecycleEventObserver { _, event ->
        when (event) {
          Lifecycle.Event.ON_RESUME -> {
            biometricAvailability = BiometricAuthHelper.checkAvailability(context)
            viewModel.refreshPermissionState(autoSyncIfGranted = false)
          }
          Lifecycle.Event.ON_STOP -> {
            viewModel.onAppStopped()
          }
          else -> Unit
        }
      }
      lifecycleOwner.lifecycle.addObserver(observer)
      onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Automatically launch biometric prompt when locked and ready
    LaunchedEffect(uiState.biometricLockEnabled, uiState.isAppUnlocked, biometricAvailability) {
      if (uiState.biometricLockEnabled &&
        !uiState.isAppUnlocked &&
        biometricAvailability == BiometricAvailability.READY
      ) {
        triggerBiometricPrompt()
      }
    }

    LaunchedEffect(uiState.statusBannerMessage) {
      val msg = uiState.statusBannerMessage
      if (!msg.isNullOrBlank()) {
        snackbarHostState.showSnackbar(msg)
        viewModel.dismissStatusBanner()
      }
    }

    if (uiState.biometricLockEnabled && !uiState.isAppUnlocked) {
      BiometricLockScreen(
        availability = biometricAvailability,
        errorMessage = uiState.biometricError,
        onUnlockClick = triggerBiometricPrompt,
        onOpenSecuritySettings = { BiometricAuthHelper.openBiometricEnrollSettings(context) },
        onBypassWhenUnenrolled = {
          viewModel.setBiometricLockEnabled(false)
          viewModel.unlockApp()
        },
      )
      return@BirrLedgerTheme
    }

    val isTopLevelRoute = currentRoute != Routes.PRIVACY_VAULT

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
      val useNavRail = maxWidth >= 600.dp

      if (!isTopLevelRoute) {
        PrivacyVaultScreen(
          state = uiState,
          onNavigateBack = { navController.popBackStack() },
          onSelectThemeMode = viewModel::setThemeMode,
          onSelectColorTheme = viewModel::setColorTheme,
          onToggleBiometricLock = { enabled ->
            if (!enabled && biometricAvailability == BiometricAvailability.READY) {
              BiometricAuthHelper.promptUnlock(
                activity = activity,
                title = "Disable Biometric Lock",
                subtitle = "Confirm identity to turn off vault lock",
                onSuccess = { viewModel.setBiometricLockEnabled(false) },
                onFailureMessage = { msg -> viewModel.setBiometricError(msg) },
              )
            } else {
              viewModel.setBiometricLockEnabled(enabled)
            }
          },
          onToggleLockOnBackground = viewModel::setLockOnBackground,
          onLockNow = {
            navController.popBackStack()
            viewModel.lockAppNow()
          },
          onToggleHideBalancesDefault = viewModel::setHideBalancesByDefault,
          onToggleShowDecimals = viewModel::setShowDecimals,
          onSelectPreferredCurrency = viewModel::setPreferredCurrency,
          onToggleAutoSyncOnLaunch = viewModel::setAutoSyncOnLaunch,
          onRequestSmsPermission = requestSmsPermissions,
          onToggleNotifications = { enabled ->
            if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
              notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
              viewModel.setNotificationsEnabled(enabled)
            }
          },
          onAddCbeKeyword = viewModel::addCbeKeyword,
          onAddMpesaKeyword = viewModel::addMpesaKeyword,
          onResetKeywords = viewModel::resetSenderKeywords,
          onPruneNow = viewModel::pruneExpiredNow,
          onClearAllData = viewModel::clearAllLocalData,
        )
      } else {
        Scaffold(
          modifier = Modifier.fillMaxSize(),
          contentWindowInsets = WindowInsets(0, 0, 0, 0),
          snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
          topBar = {
            TopAppBar(
              colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
              ),
              title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Surface(
                    color = IncomeGreen.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(99.dp),
                  ) {
                    Text(
                      text = "365d Local",
                      style = MaterialTheme.typography.labelSmall,
                      color = IncomeGreen,
                      fontWeight = FontWeight.Bold,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                  }
                }
              },
              actions = {
                IconButton(
                  onClick = { showParseSmsSheet = true },
                  modifier = Modifier.testTag("topbar_parse_sms_button"),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Sms,
                    contentDescription = stringResource(R.string.paste_sms_action),
                  )
                }
                if (uiState.biometricLockEnabled) {
                  IconButton(
                    onClick = { viewModel.lockAppNow() },
                    modifier = Modifier.testTag("topbar_lock_button"),
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.Lock,
                      contentDescription = "Lock Vault",
                    )
                  }
                }
                IconButton(
                  onClick = { navController.navigate(Routes.PRIVACY_VAULT) },
                  modifier = Modifier.testTag("topbar_privacy_vault_button"),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = stringResource(R.string.settings_title),
                    tint = MaterialTheme.colorScheme.primary,
                  )
                }
              },
            )
          },
          bottomBar = {
            if (!useNavRail) {
              NavigationBar {
                topLevelDestinations.forEach { dest ->
                  val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                  NavigationBarItem(
                    selected = selected,
                    onClick = {
                      navController.navigate(dest.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                          saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                      }
                    },
                    icon = {
                      Icon(
                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                        contentDescription = stringResource(dest.labelRes),
                      )
                    },
                    label = { Text(stringResource(dest.labelRes)) },
                    modifier = Modifier.testTag(dest.testTag),
                  )
                }
              }
            }
          },
          floatingActionButton = {
            if (currentRoute == Routes.OVERVIEW || currentRoute == Routes.LEDGER) {
              FloatingActionButton(
                onClick = { showManualSheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_add_entry"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Add,
                  contentDescription = stringResource(R.string.add_manual_action),
                  modifier = Modifier.size(26.dp),
                )
              }
            }
          },
        ) { innerPadding ->
          Row(
            modifier = Modifier
              .fillMaxSize()
              .padding(innerPadding),
          ) {
            if (useNavRail) {
              NavigationRail {
                topLevelDestinations.forEach { dest ->
                  val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                  NavigationRailItem(
                    selected = selected,
                    onClick = {
                      navController.navigate(dest.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                          saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                      }
                    },
                    icon = {
                      Icon(
                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                        contentDescription = stringResource(dest.labelRes),
                      )
                    },
                    label = { Text(stringResource(dest.labelRes)) },
                    modifier = Modifier.testTag(dest.testTag),
                  )
                }
              }
            }

            Box(
              modifier = Modifier
                .weight(1f)
                .fillMaxSize(),
              contentAlignment = Alignment.TopCenter,
            ) {
              NavHost(
                navController = navController,
                startDestination = Routes.OVERVIEW,
                modifier = Modifier
                  .widthIn(max = 720.dp)
                  .fillMaxSize(),
              ) {
                composable(Routes.OVERVIEW) {
                  OverviewScreen(
                    state = uiState,
                    onRequestSmsPermission = requestSmsPermissions,
                    onSyncSmsNow = viewModel::syncSmsInbox,
                    onSelectOverviewPeriod = viewModel::setOverviewPeriod,
                    onToggleBalanceMask = viewModel::toggleSessionBalanceMask,
                    onTransactionClick = { selectedTransaction = it },
                    onViewAllLedgerClick = {
                      navController.navigate(Routes.LEDGER) {
                        popUpTo(navController.graph.findStartDestination().id) {
                          saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                      }
                    },
                    onOpenParseSmsClick = { showParseSmsSheet = true },
                  )
                }
                composable(Routes.ANALYTICS) {
                  AnalyticsScreen(
                    state = uiState,
                    onSelectPeriod = viewModel::setAnalyticsPeriod,
                    onSelectMonth = viewModel::selectAnalyticsMonth,
                    onSelectSourceFilter = viewModel::setAnalyticsSourceFilter,
                  )
                }
                composable(Routes.LEDGER) {
                  LedgerScreen(
                    state = uiState,
                    onSearchQueryChange = viewModel::setSearchQuery,
                    onSelectMonth = viewModel::selectLedgerMonth,
                    onTypeFilterChange = viewModel::setLedgerTypeFilter,
                    onSourceFilterChange = viewModel::setLedgerSourceFilter,
                    onTransactionClick = { selectedTransaction = it },
                    onAddManualClick = { showManualSheet = true },
                    onParseSmsClick = { showParseSmsSheet = true },
                  )
                }
              }
            }
          }
        }
      }
    }

    selectedTransaction?.let { tx ->
      TransactionDetailSheet(
        transaction = tx,
        onDismiss = { selectedTransaction = null },
        onSave = { updated ->
          viewModel.updateTransaction(updated)
          selectedTransaction = null
        },
        onDelete = { id ->
          viewModel.deleteTransaction(id)
          selectedTransaction = null
        },
      )
    }

    if (showManualSheet) {
      AddManualTransactionSheet(
        onDismiss = { showManualSheet = false },
        onSubmit = { type, amount, currency, category, counterparty, note ->
          viewModel.addManualEntry(type, amount, currency, category, counterparty, note)
          showManualSheet = false
        },
      )
    }

    if (showParseSmsSheet) {
      ParseSmsSheet(
        onDismiss = { showParseSmsSheet = false },
        onParseAndSave = { sender, body ->
          val ok = viewModel.parseAndIngestSmsText(sender, body)
          if (ok) {
            showParseSmsSheet = false
          }
          ok
        },
      )
    }
  }
}
