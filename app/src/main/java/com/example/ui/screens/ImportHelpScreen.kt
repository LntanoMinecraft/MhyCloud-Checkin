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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

private enum class GuidePlatformTab(val label: String) {
  PHONE("手机端"),
  DESKTOP("电脑 / 模拟器"),
}

private data class GuideStep(
  val number: Int,
  val stepLabel: String,
  val title: String,
  val description: String,
)

private val phoneSteps = listOf(
  GuideStep(
    number = 1,
    stepLabel = "步骤 1 · 进入导入弹窗",
    title = "选择《云·原神》并打开内置网页",
    description = "在首页右上角点击「+ 添加」，选中《云·原神》后，软件会自动为您打开官方云游戏网页登录界面。",
  ),
  GuideStep(
    number = 2,
    stepLabel = "步骤 2 · 像平时一样登录",
    title = "输入您的米哈游账号或验证码",
    description = "在内置网页中按正常流程输入手机号验证码或密码登录。如果弹出滑块或点选验证，正常完成即可。",
  ),
  GuideStep(
    number = 3,
    stepLabel = "步骤 3 · 无需抓包，自动保存",
    title = "看到游戏主界面后自动返回",
    description = "登录成功并加载出《云·原神》主界面（能看到剩余时长）后，软件会自动捕获并加密保存凭证，随后自动关闭网页回到首页。",
  ),
)

