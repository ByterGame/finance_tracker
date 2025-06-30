package com.example.finance_tracker_app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "operations")
data class Operation(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String,
    val amount: Double,
    val accountName: String,
    val currency: String,
    val category: String,
    val date: Long,
    val note: String?
)