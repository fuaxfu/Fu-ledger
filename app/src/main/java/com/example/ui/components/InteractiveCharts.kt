package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.analytics.CashFlowBucket
import com.example.data.analytics.CategorySlice
import com.example.data.analytics.SourceSummary
import com.example.data.model.TransactionSource
import com.example.ui.theme.CbeGold
import com.example.ui.theme.ChartPalette
import com.example.ui.theme.ExpenseCoral
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.MonoAmountMedium
import com.example.ui.theme.MonoAmountSmall
import com.example.ui.theme.MpesaGreen
import com.example.ui.util.FormatUtils
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max

@Composable
fun InteractiveCashFlowChart(
  buckets: List<CashFlowBucket>,
  currency: String,
  modifier: Modifier = Modifier,
) {
  var selectedIndex by remember(buckets) {
    mutableIntStateOf(
      buckets.indexOfLast { it.income > 0.0 || it.expense > 0.0 }
        .takeIf { it >= 0 } ?: (buckets.lastIndex.coerceAtLeast(0)),
    )
  }

  val progress = remember(buckets) { Animatable(0f) }
  LaunchedEffect(buckets) {
    progress.snapTo(0f)
    progress.animateTo(
      targetValue = 1f,
      animationSpec = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow,
      ),
    )
  }

  val maxValue = remember(buckets) {
    val maxData = buckets.maxOfOrNull { max(it.income, it.expense) } ?: 0.0
    if (maxData <= 0.0) 1000.0 else maxData * 1.15
  }

  val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
  val highlightBandColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
  val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

  Column(modifier = modifier.fillMaxWidth()) {
    // Interactive Inspector Pill
    val selectedBucket = buckets.getOrNull(selectedIndex)
    AnimatedVisibility(
      visible = selectedBucket != null,
      enter = fadeIn(),
      exit = fadeOut(),
    ) {
      if (selectedBucket != null) {
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = selectedBucket.fullLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
              )
              val net = selectedBucket.income - selectedBucket.expense
              Text(
                text = "Net ${if (net >= 0) "+" else "-"}${FormatUtils.formatAmount(kotlin.math.abs(net), currency, false)}",
                style = MonoAmountSmall,
                color = if (net >= 0) IncomeGreen else ExpenseCoral,
              )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
              LegendMetric(
                label = "In",
                amount = FormatUtils.formatShortNumber(selectedBucket.income),
                color = IncomeGreen,
              )
              LegendMetric(
                label = "Out",
                amount = FormatUtils.formatShortNumber(selectedBucket.expense),
                color = ExpenseCoral,
              )
            }
          }
        }
      }
    }

    if (buckets.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(170.dp),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = "No activity in this range",
          style = MaterialTheme.typography.bodyMedium,
          color = labelColor,
        )
      }
      return
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      // Y-Axis Labels
      Column(
        modifier = Modifier
          .height(168.dp)
          .padding(end = 6.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.End,
      ) {
        Text(
          text = FormatUtils.formatShortNumber(maxValue),
          style = MonoAmountSmall,
          color = labelColor,
        )
        Text(
          text = FormatUtils.formatShortNumber(maxValue / 2.0),
          style = MonoAmountSmall,
          color = labelColor,
        )
        Text(
          text = "0",
          style = MonoAmountSmall,
          color = labelColor,
        )
      }

      // Interactive Bar Canvas
      Canvas(
        modifier = Modifier
          .weight(1f)
          .height(168.dp)
          .testTag("cash_flow_chart_canvas")
          .semantics { contentDescription = "Interactive cash flow chart. Tap or drag to inspect periods." }
          .pointerInput(buckets) {
            detectTapGestures { offset ->
              if (buckets.isNotEmpty() && size.width > 0) {
                val slotWidth = size.width.toFloat() / buckets.size
                selectedIndex = (offset.x / slotWidth).toInt().coerceIn(0, buckets.lastIndex)
              }
            }
          }
          .pointerInput(buckets) {
            detectHorizontalDragGestures { change, _ ->
              change.consume()
              if (buckets.isNotEmpty() && size.width > 0) {
                val slotWidth = size.width.toFloat() / buckets.size
                selectedIndex = (change.position.x / slotWidth).toInt().coerceIn(0, buckets.lastIndex)
              }
            }
          },
      ) {
        val chartHeight = size.height
        val slotWidth = size.width / buckets.size
        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)

        // Horizontal grid lines (top, mid, bottom)
        listOf(0.05f, 0.5f, 0.98f).forEach { ratio ->
          val y = chartHeight * ratio
          drawLine(
            color = gridColor,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1.dp.toPx(),
            pathEffect = if (ratio < 0.95f) dashEffect else null,
          )
        }

        val usableHeight = chartHeight * 0.92f
        val baseY = chartHeight * 0.98f
        val barWidth = (slotWidth * 0.28f).coerceIn(5.dp.toPx(), 16.dp.toPx())
        val gapBetweenPair = (barWidth * 0.28f).coerceAtLeast(2.dp.toPx())

        buckets.forEachIndexed { index, bucket ->
          val centerX = index * slotWidth + slotWidth / 2f
          val isSelected = index == selectedIndex

          if (isSelected) {
            drawRoundRect(
              color = highlightBandColor,
              topLeft = Offset(index * slotWidth + 2.dp.toPx(), 0f),
              size = Size((slotWidth - 4.dp.toPx()).coerceAtLeast(1f), baseY),
              cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
            )
          }

          val incomeHeight = ((bucket.income / maxValue).toFloat() * usableHeight * progress.value)
            .coerceAtLeast(if (bucket.income > 0) 4.dp.toPx() else 0f)
          val expenseHeight = ((bucket.expense / maxValue).toFloat() * usableHeight * progress.value)
            .coerceAtLeast(if (bucket.expense > 0) 4.dp.toPx() else 0f)

          val incomeX = centerX - gapBetweenPair / 2f - barWidth
          val expenseX = centerX + gapBetweenPair / 2f

          if (incomeHeight > 0f) {
            drawRoundRect(
              color = if (isSelected) IncomeGreen else IncomeGreen.copy(alpha = 0.72f),
              topLeft = Offset(incomeX, baseY - incomeHeight),
              size = Size(barWidth, incomeHeight),
              cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
            )
          }

          if (expenseHeight > 0f) {
            drawRoundRect(
              color = if (isSelected) ExpenseCoral else ExpenseCoral.copy(alpha = 0.72f),
              topLeft = Offset(expenseX, baseY - expenseHeight),
              size = Size(barWidth, expenseHeight),
              cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(6.dp))

    // X-Axis Labels aligned to slots
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(start = 34.dp),
    ) {
      val step = if (buckets.size > 8) 2 else 1
      buckets.forEachIndexed { idx, bucket ->
        val showLabel = idx % step == 0 || idx == selectedIndex
        Text(
          text = if (showLabel) bucket.label else "",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = if (idx == selectedIndex) FontWeight.Bold else FontWeight.Medium,
          color = if (idx == selectedIndex) MaterialTheme.colorScheme.primary else labelColor,
          textAlign = TextAlign.Center,
          maxLines = 1,
          overflow = TextOverflow.Clip,
          modifier = Modifier.weight(1f),
        )
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Chart Legend
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      LegendDot(color = IncomeGreen, label = "Income")
      Spacer(modifier = Modifier.width(20.dp))
      LegendDot(color = ExpenseCoral, label = "Expenses")
    }
  }
}

@Composable
fun InteractiveCategoryDonutChart(
  slices: List<CategorySlice>,
  totalAmount: Double,
  currency: String,
  selectedIndex: Int?,
  onSelectIndex: (Int?) -> Unit,
  modifier: Modifier = Modifier,
) {
  val sweepProgress = remember(slices) { Animatable(0f) }
  LaunchedEffect(slices) {
    sweepProgress.snapTo(0f)
    sweepProgress.animateTo(
      targetValue = 1f,
      animationSpec = tween(durationMillis = 550),
    )
  }

  val activeSlice = selectedIndex?.let { slices.getOrNull(it) }
  val emptyTrackColor = MaterialTheme.colorScheme.surfaceVariant

  Column(
    modifier = modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Box(
      modifier = Modifier
        .size(216.dp)
        .padding(8.dp),
      contentAlignment = Alignment.Center,
    ) {
      Canvas(
        modifier = Modifier
          .matchParentSize()
          .testTag("category_donut_canvas")
          .semantics { contentDescription = "Interactive category spending donut chart" }
          .pointerInput(slices) {
            detectTapGestures { offset ->
              if (slices.isEmpty() || totalAmount <= 0.0) return@detectTapGestures
              val cx = size.width / 2f
              val cy = size.height / 2f
              val dx = offset.x - cx
              val dy = offset.y - cy
              val dist = hypot(dx, dy)
              val outerR = minOf(size.width, size.height).toFloat() / 2f
              val innerR = outerR * 0.45f
              if (dist < innerR || dist > outerR) {
                onSelectIndex(null)
                return@detectTapGestures
              }

              // Angle starting at -90 deg (12 o'clock), moving clockwise
              var angleDeg = (atan2(dy, dx) * 180.0 / PI).toFloat() + 90f
              if (angleDeg < 0f) angleDeg += 360f

              var cumulative = 0f
              slices.forEachIndexed { idx, slice ->
                val sliceAngle = ((slice.amount / totalAmount) * 360.0).toFloat()
                if (angleDeg >= cumulative && angleDeg <= cumulative + sliceAngle) {
                  onSelectIndex(if (selectedIndex == idx) null else idx)
                  return@detectTapGestures
                }
                cumulative += sliceAngle
              }
            }
          },
      ) {
        val baseStroke = 26.dp.toPx()
        val inset = baseStroke / 2f + 6.dp.toPx()
        val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
        val topLeft = Offset(inset, inset)

        if (slices.isEmpty() || totalAmount <= 0.0) {
          drawArc(
            color = emptyTrackColor,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = baseStroke),
          )
        } else {
          var startAngle = -90f
          val gapAngle = if (slices.size > 1) 2.8f else 0f
          slices.forEachIndexed { idx, slice ->
            val rawSweep = ((slice.amount / totalAmount) * 360.0).toFloat() * sweepProgress.value
            val sweep = (rawSweep - gapAngle).coerceAtLeast(1.5f)
            val isSelected = selectedIndex == idx
            val isDimmed = selectedIndex != null && !isSelected
            val color = ChartPalette[idx % ChartPalette.size].let {
              if (isDimmed) it.copy(alpha = 0.35f) else it
            }
            val strokeWidth = if (isSelected) baseStroke + 8.dp.toPx() else baseStroke

            drawArc(
              color = color,
              startAngle = startAngle,
              sweepAngle = sweep,
              useCenter = false,
              topLeft = topLeft,
              size = arcSize,
              style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
            )
            startAngle += rawSweep
          }
        }
      }

      // Center Readout
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 24.dp),
      ) {
        Text(
          text = activeSlice?.category?.displayName ?: "Total Spent",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = FormatUtils.formatAmount(activeSlice?.amount ?: totalAmount, currency, false),
          style = MonoAmountMedium,
          color = MaterialTheme.colorScheme.onSurface,
          textAlign = TextAlign.Center,
        )
        if (activeSlice != null) {
          Text(
            text = "${String.format(java.util.Locale.US, "%.1f", activeSlice.percentage)}% • ${activeSlice.count} tx",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Interactive Legend Rows
    Column(
      modifier = Modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      slices.forEachIndexed { idx, slice ->
        val isSelected = selectedIndex == idx
        val sliceColor = ChartPalette[idx % ChartPalette.size]
        val rowAlpha by animateFloatAsState(
          targetValue = if (selectedIndex == null || isSelected) 1f else 0.55f,
          label = "slice_row_alpha",
        )

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
              if (isSelected) sliceColor.copy(alpha = 0.12f)
              else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            )
            .border(
              width = if (isSelected) 1.dp else 0.dp,
              color = if (isSelected) sliceColor.copy(alpha = 0.6f) else Color.Transparent,
              shape = RoundedCornerShape(12.dp),
            )
            .clickable { onSelectIndex(if (isSelected) null else idx) }
            .padding(horizontal = 12.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Box(
            modifier = Modifier
              .size(11.dp)
              .clip(CircleShape)
              .background(sliceColor.copy(alpha = rowAlpha)),
          )
          Spacer(modifier = Modifier.width(10.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = slice.category.displayName,
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface.copy(alpha = rowAlpha),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = "${slice.count} transactions",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Column(horizontalAlignment = Alignment.End) {
            Text(
              text = FormatUtils.formatAmount(slice.amount, currency, false),
              style = MonoAmountSmall,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface.copy(alpha = rowAlpha),
            )
            Text(
              text = "${String.format(java.util.Locale.US, "%.1f", slice.percentage)}%",
              style = MaterialTheme.typography.labelSmall,
              color = sliceColor,
            )
          }
        }
      }
    }
  }
}

@Composable
fun SourceComparisonCardContent(
  summaries: List<SourceSummary>,
  currency: String,
) {
  val totalVolume = summaries.sumOf { it.income + it.expense }.coerceAtLeast(1.0)
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    // Proportional split bar
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(12.dp)
        .clip(RoundedCornerShape(6.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
      summaries.forEach { summary ->
        val vol = (summary.income + summary.expense)
        val fraction = (vol / totalVolume).toFloat()
        if (fraction > 0.01f) {
          val barColor = when (summary.source) {
            TransactionSource.CBE -> CbeGold
            TransactionSource.MPESA -> MpesaGreen
            TransactionSource.MANUAL -> MaterialTheme.colorScheme.secondary
          }
          Box(
            modifier = Modifier
              .weight(fraction)
              .height(12.dp)
              .background(barColor),
          )
        }
      }
    }

    summaries.forEach { summary ->
      val color = when (summary.source) {
        TransactionSource.CBE -> CbeGold
        TransactionSource.MPESA -> MpesaGreen
        TransactionSource.MANUAL -> MaterialTheme.colorScheme.secondary
      }
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(10.dp)
              .clip(CircleShape)
              .background(color),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Column {
            Text(
              text = summary.source.displayName,
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.SemiBold,
            )
            Text(
              text = "${summary.count} logged transactions",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
        Column(horizontalAlignment = Alignment.End) {
          Text(
            text = "+${FormatUtils.formatShortNumber(summary.income)} $currency",
            style = MonoAmountSmall,
            color = IncomeGreen,
          )
          Text(
            text = "-${FormatUtils.formatShortNumber(summary.expense)} $currency",
            style = MonoAmountSmall,
            color = ExpenseCoral,
          )
        }
      }
    }
  }
}

@Composable
private fun LegendMetric(label: String, amount: String, color: Color) {
  Column(horizontalAlignment = Alignment.End) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
      text = amount,
      style = MonoAmountSmall,
      fontWeight = FontWeight.Bold,
      color = color,
    )
  }
}

@Composable
private fun LegendDot(color: Color, label: String) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Box(
      modifier = Modifier
        .size(9.dp)
        .clip(CircleShape)
        .background(color),
    )
    Spacer(modifier = Modifier.width(6.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}
