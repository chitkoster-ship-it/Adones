package com.example.data.market

import com.example.domain.model.Candle
import com.example.domain.model.FeedSource
import com.example.domain.model.MarketAsset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

class MarketFeedClient {

    suspend fun fetchLiveSpotPrice(asset: MarketAsset): Pair<Double, FeedSource> =
        withContext(Dispatchers.IO) {
            val urlStr = when (asset.symbol) {
                "BTC-USD", "ETH-USD", "SOL-USD" ->
                    "https://api.coinbase.com/v2/prices/${asset.symbol}/spot"
                "EUR-USD" ->
                    "https://api.frankfurter.app/latest?from=EUR&to=USD"
                else -> null
            }

            if (urlStr != null) {
                try {
                    val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 2500
                        readTimeout = 2500
                        requestMethod = "GET"
                        setRequestProperty("Accept", "application/json")
                    }
                    if (conn.responseCode == 200) {
                        val body = conn.inputStream.bufferedReader().use { it.readText() }
                        val json = JSONObject(body)
                        val price = when (asset.symbol) {
                            "EUR-USD" -> json.getJSONObject("rates").getDouble("USD")
                            else -> json.getJSONObject("data").getString("amount").toDouble()
                        }
                        if (price > 0.0) {
                            return@withContext price to FeedSource.LIVE_API
                        }
                    }
                    conn.disconnect()
                } catch (_: Exception) {
                    // Fall back smoothly to calibrated local simulator feed
                }
            }
            return@withContext asset.seedPrice to FeedSource.SIMULATOR
        }

    fun generateInitialCandles(
        asset: MarketAsset,
        anchorPrice: Double,
        count: Int = 55,
        candleIntervalMillis: Long = 5_000L
    ): List<Candle> {
        val now = System.currentTimeMillis()
        val rng = Random(asset.symbol.hashCode() + (now / 60_000L).toInt())
        val candles = ArrayList<Candle>(count)

        // Build backward walk and then shift so final close matches anchorPrice
        val rawPrices = DoubleArray(count + 1)
        rawPrices[0] = anchorPrice
        for (i in 1..count) {
            val wave = sin(i * 0.35) * asset.volatilityFactor * 0.65
            val noise = (rng.nextDouble() - 0.5) * 2.0 * asset.volatilityFactor
            rawPrices[i] = rawPrices[i - 1] * (1.0 + wave + noise)
        }
        val ratio = anchorPrice / rawPrices[count]

        for (i in 0 until count) {
            val open = rawPrices[i] * ratio
            val close = rawPrices[i + 1] * ratio
            val wickScale = asset.volatilityFactor * anchorPrice * (0.4 + rng.nextDouble() * 0.8)
            val high = max(open, close) + abs(wickScale * rng.nextDouble())
            val low = min(open, close) - abs(wickScale * rng.nextDouble())
            val ts = now - (count - 1 - i) * candleIntervalMillis
            candles.add(Candle(ts, open, high, low, close))
        }
        return candles
    }

    fun nextSimulatedTickPrice(
        currentPrice: Double,
        asset: MarketAsset,
        tickCount: Long
    ): Double {
        val microWave = sin(tickCount * 0.28) * asset.volatilityFactor * 0.32
        val randomShock = (Random.nextDouble() - 0.5) * asset.volatilityFactor * 0.85
        return (currentPrice * (1.0 + microWave + randomShock)).coerceAtLeast(0.0001)
    }
}
