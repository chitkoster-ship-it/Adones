package com.example.data.repository

import com.example.data.local.AccountStateDao
import com.example.data.local.AccountStateEntity
import com.example.data.local.StrategyPresetDao
import com.example.data.local.StrategyPresetEntity
import com.example.data.local.TradeRecordDao
import com.example.data.local.TradeRecordEntity
import com.example.data.market.MarketFeedClient
import com.example.domain.model.BotConfig
import com.example.domain.model.Candle
import com.example.domain.model.FeedSource
import com.example.domain.model.MarketAsset
import com.example.domain.model.MoneyManagementMode
import com.example.domain.model.StrategyType
import kotlinx.coroutines.flow.Flow

class TradingRepository(
    private val tradeRecordDao: TradeRecordDao,
    private val strategyPresetDao: StrategyPresetDao,
    private val accountStateDao: AccountStateDao,
    private val marketFeedClient: MarketFeedClient
) {
    val tradesFlow: Flow<List<TradeRecordEntity>> = tradeRecordDao.observeAllTrades()
    val presetsFlow: Flow<List<StrategyPresetEntity>> = strategyPresetDao.observeAllPresets()
    val accountStateFlow: Flow<AccountStateEntity?> = accountStateDao.observeAccountState()

    suspend fun ensureInitialized() {
        val currentAccount = accountStateDao.getAccountStateOnce()
        if (currentAccount == null) {
            accountStateDao.upsertAccountState(AccountStateEntity())
        }
        if (strategyPresetDao.countPresets() == 0) {
            val defaults = listOf(
                StrategyPresetEntity(
                    name = "Conservative RSI Scalper",
                    strategyTypeName = StrategyType.RSI_EMA_CONFLUENCE.name,
                    baseStake = 25.0,
                    expirySeconds = 30,
                    moneyModeName = MoneyManagementMode.FLAT.name,
                    martingaleMultiplier = 2.0,
                    maxSteps = 2,
                    dailyTakeProfit = 350.0,
                    dailyStopLoss = 150.0,
                    maxConsecutiveLosses = 3,
                    minConfidence = 62,
                    isDefault = true
                ),
                StrategyPresetEntity(
                    name = "Bollinger Volatility Fade",
                    strategyTypeName = StrategyType.BOLLINGER_REVERSION.name,
                    baseStake = 30.0,
                    expirySeconds = 45,
                    moneyModeName = MoneyManagementMode.SOROS.name,
                    martingaleMultiplier = 2.1,
                    maxSteps = 3,
                    dailyTakeProfit = 500.0,
                    dailyStopLoss = 250.0,
                    maxConsecutiveLosses = 4,
                    minConfidence = 65,
                    isDefault = true
                ),
                StrategyPresetEntity(
                    name = "Trend Breakout + Martingale",
                    strategyTypeName = StrategyType.MOMENTUM_BREAKOUT.name,
                    baseStake = 20.0,
                    expirySeconds = 30,
                    moneyModeName = MoneyManagementMode.MARTINGALE.name,
                    martingaleMultiplier = 2.15,
                    maxSteps = 3,
                    dailyTakeProfit = 600.0,
                    dailyStopLoss = 300.0,
                    maxConsecutiveLosses = 4,
                    minConfidence = 66,
                    isDefault = true
                )
            )
            defaults.forEach { strategyPresetDao.insertPreset(it) }
        }
    }

    suspend fun recordSettledTrade(record: TradeRecordEntity, updatedBalance: Double) {
        tradeRecordDao.insertTrade(record)
        val existing = accountStateDao.getAccountStateOnce() ?: AccountStateEntity()
        accountStateDao.upsertAccountState(
            existing.copy(
                practiceBalance = updatedBalance,
                updatedAtMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun resetPracticeAccount(balance: Double = 10_000.0) {
        val existing = accountStateDao.getAccountStateOnce() ?: AccountStateEntity()
        accountStateDao.upsertAccountState(
            existing.copy(
                practiceBalance = balance,
                initialDeposit = balance,
                updatedAtMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun updateBalanceOnly(balance: Double) {
        val existing = accountStateDao.getAccountStateOnce() ?: AccountStateEntity()
        accountStateDao.upsertAccountState(
            existing.copy(
                practiceBalance = balance,
                updatedAtMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun clearAllTrades() {
        tradeRecordDao.clearAllTrades()
    }

    suspend fun savePreset(name: String, config: BotConfig): Long {
        val entity = StrategyPresetEntity(
            name = name.trim(),
            strategyTypeName = config.strategyType.name,
            baseStake = config.baseStake,
            expirySeconds = config.expirySeconds,
            moneyModeName = config.moneyMode.name,
            martingaleMultiplier = config.martingaleMultiplier,
            maxSteps = config.maxSteps,
            dailyTakeProfit = config.dailyTakeProfit,
            dailyStopLoss = config.dailyStopLoss,
            maxConsecutiveLosses = config.maxConsecutiveLosses,
            minConfidence = config.minConfidence,
            isDefault = false
        )
        return strategyPresetDao.insertPreset(entity)
    }

    suspend fun deletePreset(presetId: Long) {
        strategyPresetDao.deletePresetById(presetId)
    }

    suspend fun fetchLiveSpotPrice(asset: MarketAsset): Pair<Double, FeedSource> =
        marketFeedClient.fetchLiveSpotPrice(asset)

    fun generateInitialCandles(asset: MarketAsset, anchorPrice: Double): List<Candle> =
        marketFeedClient.generateInitialCandles(asset, anchorPrice)

    fun nextSimulatedTick(currentPrice: Double, asset: MarketAsset, tickCount: Long): Double =
        marketFeedClient.nextSimulatedTickPrice(currentPrice, asset, tickCount)
}
