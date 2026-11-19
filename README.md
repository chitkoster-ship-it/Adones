# IQ Trading Bot (Android)

An automated quantitative binary-options trading bot terminal and strategy studio built with **Kotlin**, **Jetpack Compose (Material 3)**, **Room + KSP**, and **Navigation Compose**.

## Features

- **Live Market Terminal**: Real-time OHLC candlestick chart with EMA(9), EMA(21), active trade strike lines, and interactive touch inspection crosshair. Fetches live public spot prices (`BTC-USD`, `ETH-USD`, `SOL-USD`, `EUR-USD`) with automatic calibrated simulator fallback.
- **Quantitative Signal Engine**: Real-time technical indicator calculation supporting three selectable algorithms:
  - *RSI + EMA Confluence*
  - *Bollinger Mean Reversion*
  - *EMA Momentum Breakout*
- **Automated Bot & Manual Override**: Start/stop automated signal execution or place manual `CALL` / `PUT` paper positions with live expiry countdowns and ITM/OTM tracking.
- **Money Management & Risk Guardrails**: Configurable Flat, Martingale, and Soros Compound sizing alongside Daily Take Profit, Daily Stop Loss, and Max Consecutive Losses auto-halt protections.
- **Strategy Vault & Execution Journal**: Local Room persistence for custom strategy presets, practice account equity ($10,000 virtual starting balance), performance KPIs, equity curve visualization, and live bot audit logs.

> **Practice Engine Notice**: All trade executions run locally in a simulated paper-trading engine; no real funds or external broker credentials are used.
