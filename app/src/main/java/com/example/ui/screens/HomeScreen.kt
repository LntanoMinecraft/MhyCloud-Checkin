package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.AccountStatus
import com.example.data.AccountWithState
import com.example.data.AppGlobalConfig
import com.example.data.CredentialSource
import com.example.data.GameType
import com.example.data.PermissionMode
import com.example.scheduler.ScheduleManager
import com.example.ui.components.BrandCloudEmblem
import com.example.ui.components.GameEmblemBadge
import com.example.ui.theme.LocalStatusColors

@Composable
fun HomeScreen(
  accounts: List<AccountWithState>,
  config: AppGlobalConfig,
  onAddAccountClick: () -> Unit,
  onSingleCheckinClick: (String) -> Unit,
  onRepairCredentialClick: (String) -> Unit,
  onResumeFromCapClick: (String) -> Unit,
  onToggleEnabledClick: (String) -> Unit,
  onDeleteAccountClick: (String) -> Unit,
  onBuildExportHeaders: (String) -> String?,
  onBuildExportCurl: (String) -> String?,
  onBuildMaskedExportPreview: (String) -> String,
  onShowMessage: (String) -> Unit,
) {
  var pendingDeleteAccount by remember { mutableStateOf<AccountWithState?>(null) }
  var pendingExportAccount by remember { mutableStateOf<AccountWithState?>(null) }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .statusBarsPadding(),
    contentAlignment = Alignment.TopCenter,
  ) {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 640.dp),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 164.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      item(key = "header_title") {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = MaterialTheme.colorScheme.primaryContainer,
              modifier = Modifier.size(38.dp),
            ) {
              Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(7.dp)) {
                BrandCloudEmblem(
                  modifier = Modifier.fillMaxSize(),
                  tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
              }
            }
            Text(
              text = "云签助手",
              style = MaterialTheme.typography.headlineMedium,
              color = MaterialTheme.colorScheme.onBackground,
            )
          }

          FilledTonalButton(
            onClick = onAddAccountClick,
            shape = CircleShape,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .testTag("add_account_button"),
          ) {
            Icon(
              imageVector = Icons.Filled.Add,
              contentDescription = "添加账号",
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("添加", style = MaterialTheme.typography.labelLarge)
          }
        }
      }

      item(key = "overview_hero") {
        OverviewHeroCard(
          accounts = accounts,
          config = config,
        )
      }

      if (accounts.isEmpty()) {
        item(key = "empty_state") {
          EmptyAccountsCard(onAddClick = onAddAccountClick)
        }
      } else {
        items(
          items = accounts,
          key = { it.profile.accountId },
        ) { item ->
          AccountCard(
            item = item,
            maxFreeMinutes = config.maxFreeMinutes,
            onCheckinNow = { onSingleCheckinClick(item.profile.accountId) },
            onRepairCredential = { onRepairCredentialClick(item.profile.accountId) },
            onResumeFromCap = { onResumeFromCapClick(item.profile.accountId) },
            onToggleEnabled = { onToggleEnabledClick(item.profile.accountId) },
            onExportTokenClick = { pendingExportAccount = item },
            onDeleteClick = { pendingDeleteAccount = item },
          )
        }
      }
    }
  }

  pendingDeleteAccount?.let { target ->
    AlertDialog(
      onDismissRequest = { pendingDeleteAccount = null },
      icon = {
        Icon(
          imageVector = Icons.Filled.DeleteOutline,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.error,
        )
      },
      title = { Text("移除「${target.profile.alias}」") },
      text = { Text("将清除本地加密存储的凭证。") },
      confirmButton = {
        Button(
          onClick = {
            onDeleteAccountClick(target.profile.accountId)
            pendingDeleteAccount = null
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
          ),
        ) {
          Text("移除")
        }
      },
      dismissButton = {
        TextButton(onClick = { pendingDeleteAccount = null }) {
          Text("取消")
        }
      },
    )
  }

  pendingExportAccount?.let { target ->
    ExportTokenDialog(
      account = target,
      maskedPreview = remember(target.profile.accountId, target.profile.updatedAt) {
        onBuildMaskedExportPreview(target.profile.accountId)
      },
      onCopyStandardHeaders = {
        onBuildExportHeaders(target.profile.accountId)
      },
      onCopyCurlCommand = {
        onBuildExportCurl(target.profile.accountId)
      },
      onCopied = {
        onShowMessage("已复制到剪贴板")
        pendingExportAccount = null
      },
      onDismiss = { pendingExportAccount = null },
    )
  }
}

