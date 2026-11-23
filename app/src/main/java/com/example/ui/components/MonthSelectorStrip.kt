package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.analytics.MonthFilterOption
import java.util.Calendar

@Composable
fun MonthSelectorStrip(
  months: List<MonthFilterOption>,
  selectedMonth: MonthFilterOption?,
  onSelectMonth: (MonthFilterOption?) -> Unit,
  modifier: Modifier = Modifier,
) {
  val currentYear = Calendar.getInstance().get(Calendar.YEAR)

  Row(
    modifier = modifier
      .fillMaxWidth()
      .horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    FilterChip(
      selected = selectedMonth == null,
      onClick = { onSelectMonth(null) },
      leadingIcon = {
        Icon(
          Icons.Rounded.CalendarMonth,
          contentDescription = null,
          modifier = Modifier.size(16.dp),
        )
      },
      label = { Text("All Year") },
      modifier = Modifier.testTag("month_chip_all"),
    )

    months.forEach { month ->
      val labelText = if (month.year == currentYear) {
        month.monthName
      } else {
        "${month.monthName} '${month.year % 100}"
      }
      FilterChip(
        selected = selectedMonth?.id == month.id,
        onClick = {
          onSelectMonth(if (selectedMonth?.id == month.id) null else month)
        },
        label = { Text(labelText) },
        modifier = Modifier.testTag("month_chip_${month.id}"),
      )
    }
  }
}
