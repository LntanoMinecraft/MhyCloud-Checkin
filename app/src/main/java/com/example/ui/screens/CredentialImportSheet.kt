package com.example.ui.screens

import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.core.CapturedWebCredentials
import com.example.core.ClipboardParseResult
import com.example.core.RootScanAccountResult
import com.example.core.WebViewCredentialManager
import com.example.data.GameType
import com.example.data.PermissionMode
import com.example.ui.theme.LocalStatusColors
import com.example.viewmodel.ImportSheetTarget

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CredentialImportSheet(
  target: ImportSheetTarget,
  permissionMode: PermissionMode,
  isScanningRoot: Boolean,
  rootScanResults: List<RootScanAccountResult>,
  clipboardResult: ClipboardParseResult?,
  onDismiss: () -> Unit,
  onOpenHelpClick: () -> Unit,
  onScanRootClick: () -> Unit,
  onConfirmRootImport: (RootScanAccountResult, String) -> Unit,
  onWebViewCaptured: (CapturedWebCredentials, String, GameType) -> Unit,
  onParseClipboardClick: () -> Unit,
  onParseManualText: (String) -> Unit,
  onConfirmClipboardImport: (String, GameType) -> Unit,
) {
  if (!target.isVisible) return

  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var selectedGame by remember(target) { mutableStateOf(target.preselectedGame) }
  var aliasInput by remember(target) { mutableStateOf(target.initialAlias) }
  var showWebViewDialog by remember { mutableStateOf(false) }
  var clearCookiesBeforeLogin by remember { mutableStateOf(true) }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .navigationBarsPadding()
        .padding(horizontal = 20.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = if (target.repairAccountId != null) "更新失效凭证" else "导入云游戏账号",
            style = MaterialTheme.typography.headlineSmall,
          )
          Text(
            text = "当前模式：${permissionMode.label}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        FilledTonalButton(
          onClick = onOpenHelpClick,
          shape = CircleShape,
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("sheet_open_help_button"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text("不会操作？看教程", style = MaterialTheme.typography.labelLarge)
        }
      }

      if (permissionMode == PermissionMode.ROOT) {
        RootImportSection(
          aliasInput = aliasInput,
          onAliasChange = { aliasInput = it },
          isScanning = isScanningRoot,
          results = rootScanResults,
          onScanClick = onScanRootClick,
          onImportClick = { scan -> onConfirmRootImport(scan, aliasInput) },
        )
      } else {
        // Basic Mode branches by game type
        SingleChoiceSegmentedButtonRow(
          modifier = Modifier
            .fillMaxWidth()
            .testTag("import_game_selector"),
        ) {
          GameType.entries.forEachIndexed { index, game ->
            SegmentedButton(
              selected = selectedGame == game,
              onClick = { selectedGame = game },
              enabled = target.repairAccountId == null || target.preselectedGame == game,
              shape = SegmentedButtonDefaults.itemShape(index = index, count = GameType.entries.size),
              label = { Text(game.displayName) },
            )
          }
        }

        OutlinedTextField(
          value = aliasInput,
          onValueChange = { aliasInput = it },
          label = { Text("账号备注（可选）") },
          placeholder = { Text("例如：大号 / 官服主力") },
          singleLine = true,
          shape = RoundedCornerShape(18.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("import_alias_input"),
        )

        if (selectedGame == GameType.GENSHIN) {
          BasicGenshinWebViewSection(
            clearCookiesBeforeLogin = clearCookiesBeforeLogin,
            onClearCookiesToggle = { clearCookiesBeforeLogin = it },
            onLaunchWebView = { showWebViewDialog = true },
          )
        } else {
          BasicZzzClipboardSection(
            clipboardResult = clipboardResult,
            onParseClipboard = onParseClipboardClick,
            onParseManualText = onParseManualText,
            onConfirmImport = { onConfirmClipboardImport(aliasInput, GameType.ZZZ) },
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))
    }
  }

  if (showWebViewDialog) {
    IsolatedGenshinWebViewDialog(
      clearCookiesOnStart = clearCookiesBeforeLogin,
      onDismiss = { showWebViewDialog = false },
      onCredentialsCaptured = { captured ->
        showWebViewDialog = false
        onWebViewCaptured(captured, aliasInput, GameType.GENSHIN)
      },
    )
  }
}

