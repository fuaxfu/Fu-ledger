package com.example.sms

import com.example.data.local.UserPreferencesRepository
import com.example.data.model.ExpenseCategory
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionSource
import com.example.data.model.TransactionType
import java.security.MessageDigest
import java.util.Locale

data class ParsedSmsResult(
  val transaction: TransactionEntity,
  val accountLabel: String,
)

object BankSmsParser {

  private val balanceRegex = Regex(
    pattern = """(?:current|available|new\s*m-?pesa|m-?pesa|new)?\s*balance\s*(?:is|:)?\s*(ETB|Birr|Br|KES|Ksh|USD)?\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)""",
    options = setOf(RegexOption.IGNORE_CASE),
  )

  private val currencyBeforeAmountRegex = Regex(
    pattern = """\b(ETB|Birr|Br|KES|Ksh|USD)\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)""",
    options = setOf(RegexOption.IGNORE_CASE),
  )

  private val amountBeforeCurrencyRegex = Regex(
    pattern = """([0-9][0-9,]*(?:\.[0-9]{1,2})?)\s*(ETB|Birr|Br|KES|Ksh|USD)\b""",
    options = setOf(RegexOption.IGNORE_CASE),
  )

  private val accountMaskRegex = Regex(
    pattern = """(?:Account|Acc(?:ount)?\.?)\s*(?:No\.?|Number)?\s*([0-9]\*+[0-9]{2,4})""",
    options = setOf(RegexOption.IGNORE_CASE),
  )

  private val referenceRegex = Regex(
    pattern = """\b(?:Ref(?:erence)?[:#\s]*([A-Z0-9]{6,18})|(FT[A-Z0-9]{6,16})|([A-Z0-9]{10})\s+Confirmed)\b""",
    options = setOf(RegexOption.IGNORE_CASE),
  )

  private val counterpartyToFromRegex = Regex(
    pattern = """\b(?:to|from|at)\s+([A-Za-z][A-Za-z0-9 .'\-&]{1,36}?)(?=\s+(?:on|at|with|for|Account|Acc|Your|New|Ref|via|07\d|09\d|\+251|\d{2}/|\.\s|$)|[.,;]|$)""",
    options = setOf(RegexOption.IGNORE_CASE),
  )

  private val cbeExpensePattern = Regex(
    pattern = """\b(debited|debit|transfer+ed|paid|withdrawn|withdrawal|deducted|purchase|payment)\b""",
    options = setOf(RegexOption.IGNORE_CASE),
  )

  private val cbeIncomePattern = Regex(
    pattern = """\b(credited|credit|received?|deposited|deposit)\b""",
    options = setOf(RegexOption.IGNORE_CASE),
  )

  private val mpesaExpensePattern = Regex(
    pattern = """\b(bought|sent|paid|pay|withdraw|withdrew|purchase|airtime|bundle|paybill|till)\b""",
    options = setOf(RegexOption.IGNORE_CASE),
  )

  private val mpesaIncomePattern = Regex(
    pattern = """\b(received?|credited|deposited|deposit)\b""",
    options = setOf(RegexOption.IGNORE_CASE),
  )

  fun detectSource(
    sender: String,
    body: String,
    cbeKeywords: Set<String> = UserPreferencesRepository.DEFAULT_CBE_SENDERS,
    mpesaKeywords: Set<String> = UserPreferencesRepository.DEFAULT_MPESA_SENDERS,
    allowBodyFallback: Boolean = false,
  ): TransactionSource? {
    val normSender = sender.trim().uppercase(Locale.ROOT)
    val normBody = body.uppercase(Locale.ROOT)

    if (mpesaKeywords.any { kw -> kw.isNotBlank() && normSender.contains(kw.uppercase(Locale.ROOT)) }) {
      return TransactionSource.MPESA
    }
    if (cbeKeywords.any { kw -> kw.isNotBlank() && normSender.contains(kw.uppercase(Locale.ROOT)) }) {
      return TransactionSource.CBE
    }

    if (allowBodyFallback) {
      if (normBody.contains("M-PESA") || normBody.contains("MPESA")) {
        return TransactionSource.MPESA
      }
      if (normBody.contains("CBE") ||
        normBody.contains("COMMERCIAL BANK") ||
        (accountMaskRegex.containsMatchIn(body) &&
          (normBody.contains("CREDITED") || normBody.contains("DEBITED") || normBody.contains("TRANSFER")))
      ) {
        return TransactionSource.CBE
      }
    }

    return null
  }

