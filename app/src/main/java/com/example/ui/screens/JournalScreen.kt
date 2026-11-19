package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.TradeRecordEntity
import com.example.domain.model.MarketAsset
import com.example.ui.components.EquityCurveCanvas
import com.example.ui.theme.BorderHairline
import com.example.ui.theme.CrimsonPut
import com.example.ui.theme.CrimsonPutDim
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.EmeraldCall
import com.example.ui.theme.EmeraldCallDim
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SlateCard
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class JournalFilter(val label: String) {
    ALL("All"),
    WINS("Wins (ITM)"),
    LOSSES("Losses (OTM)"),
    BOT("Bot Auto"),
    MANUAL("Manual")
}

@Composable
fun JournalScreen(
    trades: List<TradeRecordEntity>,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf(JournalFilter.ALL) }
    val timeFormatter = remember { SimpleDateFormat("MMM dd, HH:mm:ss", Locale.US) }

    val filteredTrades = remember(trades, selectedFilter) {
        when (selectedFilter) {
            JournalFilter.ALL -> trades
            JournalFilter.WINS -> trades.filter { it.outcome == "WIN" }
            JournalFilter.LOSSES -> trades.filter { it.outcome == "LOSS" }
            JournalFilter.BOT -> trades.filter { it.isBotTrade }
            JournalFilter.MANUAL -> trades.filter { !it.isBotTrade }
        }
    }

    val totalTrades = trades.size
    val wins = trades.count { it.outcome == "WIN" }
    val winRate = if (totalTrades > 0) (wins * 100.0 / totalTrades) else 0.0
    val netPnl = trades.sumOf { it.profitLoss }
    val grossProfit = trades.filter { it.profitLoss > 0 }.sumOf { it.profitLoss }
    val grossLoss = kotlin.math.abs(trades.filter { it.profitLoss < 0 }.sumOf { it.profitLoss })
    val profitFactor = when {
        grossLoss > 0.0 -> grossProfit / grossLoss
        grossProfit > 0.0 -> 9.99
        else -> 0.0
    }

    val equityPoints = remember(trades) {
        val chronological = trades.reversed()
        val list = mutableListOf(10_000.0)
        var running = 10_000.0
        chronological.forEach {
            running += it.profitLoss
            list.add(running)
        }
        list
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text(
                    text = "PERFORMANCE ANALYTICS",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyberCyan
                )
                Text(
                    text = "Execution Journal",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary
                )
            }
        }

        // KPI Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KpiCard(
                    title = "NET PNL",
                    value = "${if (netPnl >= 0) "+" else ""}$${String.format(Locale.US, "%.2f", netPnl)}",
                    valueColor = if (netPnl >= 0) EmeraldCall else CrimsonPut,
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "WIN RATE",
                    value = "${String.format(Locale.US, "%.1f", winRate)}%",
                    valueColor = if (winRate >= 54.0) EmeraldCall else TextPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KpiCard(
                    title = "PROFIT FACTOR",
                    value = String.format(Locale.US, "%.2f", profitFactor),
                    valueColor = CyberCyan,
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "TOTAL TRADES",
                    value = "$totalTrades ($wins W)",
                    valueColor = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Equity Curve
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "SIMULATED EQUITY CURVE",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
                EquityCurveCanvas(equityPoints = equityPoints)
            }
        }

        // Filter Chips
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(JournalFilter.entries) { filter ->
                    val selected = selectedFilter == filter
                    FilterChip(
                        selected = selected,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldCallDim,
                            selectedLabelColor = EmeraldCall,
                            containerColor = SlateCard,
                            labelColor = TextSecondary
                        ),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("journal_filter_${filter.name}")
                    )
                }
            }
        }

        if (filteredTrades.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SlateCard)
                        .border(1.dp, BorderHairline, RoundedCornerShape(16.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_quant_banner_1795089629373),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp)
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        ObsidianBg.copy(alpha = 0.65f),
                                        ObsidianBg.copy(alpha = 0.95f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.History,
                            contentDescription = "Empty journal",
                            tint = CyberCyan,
                            modifier = Modifier.size(34.dp)
                        )
                        Text(
                            text = stringResource(R.string.empty_history_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = stringResource(R.string.empty_history_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        } else {
            items(filteredTrades, key = { it.id }) { trade ->
                TradeHistoryCard(trade = trade, timeFormatter = timeFormatter)
            }
        }
    }
}

@Composable
private fun KpiCard(
    title: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(SlateCard)
            .border(1.dp, BorderHairline, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Text(text = title, style = MaterialTheme.typography.labelSmall, color = TextMuted)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontFamily = JetBrainsMonoFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = valueColor
        )
    }
}

@Composable
private fun TradeHistoryCard(
    trade: TradeRecordEntity,
    timeFormatter: SimpleDateFormat
) {
    val isWin = trade.outcome == "WIN"
    val isCall = trade.direction == "CALL"
    val outcomeColor = when (trade.outcome) {
        "WIN" -> EmeraldCall
        "LOSS" -> CrimsonPut
        else -> TextSecondary
    }
    val decimals = MarketAsset.defaultAssets.find { it.symbol == trade.assetSymbol }?.decimals ?: 2
    val pnlPrefix = if (trade.profitLoss >= 0) "+" else ""

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SlateCard)
            .border(1.dp, BorderHairline, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isCall) EmeraldCallDim else CrimsonPutDim)
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = if (isCall) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                            contentDescription = trade.direction,
                            tint = if (isCall) EmeraldCall else CrimsonPut,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = trade.direction,
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (isCall) EmeraldCall else CrimsonPut
                        )
                    }
                }
                Text(
                    text = trade.assetSymbol,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary
                )
                Text(
                    text = if (trade.isBotTrade) "BOT • ${trade.strategyName}" else "MANUAL",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyberCyan
                )
            }

            Text(
                text = "$pnlPrefix$${String.format(Locale.US, "%.2f", trade.profitLoss)}",
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = outcomeColor
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Entry ${String.format(Locale.US, "%.${decimals}f", trade.entryPrice)} → ${String.format(Locale.US, "%.${decimals}f", trade.closePrice)}",
                fontFamily = JetBrainsMonoFamily,
                fontSize = 11.sp,
                color = TextSecondary
            )
            Text(
                text = "Stake $${String.format(Locale.US, "%.0f", trade.stake)} • ${if (isWin) "ITM" else trade.outcome}",
                fontFamily = JetBrainsMonoFamily,
                fontSize = 11.sp,
                color = TextMuted
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "RSI ${String.format(Locale.US, "%.1f", trade.rsiAtEntry)} • Step ${trade.stepIndex}",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
            Text(
                text = timeFormatter.format(Date(trade.closedAtMillis)),
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }
    }
}
