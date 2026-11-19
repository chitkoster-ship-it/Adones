package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.StrategyPresetEntity
import com.example.data.local.TradeRecordEntity
import com.example.data.repository.TradingRepository
import com.example.domain.engine.IndicatorCalculator
import com.example.domain.model.BotConfig
import com.example.domain.model.BotLogEntry
import com.example.domain.model.BotStatus
import com.example.domain.model.Candle
import com.example.domain.model.FeedSource
import com.example.domain.model.LogLevel
import com.example.domain.model.MarketAsset
import com.example.domain.model.MoneyManagementMode
import com.example.domain.model.OpenPosition
import com.example.domain.model.StrategyType
import com.example.domain.model.TechnicalSnapshot
import com.example.domain.model.TradeDirection
import com.example.domain.model.TradeOutcome
import com.example.domain.model.TradeSignal
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

data class TerminalUiState(
    val assets: List<MarketAsset> = MarketAsset.defaultAssets,
    val selectedAsset: MarketAsset = MarketAsset.defaultAssets.first(),
    val candles: List<Candle> = emptyList(),
    val currentPrice: Double = MarketAsset.defaultAssets.first().seedPrice,
    val previousClosePrice: Double = MarketAsset.defaultAssets.first().seedPrice,
    val feedSource: FeedSource = FeedSource.SIMULATOR,
    val technicalSnapshot: TechnicalSnapshot = TechnicalSnapshot(),
    val botConfig: BotConfig = BotConfig(),
    val botStatus: BotStatus = BotStatus.IDLE,
    val isBotRunning: Boolean = false,
    val practiceBalance: Double = 10_000.0,
    val sessionPnl: Double = 0.0,
    val currentStepIndex: Int = 0,
    val nextBotStake: Double = 25.0,
    val consecutiveLosses: Int = 0,
    val openPositions: List<OpenPosition> = emptyList(),
    val logs: List<BotLogEntry> = emptyList(),
    val currentTimeMillis: Long = System.currentTimeMillis(),
    val lastBannerMessage: String? = null,
    val activePresetName: String = "Conservative RSI Scalper"
)

