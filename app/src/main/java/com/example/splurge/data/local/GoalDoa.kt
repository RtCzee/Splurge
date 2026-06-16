package com.example.splurge.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals WHERE userId = :userId AND status = 'IN_PROGRESS' AND deadline >= :currentDate ORDER BY deadline ASC")
    fun getActiveGoals(userId: Long, currentDate: Date): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE userId = :userId AND status != 'IN_PROGRESS' ORDER BY deadline DESC")
    suspend fun getCompletedAndMissedGoals(userId: Long): List<GoalEntity>

    @Query("SELECT * FROM goals WHERE userId = :userId AND deadline < :currentDate AND status = 'IN_PROGRESS'")
    suspend fun getExpiredGoals(userId: Long, currentDate: Date): List<GoalEntity>

    @Query("SELECT * FROM goals WHERE id = :goalId AND userId = :userId")
    suspend fun getGoalById(goalId: Long, userId: Long): GoalEntity?

    @Insert
    suspend fun insertGoal(goal: GoalEntity): Long

    @Update
    suspend fun updateGoal(goal: GoalEntity)

    @Delete
    suspend fun deleteGoal(goal: GoalEntity)

    @Query("UPDATE goals SET currentAmount = currentAmount + :amount, lastUpdated = :updatedDate WHERE id = :goalId")
    suspend fun addProgress(goalId: Long, amount: Double, updatedDate: Date)

    @Query("UPDATE goals SET status = :status WHERE id = :goalId")
    suspend fun updateGoalStatus(goalId: Long, status: String)
}