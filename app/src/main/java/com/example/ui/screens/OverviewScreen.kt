package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.PhoneIphone
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.analytics.AnalyticsPeriod
import com.example.data.model.AccountBalanceEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionSource
import com.example.ui.components.EmptyVaultCard
import com.example.ui.components.InteractiveCashFlowChart
import com.example.ui.components.TransactionRowCard
import com.example.ui.theme.CbeGold
import com.example.ui.theme.ExpenseCoralSoft
import com.example.ui.theme.IncomeGreenSoft
import com.example.ui.theme.MonoAmountLarge
import com.example.ui.theme.MonoAmountMedium
import com.example.ui.theme.MonoAmountSmall
import com.example.ui.theme.MpesaGreen
import com.example.ui.util.FormatUtils
import com.example.ui.viewmodel.LedgerUiState

@Composable
fun OverviewScreen(
  state: LedgerUiState,
  onRequestSmsPermission: () -> Unit,
  onSyncSmsNow: () -> Unit,
  onSelectOverviewPeriod: (AnalyticsPeriod) -> Unit,
  onToggleBalanceMask: () -> Unit,
  onTransactionClick: (TransactionEntity) -> Unit,
  onViewAllLedgerClick: () -> Unit,
  onOpenParseSmsClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val cbeBalance = state.balances.firstOrNull { it.source == TransactionSource.CBE }
  val mpesaBalance = state.balances.firstOrNull { it.source == TransactionSource.MPESA }
  val totalSyncedBalance = state.balances.sumOf { it.balance }
  val currency = state.overviewAnalytics.primaryCurrency

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    // Hero Balance Card
    item {
      HeroBalanceCard(
        totalBalance = totalSyncedBalance,
        hasAnyBalance = state.balances.isNotEmpty(),
        currency = currency,
        showDecimals = state.showDecimals,
        isMasked = state.isBalancesMasked,
        onToggleMask = onToggleBalanceMask,
        periodIncome = state.overviewAnalytics.totalIncome,
        periodExpense = state.overviewAnalytics.totalExpense,
        periodLabel = state.overviewPeriod.label,
        cbeBalance = cbeBalance,
        mpesaBalance = mpesaBalance,
      )
    }

    // SMS Sync Status Strip
    item {
      SmsSyncStatusCard(
        hasSmsPermissions = state.hasSmsPermissions,
        isSyncing = state.isSyncingSms,
        lastSyncMillis = state.lastSyncMillis,
        onGrantPermission = onRequestSmsPermission,
        onSyncNow = onSyncSmsNow,
        onParseSmsClick = onOpenParseSmsClick,
      )
    }

    if (state.transactions.isEmpty()) {
      item {
        EmptyVaultCard(
          hasSmsPermission = state.hasSmsPermissions,
          onGrantSmsPermission = onRequestSmsPermission,
          onParseSmsClick = onOpenParseSmsClick,
        )
      }
    } else {
      // Interactive Cash-Flow Chart Card
      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          shape = RoundedCornerShape(22.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column {
                Text(
                  text = "Cash Flow Pulse",
                  style = MaterialTheme.typography.titleLarge,
                )
                Text(
                  text = "Tap or drag bars to inspect",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(AnalyticsPeriod.DAYS_7, AnalyticsPeriod.DAYS_30).forEach { period ->
                  FilterChip(
                    selected = state.overviewPeriod == period,
                    onClick = { onSelectOverviewPeriod(period) },
                    label = { Text(period.label) },
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            InteractiveCashFlowChart(
              buckets = state.overviewAnalytics.buckets,
              currency = currency,
            )
          }
        }
      }

      // Recent Activity Header
      item {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "Recent Transactions",
            style = MaterialTheme.typography.titleLarge,
          )
          TextButton(
            onClick = onViewAllLedgerClick,
            modifier = Modifier.testTag("overview_view_all_button"),
          ) {
            Text("View All (${state.transactions.size})")
          }
        }
      }

      items(
        items = state.transactions.take(6),
        key = { it.id },
      ) { tx ->
        TransactionRowCard(
          transaction = tx,
          onClick = { onTransactionClick(tx) },
          showDecimals = state.showDecimals,
          hideAmount = state.isBalancesMasked,
        )
      }
    }

    item { Spacer(modifier = Modifier.height(72.dp)) }
  }
}

