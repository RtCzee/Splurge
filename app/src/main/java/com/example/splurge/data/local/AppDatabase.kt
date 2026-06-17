package com.example.splurge.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room database backing all persistent finance data in the app.
 */
@Database(
    entities = [
        CategoryEntity::class,
        ExpenseEntity::class,
        BudgetGoalEntity::class,
        SavingsGoalEntity::class,
        TransactionEntity::class,
        UserEntity::class,
        BillEntity::class,
        GoalEntity::class,
        GoalHistoryEntity::class
    ],
    version = 9,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    /** DAO for category CRUD and lookups. */
    abstract fun categoryDao(): CategoryDao

    /** DAO for expense inserts and spending reports. */
    abstract fun expenseDao(): ExpenseDao

    /** DAO for monthly budget targets. */
    abstract fun budgetGoalDao(): BudgetGoalDao

    /** DAO for savings goal tracking. */
    abstract fun savingsGoalDao(): SavingsGoalDao

    /** DAO for unified transactions. */
    abstract fun transactionDao(): TransactionDao

    /** DAO for bills and bill history. */
    abstract fun billDao(): BillDao

    /** DAO for local account registration and sign-in. */
    abstract fun userDao(): UserDao

    /** DAO for monthly financial goals. */
    abstract fun goalDao(): GoalDao

    /** DAO for completed/missed goal history. */
    abstract fun goalHistoryDao(): GoalHistoryDao

    companion object {
        // Volatile keeps the singleton safe when multiple threads ask for it.
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /** Builds or returns the existing Room database instance. */
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "splurge_room.db"
                )
                    // This project currently recreates the database if the schema changes.
                    .fallbackToDestructiveMigration()
                    // Queries are allowed on the main thread to keep the sample app simple.
                    .allowMainThreadQueries()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