@Composable
private fun OverviewHeroCard(
  accounts: List<AccountWithState>,
  config: AppGlobalConfig,
) {
  val total = accounts.size
  val completedToday = accounts.count {
    it.state.status == AccountStatus.DONE_TODAY || it.state.status == AccountStatus.PAUSED_CAP_REACHED
  }
  val allPausedCap = accounts.isNotEmpty() && accounts.all {
    it.state.status == AccountStatus.PAUSED_CAP_REACHED
  }

  val progressTarget = if (total > 0) completedToday.toFloat() / total.toFloat() else 0f
  val animatedProgress by animateFloatAsState(
    targetValue = progressTarget,
    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
    label = "overview_progress",
  )

  val primaryContainer = MaterialTheme.colorScheme.primaryContainer
  val tertiaryContainer = MaterialTheme.colorScheme.tertiaryContainer

  Card(
    shape = RoundedCornerShape(28.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("overview_hero_card"),
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .drawBehind {
          drawRect(
            brush = Brush.linearGradient(
              colors = listOf(
                primaryContainer.copy(alpha = 0.78f),
                tertiaryContainer.copy(alpha = 0.42f),
              ),
              start = Offset.Zero,
              end = Offset(size.width, size.height),
            ),
          )
        }
        .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.Top,
        ) {
          Column {
            Text(
              text = "今日签到",
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.Bottom) {
              Text(
                text = "$completedToday/$total",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "已完成",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                modifier = Modifier.padding(bottom = 6.dp),
              )
            }
          }

          Surface(
            shape = CircleShape,
            color = if (config.permissionMode == PermissionMode.ROOT) {
              MaterialTheme.colorScheme.secondaryContainer
            } else {
              MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
            },
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
              Icon(
                imageVector = Icons.Filled.Security,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.primary,
              )
              Text(
                text = config.permissionMode.label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
              )
            }
          }
        }

        LinearProgressIndicator(
          progress = { animatedProgress },
          modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(CircleShape),
          color = MaterialTheme.colorScheme.primary,
          trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.52f),
          strokeCap = StrokeCap.Round,
        )

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Icon(
            imageVector = if (allPausedCap) Icons.Filled.PauseCircle else Icons.Filled.Schedule,
            contentDescription = null,
            modifier = Modifier.size(15.dp),
            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f),
          )
          Text(
            text = when {
              accounts.isEmpty() -> "待添加账号"
              allPausedCap -> "已全部达上限暂停，消耗时长后可恢复"
              config.nextScheduledEpochMillis > 0L ->
                "下次 ${ScheduleManager.formatNextScheduleDisplay(config.nextScheduledEpochMillis)}"
              else -> "调度已暂停"
            },
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.88f),
          )
        }
      }
    }
  }
}

