package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.example.core.RootProbeResult
import com.example.data.AppGlobalConfig
import com.example.data.PermissionMode
import com.example.data.ThemePreference
import com.example.ui.theme.LocalStatusColors
import com.example.ui.theme.ThemeSeedPreset
import com.example.ui.theme.ThemeSeedPresets
import com.example.viewmodel.SystemPermissionStatus
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
  config: AppGlobalConfig,
  isProbingRoot: Boolean,
  lastRootProbe: RootProbeResult?,
  permissionStatus: SystemPermissionStatus,
  onRefreshPermissions: () -> Unit,
  onSwitchPermissionMode: (PermissionMode) -> Unit,
  onUpdateScheduleTime: (Int, Int) -> Unit,
  onUpdateJitterMinutes: (Int) -> Unit,
  onUpdateMaxFreeMinutes: (Int) -> Unit,
  onToggleAutoBackupRenewal: (Boolean) -> Unit,
  onToggleNotifications: (Boolean) -> Unit,
  onToggleDynamicColor: (Boolean) -> Unit,
  onSelectThemeSeedColor: (String) -> Unit,
  onUpdateThemePreference: (ThemePreference) -> Unit,
  onOpenImportHelp: () -> Unit,
) {
  val context = LocalContext.current
  var showTimePicker by remember { mutableStateOf(false) }

  LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
    onRefreshPermissions()
  }

  val notificationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission(),
  ) {
    onRefreshPermissions()
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
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      item(key = "settings_header") {
        Column(modifier = Modifier.padding(horizontal = 4.dp)) {
          Text(
            text = "设置与调度",
            style = MaterialTheme.typography.headlineMedium,
          )
          Text(
            text = "权限工作模式、每日防检测调度、时长上限与主题外观",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      // 1. Permission Mode Card
      item(key = "permission_mode_card") {
        PermissionModeCard(
          currentMode = config.permissionMode,
          isProbingRoot = isProbingRoot,
          lastRootProbe = lastRootProbe,
          onSelectMode = onSwitchPermissionMode,
        )
      }

      // 2. Schedule, Cap & Notification Card
      item(key = "schedule_cap_card") {
        ScheduleAndCapCard(
          config = config,
          onPickTimeClick = { showTimePicker = true },
          onUpdateJitterMinutes = onUpdateJitterMinutes,
          onUpdateMaxFreeMinutes = onUpdateMaxFreeMinutes,
          onToggleAutoBackupRenewal = onToggleAutoBackupRenewal,
          onToggleNotifications = onToggleNotifications,
        )
      }

      // 3. Appearance Card (Dynamic Color, Custom Seed Palette & Light/Dark Theme)
      item(key = "appearance_card") {
        AppearanceCard(
          useDynamicColor = config.useDynamicColor,
          themeSeedColorHex = config.themeSeedColorHex,
          themePreference = config.themePreference,
          onToggleDynamicColor = onToggleDynamicColor,
          onSelectThemeSeedColor = onSelectThemeSeedColor,
          onSelectThemePreference = onUpdateThemePreference,
        )
      }

      // 4. Help & Tutorial Card
      item(key = "help_guide_card") {
        HelpGuideEntryCard(onClick = onOpenImportHelp)
      }

      // 5. System Permissions Self-Check Card
      item(key = "system_permissions_card") {
        SystemPermissionSelfCheckCard(
          status = permissionStatus,
          onRequestNotification = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !permissionStatus.notificationsGranted) {
              notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
              val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
              }
              context.startActivity(intent)
            }
          },
          onOpenBatterySettings = {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            context.startActivity(intent)
          },
          onOpenExactAlarmSettings = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
              val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${context.packageName}")
              }
              context.startActivity(intent)
            }
          },
        )
      }
    }
  }

  if (showTimePicker) {
    val timePickerState = rememberTimePickerState(
      initialHour = config.scheduleHour,
      initialMinute = config.scheduleMinute,
      is24Hour = true,
    )
    AlertDialog(
      onDismissRequest = { showTimePicker = false },
      title = { Text("设置每日基准签到时间") },
      text = {
        Box(
          modifier = Modifier.fillMaxWidth(),
          contentAlignment = Alignment.Center,
        ) {
          TimePicker(state = timePickerState)
        }
      },
      confirmButton = {
        Button(
          onClick = {
            onUpdateScheduleTime(timePickerState.hour, timePickerState.minute)
            showTimePicker = false
          },
        ) {
          Text("确认")
        }
      },
      dismissButton = {
        TextButton(onClick = { showTimePicker = false }) {
          Text("取消")
        }
      },
    )
  }
}

