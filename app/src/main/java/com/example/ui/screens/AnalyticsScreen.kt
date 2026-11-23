package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.analytics.AnalyticsPeriod
import com.example.data.analytics.MonthFilterOption
import com.example.data.model.TransactionSource
import com.example.ui.components.InteractiveCashFlowChart
import com.example.ui.components.InteractiveCategoryDonutChart
import com.example.ui.components.MonthSelectorStrip
import com.example.ui.components.SourceBadge
import com.example.ui.components.SourceComparisonCardContent
import com.example.ui.theme.ExpenseCoral
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.MonoAmountMedium
import com.example.ui.theme.MonoAmountSmall
import com.example.ui.util.FormatUtils
import com.example.ui.viewmodel.LedgerUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AnalyticsScreen(
  state: LedgerUiState,
  onSelectPeriod: (AnalyticsPeriod) -> Unit,
  onSelectMonth: (MonthFilterOption?) -> Unit,
  onSelectSourceFilter: (TransactionSource?) -> Unit,
  modifier: Modifier = Modifier,
) {
  val snapshot = state.analyticsSnapshot
  val currency = snapshot.primaryCurrency
  val scopeLabel = state.selectedAnalyticsMonth?.yearMonthLabel ?: state.analyticsPeriod.label

  var selectedSliceIndex by remember(snapshot.expenseSlices, snapshot.incomeSlices) {
    mutableStateOf<Int?>(null)
  }
  var donutModeIndex by remember { mutableIntStateOf(0) } // 0 = Expenses, 1 = Income

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    // Period, Month Segregation & Source Filter Controls
    item {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
          AnalyticsPeriod.entries.forEachIndexed { idx, period ->
            SegmentedButton(
              selected = state.selectedAnalyticsMonth == null && state.analyticsPeriod == period,
              onClick = { onSelectPeriod(period) },
              shape = SegmentedButtonDefaults.itemShape(
                index = idx,
                count = AnalyticsPeriod.entries.size,
              ),
              modifier = Modifier.testTag("analytics_period_${period.label}"),
            ) {
              Text(period.label)
            }
          }
        }

        // Segregate Analytics by specific month (January, February, ...)
        MonthSelectorStrip(
          months = state.rollingMonths,
          selectedMonth = state.selectedAnalyticsMonth,
          onSelectMonth = onSelectMonth,
        )

        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          FilterChip(
            selected = state.analyticsSourceFilter == null,
            onClick = { onSelectSourceFilter(null) },
            label = { Text("All Accounts") },
          )
          FilterChip(
            selected = state.analyticsSourceFilter == TransactionSource.CBE,
            onClick = { onSelectSourceFilter(TransactionSource.CBE) },
            label = { Text("CBE") },
          )
          FilterChip(
            selected = state.analyticsSourceFilter == TransactionSource.MPESA,
            onClick = { onSelectSourceFilter(TransactionSource.MPESA) },
            label = { Text("M-Pesa") },
          )
          FilterChip(
            selected = state.analyticsSourceFilter == TransactionSource.MANUAL,
            onClick = { onSelectSourceFilter(TransactionSource.MANUAL) },
            label = { Text("Manual") },
          )
        }
      }
    }

    // KPI Summary Strip
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        AnalyticsKpiCard(
          label = "Total Inflow",
          value = if (state.isBalancesMasked) "$currency ••••"
          else FormatUtils.formatAmount(snapshot.totalIncome, currency, state.showDecimals),
          accent = IncomeGreen,
          modifier = Modifier.weight(1f),
        )
        AnalyticsKpiCard(
          label = "Total Outflow",
          value = if (state.isBalancesMasked) "$currency ••••"
          else FormatUtils.formatAmount(snapshot.totalExpense, currency, state.showDecimals),
          accent = ExpenseCoral,
          modifier = Modifier.weight(1f),
        )
      }
    }

    item {
      Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column {
            Text(
              text = "Net Savings ($scopeLabel)",
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val netSign = if (snapshot.netCashFlow >= 0) "+" else "-"
            Text(
              text = if (state.isBalancesMasked) "$currency ••••••"
              else "$netSign${FormatUtils.formatAmount(kotlin.math.abs(snapshot.netCashFlow), currency, state.showDecimals)}",
              style = MonoAmountMedium,
              fontWeight = FontWeight.Bold,
              color = if (snapshot.netCashFlow >= 0) IncomeGreen else ExpenseCoral,
            )
          }
          Surface(
            color = if (snapshot.savingsRatePercent >= 0) {
              IncomeGreen.copy(alpha = 0.14f)
            } else {
              ExpenseCoral.copy(alpha = 0.14f)
            },
            shape = RoundedCornerShape(12.dp),
          ) {
            Text(
              text = "Savings ${snapshot.savingsRatePercent}%",
              style = MaterialTheme.typography.labelLarge,
              color = if (snapshot.savingsRatePercent >= 0) IncomeGreen else ExpenseCoral,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
          }
        }
      }
    }

    // Cash Flow Trend Chart Card
    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            text = if (state.selectedAnalyticsMonth != null) {
              "${state.selectedAnalyticsMonth.monthName} Cash Flow"
            } else {
              "Income vs. Spending Trend"
            },
            style = MaterialTheme.typography.titleLarge,
          )
          Text(
            text = "Scrub across bars to inspect exact totals per period",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Spacer(modifier = Modifier.height(14.dp))
          InteractiveCashFlowChart(
            buckets = snapshot.buckets,
            currency = currency,
          )
        }
      }
    }

    // Interactive Category Donut Card
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
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Category Breakdown",
                style = MaterialTheme.typography.titleLarge,
              )
              Text(
                text = "Tap any slice or row to highlight",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              FilterChip(
                selected = donutModeIndex == 0,
                onClick = {
                  donutModeIndex = 0
                  selectedSliceIndex = null
                },
                label = { Text("Spent") },
              )
              FilterChip(
                selected = donutModeIndex == 1,
                onClick = {
                  donutModeIndex = 1
                  selectedSliceIndex = null
                },
                label = { Text("Earned") },
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          val slices = if (donutModeIndex == 0) snapshot.expenseSlices else snapshot.incomeSlices
          val totalForDonut = if (donutModeIndex == 0) snapshot.totalExpense else snapshot.totalIncome

          InteractiveCategoryDonutChart(
            slices = slices,
            totalAmount = totalForDonut,
            currency = currency,
            selectedIndex = selectedSliceIndex,
            onSelectIndex = { selectedSliceIndex = it },
          )
        }
      }
    }

    // Monthly Breakdown Ledger (January, February, ...)
    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Text(
            text = "Monthly Breakdown (12 Months)",
            style = MaterialTheme.typography.titleLarge,
          )
          Text(
            text = "Tap a month to segregate charts and totals",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )

          snapshot.monthlyBreakdowns.forEachIndexed { idx, row ->
            val isSelected = state.selectedAnalyticsMonth?.id == row.month.id
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(
                  if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                  else Color.Transparent,
                )
                .clickable {
                  onSelectMonth(if (isSelected) null else row.month)
                }
                .padding(horizontal = 10.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = row.month.yearMonthLabel,
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                )
                Text(
                  text = if (row.count == 0) "No transactions" else "${row.count} transactions",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Column(horizontalAlignment = Alignment.End) {
                Text(
                  text = if (state.isBalancesMasked) "+••••"
                  else "+${FormatUtils.formatShortNumber(row.income)}",
                  style = MonoAmountSmall,
                  color = IncomeGreen,
                )
                Text(
                  text = if (state.isBalancesMasked) "-••••"
                  else "-${FormatUtils.formatShortNumber(row.expense)}",
                  style = MonoAmountSmall,
                  color = ExpenseCoral,
                )
              }
              Spacer(modifier = Modifier.width(6.dp))
              Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = "Inspect ${row.month.monthName}",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
              )
            }
            if (idx < snapshot.monthlyBreakdowns.lastIndex) {
              HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            }
          }
        }
      }
    }

    // CBE vs M-Pesa Source Split
    if (snapshot.sourceSummaries.isNotEmpty()) {
      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          shape = RoundedCornerShape(22.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Text(
              text = "Channel Breakdown (CBE & M-Pesa)",
              style = MaterialTheme.typography.titleLarge,
            )
            Spacer(modifier = Modifier.height(12.dp))
            SourceComparisonCardContent(
              summaries = snapshot.sourceSummaries,
              currency = currency,
            )
          }
        }
      }
    }

    // Top Merchants / Counterparties
    if (snapshot.topMerchants.isNotEmpty()) {
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
            Text(
              text = "Top Recipients & Merchants",
              style = MaterialTheme.typography.titleLarge,
            )
            snapshot.topMerchants.forEachIndexed { idx, stat ->
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Box(
                  modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                  contentAlignment = Alignment.Center,
                ) {
                  Text(
                    text = "${idx + 1}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                  )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = stat.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                  ) {
                    SourceBadge(source = stat.source)
                    Text(
                      text = "${stat.count} payments",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                }
                Text(
                  text = if (state.isBalancesMasked) "$currency ••••"
                  else FormatUtils.formatAmount(stat.amount, currency, false),
                  style = MonoAmountSmall,
                  fontWeight = FontWeight.Bold,
                  color = ExpenseCoral,
                )
              }
            }
          }
        }
      }
    }

    item { Spacer(modifier = Modifier.height(72.dp)) }
  }
}

@Composable
private fun AnalyticsKpiCard(
  label: String,
  value: String,
  accent: Color,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 1.dp,
    shape = RoundedCornerShape(18.dp),
    modifier = modifier,
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = value,
        style = MonoAmountMedium,
        fontWeight = FontWeight.Bold,
        color = accent,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}
