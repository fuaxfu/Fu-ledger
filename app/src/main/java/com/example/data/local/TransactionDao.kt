package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AccountBalanceEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionSource
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

  @Query("SELECT * FROM transactions ORDER BY timestampMillis DESC")
  fun observeAllTransactions(): Flow<List<TransactionEntity>>

  @Query("SELECT * FROM transactions WHERE timestampMillis >= :sinceMillis ORDER BY timestampMillis DESC")
  fun observeTransactionsSince(sinceMillis: Long): Flow<List<TransactionEntity>>

  @Query("SELECT * FROM account_balances ORDER BY source ASC")
  fun observeBalances(): Flow<List<AccountBalanceEntity>>

  @Query("SELECT * FROM account_balances WHERE source = :source LIMIT 1")
  suspend fun getBalanceForSource(source: TransactionSource): AccountBalanceEntity?

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insertTransactionIgnoreDuplicate(transaction: TransactionEntity): Long

  @Update
  suspend fun updateTransaction(transaction: TransactionEntity)

  @Query("DELETE FROM transactions WHERE id = :id")
  suspend fun deleteTransactionById(id: Long)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertBalance(balance: AccountBalanceEntity)

  @Query("DELETE FROM transactions WHERE timestampMillis < :cutoffMillis")
  suspend fun deleteOlderThan(cutoffMillis: Long): Int

  @Query("DELETE FROM transactions")
  suspend fun deleteAllTransactions()

  @Query("DELETE FROM account_balances")
  suspend fun deleteAllBalances()

  @Query("SELECT COUNT(*) FROM transactions")
  fun observeTransactionCount(): Flow<Int>

  @Query("SELECT MIN(timestampMillis) FROM transactions")
  fun observeOldestTimestamp(): Flow<Long?>
}
