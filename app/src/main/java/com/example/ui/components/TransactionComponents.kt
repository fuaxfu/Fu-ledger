package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CallReceived
import androidx.compose.material.icons.automirrored.rounded.CompareArrows
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Atm
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.WorkOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.ExpenseCategory
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionSource
import com.example.data.model.TransactionType
import com.example.ui.theme.CbeGold
import com.example.ui.theme.ExpenseCoral
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.MonoAmountMedium
import com.example.ui.theme.MonoAmountSmall
import com.example.ui.theme.MpesaGreen
import com.example.ui.util.FormatUtils

fun categoryIcon(category: ExpenseCategory): ImageVector = when (category) {
  ExpenseCategory.TRANSFER -> Icons.AutoMirrored.Rounded.CompareArrows
  ExpenseCategory.BILLS_AIRTIME -> Icons.Rounded.PhoneAndroid
  ExpenseCategory.MERCHANT_SHOPPING -> Icons.Rounded.ShoppingBag
  ExpenseCategory.FOOD_DINING -> Icons.Rounded.Restaurant
  ExpenseCategory.TRANSPORT -> Icons.Rounded.DirectionsCar
  ExpenseCategory.CASH_WITHDRAWAL -> Icons.Rounded.Atm
  ExpenseCategory.FEES_CHARGES -> Icons.Rounded.ReceiptLong
  ExpenseCategory.OTHER_EXPENSE -> Icons.Rounded.Payments
  ExpenseCategory.SALARY_INCOME -> Icons.Rounded.WorkOutline
  ExpenseCategory.RECEIVED_TRANSFER -> Icons.AutoMirrored.Rounded.CallReceived
  ExpenseCategory.DEPOSIT_CREDIT -> Icons.Rounded.Savings
  ExpenseCategory.REFUND -> Icons.Rounded.Replay
  ExpenseCategory.OTHER_INCOME -> Icons.Rounded.AccountBalanceWallet
}

@Composable
fun SourceBadge(source: TransactionSource, modifier: Modifier = Modifier) {
  val (bg, fg) = when (source) {
    TransactionSource.CBE -> CbeGold.copy(alpha = 0.16f) to CbeGold
    TransactionSource.MPESA -> MpesaGreen.copy(alpha = 0.16f) to MpesaGreen
    TransactionSource.MANUAL -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
  }

  Surface(
    color = bg,
    shape = RoundedCornerShape(6.dp),
    modifier = modifier,
  ) {
    Text(
      text = source.shortBadge,
      style = MaterialTheme.typography.labelSmall,
      fontWeight = FontWeight.Bold,
      color = fg,
      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
    )
  }
}

@Composable
fun TransactionRowCard(
  transaction: TransactionEntity,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  showDecimals: Boolean = true,
  hideAmount: Boolean = false,
) {
  val isExpense = transaction.type == TransactionType.EXPENSE
  val amountColor = if (isExpense) ExpenseCoral else IncomeGreen
  val prefix = if (isExpense) "-" else "+"

  Surface(
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 1.dp,
    shape = RoundedCornerShape(16.dp),
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .clickable(onClick = onClick)
      .testTag("transaction_item_${transaction.id}"),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier = Modifier
          .size(44.dp)
          .clip(CircleShape)
          .background(
            if (isExpense) ExpenseCoral.copy(alpha = 0.12f)
            else IncomeGreen.copy(alpha = 0.12f),
          ),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = categoryIcon(transaction.category),
          contentDescription = transaction.category.displayName,
          tint = amountColor,
          modifier = Modifier.size(22.dp),
        )
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = transaction.counterparty,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(3.dp))
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          SourceBadge(source = transaction.source)
          Text(
            text = "${transaction.category.displayName} • ${FormatUtils.formatTimeOnly(transaction.timestampMillis)}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }

      Spacer(modifier = Modifier.width(8.dp))

      Column(horizontalAlignment = Alignment.End) {
        Text(
          text = if (hideAmount) {
            "$prefix${transaction.currency} ••••"
          } else {
            "$prefix${FormatUtils.formatAmount(transaction.amount, transaction.currency, showDecimals)}"
          },
          style = MonoAmountMedium,
          color = amountColor,
        )
        transaction.balanceAfter?.let { bal ->
          Text(
            text = if (hideAmount) "Bal ••••" else "Bal ${FormatUtils.formatShortNumber(bal)}",
            style = MonoAmountSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}

@Composable
fun EmptyVaultCard(
  hasSmsPermission: Boolean,
  onGrantSmsPermission: () -> Unit,
  onParseSmsClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = RoundedCornerShape(22.dp),
    modifier = modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(18.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Image(
        painter = painterResource(id = R.drawable.img_local_vault_1795435746950),
        contentDescription = "Local encrypted expense vault illustration",
        contentScale = ContentScale.Crop,
        modifier = Modifier
          .fillMaxWidth()
          .height(146.dp)
          .clip(RoundedCornerShape(16.dp)),
      )

      Spacer(modifier = Modifier.height(16.dp))

      Text(
        text = if (hasSmsPermission) {
          "Ready for Real-Time Bank SMS"
        } else {
          "Private On-Device Expense Vault"
        },
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
      )

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = if (hasSmsPermission) {
          "No CBE or M-Pesa SMS found in the last 365 days yet. Incoming messages will auto-log in real time, or you can paste an SMS or add an entry."
        } else {
          "Grant SMS access to automatically extract CBE (debit/transfer & credit/receive) and M-Pesa (bought/sent & receive) messages. All history stays strictly on your device for 1 year."
        },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      Spacer(modifier = Modifier.height(16.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        if (!hasSmsPermission) {
          Button(
            onClick = onGrantSmsPermission,
            modifier = Modifier
              .weight(1f)
              .testTag("empty_grant_sms_button"),
          ) {
            Icon(Icons.Rounded.Sms, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Grant SMS")
          }
        }
        OutlinedButton(
          onClick = onParseSmsClick,
          modifier = Modifier
            .weight(1f)
            .testTag("empty_parse_sms_button"),
        ) {
          Text("Parse SMS Text")
        }
      }
    }
  }
}
