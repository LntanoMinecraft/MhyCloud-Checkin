package com.example.core

import android.content.Context
import com.example.data.AccountProfile
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class BatchCheckinResult(
  val outcomes: List<BatchAccountOutcome>,
  val hasTransientFailure: Boolean,
  val skippedAlreadyDoneCount: Int = 0,
)

enum class CheckinVerdictType {
  SUCCESS_DURATION_INCREASED,
  SUCCESS_CAP_REACHED,
  SUCCESS_INITIAL_BASELINE,
  NO_INCREASE_KEEP_READY,
}

data class CheckinVerdict(
  val type: CheckinVerdictType,
  val nextStatus: AccountStatus,
  val computedDelta: Int,
  val shouldMarkLogicalDate: Boolean,
  val reachedCap: Boolean,
)

class CheckinOrchestrator(
  private val context: Context,
  private val storage: EncryptedStorageRepository,
  private val apiClient: CloudGameApiClient,
  private val scheduleManager: ScheduleManager,
  private val recheckDelayMillis: Long = DEFAULT_RECHECK_DELAY_MILLIS,
) {

  private val mutex = Mutex()

  suspend fun runBatchCheckin(isScheduledRun: Boolean): BatchCheckinResult = mutex.withLock {
    storage.performMidnightResetIfNeeded()
    val logicalToday = storage.getGameLogicalDate()

    val eligibleAccounts = storage.accounts.value.filter { item ->
      item.profile.isEnabled &&
        item.state.status != AccountStatus.PAUSED_CAP_REACHED &&
        item.state.status != AccountStatus.NEED_REAUTH
    }

    val (alreadyDoneToday, targets) = eligibleAccounts.partition { item ->
      item.state.status == AccountStatus.DONE_TODAY && item.state.lastCheckinDate == logicalToday
    }

    if (targets.isEmpty()) {
      scheduleManager.syncScheduleWithAccounts(forceTomorrow = false)
      return BatchCheckinResult(
        outcomes = emptyList(),
        hasTransientFailure = false,
        skippedAlreadyDoneCount = alreadyDoneToday.size,
      )
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
    val succeededOutcomes = outcomes.filter {
      it.status == AccountStatus.DONE_TODAY || it.status == AccountStatus.PAUSED_CAP_REACHED
    }
    if (isScheduledRun && cfg.notificationsEnabled && succeededOutcomes.isNotEmpty()) {
      NotificationHelper.showSummaryNotification(context, succeededOutcomes)
    }

    BatchCheckinResult(
      outcomes = outcomes,
      hasTransientFailure = hasTransientFailure,
      skippedAlreadyDoneCount = alreadyDoneToday.size,
    )
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
    val logicalToday = storage.getGameLogicalDate()
    val alreadyDoneLogicalToday =
      prevState.status == AccountStatus.DONE_TODAY && prevState.lastCheckinDate == logicalToday
    val hasBaselineRecord =
      prevState.lastCheckinDate.isNotBlank() || prevState.freeMinutes > 0 || prevState.totalMinutes > 0
    val oldFreeMinutes = prevState.freeMinutes

    val chainBuilder = StringBuilder()
    var usedBackupRenewal = false

    storage.updateAccountState(profile.accountId) {
      it.copy(status = AccountStatus.CHECKING, lastAttemptAt = System.currentTimeMillis())
    }

    chainBuilder.append("1. 主方案请求 ${profile.gameType.walletEndpoint} (来源=${profile.credentialSource.label}, 逻辑日=$logicalToday)。")
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

    return when (val firstResponse = response) {
      is WalletCheckinResponse.Success -> {
        handleWalletSuccess(
          profile = profile,
          prevState = prevState,
          firstResp = firstResponse,
          logicalToday = logicalToday,
          alreadyDoneLogicalToday = alreadyDoneLogicalToday,
          hasBaselineRecord = hasBaselineRecord,
          oldFreeMinutes = oldFreeMinutes,
          maxFreeMinutes = cfg.maxFreeMinutes,
          notificationsEnabled = cfg.notificationsEnabled,
          usedBackupRenewal = usedBackupRenewal,
          chainBuilder = chainBuilder,
        )
      }

      is WalletCheckinResponse.AuthExpired -> {
        handleAuthExpired(
          profile = profile,
          prevState = prevState,
          finalResp = firstResponse,
          notificationsEnabled = cfg.notificationsEnabled,
          usedBackupRenewal = usedBackupRenewal,
          chainBuilder = chainBuilder,
        )
      }

      is WalletCheckinResponse.TransientError -> {
        handleTransientError(
          profile = profile,
          prevState = prevState,
          finalResp = firstResponse,
          usedBackupRenewal = usedBackupRenewal,
          chainBuilder = chainBuilder,
        )
      }
    }
  }

  private suspend fun handleWalletSuccess(
    profile: AccountProfile,
    prevState: com.example.data.CheckinState,
    firstResp: WalletCheckinResponse.Success,
    logicalToday: String,
    alreadyDoneLogicalToday: Boolean,
    hasBaselineRecord: Boolean,
    oldFreeMinutes: Int,
    maxFreeMinutes: Int,
    notificationsEnabled: Boolean,
    usedBackupRenewal: Boolean,
    chainBuilder: StringBuilder,
  ): InternalExecutionResult {
    // Already completed in this logical day (e.g. single-account manual refresh): sync minutes without downgrading state
    if (alreadyDoneLogicalToday) {
      val reachedCap = firstResp.freeMinutes >= maxFreeMinutes
      val nextStatus = if (reachedCap) AccountStatus.PAUSED_CAP_REACHED else AccountStatus.DONE_TODAY
      chainBuilder.append(
        "\n✔ 今日已完成签到，仅同步最新时长 (HTTP=${firstResp.httpCode})：免费时长 ${firstResp.freeMinutes} 分钟，总时长 ${firstResp.totalMinutes} 分钟。",
      )
      storage.updateAccountAndState(
        accountId = profile.accountId,
        transformProfile = { if (reachedCap) profile.copy(isEnabled = false) else profile },
        transformState = {
          it.copy(
            status = nextStatus,
            lastCheckinDate = logicalToday,
            lastAttemptAt = System.currentTimeMillis(),
            freeMinutes = firstResp.freeMinutes,
            totalMinutes = firstResp.totalMinutes,
            failCount = 0,
            lastErrorMsg = "",
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
          resultTitle = if (reachedCap) "已达上限 · 自动暂停" else "今日已完成签到",
          isError = false,
          deltaMinutes = 0,
          httpStatusCode = firstResp.httpCode,
          detailChain = chainBuilder.toString(),
          usedBackupRenewal = usedBackupRenewal,
          pausedDueToCap = reachedCap,
        ),
      )
      if (reachedCap && notificationsEnabled) {
        NotificationHelper.showAlertNotification(
          context = context,
          accountId = profile.accountId,
          title = "${profile.gameType.displayName}·${profile.alias} 已达时长上限",
          message = "当前剩余免费时长 ${firstResp.freeMinutes} 分钟，已达到阈值 $maxFreeMinutes 分钟，系统已自动暂停该账号定时签到。",
          isReauthRequired = false,
        )
      }
      return InternalExecutionResult(
        outcome = BatchAccountOutcome(
          accountId = profile.accountId,
          gameType = profile.gameType,
          alias = profile.alias,
          status = nextStatus,
          deltaMinutes = 0,
          freeMinutes = firstResp.freeMinutes,
          message = if (reachedCap) "已达上限自动暂停" else "今日已完成签到",
        ),
        isTransientError = false,
      )
    }

    var finalResp = firstResp
    var didRecheck = false

    val firstDeltaFromOld = if (hasBaselineRecord) firstResp.freeMinutes - oldFreeMinutes else 0
    val shouldRecheckAfterDelay =
      firstDeltaFromOld <= 0 &&
        firstResp.sendFreeMinutes == 0 &&
        firstResp.freeMinutes < maxFreeMinutes

    if (shouldRecheckAfterDelay) {
      chainBuilder.append(
        "\n2. 首次响应未见时长增加 (HTTP=${firstResp.httpCode}, free=${firstResp.freeMinutes}m, send_freetime=0, 未达上限 ${maxFreeMinutes}m)，延迟 ${recheckDelayMillis / 1000} 秒后二次核对...",
      )
      delay(recheckDelayMillis)
      when (val recheckResp = apiClient.performWalletCheckin(profile)) {
        is WalletCheckinResponse.Success -> {
          finalResp = recheckResp
          didRecheck = true
          chainBuilder.append(
            "\n3. 二次核对响应 (HTTP=${recheckResp.httpCode})：免费时长 ${recheckResp.freeMinutes} 分钟，send_freetime=${recheckResp.sendFreeMinutes}。",
          )
        }
        is WalletCheckinResponse.AuthExpired -> {
          return handleAuthExpired(
            profile = profile,
            prevState = prevState,
            finalResp = recheckResp,
            notificationsEnabled = notificationsEnabled,
            usedBackupRenewal = usedBackupRenewal,
            chainBuilder = chainBuilder,
          )
        }
        is WalletCheckinResponse.TransientError -> {
          chainBuilder.append("\n3. 二次核对网络异常 (${recheckResp.message})，保留首次同步时长并保持 READY 等待重试。")
          storage.updateAccountState(profile.accountId) {
            it.copy(
              status = AccountStatus.READY,
              lastAttemptAt = System.currentTimeMillis(),
              freeMinutes = firstResp.freeMinutes,
              totalMinutes = firstResp.totalMinutes,
              failCount = it.failCount + 1,
              lastErrorMsg = recheckResp.message,
            )
          }
          storage.appendLog(
            CheckinLogEntry(
              id = UUID.randomUUID().toString(),
              timestamp = System.currentTimeMillis(),
              gameType = profile.gameType,
              accountAlias = profile.alias,
              accountId = profile.accountId,
              resultTitle = "二次核对网络异常 · 待重试",
              isError = true,
              deltaMinutes = 0,
              httpStatusCode = recheckResp.httpCode,
              detailChain = chainBuilder.toString(),
              usedBackupRenewal = usedBackupRenewal,
              pausedDueToCap = false,
            ),
          )
          return InternalExecutionResult(
            outcome = BatchAccountOutcome(
              accountId = profile.accountId,
              gameType = profile.gameType,
              alias = profile.alias,
              status = AccountStatus.READY,
              deltaMinutes = 0,
              freeMinutes = firstResp.freeMinutes,
              message = recheckResp.message,
            ),
            isTransientError = true,
          )
        }
      }
    }

    val verdict = evaluateCheckinVerdict(
      hasBaselineRecord = hasBaselineRecord,
      oldFreeMinutes = oldFreeMinutes,
      firstFreeMinutes = firstResp.freeMinutes,
      firstSendFreeMinutes = firstResp.sendFreeMinutes,
      finalFreeMinutes = finalResp.freeMinutes,
      finalSendFreeMinutes = finalResp.sendFreeMinutes,
      didRecheck = didRecheck,
      maxFreeMinutes = maxFreeMinutes,
    )

    if (verdict.type == CheckinVerdictType.NO_INCREASE_KEEP_READY) {
      chainBuilder.append(
        "\n⚠ $NO_INCREASE_MESSAGE (HTTP=${finalResp.httpCode})：免费时长 ${finalResp.freeMinutes} 分钟，总时长 ${finalResp.totalMinutes} 分钟，状态保持 READY。",
      )

      storage.updateAccountAndState(
        accountId = profile.accountId,
        transformProfile = { profile },
        transformState = {
          it.copy(
            status = AccountStatus.READY,
            lastCheckinDate = prevState.lastCheckinDate,
            lastAttemptAt = System.currentTimeMillis(),
            freeMinutes = finalResp.freeMinutes,
            totalMinutes = finalResp.totalMinutes,
            failCount = 0,
            lastErrorMsg = "",
            lastDeltaMinutes = 0,
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
          resultTitle = NO_INCREASE_MESSAGE,
          isError = true,
          deltaMinutes = 0,
          httpStatusCode = finalResp.httpCode,
          detailChain = chainBuilder.toString(),
          usedBackupRenewal = usedBackupRenewal,
          pausedDueToCap = false,
        ),
      )

      return InternalExecutionResult(
        outcome = BatchAccountOutcome(
          accountId = profile.accountId,
          gameType = profile.gameType,
          alias = profile.alias,
          status = AccountStatus.READY,
          deltaMinutes = 0,
          freeMinutes = finalResp.freeMinutes,
          message = NO_INCREASE_MESSAGE,
        ),
        isTransientError = false,
      )
    }

    val reachedCap = verdict.reachedCap
    val computedDelta = verdict.computedDelta
    val nextStatus = verdict.nextStatus
    val resultTitle = when (verdict.type) {
      CheckinVerdictType.SUCCESS_CAP_REACHED -> "已达上限 · 自动暂停"
      CheckinVerdictType.SUCCESS_INITIAL_BASELINE -> "首次基准同步成功"
      else -> "签到成功"
    }
    val outcomeMsg = when (verdict.type) {
      CheckinVerdictType.SUCCESS_CAP_REACHED -> "已达上限自动暂停"
      CheckinVerdictType.SUCCESS_INITIAL_BASELINE -> "首次基准时长同步完成"
      else -> "签到成功"
    }

    chainBuilder.append(
      "\n✔ $resultTitle (HTTP=${finalResp.httpCode})：免费时长 ${finalResp.freeMinutes} 分钟，总时长 ${finalResp.totalMinutes} 分钟，本次变化 +$computedDelta 分钟。",
    )
    if (reachedCap) {
      chainBuilder.append("\n⚠ 免费时长已达到上限阈值 ($maxFreeMinutes 分钟)，已自动关闭调度并暂停该账号。")
    }

    storage.updateAccountAndState(
      accountId = profile.accountId,
      transformProfile = {
        if (reachedCap) profile.copy(isEnabled = false) else profile
      },
      transformState = {
        it.copy(
          status = nextStatus,
          lastCheckinDate = if (verdict.shouldMarkLogicalDate) logicalToday else prevState.lastCheckinDate,
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
        resultTitle = resultTitle,
        isError = false,
        deltaMinutes = computedDelta,
        httpStatusCode = finalResp.httpCode,
        detailChain = chainBuilder.toString(),
        usedBackupRenewal = usedBackupRenewal,
        pausedDueToCap = reachedCap,
      ),
    )

    if (reachedCap && notificationsEnabled) {
      NotificationHelper.showAlertNotification(
        context = context,
        accountId = profile.accountId,
        title = "${profile.gameType.displayName}·${profile.alias} 已达时长上限",
        message = "当前剩余免费时长 ${finalResp.freeMinutes} 分钟，已达到阈值 $maxFreeMinutes 分钟，系统已自动暂停该账号定时签到。",
        isReauthRequired = false,
      )
    }

    return InternalExecutionResult(
      outcome = BatchAccountOutcome(
        accountId = profile.accountId,
        gameType = profile.gameType,
        alias = profile.alias,
        status = nextStatus,
        deltaMinutes = computedDelta,
        freeMinutes = finalResp.freeMinutes,
        message = outcomeMsg,
      ),
      isTransientError = false,
    )
  }

  private fun handleAuthExpired(
    profile: AccountProfile,
    prevState: com.example.data.CheckinState,
    finalResp: WalletCheckinResponse.AuthExpired,
    notificationsEnabled: Boolean,
    usedBackupRenewal: Boolean,
    chainBuilder: StringBuilder,
  ): InternalExecutionResult {
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

    if (notificationsEnabled) {
      NotificationHelper.showAlertNotification(
        context = context,
        accountId = profile.accountId,
        title = "${profile.gameType.displayName}·${profile.alias} 凭证已失效",
        message = "自动备用续期未能恢复凭证，请点击通知更新登录凭证。",
        isReauthRequired = true,
      )
    }

    return InternalExecutionResult(
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

  private fun handleTransientError(
    profile: AccountProfile,
    prevState: com.example.data.CheckinState,
    finalResp: WalletCheckinResponse.TransientError,
    usedBackupRenewal: Boolean,
    chainBuilder: StringBuilder,
  ): InternalExecutionResult {
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

    return InternalExecutionResult(
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

  private suspend fun attemptBackupCredentialRefresh(
    profile: AccountProfile,
    permissionMode: PermissionMode,
    chainBuilder: StringBuilder,
  ): AccountProfile? {
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

  companion object {
    const val DEFAULT_RECHECK_DELAY_MILLIS = 3_000L
    const val NO_INCREASE_MESSAGE = "未检测到时长增加（可能今日已在官方客户端领过或服务端延迟）"

    fun evaluateCheckinVerdict(
      hasBaselineRecord: Boolean,
      oldFreeMinutes: Int,
      firstFreeMinutes: Int,
      firstSendFreeMinutes: Int,
      finalFreeMinutes: Int,
      finalSendFreeMinutes: Int,
      didRecheck: Boolean,
      maxFreeMinutes: Int,
    ): CheckinVerdict {
      val effectiveSendFree = maxOf(firstSendFreeMinutes, finalSendFreeMinutes)
      val deltaFromOld = if (hasBaselineRecord) (finalFreeMinutes - oldFreeMinutes).coerceAtLeast(0) else 0
      val deltaFromFirst = if (didRecheck) (finalFreeMinutes - firstFreeMinutes).coerceAtLeast(0) else 0
      val durationIncreased = effectiveSendFree > 0 || deltaFromOld > 0 || deltaFromFirst > 0
      val computedDelta = when {
        effectiveSendFree > 0 -> effectiveSendFree
        deltaFromFirst > 0 -> deltaFromFirst
        deltaFromOld > 0 -> deltaFromOld
        else -> 0
      }

      if (finalFreeMinutes >= maxFreeMinutes) {
        return CheckinVerdict(
          type = CheckinVerdictType.SUCCESS_CAP_REACHED,
          nextStatus = AccountStatus.PAUSED_CAP_REACHED,
          computedDelta = computedDelta,
          shouldMarkLogicalDate = true,
          reachedCap = true,
        )
      }

      if (durationIncreased) {
        return CheckinVerdict(
          type = CheckinVerdictType.SUCCESS_DURATION_INCREASED,
          nextStatus = AccountStatus.DONE_TODAY,
          computedDelta = computedDelta,
          shouldMarkLogicalDate = true,
          reachedCap = false,
        )
      }

      if (!hasBaselineRecord) {
        return CheckinVerdict(
          type = CheckinVerdictType.SUCCESS_INITIAL_BASELINE,
          nextStatus = AccountStatus.DONE_TODAY,
          computedDelta = 0,
          shouldMarkLogicalDate = true,
          reachedCap = false,
        )
      }

      return CheckinVerdict(
        type = CheckinVerdictType.NO_INCREASE_KEEP_READY,
        nextStatus = AccountStatus.READY,
        computedDelta = 0,
        shouldMarkLogicalDate = false,
        reachedCap = false,
      )
    }
  }
}
