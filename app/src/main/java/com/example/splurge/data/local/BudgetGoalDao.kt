package com.example.splurge.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

/**
 * Database operations for the monthly budget goal table.
 */
@Dao
interface BudgetGoalDao {

    /** Inserts a new monthly goal or updates the existing one for the same month key. */
    @Upsert
    fun upsert(goal: BudgetGoalEntity)

    /** Returns the stored goal for a single month, if one has been configured. */
    @Query("SELECT * FROM budget_goals WHERE monthKey = :monthKey LIMIT 1")
    fun getGoalForMonth(monthKey: String): BudgetGoalEntity?
}