@Composable
private fun PermissionModeCard(
  currentMode: PermissionMode,
  isProbingRoot: Boolean,
  lastRootProbe: RootProbeResult?,
  onSelectMode: (PermissionMode) -> Unit,
) {
  val statusColors = LocalStatusColors.current
  Card(
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(
          imageVector = Icons.Filled.Security,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
        )
        Text(
          text = "运行权限模式",
          style = MaterialTheme.typography.titleMedium,
        )
      }

      Text(
        text = "Root 模式支持一键直读官方客户端配置与写入 Doze 白名单；切换时会实时探针校验 su 授权。",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      SingleChoiceSegmentedButtonRow(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("permission_mode_selector"),
      ) {
        PermissionMode.entries.forEachIndexed { index, mode ->
          SegmentedButton(
            selected = currentMode == mode,
            enabled = !isProbingRoot,
            onClick = { onSelectMode(mode) },
            shape = SegmentedButtonDefaults.itemShape(index = index, count = PermissionMode.entries.size),
            label = { Text(mode.label) },
          )
        }
      }

      AnimatedVisibility(visible = isProbingRoot) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
          Text(
            text = "正在探测 su 授权并写入 Doze 白名单...",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
          )
        }
      }

      if (lastRootProbe != null && !isProbingRoot) {
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = if (lastRootProbe.isRooted) statusColors.successContainer else MaterialTheme.colorScheme.errorContainer,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = lastRootProbe.detailMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = if (lastRootProbe.isRooted) statusColors.onSuccessContainer else MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(12.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun ScheduleAndCapCard(
  config: AppGlobalConfig,
  onPickTimeClick: () -> Unit,
  onUpdateJitterMinutes: (Int) -> Unit,
  onUpdateMaxFreeMinutes: (Int) -> Unit,
  onToggleAutoBackupRenewal: (Boolean) -> Unit,
  onToggleNotifications: (Boolean) -> Unit,
) {
  Card(
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(
          imageVector = Icons.Filled.Schedule,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
        )
        Text(
          text = "调度、上限与通知策略",
          style = MaterialTheme.typography.titleMedium,
        )
      }

      // Base schedule time
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text("每日基准签到时间", style = MaterialTheme.typography.bodyLarge)
          Text(
            text = "将在基准时间 + 随机抖动后触发加急任务",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        FilledTonalButton(
          onClick = onPickTimeClick,
          shape = CircleShape,
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("pick_schedule_time_button"),
        ) {
          Text(String.format("%02d:%02d", config.scheduleHour, config.scheduleMinute))
        }
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

      // Jitter slider
      Column {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text("随机抖动延迟范围", style = MaterialTheme.typography.bodyLarge)
          Text(
            text = "0 ~ ${config.jitterMinutes} 分钟",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
          )
        }
        Slider(
          value = config.jitterMinutes.toFloat(),
          onValueChange = { onUpdateJitterMinutes(it.roundToInt()) },
          valueRange = 0f..30f,
          steps = 29,
          modifier = Modifier.testTag("jitter_slider"),
        )
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

      // Free time cap threshold
      Column {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text("免费时长上限暂停阈值", style = MaterialTheme.typography.bodyLarge)
          Text(
            text = "${config.maxFreeMinutes} 分钟",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
          )
        }
        Text(
          text = "达到上限后自动暂停该账号，全部暂停时释放后台调度",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Slider(
          value = config.maxFreeMinutes.toFloat(),
          onValueChange = {
            val snapped = ((it / 30f).roundToInt() * 30).coerceIn(300, 1200)
            onUpdateMaxFreeMinutes(snapped)
          },
          valueRange = 300f..1200f,
          steps = 29,
          modifier = Modifier.testTag("cap_threshold_slider"),
        )
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

      // Auto backup renewal switch
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text("自动备用续期", style = MaterialTheme.typography.bodyLarge)
          Text(
            text = "主方案鉴权失败时自动重读 Root 配置或后台 WebView 刷新",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
          checked = config.autoBackupRenewal,
          onCheckedChange = onToggleAutoBackupRenewal,
          modifier = Modifier.testTag("switch_auto_backup"),
        )
      }

      // Daily notifications switch
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text("每日签到与异常通知", style = MaterialTheme.typography.bodyLarge)
          Text(
            text = "定时签到汇总汇报与上限/失效高优先级提醒",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
          checked = config.notificationsEnabled,
          onCheckedChange = onToggleNotifications,
          modifier = Modifier.testTag("switch_notifications"),
        )
      }
    }
  }
}

