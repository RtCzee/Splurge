package com.example.splurge.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Room database backing all persistent finance data in the app.
 */
@Database(
    entities = [
        CategoryEntity::class,
        ExpenseEntity::class,
        BudgetGoalEntity::class,
        SavingsGoalEntity::class,
        TransactionEntity::class
    ],
    version = 3,
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
