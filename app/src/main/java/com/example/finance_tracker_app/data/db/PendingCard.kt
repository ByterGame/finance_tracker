package com.example.finance_tracker_app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_cards")
data class PendingCard(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val cardJson: String
)