package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.engine.IndicatorCalculator
import com.example.domain.model.Candle
import com.example.domain.model.OpenPosition
import com.example.domain.model.StrategyType
import com.example.domain.model.TradeDirection
import com.example.ui.theme.BorderHairline
import com.example.ui.theme.CrimsonPut
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.EmeraldCall
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SolarAmber
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun CandlestickChart(
    candles: List<Candle>,
    currentPrice: Double,
    decimals: Int,
    openPositions: List<OpenPosition>,
    strategyType: StrategyType,
    modifier: Modifier = Modifier
) {
    val visibleCandles = remember(candles) { candles.takeLast(44) }
    val ema9Series = remember(visibleCandles) {
        IndicatorCalculator.calculateEmaSeries(visibleCandles.map { it.close }, 9)
    }
    val ema21Series = remember(visibleCandles) {
        IndicatorCalculator.calculateEmaSeries(visibleCandles.map { it.close }, 21)
    }

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss", Locale.US) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SlateCard)
            .border(1.dp, BorderHairline, RoundedCornerShape(16.dp))
            .padding(12.dp)
            .testTag("candlestick_chart")
    ) {
        // Legend + Crosshair OHLC readout
        val inspected = selectedIndex?.let { visibleCandles.getOrNull(it) }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (inspected != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = timeFormatter.format(Date(inspected.timestampMillis)),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                    Text(
                        text = "O:${fmt(inspected.open, decimals)} H:${fmt(inspected.high, decimals)} L:${fmt(inspected.low, decimals)} C:${fmt(inspected.close, decimals)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (inspected.isBullish) EmeraldCall else CrimsonPut
                    )
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendDot("EMA 9", CyberCyan)
                    LegendDot("EMA 21", SolarAmber)
                    LegendDot(strategyType.shortName, EmeraldCall)
                }
            }
            Text(
                text = if (inspected != null) "INSPECTING" else "5s OHLC",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (visibleCandles.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Connecting market stream...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        } else {
            val minLow = visibleCandles.minOf { it.low }
            val maxHigh = visibleCandles.maxOf { it.high }
            val posMin = openPositions.minOfOrNull { it.entryPrice } ?: minLow
            val posMax = openPositions.maxOfOrNull { it.entryPrice } ?: maxHigh
            val rawMin = min(minLow, min(posMin, currentPrice))
            val rawMax = max(maxHigh, max(posMax, currentPrice))
            val span = (rawMax - rawMin).coerceAtLeast(rawMax * 0.0005)
            val paddedMin = rawMin - span * 0.14
            val paddedMax = rawMax + span * 0.14
            val totalRange = (paddedMax - paddedMin).coerceAtLeast(1e-9)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(215.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .pointerInput(visibleCandles.size) {
                            detectTapGestures(
                                onTap = { offset ->
                                    val idx = ((offset.x / size.width) * visibleCandles.size)
                                        .roundToInt()
                                        .coerceIn(0, visibleCandles.lastIndex)
                                    selectedIndex = if (selectedIndex == idx) null else idx
                                }
                            )
                        }
                        .pointerInput(visibleCandles.size) {
                            detectDragGestures(
                                onDragEnd = { selectedIndex = null },
                                onDragCancel = { selectedIndex = null },
                                onDrag = { change, _ ->
                                    val idx = ((change.position.x / size.width) * visibleCandles.size)
                                        .roundToInt()
                                        .coerceIn(0, visibleCandles.lastIndex)
                                    selectedIndex = idx
                                }
                            )
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        fun priceToY(price: Double): Float {
                            val ratio = ((price - paddedMin) / totalRange).toFloat().coerceIn(0f, 1f)
                            return h - (ratio * h)
                        }

                        // Subtle horizontal price gridlines
                        val gridSteps = 4
                        for (i in 0..gridSteps) {
                            val y = (h / gridSteps) * i
                            drawLine(
                                color = BorderHairline.copy(alpha = 0.5f),
                                start = Offset(0f, y),
                                end = Offset(w, y),
                                strokeWidth = 1f
                            )
                        }

                        val stepX = w / visibleCandles.size.coerceAtLeast(1)
                        val bodyWidth = (stepX * 0.62f).coerceIn(4f, 16f)

                        // Area glow under closes
                        val areaPath = Path().apply {
                            visibleCandles.forEachIndexed { idx, c ->
                                val cx = idx * stepX + stepX / 2f
                                val cy = priceToY(c.close)
                                if (idx == 0) moveTo(cx, cy) else lineTo(cx, cy)
                            }
                            lineTo((visibleCandles.lastIndex) * stepX + stepX / 2f, h)
                            lineTo(stepX / 2f, h)
                            close()
                        }
                        drawPath(
                            path = areaPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    CyberCyan.copy(alpha = 0.12f),
                                    Color.Transparent
                                )
                            )
                        )

                        // Candlesticks
                        visibleCandles.forEachIndexed { idx, candle ->
                            val cx = idx * stepX + stepX / 2f
                            val yOpen = priceToY(candle.open)
                            val yClose = priceToY(candle.close)
                            val yHigh = priceToY(candle.high)
                            val yLow = priceToY(candle.low)
                            val color = if (candle.isBullish) EmeraldCall else CrimsonPut

                            // Wick
                            drawLine(
                                color = color.copy(alpha = 0.85f),
                                start = Offset(cx, yHigh),
                                end = Offset(cx, yLow),
                                strokeWidth = 2f,
                                cap = StrokeCap.Round
                            )

                            // Body
                            val top = min(yOpen, yClose)
                            val bodyHeight = abs(yOpen - yClose).coerceAtLeast(3f)
                            drawRect(
                                color = color,
                                topLeft = Offset(cx - bodyWidth / 2f, top),
                                size = Size(bodyWidth, bodyHeight)
                            )
                        }

                        // EMA 9 and EMA 21 curves
                        if (ema9Series.size > 1) {
                            val ema9Path = Path()
                            ema9Series.forEachIndexed { idx, v ->
                                val cx = idx * stepX + stepX / 2f
                                val cy = priceToY(v)
                                if (idx == 0) ema9Path.moveTo(cx, cy) else ema9Path.lineTo(cx, cy)
                            }
                            drawPath(
                                path = ema9Path,
                                color = CyberCyan,
                                style = Stroke(width = 2.4f, cap = StrokeCap.Round)
                            )
                        }
                        if (ema21Series.size > 1) {
                            val ema21Path = Path()
                            ema21Series.forEachIndexed { idx, v ->
                                val cx = idx * stepX + stepX / 2f
                                val cy = priceToY(v)
                                if (idx == 0) ema21Path.moveTo(cx, cy) else ema21Path.lineTo(cx, cy)
                            }
                            drawPath(
                                path = ema21Path,
                                color = SolarAmber,
                                style = Stroke(width = 2.2f, cap = StrokeCap.Round)
                            )
                        }

                        // Active trade strike lines
                        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                        openPositions.forEach { pos ->
                            val strikeY = priceToY(pos.entryPrice)
                            val strikeColor = if (pos.direction == TradeDirection.CALL) EmeraldCall else CrimsonPut
                            drawLine(
                                color = strikeColor,
                                start = Offset(0f, strikeY),
                                end = Offset(w, strikeY),
                                strokeWidth = 2.5f,
                                pathEffect = dashEffect
                            )
                            drawCircle(
                                color = strikeColor,
                                radius = 5f,
                                center = Offset(w - 8f, strikeY)
                            )
                        }

                        // Live current price dashed line & beacon
                        val curY = priceToY(currentPrice)
                        drawLine(
                            color = TextPrimary.copy(alpha = 0.65f),
                            start = Offset(0f, curY),
                            end = Offset(w, curY),
                            strokeWidth = 1.5f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                        )
                        drawCircle(
                            color = EmeraldCall.copy(alpha = 0.25f),
                            radius = 10f,
                            center = Offset(w - stepX / 2f, curY)
                        )
                        drawCircle(
                            color = EmeraldCall,
                            radius = 4.5f,
                            center = Offset(w - stepX / 2f, curY)
                        )

                        // Crosshair vertical cursor
                        selectedIndex?.let { idx ->
                            val cx = idx * stepX + stepX / 2f
                            drawLine(
                                color = TextSecondary.copy(alpha = 0.7f),
                                start = Offset(cx, 0f),
                                end = Offset(cx, h),
                                strokeWidth = 1.5f
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Right-side price scale column
                Column(
                    modifier = Modifier
                        .width(62.dp)
                        .height(215.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = fmt(paddedMax, decimals),
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                    Text(
                        text = fmt((paddedMax + paddedMin) / 2.0, decimals),
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(ObsidianBg)
                            .border(1.dp, EmeraldCall.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = fmt(currentPrice, decimals),
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = EmeraldCall
                        )
                    }
                    Text(
                        text = fmt(paddedMin, decimals),
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier
                .width(8.dp)
                .height(8.dp)
                .clip(RoundedCornerShape(50))
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
    }
}

private fun fmt(value: Double, decimals: Int): String =
    String.format(Locale.US, "%.${decimals}f", value)
