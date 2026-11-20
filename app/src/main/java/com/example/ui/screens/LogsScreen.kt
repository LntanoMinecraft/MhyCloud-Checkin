package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.CheckinLogEntry
import com.example.data.EncryptedStorageRepository
import com.example.ui.theme.LocalStatusColors
import java.time.Instant
import java.time.format.DateTimeFormatter

private enum class LogFilter(val label: String) {
  ALL("全部"),
  ERRORS_ONLY("仅异常"),
}

@Composable
fun LogsScreen(
  logs: List<CheckinLogEntry>,
  onBuildDiagnosticText: () -> String,
  onClearLogs: () -> Unit,
  onCopiedMessage: (String) -> Unit,
) {
  var filter by remember { mutableStateOf(LogFilter.ALL) }
  var expandedLogId by remember { mutableStateOf<String?>(null) }
  val clipboardManager = LocalClipboardManager.current
  val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

  val filteredLogs = remember(logs, filter) {
    when (filter) {
      LogFilter.ALL -> logs
      LogFilter.ERRORS_ONLY -> logs.filter { it.isError || it.pausedDueToCap }
    }
  }

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
      contentPadding = PaddingValues(
        start = 16.dp,
        end = 16.dp,
        top = 12.dp,
        bottom = navBarBottom + 112.dp,
      ),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item(key = "logs_header") {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "运行日志",
              style = MaterialTheme.typography.headlineMedium,
            )
            Text(
              text = "本地保留最近 ${EncryptedStorageRepository.MAX_LOG_COUNT} 条 · 已自动脱敏",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalIconButton(
              onClick = {
                clipboardManager.setText(AnnotatedString(onBuildDiagnosticText()))
                onCopiedMessage("已复制脱敏诊断日志到剪贴板")
              },
              modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("copy_logs_button"),
            ) {
              Icon(
                imageVector = Icons.Filled.ContentCopy,
                contentDescription = "复制脱敏诊断日志",
              )
            }

            FilledTonalIconButton(
              onClick = onClearLogs,
              enabled = logs.isNotEmpty(),
              modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("clear_logs_button"),
            ) {
              Icon(
                imageVector = Icons.Filled.DeleteSweep,
                contentDescription = "清空日志",
              )
            }
          }
        }
      }

      item(key = "logs_filter") {
        SingleChoiceSegmentedButtonRow(
          modifier = Modifier
            .fillMaxWidth()
            .testTag("logs_filter_row"),
        ) {
          LogFilter.entries.forEachIndexed { index, entry ->
            SegmentedButton(
              selected = filter == entry,
              onClick = { filter = entry },
              shape = SegmentedButtonDefaults.itemShape(index = index, count = LogFilter.entries.size),
              label = { Text(entry.label) },
            )
          }
        }
      }

      if (filteredLogs.isEmpty()) {
        item(key = "logs_empty") {
          Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Icon(
                imageVector = Icons.Filled.ReceiptLong,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(36.dp),
              )
              Text(
                text = if (filter == LogFilter.ALL) "暂无签到执行记录" else "暂无异常或暂停记录",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text = "执行一次一键签到或等待定时调度触发后，可在此展开查看完整链路。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      } else {
        items(
          items = filteredLogs,
          key = { it.id },
        ) { log ->
          CollapsibleLogRowCard(
            entry = log,
            expanded = expandedLogId == log.id,
            onToggleExpand = {
              expandedLogId = if (expandedLogId == log.id) null else log.id
            },
          )
        }
      }
    }
  }
}

@Composable
private fun CollapsibleLogRowCard(
  entry: CheckinLogEntry,
  expanded: Boolean,
  onToggleExpand: () -> Unit,
) {
  val statusColors = LocalStatusColors.current
  val timeStr = remember(entry.timestamp) {
    Instant.ofEpochMilli(entry.timestamp)
      .atZone(EncryptedStorageRepository.BEIJING_ZONE)
      .format(DateTimeFormatter.ofPattern("MM-dd HH:mm"))
  }

  val badgeBg = when {
    entry.isError -> MaterialTheme.colorScheme.errorContainer
    entry.pausedDueToCap -> statusColors.warningContainer
    else -> statusColors.successContainer
  }
  val badgeFg = when {
    entry.isError -> MaterialTheme.colorScheme.onErrorContainer
    entry.pausedDueToCap -> statusColors.onWarningContainer
    else -> statusColors.onSuccessContainer
  }

  val badgeTitle = remember(entry.resultTitle) {
    compactLogBadgeTitle(entry.resultTitle)
  }

  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ),
    border = if (entry.isError) {
      BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f))
    } else {
      null
    },
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(20.dp))
      .clickable(onClick = onToggleExpand)
      .testTag("log_card_${entry.id}"),
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      // Single-line summary: [时间] [游戏] [结果] [时长变化]
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text(
          text = timeStr,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          softWrap = false,
        )

        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
          Text(
            text = entry.gameType.shortTag,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            softWrap = false,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
          )
        }

        Box(
          modifier = Modifier.weight(1f),
          contentAlignment = Alignment.CenterStart,
        ) {
          Surface(
            shape = CircleShape,
            color = badgeBg,
          ) {
            Text(
              text = badgeTitle,
              style = MaterialTheme.typography.labelMedium,
              color = badgeFg,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
          }
        }

        Text(
          text = if (entry.deltaMinutes > 0) "+${entry.deltaMinutes}m" else "±0m",
          style = MaterialTheme.typography.labelLarge,
          color = if (entry.deltaMinutes > 0) statusColors.successAccent else MaterialTheme.colorScheme.onSurfaceVariant,
          softWrap = false,
        )

        Icon(
          imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
          contentDescription = if (expanded) "收起详情" else "展开详情",
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(20.dp),
        )
      }

      AnimatedVisibility(visible = expanded) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            MetaTagChip(text = "账号: ${entry.accountAlias}")
            MetaTagChip(text = "HTTP: ${entry.httpStatusCode ?: "-"}")
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            MetaTagChip(text = if (entry.usedBackupRenewal) "触发备用续期: 是" else "触发备用续期: 否")
            MetaTagChip(text = if (entry.pausedDueToCap) "达上限暂停: 是" else "达上限暂停: 否")
          }

          Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = entry.detailChain,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(12.dp),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun MetaTagChip(text: String) {
  Surface(
    shape = CircleShape,
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
    )
  }
}

private fun compactLogBadgeTitle(rawTitle: String): String {
  return when {
    rawTitle.contains("今日已完成") || rawTitle.contains("首次基准同步") -> "已完成"
    rawTitle.contains("签到成功") -> "签到成功"
    rawTitle.contains("已达上限") -> "已达上限"
    rawTitle.contains("凭证失效") -> "凭证失效"
    rawTitle.contains("未检测到时长增加") -> "未增时长"
    rawTitle.contains("网络异常") -> "网络异常"
    else -> rawTitle
  }
}