class TradingBotViewModel(
    private val repository: TradingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TerminalUiState())
    val uiState: StateFlow<TerminalUiState> = _uiState.asStateFlow()

    val tradeHistory: StateFlow<List<TradeRecordEntity>> = repository.tradesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val strategyPresets: StateFlow<List<StrategyPresetEntity>> = repository.presetsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var tickCounter: Long = 0L
    private var lastBotCloseMillis: Long = 0L
    private var lastLiveFetchMillis: Long = 0L
    private var candlesPerAsset = mutableMapOf<String, MutableList<Candle>>()

    init {
        viewModelScope.launch {
            repository.ensureInitialized()
            val savedAccount = repository.accountStateFlow.first()
            if (savedAccount != null) {
                _uiState.update { it.copy(practiceBalance = savedAccount.practiceBalance) }
            }
            initializeAssetFeed(MarketAsset.defaultAssets.first())
            appendLog(
                LogLevel.INFO,
                "Engine Initialized",
                "Practice Paper Engine ready ($${formatMoney(_uiState.value.practiceBalance)} virtual equity)"
            )
            startMarketLoop()
        }

        // Keep practice balance synchronized with Room updates
        viewModelScope.launch {
            repository.accountStateFlow.collect { state ->
                if (state != null) {
                    _uiState.update { it.copy(practiceBalance = state.practiceBalance) }
                }
            }
        }
    }

    private suspend fun initializeAssetFeed(asset: MarketAsset) {
        val (spot, source) = repository.fetchLiveSpotPrice(asset)
        val initialCandles = candlesPerAsset[asset.symbol]
            ?: repository.generateInitialCandles(asset, spot).toMutableList().also {
                candlesPerAsset[asset.symbol] = it
            }
        val latestClose = initialCandles.lastOrNull()?.close ?: spot
        val openRef = initialCandles.firstOrNull()?.open ?: latestClose
        val snapshot = IndicatorCalculator.evaluate(initialCandles, _uiState.value.botConfig.strategyType)

        _uiState.update {
            it.copy(
                selectedAsset = asset,
                candles = initialCandles.toList(),
                currentPrice = latestClose,
                previousClosePrice = openRef,
                feedSource = source,
                technicalSnapshot = snapshot,
                botConfig = it.botConfig.copy(payoutPercent = asset.payoutPercent)
            )
        }
    }

    private fun startMarketLoop() {
        viewModelScope.launch {
            while (isActive) {
                delay(1_000L)
                tickCounter++
                val now = System.currentTimeMillis()
                val state = _uiState.value
                val asset = state.selectedAsset

                // Periodically refresh real live price if available (every 5s)
                var liveAnchorPrice: Double? = null
                var updatedSource = state.feedSource
                if (now - lastLiveFetchMillis >= 5_000L) {
                    lastLiveFetchMillis = now
                    val (fetched, source) = repository.fetchLiveSpotPrice(asset)
                    updatedSource = source
                    if (source == FeedSource.LIVE_API) {
                        liveAnchorPrice = fetched
                    }
                }

                val currentSeries = candlesPerAsset.getOrPut(asset.symbol) {
                    repository.generateInitialCandles(asset, state.currentPrice).toMutableList()
                }
                val lastCandle = currentSeries.lastOrNull()
                val prevPrice = lastCandle?.close ?: state.currentPrice

                // Blend live anchor gently if live API responded, otherwise calibrated micro-tick
                val nextPrice = if (liveAnchorPrice != null) {
                    val delta = (liveAnchorPrice - prevPrice) * 0.45
                    val micro = repository.nextSimulatedTick(prevPrice, asset, tickCounter) - prevPrice
                    (prevPrice + delta + micro * 0.35).coerceAtLeast(0.0001)
                } else {
                    repository.nextSimulatedTick(prevPrice, asset, tickCounter)
                }

                // Every 5 seconds open a new candle; otherwise update forming candle
                if (tickCounter % 5L == 0L || lastCandle == null) {
                    val newCandle = Candle(
                        timestampMillis = now,
                        open = prevPrice,
                        high = max(prevPrice, nextPrice),
                        low = min(prevPrice, nextPrice),
                        close = nextPrice
                    )
                    currentSeries.add(newCandle)
                    if (currentSeries.size > 80) {
                        currentSeries.removeAt(0)
                    }
                } else {
                    val updated = lastCandle.copy(
                        high = max(lastCandle.high, nextPrice),
                        low = min(lastCandle.low, nextPrice),
                        close = nextPrice
                    )
                    currentSeries[currentSeries.lastIndex] = updated
                }

                val snapshot = IndicatorCalculator.evaluate(currentSeries, state.botConfig.strategyType)
                _uiState.update {
                    it.copy(
                        candles = currentSeries.toList(),
                        currentPrice = nextPrice,
                        feedSource = updatedSource,
                        technicalSnapshot = snapshot,
                        currentTimeMillis = now
                    )
                }

                // Settle any expired positions
                settleExpiredPositions(now)

                // Evaluate automated bot trigger
                if (_uiState.value.isBotRunning) {
                    evaluateBotTick(now)
                }
            }
        }
    }

    private suspend fun settleExpiredPositions(now: Long) {
        val currentState = _uiState.value
        val expired = currentState.openPositions.filter { it.expiresAtMillis <= now }
        if (expired.isEmpty()) return

        val remaining = currentState.openPositions.filter { it.expiresAtMillis > now }
        var balance = currentState.practiceBalance
        var sessionPnl = currentState.sessionPnl
        var stepIndex = currentState.currentStepIndex
        var consecutiveLosses = currentState.consecutiveLosses
        val config = currentState.botConfig

        for (pos in expired) {
            // Use latest price if same asset, else current series last close
            val settlePrice = if (pos.assetSymbol == currentState.selectedAsset.symbol) {
                currentState.currentPrice
            } else {
                candlesPerAsset[pos.assetSymbol]?.lastOrNull()?.close ?: pos.entryPrice
            }

            val outcome = when {
                settlePrice == pos.entryPrice -> TradeOutcome.TIE
                pos.direction == TradeDirection.CALL && settlePrice > pos.entryPrice -> TradeOutcome.WIN
                pos.direction == TradeDirection.PUT && settlePrice < pos.entryPrice -> TradeOutcome.WIN
                else -> TradeOutcome.LOSS
            }

            val pnl = when (outcome) {
                TradeOutcome.WIN -> pos.stake * (pos.payoutPercent / 100.0)
                TradeOutcome.TIE -> 0.0
                TradeOutcome.LOSS -> -pos.stake
            }

            // Stake was deducted on open; return stake + pnl (or stake on tie, 0 on loss)
            val returnedCapital = when (outcome) {
                TradeOutcome.WIN -> pos.stake + pnl
                TradeOutcome.TIE -> pos.stake
                TradeOutcome.LOSS -> 0.0
            }
            balance += returnedCapital
            sessionPnl += pnl

            if (pos.isBotTrade) {
                lastBotCloseMillis = now
                when (outcome) {
                    TradeOutcome.WIN -> {
                        consecutiveLosses = 0
                        stepIndex = when (config.moneyMode) {
                            MoneyManagementMode.SOROS -> if (stepIndex + 1 <= config.maxSteps) stepIndex + 1 else 0
                            else -> 0
                        }
                    }
                    TradeOutcome.LOSS -> {
                        consecutiveLosses++
                        stepIndex = when (config.moneyMode) {
                            MoneyManagementMode.MARTINGALE -> if (stepIndex + 1 <= config.maxSteps) stepIndex + 1 else 0
                            MoneyManagementMode.SOROS -> 0
                            MoneyManagementMode.FLAT -> 0
                        }
                    }
                    TradeOutcome.TIE -> Unit
                }
            }

            val record = TradeRecordEntity(
                assetSymbol = pos.assetSymbol,
                direction = pos.direction.name,
                entryPrice = pos.entryPrice,
                closePrice = settlePrice,
                stake = pos.stake,
                payoutPercent = pos.payoutPercent,
                profitLoss = pnl,
                outcome = outcome.name,
                isBotTrade = pos.isBotTrade,
                strategyName = pos.strategyLabel,
                rsiAtEntry = pos.rsiAtEntry,
                stepIndex = pos.stepIndex,
                openedAtMillis = pos.openedAtMillis,
                closedAtMillis = now
            )
            repository.recordSettledTrade(record, balance)

            val sign = if (pnl >= 0) "+" else ""
            appendLog(
                LogLevel.TRADE,
                "${outcome.name}: ${pos.assetSymbol} ${pos.direction.name}",
                "PnL: $sign$${formatMoney(pnl)} | Entry ${formatPrice(pos.entryPrice, pos.assetSymbol)} -> ${formatPrice(settlePrice, pos.assetSymbol)}"
            )
        }

        val nextStake = computeNextStake(config, stepIndex)
        _uiState.update {
            it.copy(
                openPositions = remaining,
                practiceBalance = balance,
                sessionPnl = sessionPnl,
                currentStepIndex = stepIndex,
                nextBotStake = nextStake,
                consecutiveLosses = consecutiveLosses
            )
        }
    }

    private suspend fun evaluateBotTick(now: Long) {
        val state = _uiState.value
        val config = state.botConfig

        // Check risk guardrails
        if (state.sessionPnl >= config.dailyTakeProfit) {
            haltBot(BotStatus.HALTED_TP, "Daily Take Profit ($${formatMoney(config.dailyTakeProfit)}) reached")
            return
        }
        if (state.sessionPnl <= -config.dailyStopLoss) {
            haltBot(BotStatus.HALTED_SL, "Daily Stop Loss ($${formatMoney(config.dailyStopLoss)}) hit")
            return
        }
        if (state.consecutiveLosses >= config.maxConsecutiveLosses) {
            haltBot(
                BotStatus.HALTED_LOSS_STREAK,
                "Halted after ${state.consecutiveLosses} consecutive losses"
            )
            return
        }

        // Wait if a bot trade is already active
        if (state.openPositions.any { it.isBotTrade }) {
            _uiState.update { it.copy(botStatus = BotStatus.IN_POSITION) }
            return
        }

        // 4-second cooldown between bot trades
        if (now - lastBotCloseMillis < 4_000L) {
            _uiState.update { it.copy(botStatus = BotStatus.COOLDOWN) }
            return
        }

        _uiState.update { it.copy(botStatus = BotStatus.SCANNING) }

        val snap = state.technicalSnapshot
        val direction = when {
            snap.signal == TradeSignal.CALL && snap.confidence >= config.minConfidence -> TradeDirection.CALL
            snap.signal == TradeSignal.PUT && snap.confidence >= config.minConfidence -> TradeDirection.PUT
            else -> null
        } ?: return

        val stake = computeNextStake(config, state.currentStepIndex)
        if (stake > state.practiceBalance || stake <= 0.0) {
            haltBot(BotStatus.IDLE, "Insufficient practice balance for $${formatMoney(stake)} stake")
            return
        }

        appendLog(
            LogLevel.SIGNAL,
            "Signal Triggered: ${direction.name} (${snap.confidence}%)",
            "${state.selectedAsset.symbol} • ${snap.reason}"
        )

        executePosition(
            direction = direction,
            stake = stake,
            isBotTrade = true,
            strategyLabel = config.strategyType.shortName,
            stepIndex = state.currentStepIndex
        )
    }

    private fun haltBot(status: BotStatus, reason: String) {
        _uiState.update {
            it.copy(
                isBotRunning = false,
                botStatus = status,
                lastBannerMessage = reason
            )
        }
        appendLog(LogLevel.RISK, "Bot Halted: ${status.label}", reason)
    }

    fun startBot() {
        val state = _uiState.value
        if (state.practiceBalance < state.botConfig.baseStake) {
            _uiState.update {
                it.copy(lastBannerMessage = "Practice balance too low. Top up or lower base stake.")
            }
            return
        }
        val nextStake = computeNextStake(state.botConfig, 0)
        _uiState.update {
            it.copy(
                isBotRunning = true,
                botStatus = BotStatus.SCANNING,
                currentStepIndex = 0,
                consecutiveLosses = 0,
                nextBotStake = nextStake,
                lastBannerMessage = null
            )
        }
        appendLog(
            LogLevel.INFO,
            "Bot Started (${state.botConfig.strategyType.shortName})",
            "Scanning ${state.selectedAsset.symbol} • Min Confidence ${state.botConfig.minConfidence}% • Stake $${formatMoney(nextStake)}"
        )
    }

    fun stopBot() {
        _uiState.update {
            it.copy(
                isBotRunning = false,
                botStatus = BotStatus.IDLE
            )
        }
        appendLog(LogLevel.INFO, "Bot Stopped", "Automated signal execution paused by user")
    }

    fun placeManualTrade(direction: TradeDirection) {
        val state = _uiState.value
        val stake = state.botConfig.baseStake
        if (stake > state.practiceBalance) {
            _uiState.update {
                it.copy(lastBannerMessage = "Insufficient practice balance ($${formatMoney(state.practiceBalance)})")
            }
            return
        }
        viewModelScope.launch {
            executePosition(
                direction = direction,
                stake = stake,
                isBotTrade = false,
                strategyLabel = "Manual",
                stepIndex = 0
            )
        }
    }

    private suspend fun executePosition(
        direction: TradeDirection,
        stake: Double,
        isBotTrade: Boolean,
        strategyLabel: String,
        stepIndex: Int
    ) {
        val state = _uiState.value
        val now = System.currentTimeMillis()
        val newBalance = (state.practiceBalance - stake).coerceAtLeast(0.0)
        val position = OpenPosition(
            id = UUID.randomUUID().toString(),
            assetSymbol = state.selectedAsset.symbol,
            direction = direction,
            entryPrice = state.currentPrice,
            stake = stake,
            payoutPercent = state.selectedAsset.payoutPercent,
            openedAtMillis = now,
            expiresAtMillis = now + state.botConfig.expirySeconds * 1_000L,
            isBotTrade = isBotTrade,
            strategyLabel = strategyLabel,
            rsiAtEntry = state.technicalSnapshot.rsi14,
            stepIndex = stepIndex
        )
        repository.updateBalanceOnly(newBalance)
        _uiState.update {
            it.copy(
                practiceBalance = newBalance,
                openPositions = it.openPositions + position,
                botStatus = if (it.isBotRunning) BotStatus.IN_POSITION else it.botStatus,
                lastBannerMessage = null
            )
        }
        appendLog(
            LogLevel.TRADE,
            "Opened ${direction.name} • ${state.selectedAsset.symbol}",
            "${if (isBotTrade) "BOT" else "MANUAL"} | Stake $${formatMoney(stake)} | Entry ${formatPrice(position.entryPrice, position.assetSymbol)} | ${state.botConfig.expirySeconds}s"
        )
    }

    fun selectAsset(asset: MarketAsset) {
        if (asset.symbol == _uiState.value.selectedAsset.symbol) return
        viewModelScope.launch {
            initializeAssetFeed(asset)
            appendLog(
                LogLevel.INFO,
                "Switched Asset to ${asset.symbol}",
                "Payout ${asset.payoutPercent}% • Feed ${_uiState.value.feedSource.label}"
            )
        }
    }

    fun updateBotConfig(transform: (BotConfig) -> BotConfig) {
        _uiState.update { state ->
            val updated = transform(state.botConfig)
            val snap = IndicatorCalculator.evaluate(state.candles, updated.strategyType)
            state.copy(
                botConfig = updated,
                technicalSnapshot = snap,
                nextBotStake = computeNextStake(updated, state.currentStepIndex)
            )
        }
    }

    fun saveStrategyPreset(name: String) {
        if (name.isBlank()) return
        val config = _uiState.value.botConfig
        viewModelScope.launch {
            repository.savePreset(name, config)
            _uiState.update { it.copy(activePresetName = name.trim()) }
            appendLog(LogLevel.INFO, "Preset Saved", "\"${name.trim()}\" stored to local strategy vault")
        }
    }

    fun applyPreset(preset: StrategyPresetEntity) {
        val strategy = runCatching { StrategyType.valueOf(preset.strategyTypeName) }
            .getOrDefault(StrategyType.RSI_EMA_CONFLUENCE)
        val moneyMode = runCatching { MoneyManagementMode.valueOf(preset.moneyModeName) }
            .getOrDefault(MoneyManagementMode.FLAT)

        updateBotConfig {
            it.copy(
                strategyType = strategy,
                baseStake = preset.baseStake,
                expirySeconds = preset.expirySeconds,
                moneyMode = moneyMode,
                martingaleMultiplier = preset.martingaleMultiplier,
                maxSteps = preset.maxSteps,
                dailyTakeProfit = preset.dailyTakeProfit,
                dailyStopLoss = preset.dailyStopLoss,
                maxConsecutiveLosses = preset.maxConsecutiveLosses,
                minConfidence = preset.minConfidence
            )
        }
        _uiState.update { it.copy(activePresetName = preset.name, currentStepIndex = 0) }
        appendLog(LogLevel.INFO, "Preset Loaded", "Active profile: ${preset.name}")
    }

    fun deletePreset(presetId: Long) {
        viewModelScope.launch {
            repository.deletePreset(presetId)
        }
    }

    fun resetPracticeAccount() {
        viewModelScope.launch {
            repository.resetPracticeAccount(10_000.0)
            _uiState.update {
                it.copy(
                    practiceBalance = 10_000.0,
                    sessionPnl = 0.0,
                    currentStepIndex = 0,
                    consecutiveLosses = 0,
                    nextBotStake = it.botConfig.baseStake,
                    lastBannerMessage = "Practice balance reset to $10,000.00"
                )
            }
            appendLog(LogLevel.INFO, "Account Reset", "Virtual balance restored to $10,000.00")
        }
    }

    fun clearTradeHistory() {
        viewModelScope.launch {
            repository.clearAllTrades()
            _uiState.update { it.copy(sessionPnl = 0.0) }
            appendLog(LogLevel.INFO, "Journal Cleared", "Historical trade ledger wiped")
        }
    }

    fun dismissBanner() {
        _uiState.update { it.copy(lastBannerMessage = null) }
    }

    private fun computeNextStake(config: BotConfig, stepIndex: Int): Double {
        if (stepIndex <= 0 || config.moneyMode == MoneyManagementMode.FLAT) {
            return config.baseStake
        }
        val raw = when (config.moneyMode) {
            MoneyManagementMode.FLAT -> config.baseStake
            MoneyManagementMode.MARTINGALE -> config.baseStake * config.martingaleMultiplier.pow(stepIndex.toDouble())
            MoneyManagementMode.SOROS -> {
                val multiplier = 1.0 + (config.payoutPercent / 100.0)
                config.baseStake * multiplier.pow(stepIndex.toDouble())
            }
        }
        return ((raw * 100.0).roundToInt() / 100.0).coerceAtMost(5_000.0)
    }

    private fun appendLog(level: LogLevel, title: String, detail: String) {
        val entry = BotLogEntry(
            id = UUID.randomUUID().toString(),
            timestampMillis = System.currentTimeMillis(),
            level = level,
            title = title,
            detail = detail
        )
        _uiState.update { state ->
            state.copy(logs = (listOf(entry) + state.logs).take(120))
        }
    }

    private fun formatMoney(v: Double): String = String.format(Locale.US, "%.2f", v)

    private fun formatPrice(price: Double, symbol: String): String {
        val decimals = MarketAsset.defaultAssets.find { it.symbol == symbol }?.decimals ?: 2
        return String.format(Locale.US, "%.${decimals}f", price)
    }

    class Factory(private val repository: TradingRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TradingBotViewModel(repository) as T
        }
    }
}
