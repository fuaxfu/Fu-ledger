package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoDelete
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.AppColorTheme
import com.example.data.local.ThemeMode
import com.example.ui.theme.ExpenseCoral
import com.example.ui.theme.IncomeGreen
import com.example.ui.util.FormatUtils
import com.example.ui.viewmodel.LedgerUiState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PrivacyVaultScreen(
  state: LedgerUiState,
  onNavigateBack: () -> Unit,
  onSelectThemeMode: (ThemeMode) -> Unit,
  onSelectColorTheme: (AppColorTheme) -> Unit,
  onToggleBiometricLock: (Boolean) -> Unit,
  onToggleLockOnBackground: (Boolean) -> Unit,
  onLockNow: () -> Unit,
  onToggleHideBalancesDefault: (Boolean) -> Unit,
  onToggleShowDecimals: (Boolean) -> Unit,
  onSelectPreferredCurrency: (String) -> Unit,
  onToggleAutoSyncOnLaunch: (Boolean) -> Unit,
  onRequestSmsPermission: () -> Unit,
  onToggleNotifications: (Boolean) -> Unit,
  onAddCbeKeyword: (String) -> Unit,
  onAddMpesaKeyword: (String) -> Unit,
  onResetKeywords: () -> Unit,
  onPruneNow: () -> Unit,
  onClearAllData: () -> Unit,
) {
  BackHandler(onBack = onNavigateBack)

  var newCbeKeyword by remember { mutableStateOf("") }
  var newMpesaKeyword by remember { mutableStateOf("") }
  var showClearConfirm by remember { mutableStateOf(false) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Settings & Vault") },
        navigationIcon = {
          IconButton(
            onClick = onNavigateBack,
            modifier = Modifier.testTag("vault_back_button"),
          ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
          }
        },
      )
    },
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // 1. Appearance & Themes
      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          shape = RoundedCornerShape(22.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                Icons.Rounded.Palette,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Appearance & Themes",
                style = MaterialTheme.typography.titleLarge,
              )
            }

            Text(
              text = "Brightness Mode",
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
              ThemeMode.entries.forEachIndexed { idx, mode ->
                SegmentedButton(
                  selected = state.themeMode == mode,
                  onClick = { onSelectThemeMode(mode) },
                  shape = SegmentedButtonDefaults.itemShape(
                    index = idx,
                    count = ThemeMode.entries.size,
                  ),
                  modifier = Modifier.testTag("theme_mode_${mode.name}"),
                ) {
                  Text(mode.label)
                }
              }
            }

            HorizontalDivider()

            Text(
              text = "Color Palette",
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              AppColorTheme.entries.forEach { palette ->
                val isSelected = state.colorTheme == palette
                val swatchColor = Color(palette.previewHex)
                Surface(
                  color = if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                  } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                  },
                  shape = RoundedCornerShape(14.dp),
                  modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .border(
                      width = if (isSelected) 1.5.dp else 0.dp,
                      color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                      shape = RoundedCornerShape(14.dp),
                    )
                    .clickable { onSelectColorTheme(palette) }
                    .testTag("color_theme_${palette.name}"),
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Box(
                      modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(swatchColor),
                      contentAlignment = Alignment.Center,
                    ) {
                      if (isSelected) {
                        Icon(
                          Icons.Rounded.Check,
                          contentDescription = null,
                          tint = Color.White,
                          modifier = Modifier.size(12.dp),
                        )
                      }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                      text = palette.label,
                      style = MaterialTheme.typography.labelLarge,
                      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    )
                  }
                }
              }
            }
          }
        }
      }

      // 2. Biometric Security
      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          shape = RoundedCornerShape(22.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                Icons.Rounded.Fingerprint,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Biometric App Security",
                style = MaterialTheme.typography.titleLarge,
              )
            }

            SettingsSwitchRow(
              icon = Icons.Rounded.Fingerprint,
              title = "Biometric / PIN Vault Lock",
              subtitle = "Require fingerprint, face, or PIN to open BirrLedger",
              checked = state.biometricLockEnabled,
              onCheckedChange = onToggleBiometricLock,
              testTag = "settings_biometric_switch",
            )

            if (state.biometricLockEnabled) {
              HorizontalDivider()

              SettingsSwitchRow(
                icon = Icons.Rounded.Lock,
                title = "Lock When Leaving App",
                subtitle = "Automatically lock ledger when switching apps",
                checked = state.lockOnBackground,
                onCheckedChange = onToggleLockOnBackground,
                testTag = "settings_lock_background_switch",
              )

              OutlinedButton(
                onClick = onLockNow,
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("settings_lock_now_button"),
              ) {
                Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Lock Vault Now")
              }
            }
          }
        }
      }

      // 3. Accessories & Display Preferences
      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          shape = RoundedCornerShape(22.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                Icons.Rounded.Tune,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Accessories & Preferences",
                style = MaterialTheme.typography.titleLarge,
              )
            }

            SettingsSwitchRow(
              icon = Icons.Rounded.VisibilityOff,
              title = "Privacy Balance Masking",
              subtitle = "Hide balances and amounts on launch until tapped",
              checked = state.hideBalancesByDefault,
              onCheckedChange = onToggleHideBalancesDefault,
              testTag = "settings_hide_balances_switch",
            )

            HorizontalDivider()

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Show Decimal Precision",
                  style = MaterialTheme.typography.titleMedium,
                )
                Text(
                  text = if (state.showDecimals) "Showing e.g. ETB 1,250.00" else "Compact e.g. ETB 1,250",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Switch(
                checked = state.showDecimals,
                onCheckedChange = onToggleShowDecimals,
                modifier = Modifier.testTag("settings_decimals_switch"),
              )
            }

            HorizontalDivider()

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Text(
                text = "Default Fallback Currency",
                style = MaterialTheme.typography.titleMedium,
              )
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("ETB", "KES", "USD").forEach { curr ->
                  FilterChip(
                    selected = state.preferredCurrency == curr,
                    onClick = { onSelectPreferredCurrency(curr) },
                    label = { Text(curr) },
                    modifier = Modifier.testTag("currency_chip_$curr"),
                  )
                }
              }
            }

            HorizontalDivider()

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Auto-Scan SMS on App Open",
                  style = MaterialTheme.typography.titleMedium,
                )
                Text(
                  text = "Automatically checks CBE & M-Pesa inbox on launch",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Switch(
                checked = state.autoSyncOnLaunch,
                onCheckedChange = onToggleAutoSyncOnLaunch,
                modifier = Modifier.testTag("settings_autosync_switch"),
              )
            }
          }
        }
      }

      // 4. SMS Sync & Notifications
      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          shape = RoundedCornerShape(22.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
          ) {
            Text(
              text = "SMS Sync & Notifications",
              style = MaterialTheme.typography.titleLarge,
            )

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
              ) {
                Icon(
                  Icons.Rounded.Sms,
                  contentDescription = null,
                  tint = if (state.hasSmsPermissions) IncomeGreen else MaterialTheme.colorScheme.tertiary,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                  Text(
                    text = "Real-Time SMS Receiver",
                    style = MaterialTheme.typography.titleMedium,
                  )
                  Text(
                    text = if (state.hasSmsPermissions) "Granted (Android 14+ active)" else "Permission required",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
              if (!state.hasSmsPermissions) {
                Button(onClick = onRequestSmsPermission) {
                  Text("Grant")
                }
              } else {
                Surface(
                  color = IncomeGreen.copy(alpha = 0.14f),
                  shape = RoundedCornerShape(8.dp),
                ) {
                  Text(
                    text = "Active",
                    style = MaterialTheme.typography.labelMedium,
                    color = IncomeGreen,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                  )
                }
              }
            }

            HorizontalDivider()

            SettingsSwitchRow(
              icon = Icons.Rounded.NotificationsActive,
              title = "Local Transaction Alerts",
              subtitle = "Show notification when CBE/M-Pesa SMS auto-logs",
              checked = state.notificationsEnabled,
              onCheckedChange = onToggleNotifications,
              testTag = "vault_notifications_switch",
            )
          }
        }
      }

      // 5. Monitored Senders
      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          shape = RoundedCornerShape(22.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "Monitored Bank Senders",
                style = MaterialTheme.typography.titleLarge,
              )
              TextButton(onClick = onResetKeywords) {
                Text("Reset")
              }
            }

            Text(
              text = "CBE Sender Keywords",
              style = MaterialTheme.typography.labelLarge,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              state.cbeKeywords.sorted().forEach { kw ->
                AssistChip(onClick = {}, label = { Text(kw) })
              }
            }
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              OutlinedTextField(
                value = newCbeKeyword,
                onValueChange = { newCbeKeyword = it },
                label = { Text("Add CBE sender ID") },
                singleLine = true,
                modifier = Modifier.weight(1f),
              )
              Button(
                onClick = {
                  if (newCbeKeyword.isNotBlank()) {
                    onAddCbeKeyword(newCbeKeyword)
                    newCbeKeyword = ""
                  }
                },
              ) {
                Text("Add")
              }
            }

            HorizontalDivider()

            Text(
              text = "M-Pesa Sender Keywords",
              style = MaterialTheme.typography.labelLarge,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              state.mpesaKeywords.sorted().forEach { kw ->
                AssistChip(onClick = {}, label = { Text(kw) })
              }
            }
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              OutlinedTextField(
                value = newMpesaKeyword,
                onValueChange = { newMpesaKeyword = it },
                label = { Text("Add M-Pesa sender ID") },
                singleLine = true,
                modifier = Modifier.weight(1f),
              )
              Button(
                onClick = {
                  if (newMpesaKeyword.isNotBlank()) {
                    onAddMpesaKeyword(newMpesaKeyword)
                    newMpesaKeyword = ""
                  }
                },
              ) {
                Text("Add")
              }
            }
          }
        }
      }

      // 6. On-Device Guarantee & 365-Day Retention Vault
      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          shape = RoundedCornerShape(22.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Image(
              painter = painterResource(id = R.drawable.img_local_vault_1795435746950),
              contentDescription = "100% on-device vault shield",
              contentScale = ContentScale.Crop,
              modifier = Modifier
                .fillMaxWidth()
                .height(124.dp)
                .clip(RoundedCornerShape(16.dp)),
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                Icons.Rounded.AutoDelete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "100% Local • 365-Day Retention",
                style = MaterialTheme.typography.titleLarge,
              )
            }

            Text(
              text = "Zero internet permissions. History accumulates for 365 days and older records are automatically erased.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Surface(
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                Text(
                  text = "Stored Records: ${state.totalRecordCount}",
                  style = MaterialTheme.typography.labelLarge,
                )
                Text(
                  text = "Oldest Record: ${
                    state.oldestRecordMillis?.let { FormatUtils.formatDateOnly(it) } ?: "None yet"
                  }",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              OutlinedButton(
                onClick = onPruneNow,
                modifier = Modifier
                  .weight(1f)
                  .testTag("vault_prune_button"),
              ) {
                Text("Prune > 1 Year")
              }

              OutlinedButton(
                onClick = { showClearConfirm = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpenseCoral),
                modifier = Modifier
                  .weight(1f)
                  .testTag("vault_erase_all_button"),
              ) {
                Icon(Icons.Rounded.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Erase All")
              }
            }
          }
        }
      }

      item { Spacer(modifier = Modifier.height(24.dp)) }
    }
  }

  if (showClearConfirm) {
    AlertDialog(
      onDismissRequest = { showClearConfirm = false },
      title = { Text("Erase All Local History?") },
      text = {
        Text("This permanently deletes all stored transactions and synced CBE/M-Pesa balances from this device. Your SMS inbox messages are not affected.")
      },
      confirmButton = {
        Button(
          onClick = {
            showClearConfirm = false
            onClearAllData()
          },
          colors = ButtonDefaults.buttonColors(containerColor = ExpenseCoral),
        ) {
          Text("Erase Everything")
        }
      },
      dismissButton = {
        TextButton(onClick = { showClearConfirm = false }) {
          Text("Cancel")
        }
      },
    )
  }
}

@Composable
private fun SettingsSwitchRow(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  subtitle: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  testTag: String,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.weight(1f),
    ) {
      Icon(
        icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
      )
      Spacer(modifier = Modifier.width(12.dp))
      Column {
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
    Spacer(modifier = Modifier.width(8.dp))
    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
      modifier = Modifier.testTag(testTag),
    )
  }
}
