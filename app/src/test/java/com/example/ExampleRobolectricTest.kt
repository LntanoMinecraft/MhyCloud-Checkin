package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.CheckinOrchestrator
import com.example.core.CheckinVerdictType
import com.example.core.ClipboardCredentialParser
import com.example.core.RootHelper
import com.example.core.WebViewCredentialManager
import com.example.data.AccountStatus
import com.example.data.EncryptedStorageRepository
import com.example.data.SecuritySanitizer
import com.example.scheduler.ScheduleManager
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExampleRobolectricTest {

  @Test
  fun `app_name matches configured launcher label`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("云签助手", appName)
  }

  @Test
  fun `clipboard parser extracts curl and raw headers accurately`() {
    val curlSample = """
      curl 'https://api-cloudgame.mihoyo.com/nap_cn/wallet/wallet/get' \
        -H 'x-rpc-combo_token: ai=12;ci=1;oi=1234567890;ct=abcdefgh12345678;si=nap_cn;bi=nap_cn' \
        -H 'x-rpc-device_id: 550e8400-e29b-41d4-a716-446655440000' \
        -H 'x-rpc-client_type: 4' \
        -H 'x-rpc-app_version: 1.4.0'
    """.trimIndent()
    val parsedCurl = ClipboardCredentialParser.parseText(curlSample)
    assertTrue(parsedCurl.success)
    assertTrue(parsedCurl.hasComboToken)
    assertTrue(parsedCurl.hasDeviceId)
    assertTrue(parsedCurl.hasClientType)
    assertTrue(parsedCurl.hasAppVersion)
    assertFalse(parsedCurl.maskedComboToken.contains("abcdefgh12345678"))

    val rawHeaderSample = """
      X-Rpc-Combo_Token: ai=12;ci=1;oi=987654321;ct=secretTokenValue99;si=nap_cn;bi=nap_cn
      X-Rpc-Device_Id: 550e8400-e29b-41d4-a716-446655440001
    """.trimIndent()
    val parsedRaw = ClipboardCredentialParser.parseText(rawHeaderSample)
    assertTrue(parsedRaw.success)
    assertTrue(parsedRaw.hasComboToken)
    assertTrue(parsedRaw.hasDeviceId)
  }

  @Test
  fun `security sanitizer masks tokens keeping first 4 and last 4 chars`() {
    val input = "x-rpc-combo_token: ai=4;ci=1;oi=1122334455;ct=1234567890abcdef;si=hk4e_cn"
    val sanitized = SecuritySanitizer.sanitizeText(input)
    assertFalse(sanitized.contains("1234567890abcdef"))
    assertTrue(sanitized.contains("1234****cdef"))
  }

  @Test
  fun `webview allowlist permits only mihoyo and hoyoverse https domains`() {
    assertTrue(WebViewCredentialManager.isAllowedHost("https://ys.mihoyo.com/cloud/#/"))
    assertTrue(WebViewCredentialManager.isAllowedHost("https://account.hoyoverse.com/login"))
    assertFalse(WebViewCredentialManager.isAllowedHost("http://ys.mihoyo.com/cloud/"))
    assertFalse(WebViewCredentialManager.isAllowedHost("https://evil-mihoyo.com/phish"))
    assertFalse(WebViewCredentialManager.isAllowedHost("file:///etc/passwd"))
  }

  @Test
  fun `root xml parser extracts shared_prefs strings cleanly`() {
    val xml = """
      <?xml version='1.0' encoding='utf-8' standalone='yes' ?>
      <map>
          <string name="sp_key_app_id">4</string>
          <string name="sp_key_open_id">123456789</string>
          <string name="sp_key_combo_token">token_value_123456</string>
      </map>
    """.trimIndent()
    val parsed = RootHelper.parseSharedPrefsXml(xml)
    assertNotNull(parsed)
    assertEquals("4", parsed!!["sp_key_app_id"])
    assertEquals("123456789", parsed["sp_key_open_id"])
  }

  @Test
  fun `game logical date resets at 04_00 Beijing time`() {
    val zone = EncryptedStorageRepository.BEIJING_ZONE
    val beforeReset = ZonedDateTime.of(2026, 11, 19, 3, 59, 59, 0, zone)
    val atReset = ZonedDateTime.of(2026, 11, 19, 4, 0, 0, 0, zone)
    val midnight = ZonedDateTime.of(2026, 11, 19, 0, 0, 0, 0, zone)

    assertEquals("2026-11-18", EncryptedStorageRepository.getGameLogicalDate(midnight))
    assertEquals("2026-11-18", EncryptedStorageRepository.getGameLogicalDate(beforeReset))
    assertEquals("2026-11-19", EncryptedStorageRepository.getGameLogicalDate(atReset))
  }

  @Test
  fun `periodic fallback respects 04_00 boundary and user schedule time`() {
    val zone = EncryptedStorageRepository.BEIJING_ZONE
    assertFalse(
      ScheduleManager.isPeriodicFallbackAllowedNow(
        scheduleHour = 7,
        scheduleMinute = 0,
        now = ZonedDateTime.of(2026, 11, 19, 2, 30, 0, 0, zone),
      ),
    )
    assertFalse(
      ScheduleManager.isPeriodicFallbackAllowedNow(
        scheduleHour = 7,
        scheduleMinute = 0,
        now = ZonedDateTime.of(2026, 11, 19, 6, 59, 0, 0, zone),
      ),
    )
    assertTrue(
      ScheduleManager.isPeriodicFallbackAllowedNow(
        scheduleHour = 7,
        scheduleMinute = 0,
        now = ZonedDateTime.of(2026, 11, 19, 7, 0, 0, 0, zone),
      ),
    )
    // Even if user configured 02:00, never run before 04:00 game day reset
    assertFalse(
      ScheduleManager.isPeriodicFallbackAllowedNow(
        scheduleHour = 2,
        scheduleMinute = 0,
        now = ZonedDateTime.of(2026, 11, 19, 3, 30, 0, 0, zone),
      ),
    )
  }

  @Test
  fun `checkin verdict enforces duration increase, cap pause, initial sync, and READY on zero delta`() {
    // 1. No increase after 3s recheck on existing account below cap -> MUST remain READY
    val noIncrease = CheckinOrchestrator.evaluateCheckinVerdict(
      hasBaselineRecord = true,
      oldFreeMinutes = 120,
      firstFreeMinutes = 120,
      firstSendFreeMinutes = 0,
      finalFreeMinutes = 120,
      finalSendFreeMinutes = 0,
      didRecheck = true,
      maxFreeMinutes = 600,
    )
    assertEquals(CheckinVerdictType.NO_INCREASE_KEEP_READY, noIncrease.type)
    assertEquals(AccountStatus.READY, noIncrease.nextStatus)
    assertFalse(noIncrease.shouldMarkLogicalDate)

    // 2. Async grant increased on 3s recheck even if user spent minutes since yesterday -> DONE_TODAY (+15)
    val asyncIncreased = CheckinOrchestrator.evaluateCheckinVerdict(
      hasBaselineRecord = true,
      oldFreeMinutes = 120,
      firstFreeMinutes = 60,
      firstSendFreeMinutes = 0,
      finalFreeMinutes = 75,
      finalSendFreeMinutes = 0,
      didRecheck = true,
      maxFreeMinutes = 600,
    )
    assertEquals(CheckinVerdictType.SUCCESS_DURATION_INCREASED, asyncIncreased.type)
    assertEquals(AccountStatus.DONE_TODAY, asyncIncreased.nextStatus)
    assertEquals(15, asyncIncreased.computedDelta)
    assertTrue(asyncIncreased.shouldMarkLogicalDate)

    // 3. Reached free minute cap -> PAUSED_CAP_REACHED
    val capReached = CheckinOrchestrator.evaluateCheckinVerdict(
      hasBaselineRecord = true,
      oldFreeMinutes = 600,
      firstFreeMinutes = 600,
      firstSendFreeMinutes = 0,
      finalFreeMinutes = 600,
      finalSendFreeMinutes = 0,
      didRecheck = false,
      maxFreeMinutes = 600,
    )
    assertEquals(CheckinVerdictType.SUCCESS_CAP_REACHED, capReached.type)
    assertEquals(AccountStatus.PAUSED_CAP_REACHED, capReached.nextStatus)
    assertTrue(capReached.reachedCap)

    // 4. First-time baseline sync (never synced before) -> DONE_TODAY
    val initialBaseline = CheckinOrchestrator.evaluateCheckinVerdict(
      hasBaselineRecord = false,
      oldFreeMinutes = 0,
      firstFreeMinutes = 180,
      firstSendFreeMinutes = 0,
      finalFreeMinutes = 180,
      finalSendFreeMinutes = 0,
      didRecheck = true,
      maxFreeMinutes = 600,
    )
    assertEquals(CheckinVerdictType.SUCCESS_INITIAL_BASELINE, initialBaseline.type)
    assertEquals(AccountStatus.DONE_TODAY, initialBaseline.nextStatus)
    assertEquals(0, initialBaseline.computedDelta)
    assertTrue(initialBaseline.shouldMarkLogicalDate)
  }
}
