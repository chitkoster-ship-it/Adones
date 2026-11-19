package com.example.data

import com.example.data.local.AppDatabase
import com.example.data.market.MarketFeedClient
import com.example.data.repository.TradingRepository
import com.example.IqTradingBotApplication

// Helper container in Application for clean constructor injection into ViewModelFactory
class AppContainer(application: IqTradingBotApplication) {
    private val database: AppDatabase by lazy {
        AppDatabase.getInstance(application)
    }
    private val marketFeedClient: MarketFeedClient by lazy {
        MarketFeedClient()
    }
    val tradingRepository: TradingRepository by lazy {
        TradingRepository(
            tradeRecordDao = database.tradeRecordDao(),
            strategyPresetDao = database.strategyPresetDao(),
            accountStateDao = database.accountStateDao(),
            marketFeedClient = marketFeedClient
        )
    }
}
