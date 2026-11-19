package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TradeRecordDao {
    @Query("SELECT * FROM trade_records ORDER BY closedAtMillis DESC")
    fun observeAllTrades(): Flow<List<TradeRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrade(record: TradeRecordEntity)

    @Query("DELETE FROM trade_records")
    suspend fun clearAllTrades()
}

@Dao
interface StrategyPresetDao {
    @Query("SELECT * FROM strategy_presets ORDER BY isDefault DESC, createdAtMillis DESC")
    fun observeAllPresets(): Flow<List<StrategyPresetEntity>>

    @Query("SELECT COUNT(*) FROM strategy_presets")
    suspend fun countPresets(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: StrategyPresetEntity): Long

    @Query("DELETE FROM strategy_presets WHERE id = :presetId")
    suspend fun deletePresetById(presetId: Long)
}

@Dao
interface AccountStateDao {
    @Query("SELECT * FROM account_state WHERE id = 1")
    fun observeAccountState(): Flow<AccountStateEntity?>

    @Query("SELECT * FROM account_state WHERE id = 1")
    suspend fun getAccountStateOnce(): AccountStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAccountState(state: AccountStateEntity)
}
