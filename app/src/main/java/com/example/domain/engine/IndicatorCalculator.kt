package com.example.domain.engine

import com.example.domain.model.Candle
import com.example.domain.model.StrategyType
import com.example.domain.model.TechnicalSnapshot
import com.example.domain.model.TradeSignal
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

object IndicatorCalculator {

    fun calculateEmaSeries(closes: List<Double>, period: Int): List<Double> {
        if (closes.isEmpty()) return emptyList()
        val k = 2.0 / (period + 1.0)
        val result = ArrayList<Double>(closes.size)
        var ema = closes.first()
        result.add(ema)
        for (i in 1 until closes.size) {
            ema = closes[i] * k + ema * (1.0 - k)
            result.add(ema)
        }
        return result
    }

    fun calculateRsi(closes: List<Double>, period: Int = 14): Double {
        if (closes.size <= period) return 50.0
        var gains = 0.0
        var losses = 0.0
        val startIdx = closes.size - period
        for (i in startIdx until closes.size) {
            val diff = closes[i] - closes[i - 1]
            if (diff > 0) gains += diff else losses += abs(diff)
        }
        val avgGain = gains / period
        val avgLoss = losses / period
        if (avgLoss == 0.0) return if (avgGain == 0.0) 50.0 else 99.0
        val rs = avgGain / avgLoss
        return (100.0 - (100.0 / (1.0 + rs))).coerceIn(1.0, 99.0)
    }

    fun calculateBollingerBands(
        closes: List<Double>,
        period: Int = 20,
        stdDevMult: Double = 2.0
    ): Triple<Double, Double, Double> {
        if (closes.isEmpty()) return Triple(0.0, 0.0, 0.0)
        val window = closes.takeLast(period)
        val mean = window.average()
        val variance = window.sumOf { (it - mean) * (it - mean) } / window.size
        val stdDev = sqrt(variance)
        return Triple(mean + stdDev * stdDevMult, mean, mean - stdDev * stdDevMult)
    }

