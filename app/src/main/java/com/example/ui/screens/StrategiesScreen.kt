package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.StrategyPresetEntity
import com.example.domain.model.BotConfig
import com.example.domain.model.MoneyManagementMode
import com.example.domain.model.StrategyType
import com.example.ui.theme.BorderHairline
import com.example.ui.theme.CrimsonPut
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.EmeraldCall
import com.example.ui.theme.EmeraldCallDim
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateCardElevated
import com.example.ui.theme.SolarAmber
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun StrategiesScreen(
    config: BotConfig,
    activePresetName: String,
    presets: List<StrategyPresetEntity>,
    onUpdateConfig: ((BotConfig) -> BotConfig) -> Unit,
    onSavePreset: (String) -> Unit,
    onApplyPreset: (StrategyPresetEntity) -> Unit,
    onDeletePreset: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSaveDialog by remember { mutableStateOf(false) }
    var presetNameInput by remember { mutableStateOf("") }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            containerColor = SlateCardElevated,
            title = {
                Text("Save Strategy Preset", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            },
            text = {
                OutlinedTextField(
                    value = presetNameInput,
                    onValueChange = { presetNameInput = it },
                    label = { Text("Preset Name") },
                    placeholder = { Text("e.g. BTC Aggressive 30s") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("preset_name_input")
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (presetNameInput.isNotBlank()) {
                            onSavePreset(presetNameInput)
                            presetNameInput = ""
                            showSaveDialog = false
                        }
                    }
                ) {
                    Text("Save", color = EmeraldCall)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Strategy Studio Header with generated artwork
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(144.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.dp, BorderHairline, RoundedCornerShape(18.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_quant_banner_1795089629373),
                    contentDescription = "Quantitative strategy studio banner",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    ObsidianBg.copy(alpha = 0.35f),
                                    ObsidianBg.copy(alpha = 0.92f)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "STRATEGY ENGINE",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldCall
                    )
                    Text(
                        text = "Algorithm & Risk Guardrails",
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary
                    )
                    Text(
                        text = "Active Profile: $activePresetName",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

        // Strategy Selection Cards
        item {
            Text(
                text = "SIGNAL ALGORITHM",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StrategyType.entries.forEach { strategy ->
                    val selected = config.strategyType == strategy
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (selected) SlateCardElevated else SlateCard)
                            .border(
                                1.dp,
                                if (selected) EmeraldCall else BorderHairline,
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { onUpdateConfig { it.copy(strategyType = strategy) } }
                            .padding(14.dp)
                            .testTag("strategy_card_${strategy.name}")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = strategy.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (selected) EmeraldCall else TextPrimary
                            )
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = "Selected strategy",
                                    tint = EmeraldCall,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = strategy.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        // Execution Sizing & Expiry
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SlateCard)
                    .border(1.dp, BorderHairline, RoundedCornerShape(16.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "POSITION & SIGNAL PARAMETERS",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyberCyan
                )

                // Base Stake Slider
                ParameterSlider(
                    label = "Base Stake",
                    valueText = "$${String.format(Locale.US, "%.0f", config.baseStake)}",
                    value = config.baseStake.toFloat(),
                    range = 5f..250f,
                    onValueChange = { v ->
                        val rounded = ((v / 5f).roundToInt() * 5).toDouble().coerceAtLeast(5.0)
                        onUpdateConfig { it.copy(baseStake = rounded) }
                    }
                )

                // Min Confidence Slider
                ParameterSlider(
                    label = "Min Signal Confidence",
                    valueText = "${config.minConfidence}%",
                    value = config.minConfidence.toFloat(),
                    range = 50f..90f,
                    onValueChange = { v ->
                        onUpdateConfig { it.copy(minConfidence = v.roundToInt()) }
                    }
                )

                // Expiry Chips
                Text(
                    text = "Option Expiry Duration",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(15, 30, 60, 120).forEach { secs ->
                        val isSelected = config.expirySeconds == secs
                        FilterChip(
                            selected = isSelected,
                            onClick = { onUpdateConfig { it.copy(expirySeconds = secs) } },
                            label = {
                                Text(
                                    text = "${secs}s",
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldCallDim,
                                selectedLabelColor = EmeraldCall,
                                containerColor = ObsidianBg,
                                labelColor = TextSecondary
                            ),
                            modifier = Modifier.minimumInteractiveComponentSize()
                        )
                    }
                }
            }
        }

        // Money Management Mode
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SlateCard)
                    .border(1.dp, BorderHairline, RoundedCornerShape(16.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "MONEY MANAGEMENT",
                    style = MaterialTheme.typography.labelSmall,
                    color = SolarAmber
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MoneyManagementMode.entries.forEach { mode ->
                        val selected = config.moneyMode == mode
                        FilterChip(
                            selected = selected,
                            onClick = { onUpdateConfig { it.copy(moneyMode = mode) } },
                            label = { Text(mode.displayName.split(" ").first()) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldCallDim,
                                selectedLabelColor = EmeraldCall,
                                containerColor = ObsidianBg,
                                labelColor = TextSecondary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                        )
                    }
                }
                Text(
                    text = config.moneyMode.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                if (config.moneyMode != MoneyManagementMode.FLAT) {
                    ParameterSlider(
                        label = "Max Progression Steps",
                        valueText = "${config.maxSteps} steps",
                        value = config.maxSteps.toFloat(),
                        range = 1f..5f,
                        onValueChange = { v ->
                            onUpdateConfig { it.copy(maxSteps = v.roundToInt()) }
                        }
                    )
                }
                if (config.moneyMode == MoneyManagementMode.MARTINGALE) {
                    ParameterSlider(
                        label = "Martingale Multiplier",
                        valueText = String.format(Locale.US, "%.2fx", config.martingaleMultiplier),
                        value = config.martingaleMultiplier.toFloat(),
                        range = 1.5f..2.6f,
                        onValueChange = { v ->
                            val rounded = (v * 20).roundToInt() / 20.0
                            onUpdateConfig { it.copy(martingaleMultiplier = rounded) }
                        }
                    )
                }
            }
        }

        // Risk Guardrails
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SlateCard)
                    .border(1.dp, BorderHairline, RoundedCornerShape(16.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Security,
                        contentDescription = "Risk guardrails",
                        tint = CrimsonPut,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "AUTOMATED RISK GUARDRAILS",
                        style = MaterialTheme.typography.labelSmall,
                        color = CrimsonPut
                    )
                }

                ParameterSlider(
                    label = "Daily Take Profit",
                    valueText = "+$${String.format(Locale.US, "%.0f", config.dailyTakeProfit)}",
                    value = config.dailyTakeProfit.toFloat(),
                    range = 100f..2000f,
                    onValueChange = { v ->
                        val rounded = ((v / 50f).roundToInt() * 50).toDouble()
                        onUpdateConfig { it.copy(dailyTakeProfit = rounded) }
                    }
                )
                ParameterSlider(
                    label = "Daily Stop Loss",
                    valueText = "-$${String.format(Locale.US, "%.0f", config.dailyStopLoss)}",
                    value = config.dailyStopLoss.toFloat(),
                    range = 50f..1500f,
                    onValueChange = { v ->
                        val rounded = ((v / 25f).roundToInt() * 25).toDouble()
                        onUpdateConfig { it.copy(dailyStopLoss = rounded) }
                    }
                )
                ParameterSlider(
                    label = "Max Consecutive Losses",
                    valueText = "${config.maxConsecutiveLosses} losses",
                    value = config.maxConsecutiveLosses.toFloat(),
                    range = 2f..8f,
                    onValueChange = { v ->
                        onUpdateConfig { it.copy(maxConsecutiveLosses = v.roundToInt()) }
                    }
                )
            }
        }

        // Save Preset Action
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SAVED STRATEGY PRESETS",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
                Button(
                    onClick = { showSaveDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldCallDim,
                        contentColor = EmeraldCall
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("save_preset_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.BookmarkAdd,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Current", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        items(presets, key = { it.id }) { preset ->
            val isActive = preset.name == activePresetName
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isActive) SlateCardElevated else SlateCard)
                    .border(
                        1.dp,
                        if (isActive) CyberCyan else BorderHairline,
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { onApplyPreset(preset) }
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .testTag("preset_item_${preset.id}"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = preset.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (isActive) CyberCyan else TextPrimary
                    )
                    Text(
                        text = "${preset.strategyTypeName.replace("_", " ")} • $${preset.baseStake.toInt()} • ${preset.expirySeconds}s",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
                if (!preset.isDefault) {
                    IconButton(
                        onClick = { onDeletePreset(preset.id) },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = "Delete preset",
                            tint = CrimsonPut
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ParameterSlider(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Text(
                text = valueText,
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = TextPrimary
            )
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = EmeraldCall,
                activeTrackColor = EmeraldCall,
                inactiveTrackColor = ObsidianBg
            )
        )
    }
}
