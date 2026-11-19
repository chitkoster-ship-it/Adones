package com.example.domain.model

enum class TradeDirection {
    CALL,
    PUT
}

enum class TradeSignal {
    CALL,
    PUT,
    NEUTRAL
}

enum class TradeOutcome {
    WIN,
    LOSS,
    TIE
}

enum class StrategyType(
    val displayName: String,
    val shortName: String,
    val description: String
) {
    RSI_EMA_CONFLUENCE(
        displayName = "RSI + EMA Confluence",
        shortName = "RSI+EMA",
        description = "Triggers CALL when RSI < 34 with EMA9 turning above EMA21, or PUT when RSI > 66 with EMA9 below EMA21."
    ),
    BOLLINGER_REVERSION(
        displayName = "Bollinger Mean Reversion",
        shortName = "BB Reversion",
        description = "Fades extreme volatility spikes at the 20-period upper/lower Bollinger Bands when momentum exhausts."
    ),
    MOMENTUM_BREAKOUT(
        displayName = "EMA Momentum Breakout",
        shortName = "Momentum",
        description = "Follows 3-candle directional expansion aligned with EMA9/EMA21 trend slope."
    )
}

enum class MoneyManagementMode(
    val displayName: String,
    val description: String
) {
    FLAT(
        displayName = "Flat Stake",
        description = "Keeps stake constant on every execution."
    ),
    MARTINGALE(
        displayName = "Martingale",
        description = "Multiplies stake after a loss to recover drawdown, up to max step cap."
    ),
    SOROS(
        displayName = "Soros Compound",
        description = "Reinvests profit from winning trades up to max consecutive compound steps."
    )
}

enum class BotStatus(val label: String) {
    IDLE("STANDBY"),
    SCANNING("SCANNING SIGNALS"),
    IN_POSITION("POSITION ACTIVE"),
    COOLDOWN("COOLDOWN"),
    HALTED_TP("TAKE PROFIT REACHED"),
    HALTED_SL("STOP LOSS HIT"),
    HALTED_LOSS_STREAK("MAX STREAK HALT")
}

enum class FeedSource(val label: String) {
    LIVE_API("LIVE MARKET"),
    SIMULATOR("SIM FEED")
}

enum class LogLevel {
    INFO,
    SIGNAL,
    TRADE,
    RISK
}

data class MarketAsset(
    val symbol: String,
    val baseCurrency: String,
    val quoteCurrency: String,
    val category: String,
    val seedPrice: Double,
    val decimals: Int,
    val volatilityFactor: Double,
    val payoutPercent: Int = 85
) {
    companion object {
        val defaultAssets = listOf(
            MarketAsset("BTC-USD", "BTC", "USD", "CRYPTO", 91450.0, 2, 0.00075, 87),
            MarketAsset("ETH-USD", "ETH", "USD", "CRYPTO", 3120.0, 2, 0.00095, 86),
            MarketAsset("SOL-USD", "SOL", "USD", "CRYPTO", 238.50, 2, 0.00125, 85),
            MarketAsset("EUR-USD", "EUR", "USD", "FOREX", 1.0565, 5, 0.00022, 85),
            MarketAsset("XAU-USD", "XAU", "USD", "METAL", 2645.20, 2, 0.00045, 84)
        )
    }
}

data class Candle(
    val timestampMillis: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double
) {
    val isBullish: Boolean get() = close >= open
}

data class TechnicalSnapshot(
    val rsi14: Double = 50.0,
    val ema9: Double = 0.0,
    val ema21: Double = 0.0,
    val bollingerUpper: Double = 0.0,
    val bollingerMiddle: Double = 0.0,
    val bollingerLower: Double = 0.0,
    val signal: TradeSignal = TradeSignal.NEUTRAL,
    val confidence: Int = 0,
    val reason: String = "Calibrating indicator window..."
)

data class BotConfig(
    val strategyType: StrategyType = StrategyType.RSI_EMA_CONFLUENCE,
    val baseStake: Double = 25.0,
    val expirySeconds: Int = 30,
    val moneyMode: MoneyManagementMode = MoneyManagementMode.FLAT,
    val martingaleMultiplier: Double = 2.1,
    val maxSteps: Int = 3,
    val dailyTakeProfit: Double = 500.0,
    val dailyStopLoss: Double = 250.0,
    val maxConsecutiveLosses: Int = 4,
    val minConfidence: Int = 62,
    val payoutPercent: Int = 85
)

data class OpenPosition(
    val id: String,
    val assetSymbol: String,
    val direction: TradeDirection,
    val entryPrice: Double,
    val stake: Double,
    val payoutPercent: Int,
    val openedAtMillis: Long,
    val expiresAtMillis: Long,
    val isBotTrade: Boolean,
    val strategyLabel: String,
    val rsiAtEntry: Double,
    val stepIndex: Int
)

data class BotLogEntry(
    val id: String,
    val timestampMillis: Long,
    val level: LogLevel,
    val title: String,
    val detail: String
)