@Composable
private fun AppearanceCard(
  useDynamicColor: Boolean,
  themeSeedColorHex: String?,
  themePreference: ThemePreference,
  onToggleDynamicColor: (Boolean) -> Unit,
  onSelectThemeSeedColor: (String) -> Unit,
  onSelectThemePreference: (ThemePreference) -> Unit,
) {
  val supportsDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
  val isDynamicActive = useDynamicColor && supportsDynamicColor
  val activePreset = ThemeSeedPresets.firstOrNull {
    it.hex.equals(themeSeedColorHex, ignoreCase = true)
  }

  Card(
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(
          imageVector = Icons.Filled.Palette,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
        )
        Text(
          text = "外观与动态取色",
          style = MaterialTheme.typography.titleMedium,
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text("跟随系统壁纸动态取色", style = MaterialTheme.typography.bodyLarge)
          Text(
            text = when {
              isDynamicActive -> "Android 12+ 自动提取壁纸莫奈色彩"
              supportsDynamicColor -> "已启用自定义调色盘配色，可随时切回系统动态取色"
              else -> "当前系统低于 Android 12，将使用自定义调色盘配色"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
          checked = useDynamicColor,
          onCheckedChange = onToggleDynamicColor,
          modifier = Modifier.testTag("switch_dynamic_color"),
        )
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text("主题调色盘", style = MaterialTheme.typography.bodyLarge)
          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
          ) {
            Text(
              text = when {
                isDynamicActive -> "跟随系统壁纸"
                activePreset != null -> activePreset.name
                else -> "默认靛紫"
              },
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
              color = MaterialTheme.colorScheme.onSecondaryContainer,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
          }
        }

        LazyRow(
          modifier = Modifier
            .fillMaxWidth()
            .testTag("theme_seed_palette_row"),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          items(
            items = ThemeSeedPresets,
            key = { it.hex },
          ) { preset ->
            val selected = !isDynamicActive && preset.hex.equals(themeSeedColorHex, ignoreCase = true)
            ThemeSeedSwatch(
              preset = preset,
              selected = selected,
              onClick = { onSelectThemeSeedColor(preset.hex) },
            )
          }
        }
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

      Text("深浅色显示模式", style = MaterialTheme.typography.bodyLarge)
      SingleChoiceSegmentedButtonRow(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("theme_preference_selector"),
      ) {
        ThemePreference.entries.forEachIndexed { index, pref ->
          SegmentedButton(
            selected = themePreference == pref,
            onClick = { onSelectThemePreference(pref) },
            shape = SegmentedButtonDefaults.itemShape(index = index, count = ThemePreference.entries.size),
            label = { Text(pref.label) },
          )
        }
      }
    }
  }
}

@Composable
private fun ThemeSeedSwatch(
  preset: ThemeSeedPreset,
  selected: Boolean,
  onClick: () -> Unit,
) {
  val scale by animateFloatAsState(
    targetValue = if (selected) 1.1f else 1.0f,
    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
    label = "seed_swatch_scale",
  )
  val isBright = ColorUtils.calculateLuminance(preset.color.toArgb()) > 0.45
  val checkTint = if (isBright) Color.Black else Color.White

  Box(
    modifier = Modifier
      .size(48.dp)
      .scale(scale)
      .clip(CircleShape)
      .clickable(
        role = Role.RadioButton,
        onClick = onClick,
      )
      .semantics {
        contentDescription = "${preset.name}${if (selected) "（已选中）" else ""}"
      }
      .testTag("seed_color_${preset.hex}"),
    contentAlignment = Alignment.Center,
  ) {
    Box(
      modifier = Modifier
        .size(42.dp)
        .border(
          width = if (selected) 2.5.dp else 1.dp,
          color = if (selected) {
            MaterialTheme.colorScheme.onSurface
          } else {
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
          },
          shape = CircleShape,
        )
        .padding(4.dp)
        .clip(CircleShape)
        .background(preset.color),
      contentAlignment = Alignment.Center,
    ) {
      if (selected) {
        Icon(
          imageVector = Icons.Filled.Check,
          contentDescription = null,
          tint = checkTint,
          modifier = Modifier.size(18.dp),
        )
      }
    }
  }
}

