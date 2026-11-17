package com.example.viewmodel

import android.app.Application
import android.os.PowerManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.CloudCheckinApp
import com.example.core.CapturedWebCredentials
import com.example.core.ClipboardCredentialParser
import com.example.core.ClipboardParseResult
import com.example.core.RootHelper
import com.example.core.RootProbeResult
import com.example.core.RootScanAccountResult
import com.example.data.AccountProfile
import com.example.data.AccountStatus
import com.example.data.AccountWithState
import com.example.data.AppGlobalConfig
import com.example.data.CheckinLogEntry
import com.example.data.CredentialSource
import com.example.data.GameType
import com.example.data.PermissionMode
import com.example.data.ThemePreference
import com.example.notification.NotificationHelper
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ImportSheetTarget(
  val isVisible: Boolean = false,
  val preselectedGame: GameType = GameType.GENSHIN,
  val repairAccountId: String? = null,
  val initialAlias: String = "",
)

data class SystemPermissionStatus(
  val notificationsGranted: Boolean = false,
  val batteryOptimizationIgnored: Boolean = false,
  val exactAlarmAllowed: Boolean = false,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

  private val app = application as CloudCheckinApp
  private val storage = app.container.storageRepository
  private val orchestrator = app.container.orchestrator
  private val scheduleManager = app.container.scheduleManager

  val accounts: StateFlow<List<AccountWithState>> = storage.accounts
  val config: StateFlow<AppGlobalConfig> = storage.config
  val logs: StateFlow<List<CheckinLogEntry>> = storage.logs

  private val _isBatchRunning = MutableStateFlow(false)
  val isBatchRunning: StateFlow<Boolean> = _isBatchRunning.asStateFlow()

  private val _isProbingRoot = MutableStateFlow(false)
  val isProbingRoot: StateFlow<Boolean> = _isProbingRoot.asStateFlow()

  private val _lastRootProbe = MutableStateFlow<RootProbeResult?>(null)
  val lastRootProbe: StateFlow<RootProbeResult?> = _lastRootProbe.asStateFlow()

  private val _isScanningRoot = MutableStateFlow(false)
  val isScanningRoot: StateFlow<Boolean> = _isScanningRoot.asStateFlow()

  private val _rootScanResults = MutableStateFlow<List<RootScanAccountResult>>(emptyList())
  val rootScanResults: StateFlow<List<RootScanAccountResult>> = _rootScanResults.asStateFlow()

  private val _clipboardParseResult = MutableStateFlow<ClipboardParseResult?>(null)
  val clipboardParseResult: StateFlow<ClipboardParseResult?> = _clipboardParseResult.asStateFlow()

  private val _importSheetTarget = MutableStateFlow(ImportSheetTarget())
  val importSheetTarget: StateFlow<ImportSheetTarget> = _importSheetTarget.asStateFlow()

  private val _permissionStatus = MutableStateFlow(SystemPermissionStatus())
  val permissionStatus: StateFlow<SystemPermissionStatus> = _permissionStatus.asStateFlow()

  private val _snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
  val snackbarEvents: SharedFlow<String> = _snackbarEvents.asSharedFlow()

  init {
    refreshSystemPermissions()
    storage.performMidnightResetIfNeeded()
    scheduleManager.syncScheduleWithAccounts(forceTomorrow = false)
  }

  fun refreshSystemPermissions() {
    val ctx = getApplication<Application>()
    val notifOk = NotificationHelper.hasNotificationPermission(ctx)
    val pm = ctx.getSystemService(PowerManager::class.java)
    val batteryOk = pm?.isIgnoringBatteryOptimizations(ctx.packageName) ?: false
    val exactAlarmOk = scheduleManager.canScheduleExactAlarms()
    _permissionStatus.value = SystemPermissionStatus(
      notificationsGranted = notifOk,
      batteryOptimizationIgnored = batteryOk,
      exactAlarmAllowed = exactAlarmOk,
    )
  }

  fun openImportSheet(
    gameType: GameType = GameType.GENSHIN,
    repairAccountId: String? = null,
  ) {
    val existing = repairAccountId?.let { storage.getAccount(it) }
    _clipboardParseResult.value = null
    _rootScanResults.value = emptyList()
    _importSheetTarget.value = ImportSheetTarget(
      isVisible = true,
      preselectedGame = existing?.profile?.gameType ?: gameType,
      repairAccountId = repairAccountId,
      initialAlias = existing?.profile?.alias.orEmpty(),
    )
  }

  fun closeImportSheet() {
    _importSheetTarget.value = ImportSheetTarget(isVisible = false)
  }

  fun runCheckinAll() {
    if (_isBatchRunning.value) return
    val enabledCount = accounts.value.count {
      it.profile.isEnabled &&
        it.state.status != AccountStatus.PAUSED_CAP_REACHED &&
        it.state.status != AccountStatus.NEED_REAUTH
    }
    if (enabledCount == 0) {
      _snackbarEvents.tryEmit("暂无可签到的已启用账号，请先添加或恢复账号")
      return
    }
    viewModelScope.launch {
      _isBatchRunning.value = true
      try {
        val result = orchestrator.runBatchCheckin(isScheduledRun = false)
        val okCount = result.outcomes.count {
          it.status == AccountStatus.DONE_TODAY || it.status == AccountStatus.PAUSED_CAP_REACHED
        }
        _snackbarEvents.tryEmit("执行完成：$okCount/${result.outcomes.size} 个账号同步成功")
      } finally {
        _isBatchRunning.value = false
      }
    }
  }

  fun runSingleCheckin(accountId: String) {
    viewModelScope.launch {
      val outcome = orchestrator.runSingleAccountCheckin(accountId)
      if (outcome != null) {
        _snackbarEvents.tryEmit("${outcome.gameType.displayName}·${outcome.alias}: ${outcome.message}")
      }
    }
  }

  fun resumeAccountFromCap(accountId: String) {
    storage.resumeAccountFromCap(accountId)
    scheduleManager.syncScheduleWithAccounts(forceTomorrow = false)
    _snackbarEvents.tryEmit("已恢复该账号的自动签到调度")
  }

  fun toggleAccountEnabled(accountId: String) {
    val item = storage.getAccount(accountId) ?: return
    val nextEnabled = !item.profile.isEnabled
    storage.updateAccountProfile(accountId) { it.copy(isEnabled = nextEnabled) }
    scheduleManager.syncScheduleWithAccounts(forceTomorrow = false)
  }

  fun deleteAccount(accountId: String) {
    storage.removeAccount(accountId)
    scheduleManager.syncScheduleWithAccounts(forceTomorrow = false)
    _snackbarEvents.tryEmit("已移除账号")
  }

  fun switchPermissionMode(targetMode: PermissionMode) {
    if (targetMode == PermissionMode.BASIC) {
      storage.updateConfig { it.copy(permissionMode = PermissionMode.BASIC) }
      _lastRootProbe.value = null
      return
    }

    if (_isProbingRoot.value) return
    viewModelScope.launch {
      _isProbingRoot.value = true
      try {
        val probe = RootHelper.probeRootAndApplyDoze(getApplication())
        _lastRootProbe.value = probe
        if (probe.isRooted) {
          storage.updateConfig { it.copy(permissionMode = PermissionMode.ROOT) }
          _snackbarEvents.tryEmit(probe.detailMessage)
        } else {
          // Bounce back to BASIC mode on failure
          storage.updateConfig { it.copy(permissionMode = PermissionMode.BASIC) }
          _snackbarEvents.tryEmit(probe.detailMessage)
        }
      } finally {
        _isProbingRoot.value = false
      }
    }
  }

  fun scanRootInstalledGames() {
    if (_isScanningRoot.value) return
    viewModelScope.launch {
      _isScanningRoot.value = true
      try {
        _rootScanResults.value = RootHelper.scanAllInstalledGames(getApplication())
      } finally {
        _isScanningRoot.value = false
      }
    }
  }

  fun importFromRootScan(scan: RootScanAccountResult, aliasInput: String) {
    if (!scan.found) return
    val targetRepairId = _importSheetTarget.value.repairAccountId
    val accountId = targetRepairId ?: storage.newAccountId(scan.gameType, scan.openId)
    val alias = aliasInput.trim().ifBlank {
      "${scan.gameType.displayName} (${scan.maskedOpenId})"
    }

    val profile = AccountProfile(
      accountId = accountId,
      gameType = scan.gameType,
      alias = alias,
      isEnabled = true,
      credentialSource = CredentialSource.ROOT_READ,
      comboToken = scan.comboTokenHeader,
      deviceHeadersJson = scan.deviceHeadersJson,
      hasWebviewSession = false,
      maskedUid = scan.maskedOpenId,
      updatedAt = System.currentTimeMillis(),
    )
    storage.upsertAccount(profile, AccountStatus.READY)
    scheduleManager.syncScheduleWithAccounts(forceTomorrow = false)
    closeImportSheet()
    _snackbarEvents.tryEmit("已通过 Root 直读同步 $alias")
  }

  fun importFromWebView(
    captured: CapturedWebCredentials,
    aliasInput: String,
    gameType: GameType = GameType.GENSHIN,
  ) {
    val targetRepairId = _importSheetTarget.value.repairAccountId
    val seed = captured.maskedUid.ifBlank { captured.comboToken.take(10) }
    val accountId = targetRepairId ?: storage.newAccountId(gameType, seed)
    val alias = aliasInput.trim().ifBlank {
      if (captured.maskedUid.isNotBlank()) "${gameType.displayName} (${captured.maskedUid})" else "${gameType.displayName} 账号"
    }

    val profile = AccountProfile(
      accountId = accountId,
      gameType = gameType,
      alias = alias,
      isEnabled = true,
      credentialSource = CredentialSource.WEBVIEW,
      comboToken = captured.comboToken,
      deviceHeadersJson = captured.deviceHeadersJson,
      hasWebviewSession = true,
      maskedUid = captured.maskedUid,
      updatedAt = System.currentTimeMillis(),
    )
    storage.upsertAccount(profile, AccountStatus.READY)
    scheduleManager.syncScheduleWithAccounts(forceTomorrow = false)
    closeImportSheet()
    _snackbarEvents.tryEmit("已捕获 WebView 登录凭证：$alias")
  }

  fun parseClipboardForZzz() {
    val result = ClipboardCredentialParser.readAndParseFromClipboard(getApplication())
    _clipboardParseResult.value = result
    if (result.success) {
      _snackbarEvents.tryEmit("凭证解析成功，已自动清理系统剪贴板")
    } else {
      _snackbarEvents.tryEmit(result.errorMessage)
    }
  }

  fun parseManualHeaderText(rawText: String) {
    val result = ClipboardCredentialParser.parseText(rawText)
    _clipboardParseResult.value = result
    if (!result.success) {
      _snackbarEvents.tryEmit(result.errorMessage)
    }
  }

  fun confirmClipboardImport(aliasInput: String, gameType: GameType) {
    val parsed = _clipboardParseResult.value ?: return
    if (!parsed.success) return

    val targetRepairId = _importSheetTarget.value.repairAccountId
    val openIdMatch = Regex("""oi=([^;]+)""").find(parsed.comboToken)?.groupValues?.getOrNull(1).orEmpty()
    val maskedUid = if (openIdMatch.isNotBlank()) {
      com.example.data.SecuritySanitizer.maskTokenValue(openIdMatch)
    } else {
      ""
    }
    val accountId = targetRepairId ?: storage.newAccountId(gameType, openIdMatch.ifBlank { parsed.comboToken.take(10) })
    val alias = aliasInput.trim().ifBlank {
      if (maskedUid.isNotBlank()) "${gameType.displayName} ($maskedUid)" else "${gameType.displayName} 账号"
    }

    val profile = AccountProfile(
      accountId = accountId,
      gameType = gameType,
      alias = alias,
      isEnabled = true,
      credentialSource = CredentialSource.CLIPBOARD,
      comboToken = parsed.comboToken,
      deviceHeadersJson = parsed.headersJson,
      hasWebviewSession = false,
      maskedUid = maskedUid,
      updatedAt = System.currentTimeMillis(),
    )
    storage.upsertAccount(profile, AccountStatus.READY)
    scheduleManager.syncScheduleWithAccounts(forceTomorrow = false)
    closeImportSheet()
    _snackbarEvents.tryEmit("已导入凭证：$alias")
  }

  fun updateScheduleTime(hour: Int, minute: Int) {
    storage.updateConfig { it.copy(scheduleHour = hour, scheduleMinute = minute) }
    scheduleManager.syncScheduleWithAccounts(forceTomorrow = false)
    _snackbarEvents.tryEmit("已更新每日基准时间并重新预约")
  }

  fun updateJitterMinutes(minutes: Int) {
    storage.updateConfig { it.copy(jitterMinutes = minutes.coerceIn(0, 30)) }
    scheduleManager.syncScheduleWithAccounts(forceTomorrow = false)
  }

  fun updateMaxFreeMinutes(minutes: Int) {
    storage.updateConfig { it.copy(maxFreeMinutes = minutes.coerceIn(300, 1200)) }
  }

  fun toggleAutoBackupRenewal(enabled: Boolean) {
    storage.updateConfig { it.copy(autoBackupRenewal = enabled) }
  }

  fun toggleNotifications(enabled: Boolean) {
    storage.updateConfig { it.copy(notificationsEnabled = enabled) }
  }

  fun toggleDynamicColor(enabled: Boolean) {
    storage.updateConfig { it.copy(dynamicColorEnabled = enabled) }
  }

  fun updateThemePreference(preference: ThemePreference) {
    storage.updateConfig { it.copy(themePreference = preference) }
  }

  fun clearLogs() {
    storage.clearLogs()
    _snackbarEvents.tryEmit("已清空运行日志")
  }

  fun buildSanitizedDiagnosticText(): String = storage.exportSanitizedDiagnosticLogs()

  fun showMessage(message: String) {
    _snackbarEvents.tryEmit(message)
  }
}
