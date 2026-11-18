package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.notification.NotificationHelper
import com.example.ui.navigation.AppTab
import com.example.ui.navigation.FloatingNavigationChrome
import com.example.ui.navigation.HomeRoute
import com.example.ui.navigation.ImportHelpRoute
import com.example.ui.navigation.LogsRoute
import com.example.ui.navigation.SettingsRoute
import com.example.ui.screens.CredentialImportSheet
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ImportHelpScreen
import com.example.ui.screens.LogsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.CloudCheckinTheme
import com.example.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

  private val viewModel: MainViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    handleAlertIntent(intent)

    setContent {
      val config by viewModel.config.collectAsStateWithLifecycle()

      CloudCheckinTheme(
        themePreference = config.themePreference,
        dynamicColor = config.dynamicColorEnabled,
      ) {
        CloudCheckinRootContent(viewModel = viewModel)
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleAlertIntent(intent)
  }

  private fun handleAlertIntent(intent: Intent?) {
    val action = intent?.getStringExtra(NotificationHelper.EXTRA_ALERT_ACTION)
    val accountId = intent?.getStringExtra(NotificationHelper.EXTRA_ALERT_ACCOUNT_ID)
    if (action == NotificationHelper.ACTION_OPEN_REPAIR && !accountId.isNullOrBlank()) {
      viewModel.openImportSheet(repairAccountId = accountId)
    }
  }
}

@Composable
private fun CloudCheckinRootContent(viewModel: MainViewModel) {
  val accounts by viewModel.accounts.collectAsStateWithLifecycle()
  val config by viewModel.config.collectAsStateWithLifecycle()
  val logs by viewModel.logs.collectAsStateWithLifecycle()
  val isBatchRunning by viewModel.isBatchRunning.collectAsStateWithLifecycle()
  val isProbingRoot by viewModel.isProbingRoot.collectAsStateWithLifecycle()
  val lastRootProbe by viewModel.lastRootProbe.collectAsStateWithLifecycle()
  val isScanningRoot by viewModel.isScanningRoot.collectAsStateWithLifecycle()
  val rootScanResults by viewModel.rootScanResults.collectAsStateWithLifecycle()
  val clipboardResult by viewModel.clipboardParseResult.collectAsStateWithLifecycle()
  val importSheetTarget by viewModel.importSheetTarget.collectAsStateWithLifecycle()
  val permissionStatus by viewModel.permissionStatus.collectAsStateWithLifecycle()

  val snackbarHostState = remember { SnackbarHostState() }
  val navController = rememberNavController()
  val backStackEntry by navController.currentBackStackEntryAsState()
  val currentDestination = backStackEntry?.destination

  val isHelpScreen = currentDestination?.hasRoute<ImportHelpRoute>() == true
  val selectedTab = when {
    currentDestination?.hasRoute<LogsRoute>() == true -> AppTab.LOGS
    currentDestination?.hasRoute<SettingsRoute>() == true -> AppTab.SETTINGS
    else -> AppTab.HOME
  }

  LaunchedEffect(Unit) {
    viewModel.snackbarEvents.collect { msg ->
      snackbarHostState.showSnackbar(msg)
    }
  }

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    snackbarHost = {
      SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier
          .navigationBarsPadding()
          .padding(bottom = if (selectedTab == AppTab.HOME) 152.dp else 88.dp),
      )
    },
  ) { _ ->
    // Screen content handles top statusBarsPadding and bottom 104.dp padding for floating nav clearance
    Box(modifier = Modifier.fillMaxSize()) {
      NavHost(
        navController = navController,
        startDestination = HomeRoute,
        modifier = Modifier.fillMaxSize(),
      ) {
        composable<HomeRoute> {
          HomeScreen(
            accounts = accounts,
            config = config,
            onAddAccountClick = { viewModel.openImportSheet() },
            onSingleCheckinClick = { id -> viewModel.runSingleCheckin(id) },
            onRepairCredentialClick = { id -> viewModel.openImportSheet(repairAccountId = id) },
            onResumeFromCapClick = { id -> viewModel.resumeAccountFromCap(id) },
            onToggleEnabledClick = { id -> viewModel.toggleAccountEnabled(id) },
            onDeleteAccountClick = { id -> viewModel.deleteAccount(id) },
          )
        }

        composable<LogsRoute> {
          LogsScreen(
            logs = logs,
            onBuildDiagnosticText = { viewModel.buildSanitizedDiagnosticText() },
            onClearLogs = { viewModel.clearLogs() },
            onCopiedMessage = { msg -> viewModel.showMessage(msg) },
          )
        }

        composable<SettingsRoute> {
          SettingsScreen(
            config = config,
            isProbingRoot = isProbingRoot,
            lastRootProbe = lastRootProbe,
            permissionStatus = permissionStatus,
            onRefreshPermissions = { viewModel.refreshSystemPermissions() },
            onSwitchPermissionMode = { mode -> viewModel.switchPermissionMode(mode) },
            onUpdateScheduleTime = { h, m -> viewModel.updateScheduleTime(h, m) },
            onUpdateJitterMinutes = { j -> viewModel.updateJitterMinutes(j) },
            onUpdateMaxFreeMinutes = { cap -> viewModel.updateMaxFreeMinutes(cap) },
            onToggleAutoBackupRenewal = { enabled -> viewModel.toggleAutoBackupRenewal(enabled) },
            onToggleNotifications = { enabled -> viewModel.toggleNotifications(enabled) },
            onToggleDynamicColor = { enabled -> viewModel.toggleDynamicColor(enabled) },
            onUpdateThemePreference = { pref -> viewModel.updateThemePreference(pref) },
            onOpenImportHelp = { navController.navigate(ImportHelpRoute) },
          )
        }

        composable<ImportHelpRoute> {
          ImportHelpScreen(
            onBackClick = { navController.popBackStack() },
          )
        }
      }

      if (!isHelpScreen) {
        FloatingNavigationChrome(
          selectedTab = selectedTab,
          onSelectTab = { tab ->
            val targetRoute: Any = when (tab) {
              AppTab.HOME -> HomeRoute
              AppTab.LOGS -> LogsRoute
              AppTab.SETTINGS -> SettingsRoute
            }
            navController.navigate(targetRoute) {
              popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
              }
              launchSingleTop = true
              restoreState = true
            }
          },
          showBatchFab = selectedTab == AppTab.HOME,
          isBatchRunning = isBatchRunning,
          onBatchCheckinClick = { viewModel.runCheckinAll() },
          modifier = Modifier.align(Alignment.BottomCenter),
        )
      }
    }
  }

  CredentialImportSheet(
    target = importSheetTarget,
    permissionMode = config.permissionMode,
    isScanningRoot = isScanningRoot,
    rootScanResults = rootScanResults,
    clipboardResult = clipboardResult,
    onDismiss = { viewModel.closeImportSheet() },
    onOpenHelpClick = {
      viewModel.closeImportSheet()
      navController.navigate(ImportHelpRoute)
    },
    onScanRootClick = { viewModel.scanRootInstalledGames() },
    onConfirmRootImport = { scan, alias -> viewModel.importFromRootScan(scan, alias) },
    onWebViewCaptured = { captured, alias, game ->
      viewModel.importFromWebView(captured, alias, game)
    },
    onParseClipboardClick = { viewModel.parseClipboardForZzz() },
    onParseManualText = { raw -> viewModel.parseManualHeaderText(raw) },
    onConfirmClipboardImport = { alias, game -> viewModel.confirmClipboardImport(alias, game) },
  )
}