  fun parseSms(
    sender: String,
    body: String,
    timestampMillis: Long,
    cbeKeywords: Set<String> = UserPreferencesRepository.DEFAULT_CBE_SENDERS,
    mpesaKeywords: Set<String> = UserPreferencesRepository.DEFAULT_MPESA_SENDERS,
    allowBodyFallback: Boolean = false,
  ): ParsedSmsResult? {
    if (body.isBlank()) return null
    val source = detectSource(sender, body, cbeKeywords, mpesaKeywords, allowBodyFallback) ?: return null

    val type = when (source) {
      TransactionSource.CBE -> classifyCbeType(body)
      TransactionSource.MPESA -> classifyMpesaType(body)
      TransactionSource.MANUAL -> return null
    } ?: return null

    // Remove balance and fee clauses when extracting the primary transaction amount
    val primarySegment = body
      .replace(balanceRegex, " ")
      .replace(Regex("""transaction\s*cost[^.]*""", RegexOption.IGNORE_CASE), " ")

    val (amount, rawCurrency) = extractAmountAndCurrency(primarySegment)
      ?: extractAmountAndCurrency(body)
      ?: return null

    if (amount <= 0.0) return null

    val balanceMatch = balanceRegex.find(body)
    val balanceAfter = balanceMatch?.groupValues?.getOrNull(2)
      ?.replace(",", "")
      ?.toDoubleOrNull()

    val currency = normalizeCurrency(
      rawCurrency ?: balanceMatch?.groupValues?.getOrNull(1),
    )

    val referenceCode = extractReference(body)
    val accountMask = accountMaskRegex.find(body)?.groupValues?.getOrNull(1)
    val counterparty = extractCounterparty(body, source, type)
    val category = autoCategorize(source, type, body, counterparty)

    val accountLabel = when (source) {
      TransactionSource.CBE -> accountMask ?: "CBE Account"
      TransactionSource.MPESA -> "M-Pesa Wallet"
      TransactionSource.MANUAL -> "Manual"
    }

    val signature = buildSignature(source, referenceCode, amount, timestampMillis, body)

    val entity = TransactionEntity(
      smsSignature = signature,
      source = source,
      type = type,
      amount = amount,
      currency = currency,
      category = category,
      counterparty = counterparty,
      referenceCode = referenceCode,
      balanceAfter = balanceAfter,
      timestampMillis = timestampMillis,
      note = null,
      rawSmsSnippet = body.trim().take(280),
      isAutoLogged = true,
    )

    return ParsedSmsResult(transaction = entity, accountLabel = accountLabel)
  }

  private fun classifyCbeType(body: String): TransactionType? {
    val hasExpense = cbeExpensePattern.containsMatchIn(body)
    val hasIncome = cbeIncomePattern.containsMatchIn(body)
    return when {
      hasExpense && !hasIncome -> TransactionType.EXPENSE
      hasIncome && !hasExpense -> TransactionType.INCOME
      hasExpense && hasIncome -> {
        val expenseIdx = cbeExpensePattern.find(body)?.range?.first ?: Int.MAX_VALUE
        val incomeIdx = cbeIncomePattern.find(body)?.range?.first ?: Int.MAX_VALUE
        if (expenseIdx <= incomeIdx) TransactionType.EXPENSE else TransactionType.INCOME
      }
      else -> null
    }
  }

  private fun classifyMpesaType(body: String): TransactionType? {
    val hasExpense = mpesaExpensePattern.containsMatchIn(body)
    val hasIncome = mpesaIncomePattern.containsMatchIn(body)
    return when {
      hasExpense && !hasIncome -> TransactionType.EXPENSE
      hasIncome && !hasExpense -> TransactionType.INCOME
      hasExpense && hasIncome -> {
        val expenseIdx = mpesaExpensePattern.find(body)?.range?.first ?: Int.MAX_VALUE
        val incomeIdx = mpesaIncomePattern.find(body)?.range?.first ?: Int.MAX_VALUE
        if (expenseIdx <= incomeIdx) TransactionType.EXPENSE else TransactionType.INCOME
      }
      else -> null
    }
  }

  private fun extractAmountAndCurrency(text: String): Pair<Double, String?>? {
    currencyBeforeAmountRegex.find(text)?.let { m ->
      val curr = m.groupValues[1]
      val amt = m.groupValues[2].replace(",", "").toDoubleOrNull()
      if (amt != null) return amt to curr
    }
    amountBeforeCurrencyRegex.find(text)?.let { m ->
      val amt = m.groupValues[1].replace(",", "").toDoubleOrNull()
      val curr = m.groupValues[2]
      if (amt != null) return amt to curr
    }
    return null
  }

