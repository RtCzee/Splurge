package com.example.splurge.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "goal_history",
    foreignKeys = [
        ForeignKey(
            entity = GoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goalId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class GoalHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val goalId: Long,
    val userId: Long,
    val name: String,
    val targetAmount: Double,
    val finalAmount: Double,
    val resultStatus: String,
    val completedDate: Date,
    val category: String,
    val notes: String = ""
)