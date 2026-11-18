package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PhoneAndroid
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalStatusColors

private enum class GuidePlatformTab(val label: String) {
  PHONE("手机端（云·原神）"),
  DESKTOP("电脑端（云·绝区零 / 通用）"),
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
    stepLabel = "步骤 1 · 选择游戏并打开内置网页",
    title = "打开内置登录页",
    description = "在首页点击“添加账号”，选择《云·原神》并进入登录界面。软件会自动为您打开官方云游戏网页。",
  ),
  GuideStep(
    number = 2,
    stepLabel = "步骤 2 · 像平时一样登录账号",
    title = "正常登录您的米哈游账号",
    description = "在网页里输入手机号验证码或密码登录。如果需要滑块验证，正常滑动即可，就像平时用浏览器一样。",
  ),
  GuideStep(
    number = 3,
    stepLabel = "步骤 3 · 进入主页后自动完成",
    title = "无需抓包，自动保存",
    description = "登录成功并看到《云·原神》主界面后，软件会自动记录签到所需的凭证并关闭网页，之后每天都会按时自动帮您领时长。",
  ),
)

private val desktopSteps = listOf(
  GuideStep(
    number = 1,
    stepLabel = "步骤 1 · 准备抓包软件",
    title = "在电脑上打开抓包工具",
    description = "在电脑安装并打开常用抓包软件（如 Reqable 或 Charles），按软件新手提示开启“系统代理”并信任电脑根证书。",
  ),
  GuideStep(
    number = 2,
    stepLabel = "步骤 2 · 登录游戏触发记录",
    title = "打开游戏客户端登录账号",
    description = "在电脑上打开《云·绝区零》PC 客户端（或《云·原神》网页版），正常登录并进入游戏主界面，等待几秒钟让页面加载完成。",
  ),
  GuideStep(
    number = 3,
    stepLabel = "步骤 3 · 一键复制整条记录",
    title = "搜索 wallet 并复制为 cURL",
    description = "回到抓包软件，在顶部的搜索框里输入 wallet，找到状态显示为 200 的那一条记录，鼠标右键选择「复制为 cURL」（或者“复制全部请求头”）。",
  ),
  GuideStep(
    number = 4,
    stepLabel = "步骤 4 · 发到手机并一键识别",
    title = "粘贴回本软件自动提取",
    description = "把刚才复制的那一大段文字通过微信、QQ 或电脑手机跨端剪贴板发到手机并复制。回到本软件的导入界面，点击「从剪贴板一键识别」即可自动完成配置。",
  ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportHelpScreen(
  onBackClick: () -> Unit,
) {
  var selectedTab by rememberSaveable { mutableStateOf(GuidePlatformTab.PHONE) }
  val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

  Scaffold(
    modifier = Modifier
      .fillMaxSize()
      .nestedScroll(scrollBehavior.nestedScrollConnection),
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
        .padding(innerPadding)
        .navigationBarsPadding(),
      contentAlignment = Alignment.TopCenter,
    ) {
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .widthIn(max = 640.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
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
          ) {
            Text(
              text = if (selectedTab == GuidePlatformTab.PHONE) {
                "推荐《云·原神》，全程手机操作无需电脑"
              } else {
                "推荐《云·绝区零》，同样支持《云·原神》"
              },
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.onSecondaryContainer,
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            )
          }
        }

        val activeSteps = if (selectedTab == GuidePlatformTab.PHONE) phoneSteps else desktopSteps
        items(
          items = activeSteps,
          key = { "${selectedTab.name}_${it.number}" },
        ) { step ->
          GuideStepCard(step = step)
        }

        if (selectedTab == GuidePlatformTab.PHONE) {
          item(key = "zzz_phone_notice") {
            ZzzNoticeHighlightCard(
              onSwitchToDesktop = { selectedTab = GuidePlatformTab.DESKTOP },
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
private fun ZzzNoticeHighlightCard(
  onSwitchToDesktop: () -> Unit,
) {
  val statusColors = LocalStatusColors.current

  Card(
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(
      containerColor = statusColors.warningContainer.copy(alpha = 0.65f),
    ),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(
          imageVector = Icons.Filled.Lightbulb,
          contentDescription = null,
          tint = statusColors.onWarningContainer,
          modifier = Modifier.size(20.dp),
        )
        Text(
          text = "为什么《云·绝区零》不能这样登录？",
          style = MaterialTheme.typography.titleMedium,
          color = statusColors.onWarningContainer,
        )
      }

      Text(
        text = "目前《云·绝区零》官方暂未开放手机网页版，如果您没有开启 Root 模式，请切换到上方的「电脑端」标签，用电脑协助获取一次凭证即可长期使用。",
        style = MaterialTheme.typography.bodyMedium,
        color = statusColors.onWarningContainer.copy(alpha = 0.92f),
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
      ) {
        TextButton(
          onClick = onSwitchToDesktop,
          modifier = Modifier.minimumInteractiveComponentSize(),
        ) {
          Text(
            text = "切换到「电脑端」教程",
            color = statusColors.onWarningContainer,
            fontWeight = FontWeight.SemiBold,
          )
        }
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
        title = "不用自己挑字符",
        body = "复制出来的文字很长是正常的，您不需要自己去里面翻找 token，直接整段复制过来，软件会自动帮您挑出需要的部分。",
      )
      FriendlyTipItem(
        title = "自动清空剪贴板",
        body = "点击识别并保存成功后，软件会立刻自动清空您的手机剪贴板，防止其他应用偷看您的账号凭证。",
      )
      FriendlyTipItem(
        title = "一次导入长期有效",
        body = "只要您不修改账号密码，通常只需操作这一次，以后每天都会在后台全自动签到。",
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
