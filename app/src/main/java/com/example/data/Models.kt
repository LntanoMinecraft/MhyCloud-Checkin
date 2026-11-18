package com.example.data

import kotlinx.serialization.Serializable

@Serializable
enum class GameType(
  val displayName: String,
  val shortTag: String,
  val packageName: String,
  val gameKey: String,
  val webUrl: String,
  val walletEndpoint: String,
  val notificationsEndpoint: String,
) {
  GENSHIN(
    displayName = "云·原神",
    shortTag = "YS",
    packageName = "com.miHoYo.cloudgames.ys",
    gameKey = "hk4e_cn",
    webUrl = "https://ys.mihoyo.com/cloud/#/",
    walletEndpoint = "https://api-cloudgame.mihoyo.com/hk4e_cg_cn/wallet/wallet/get?cost_method=0&get_type=0",
    notificationsEndpoint = "https://api-cloudgame.mihoyo.com/hk4e_cg_cn/gamer/api/listNotifications",
  ),
  ZZZ(
    displayName = "云·绝区零",
    shortTag = "NAP",
    packageName = "com.miHoYo.cloudgames.Nap",
    gameKey = "nap_cn",
    webUrl = "https://cg.mihoyo.com/zzz/",
    walletEndpoint = "https://cg-nap-api.mihoyo.com/nap_cn/cg/wallet/wallet/get?cost_method=0&get_type=0",
    notificationsEndpoint = "https://api-cloudgame.mihoyo.com/nap_cn/gamer/api/listNotifications",
  ),
}

@Serializable
enum class PermissionMode(val label: String) {
  BASIC("基础模式"),
  ROOT("Root 模式"),
}

@Serializable
enum class CredentialSource(val label: String) {
  ROOT_READ("Root 直读"),
  WEBVIEW("内置 WebView"),
  CLIPBOARD("剪贴板导入"),
}

@Serializable
enum class AccountStatus(val label: String) {
  PENDING_INIT("待初始化"),
  READY("待签到"),
  CHECKING("签到中"),
  DONE_TODAY("今日已完成"),
  PAUSED_CAP_REACHED("已达上限 · 自动暂停"),
  NEED_REAUTH("凭证失效"),
}

@Serializable
enum class ThemePreference(val label: String) {
  SYSTEM("跟随系统"),
  LIGHT("浅色"),
  DARK("深色"),
}

@Serializable
data class AccountProfile(
  val accountId: String,
  val gameType: GameType,
  val alias: String,
  val region: String = "cn_official",
  val isEnabled: Boolean = true,
  val credentialSource: CredentialSource,
  val comboToken: String,
  val deviceHeadersJson: String,
  val hasWebviewSession: Boolean = false,
  val maskedUid: String = "",
  val updatedAt: Long = System.currentTimeMillis(),
)

@Serializable
data class CheckinState(
  val accountId: String,
  val status: AccountStatus = AccountStatus.READY,
  val lastCheckinDate: String = "",
  val lastAttemptAt: Long = 0L,
  val freeMinutes: Int = 0,
  val totalMinutes: Int = 0,
  val failCount: Int = 0,
  val lastErrorMsg: String = "",
  val lastDeltaMinutes: Int = 0,
)

@Serializable
data class AppGlobalConfig(
  val permissionMode: PermissionMode = PermissionMode.BASIC,
  val scheduleHour: Int = 8,
  val scheduleMinute: Int = 30,
  val jitterMinutes: Int = 15,
  val autoBackupRenewal: Boolean = true,
  val maxFreeMinutes: Int = 600,
  val notificationsEnabled: Boolean = true,
  val useDynamicColor: Boolean = true,
  val themeSeedColorHex: String? = null,
  val themePreference: ThemePreference = ThemePreference.SYSTEM,
  val nextScheduledEpochMillis: Long = 0L,
)

@Serializable
data class CheckinLogEntry(
  val id: String,
  val timestamp: Long,
  val gameType: GameType,
  val accountAlias: String,
  val accountId: String,
  val resultTitle: String,
  val isError: Boolean,
  val deltaMinutes: Int = 0,
  val httpStatusCode: Int? = null,
  val detailChain: String,
  val usedBackupRenewal: Boolean = false,
  val pausedDueToCap: Boolean = false,
)

data class AccountWithState(
  val profile: AccountProfile,
  val state: CheckinState,
)
