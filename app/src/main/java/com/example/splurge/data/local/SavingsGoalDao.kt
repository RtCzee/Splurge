package com.example.splurge.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

/**
 * Database operations for savings goals and their aggregate summaries.
 */
@Dao
interface SavingsGoalDao {

    /** Inserts a new savings goal row. */
    @Insert
    fun insert(goal: SavingsGoalEntity): Long

    /** Returns every savings goal ordered by target date. */
    @Query("SELECT * FROM savings_goals ORDER BY targetDate ASC, id DESC")
    fun getAllGoals(): List<SavingsGoalEntity>

    /** Returns the total current progress across all savings goals. */
    @Query("SELECT COALESCE(SUM(currentAmount), 0) FROM savings_goals")
    fun getTotalSaved(): Double

    /** Returns the sum of all savings targets. */
    @Query("SELECT COALESCE(SUM(targetAmount), 0) FROM savings_goals")
    fun getTotalTarget(): Double
}