private val desktopEmulatorSteps = listOf(
  GuideStep(
    number = 1,
    stepLabel = "步骤 1 · 先装好软件再登录",
    title = "在模拟器开启 Root 并提前安装本软件",
    description = "在电脑上打开常用安卓模拟器（如 MuMu 12、雷电等），进入模拟器设置开启「Root 权限」；随后提前在模拟器内安装好本软件（《云签助手》），并在「设置」中切换为 Root 模式（完全不影响主力手机）。",
  ),
  GuideStep(
    number = 2,
    stepLabel = "步骤 2 · 生成登录会话",
    title = "在模拟器安装并登录《云·绝区零》",
    description = "在模拟器内安装并打开官方《云·绝区零》客户端，正常登录您的米哈游账号，等待完全进入游戏主界面（确保客户端已生成完整会话文件）。",
  ),
  GuideStep(
    number = 3,
    stepLabel = "步骤 3 · 导出跨设备文本",
    title = "扫描提取并发送回主力手机",
    description = "切回模拟器里的《云签助手》，点击「+ 添加 → 一键扫描本机」，同步账号后在首页卡片点击「导出令牌」，复制标准请求头文本并通过微信、QQ 等发送给主力手机。",
  ),
  GuideStep(
    number = 4,
    stepLabel = "步骤 4 · 完成绑定",
    title = "在主力手机从剪贴板一键识别",
    description = "在主力手机复制刚才发来的文本，打开《云签助手》（基础模式）进入《云·绝区零》导入页，点击「从剪贴板一键识别」即可保存成功。",
  ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportHelpScreen(
  onBackClick: () -> Unit,
) {
  var selectedTab by rememberSaveable { mutableStateOf(GuidePlatformTab.PHONE) }
  var advancedExpanded by rememberSaveable { mutableStateOf(false) }
  val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

  val navBarBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

  Scaffold(
    modifier = Modifier
      .fillMaxSize()
      .nestedScroll(scrollBehavior.nestedScrollConnection),
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    topBar = {
      LargeTopAppBar(
        title = {
          Text(
            text = "凭证获取指南",
            style = MaterialTheme.typography.headlineMedium,
          )
        },
        navigationIcon = {
          IconButton(
            onClick = onBackClick,
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .testTag("help_back_button"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "返回",
            )
          }
        },
        scrollBehavior = scrollBehavior,
      )
    },
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .widthIn(max = 640.dp),
        contentPadding = PaddingValues(
          start = 16.dp,
          end = 16.dp,
          top = 8.dp,
          bottom = navBarBottomPadding + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        item(key = "platform_switcher") {
          SingleChoiceSegmentedButtonRow(
            modifier = Modifier
              .fillMaxWidth()
              .testTag("help_platform_tabs"),
          ) {
            GuidePlatformTab.entries.forEachIndexed { index, tab ->
              SegmentedButton(
                selected = selectedTab == tab,
                onClick = { selectedTab = tab },
                shape = SegmentedButtonDefaults.itemShape(
                  index = index,
                  count = GuidePlatformTab.entries.size,
                ),
                modifier = Modifier
                  .weight(1f)
                  .heightIn(min = 48.dp),
                icon = {
                  Icon(
                    imageVector = if (tab == GuidePlatformTab.PHONE) {
                      Icons.Filled.PhoneAndroid
                    } else {
                      Icons.Filled.Computer
                    },
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                  )
                },
                label = {
                  Text(
                    text = tab.label,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                  )
                },
              )
            }
          }
        }

        item(key = "tab_intro_pill") {
          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
          ) {
            Text(
              text = if (selectedTab == GuidePlatformTab.PHONE) {
                "适合《云·原神》及已 Root 手机"
              } else {
                "适合无 Root 用户的《云·绝区零》"
              },
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.onSecondaryContainer,
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            )
          }
        }

        if (selectedTab == GuidePlatformTab.PHONE) {
          items(
            items = phoneSteps,
            key = { "phone_${it.number}" },
          ) { step ->
            GuideStepCard(step = step)
          }

          item(key = "zzz_phone_notice") {
            HighlightNoticeCard(
              title = "为什么《云·绝区零》不能在手机网页登录？",
              body = "目前《云·绝区零》官方暂未开放手机网页版，且安卓系统禁止未 Root 手机直接解密其他 App 的网络请求。",
              extraGuidance = "• 如果您的手机已 Root：请在「设置」切为 Root 模式，直接一键扫描即可。\n• 如果您的手机未 Root：请点击下方按钮切换到「电脑端 / 模拟器」教程，只需在电脑操作一次即可长期使用。",
              actionLabel = "切换到「电脑端 / 模拟器」教程",
              onActionClick = { selectedTab = GuidePlatformTab.DESKTOP },
              testTag = "switch_to_desktop_guide_btn",
            )
          }
        } else {
          item(key = "pc_capture_pitfall_notice") {
            HighlightNoticeCard(
              title = "为什么直接在电脑抓《云·绝区零》PC 版常搜不到 Token？",
              body = "《云·绝区零》Windows PC 客户端默认会绕过普通系统代理，直接用 Reqable 只能抓到 dataUpload、h5log 等日志上报接口（里面没有登录凭证）。因此推荐优先使用下方的「电脑安卓模拟器」方案，最简单稳定。",
            )
          }

          item(key = "desktop_emulator_section_title") {
            Text(
              text = "推荐方案：电脑安卓模拟器一键提取（成功率最高）",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            )
          }

          items(
            items = desktopEmulatorSteps,
            key = { "desktop_${it.number}" },
          ) { step ->
            GuideStepCard(step = step)
          }

          item(key = "desktop_advanced_capture_card") {
            AdvancedCaptureCard(
              expanded = advancedExpanded,
              onToggleExpand = { advancedExpanded = !advancedExpanded },
            )
          }
        }

        item(key = "friendly_tips_card") {
          FriendlyTipsCard()
        }
      }
    }
  }
}

