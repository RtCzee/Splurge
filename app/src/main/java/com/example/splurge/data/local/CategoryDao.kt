package com.example.splurge.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * Database operations for expense categories.
 */
@Dao
interface CategoryDao {

    /** Inserts a category and ignores duplicates based on the unique name index. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(category: CategoryEntity): Long

    /** Returns all categories ordered alphabetically for lists and dropdowns. */
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): List<CategoryEntity>

    /** Returns how many categories are available. */
    @Query("SELECT COUNT(*) FROM categories")
    fun getCategoryCount(): Int
}

