package com.example.data

import com.example.BuildConfig

object SecuritySanitizer {

  private val sensitiveKeyValueRegex =
    Regex("""(?i)(ct|combo_token|x-rpc-combo_token|open_id|oi|cookie)\s*([=:])\s*([^\s;'",&\r\n]+)""")

  fun maskTokenValue(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return ""
    if (trimmed.length <= 8) {
      return "${trimmed.take(2)}****"
    }
    return "${trimmed.take(4)}****${trimmed.takeLast(4)}"
  }

  fun maskComboTokenPayload(comboToken: String): String {
    if (comboToken.isBlank()) return ""
    // Format may be semicolon-separated ai=...;ci=...;oi=...;ct=...;si=...;bi=... or raw token
    if (!comboToken.contains("=")) {
      return maskTokenValue(comboToken)
    }
    return comboToken.split(";").joinToString(";") { segment ->
      val parts = segment.split("=", limit = 2)
      if (parts.size == 2) {
        val key = parts[0].trim()
        val value = parts[1].trim()
        if (key.equals("ct", ignoreCase = true) ||
          key.equals("oi", ignoreCase = true) ||
          key.equals("combo_token", ignoreCase = true) ||
          key.equals("open_id", ignoreCase = true)
        ) {
          "$key=${maskTokenValue(value)}"
        } else {
          "$key=$value"
        }
      } else {
        segment
      }
    }
  }

  fun sanitizeText(input: String): String {
    if (input.isBlank()) return input
    var output = sensitiveKeyValueRegex.replace(input) { match ->
      val key = match.groupValues[1]
      val separator = match.groupValues[2]
      val value = match.groupValues[3]
      val masked = if (value.contains("ct=") || value.contains("oi=")) {
        maskComboTokenPayload(value)
      } else {
        maskTokenValue(value)
      }
      "$key$separator$masked"
    }
    // Redact Bearer or Authorization lines if present
    output = output.replace(Regex("""(?i)(authorization\s*:\s*)([^\r\n]+)""")) { match ->
      "${match.groupValues[1]}${maskTokenValue(match.groupValues[2])}"
    }
    return output
  }

  fun sanitizeHeadersForLog(headers: Map<String, String>): String {
    if (!BuildConfig.DEBUG) {
      return "[Release 模式已隐藏完整请求头，共 ${headers.size} 项]"
    }
    return headers.entries.joinToString("\n") { (key, value) ->
      val maskedValue = when {
        key.equals("x-rpc-combo_token", ignoreCase = true) -> maskComboTokenPayload(value)
        key.equals("cookie", ignoreCase = true) -> maskTokenValue(value)
        key.equals("authorization", ignoreCase = true) -> maskTokenValue(value)
        else -> value
      }
      "$key: $maskedValue"
    }
  }
}
