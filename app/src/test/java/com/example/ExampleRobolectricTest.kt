package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.ClipboardCredentialParser
import com.example.core.RootHelper
import com.example.core.WebViewCredentialManager
import com.example.data.SecuritySanitizer
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
}
