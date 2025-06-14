package com.example.finance_tracker_app

import androidx.room.*

@Dao
interface OperationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOperation(operation: Operation)

    @Query("SELECT * FROM operations ORDER BY date DESC")
    suspend fun getAllOperations(): List<Operation>

    @Delete
    suspend fun deleteOperation(operation: Operation)

    @Update
    suspend fun updateOperation(operation: Operation)
}