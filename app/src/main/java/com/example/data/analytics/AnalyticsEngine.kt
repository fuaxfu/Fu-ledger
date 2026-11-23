package com.example.data.analytics

import com.example.data.model.ExpenseCategory
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionSource
import com.example.data.model.TransactionType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

enum class AnalyticsPeriod(val label: String, val days: Int) {
  DAYS_7("7D", 7),
  DAYS_30("30D", 30),
  DAYS_90("90D", 90),
  YEAR_1("1Y", 365),
}

data class MonthFilterOption(
  val id: String,
  val year: Int,
  val monthZeroBased: Int,
  val monthName: String,
  val shortName: String,
  val yearMonthLabel: String,
  val startMillis: Long,
  val endMillis: Long,
)

data class MonthlyBreakdownSummary(
  val month: MonthFilterOption,
  val income: Double,
  val expense: Double,
  val net: Double,
  val count: Int,
)

data class CashFlowBucket(
  val label: String,
  val fullLabel: String,
  val income: Double,
  val expense: Double,
)

data class CategorySlice(
  val category: ExpenseCategory,
  val amount: Double,
  val percentage: Float,
  val count: Int,
)

data class CounterpartyStat(
  val name: String,
  val amount: Double,
  val count: Int,
  val source: TransactionSource,
)

data class SourceSummary(
  val source: TransactionSource,
  val income: Double,
  val expense: Double,
  val count: Int,
)

data class AnalyticsSnapshot(
  val totalIncome: Double,
  val totalExpense: Double,
  val netCashFlow: Double,
  val savingsRatePercent: Int,
  val buckets: List<CashFlowBucket>,
  val expenseSlices: List<CategorySlice>,
  val incomeSlices: List<CategorySlice>,
  val sourceSummaries: List<SourceSummary>,
  val topMerchants: List<CounterpartyStat>,
  val monthlyBreakdowns: List<MonthlyBreakdownSummary>,
  val primaryCurrency: String,
)

object AnalyticsEngine {