@Composable
private fun AccountCard(
  item: AccountWithState,
  maxFreeMinutes: Int,
  onCheckinNow: () -> Unit,
  onRepairCredential: () -> Unit,
  onResumeFromCap: () -> Unit,
  onToggleEnabled: () -> Unit,
  onExportTokenClick: () -> Unit,
  onDeleteClick: () -> Unit,
) {
  var menuExpanded by remember { mutableStateOf(false) }
  val statusColors = LocalStatusColors.current
  val profile = item.profile
  val state = item.state

  val isNeedReauth = state.status == AccountStatus.NEED_REAUTH
  val isPausedCap = state.status == AccountStatus.PAUSED_CAP_REACHED

  val gameAccent = if (profile.gameType == GameType.GENSHIN) {
    statusColors.genshinAccent
  } else {
    statusColors.zzzAccent
  }

  val ratio = if (maxFreeMinutes > 0) {
    (state.freeMinutes.toFloat() / maxFreeMinutes.toFloat()).coerceIn(0f, 1f)
  } else {
    0f
  }
  val animatedRatio by animateFloatAsState(
    targetValue = ratio,
    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
    label = "free_time_ratio",
  )

  val shortSourceLabel = when (profile.credentialSource) {
    CredentialSource.ROOT_READ -> "Root"
    CredentialSource.WEBVIEW -> "WebView"
    CredentialSource.CLIPBOARD -> "剪贴板"
  }

  Card(
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ),
    border = if (isNeedReauth) {
      BorderStroke(1.5.dp, MaterialTheme.colorScheme.error)
    } else {
      BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    },
    modifier = Modifier
      .fillMaxWidth()
      .testTag("account_card_${profile.accountId}"),
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      // Top row: Geometric Game Emblem, Alias, Concise Status Pill
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          GameEmblemBadge(gameType = profile.gameType, size = 42.dp)

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = profile.alias,
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = buildString {
                append(profile.gameType.displayName)
                if (profile.maskedUid.isNotBlank()) {
                  append(" · ")
                  append(profile.maskedUid)
                }
              },
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }

        Spacer(modifier = Modifier.width(8.dp))
        AccountStatusCapsule(status = state.status)
      }

      // Free duration metric + progress bar
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.Bottom,
        ) {
          Row(verticalAlignment = Alignment.Bottom) {
            Text(
              text = "${state.freeMinutes}",
              style = MaterialTheme.typography.headlineMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
              text = "/ $maxFreeMinutes 分钟",
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(bottom = 4.dp),
            )
          }

          if (state.lastDeltaMinutes > 0) {
            Surface(
              shape = CircleShape,
              color = statusColors.successContainer,
            ) {
              Text(
                text = "+${state.lastDeltaMinutes}m",
                style = MaterialTheme.typography.labelMedium,
                color = statusColors.onSuccessContainer,
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
              )
            }
          }
        }

        LinearProgressIndicator(
          progress = { animatedRatio },
          modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(CircleShape),
          color = if (isPausedCap) statusColors.warningAccent else gameAccent,
          trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
          strokeCap = StrokeCap.Round,
        )
      }

      // Concise single-line error hint only if present
      AnimatedVisibility(visible = state.lastErrorMsg.isNotBlank()) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Icon(
            imageVector = Icons.Filled.WarningAmber,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.error,
          )
          Text(
            text = state.lastErrorMsg,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.error,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }

      // Footer controls
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
          Text(
            text = shortSourceLabel,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
          )
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          if (!isPausedCap) {
            Switch(
              checked = profile.isEnabled,
              onCheckedChange = { onToggleEnabled() },
              modifier = Modifier.testTag("toggle_account_${profile.accountId}"),
            )
          }

          when {
            isNeedReauth -> {
              Button(
                onClick = onRepairCredential,
                shape = CircleShape,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = MaterialTheme.colorScheme.error,
                  contentColor = MaterialTheme.colorScheme.onError,
                ),
                modifier = Modifier
                  .minimumInteractiveComponentSize()
                  .testTag("repair_btn_${profile.accountId}"),
              ) {
                Icon(
                  imageVector = Icons.Filled.Key,
                  contentDescription = null,
                  modifier = Modifier.size(15.dp),
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text("更新凭证", style = MaterialTheme.typography.labelLarge)
              }
            }

            isPausedCap -> {
              FilledTonalButton(
                onClick = onResumeFromCap,
                shape = CircleShape,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                  containerColor = statusColors.warningContainer,
                  contentColor = statusColors.onWarningContainer,
                ),
                modifier = Modifier
                  .minimumInteractiveComponentSize()
                  .testTag("resume_btn_${profile.accountId}"),
              ) {
                Icon(
                  imageVector = Icons.Filled.PlayArrow,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("恢复", style = MaterialTheme.typography.labelLarge)
              }
            }

            else -> {
              FilledTonalIconButton(
                onClick = onCheckinNow,
                enabled = state.status != AccountStatus.CHECKING,
                modifier = Modifier
                  .minimumInteractiveComponentSize()
                  .testTag("checkin_single_btn_${profile.accountId}"),
              ) {
                Icon(
                  imageVector = Icons.Filled.Refresh,
                  contentDescription = "立即同步",
                  modifier = Modifier.size(18.dp),
                )
              }
            }
          }

          Box {
            IconButton(
              onClick = { menuExpanded = true },
              modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("account_menu_btn_${profile.accountId}"),
            ) {
              Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = "更多操作",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
              )
            }

            DropdownMenu(
              expanded = menuExpanded,
              onDismissRequest = { menuExpanded = false },
            ) {
              DropdownMenuItem(
                text = { Text("导出令牌") },
                leadingIcon = {
                  Icon(
                    imageVector = Icons.Filled.IosShare,
                    contentDescription = null,
                  )
                },
                onClick = {
                  menuExpanded = false
                  onExportTokenClick()
                },
                modifier = Modifier.testTag("export_token_menu_${profile.accountId}"),
              )
              DropdownMenuItem(
                text = {
                  Text(
                    text = "删除账号",
                    color = MaterialTheme.colorScheme.error,
                  )
                },
                leadingIcon = {
                  Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                  )
                },
                onClick = {
                  menuExpanded = false
                  onDeleteClick()
                },
                modifier = Modifier.testTag("delete_account_menu_${profile.accountId}"),
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ExportTokenDialog(
  account: AccountWithState,
  maskedPreview: String,
  onCopyStandardHeaders: () -> String?,
  onCopyCurlCommand: () -> String?,
  onCopied: () -> Unit,
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  var previewExpanded by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(
        imageVector = Icons.Filled.IosShare,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
      )
    },
    title = {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
      ) {
        Text(
          text = "导出令牌",
          style = MaterialTheme.typography.headlineSmall,
        )
        Text(
          text = "${account.profile.gameType.displayName} · ${account.profile.alias}",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.errorContainer,
          contentColor = MaterialTheme.colorScheme.onErrorContainer,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top,
          ) {
            Icon(
              imageVector = Icons.Filled.WarningAmber,
              contentDescription = null,
              modifier = Modifier
                .padding(top = 1.dp)
                .size(18.dp),
            )
            Text(
              text = "以下内容包含敏感登录凭证，仅用于迁移到您自己的其他设备或备份，请勿泄露给他人",
              style = MaterialTheme.typography.bodySmall,
            )
          }
        }

        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surfaceContainerHigh,
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { previewExpanded = !previewExpanded }
            .testTag("export_preview_toggle"),
        ) {
          Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = if (previewExpanded) "收起脱敏请求头预览" else "展开脱敏请求头预览",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Icon(
                imageVector = if (previewExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
              )
            }

            AnimatedVisibility(visible = previewExpanded) {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .heightIn(max = 150.dp)
                  .verticalScroll(rememberScrollState()),
              ) {
                Text(
                  text = maskedPreview.ifBlank { "暂无可预览内容" },
                  style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Button(
            onClick = {
              val payload = onCopyStandardHeaders()
              if (!payload.isNullOrBlank()) {
                copyTextToClipboard(context, "cloud_checkin_headers", payload)
                onCopied()
              }
            },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .fillMaxWidth()
              .minimumInteractiveComponentSize()
              .testTag("copy_standard_headers_btn"),
          ) {
            Icon(
              imageVector = Icons.Filled.ContentCopy,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "复制标准请求头（推荐，跨设备导入本软件）",
              style = MaterialTheme.typography.labelLarge,
            )
          }

          FilledTonalButton(
            onClick = {
              val curlPayload = onCopyCurlCommand()
              if (!curlPayload.isNullOrBlank()) {
                copyTextToClipboard(context, "cloud_checkin_curl", curlPayload)
                onCopied()
              }
            },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .fillMaxWidth()
              .minimumInteractiveComponentSize()
              .testTag("copy_curl_command_btn"),
          ) {
            Icon(
              imageVector = Icons.Filled.Terminal,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "复制完整 cURL 命令",
              style = MaterialTheme.typography.labelLarge,
            )
          }
        }
      }
    },
    confirmButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.minimumInteractiveComponentSize(),
      ) {
        Text("关闭")
      }
    },
  )
}

