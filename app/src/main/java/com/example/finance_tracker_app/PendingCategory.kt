package com.example.finance_tracker_app

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_categories")
data class PendingCategory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val categoryJson: String
)