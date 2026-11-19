package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.model.BotStatus
import com.example.domain.model.MarketAsset
import com.example.domain.model.TradeDirection
import com.example.ui.components.CandlestickChart
import com.example.ui.components.OpenPositionCard
import com.example.ui.components.SignalMetricsCard
import com.example.ui.components.TerminalTopHeader
import com.example.ui.theme.BorderHairline
import com.example.ui.theme.CrimsonPut
import com.example.ui.theme.CrimsonPutDim
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.EmeraldCall
import com.example.ui.theme.EmeraldCallDim
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateCardElevated
import com.example.ui.theme.SolarAmber
import com.example.ui.theme.SolarAmberDim
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.TerminalUiState
import java.util.Locale

@Composable
fun TerminalScreen(
    state: TerminalUiState,
    onSelectAsset: (MarketAsset) -> Unit,
    onStartBot: () -> Unit,
    onStopBot: () -> Unit,
    onManualTrade: (TradeDirection) -> Unit,
    onDismissBanner: () -> Unit,
    onOpenStrategies: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDisclaimerDialog by remember { mutableStateOf(false) }

    if (showDisclaimerDialog) {
        AlertDialog(
            onDismissRequest = { showDisclaimerDialog = false },
            containerColor = SlateCardElevated,
            title = {
                Text(
                    text = stringResource(R.string.disclaimer_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.disclaimer_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = { showDisclaimerDialog = false }) {
                    Text("Understood", color = EmeraldCall)
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            TerminalTopHeader(
                practiceBalance = state.practiceBalance,
                sessionPnl = state.sessionPnl,
                feedSource = state.feedSource,
                onPracticeBadgeClick = { showDisclaimerDialog = true }
            )
        }

        // Alert / Risk Halt Banner
        if (state.lastBannerMessage != null) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SolarAmberDim)
                        .border(1.dp, SolarAmber.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = state.lastBannerMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = SolarAmber,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onDismissBanner,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Dismiss notification",
                            tint = SolarAmber,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Asset Selector Strip
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.assets, key = { it.symbol }) { asset ->
                    val isSelected = asset.symbol == state.selectedAsset.symbol
                    Surface(
                        color = if (isSelected) SlateCardElevated else SlateCard,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .border(
                                width = 1.dp,
                                color = if (isSelected) EmeraldCall else BorderHairline,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectAsset(asset) }
                            .testTag("asset_chip_${asset.symbol}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column {
                                Text(
                                    text = asset.symbol,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (isSelected) TextPrimary else TextSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${asset.category} • +${asset.payoutPercent}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) EmeraldCall else TextMuted
                                )
                            }
                        }
                    }
                }
            }
        }

        // Live Asset Ticker Headline
        item {
            val deltaPct = if (state.previousClosePrice > 0) {
                ((state.currentPrice - state.previousClosePrice) / state.previousClosePrice) * 100.0
            } else 0.0
            val isPositive = deltaPct >= 0.0
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "${state.selectedAsset.baseCurrency}/${state.selectedAsset.quoteCurrency} SPOT",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                    Text(
                        text = String.format(Locale.US, "%.${state.selectedAsset.decimals}f", state.currentPrice),
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        color = TextPrimary,
                        modifier = Modifier.testTag("live_asset_price")
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isPositive) EmeraldCallDim else CrimsonPutDim)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "${if (isPositive) "+" else ""}${String.format(Locale.US, "%.2f", deltaPct)}%",
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (isPositive) EmeraldCall else CrimsonPut
                    )
                }
            }
        }

        // Candlestick Chart
        item {
            CandlestickChart(
                candles = state.candles,
                currentPrice = state.currentPrice,
                decimals = state.selectedAsset.decimals,
                openPositions = state.openPositions.filter { it.assetSymbol == state.selectedAsset.symbol },
                strategyType = state.botConfig.strategyType,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // Technical Indicator Gauge
        item {
            SignalMetricsCard(
                snapshot = state.technicalSnapshot,
                minConfidence = state.botConfig.minConfidence,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // Bot Master Control Card
        item {
            val botColor by animateColorAsState(
                targetValue = if (state.isBotRunning) EmeraldCall else TextSecondary,
                label = "botColor"
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SlateCard)
                    .border(
                        width = 1.dp,
                        color = if (state.isBotRunning) EmeraldCall.copy(alpha = 0.6f) else BorderHairline,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (state.isBotRunning) EmeraldCallDim else SlateCardElevated),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SmartToy,
                                contentDescription = "Trading bot engine",
                                tint = botColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when (state.botStatus) {
                                                BotStatus.SCANNING, BotStatus.IN_POSITION -> EmeraldCall
                                                BotStatus.COOLDOWN -> CyberCyan
                                                BotStatus.HALTED_TP -> SolarAmber
                                                BotStatus.HALTED_SL, BotStatus.HALTED_LOSS_STREAK -> CrimsonPut
                                                BotStatus.IDLE -> TextMuted
                                            }
                                        )
                                )
                                Text(
                                    text = state.botStatus.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (state.isBotRunning) EmeraldCall else TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "${state.activePresetName} • ${state.botConfig.moneyMode.displayName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    IconButton(
                        onClick = onOpenStrategies,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("open_strategies_quick_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Tune,
                            contentDescription = "Configure bot strategy",
                            tint = CyberCyan
                        )
                    }
                }

                // Stake / Expiry / Step metrics row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(ObsidianBg)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricChip("NEXT STAKE", "$${String.format(Locale.US, "%.2f", state.nextBotStake)}")
                    MetricChip("EXPIRY", "${state.botConfig.expirySeconds}s")
                    MetricChip("STEP", "${state.currentStepIndex}/${state.botConfig.maxSteps}")
                    MetricChip("PAYOUT", "+${state.selectedAsset.payoutPercent}%")
                }

                // Start/Stop Primary Bot CTA
                Button(
                    onClick = { if (state.isBotRunning) onStopBot() else onStartBot() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.isBotRunning) CrimsonPut else EmeraldCall,
                        contentColor = if (state.isBotRunning) TextPrimary else ObsidianBg
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp)
                        .testTag(if (state.isBotRunning) "stop_bot_button" else "start_bot_button")
                ) {
                    Icon(
                        imageVector = if (state.isBotRunning) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(
                            if (state.isBotRunning) R.string.stop_bot else R.string.start_bot
                        ),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        // Manual Quick Execution Row
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MANUAL OVERRIDE",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                    Text(
                        text = "Stake $${String.format(Locale.US, "%.2f", state.botConfig.baseStake)} • ${state.botConfig.expirySeconds}s",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { onManualTrade(TradeDirection.CALL) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldCallDim,
                            contentColor = EmeraldCall
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 52.dp)
                            .border(1.dp, EmeraldCall.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                            .testTag("manual_call_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = "Execute manual CALL",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${stringResource(R.string.call_higher)} ↑",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    Button(
                        onClick = { onManualTrade(TradeDirection.PUT) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CrimsonPutDim,
                            contentColor = CrimsonPut
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 52.dp)
                            .border(1.dp, CrimsonPut.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                            .testTag("manual_put_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingDown,
                            contentDescription = "Execute manual PUT",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${stringResource(R.string.put_lower)} ↓",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }

        // Open Positions Strip
        item {
            AnimatedVisibility(visible = state.openPositions.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "OPEN POSITIONS (${state.openPositions.size})",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberCyan
                    )
                    state.openPositions.forEach { pos ->
                        val priceForAsset = if (pos.assetSymbol == state.selectedAsset.symbol) {
                            state.currentPrice
                        } else {
                            pos.entryPrice
                        }
                        val dec = state.assets.find { it.symbol == pos.assetSymbol }?.decimals ?: 2
                        OpenPositionCard(
                            position = pos,
                            currentPrice = priceForAsset,
                            currentTimeMillis = state.currentTimeMillis,
                            decimals = dec
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted
        )
        Text(
            text = value,
            fontFamily = JetBrainsMonoFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = TextPrimary
        )
    }
}
