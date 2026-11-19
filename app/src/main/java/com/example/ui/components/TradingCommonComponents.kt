package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.FeedSource
import com.example.domain.model.OpenPosition
import com.example.domain.model.TechnicalSnapshot
import com.example.domain.model.TradeDirection
import com.example.domain.model.TradeSignal
import com.example.ui.theme.BorderHairline
import com.example.ui.theme.CrimsonPut
import com.example.ui.theme.CrimsonPutDim
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberCyanDim
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
import java.util.Locale
import kotlin.math.max

@Composable
fun TerminalTopHeader(
    practiceBalance: Double,
    sessionPnl: Double,
    feedSource: FeedSource,
    onPracticeBadgeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "PRACTICE BALANCE",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
                Surface(
                    color = SolarAmberDim,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onPracticeBadgeClick)
                        .testTag("practice_mode_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Shield,
                            contentDescription = "Practice mode indicator",
                            tint = SolarAmber,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = "PAPER ENGINE",
                            style = MaterialTheme.typography.labelSmall,
                            color = SolarAmber,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "$${String.format(Locale.US, "%,.2f", practiceBalance)}",
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = TextPrimary,
                    modifier = Modifier.testTag("practice_balance_text")
                )
                val pnlPositive = sessionPnl >= 0
                val pnlColor = if (pnlPositive) EmeraldCall else CrimsonPut
                val pnlBg = if (pnlPositive) EmeraldCallDim else CrimsonPutDim
                val prefix = if (pnlPositive) "+" else ""
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(pnlBg)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "$prefix$${String.format(Locale.US, "%.2f", sessionPnl)}",
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        color = pnlColor
                    )
                }
            }
        }

        // Feed badge
        val isLive = feedSource == FeedSource.LIVE_API
        Surface(
            color = if (isLive) EmeraldCallDim else CyberCyanDim,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onPracticeBadgeClick)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(if (isLive) EmeraldCall else CyberCyan)
                )
                Text(
                    text = feedSource.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isLive) EmeraldCall else CyberCyan,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = "Feed info",
                    tint = if (isLive) EmeraldCall else CyberCyan,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

@Composable
fun SignalMetricsCard(
    snapshot: TechnicalSnapshot,
    minConfidence: Int,
    modifier: Modifier = Modifier
) {
    val signalColor by animateColorAsState(
        targetValue = when (snapshot.signal) {
            TradeSignal.CALL -> EmeraldCall
            TradeSignal.PUT -> CrimsonPut
            TradeSignal.NEUTRAL -> TextSecondary
        },
        label = "signalColor"
    )
    val signalBg = when (snapshot.signal) {
        TradeSignal.CALL -> EmeraldCallDim
        TradeSignal.PUT -> CrimsonPutDim
        TradeSignal.NEUTRAL -> SlateCardElevated
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SlateCard)
            .border(1.dp, BorderHairline, RoundedCornerShape(14.dp))
            .padding(12.dp)
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
                        .clip(RoundedCornerShape(8.dp))
                        .background(signalBg)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = when (snapshot.signal) {
                                TradeSignal.PUT -> Icons.AutoMirrored.Filled.TrendingDown
                                else -> Icons.AutoMirrored.Filled.TrendingUp
                            },
                            contentDescription = "Signal direction",
                            tint = signalColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${snapshot.signal.name} ${snapshot.confidence}%",
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = signalColor
                        )
                    }
                }
                Text(
                    text = "Threshold ≥$minConfidence%",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }

            // RSI pill
            val rsiColor = when {
                snapshot.rsi14 <= 35.0 -> EmeraldCall
                snapshot.rsi14 >= 65.0 -> CrimsonPut
                else -> CyberCyan
            }
            Text(
                text = "RSI(14) ${String.format(Locale.US, "%.1f", snapshot.rsi14)}",
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = rsiColor
            )
        }

        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { (snapshot.confidence / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = signalColor,
            trackColor = ObsidianBg
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = snapshot.reason,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun OpenPositionCard(
    position: OpenPosition,
    currentPrice: Double,
    currentTimeMillis: Long,
    decimals: Int,
    modifier: Modifier = Modifier
) {
    val totalMs = max(1L, position.expiresAtMillis - position.openedAtMillis)
    val remainingMs = max(0L, position.expiresAtMillis - currentTimeMillis)
    val progress = (remainingMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)
    val secondsLeft = ((remainingMs + 999L) / 1000L).toInt()

    val isItm = when (position.direction) {
        TradeDirection.CALL -> currentPrice > position.entryPrice
        TradeDirection.PUT -> currentPrice < position.entryPrice
    }
    val statusColor = if (isItm) EmeraldCall else CrimsonPut
    val dirColor = if (position.direction == TradeDirection.CALL) EmeraldCall else CrimsonPut

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SlateCardElevated)
            .border(1.dp, statusColor.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
            .padding(10.dp)
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
                        .background(if (position.direction == TradeDirection.CALL) EmeraldCallDim else CrimsonPutDim)
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = position.direction.name,
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = dirColor
                    )
                }
                Text(
                    text = "${position.assetSymbol} • $${String.format(Locale.US, "%.2f", position.stake)}",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary
                )
                Text(
                    text = if (position.isBotTrade) "BOT" else "MANUAL",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyberCyan
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (isItm) "ITM" else "OTM",
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = statusColor
                )
                Text(
                    text = "${secondsLeft}s",
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = TextPrimary
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = statusColor,
            trackColor = ObsidianBg
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Strike: ${String.format(Locale.US, "%.${decimals}f", position.entryPrice)} • Payout: +${position.payoutPercent}%",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted
        )
    }
}

@Composable
fun EquityCurveCanvas(
    equityPoints: List<Double>,
    modifier: Modifier = Modifier
) {
    if (equityPoints.size < 2) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(SlateCard)
                .border(1.dp, BorderHairline, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Execute at least 2 trades to plot equity curve",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }
        return
    }

    val isOverallProfit = equityPoints.last() >= equityPoints.first()
    val lineColor = if (isOverallProfit) EmeraldCall else CrimsonPut

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(SlateCard)
            .border(1.dp, BorderHairline, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(106.dp)) {
            val minVal = equityPoints.min()
            val maxVal = equityPoints.max()
            val span = (maxVal - minVal).coerceAtLeast(1.0)
            val w = size.width
            val h = size.height
            val stepX = w / (equityPoints.size - 1).coerceAtLeast(1)

            val strokePath = Path()
            val fillPath = Path()

            equityPoints.forEachIndexed { idx, value ->
                val x = idx * stepX
                val ratio = ((value - minVal) / span).toFloat().coerceIn(0f, 1f)
                val y = h - (ratio * (h - 12f) + 6f)
                if (idx == 0) {
                    strokePath.moveTo(x, y)
                    fillPath.moveTo(x, h)
                    fillPath.lineTo(x, y)
                } else {
                    strokePath.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }
                if (idx == equityPoints.lastIndex) {
                    fillPath.lineTo(x, h)
                    fillPath.close()
                }
            }

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(lineColor.copy(alpha = 0.25f), Color.Transparent)
                )
            )
            drawPath(
                path = strokePath,
                color = lineColor,
                style = Stroke(width = 3f, cap = StrokeCap.Round)
            )

            // Final node
            val lastRatio = ((equityPoints.last() - minVal) / span).toFloat().coerceIn(0f, 1f)
            val lastY = h - (lastRatio * (h - 12f) + 6f)
            drawCircle(
                color = lineColor,
                radius = 5f,
                center = Offset(w, lastY)
            )
        }
    }
}