private fun copyTextToClipboard(context: Context, label: String, text: String) {
  val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
  clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}

@Composable
private fun AccountStatusCapsule(status: AccountStatus) {
  val statusColors = LocalStatusColors.current

  val (bg, fg, text) = when (status) {
    AccountStatus.DONE_TODAY -> Triple(
      statusColors.successContainer,
      statusColors.onSuccessContainer,
      "已完成",
    )
    AccountStatus.READY, AccountStatus.PENDING_INIT -> Triple(
      MaterialTheme.colorScheme.surfaceContainerHigh,
      MaterialTheme.colorScheme.onSurfaceVariant,
      "待签到",
    )
    AccountStatus.CHECKING -> Triple(
      MaterialTheme.colorScheme.primaryContainer,
      MaterialTheme.colorScheme.onPrimaryContainer,
      "同步中",
    )
    AccountStatus.PAUSED_CAP_REACHED -> Triple(
      statusColors.warningContainer,
      statusColors.onWarningContainer,
      "已达上限",
    )
    AccountStatus.NEED_REAUTH -> Triple(
      MaterialTheme.colorScheme.errorContainer,
      MaterialTheme.colorScheme.onErrorContainer,
      "需更新",
    )
  }

  Surface(
    shape = CircleShape,
    color = bg,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      when (status) {
        AccountStatus.CHECKING -> {
          CircularProgressIndicator(
            modifier = Modifier.size(12.dp),
            strokeWidth = 2.dp,
            color = fg,
          )
        }
        AccountStatus.DONE_TODAY -> {
          Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = fg,
            modifier = Modifier.size(13.dp),
          )
        }
        AccountStatus.PAUSED_CAP_REACHED -> {
          Icon(
            imageVector = Icons.Filled.PauseCircle,
            contentDescription = null,
            tint = fg,
            modifier = Modifier.size(13.dp),
          )
        }
        AccountStatus.NEED_REAUTH -> {
          Icon(
            imageVector = Icons.Filled.WarningAmber,
            contentDescription = null,
            tint = fg,
            modifier = Modifier.size(13.dp),
          )
        }
        else -> {}
      }
      Text(
        text = text,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
        color = fg,
      )
    }
  }
}

@Composable
private fun EmptyAccountsCard(onAddClick: () -> Unit) {
  Card(
    shape = RoundedCornerShape(28.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp, vertical = 36.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.size(60.dp),
      ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(12.dp)) {
          BrandCloudEmblem(
            modifier = Modifier.fillMaxSize(),
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
          )
        }
      }
      Text(
        text = "暂无账号",
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = "导入《云·原神》或《云·绝区零》账号开始托管",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(modifier = Modifier.height(2.dp))
      Button(
        onClick = onAddClick,
        shape = CircleShape,
        modifier = Modifier
          .minimumInteractiveComponentSize()
          .testTag("empty_add_account_btn"),
      ) {
        Icon(imageVector = Icons.Filled.Add, contentDescription = null)
        Spacer(modifier = Modifier.width(6.dp))
        Text("导入账号")
      }
    }
  }
}
