package com.example.splurge.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface BudgetGoalDao {

    @Upsert
    fun upsert(goal: BudgetGoalEntity)

    @Query("SELECT * FROM budget_goals WHERE monthKey = :monthKey LIMIT 1")
    fun getGoalForMonth(monthKey: String): BudgetGoalEntity?
}

