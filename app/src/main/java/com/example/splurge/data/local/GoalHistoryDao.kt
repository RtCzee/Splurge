package com.example.splurge.data.local

import androidx.room.*
import java.util.Date

@Dao
interface GoalHistoryDao {
    @Insert
    suspend fun insertHistory(history: GoalHistoryEntity): Long

    @Query("SELECT * FROM goal_history WHERE userId = :userId ORDER BY completedDate DESC")
    suspend fun getUserHistory(userId: Long): List<GoalHistoryEntity>

    @Query("SELECT * FROM goal_history WHERE userId = :userId AND completedDate BETWEEN :startDate AND :endDate")
    suspend fun getHistoryForMonth(userId: Long, startDate: Date, endDate: Date): List<GoalHistoryEntity>

    @Query("SELECT COUNT(*) FROM goal_history WHERE userId = :userId AND resultStatus = 'COMPLETED' AND completedDate BETWEEN :startDate AND :endDate")
    suspend fun getCompletedCountForMonth(userId: Long, startDate: Date, endDate: Date): Int

    @Query("""
        SELECT category, COUNT(*) as count 
        FROM goal_history 
        WHERE userId = :userId AND resultStatus = 'COMPLETED' 
        GROUP BY category 
        ORDER BY count DESC 
        LIMIT 1
    """)
    suspend fun getBestPerformingCategory(userId: Long): CategoryStats?

    @Query("DELETE FROM goal_history WHERE userId = :userId")
    suspend fun clearUserHistory(userId: Long)
}

data class CategoryStats(
    val category: String,
    val count: Int
)