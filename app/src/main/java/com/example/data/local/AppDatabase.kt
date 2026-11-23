package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.data.model.AccountBalanceEntity
import com.example.data.model.ExpenseCategory
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionSource
import com.example.data.model.TransactionType

class LedgerTypeConverters {
  @TypeConverter
  fun fromSource(value: TransactionSource): String = value.name

  @TypeConverter
  fun toSource(value: String): TransactionSource =
    TransactionSource.entries.firstOrNull { it.name == value } ?: TransactionSource.MANUAL

  @TypeConverter
  fun fromType(value: TransactionType): String = value.name

  @TypeConverter
  fun toType(value: String): TransactionType =
    TransactionType.entries.firstOrNull { it.name == value } ?: TransactionType.EXPENSE

  @TypeConverter
  fun fromCategory(value: ExpenseCategory): String = value.name

  @TypeConverter
  fun toCategory(value: String): ExpenseCategory =
    ExpenseCategory.entries.firstOrNull { it.name == value } ?: ExpenseCategory.OTHER_EXPENSE
}

@Database(
  entities = [TransactionEntity::class, AccountBalanceEntity::class],
  version = 1,
  exportSchema = false,
)
@TypeConverters(LedgerTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
  abstract fun transactionDao(): TransactionDao

  companion object {
    @Volatile
    private var instance: AppDatabase? = null

    fun getInstance(context: Context): AppDatabase {
      return instance ?: synchronized(this) {
        instance ?: Room.databaseBuilder(
          context.applicationContext,
          AppDatabase::class.java,
          "birr_ledger_local.db",
        ).build().also { instance = it }
      }
    }
  }
}
