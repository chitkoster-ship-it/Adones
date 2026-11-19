package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        TradeRecordEntity::class,
        StrategyPresetEntity::class,
        AccountStateEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tradeRecordDao(): TradeRecordDao
    abstract fun strategyPresetDao(): StrategyPresetDao
    abstract fun accountStateDao(): AccountStateDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "iq_trading_bot.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
