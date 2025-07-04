package com.example.finance_tracker_app.data.db

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context

@Database(entities = [Operation::class, PendingOperation::class, PendingCard::class, PendingCategory::class], version = 5)
abstract class AppDatabase : RoomDatabase() {
    abstract fun operationDao(): OperationDao
    abstract fun pendingOperationDao(): PendingOperationDao
    abstract fun pendingCardDao(): PendingCardDao
    abstract fun pendingCategoryDao(): PendingCategoryDao


    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                                context.applicationContext,
                                AppDatabase::class.java,
                                "finance_tracker_database"
                            ).fallbackToDestructiveMigration(true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
