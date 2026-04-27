package com.example.splurge.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(category: CategoryEntity): Long

    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): List<CategoryEntity>

    @Query("SELECT COUNT(*) FROM categories")
    fun getCategoryCount(): Int
}