    fun evaluate(
        candles: List<Candle>,
        strategyType: StrategyType
    ): TechnicalSnapshot {
        if (candles.size < 22) {
            val latest = candles.lastOrNull()?.close ?: 0.0
            return TechnicalSnapshot(
                ema9 = latest,
                ema21 = latest,
                bollingerUpper = latest,
                bollingerMiddle = latest,
                bollingerLower = latest,
                reason = "Warming up ${candles.size}/22 candles..."
            )
        }

        val closes = candles.map { it.close }
        val latestPrice = closes.last()
        val rsi14 = calculateRsi(closes, 14)
        val ema9Series = calculateEmaSeries(closes, 9)
        val ema21Series = calculateEmaSeries(closes, 21)
        val ema9 = ema9Series.last()
        val ema21 = ema21Series.last()
        val prevEma9 = ema9Series[ema9Series.lastIndex - 1]
        val prevEma21 = ema21Series[ema21Series.lastIndex - 1]
        val (bbUpper, bbMid, bbLower) = calculateBollingerBands(closes, 20, 2.0)

        return when (strategyType) {
            StrategyType.RSI_EMA_CONFLUENCE -> {
                val bullishCrossOrTurn = (ema9 > ema21 && prevEma9 <= prevEma21) ||
                    (ema9 > prevEma9 && rsi14 < 38.0)
                val bearishCrossOrTurn = (ema9 < ema21 && prevEma9 >= prevEma21) ||
                    (ema9 < prevEma9 && rsi14 > 62.0)

                when {
                    rsi14 <= 36.0 && bullishCrossOrTurn -> {
                        val conf = (60 + ((38.0 - rsi14) * 2.2)).roundToInt().coerceIn(55, 96)
                        TechnicalSnapshot(
                            rsi14, ema9, ema21, bbUpper, bbMid, bbLower,
                            TradeSignal.CALL, conf,
                            "Oversold RSI (${format1(rsi14)}) + EMA9 turning bullish"
                        )
                    }
                    rsi14 >= 64.0 && bearishCrossOrTurn -> {
                        val conf = (60 + ((rsi14 - 62.0) * 2.2)).roundToInt().coerceIn(55, 96)
                        TechnicalSnapshot(
                            rsi14, ema9, ema21, bbUpper, bbMid, bbLower,
                            TradeSignal.PUT, conf,
                            "Overbought RSI (${format1(rsi14)}) + EMA9 turning bearish"
                        )
                    }
                    ema9 > ema21 && rsi14 in 45.0..60.0 -> {
                        TechnicalSnapshot(
                            rsi14, ema9, ema21, bbUpper, bbMid, bbLower,
                            TradeSignal.CALL, 48,
                            "Mild bullish EMA9 > EMA21 drift (RSI ${format1(rsi14)})"
                        )
                    }
                    ema9 < ema21 && rsi14 in 40.0..55.0 -> {
                        TechnicalSnapshot(
                            rsi14, ema9, ema21, bbUpper, bbMid, bbLower,
                            TradeSignal.PUT, 48,
                            "Mild bearish EMA9 < EMA21 drift (RSI ${format1(rsi14)})"
                        )
                    }
                    else -> {
                        TechnicalSnapshot(
                            rsi14, ema9, ema21, bbUpper, bbMid, bbLower,
                            TradeSignal.NEUTRAL, 32,
                            "Waiting for RSI/EMA confluence (RSI ${format1(rsi14)})"
                        )
                    }
                }
            }

            StrategyType.BOLLINGER_REVERSION -> {
                val bandWidth = (bbUpper - bbLower).coerceAtLeast(1e-8)
                val lowerDist = (latestPrice - bbLower) / bandWidth
                val upperDist = (bbUpper - latestPrice) / bandWidth
                val lastCandle = candles.last()

                when {
                    lowerDist <= 0.12 && rsi14 < 42.0 -> {
                        val conf = (65 + (1.0 - lowerDist.coerceAtLeast(0.0)) * 28).roundToInt().coerceIn(58, 95)
                        TechnicalSnapshot(
                            rsi14, ema9, ema21, bbUpper, bbMid, bbLower,
                            TradeSignal.CALL, conf,
                            "Lower BB touch + oversold mean reversion"
                        )
                    }
                    upperDist <= 0.12 && rsi14 > 58.0 -> {
                        val conf = (65 + (1.0 - upperDist.coerceAtLeast(0.0)) * 28).roundToInt().coerceIn(58, 95)
                        TechnicalSnapshot(
                            rsi14, ema9, ema21, bbUpper, bbMid, bbLower,
                            TradeSignal.PUT, conf,
                            "Upper BB extension + overbought reversion"
                        )
                    }
                    lastCandle.isBullish && lowerDist < 0.35 -> {
                        TechnicalSnapshot(
                            rsi14, ema9, ema21, bbUpper, bbMid, bbLower,
                            TradeSignal.CALL, 50,
                            "Rebounding inside lower Bollinger channel"
                        )
                    }
                    !lastCandle.isBullish && upperDist < 0.35 -> {
                        TechnicalSnapshot(
                            rsi14, ema9, ema21, bbUpper, bbMid, bbLower,
                            TradeSignal.PUT, 50,
                            "Rejecting inside upper Bollinger channel"
                        )
                    }
                    else -> {
                        TechnicalSnapshot(
                            rsi14, ema9, ema21, bbUpper, bbMid, bbLower,
                            TradeSignal.NEUTRAL, 30,
                            "Price balanced near BB midline"
                        )
                    }
                }
            }

            StrategyType.MOMENTUM_BREAKOUT -> {
                val recent3 = candles.takeLast(3)
                val threeBull = recent3.all { it.isBullish }
                val threeBear = recent3.all { !it.isBullish }
                val emaSpreadBull = ema9 > ema21 && ema9 > prevEma9
                val emaSpreadBear = ema9 < ema21 && ema9 < prevEma9

                when {
                    threeBull && emaSpreadBull && rsi14 in 50.0..74.0 -> {
                        val conf = (68 + (rsi14 - 50.0) * 0.9).roundToInt().coerceIn(60, 94)
                        TechnicalSnapshot(
                            rsi14, ema9, ema21, bbUpper, bbMid, bbLower,
                            TradeSignal.CALL, conf,
                            "3-candle bullish breakout aligned with EMA9 > EMA21"
                        )
                    }
                    threeBear && emaSpreadBear && rsi14 in 26.0..50.0 -> {
                        val conf = (68 + (50.0 - rsi14) * 0.9).roundToInt().coerceIn(60, 94)
                        TechnicalSnapshot(
                            rsi14, ema9, ema21, bbUpper, bbMid, bbLower,
                            TradeSignal.PUT, conf,
                            "3-candle bearish breakdown aligned with EMA9 < EMA21"
                        )
                    }
                    emaSpreadBull -> {
                        TechnicalSnapshot(
                            rsi14, ema9, ema21, bbUpper, bbMid, bbLower,
                            TradeSignal.CALL, 52,
                            "EMA9 upward slope building momentum"
                        )
                    }
                    emaSpreadBear -> {
                        TechnicalSnapshot(
                            rsi14, ema9, ema21, bbUpper, bbMid, bbLower,
                            TradeSignal.PUT, 52,
                            "EMA9 downward slope building pressure"
                        )
                    }
                    else -> {
                        TechnicalSnapshot(
                            rsi14, ema9, ema21, bbUpper, bbMid, bbLower,
                            TradeSignal.NEUTRAL, 35,
                            "Consolidating — awaiting 3-candle impulse"
                        )
                    }
                }
            }
        }
    }

    private fun format1(value: Double): String = String.format(java.util.Locale.US, "%.1f", value)
}
