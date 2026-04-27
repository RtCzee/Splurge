package com.example.splurge.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budget_goals")
data class BudgetGoalEntity(
    @PrimaryKey val monthKey: String,
    val minimumGoal: Double,
    val maximumGoal: Double
)