@Composable
private fun SystemPermissionSelfCheckCard(
  status: SystemPermissionStatus,
  onRequestNotification: () -> Unit,
  onOpenBatterySettings: () -> Unit,
  onOpenExactAlarmSettings: () -> Unit,
) {
  Card(
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
        text = "系统权限自检",
        style = MaterialTheme.typography.titleMedium,
      )
      Text(
        text = "保障后台链式闹钟、防杀保活与状态推送正常送达。",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      PermissionCheckRow(
        icon = Icons.Filled.NotificationsActive,
        title = "通知推送权限",
        subtitle = if (status.notificationsGranted) "已开启，可接收汇总与异常提醒" else "未开启，Android 13+ 需手动授权",
        isGranted = status.notificationsGranted,
        actionLabel = if (status.notificationsGranted) "已就绪" else "去开启",
        onClick = onRequestNotification,
        testTag = "perm_notif_btn",
      )

      PermissionCheckRow(
        icon = Icons.Filled.BatteryChargingFull,
        title = "电池优化无限制",
        subtitle = if (status.batteryOptimizationIgnored) "已加入无限制白名单" else "建议设为无限制防止后台休眠",
        isGranted = status.batteryOptimizationIgnored,
        actionLabel = if (status.batteryOptimizationIgnored) "已就绪" else "去设置",
        onClick = onOpenBatterySettings,
        testTag = "perm_battery_btn",
      )

      PermissionCheckRow(
        icon = Icons.Filled.Alarm,
        title = "精确闹钟权限",
        subtitle = if (status.exactAlarmAllowed) "已授权 setExactAndAllowWhileIdle" else "未授权时将自动降级为非精确调度",
        isGranted = status.exactAlarmAllowed,
        actionLabel = if (status.exactAlarmAllowed) "已就绪" else "去授权",
        onClick = onOpenExactAlarmSettings,
        testTag = "perm_alarm_btn",
      )
    }
  }
}

@Composable
private fun PermissionCheckRow(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  subtitle: String,
  isGranted: Boolean,
  actionLabel: String,
  onClick: () -> Unit,
  testTag: String,
) {
  val statusColors = LocalStatusColors.current

  Surface(
    shape = RoundedCornerShape(18.dp),
    color = MaterialTheme.colorScheme.surfaceContainer,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Icon(
          imageVector = if (isGranted) Icons.Filled.CheckCircle else Icons.Filled.WarningAmber,
          contentDescription = null,
          tint = if (isGranted) statusColors.successAccent else statusColors.warningAccent,
          modifier = Modifier.size(22.dp),
        )
        Column {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(
              imageVector = icon,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(text = title, style = MaterialTheme.typography.titleMedium)
          }
          Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      Spacer(modifier = Modifier.width(8.dp))

      FilledTonalButton(
        onClick = onClick,
        shape = CircleShape,
        modifier = Modifier
          .minimumInteractiveComponentSize()
          .testTag(testTag),
      ) {
        Text(actionLabel)
      }
    }
  }
}

@Composable
private fun HelpGuideEntryCard(onClick: () -> Unit) {
  Card(
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(24.dp))
      .clickable(onClick = onClick)
      .testTag("settings_open_help_card"),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .minimumInteractiveComponentSize()
        .padding(18.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.primaryContainer,
          modifier = Modifier.size(40.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.HelpOutline,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.size(20.dp),
            )
          }
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(
            text = "基础模式凭证导入教程",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text = "手把手教您用手机或电脑获取登录凭证",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
      Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = "查看教程",
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
