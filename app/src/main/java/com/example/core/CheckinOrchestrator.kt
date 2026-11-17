package com.example.core

import android.content.Context
import com.example.data.AccountStatus
import com.example.data.AccountWithState
import com.example.data.CheckinLogEntry
import com.example.data.CredentialSource
import com.example.data.EncryptedStorageRepository
import com.example.data.GameType
import com.example.data.PermissionMode
import com.example.notification.BatchAccountOutcome
import com.example.notification.NotificationHelper
import com.example.scheduler.ScheduleManager
import java.util.UUID
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class BatchCheckinResult(
  val outcomes: List<BatchAccountOutcome>,
  val hasTransientFailure: Boolean,
)

class CheckinOrchestrator(
  private val context: Context,
  private val storage: EncryptedStorageRepository,
  private val apiClient: CloudGameApiClient,
  private val scheduleManager: ScheduleManager,
) {

  private val mutex = Mutex()

  suspend fun runBatchCheckin(isScheduledRun: Boolean): BatchCheckinResult = mutex.withLock {
    storage.performMidnightResetIfNeeded()

    val targets = storage.accounts.value.filter { item ->
      item.profile.isEnabled &&
        item.state.status != AccountStatus.PAUSED_CAP_REACHED &&
        item.state.status != AccountStatus.NEED_REAUTH &&
        (!isScheduledRun || item.state.status != AccountStatus.DONE_TODAY)
    }

    if (targets.isEmpty()) {
      scheduleManager.syncScheduleWithAccounts(forceTomorrow = false)
      return BatchCheckinResult(outcomes = emptyList(), hasTransientFailure = false)
    }

    val outcomes = mutableListOf<BatchAccountOutcome>()
    var hasTransientFailure = false

    for (target in targets) {
      val single = executeForAccountInternal(target)
      outcomes.add(single.outcome)
      if (single.isTransientError) {
        hasTransientFailure = true
      }
    }

    // Re-evaluate schedules: cancels everything if all accounts are now paused/disabled
    scheduleManager.syncScheduleWithAccounts(forceTomorrow = false)

    val cfg = storage.config.value
    if (isScheduledRun && cfg.notificationsEnabled && outcomes.isNotEmpty()) {
      NotificationHelper.showSummaryNotification(context, outcomes)
    }

    BatchCheckinResult(outcomes = outcomes, hasTransientFailure = hasTransientFailure)
  }

  suspend fun runSingleAccountCheckin(accountId: String): BatchAccountOutcome? = mutex.withLock {
    storage.performMidnightResetIfNeeded()
    val target = storage.getAccount(accountId) ?: return null
    val result = executeForAccountInternal(target)
    scheduleManager.syncScheduleWithAccounts(forceTomorrow = false)
    return result.outcome
  }

  private data class InternalExecutionResult(
    val outcome: BatchAccountOutcome,
    val isTransientError: Boolean,
  )

  private suspend fun executeForAccountInternal(item: AccountWithState): InternalExecutionResult {
    var profile = item.profile
    val prevState = item.state
    val cfg = storage.config.value
    val todayBeijing = storage.currentBeijingDate()
    val chainBuilder = StringBuilder()
    var usedBackupRenewal = false

    storage.updateAccountState(profile.accountId) {
      it.copy(status = AccountStatus.CHECKING, lastAttemptAt = System.currentTimeMillis())
    }

    chainBuilder.append("1. 主方案请求 ${profile.gameType.walletEndpoint} (来源=${profile.credentialSource.label})。")
    var response = apiClient.performWalletCheckin(profile)

    if (response is WalletCheckinResponse.AuthExpired) {
      chainBuilder.append("\n2. 主方案鉴权失效 (HTTP=${response.httpCode}, retcode=${response.retcode}: ${response.message})。")

      if (cfg.autoBackupRenewal) {
        val refreshedProfile = attemptBackupCredentialRefresh(profile, cfg.permissionMode, chainBuilder)
        if (refreshedProfile != null) {
          usedBackupRenewal = true
          profile = refreshedProfile
          chainBuilder.append("\n4. 备用凭证已刷新，正在执行第 1 次补签...")
          response = apiClient.performWalletCheckin(profile)
        } else {
          chainBuilder.append("\n4. 备用续期未获取到有效新凭证。")
        }
      } else {
        chainBuilder.append("\n2b. 自动备用续期已关闭。")
      }
    }

    return when (val finalResp = response) {
      is WalletCheckinResponse.Success -> {
        val computedDelta = when {
          finalResp.sendFreeMinutes > 0 -> finalResp.sendFreeMinutes
          prevState.freeMinutes > 0 && finalResp.freeMinutes > prevState.freeMinutes ->
            finalResp.freeMinutes - prevState.freeMinutes
          else -> 0
        }

        val reachedCap = finalResp.freeMinutes >= cfg.maxFreeMinutes
        val nextStatus = if (reachedCap) {
          AccountStatus.PAUSED_CAP_REACHED
        } else {
          AccountStatus.DONE_TODAY
        }

        chainBuilder.append(
          "\n✔ 签到/同步成功 (HTTP=${finalResp.httpCode})：免费时长 ${finalResp.freeMinutes} 分钟，总时长 ${finalResp.totalMinutes} 分钟，本次变化 +$computedDelta 分钟。",
        )
        if (reachedCap) {
          chainBuilder.append("\n⚠ 免费时长已达到上限阈值 (${cfg.maxFreeMinutes} 分钟)，已自动关闭调度并暂停该账号。")
        }

        storage.updateAccountAndState(
          accountId = profile.accountId,
          transformProfile = {
            if (reachedCap) profile.copy(isEnabled = false) else profile
          },
          transformState = {
            it.copy(
              status = nextStatus,
              lastCheckinDate = todayBeijing,
              lastAttemptAt = System.currentTimeMillis(),
              freeMinutes = finalResp.freeMinutes,
              totalMinutes = finalResp.totalMinutes,
              failCount = 0,
              lastErrorMsg = "",
              lastDeltaMinutes = computedDelta,
            )
          },
        )

        storage.appendLog(
          CheckinLogEntry(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            gameType = profile.gameType,
            accountAlias = profile.alias,
            accountId = profile.accountId,
            resultTitle = if (reachedCap) "已达上限 · 自动暂停" else "签到成功",
            isError = false,
            deltaMinutes = computedDelta,
            httpStatusCode = finalResp.httpCode,
            detailChain = chainBuilder.toString(),
            usedBackupRenewal = usedBackupRenewal,
            pausedDueToCap = reachedCap,
          ),
        )

        if (reachedCap && cfg.notificationsEnabled) {
          NotificationHelper.showAlertNotification(
            context = context,
            accountId = profile.accountId,
            title = "${profile.gameType.displayName}·${profile.alias} 已达时长上限",
            message = "当前剩余免费时长 ${finalResp.freeMinutes} 分钟，已达到阈值 ${cfg.maxFreeMinutes} 分钟，系统已自动暂停该账号定时签到。",
            isReauthRequired = false,
          )
        }

        InternalExecutionResult(
          outcome = BatchAccountOutcome(
            accountId = profile.accountId,
            gameType = profile.gameType,
            alias = profile.alias,
            status = nextStatus,
            deltaMinutes = computedDelta,
            freeMinutes = finalResp.freeMinutes,
            message = if (reachedCap) "已达上限自动暂停" else "签到成功",
          ),
          isTransientError = false,
        )
      }

      is WalletCheckinResponse.AuthExpired -> {
        chainBuilder.append("\n✖ 熔断：凭证彻底失效 (HTTP=${finalResp.httpCode}, retcode=${finalResp.retcode})，已停止自动重试。")
        storage.updateAccountState(profile.accountId) {
          it.copy(
            status = AccountStatus.NEED_REAUTH,
            lastAttemptAt = System.currentTimeMillis(),
            failCount = it.failCount + 1,
            lastErrorMsg = "凭证失效 (${finalResp.message})",
          )
        }

        storage.appendLog(
          CheckinLogEntry(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            gameType = profile.gameType,
            accountAlias = profile.alias,
            accountId = profile.accountId,
            resultTitle = "凭证失效 · 需更新",
            isError = true,
            deltaMinutes = 0,
            httpStatusCode = finalResp.httpCode,
            detailChain = chainBuilder.toString(),
            usedBackupRenewal = usedBackupRenewal,
            pausedDueToCap = false,
          ),
        )

        if (cfg.notificationsEnabled) {
          NotificationHelper.showAlertNotification(
            context = context,
            accountId = profile.accountId,
            title = "${profile.gameType.displayName}·${profile.alias} 凭证已失效",
            message = "自动备用续期未能恢复凭证，请点击通知更新登录凭证。",
            isReauthRequired = true,
          )
        }

        InternalExecutionResult(
          outcome = BatchAccountOutcome(
            accountId = profile.accountId,
            gameType = profile.gameType,
            alias = profile.alias,
            status = AccountStatus.NEED_REAUTH,
            deltaMinutes = 0,
            freeMinutes = prevState.freeMinutes,
            message = finalResp.message,
          ),
          isTransientError = false,
        )
      }

      is WalletCheckinResponse.TransientError -> {
        chainBuilder.append("\n⏳ 瞬态网络/服务异常 (${finalResp.message})，保持 READY 状态等待退避重试。")
        storage.updateAccountState(profile.accountId) {
          it.copy(
            status = AccountStatus.READY,
            lastAttemptAt = System.currentTimeMillis(),
            failCount = it.failCount + 1,
            lastErrorMsg = finalResp.message,
          )
        }

        storage.appendLog(
          CheckinLogEntry(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            gameType = profile.gameType,
            accountAlias = profile.alias,
            accountId = profile.accountId,
            resultTitle = "网络异常 · 待重试",
            isError = true,
            deltaMinutes = 0,
            httpStatusCode = finalResp.httpCode,
            detailChain = chainBuilder.toString(),
            usedBackupRenewal = usedBackupRenewal,
            pausedDueToCap = false,
          ),
        )

        InternalExecutionResult(
          outcome = BatchAccountOutcome(
            accountId = profile.accountId,
            gameType = profile.gameType,
            alias = profile.alias,
            status = AccountStatus.READY,
            deltaMinutes = 0,
            freeMinutes = prevState.freeMinutes,
            message = finalResp.message,
          ),
          isTransientError = true,
        )
      }
    }
  }

  private suspend fun attemptBackupCredentialRefresh(
    profile: com.example.data.AccountProfile,
    permissionMode: PermissionMode,
    chainBuilder: StringBuilder,
  ): com.example.data.AccountProfile? {
    // Root mode or Root-sourced account: re-read official XML
    if (permissionMode == PermissionMode.ROOT || profile.credentialSource == CredentialSource.ROOT_READ) {
      chainBuilder.append("\n3. 触发 Root 备用续期：正在重新读取官方客户端 XML...")
      val scan = RootHelper.scanCloudGameAccount(context, profile.gameType)
      if (scan.found && scan.comboTokenHeader.isNotBlank()) {
        val updated = profile.copy(
          comboToken = scan.comboTokenHeader,
          deviceHeadersJson = scan.deviceHeadersJson,
          credentialSource = CredentialSource.ROOT_READ,
          maskedUid = scan.maskedOpenId,
          updatedAt = System.currentTimeMillis(),
        )
        storage.updateAccountProfile(profile.accountId) { updated }
        return updated
      } else {
        chainBuilder.append(" 失败 (${scan.errorReason})")
      }
    }

    // Basic mode Genshin: attempt off-screen WebView refresh using existing session cookies
    if (profile.gameType == GameType.GENSHIN && profile.hasWebviewSession) {
      chainBuilder.append("\n3. 触发 WebView 后台静默续期...")
      val captured = WebViewCredentialManager.trySilentRefreshGenshin(context)
      if (captured != null && captured.comboToken.isNotBlank()) {
        val updated = profile.copy(
          comboToken = captured.comboToken,
          deviceHeadersJson = captured.deviceHeadersJson,
          credentialSource = CredentialSource.WEBVIEW,
          hasWebviewSession = true,
          maskedUid = captured.maskedUid.ifBlank { profile.maskedUid },
          updatedAt = System.currentTimeMillis(),
        )
        storage.updateAccountProfile(profile.accountId) { updated }
        return updated
      } else {
        chainBuilder.append(" WebView 会话已失效或超时。")
      }
    }

    // Basic mode ZZZ (clipboard) has no silent refresh path
    if (profile.gameType == GameType.ZZZ && permissionMode == PermissionMode.BASIC) {
      chainBuilder.append("\n3. 基础模式下云·绝区零剪贴板凭证不支持静默续期。")
    }

    return null
  }
}