@Composable
private fun GuideStepCard(step: GuideStep) {
  Card(
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(18.dp),
      horizontalArrangement = Arrangement.spacedBy(14.dp),
      verticalAlignment = Alignment.Top,
    ) {
      Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.size(40.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Text(
            text = step.number.toString(),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
          )
        }
      }

      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Text(
          text = step.stepLabel,
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.primary,
        )
        Text(
          text = step.title,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = step.description,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
private fun HighlightNoticeCard(
  title: String,
  body: String,
  extraGuidance: String? = null,
  actionLabel: String? = null,
  onActionClick: (() -> Unit)? = null,
  testTag: String? = null,
) {
  val containerColor = MaterialTheme.colorScheme.tertiaryContainer
  val contentColor = MaterialTheme.colorScheme.onTertiaryContainer

  Card(
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(
      containerColor = containerColor,
      contentColor = contentColor,
    ),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(
          imageVector = Icons.Filled.Lightbulb,
          contentDescription = null,
          tint = contentColor,
          modifier = Modifier
            .padding(top = 2.dp)
            .size(20.dp),
        )
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
          color = contentColor,
        )
      }

      Text(
        text = body,
        style = MaterialTheme.typography.bodyMedium,
        color = contentColor.copy(alpha = 0.88f),
      )

      if (!extraGuidance.isNullOrBlank()) {
        Text(
          text = extraGuidance,
          style = MaterialTheme.typography.bodyMedium,
          color = contentColor.copy(alpha = 0.88f),
        )
      }

      if (!actionLabel.isNullOrBlank() && onActionClick != null) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
        ) {
          TextButton(
            onClick = onActionClick,
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .let { mod -> if (testTag != null) mod.testTag(testTag) else mod },
          ) {
            Text(
              text = actionLabel,
              color = contentColor,
              fontWeight = FontWeight.SemiBold,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun AdvancedCaptureCard(
  expanded: Boolean,
  onToggleExpand: () -> Unit,
) {
  Card(
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(24.dp))
      .clickable(onClick = onToggleExpand)
      .testTag("advanced_capture_card"),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .minimumInteractiveComponentSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Icon(
          imageVector = Icons.Filled.Terminal,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(20.dp),
        )
        Text(
          text = "进阶方案：Proxifier + Reqable 强制抓包（不装模拟器）",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.weight(1f),
        )
        Icon(
          imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
          contentDescription = if (expanded) "收起进阶方案" else "展开进阶方案",
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(22.dp),
        )
      }

      AnimatedVisibility(visible = expanded) {
        Text(
          text = "1. 在电脑安装 Reqable（开启系统代理并安装信任根证书）以及 Proxifier。\n" +
            "2. 在 Proxifier 中添加代理规则，将《云·绝区零》安装目录下的所有 .exe 进程流量强制转发至 Reqable 监听端口，然后冷启动游戏进入主界面。\n" +
            "3. 回到 Reqable，将顶部搜索范围设为「URL」并搜索 cg-nap-api，找到状态码为 200 的请求，右键选择「复制为 cURL」发回手机导入即可。",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
private fun FriendlyTipsCard() {
  val containerColor = MaterialTheme.colorScheme.secondaryContainer
  val contentColor = MaterialTheme.colorScheme.onSecondaryContainer

  Card(
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(
      containerColor = containerColor,
      contentColor = contentColor,
    ),
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
          imageVector = Icons.Filled.TipsAndUpdates,
          contentDescription = null,
          tint = contentColor,
          modifier = Modifier.size(20.dp),
        )
        Text(
          text = "贴心提醒",
          style = MaterialTheme.typography.titleMedium,
          color = contentColor,
        )
      }

      FriendlyTipItem(
        title = "不用自己翻找 Token",
        body = "复制出来的请求头或 cURL 文本很长是正常的，您不需要自己去里面挑字符，整段复制过来软件会自动识别。",
      )
      FriendlyTipItem(
        title = "导入后自动清空剪贴板",
        body = "识别并保存成功后，软件会立即清空手机剪贴板，防止其他后台应用读取您的登录凭证。",
      )
      FriendlyTipItem(
        title = "满 600 分钟自动暂停",
        body = "当检测到免费时长达到 600 分钟上限时，软件会自动推送提醒并暂停签到，消耗时长后可在首页卡片一键恢复。",
      )
      FriendlyTipItem(
        title = "一次导入长期有效",
        body = "只要不修改账号密码或在其他客户端顶掉登录态，通常只需配置这一次，后续每天都会自动签到。",
      )
    }
  }
}

@Composable
private fun FriendlyTipItem(
  title: String,
  body: String,
) {
  val contentColor = MaterialTheme.colorScheme.onSecondaryContainer

  Row(
    horizontalArrangement = Arrangement.spacedBy(10.dp),
    verticalAlignment = Alignment.Top,
  ) {
    Icon(
      imageVector = Icons.Filled.CheckCircleOutline,
      contentDescription = null,
      tint = contentColor,
      modifier = Modifier
        .padding(top = 2.dp)
        .size(18.dp),
    )
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = contentColor,
      )
      Text(
        text = body,
        style = MaterialTheme.typography.bodyMedium,
        color = contentColor.copy(alpha = 0.85f),
      )
    }
  }
}
