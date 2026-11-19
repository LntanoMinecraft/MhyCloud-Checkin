package com.example.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class EncryptedStorageRepository(context: Context) {

  private val appContext = context.applicationContext

  private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
  }

  private val prefs: SharedPreferences by lazy {
    val masterKey = MasterKey.Builder(appContext)
      .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
      .build()
    EncryptedSharedPreferences.create(
      appContext,
      PREFS_FILE_NAME,
      masterKey,
      EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
      EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )
  }

  private val lock = Any()

  private val _accounts = MutableStateFlow<List<AccountWithState>>(emptyList())
  val accounts: StateFlow<List<AccountWithState>> = _accounts.asStateFlow()

  private val _config = MutableStateFlow(AppGlobalConfig())
  val config: StateFlow<AppGlobalConfig> = _config.asStateFlow()

  private val _logs = MutableStateFlow<List<CheckinLogEntry>>(emptyList())
  val logs: StateFlow<List<CheckinLogEntry>> = _logs.asStateFlow()

  init {
    loadAll()
    performMidnightResetIfNeeded()
  }

  private fun loadAll() {
    synchronized(lock) {
      val profiles = decodeList<AccountProfile>(prefs.getString(KEY_ACCOUNTS, null))
      val states = decodeList<CheckinState>(prefs.getString(KEY_STATES, null)).associateBy { it.accountId }
      val merged = profiles.map { profile ->
        AccountWithState(
          profile = profile,
          state = states[profile.accountId] ?: CheckinState(accountId = profile.accountId),
        )
      }
      _accounts.value = merged

      val cfgStr = prefs.getString(KEY_CONFIG, null)
      _config.value = if (cfgStr.isNullOrBlank()) {
        AppGlobalConfig()
      } else {
        val decoded = runCatching { json.decodeFromString<AppGlobalConfig>(cfgStr) }.getOrDefault(AppGlobalConfig())
        if (!cfgStr.contains("\"useDynamicColor\"") && cfgStr.contains("\"dynamicColorEnabled\":false")) {
          decoded.copy(useDynamicColor = false)
        } else {
          decoded
        }
      }

      _logs.value = decodeList<CheckinLogEntry>(prefs.getString(KEY_LOGS, null))
        .sortedByDescending { it.timestamp }
        .take(MAX_LOG_COUNT)
    }
  }

  private inline fun <reified T> decodeList(raw: String?): List<T> {
    if (raw.isNullOrBlank()) return emptyList()
    return runCatching { json.decodeFromString<List<T>>(raw) }.getOrDefault(emptyList())
  }

  private fun persistAccountsAndStatesLocked(items: List<AccountWithState>) {
    val profiles = items.map { it.profile }
    val states = items.map { it.state }
    prefs.edit()
      .putString(KEY_ACCOUNTS, json.encodeToString(profiles))
      .putString(KEY_STATES, json.encodeToString(states))
      .apply()
    _accounts.value = items
  }

  fun getGameLogicalDate(): String = Companion.getGameLogicalDate()

  fun currentBeijingDate(): String = getGameLogicalDate()

  /**
   * Cross-day reset evaluated strictly by game logical day (04:00 Beijing Time, UTC+8).
   * Only DONE_TODAY accounts from a previous logical day reset to READY;
   * PAUSED_CAP_REACHED and NEED_REAUTH never auto-reset.
   */
  fun performMidnightResetIfNeeded(): Boolean {
    val logicalToday = getGameLogicalDate()
    var changed = false
    synchronized(lock) {
      val updated = _accounts.value.map { item ->
        val state = item.state
        if (state.status == AccountStatus.DONE_TODAY && state.lastCheckinDate != logicalToday) {
          changed = true
          item.copy(
            state = state.copy(
              status = AccountStatus.READY,
              lastDeltaMinutes = 0,
            ),
          )
        } else {
          item
        }
      }
      if (changed) {
        persistAccountsAndStatesLocked(updated)
      }
    }
    return changed
  }

  fun upsertAccount(
    profile: AccountProfile,
    initialStatus: AccountStatus = AccountStatus.READY,
  ): AccountWithState {
    synchronized(lock) {
      val current = _accounts.value.toMutableList()
      val existingIndex = current.indexOfFirst { it.profile.accountId == profile.accountId }
      val result = if (existingIndex >= 0) {
        val existing = current[existingIndex]
        val nextState = existing.state.copy(
          status = initialStatus,
          failCount = 0,
          lastErrorMsg = "",
        )
        AccountWithState(profile = profile, state = nextState).also {
          current[existingIndex] = it
        }
      } else {
        AccountWithState(
          profile = profile,
          state = CheckinState(accountId = profile.accountId, status = initialStatus),
        ).also {
          current.add(it)
        }
      }
      persistAccountsAndStatesLocked(current)
      return result
    }
  }

  fun updateAccountProfile(accountId: String, transform: (AccountProfile) -> AccountProfile) {
    synchronized(lock) {
      val updated = _accounts.value.map { item ->
        if (item.profile.accountId == accountId) {
          item.copy(profile = transform(item.profile))
        } else {
          item
        }
      }
      persistAccountsAndStatesLocked(updated)
    }
  }

  fun updateAccountState(accountId: String, transform: (CheckinState) -> CheckinState) {
    synchronized(lock) {
      val updated = _accounts.value.map { item ->
        if (item.profile.accountId == accountId) {
          item.copy(state = transform(item.state))
        } else {
          item
        }
      }
      persistAccountsAndStatesLocked(updated)
    }
  }

  fun updateAccountAndState(
    accountId: String,
    transformProfile: (AccountProfile) -> AccountProfile,
    transformState: (CheckinState) -> CheckinState,
  ) {
    synchronized(lock) {
      val updated = _accounts.value.map { item ->
        if (item.profile.accountId == accountId) {
          AccountWithState(
            profile = transformProfile(item.profile),
            state = transformState(item.state),
          )
        } else {
          item
        }
      }
      persistAccountsAndStatesLocked(updated)
    }
  }

  fun resumeAccountFromCap(accountId: String) {
    synchronized(lock) {
      val logicalToday = getGameLogicalDate()
      val updated = _accounts.value.map { item ->
        if (item.profile.accountId == accountId) {
          val nextStatus = if (item.state.lastCheckinDate == logicalToday) {
            AccountStatus.DONE_TODAY
          } else {
            AccountStatus.READY
          }
          item.copy(
            profile = item.profile.copy(isEnabled = true),
            state = item.state.copy(status = nextStatus, lastErrorMsg = ""),
          )
        } else {
          item
        }
      }
      persistAccountsAndStatesLocked(updated)
    }
  }

  fun removeAccount(accountId: String) {
    synchronized(lock) {
      val updated = _accounts.value.filterNot { it.profile.accountId == accountId }
      persistAccountsAndStatesLocked(updated)
    }
  }

  fun getAccount(accountId: String): AccountWithState? {
    return _accounts.value.firstOrNull { it.profile.accountId == accountId }
  }

  fun hasAnyEnabledCheckinAccount(): Boolean {
    return _accounts.value.any {
      it.profile.isEnabled &&
        it.state.status != AccountStatus.PAUSED_CAP_REACHED &&
        it.state.status != AccountStatus.NEED_REAUTH
    }
  }

  fun updateConfig(transform: (AppGlobalConfig) -> AppGlobalConfig) {
    synchronized(lock) {
      val next = transform(_config.value)
      prefs.edit().putString(KEY_CONFIG, json.encodeToString(next)).apply()
      _config.value = next
    }
  }

  fun appendLog(entry: CheckinLogEntry) {
    synchronized(lock) {
      val sanitizedEntry = entry.copy(
        detailChain = SecuritySanitizer.sanitizeText(entry.detailChain),
      )
      val updated = (listOf(sanitizedEntry) + _logs.value)
        .sortedByDescending { it.timestamp }
        .take(MAX_LOG_COUNT)
      prefs.edit().putString(KEY_LOGS, json.encodeToString(updated)).apply()
      _logs.value = updated
    }
  }

  fun clearLogs() {
    synchronized(lock) {
      prefs.edit().remove(KEY_LOGS).apply()
      _logs.value = emptyList()
    }
  }

  fun exportSanitizedDiagnosticLogs(): String {
    val cfg = _config.value
    val header = buildString {
      appendLine("=== 云游戏自动签到助手 诊断日志 (已脱敏) ===")
      appendLine("权限模式: ${cfg.permissionMode.label} | 上限阈值: ${cfg.maxFreeMinutes} 分钟 | 备用续期: ${cfg.autoBackupRenewal}")
      appendLine("导出时间: ${Instant.now().atZone(BEIJING_ZONE).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))} (北京时间)")
      appendLine("账号数: ${_accounts.value.size} | 日志数: ${_logs.value.size}")
      appendLine()
    }
    val body = _logs.value.joinToString("\n\n") { log ->
      val timeStr = Instant.ofEpochMilli(log.timestamp)
        .atZone(BEIJING_ZONE)
        .format(DateTimeFormatter.ofPattern("MM-dd HH:mm:ss"))
      buildString {
        appendLine("[$timeStr] [${log.gameType.displayName}] [${log.accountAlias}] ${log.resultTitle} (${if (log.deltaMinutes >= 0) "+${log.deltaMinutes}" else log.deltaMinutes}m)")
        appendLine("  HTTP=${log.httpStatusCode ?: "-"} | 备用续期=${log.usedBackupRenewal} | 上限暂停=${log.pausedDueToCap}")
        appendLine("  链路: ${SecuritySanitizer.sanitizeText(log.detailChain)}")
      }
    }
    return header + body
  }

  fun newAccountId(gameType: GameType, uniqueSeed: String): String {
    val cleanSeed = uniqueSeed.filter { it.isLetterOrDigit() }.take(12)
    return if (cleanSeed.isNotBlank()) {
      "${gameType.shortTag.lowercase()}_$cleanSeed"
    } else {
      "${gameType.shortTag.lowercase()}_${UUID.randomUUID().toString().take(8)}"
    }
  }

  companion object {
    private const val PREFS_FILE_NAME = "cloud_checkin_secure_store"
    private const val KEY_ACCOUNTS = "accounts_json"
    private const val KEY_STATES = "states_json"
    private const val KEY_CONFIG = "config_json"
    private const val KEY_LOGS = "logs_json"
    const val MAX_LOG_COUNT = 200
    val BEIJING_ZONE: ZoneId = ZoneId.of("Asia/Shanghai")
    const val GAME_DAY_RESET_HOUR = 4

    /**
     * Game logical date based on Beijing Time (UTC+8) with 04:00 daily reset boundary.
     * Example: 2026-11-19 03:59 CST -> "2026-11-18"; 2026-11-19 04:00 CST -> "2026-11-19".
     */
    fun getGameLogicalDate(now: ZonedDateTime = ZonedDateTime.now(BEIJING_ZONE)): String {
      return now
        .withZoneSameInstant(BEIJING_ZONE)
        .minusHours(GAME_DAY_RESET_HOUR.toLong())
        .toLocalDate()
        .format(DateTimeFormatter.ISO_LOCAL_DATE)
    }
  }
}
