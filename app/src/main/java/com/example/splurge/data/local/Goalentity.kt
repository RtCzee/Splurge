package com.example.splurge.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,  // Make sure this is Long
    val name: String,
    val targetAmount: Double,
    val currentAmount: Double = 0.0,
    val category: String,
    val notes: String = "",
    val deadline: Date,
    val status: String = "IN_PROGRESS",
    val createdAt: Date = Date(),
    val lastUpdated: Date = Date()
)
