package com.example.finance_tracker_app.data.db

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