  fun buildRolling12Months(nowMillis: Long = System.currentTimeMillis()): List<MonthFilterOption> {
    val cal = Calendar.getInstance().apply {
      timeInMillis = nowMillis
      set(Calendar.DAY_OF_MONTH, 1)
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }

    val fullMonthFmt = SimpleDateFormat("MMMM", Locale.getDefault())
    val shortMonthFmt = SimpleDateFormat("MMM", Locale.getDefault())
    val yearMonthFmt = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

    val list = mutableListOf<MonthFilterOption>()
    repeat(12) {
      val start = cal.timeInMillis
      val year = cal.get(Calendar.YEAR)
      val month = cal.get(Calendar.MONTH)
      val nextCal = (cal.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
      val end = nextCal.timeInMillis
      val date = Date(start)
      list.add(
        MonthFilterOption(
          id = "$year-${month + 1}",
          year = year,
          monthZeroBased = month,
          monthName = fullMonthFmt.format(date),
          shortName = shortMonthFmt.format(date),
          yearMonthLabel = yearMonthFmt.format(date),
          startMillis = start,
          endMillis = end,
        ),
      )
      cal.add(Calendar.MONTH, -1)
    }
    return list
  }

  fun compute(
    allTransactions: List<TransactionEntity>,
    period: AnalyticsPeriod,
    sourceFilter: TransactionSource? = null,
    selectedMonth: MonthFilterOption? = null,
    nowMillis: Long = System.currentTimeMillis(),
    preferredCurrency: String = "ETB",
  ): AnalyticsSnapshot {
    val filtered = if (selectedMonth != null) {
      allTransactions.filter { tx ->
        tx.timestampMillis in selectedMonth.startMillis until selectedMonth.endMillis &&
          (sourceFilter == null || tx.source == sourceFilter)
      }
    } else {
      val startCutoff = nowMillis - TimeUnit.DAYS.toMillis(period.days.toLong())
      allTransactions.filter { tx ->
        tx.timestampMillis >= startCutoff &&
          (sourceFilter == null || tx.source == sourceFilter)
      }
    }

    val totalIncome = filtered
      .filter { it.type == TransactionType.INCOME }
      .sumOf { it.amount }
    val totalExpense = filtered
      .filter { it.type == TransactionType.EXPENSE }
      .sumOf { it.amount }
    val net = totalIncome - totalExpense
    val savingsRate = if (totalIncome > 0.0) {
      ((net / totalIncome) * 100.0).roundToInt().coerceIn(-999, 100)
    } else {
      0
    }

    val primaryCurrency = filtered
      .groupingBy { it.currency }
      .eachCount()
      .maxByOrNull { it.value }
      ?.key ?: preferredCurrency

    val buckets = if (selectedMonth != null) {
      buildBucketsForSpecificMonth(filtered, selectedMonth)
    } else {
      buildBuckets(filtered, period, nowMillis)
    }

    val expenseSlices = buildSlices(filtered.filter { it.type == TransactionType.EXPENSE }, totalExpense)
    val incomeSlices = buildSlices(filtered.filter { it.type == TransactionType.INCOME }, totalIncome)

    val sourceSummaries = listOf(TransactionSource.CBE, TransactionSource.MPESA, TransactionSource.MANUAL)
      .map { src ->
        val srcTx = if (selectedMonth != null) {
          allTransactions.filter {
            it.timestampMillis in selectedMonth.startMillis until selectedMonth.endMillis && it.source == src
          }
        } else {
          val startCutoff = nowMillis - TimeUnit.DAYS.toMillis(period.days.toLong())
          allTransactions.filter { it.timestampMillis >= startCutoff && it.source == src }
        }
        SourceSummary(
          source = src,
          income = srcTx.filter { it.type == TransactionType.INCOME }.sumOf { it.amount },
          expense = srcTx.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount },
          count = srcTx.size,
        )
      }
      .filter { it.count > 0 || it.source != TransactionSource.MANUAL }

    val topMerchants = filtered
      .filter { it.type == TransactionType.EXPENSE }
      .groupBy { it.counterparty }
      .map { (name, list) ->
        CounterpartyStat(
          name = name,
          amount = list.sumOf { it.amount },
          count = list.size,
          source = list.first().source,
        )
      }
      .sortedByDescending { it.amount }
      .take(5)

    val monthlyBreakdowns = buildRolling12Months(nowMillis).map { month ->
      val monthTx = allTransactions.filter { tx ->
        tx.timestampMillis in month.startMillis until month.endMillis &&
          (sourceFilter == null || tx.source == sourceFilter)
      }
      val inc = monthTx.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
      val exp = monthTx.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
      MonthlyBreakdownSummary(
        month = month,
        income = inc,
        expense = exp,
        net = inc - exp,
        count = monthTx.size,
      )
    }

    return AnalyticsSnapshot(
      totalIncome = totalIncome,
      totalExpense = totalExpense,
      netCashFlow = net,
      savingsRatePercent = savingsRate,
      buckets = buckets,
      expenseSlices = expenseSlices,
      incomeSlices = incomeSlices,
      sourceSummaries = sourceSummaries,
      topMerchants = topMerchants,
      monthlyBreakdowns = monthlyBreakdowns,
      primaryCurrency = primaryCurrency,
    )
  }

  private fun buildSlices(
    txList: List<TransactionEntity>,
    total: Double,
  ): List<CategorySlice> {
    if (txList.isEmpty() || total <= 0.0) return emptyList()
    return txList
      .groupBy { it.category }
      .map { (cat, list) ->
        val sum = list.sumOf { it.amount }
        CategorySlice(
          category = cat,
          amount = sum,
          percentage = ((sum / total) * 100.0).toFloat(),
          count = list.size,
        )
      }
      .sortedByDescending { it.amount }
  }

  private fun buildBucketsForSpecificMonth(
    transactions: List<TransactionEntity>,
    month: MonthFilterOption,
  ): List<CashFlowBucket> {
    val cal = Calendar.getInstance().apply {
      timeInMillis = month.startMillis
    }
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    // Split month into 6 clean intervals (days 1-5, 6-10, 11-15, 16-20, 21-25, 26-end)
    val ranges = listOf(
      1..5,
      6..10,
      11..15,
      16..20,
      21..25,
      26..daysInMonth,
    )
    return ranges.map { range ->
      val startCal = Calendar.getInstance().apply {
        set(month.year, month.monthZeroBased, range.first, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
      }
      val endCal = Calendar.getInstance().apply {
        set(month.year, month.monthZeroBased, range.last, 23, 59, 59)
        set(Calendar.MILLISECOND, 999)
      }
      val start = startCal.timeInMillis
      val end = endCal.timeInMillis
      val bucketTx = transactions.filter { it.timestampMillis in start..end }
      CashFlowBucket(
        label = "${range.first}-${range.last}",
        fullLabel = "${month.shortName} ${range.first}–${range.last}, ${month.year}",
        income = bucketTx.filter { it.type == TransactionType.INCOME }.sumOf { it.amount },
        expense = bucketTx.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount },
      )
    }
  }