  private fun normalizeCurrency(raw: String?): String {
    return when (raw?.trim()?.uppercase(Locale.ROOT)) {
      "KSH", "KES" -> "KES"
      "USD" -> "USD"
      "ETB", "BIRR", "BR", null, "" -> "ETB"
      else -> "ETB"
    }
  }

  private fun extractReference(body: String): String? {
    val match = referenceRegex.find(body) ?: return null
    return listOf(1, 2, 3)
      .mapNotNull { idx -> match.groupValues.getOrNull(idx)?.takeIf { it.isNotBlank() } }
      .firstOrNull()
      ?.uppercase(Locale.ROOT)
  }

  private fun extractCounterparty(
    body: String,
    source: TransactionSource,
    type: TransactionType,
  ): String {
    val lower = body.lowercase(Locale.ROOT)
    if (lower.contains("airtime") || lower.contains("bundle")) {
      return if (source == TransactionSource.MPESA) "Airtime & Data" else "Telecom Airtime"
    }
    if (lower.contains("atm")) {
      return "ATM Withdrawal"
    }

    val match = counterpartyToFromRegex.find(body)?.groupValues?.getOrNull(1)?.trim()
    if (!match.isNullOrBlank() &&
      !match.equals("your account", ignoreCase = true) &&
      !match.equals("Banking", ignoreCase = true)
    ) {
      return match.replace(Regex("""\s+"""), " ")
    }

    return when (source) {
      TransactionSource.CBE -> if (type == TransactionType.INCOME) "CBE Credit" else "CBE Debit"
      TransactionSource.MPESA -> if (type == TransactionType.INCOME) "M-Pesa Transfer In" else "M-Pesa Payment"
      TransactionSource.MANUAL -> "Manual Entry"
    }
  }

  fun autoCategorize(
    source: TransactionSource,
    type: TransactionType,
    body: String,
    counterparty: String,
  ): ExpenseCategory {
    val text = "$body $counterparty".lowercase(Locale.ROOT)

    if (type == TransactionType.INCOME) {
      return when {
        text.containsAny("salary", "payroll", "wage", "stipend", "bonus") -> ExpenseCategory.SALARY_INCOME
        text.containsAny("refund", "reversal", "reversed", "cashback") -> ExpenseCategory.REFUND
        text.containsAny("received", "receive", "from ") -> ExpenseCategory.RECEIVED_TRANSFER
        text.containsAny("credited", "credit", "deposit") -> ExpenseCategory.DEPOSIT_CREDIT
        else -> ExpenseCategory.OTHER_INCOME
      }
    }

    return when {
      text.containsAny("airtime", "bundle", "ethio telecom", "ethiotelecom", "safaricom", "electric", "eep", "water", "utility", "bill", "dstv", "wifi", "internet") ->
        ExpenseCategory.BILLS_AIRTIME
      text.containsAny("atm", "withdraw", "withdrew", "cash out", "agent") ->
        ExpenseCategory.CASH_WITHDRAWAL
      text.containsAny("cafe", "restaurant", "coffee", "hotel", "food", "pizza", "burger", "bakery", "bistro") ->
        ExpenseCategory.FOOD_DINING
      text.containsAny("ride", "feres", "yango", "uber", "taxi", "fuel", "gas", "total", "noc", "transport", "bus", "airline") ->
        ExpenseCategory.TRANSPORT
      text.containsAny("supermarket", "shop", "mart", "store", "pos", "merchant", "mall", "pharmacy", "bought goods", "till", "paybill", "bought") ->
        ExpenseCategory.MERCHANT_SHOPPING
      text.containsAny("fee", "charge", "commission", "vat", "service charge") ->
        ExpenseCategory.FEES_CHARGES
      text.containsAny("transfer", "transfered", "sent", "debited") ->
        if (source == TransactionSource.CBE || source == TransactionSource.MPESA) ExpenseCategory.TRANSFER else ExpenseCategory.OTHER_EXPENSE
      else -> ExpenseCategory.OTHER_EXPENSE
    }
  }

  private fun String.containsAny(vararg terms: String): Boolean =
    terms.any { this.contains(it) }

  private fun buildSignature(
    source: TransactionSource,
    referenceCode: String?,
    amount: Double,
    timestampMillis: Long,
    body: String,
  ): String {
    if (!referenceCode.isNullOrBlank()) {
      return "${source.name}_REF_$referenceCode"
    }
    // Round timestamp to minute to prevent duplicate ingestion across slightly differing timestamps
    val minuteBucket = timestampMillis / 60_000L
    val normalizedBody = body.trim().replace(Regex("""\s+"""), " ")
    val raw = "${source.name}|$amount|$minuteBucket|$normalizedBody"
    val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
    return digest.joinToString("") { "%02x".format(it) }.take(32)
  }
}
