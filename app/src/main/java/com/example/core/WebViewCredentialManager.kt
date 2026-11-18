package com.example.core

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.data.GameType
import com.example.data.SecuritySanitizer
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class CapturedWebCredentials(
  val comboToken: String,
  val maskedComboToken: String,
  val deviceHeadersJson: String,
  val maskedUid: String,
)

object WebViewCredentialManager {

  private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
  }

  fun isAllowedHost(url: String?): Boolean {
    if (url.isNullOrBlank()) return false
    val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return false
    val scheme = uri.scheme?.lowercase() ?: return false
    if (scheme != "https") return false
    val host = uri.host?.lowercase() ?: return false
    return host == "mihoyo.com" ||
      host.endsWith(".mihoyo.com") ||
      host == "hoyoverse.com" ||
      host.endsWith(".hoyoverse.com")
  }

  fun clearCookiesForFreshLogin() {
    val cookieManager = CookieManager.getInstance()
    cookieManager.removeAllCookies(null)
    cookieManager.flush()
  }

  @SuppressLint("SetJavaScriptEnabled")
  fun configureSecureWebView(
    webView: WebView,
    onCredentialsCaptured: (CapturedWebCredentials) -> Unit,
    onPageChanged: ((String) -> Unit)? = null,
  ) {
    val capturedAlready = AtomicBoolean(false)
    val settings: WebSettings = webView.settings
    settings.javaScriptEnabled = true
    settings.domStorageEnabled = true
    settings.allowFileAccess = false
    settings.allowContentAccess = false
    settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
    settings.setSupportMultipleWindows(false)

    CookieManager.getInstance().setAcceptCookie(true)
    CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

    webView.webViewClient = object : WebViewClient() {
      override fun shouldOverrideUrlLoading(
        view: WebView?,
        request: WebResourceRequest?,
      ): Boolean {
        val targetUrl = request?.url?.toString()
        // Security Red Line #5: only permit *.mihoyo.com and *.hoyoverse.com
        return !isAllowedHost(targetUrl)
      }

      override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        if (!url.isNullOrBlank()) {
          onPageChanged?.invoke(url)
        }
      }

      override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?,
      ): WebResourceResponse? {
        if (request == null) return null
        val url = request.url?.toString().orEmpty()
        if (request.method.equals("GET", ignoreCase = true) &&
          url.contains("wallet/wallet/get", ignoreCase = true)
        ) {
          val headers = request.requestHeaders ?: emptyMap()
          val comboToken = headers.entries
            .firstOrNull { it.key.equals("x-rpc-combo_token", ignoreCase = true) }
            ?.value
            ?.trim()
            .orEmpty()

          val openIdMatch = Regex("""(?:^|;)\s*oi=(\d+)\s*(?:;|$)""")
            .find(comboToken)?.groupValues?.getOrNull(1).orEmpty()
          val isOiValid = openIdMatch.isNotBlank() && openIdMatch.any { it != '0' }
          val ctMatch = Regex("""(?:^|;)\s*ct=([^;\s]+)\s*(?:;|$)""")
            .find(comboToken)?.groupValues?.getOrNull(1).orEmpty()
          val isCtValid = ctMatch.isNotBlank() &&
            ctMatch.lowercase() !in setOf("null", "undefined", "none", "empty", "placeholder", "ct") &&
            !ctMatch.contains("****")

          if (isOiValid && isCtValid && capturedAlready.compareAndSet(false, true)) {
            val boundHeaders = linkedMapOf<String, String>()
            headers.forEach { (k, v) ->
              if ((k.startsWith("x-rpc-", ignoreCase = true) ||
                  k.equals("User-Agent", ignoreCase = true) ||
                  k.equals("Referer", ignoreCase = true) ||
                  k.equals("Origin", ignoreCase = true)) &&
                !k.equals("x-rpc-combo_token", ignoreCase = true)
              ) {
                boundHeaders[k] = v
              }
            }
            if (!boundHeaders.keys.any { it.equals("User-Agent", ignoreCase = true) }) {
              boundHeaders["User-Agent"] = settings.userAgentString
            }

            val credentials = CapturedWebCredentials(
              comboToken = comboToken,
              maskedComboToken = SecuritySanitizer.maskComboTokenPayload(comboToken),
              deviceHeadersJson = json.encodeToString(boundHeaders),
              maskedUid = SecuritySanitizer.maskTokenValue(openIdMatch),
            )
            webView.post {
              onCredentialsCaptured(credentials)
            }
          }
        }
        return super.shouldInterceptRequest(view, request)
      }
    }
  }

  /**
   * Attempts silent credential refresh for Genshin Cloud using existing WebView cookies.
   * Runs off-screen on Main dispatcher with a strict timeout.
   */
  suspend fun trySilentRefreshGenshin(
    context: Context,
    timeoutMs: Long = 15_000L,
  ): CapturedWebCredentials? = withTimeoutOrNull(timeoutMs) {
    withContext(Dispatchers.Main) {
      suspendCancellableCoroutine { cont ->
        val webView = WebView(context.applicationContext).apply {
          visibility = View.GONE
          layoutParams = ViewGroup.LayoutParams(1, 1)
        }
        configureSecureWebView(
          webView = webView,
          onCredentialsCaptured = { captured ->
            runCatching {
              webView.stopLoading()
              webView.destroy()
            }
            if (cont.isActive) {
              cont.resume(captured)
            }
          },
        )
        cont.invokeOnCancellation {
          runCatching {
            webView.stopLoading()
            webView.destroy()
          }
        }
        webView.loadUrl(GameType.GENSHIN.webUrl)
      }
    }
  }
}
