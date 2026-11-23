package com.example

import com.example.data.model.ExpenseCategory
import com.example.data.model.TransactionSource
import com.example.data.model.TransactionType
import com.example.sms.BankSmsParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class BankSmsParserTest {

  @Test
  fun parseCbeCreditedAsIncomeWithBalance() {
    val sms = "Dear Dawit your Account 1*********1234 has been Credited with ETB 12,500.00. Your Current Balance is ETB 24,800.50. Ref: FT26123ABC99"
    val parsed = BankSmsParser.parseSms("CBE", sms, 1_700_000_000_000L)

    assertNotNull(parsed)
    val tx = parsed!!.transaction
    assertEquals(TransactionSource.CBE, tx.source)
    assertEquals(TransactionType.INCOME, tx.type)
    assertEquals(12500.0, tx.amount, 0.01)
    assertEquals(24800.50, tx.balanceAfter!!, 0.01)
    assertEquals("ETB", tx.currency)
    assertEquals("FT26123ABC99", tx.referenceCode)
    assertEquals(ExpenseCategory.DEPOSIT_CREDIT, tx.category)
  }

  @Test
  fun parseCbeDebitedAndTransferedAsExpense() {
    val smsDebit = "Dear Dawit your Account 1***1234 has been Debited with ETB 1,850.00. Your Current Balance is ETB 22,950.50."
    val parsedDebit = BankSmsParser.parseSms("CBE", smsDebit, 1_700_000_000_000L)!!
    assertEquals(TransactionType.EXPENSE, parsedDebit.transaction.type)
    assertEquals(1850.0, parsedDebit.transaction.amount, 0.01)
    assertEquals(22950.50, parsedDebit.transaction.balanceAfter!!, 0.01)

    val smsTransfer = "Dear Customer, you have transfered ETB 600.00 to Tigist Alemu. Your Current Balance is ETB 22,350.50."
    val parsedTransfer = BankSmsParser.parseSms("CBE", smsTransfer, 1_700_000_100_000L)!!
    assertEquals(TransactionType.EXPENSE, parsedTransfer.transaction.type)
    assertEquals(600.0, parsedTransfer.transaction.amount, 0.01)
    assertEquals("Tigist Alemu", parsedTransfer.transaction.counterparty)
    assertEquals(ExpenseCategory.TRANSFER, parsedTransfer.transaction.category)
  }

  @Test
  fun parseMpesaBoughtSentAndReceived() {
    val sentSms = "UB74K92MP1 Confirmed. ETB 350.00 sent to Selamawit Tadesse 0712345678 on 23/11/26. New M-PESA balance is ETB 2,650.00."
    val parsedSent = BankSmsParser.parseSms("MPESA", sentSms, 1_700_000_200_000L)!!
    assertEquals(TransactionSource.MPESA, parsedSent.transaction.source)
    assertEquals(TransactionType.EXPENSE, parsedSent.transaction.type)
    assertEquals(350.0, parsedSent.transaction.amount, 0.01)
    assertEquals(2650.0, parsedSent.transaction.balanceAfter!!, 0.01)
    assertEquals("Selamawit Tadesse", parsedSent.transaction.counterparty)
    assertEquals(ExpenseCategory.TRANSFER, parsedSent.transaction.category)

    val boughtSms = "UB74K93MQ2 Confirmed. You bought ETB 100.00 of airtime on 23/11/26. New M-PESA balance is ETB 2,550.00."
    val parsedBought = BankSmsParser.parseSms("M-PESA", boughtSms, 1_700_000_300_000L)!!
    assertEquals(TransactionType.EXPENSE, parsedBought.transaction.type)
    assertEquals(100.0, parsedBought.transaction.amount, 0.01)
    assertEquals(ExpenseCategory.BILLS_AIRTIME, parsedBought.transaction.category)

    val receivedSms = "UB74K95MS3 Confirmed. You have received ETB 4,500.00 from Yosef Bekele 0798765432 on 23/11/26. New M-PESA balance is ETB 5,850.00."
    val parsedReceived = BankSmsParser.parseSms("MPESA", receivedSms, 1_700_000_400_000L)!!
    assertEquals(TransactionType.INCOME, parsedReceived.transaction.type)
    assertEquals(4500.0, parsedReceived.transaction.amount, 0.01)
    assertEquals(5850.0, parsedReceived.transaction.balanceAfter!!, 0.01)
    assertEquals("Yosef Bekele", parsedReceived.transaction.counterparty)
    assertEquals(ExpenseCategory.RECEIVED_TRANSFER, parsedReceived.transaction.category)
  }

  @Test
  fun ignoreNonBankSmsSender() {
    val spam = "You won ETB 50000! Claim now!"
    val parsed = BankSmsParser.parseSms("PROMO", spam, 1_700_000_500_000L, allowBodyFallback = false)
    assertNull(parsed)
  }
}
