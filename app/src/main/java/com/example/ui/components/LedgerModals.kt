package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.ExpenseCategory
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.ui.theme.ExpenseCoral
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.MonoAmountLarge
import com.example.ui.util.FormatUtils

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionDetailSheet(
  transaction: TransactionEntity,
  onDismiss: () -> Unit,
  onSave: (TransactionEntity) -> Unit,
  onDelete: (Long) -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var selectedCategory by remember(transaction) { mutableStateOf(transaction.category) }
  var noteText by remember(transaction) { mutableStateOf(transaction.note.orEmpty()) }
  var counterpartyText by remember(transaction) { mutableStateOf(transaction.counterparty) }

  val categories = if (transaction.type == TransactionType.EXPENSE) {
    ExpenseCategory.expenseCategories()
  } else {
    ExpenseCategory.incomeCategories()
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp)
        .padding(bottom = 28.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        SourceBadge(source = transaction.source)
        Text(
          text = FormatUtils.formatDateTimeShort(transaction.timestampMillis),
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      val prefix = if (transaction.type == TransactionType.EXPENSE) "-" else "+"
      Text(
        text = "$prefix${FormatUtils.formatAmount(transaction.amount, transaction.currency)}",
        style = MonoAmountLarge,
        color = if (transaction.type == TransactionType.EXPENSE) ExpenseCoral else IncomeGreen,
      )

      transaction.balanceAfter?.let { bal ->
        Text(
          text = "Synced Balance After: ${FormatUtils.formatAmount(bal, transaction.currency)}",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      transaction.referenceCode?.let { ref ->
        Text(
          text = "Ref: $ref",
          style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMono),
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.padding(top = 4.dp),
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      OutlinedTextField(
        value = counterpartyText,
        onValueChange = { counterpartyText = it },
        label = { Text("Counterparty / Merchant") },
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("detail_counterparty_input"),
      )

      Spacer(modifier = Modifier.height(14.dp))

      Text(
        text = "Category",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Spacer(modifier = Modifier.height(8.dp))

      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        categories.forEach { cat ->
          FilterChip(
            selected = selectedCategory == cat,
            onClick = { selectedCategory = cat },
            label = { Text(cat.displayName) },
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      OutlinedTextField(
        value = noteText,
        onValueChange = { noteText = it },
        label = { Text("Personal Note (optional)") },
        maxLines = 3,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("detail_note_input"),
      )

      if (!transaction.rawSmsSnippet.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(14.dp))
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                Icons.Rounded.Lock,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.primary,
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Original SMS Extract (Local Only)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
              )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = transaction.rawSmsSnippet,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(20.dp))
      HorizontalDivider()
      Spacer(modifier = Modifier.height(16.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        OutlinedButton(
          onClick = { onDelete(transaction.id) },
          colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpenseCoral),
          modifier = Modifier
            .weight(1f)
            .testTag("detail_delete_button"),
        ) {
          Icon(Icons.Rounded.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Delete")
        }

        Button(
          onClick = {
            onSave(
              transaction.copy(
                category = selectedCategory,
                counterparty = counterpartyText.trim().ifBlank { transaction.counterparty },
                note = noteText.trim().takeIf { it.isNotEmpty() },
              ),
            )
          },
          modifier = Modifier
            .weight(1.4f)
            .testTag("detail_save_button"),
        ) {
          Text("Save Changes")
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddManualTransactionSheet(
  onDismiss: () -> Unit,
  onSubmit: (
    type: TransactionType,
    amount: Double,
    currency: String,
    category: ExpenseCategory,
    counterparty: String,
    note: String?,
  ) -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var type by remember { mutableStateOf(TransactionType.EXPENSE) }
  var amountText by remember { mutableStateOf("") }
  var currency by remember { mutableStateOf("ETB") }
  var counterparty by remember { mutableStateOf("") }
  var note by remember { mutableStateOf("") }
  var category by remember { mutableStateOf(ExpenseCategory.FOOD_DINING) }
  var errorText by remember { mutableStateOf<String?>(null) }

  val availableCategories = if (type == TransactionType.EXPENSE) {
    ExpenseCategory.expenseCategories()
  } else {
    ExpenseCategory.incomeCategories()
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp)
        .padding(bottom = 28.dp),
    ) {
      Text(
        text = "Add Local Entry",
        style = MaterialTheme.typography.headlineSmall,
      )
      Text(
        text = "Log cash or non-SMS transactions directly to your 1-year vault.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      Spacer(modifier = Modifier.height(14.dp))

      SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        TransactionType.entries.forEachIndexed { index, item ->
          SegmentedButton(
            selected = type == item,
            onClick = {
              type = item
              category = if (item == TransactionType.EXPENSE) {
                ExpenseCategory.FOOD_DINING
              } else {
                ExpenseCategory.SALARY_INCOME
              }
            },
            shape = SegmentedButtonDefaults.itemShape(index = index, count = 2),
          ) {
            Text(item.label)
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedTextField(
          value = amountText,
          onValueChange = {
            amountText = it
            errorText = null
          },
          label = { Text("Amount") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          isError = errorText != null,
          modifier = Modifier
            .weight(1.7f)
            .testTag("manual_amount_input"),
        )
        OutlinedTextField(
          value = currency,
          onValueChange = { currency = it.uppercase().take(4) },
          label = { Text("Currency") },
          singleLine = true,
          modifier = Modifier.weight(0.8f),
        )
      }

      if (errorText != null) {
        Text(
          text = errorText!!,
          style = MaterialTheme.typography.labelMedium,
          color = ExpenseCoral,
          modifier = Modifier.padding(top = 4.dp),
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      OutlinedTextField(
        value = counterparty,
        onValueChange = { counterparty = it },
        label = { Text(if (type == TransactionType.EXPENSE) "Paid to / Merchant" else "Received from / Source") },
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("manual_counterparty_input"),
      )

      Spacer(modifier = Modifier.height(14.dp))

      Text(
        text = "Category",
        style = MaterialTheme.typography.labelLarge,
      )
      Spacer(modifier = Modifier.height(8.dp))

      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        availableCategories.forEach { item ->
          FilterChip(
            selected = category == item,
            onClick = { category = item },
            label = { Text(item.displayName) },
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      OutlinedTextField(
        value = note,
        onValueChange = { note = it },
        label = { Text("Note (optional)") },
        maxLines = 2,
        modifier = Modifier.fillMaxWidth(),
      )

      Spacer(modifier = Modifier.height(20.dp))

      Button(
        onClick = {
          val parsed = amountText.replace(",", "").toDoubleOrNull()
          if (parsed == null || parsed <= 0.0) {
            errorText = "Enter a valid positive amount"
            return@Button
          }
          onSubmit(
            type,
            parsed,
            currency.ifBlank { "ETB" },
            category,
            counterparty.trim(),
            note.trim().takeIf { it.isNotEmpty() },
          )
        },
        modifier = Modifier
          .fillMaxWidth()
          .testTag("manual_submit_button"),
      ) {
        Text("Save to Local Ledger")
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParseSmsSheet(
  onDismiss: () -> Unit,
  onParseAndSave: (sender: String, body: String) -> Boolean,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var sender by remember { mutableStateOf("CBE") }
  var smsBody by remember { mutableStateOf("") }
  var parseError by remember { mutableStateOf<String?>(null) }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp)
        .padding(bottom = 28.dp),
    ) {
      Text(
        text = "Parse Bank SMS",
        style = MaterialTheme.typography.headlineSmall,
      )
      Text(
        text = "Paste a CBE or M-Pesa SMS message to extract amount, type, category, and balance locally.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      Spacer(modifier = Modifier.height(12.dp))

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("CBE", "MPESA").forEach { preset ->
          FilterChip(
            selected = sender.equals(preset, ignoreCase = true),
            onClick = { sender = preset },
            label = { Text(preset) },
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      OutlinedTextField(
        value = sender,
        onValueChange = { sender = it },
        label = { Text("Sender ID (e.g. CBE, MPESA)") },
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("parse_sms_sender_input"),
      )

      Spacer(modifier = Modifier.height(12.dp))

      OutlinedTextField(
        value = smsBody,
        onValueChange = {
          smsBody = it
          parseError = null
        },
        label = { Text("Paste SMS Message Content") },
        minLines = 4,
        maxLines = 7,
        isError = parseError != null,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("parse_sms_body_input"),
      )

      if (parseError != null) {
        Text(
          text = parseError!!,
          style = MaterialTheme.typography.labelMedium,
          color = ExpenseCoral,
          modifier = Modifier.padding(top = 6.dp),
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Text(
            text = "Supported Extraction Rules",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "• CBE: debited / transferred → Expense; credited / received → Income\n• M-Pesa: bought / sent → Expense; received → Income\n• Balance updates automatically when 'Balance is ...' is included",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      Spacer(modifier = Modifier.height(18.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        TextButton(
          onClick = onDismiss,
          modifier = Modifier.weight(1f),
        ) {
          Text("Cancel")
        }
        Button(
          onClick = {
            val ok = onParseAndSave(sender, smsBody)
            if (!ok) {
              parseError = "Could not detect a valid CBE or M-Pesa transaction & amount in this message."
            }
          },
          modifier = Modifier
            .weight(1.6f)
            .testTag("parse_sms_submit_button"),
        ) {
          Text("Extract & Log")
        }
      }
    }
  }
}
