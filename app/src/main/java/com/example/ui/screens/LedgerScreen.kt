package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.analytics.MonthFilterOption
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionSource
import com.example.data.model.TransactionType
import com.example.ui.components.MonthSelectorStrip
import com.example.ui.components.TransactionRowCard
import com.example.ui.theme.ExpenseCoral
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.MonoAmountSmall
import com.example.ui.util.FormatUtils
import com.example.ui.viewmodel.LedgerUiState

@Composable
fun LedgerScreen(
  state: LedgerUiState,
  onSearchQueryChange: (String) -> Unit,
  onSelectMonth: (MonthFilterOption?) -> Unit,
  onTypeFilterChange: (TransactionType?) -> Unit,
  onSourceFilterChange: (TransactionSource?) -> Unit,
  onTransactionClick: (TransactionEntity) -> Unit,
  onAddManualClick: () -> Unit,
  onParseSmsClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val groupedByDate = remember(state.filteredLedgerTransactions) {
    state.filteredLedgerTransactions.groupBy { FormatUtils.formatDateHeader(it.timestampMillis) }
  }

  Column(modifier = modifier.fillMaxSize()) {
    // Search & Monthly Segregation Header
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      OutlinedTextField(
        value = state.searchQuery,
        onValueChange = onSearchQueryChange,
        placeholder = { Text("Search merchant, category, or ref...") },
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Search") },
        trailingIcon = {
          if (state.searchQuery.isNotEmpty()) {
            IconButton(onClick = { onSearchQueryChange("") }) {
              Icon(Icons.Rounded.Clear, contentDescription = "Clear search")
            }
          }
        },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("ledger_search_input"),
      )

      // Segregate tracking by Month (January, February, ...)
      MonthSelectorStrip(
        months = state.rollingMonths,
        selectedMonth = state.selectedLedgerMonth,
        onSelectMonth = onSelectMonth,
      )

      // Type & Source Filter Chips
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        FilterChip(
          selected = state.ledgerTypeFilter == null && state.ledgerSourceFilter == null,
          onClick = {
            onTypeFilterChange(null)
            onSourceFilterChange(null)
          },
          label = { Text("All (${state.filteredLedgerTransactions.size})") },
        )
        FilterChip(
          selected = state.ledgerTypeFilter == TransactionType.EXPENSE,
          onClick = {
            onTypeFilterChange(
              if (state.ledgerTypeFilter == TransactionType.EXPENSE) null else TransactionType.EXPENSE,
            )
          },
          label = { Text("Expenses") },
        )
        FilterChip(
          selected = state.ledgerTypeFilter == TransactionType.INCOME,
          onClick = {
            onTypeFilterChange(
              if (state.ledgerTypeFilter == TransactionType.INCOME) null else TransactionType.INCOME,
            )
          },
          label = { Text("Income") },
        )
        FilterChip(
          selected = state.ledgerSourceFilter == TransactionSource.CBE,
          onClick = {
            onSourceFilterChange(
              if (state.ledgerSourceFilter == TransactionSource.CBE) null else TransactionSource.CBE,
            )
          },
          label = { Text("CBE") },
        )
        FilterChip(
          selected = state.ledgerSourceFilter == TransactionSource.MPESA,
          onClick = {
            onSourceFilterChange(
              if (state.ledgerSourceFilter == TransactionSource.MPESA) null else TransactionSource.MPESA,
            )
          },
          label = { Text("M-Pesa") },
        )
      }

      // Monthly Inflow / Outflow Summary Pill when segregated to a month
      state.selectedLedgerMonth?.let { month ->
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column {
              Text(
                text = month.yearMonthLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = "${state.filteredLedgerTransactions.size} entries",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
              Text(
                text = if (state.isBalancesMasked) "+••••"
                else "+${FormatUtils.formatShortNumber(state.ledgerMonthIncome)}",
                style = MonoAmountSmall,
                color = IncomeGreen,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = if (state.isBalancesMasked) "-••••"
                else "-${FormatUtils.formatShortNumber(state.ledgerMonthExpense)}",
                style = MonoAmountSmall,
                color = ExpenseCoral,
                fontWeight = FontWeight.Bold,
              )
            }
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedButton(
          onClick = onParseSmsClick,
          modifier = Modifier
            .weight(1f)
            .testTag("ledger_parse_sms_button"),
        ) {
          Icon(Icons.Rounded.Sms, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Parse SMS")
        }
        OutlinedButton(
          onClick = onAddManualClick,
          modifier = Modifier
            .weight(1f)
            .testTag("ledger_add_manual_button"),
        ) {
          Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Add Entry")
        }
      }
    }

    if (groupedByDate.isEmpty()) {
      Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 12.dp),
      ) {
        Column(
          modifier = Modifier.padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Text(
            text = state.selectedLedgerMonth?.let {
              "No transactions in ${it.yearMonthLabel}"
            } ?: if (state.transactions.isEmpty()) {
              "No transactions recorded yet"
            } else {
              "No transactions match your filters"
            },
            style = MaterialTheme.typography.titleMedium,
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "All CBE and M-Pesa records from the last 365 days appear here automatically.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        groupedByDate.forEach { (dateHeader, dayTransactions) ->
          item(key = "header_$dateHeader") {
            Text(
              text = dateHeader,
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
            )
          }

          items(
            items = dayTransactions,
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
  }
}
