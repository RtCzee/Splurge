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
        BillEntity::class
    ],
    version = 5,
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

    companion object {
        // Volatile keeps the singleton safe when multiple threads ask for it.
        @Volatile
        private var INSTANCE: AppDatabase? = null
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS users (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        fullName TEXT NOT NULL,
                        email TEXT NOT NULL,
                        passwordHash TEXT NOT NULL,
                        passwordSalt TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_users_email ON users(email)"
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `transactions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `amount` REAL NOT NULL,
                        `date` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `categoryId` INTEGER NOT NULL,
                        `type` TEXT NOT NULL,
                        `photoUri` TEXT,
                        FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON DELETE RESTRICT
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_transactions_categoryId` ON `transactions`(`categoryId`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_transactions_date` ON `transactions`(`date`)"
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `bills` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `dueDate` TEXT NOT NULL,
                        `amount` REAL,
                        `recurrence` TEXT NOT NULL,
                        `notes` TEXT,
                        `status` TEXT NOT NULL,
                        `snoozedUntilMillis` INTEGER,
                        `createdAtMillis` INTEGER NOT NULL,
                        `paidAtMillis` INTEGER
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_bills_dueDate` ON `bills`(`dueDate`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_bills_status` ON `bills`(`status`)"
                )
            }
        }

        /** Builds or returns the existing Room database instance. */
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "splurge_room.db"
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
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
