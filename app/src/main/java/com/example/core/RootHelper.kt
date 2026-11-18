package com.example.core

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Xml
import com.example.data.CredentialSource
import com.example.data.GameType
import com.example.data.SecuritySanitizer
import com.topjohnwu.superuser.Shell
import java.io.StringReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.xmlpull.v1.XmlPullParser

data class RootProbeResult(
  val isRooted: Boolean,
  val dozeWhitelisted: Boolean,
  val detailMessage: String,
)

data class RootScanAccountResult(
  val gameType: GameType,
  val found: Boolean,
  val openId: String = "",
  val maskedOpenId: String = "",
  val comboTokenHeader: String = "",
  val maskedComboToken: String = "",
  val appVersion: String = "",
  val gameBiz: String = "",
  val deviceId: String = "",
  val deviceHeadersJson: String = "",
  val errorReason: String = "",
)

object RootHelper {

  // Hardcoded package whitelist (NEVER interpolate user input)
  private const val PKG_GENSHIN = "com.miHoYo.cloudgames.ys"
  private const val PKG_ZZZ = "com.miHoYo.cloudgames.Nap"

  private const val FILE_LOGIN = "mihoyo_login.xml"
  private const val FILE_SDK = "mihoyo_sdk_preference.xml"
  private const val FILE_DEVICE = "pre_device.xml.xml"
  private const val DB_KIBANA_CLOUD_GAME = "databases/kibana_db_cloud_game"

  private val packageAllowlist = setOf(PKG_GENSHIN, PKG_ZZZ)
  private val fileAllowlist = setOf(FILE_LOGIN, FILE_SDK, FILE_DEVICE)
  private val dbPathAllowlist = setOf(DB_KIBANA_CLOUD_GAME)

