package com.example.ui.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FormatUtils {
  private val moneyFormatter = NumberFormat.getNumberInstance(Locale.US).apply {
    minimumFractionDigits = 2
    maximumFractionDigits = 2
  }

  private val compactFormatter = NumberFormat.getNumberInstance(Locale.US).apply {
    minimumFractionDigits = 0
    maximumFractionDigits = 0
  }

  fun formatAmount(amount: Double, currency: String = "ETB", showDecimals: Boolean = true): String {
    val formatted = if (showDecimals) moneyFormatter.format(amount) else compactFormatter.format(amount)
    return "$currency $formatted"
  }

  fun formatShortNumber(amount: Double): String {
    return when {
      amount >= 1_000_000 -> String.format(Locale.US, "%.1fM", amount / 1_000_000.0)
      amount >= 1_000 -> String.format(Locale.US, "%.1fk", amount / 1_000.0)
      else -> compactFormatter.format(amount)
    }
  }

  fun formatDateHeader(timestampMillis: Long): String {
    val sdf = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
    return sdf.format(Date(timestampMillis))
  }

  fun formatTimeOnly(timestampMillis: Long): String {
    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
    return sdf.format(Date(timestampMillis))
  }

  fun formatDateTimeShort(timestampMillis: Long): String {
    if (timestampMillis <= 0L) return "Never"
    val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
    return sdf.format(Date(timestampMillis))
  }

  fun formatDateOnly(timestampMillis: Long): String {
    val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return sdf.format(Date(timestampMillis))
  }
}
