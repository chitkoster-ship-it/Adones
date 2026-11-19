package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CandlestickChart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.CandlestickChart
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.navigation.AppDestination
import com.example.ui.screens.JournalScreen
import com.example.ui.screens.MonitorScreen
import com.example.ui.screens.StrategiesScreen
import com.example.ui.screens.TerminalScreen
import com.example.ui.theme.EmeraldCall
import com.example.ui.theme.EmeraldCallDim
import com.example.ui.theme.IqTradingBotTheme
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.TradingBotViewModel

private data class NavTab(
    val route: AppDestination,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val navTabs = listOf(
    NavTab(
        route = AppDestination.Terminal,
        labelRes = R.string.nav_terminal,
        selectedIcon = Icons.Filled.CandlestickChart,
        unselectedIcon = Icons.Outlined.CandlestickChart,
        testTag = "nav_tab_terminal"
    ),
    NavTab(
        route = AppDestination.Strategies,
        labelRes = R.string.nav_strategies,
        selectedIcon = Icons.Filled.Tune,
        unselectedIcon = Icons.Outlined.Tune,
        testTag = "nav_tab_strategies"
    ),
    NavTab(
        route = AppDestination.Journal,
        labelRes = R.string.nav_history,
        selectedIcon = Icons.Filled.History,
        unselectedIcon = Icons.Outlined.History,
        testTag = "nav_tab_journal"
    ),
    NavTab(
        route = AppDestination.Monitor,
        labelRes = R.string.nav_monitor,
        selectedIcon = Icons.Filled.Terminal,
        unselectedIcon = Icons.Outlined.Terminal,
        testTag = "nav_tab_monitor"
    )
)

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as IqTradingBotApplication

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val useNavigationRail = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact
            val botViewModel: TradingBotViewModel = viewModel(
                factory = TradingBotViewModel.Factory(app.container.tradingRepository)
            )

            IqTradingBotTheme {
                IqTradingBotShell(
                    viewModel = botViewModel,
                    useNavigationRail = useNavigationRail
                )
            }
        }
    }
}

@Composable
private fun IqTradingBotShell(
    viewModel: TradingBotViewModel,
    useNavigationRail: Boolean
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val terminalState by viewModel.uiState.collectAsStateWithLifecycle()
    val tradeHistory by viewModel.tradeHistory.collectAsStateWithLifecycle()
    val presets by viewModel.strategyPresets.collectAsStateWithLifecycle()

    fun navigateToTab(route: AppDestination) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = ObsidianBg,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            if (!useNavigationRail) {
                NavigationBar(
                    containerColor = ObsidianSurface,
                    contentColor = TextPrimary
                ) {
                    navTabs.forEach { tab ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.hasRoute(tab.route::class)
                        } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navigateToTab(tab.route) },
                            icon = {
                                Icon(
                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = stringResource(tab.labelRes)
                                )
                            },
                            label = { Text(stringResource(tab.labelRes)) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = EmeraldCall,
                                selectedTextColor = EmeraldCall,
                                indicatorColor = EmeraldCallDim,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextSecondary
                            ),
                            modifier = Modifier.testTag(tab.testTag)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(ObsidianBg)
        ) {
            if (useNavigationRail) {
                NavigationRail(
                    containerColor = ObsidianSurface,
                    contentColor = TextPrimary,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    navTabs.forEach { tab ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.hasRoute(tab.route::class)
                        } == true
                        NavigationRailItem(
                            selected = selected,
                            onClick = { navigateToTab(tab.route) },
                            icon = {
                                Icon(
                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = stringResource(tab.labelRes)
                                )
                            },
                            label = { Text(stringResource(tab.labelRes)) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = EmeraldCall,
                                selectedTextColor = EmeraldCall,
                                indicatorColor = EmeraldCallDim,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextSecondary
                            ),
                            modifier = Modifier.testTag(tab.testTag)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                NavHost(
                    navController = navController,
                    startDestination = AppDestination.Terminal,
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 840.dp)
                ) {
                    composable<AppDestination.Terminal> {
                        TerminalScreen(
                            state = terminalState,
                            onSelectAsset = viewModel::selectAsset,
                            onStartBot = viewModel::startBot,
                            onStopBot = viewModel::stopBot,
                            onManualTrade = viewModel::placeManualTrade,
                            onDismissBanner = viewModel::dismissBanner,
                            onOpenStrategies = { navigateToTab(AppDestination.Strategies) }
                        )
                    }
                    composable<AppDestination.Strategies> {
                        StrategiesScreen(
                            config = terminalState.botConfig,
                            activePresetName = terminalState.activePresetName,
                            presets = presets,
                            onUpdateConfig = viewModel::updateBotConfig,
                            onSavePreset = viewModel::saveStrategyPreset,
                            onApplyPreset = viewModel::applyPreset,
                            onDeletePreset = viewModel::deletePreset
                        )
                    }
                    composable<AppDestination.Journal> {
                        JournalScreen(trades = tradeHistory)
                    }
                    composable<AppDestination.Monitor> {
                        MonitorScreen(
                            practiceBalance = terminalState.practiceBalance,
                            logs = terminalState.logs,
                            onResetAccount = viewModel::resetPracticeAccount,
                            onClearHistory = viewModel::clearTradeHistory
                        )
                    }
                }
            }
        }
    }
}
