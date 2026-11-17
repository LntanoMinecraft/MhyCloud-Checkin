package com.example.core

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import com.example.data.SecuritySanitizer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class ClipboardParseResult(
  val success: Boolean,
  val comboToken: String = "",
  val maskedComboToken: String = "",
  val headers: Map<String, String> = emptyMap(),
  val headersJson: String = "",
  val hasComboToken: Boolean = false,
  val hasDeviceId: Boolean = false,
  val hasClientType: Boolean = false,
  val hasAppVersion: Boolean = false,
  val clipboardCleared: Boolean = false,
  val errorMessage: String = "",
)

object ClipboardCredentialParser {

  private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
  }

  // Matches cURL -H 'key: val', -H "key: val", or --header 'key: val'
  private val curlHeaderRegex =
    Regex("""(?i)(?:-H|--header)\s+(['"])([^'"]+?)\1""")

  // Matches raw HTTP header lines "Key: Value"
  private val rawLineHeaderRegex =
    Regex("""^\s*([A-Za-z0-9\-_]+)\s*:\s*(.+?)\s*$""")

  private val allowedHeaderKeys = setOf(
    "x-rpc-combo_token",
    "x-rpc-client_type",
    "x-rpc-app_version",
    "x-rpc-device_id",
    "x-rpc-device_name",
    "x-rpc-device_model",
    "x-rpc-sys_version",
    "x-rpc-channel",
    "x-rpc-cg_game_biz",
    "user-agent",
    "referer",
    "origin",
    "x-rpc-app_id",
    "x-rpc-vendor_id",
  )

  fun readAndParseFromClipboard(context: Context): ClipboardParseResult {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
      ?: return ClipboardParseResult(success = false, errorMessage = "无法访问系统剪贴板")
    val clip = clipboard.primaryClip
    if (clip == null || clip.itemCount == 0) {
      return ClipboardParseResult(success = false, errorMessage = "剪贴板为空，请先复制 cURL 或 HTTP Header")
    }
    val rawText = clip.getItemAt(0).coerceToText(context)?.toString().orEmpty()
    if (rawText.isBlank()) {
      return ClipboardParseResult(success = false, errorMessage = "剪贴板文本为空")
    }

    val parsed = parseText(rawText)
    if (parsed.success) {
      clearClipboard(clipboard)
      return parsed.copy(clipboardCleared = true)
    }
    return parsed
  }

  fun parseText(rawInput: String): ClipboardParseResult {
    val parsedHeaders = linkedMapOf<String, String>()

    // 1. Try cURL header extraction first
    val curlMatches = curlHeaderRegex.findAll(rawInput).toList()
    if (curlMatches.isNotEmpty()) {
      for (match in curlMatches) {
        val headerExpr = match.groupValues[2]
        val colonIdx = headerExpr.indexOf(':')
        if (colonIdx > 0) {
          val key = headerExpr.substring(0, colonIdx).trim()
          val value = headerExpr.substring(colonIdx + 1).trim()
          if (key.lowercase() in allowedHeaderKeys || key.lowercase().startsWith("x-rpc-")) {
            parsedHeaders[normalizeHeaderKey(key)] = value
          }
        }
      }
    }

    // 2. Fallback / supplement with raw multi-line HTTP Header parsing (case-insensitive)
    if (parsedHeaders.isEmpty()) {
      rawInput.lines().forEach { line ->
        val m = rawLineHeaderRegex.matchEntire(line)
        if (m != null) {
          val key = m.groupValues[1].trim()
          val value = m.groupValues[2].trim()
          if (key.lowercase() in allowedHeaderKeys || key.lowercase().startsWith("x-rpc-")) {
            parsedHeaders[normalizeHeaderKey(key)] = value
          }
        }
      }
    }

    val comboToken = parsedHeaders.entries
      .firstOrNull { it.key.equals("x-rpc-combo_token", ignoreCase = true) }
      ?.value
      .orEmpty()

    val hasCombo = comboToken.isNotBlank()
    val hasDeviceId = parsedHeaders.keys.any { it.equals("x-rpc-device_id", ignoreCase = true) }
    val hasClientType = parsedHeaders.keys.any { it.equals("x-rpc-client_type", ignoreCase = true) }
    val hasAppVersion = parsedHeaders.keys.any { it.equals("x-rpc-app_version", ignoreCase = true) }

    if (!hasCombo) {
      return ClipboardParseResult(
        success = false,
        hasComboToken = false,
        hasDeviceId = hasDeviceId,
        hasClientType = hasClientType,
        hasAppVersion = hasAppVersion,
        errorMessage = "未识别到有效的 x-rpc-combo_token，请确认复制了包含请求头的 cURL 或 Header 文本",
      )
    }

    // Remove combo_token from auxiliary deviceHeadersJson so token and headers remain cleanly separated
    val deviceHeaders = parsedHeaders.filterKeys { !it.equals("x-rpc-combo_token", ignoreCase = true) }

    return ClipboardParseResult(
      success = true,
      comboToken = comboToken,
      maskedComboToken = SecuritySanitizer.maskComboTokenPayload(comboToken),
      headers = deviceHeaders,
      headersJson = json.encodeToString(deviceHeaders),
      hasComboToken = true,
      hasDeviceId = hasDeviceId,
      hasClientType = hasClientType,
      hasAppVersion = hasAppVersion,
    )
  }

  private fun normalizeHeaderKey(key: String): String {
    return when (key.lowercase()) {
      "user-agent" -> "User-Agent"
      "referer" -> "Referer"
      "origin" -> "Origin"
      else -> key.lowercase()
    }
  }

  private fun clearClipboard(clipboard: ClipboardManager) {
    runCatching {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        clipboard.clearPrimaryClip()
      } else {
        clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
      }
    }
  }
}
