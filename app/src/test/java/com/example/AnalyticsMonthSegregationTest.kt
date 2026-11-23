package com.example

import com.example.data.analytics.AnalyticsEngine
import com.example.data.analytics.AnalyticsPeriod
import com.example.data.model.ExpenseCategory
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionSource
import com.example.data.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class AnalyticsMonthSegregationTest {

  @Test
  fun segregatesTransactionsIntoJanuaryAndFebruaryProperly() {
    val janCal = Calendar.getInstance().apply {
      set(2026, Calendar.JANUARY, 15, 10, 0, 0)
      set(Calendar.MILLISECOND, 0)
    }
    val febCal = Calendar.getInstance().apply {
      set(2026, Calendar.FEBRUARY, 10, 14, 30, 0)
      set(Calendar.MILLISECOND, 0)
    }
    val novCal = Calendar.getInstance().apply {
      set(2026, Calendar.NOVEMBER, 20, 12, 0, 0)
      set(Calendar.MILLISECOND, 0)
    }

    val janTx = TransactionEntity(
      id = 1L,
      smsSignature = "JAN_1",
      source = TransactionSource.CBE,
      type = TransactionType.INCOME,
      amount = 10_000.0,
      category = ExpenseCategory.SALARY_INCOME,
      counterparty = "Employer",
      timestampMillis = janCal.timeInMillis,
    )
    val febTx = TransactionEntity(
      id = 2L,
      smsSignature = "FEB_1",
      source = TransactionSource.MPESA,
      type = TransactionType.EXPENSE,
      amount = 2_500.0,
      category = ExpenseCategory.MERCHANT_SHOPPING,
      counterparty = "Market",
      timestampMillis = febCal.timeInMillis,
    )

    val allTx = listOf(janTx, febTx)
    val rollingMonths = AnalyticsEngine.buildRolling12Months(nowMillis = novCal.timeInMillis)
    assertEquals(12, rollingMonths.size)

    val janOption = rollingMonths.first { it.monthZeroBased == Calendar.JANUARY && it.year == 2026 }
    val febOption = rollingMonths.first { it.monthZeroBased == Calendar.FEBRUARY && it.year == 2026 }

    assertEquals("January", janOption.monthName)
    assertEquals("February", febOption.monthName)

    val janSnapshot = AnalyticsEngine.compute(
      allTransactions = allTx,
      period = AnalyticsPeriod.YEAR_1,
      selectedMonth = janOption,
      nowMillis = novCal.timeInMillis,
    )
    assertEquals(10_000.0, janSnapshot.totalIncome, 0.01)
    assertEquals(0.0, janSnapshot.totalExpense, 0.01)
    assertTrue(janSnapshot.buckets.isNotEmpty())

    val febSnapshot = AnalyticsEngine.compute(
      allTransactions = allTx,
      period = AnalyticsPeriod.YEAR_1,
      selectedMonth = febOption,
      nowMillis = novCal.timeInMillis,
    )
    assertEquals(0.0, febSnapshot.totalIncome, 0.01)
    assertEquals(2_500.0, febSnapshot.totalExpense, 0.01)
  }
}
