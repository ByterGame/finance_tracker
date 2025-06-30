package com.example.finance_tracker_app

import androidx.room.*

@Dao
interface PendingCardDao {
    @Insert
    suspend fun insert(card: PendingCard)

    @Query("SELECT * FROM pending_cards")
    suspend fun getAll(): List<PendingCard>

    @Delete
    suspend fun delete(card: PendingCard)
}