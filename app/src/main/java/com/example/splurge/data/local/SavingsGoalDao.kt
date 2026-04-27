package com.example.splurge.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface SavingsGoalDao {

    @Insert
    fun insert(goal: SavingsGoalEntity): Long

    @Query("SELECT * FROM savings_goals ORDER BY targetDate ASC, id DESC")
    fun getAllGoals(): List<SavingsGoalEntity>

    @Query("SELECT COALESCE(SUM(currentAmount), 0) FROM savings_goals")
    fun getTotalSaved(): Double

    @Query("SELECT COALESCE(SUM(targetAmount), 0) FROM savings_goals")
    fun getTotalTarget(): Double
}