  private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
  }

  private fun whitelistedPackageFor(gameType: GameType): String = when (gameType) {
    GameType.GENSHIN -> PKG_GENSHIN
    GameType.ZZZ -> PKG_ZZZ
  }

  suspend fun probeRootAndApplyDoze(context: Context): RootProbeResult = withContext(Dispatchers.IO) {
    val shell = Shell.getShell()
    if (!shell.isRoot) {
      return@withContext RootProbeResult(
        isRooted = false,
        dozeWhitelisted = false,
        detailMessage = "未获得 su Root 授权，已自动回退至基础模式",
      )
    }

    val ownPkg = context.packageName
    val dozeOk = if (ownPkg.matches(Regex("^[a-zA-Z0-9_.]+$"))) {
      val res = Shell.cmd("dumpsys deviceidle whitelist +$ownPkg").exec()
      res.isSuccess
    } else {
      false
    }

    RootProbeResult(
      isRooted = true,
      dozeWhitelisted = dozeOk,
      detailMessage = if (dozeOk) {
        "Root 授权成功，已将本应用加入 Doze 白名单"
      } else {
        "Root 授权成功，但 Doze 白名单写入失败（可手动配置电池优化无限制）"
      },
    )
  }

  suspend fun scanAllInstalledGames(context: Context): List<RootScanAccountResult> {
    return listOf(
      scanCloudGameAccount(context, GameType.GENSHIN),
      scanCloudGameAccount(context, GameType.ZZZ),
    )
  }

  suspend fun scanCloudGameAccount(
    context: Context,
    gameType: GameType,
  ): RootScanAccountResult = withContext(Dispatchers.IO) {
    val shell = Shell.getShell()
    if (!shell.isRoot) {
      return@withContext RootScanAccountResult(
        gameType = gameType,
        found = false,
        errorReason = "未取得 Root 权限",
      )
    }

    val pkg = whitelistedPackageFor(gameType)
    if (pkg !in packageAllowlist) {
      return@withContext RootScanAccountResult(
        gameType = gameType,
        found = false,
        errorReason = "目标包名不在安全白名单中",
      )
    }

    val loginXml = readProtectedSharedPrefsXml(pkg, FILE_LOGIN)
    if (loginXml == null) {
      return@withContext RootScanAccountResult(
        gameType = gameType,
        found = false,
        errorReason = "未检测到 ${gameType.displayName} 的登录配置 ($FILE_LOGIN)",
      )
    }

    val loginMap = parseSharedPrefsXmlWithRetry(pkg, FILE_LOGIN, loginXml)
    if (loginMap == null) {
      return@withContext RootScanAccountResult(
        gameType = gameType,
        found = false,
        errorReason = "解析 $FILE_LOGIN 失败（并发写入或文件损坏）",
      )
    }

    val appId = loginMap["sp_key_app_id"].orEmpty()
    val channelId = loginMap["sp_key_channel_id"].orEmpty()
    val openId = loginMap["sp_key_open_id"].orEmpty()
    val comboToken = loginMap["sp_key_combo_token"].orEmpty()

    if (openId.isBlank() || comboToken.isBlank()) {
      return@withContext RootScanAccountResult(
        gameType = gameType,
        found = false,
        errorReason = "${gameType.displayName} 尚未登录账号（open_id 或 combo_token 为空）",
      )
    }

    val sdkXml = readProtectedSharedPrefsXml(pkg, FILE_SDK)
    val sdkMap = if (sdkXml != null) parseSharedPrefsXmlWithRetry(pkg, FILE_SDK, sdkXml) else null
    val mdkInitRaw = sdkMap?.get("mdk_init_config").orEmpty()
    val gameKey = extractGameKeyFromMdkConfig(mdkInitRaw).ifBlank { gameType.gameKey }

    val deviceXml = readProtectedSharedPrefsXml(pkg, FILE_DEVICE)
    val deviceMap = if (deviceXml != null) parseSharedPrefsXmlWithRetry(pkg, FILE_DEVICE, deviceXml) else null
    val deviceId = deviceMap?.get("device_id")
      ?.takeIf { it.isNotBlank() }
      ?: java.util.UUID.randomUUID().toString()

    val appVersion = getInstalledAppVersion(context, pkg)
    val resolvedAppId = appId.ifBlank { if (gameType == GameType.GENSHIN) "4" else "12" }
    val resolvedChannelId = channelId.ifBlank { "1" }
    val sessionId = readLatestSessionId(pkg)

    val comboTokenHeader = "ai=$resolvedAppId;ci=$resolvedChannelId;oi=$openId;ct=$comboToken;si=$sessionId;bi=$gameKey"

    // Source-bound Android native client headers (aligned with real Android client traffic)
    val headersMap = linkedMapOf(
      "x-rpc-client_type" to "2",
      "x-rpc-vendor_id" to "2",
      "x-rpc-language" to "zh-cn",
      "x-rpc-app_version" to appVersion,
      "x-rpc-device_id" to deviceId,
      "x-rpc-device_name" to "${Build.MANUFACTURER} ${Build.MODEL}",
      "x-rpc-device_model" to Build.MODEL,
      "x-rpc-sys_version" to Build.VERSION.RELEASE,
      "User-Agent" to "okhttp/4.10.0",
    ).apply {
      when (gameType) {
        GameType.GENSHIN -> {
          put("x-rpc-app_id", "1953439974")
          put("x-rpc-channel", "miyoushe")
          put("x-rpc-cps", "miyoushe")
          put("x-rpc-op_biz", "clgm_cn")
          put("x-rpc-cg_game_biz", "hk4e_cn")
          put("x-rpc-cg_game_id", "9000055")
        }
        GameType.ZZZ -> {
          put("x-rpc-app_id", "1953458679")
          put("x-rpc-channel", "mys_cloud")
          put("x-rpc-cps", "mys_cloud")
          put("x-rpc-op_biz", "clgm_nap-cn")
          put("x-rpc-cg_game_biz", "nap_cn")
          put("x-rpc-cg_game_id", "9000357")
        }
      }
    }

    RootScanAccountResult(
      gameType = gameType,
      found = true,
      openId = openId,
      maskedOpenId = SecuritySanitizer.maskTokenValue(openId),
      comboTokenHeader = comboTokenHeader,
      maskedComboToken = SecuritySanitizer.maskComboTokenPayload(comboTokenHeader),
      appVersion = appVersion,
      gameBiz = gameKey,
      deviceId = deviceId,
      deviceHeadersJson = json.encodeToString(headersMap),
    )
  }

  private fun readProtectedSharedPrefsXml(pkg: String, fileName: String): String? {
    if (pkg !in packageAllowlist || fileName !in fileAllowlist) return null
    val primaryPath = "/data/user/0/$pkg/shared_prefs/$fileName"
    val fallbackPath = "/data/data/$pkg/shared_prefs/$fileName"

    val primaryRes = Shell.cmd("cat '$primaryPath'").exec()
    if (primaryRes.isSuccess && primaryRes.out.isNotEmpty()) {
      return primaryRes.out.joinToString("\n")
    }
    val fallbackRes = Shell.cmd("cat '$fallbackPath'").exec()
    if (fallbackRes.isSuccess && fallbackRes.out.isNotEmpty()) {
      return fallbackRes.out.joinToString("\n")
    }
    return null
  }

  private fun readLatestSessionId(pkg: String): String {
    if (pkg !in packageAllowlist || DB_KIBANA_CLOUD_GAME !in dbPathAllowlist) return ""
    val primaryUserDir = "/data/user/0/$pkg"
    val primaryDbPath = "$primaryUserDir/$DB_KIBANA_CLOUD_GAME"
    val fallbackDbPath = "/data/data/$pkg/$DB_KIBANA_CLOUD_GAME"

    val hasPrimaryDir = Shell.cmd("[ -d '$primaryUserDir' ]").exec().isSuccess
    val targetDbPath = if (hasPrimaryDir) primaryDbPath else fallbackDbPath

    val grepRes = Shell.cmd(
      "grep -aoE 'si=[0-9a-fA-F]{32,128}' '$targetDbPath' 2>/dev/null | tail -n 1",
    ).exec()
    val rawLine = if (grepRes.isSuccess && grepRes.out.isNotEmpty()) {
      grepRes.out.lastOrNull().orEmpty()
    } else if (hasPrimaryDir) {
      val fallbackRes = Shell.cmd(
        "grep -aoE 'si=[0-9a-fA-F]{32,128}' '$fallbackDbPath' 2>/dev/null | tail -n 1",
      ).exec()
      if (fallbackRes.isSuccess && fallbackRes.out.isNotEmpty()) {
        fallbackRes.out.lastOrNull().orEmpty()
      } else {
        ""
      }
    } else {
      ""
    }

    return Regex("""si=([0-9a-fA-F]{32,128})""")
      .find(rawLine)
      ?.groupValues
      ?.getOrNull(1)
      .orEmpty()
  }

  private suspend fun parseSharedPrefsXmlWithRetry(
    pkg: String,
    fileName: String,
    initialXml: String,
  ): Map<String, String>? {
    if (initialXml.contains("</map>")) {
      val parsed = parseSharedPrefsXml(initialXml)
      if (parsed != null) return parsed
    }
    // Concurrency protection: wait 500ms and retry once if XML was being flushed
    delay(500L)
    val retryXml = readProtectedSharedPrefsXml(pkg, fileName) ?: return null
    if (!retryXml.contains("</map>")) return null
    return parseSharedPrefsXml(retryXml)
  }

  internal fun parseSharedPrefsXml(xmlContent: String): Map<String, String>? {
    return runCatching {
      val result = mutableMapOf<String, String>()
      val parser = Xml.newPullParser()
      parser.setInput(StringReader(xmlContent))
      var eventType = parser.eventType
      while (eventType != XmlPullParser.END_DOCUMENT) {
        if (eventType == XmlPullParser.START_TAG) {
          val tagName = parser.name
          val key = parser.getAttributeValue(null, "name")
          if (!key.isNullOrBlank()) {
            when (tagName) {
              "string" -> result[key] = parser.nextText().orEmpty()
              "int", "long", "float", "boolean" -> {
                val value = parser.getAttributeValue(null, "value")
                if (value != null) result[key] = value
              }
            }
          }
        }
        eventType = parser.next()
      }
      result
    }.getOrNull()
  }

  private fun extractGameKeyFromMdkConfig(raw: String): String {
    if (raw.isBlank()) return ""
    val unescaped = raw
      .replace("&quot;", "\"")
      .replace("&amp;", "&")
      .replace("&lt;", "<")
      .replace("&gt;", ">")
    return runCatching {
      val obj = json.parseToJsonElement(unescaped).jsonObject
      obj["game_key"]?.jsonPrimitive?.content
        ?: obj["gameKey"]?.jsonPrimitive?.content
        ?: ""
    }.getOrDefault("")
  }

  private fun getInstalledAppVersion(context: Context, pkg: String): String {
    return runCatching {
      val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.packageManager.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0))
      } else {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(pkg, 0)
      }
      info.versionName?.takeIf { it.isNotBlank() }
    }.getOrNull() ?: if (pkg == PKG_GENSHIN) "5.0.0" else "1.4.0"
  }
}