@Composable
private fun HeroBalanceCard(
  totalBalance: Double,
  hasAnyBalance: Boolean,
  currency: String,
  showDecimals: Boolean,
  isMasked: Boolean,
  onToggleMask: () -> Unit,
  periodIncome: Double,
  periodExpense: Double,
  periodLabel: String,
  cbeBalance: AccountBalanceEntity?,
  mpesaBalance: AccountBalanceEntity?,
) {
  val gradientBrush = Brush.linearGradient(
    colors = listOf(
      Color(0xFF0A2E27),
      Color(0xFF0F4C3E),
      Color(0xFF08221D),
    ),
  )

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(24.dp))
      .background(gradientBrush)
      .border(
        width = 1.dp,
        color = Color(0xFF2B6F5E).copy(alpha = 0.55f),
        shape = RoundedCornerShape(24.dp),
      )
      .padding(20.dp),
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "Synced Combined Balance",
          style = MaterialTheme.typography.labelLarge,
          color = Color(0xFFAAD8CB),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = onToggleMask,
            modifier = Modifier
              .size(34.dp)
              .testTag("hero_mask_toggle_button"),
          ) {
            Icon(
              imageVector = if (isMasked) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
              contentDescription = if (isMasked) "Show balances" else "Hide balances",
              tint = Color(0xFFD6F7EC),
              modifier = Modifier.size(18.dp),
            )
          }
          Spacer(modifier = Modifier.width(6.dp))
          Surface(
            color = Color(0xFF144E40),
            shape = RoundedCornerShape(99.dp),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                Icons.Rounded.Shield,
                contentDescription = null,
                tint = IncomeGreenSoft,
                modifier = Modifier.size(13.dp),
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "Local Vault",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFD6F7EC),
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = when {
          isMasked -> "$currency ••••••"
          hasAnyBalance -> FormatUtils.formatAmount(totalBalance, currency, showDecimals)
          else -> "$currency --.--"
        },
        style = MonoAmountLarge,
        color = Color.White,
        modifier = Modifier.testTag("hero_total_balance"),
      )

      Spacer(modifier = Modifier.height(14.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        FlowSummaryPill(
          label = "$periodLabel Income",
          amount = if (isMasked) "$currency ••••" else FormatUtils.formatAmount(periodIncome, currency, false),
          icon = Icons.Rounded.ArrowDownward,
          tint = IncomeGreenSoft,
          modifier = Modifier.weight(1f),
        )
        FlowSummaryPill(
          label = "$periodLabel Spent",
          amount = if (isMasked) "$currency ••••" else FormatUtils.formatAmount(periodExpense, currency, false),
          icon = Icons.Rounded.ArrowUpward,
          tint = ExpenseCoralSoft,
          modifier = Modifier.weight(1f),
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        AccountSyncPill(
          title = "CBE",
          balance = cbeBalance,
          isMasked = isMasked,
          showDecimals = showDecimals,
          accent = CbeGold,
          icon = Icons.Rounded.AccountBalance,
          modifier = Modifier.weight(1f),
        )
        AccountSyncPill(
          title = "M-PESA",
          balance = mpesaBalance,
          isMasked = isMasked,
          showDecimals = showDecimals,
          accent = MpesaGreen,
          icon = Icons.Rounded.PhoneIphone,
          modifier = Modifier.weight(1f),
        )
      }
    }
  }
}

@Composable
private fun FlowSummaryPill(
  label: String,
  amount: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  tint: Color,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = Color.White.copy(alpha = 0.08f),
    shape = RoundedCornerShape(14.dp),
    modifier = modifier,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier = Modifier
          .size(28.dp)
          .clip(CircleShape)
          .background(tint.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
      }
      Spacer(modifier = Modifier.width(8.dp))
      Column {
        Text(
          text = label,
          style = MaterialTheme.typography.labelSmall,
          color = Color(0xFFB3D5CA),
        )
        Text(
          text = amount,
          style = MonoAmountSmall,
          fontWeight = FontWeight.Bold,
          color = Color.White,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
private fun AccountSyncPill(
  title: String,
  balance: AccountBalanceEntity?,
  isMasked: Boolean,
  showDecimals: Boolean,
  accent: Color,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = Color.Black.copy(alpha = 0.22f),
    shape = RoundedCornerShape(14.dp),
    modifier = modifier,
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(15.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = title,
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = accent,
        )
      }
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = when {
          isMasked -> "${balance?.currency ?: "ETB"} ••••"
          balance != null -> FormatUtils.formatAmount(balance.balance, balance.currency, showDecimals)
          else -> "Awaiting SMS"
        },
        style = MonoAmountMedium,
        color = if (balance != null) Color.White else Color(0xFF91B3A7),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = if (balance != null) {
          "Synced ${FormatUtils.formatDateTimeShort(balance.lastSyncedMillis)}"
        } else {
          "Auto-syncs on SMS"
        },
        style = MaterialTheme.typography.labelSmall,
        color = Color(0xFF8DB0A4),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun SmsSyncStatusCard(
  hasSmsPermissions: Boolean,
  isSyncing: Boolean,
  lastSyncMillis: Long,
  onGrantPermission: () -> Unit,
  onSyncNow: () -> Unit,
  onParseSmsClick: () -> Unit,
) {
  Surface(
    color = if (hasSmsPermissions) {
      MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
    } else {
      MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f)
    },
    shape = RoundedCornerShape(18.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(1f),
      ) {
        Box(
          modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(
              if (hasSmsPermissions) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
              else MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f),
            ),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Rounded.Sms,
            contentDescription = null,
            tint = if (hasSmsPermissions) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(20.dp),
          )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Text(
            text = if (hasSmsPermissions) "Real-Time SMS Sync Active" else "Enable Real-Time SMS Sync",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text = if (hasSmsPermissions) {
              if (lastSyncMillis > 0L) "Last scan: ${FormatUtils.formatDateTimeShort(lastSyncMillis)}"
              else "Listening for CBE & M-Pesa"
            } else {
              "Reads CBE & M-Pesa locally • Zero cloud upload"
            },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      Spacer(modifier = Modifier.width(8.dp))

      if (hasSmsPermissions) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          OutlinedButton(
            onClick = onParseSmsClick,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
            modifier = Modifier.testTag("sync_card_paste_button"),
          ) {
            Text("Paste", style = MaterialTheme.typography.labelLarge)
          }
          Button(
            onClick = onSyncNow,
            enabled = !isSyncing,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier.testTag("sync_now_button"),
          ) {
            if (isSyncing) {
              CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
              )
            } else {
              Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Sync", style = MaterialTheme.typography.labelLarge)
            }
          }
        }
      } else {
        Button(
          onClick = onGrantPermission,
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
          modifier = Modifier.testTag("enable_sms_sync_button"),
        ) {
          Text("Allow", style = MaterialTheme.typography.labelLarge)
        }
      }
    }
  }
}
