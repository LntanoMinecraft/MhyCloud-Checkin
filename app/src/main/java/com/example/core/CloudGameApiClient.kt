package com.example.core

import com.example.data.AccountProfile
import com.example.data.SecuritySanitizer
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import okhttp3.OkHttpClient
import okhttp3.Request

sealed interface WalletCheckinResponse {
  data class Success(
    val freeMinutes: Int,
    val totalMinutes: Int,
    val sendFreeMinutes: Int,
    val httpCode: Int,
    val debugNote: String,
  ) : WalletCheckinResponse

  data class AuthExpired(
    val httpCode: Int,
    val retcode: Int,
    val message: String,
  ) : WalletCheckinResponse

  data class TransientError(
    val httpCode: Int?,
    val message: String,
  ) : WalletCheckinResponse
}

class CloudGameApiClient {

  private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
  }

  private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(20, TimeUnit.SECONDS)
    .writeTimeout(15, TimeUnit.SECONDS)
    .retryOnConnectionFailure(true)
    .build()

  suspend fun performWalletCheckin(profile: AccountProfile): WalletCheckinResponse =
    withContext(Dispatchers.IO) {
      val boundHeaders = runCatching {
        json.decodeFromString<Map<String, String>>(profile.deviceHeadersJson)
      }.getOrDefault(emptyMap())

      val requestBuilder = Request.Builder()
        .url(profile.gameType.walletEndpoint)
        .get()

      // Strictly bind profile's own source headers; never mix Web and Android client headers
      boundHeaders.forEach { (key, value) ->
        if (!key.equals("x-rpc-combo_token", ignoreCase = true) && value.isNotBlank()) {
          requestBuilder.header(key, value)
        }
      }
      requestBuilder.header("x-rpc-combo_token", profile.comboToken)
      if (!boundHeaders.keys.any { it.equals("Accept", ignoreCase = true) }) {
        requestBuilder.header("Accept", "application/json, text/plain, */*")
      }

      val request = requestBuilder.build()
      try {
        okHttpClient.newCall(request).execute().use { response ->
          val httpCode = response.code
          val bodyStr = response.body?.string().orEmpty()

          if (httpCode == 401 || httpCode == 403) {
            return@withContext WalletCheckinResponse.AuthExpired(
              httpCode = httpCode,
              retcode = -100,
              message = "HTTP $httpCode 鉴权失败，凭证已过期",
            )
          }
          if (httpCode in 500..599 || httpCode == 429) {
            return@withContext WalletCheckinResponse.TransientError(
              httpCode = httpCode,
              message = "服务端繁忙或限流 (HTTP $httpCode)",
            )
          }
          if (!response.isSuccessful) {
            return@withContext WalletCheckinResponse.TransientError(
              httpCode = httpCode,
              message = "请求异常 (HTTP $httpCode)",
            )
          }

          val rootObj = runCatching {
            json.parseToJsonElement(bodyStr).jsonObject
          }.getOrNull() ?: return@withContext WalletCheckinResponse.TransientError(
            httpCode = httpCode,
            message = "响应 JSON 解析失败",
          )

          val retcode = rootObj["retcode"].asIntOrNull() ?: -1
          val message = (rootObj["message"] as? JsonPrimitive)?.content.orEmpty()

          if (retcode == 0) {
            val dataObj = rootObj["data"] as? JsonObject
            val parsedTimes = extractTimes(dataObj)
            val sanitizedHeaderSummary = SecuritySanitizer.sanitizeHeadersForLog(
              boundHeaders + ("x-rpc-combo_token" to profile.comboToken),
            )
            return@withContext WalletCheckinResponse.Success(
              freeMinutes = parsedTimes.freeMinutes,
              totalMinutes = parsedTimes.totalMinutes,
              sendFreeMinutes = parsedTimes.sendFreeMinutes,
              httpCode = httpCode,
              debugNote = sanitizedHeaderSummary,
            )
          }

          // Common miHoYo auth failure retcodes
          if (retcode in setOf(-100, -101, -102, -10002, 10001, 401, 403)) {
            return@withContext WalletCheckinResponse.AuthExpired(
              httpCode = httpCode,
              retcode = retcode,
              message = "retcode=$retcode ($message)",
            )
          }

          WalletCheckinResponse.TransientError(
            httpCode = httpCode,
            message = "接口返回 retcode=$retcode ($message)",
          )
        }
      } catch (e: IOException) {
        WalletCheckinResponse.TransientError(
          httpCode = null,
          message = "网络异常: ${e.javaClass.simpleName}",
        )
      } catch (e: Exception) {
        WalletCheckinResponse.TransientError(
          httpCode = null,
          message = "执行异常: ${e.message ?: e.javaClass.simpleName}",
        )
      }
    }

  private data class ParsedWalletTimes(
    val freeMinutes: Int,
    val totalMinutes: Int,
    val sendFreeMinutes: Int,
  )

  private fun JsonElement?.asIntOrNull(): Int? {
    val primitive = this as? JsonPrimitive ?: return null
    return primitive.intOrNull ?: primitive.content.trim().toIntOrNull()
  }

  private fun extractTimes(dataObj: JsonObject?): ParsedWalletTimes {
    if (dataObj == null) return ParsedWalletTimes(0, 0, 0)

    var freeMin = 0
    var sendFreeMin = 0

    when (val freeElem = dataObj["free_time"]) {
      is JsonObject -> {
        freeMin = freeElem["free_time"].asIntOrNull() ?: 0
        sendFreeMin = freeElem["send_freetime"].asIntOrNull()
          ?: freeElem["send_free_time"].asIntOrNull()
          ?: 0
      }
      is JsonPrimitive -> {
        freeMin = freeElem.asIntOrNull() ?: 0
      }
      else -> {}
    }

    val totalFromField = dataObj["total_time"].asIntOrNull()
    val coinTime = dataObj["coin_time"].asIntOrNull() ?: 0
    val playCard = (dataObj["play_card"] as? JsonObject)
      ?.get("short_time").asIntOrNull() ?: 0

    val totalMin = totalFromField ?: (freeMin + coinTime + playCard)
    return ParsedWalletTimes(
      freeMinutes = freeMin.coerceAtLeast(0),
      totalMinutes = totalMin.coerceAtLeast(freeMin),
      sendFreeMinutes = sendFreeMin.coerceAtLeast(0),
    )
  }
}