@Composable
private fun RootImportSection(
  aliasInput: String,
  onAliasChange: (String) -> Unit,
  isScanning: Boolean,
  results: List<RootScanAccountResult>,
  onScanClick: () -> Unit,
  onImportClick: (RootScanAccountResult) -> Unit,
) {
  val statusColors = LocalStatusColors.current

  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    OutlinedTextField(
      value = aliasInput,
      onValueChange = onAliasChange,
      label = { Text("账号备注（可选）") },
      placeholder = { Text("留空则自动使用游戏名与脱敏 UID") },
      singleLine = true,
      shape = RoundedCornerShape(18.dp),
      modifier = Modifier.fillMaxWidth(),
    )

    Button(
      onClick = onScanClick,
      enabled = !isScanning,
      shape = CircleShape,
      modifier = Modifier
        .fillMaxWidth()
        .height(52.dp)
        .testTag("root_scan_button"),
    ) {
      if (isScanning) {
        CircularProgressIndicator(
          modifier = Modifier.size(20.dp),
          strokeWidth = 2.5.dp,
          color = MaterialTheme.colorScheme.onPrimary,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("正在读取官方客户端配置...")
      } else {
        Icon(imageVector = Icons.Filled.QrCodeScanner, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("一键扫描本机《云·原神》与《云·绝区零》")
      }
    }

    results.forEach { scan ->
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = scan.gameType.displayName,
              style = MaterialTheme.typography.titleMedium,
            )
            Surface(
              shape = CircleShape,
              color = if (scan.found) statusColors.successContainer else MaterialTheme.colorScheme.errorContainer,
            ) {
              Text(
                text = if (scan.found) "已识别" else "未就绪",
                style = MaterialTheme.typography.labelMedium,
                color = if (scan.found) statusColors.onSuccessContainer else MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
              )
            }
          }

          if (scan.found) {
            Text(
              text = "UID: ${scan.maskedOpenId} · 版本: ${scan.appVersion} · biz: ${scan.gameBiz}",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              text = scan.maskedComboToken,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary,
            )
            FilledTonalButton(
              onClick = { onImportClick(scan) },
              shape = CircleShape,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = null)
              Spacer(modifier = Modifier.width(6.dp))
              Text("确认同步该账号")
            }
          } else {
            Text(
              text = scan.errorReason,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.error,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun BasicGenshinWebViewSection(
  clearCookiesBeforeLogin: Boolean,
  onClearCookiesToggle: (Boolean) -> Unit,
  onLaunchWebView: () -> Unit,
) {
  Card(
    shape = RoundedCornerShape(22.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Icon(
          imageVector = Icons.Filled.Language,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
        )
        Text(
          text = "内置网页自动登录",
          style = MaterialTheme.typography.titleMedium,
        )
      }
      Text(
        text = "只需在下方网页登录一次账号，看到游戏主界面后软件会自动保存凭证并关闭页面。",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Checkbox(
          checked = clearCookiesBeforeLogin,
          onCheckedChange = onClearCookiesToggle,
        )
        Text(
          text = "登录前清理旧 Cookie 会话（添加多账号推荐开启）",
          style = MaterialTheme.typography.bodyMedium,
        )
      }

      Button(
        onClick = onLaunchWebView,
        shape = CircleShape,
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
          .testTag("launch_webview_button"),
      ) {
        Icon(imageVector = Icons.Filled.Security, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("打开《云·原神》登录并监听凭证")
      }
    }
  }
}

@Composable
private fun BasicZzzClipboardSection(
  clipboardResult: ClipboardParseResult?,
  onParseClipboard: () -> Unit,
  onParseManualText: (String) -> Unit,
  onConfirmImport: () -> Unit,
) {
  val statusColors = LocalStatusColors.current
  var showManualInput by remember { mutableStateOf(false) }
  var manualText by remember { mutableStateOf("") }

  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Card(
      shape = RoundedCornerShape(22.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text(
          text = "从电脑抓包结果一键导入",
          style = MaterialTheme.typography.titleMedium,
        )
        Text(
          text = "请先在电脑端抓包复制包含 wallet 的 cURL 记录发送到手机，再点击下方按钮一键识别。",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Button(
          onClick = onParseClipboard,
          shape = CircleShape,
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("parse_clipboard_button"),
        ) {
          Icon(imageVector = Icons.Filled.ContentPaste, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text("从剪贴板一键识别")
        }

        OutlinedButton(
          onClick = { showManualInput = !showManualInput },
          shape = CircleShape,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(if (showManualInput) "收起手动粘贴框" else "改为手动粘贴请求头文本")
        }

        AnimatedVisibility(visible = showManualInput) {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = manualText,
              onValueChange = { manualText = it },
              label = { Text("粘贴 cURL 或 Header 文本") },
              minLines = 3,
              maxLines = 6,
              shape = RoundedCornerShape(16.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("manual_header_input"),
            )
            FilledTonalButton(
              onClick = { onParseManualText(manualText) },
              shape = CircleShape,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text("解析输入文本")
            }
          }
        }
      }
    }

    if (clipboardResult != null) {
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
          containerColor = if (clipboardResult.success) {
            statusColors.successContainer.copy(alpha = 0.55f)
          } else {
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.65f)
          },
        ),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              imageVector = if (clipboardResult.success) Icons.Filled.CheckCircle else Icons.Filled.WarningAmber,
              contentDescription = null,
              tint = if (clipboardResult.success) statusColors.onSuccessContainer else MaterialTheme.colorScheme.onErrorContainer,
            )
            Text(
              text = if (clipboardResult.success) "字段解析与脱敏预览" else "解析失败",
              style = MaterialTheme.typography.titleMedium,
            )
          }

          if (clipboardResult.success) {
            Text(
              text = "combo_token: ${clipboardResult.maskedComboToken}",
              style = MaterialTheme.typography.labelSmall,
            )
            Text(
              text = buildString {
                append("完整性校验：")
                append(if (clipboardResult.hasComboToken) "✔ combo_token  " else "✘ combo_token  ")
                append(if (clipboardResult.hasDeviceId) "✔ device_id  " else "○ device_id  ")
                append(if (clipboardResult.hasClientType) "✔ client_type  " else "○ client_type  ")
                append(if (clipboardResult.hasAppVersion) "✔ app_version" else "○ app_version")
              },
              style = MaterialTheme.typography.bodyMedium,
            )
            if (clipboardResult.clipboardCleared) {
              Text(
                text = "✔ 系统剪贴板已安全清理",
                style = MaterialTheme.typography.labelMedium,
                color = statusColors.onSuccessContainer,
              )
            }
            Button(
              onClick = onConfirmImport,
              shape = CircleShape,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("confirm_clipboard_import_btn"),
            ) {
              Text("确认保存凭证")
            }
          } else {
            Text(
              text = clipboardResult.errorMessage,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onErrorContainer,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun IsolatedGenshinWebViewDialog(
  clearCookiesOnStart: Boolean,
  onDismiss: () -> Unit,
  onCredentialsCaptured: (CapturedWebCredentials) -> Unit,
) {
  var currentUrl by remember { mutableStateOf(GameType.GENSHIN.webUrl) }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .navigationBarsPadding(),
      color = MaterialTheme.colorScheme.background,
    ) {
      Column(modifier = Modifier.fillMaxSize()) {
        // Listening status banner
        Surface(
          color = MaterialTheme.colorScheme.primaryContainer,
          tonalElevation = 2.dp,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              modifier = Modifier.weight(1f),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
              )
              Column {
                Text(
                  text = "正在监听登录凭证...",
                  style = MaterialTheme.typography.titleMedium,
                  color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                  text = currentUrl,
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                  maxLines = 1,
                )
              }
            }
            IconButton(
              onClick = onDismiss,
              modifier = Modifier.minimumInteractiveComponentSize(),
            ) {
              Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "关闭登录窗口",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
              )
            }
          }
        }

        var webViewInstance by remember { mutableStateOf<WebView?>(null) }

        DisposableEffect(Unit) {
          onDispose {
            webViewInstance?.apply {
              stopLoading()
              destroy()
            }
          }
        }

        Box(modifier = Modifier.weight(1f)) {
          AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
              if (clearCookiesOnStart) {
                WebViewCredentialManager.clearCookiesForFreshLogin()
              }
              WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                  ViewGroup.LayoutParams.MATCH_PARENT,
                  ViewGroup.LayoutParams.MATCH_PARENT,
                )
                WebViewCredentialManager.configureSecureWebView(
                  webView = this,
                  onCredentialsCaptured = onCredentialsCaptured,
                  onPageChanged = { url -> currentUrl = url },
                )
                loadUrl(GameType.GENSHIN.webUrl)
                webViewInstance = this
              }
            },
          )
        }
      }
    }
  }
}
