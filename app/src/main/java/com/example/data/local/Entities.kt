package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trade_records")
data class TradeRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val assetSymbol: String,
    val direction: String,
    val entryPrice: Double,
    val closePrice: Double,
    val stake: Double,
    val payoutPercent: Int,
    val profitLoss: Double,
    val outcome: String,
    val isBotTrade: Boolean,
    val strategyName: String,
    val rsiAtEntry: Double,
    val stepIndex: Int,
    val openedAtMillis: Long,
    val closedAtMillis: Long
)

@Entity(tableName = "strategy_presets")
data class StrategyPresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val strategyTypeName: String,
    val baseStake: Double,
    val expirySeconds: Int,
    val moneyModeName: String,
    val martingaleMultiplier: Double,
    val maxSteps: Int,
    val dailyTakeProfit: Double,
    val dailyStopLoss: Double,
    val maxConsecutiveLosses: Int,
    val minConfidence: Int,
    val isDefault: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "account_state")
data class AccountStateEntity(
    @PrimaryKey val id: Int = 1,
    val practiceBalance: Double = 10_000.0,
    val initialDeposit: Double = 10_000.0,
    val activePresetId: Long? = null,
    val updatedAtMillis: Long = System.currentTimeMillis()
)
