package com.example.finance_tracker_app

import androidx.room.*

@Dao
interface PendingCategoryDao {
    @Insert
    suspend fun insert(category: PendingCategory)

    @Query("SELECT * FROM pending_categories")
    suspend fun getAll(): List<PendingCategory>

    @Delete
    suspend fun delete(category: PendingCategory)
}