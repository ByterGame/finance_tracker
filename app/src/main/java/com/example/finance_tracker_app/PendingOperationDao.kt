package com.example.finance_tracker_app

import androidx.room.*

@Dao
interface PendingOperationDao {
    @Insert
    suspend fun insert(pendingOperation: PendingOperation)

    @Query("SELECT * FROM pending_operations")
    suspend fun getAll(): List<PendingOperation>

    @Delete
    suspend fun delete(pendingOperation: PendingOperation)
}
