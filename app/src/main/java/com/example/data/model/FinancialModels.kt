package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class TransactionSource(val displayName: String, val shortBadge: String) {
  CBE("Commercial Bank of Ethiopia", "CBE"),
  MPESA("M-Pesa", "M-PESA"),
  MANUAL("Manual Entry", "Manual"),
}

enum class TransactionType(val label: String) {
  EXPENSE("Expense"),
  INCOME("Income"),
}

enum class ExpenseCategory(
  val displayName: String,
  val isExpenseDefault: Boolean,
) {
  TRANSFER("Transfers", true),
  BILLS_AIRTIME("Bills & Airtime", true),
  MERCHANT_SHOPPING("Shopping & Merchants", true),
  FOOD_DINING("Food & Dining", true),
  TRANSPORT("Transport & Fuel", true),
  CASH_WITHDRAWAL("Cash & ATM", true),
  FEES_CHARGES("Bank & Service Fees", true),
  OTHER_EXPENSE("Other Expenses", true),
  SALARY_INCOME("Salary & Payroll", false),
  RECEIVED_TRANSFER("Received Transfers", false),
  DEPOSIT_CREDIT("Credits & Deposits", false),
  REFUND("Refunds & Reversals", false),
  OTHER_INCOME("Other Income", false);

  companion object {
    fun expenseCategories(): List<ExpenseCategory> = entries.filter { it.isExpenseDefault }
    fun incomeCategories(): List<ExpenseCategory> = entries.filter { !it.isExpenseDefault }
  }
}

@Entity(
  tableName = "transactions",
  indices = [
    Index(value = ["smsSignature"], unique = true),
    Index(value = ["timestampMillis"]),
  ],
)
data class TransactionEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  val smsSignature: String,
  val source: TransactionSource,
  val type: TransactionType,
  val amount: Double,
  val currency: String = "ETB",
  val category: ExpenseCategory,
  val counterparty: String,
  val referenceCode: String? = null,
  val balanceAfter: Double? = null,
  val timestampMillis: Long,
  val note: String? = null,
  val rawSmsSnippet: String? = null,
  val isAutoLogged: Boolean = true,
)

@Entity(tableName = "account_balances")
data class AccountBalanceEntity(
  @PrimaryKey val source: TransactionSource,
  val balance: Double,
  val currency: String = "ETB",
  val accountLabel: String,
  val lastSyncedMillis: Long,
  val lastReferenceCode: String? = null,
)