  private fun buildBuckets(
    transactions: List<TransactionEntity>,
    period: AnalyticsPeriod,
    nowMillis: Long,
  ): List<CashFlowBucket> {
    return when (period) {
      AnalyticsPeriod.YEAR_1 -> buildMonthlyBuckets(transactions, nowMillis, months = 12)
      AnalyticsPeriod.DAYS_90 -> buildDayRangeBuckets(transactions, nowMillis, totalDays = 90, bucketCount = 6)
      AnalyticsPeriod.DAYS_30 -> buildDayRangeBuckets(transactions, nowMillis, totalDays = 30, bucketCount = 6)
      AnalyticsPeriod.DAYS_7 -> buildDayRangeBuckets(transactions, nowMillis, totalDays = 7, bucketCount = 7)
    }
  }

  private fun buildMonthlyBuckets(
    transactions: List<TransactionEntity>,
    nowMillis: Long,
    months: Int,
  ): List<CashFlowBucket> {
    val cal = Calendar.getInstance().apply {
      timeInMillis = nowMillis
      set(Calendar.DAY_OF_MONTH, 1)
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
      add(Calendar.MONTH, -(months - 1))
    }

    val shortFmt = SimpleDateFormat("MMM", Locale.getDefault())
    val fullFmt = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

    val result = mutableListOf<CashFlowBucket>()
    repeat(months) {
      val start = cal.timeInMillis
      val label = shortFmt.format(Date(start))
      val fullLabel = fullFmt.format(Date(start))
      cal.add(Calendar.MONTH, 1)
      val end = cal.timeInMillis

      val bucketTx = transactions.filter { it.timestampMillis in start until end }
      result.add(
        CashFlowBucket(
          label = label,
          fullLabel = fullLabel,
          income = bucketTx.filter { it.type == TransactionType.INCOME }.sumOf { it.amount },
          expense = bucketTx.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount },
        ),
      )
    }
    return result
  }

  private fun buildDayRangeBuckets(
    transactions: List<TransactionEntity>,
    nowMillis: Long,
    totalDays: Int,
    bucketCount: Int,
  ): List<CashFlowBucket> {
    val cal = Calendar.getInstance().apply {
      timeInMillis = nowMillis
      set(Calendar.HOUR_OF_DAY, 23)
      set(Calendar.MINUTE, 59)
      set(Calendar.SECOND, 59)
      set(Calendar.MILLISECOND, 999)
    }
    val endOfToday = cal.timeInMillis
    val windowStart = endOfToday - TimeUnit.DAYS.toMillis(totalDays.toLong()) + 1L
    val bucketMillis = TimeUnit.DAYS.toMillis((totalDays / bucketCount).coerceAtLeast(1).toLong())

    val dayShortFmt = SimpleDateFormat(if (totalDays <= 7) "EEE" else "d/M", Locale.getDefault())
    val fullDayFmt = SimpleDateFormat("MMM d", Locale.getDefault())

    val result = mutableListOf<CashFlowBucket>()
    for (i in 0 until bucketCount) {
      val start = windowStart + i * bucketMillis
      val end = if (i == bucketCount - 1) endOfToday + 1 else windowStart + (i + 1) * bucketMillis
      val label = dayShortFmt.format(Date(start))
      val fullLabel = if (totalDays <= 7) {
        fullDayFmt.format(Date(start))
      } else {
        "${fullDayFmt.format(Date(start))} – ${fullDayFmt.format(Date(end - 1))}"
      }
      val bucketTx = transactions.filter { it.timestampMillis in start until end }
      result.add(
        CashFlowBucket(
          label = label,
          fullLabel = fullLabel,
          income = bucketTx.filter { it.type == TransactionType.INCOME }.sumOf { it.amount },
          expense = bucketTx.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount },
        ),
      )
    }
    return result
  }
}
