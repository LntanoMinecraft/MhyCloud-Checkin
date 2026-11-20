package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.ui.components.AppBrandLauncherIcon

private const val GITHUB_REPO_URL = "https://github.com/LntanoMinecraft/MhyCloud-Checkin"
private const val COOLAPK_PROFILE_URL = "https://www.coolapk.com/u/34054336"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
  onBackClick: () -> Unit,
  onOpenLinkError: (String) -> Unit = {},
) {
  val context = LocalContext.current

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = "关于",
            style = MaterialTheme.typography.titleLarge,
          )
        },
        navigationIcon = {
          IconButton(
            onClick = onBackClick,
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .testTag("about_back_button"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "返回",
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = Color.Transparent,
        ),
      )
    },
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .widthIn(max = 640.dp)
          .padding(horizontal = 20.dp)
          .navigationBarsPadding()
          .padding(bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        // Upper / center brand showcase
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          verticalArrangement = Arrangement.Center,
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          AppBrandLauncherIcon(
            size = 92.dp,
            modifier = Modifier.testTag("about_app_icon"),
          )

          Spacer(modifier = Modifier.height(20.dp))

          Text(
            text = "云签助手",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
          )

          Spacer(modifier = Modifier.height(10.dp))

          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.testTag("about_version_pill"),
          ) {
            Text(
              text = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.onSecondaryContainer,
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            )
          }

          Spacer(modifier = Modifier.height(10.dp))

          Text(
            text = "云游戏自动签到工具",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        // Bottom-anchored links card
        Card(
          shape = RoundedCornerShape(24.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
          ),
          elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.fillMaxWidth()) {
            AboutLinkItem(
              icon = Icons.Outlined.Code,
              title = "项目主页（GitHub）",
              subtitle = "github.com/LntanoMinecraft/MhyCloud-Checkin",
              onClick = {
                openExternalUrl(
                  context = context,
                  url = GITHUB_REPO_URL,
                  onError = onOpenLinkError,
                )
              },
              testTag = "about_github_link",
            )

            HorizontalDivider(
              modifier = Modifier.padding(horizontal = 18.dp),
              color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
            )

            AboutLinkItem(
              icon = Icons.Outlined.PersonOutline,
              title = "开发者酷安",
              subtitle = "酷安主页 · UID 34054336",
              onClick = {
                openExternalUrl(
                  context = context,
                  url = COOLAPK_PROFILE_URL,
                  onError = onOpenLinkError,
                )
              },
              testTag = "about_coolapk_link",
            )
          }
        }
      }
    }
  }
}

@Composable
private fun AboutLinkItem(
  icon: ImageVector,
  title: String,
  subtitle: String,
  onClick: () -> Unit,
  testTag: String,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .minimumInteractiveComponentSize()
      .clickable(onClick = onClick)
      .padding(horizontal = 18.dp, vertical = 16.dp)
      .testTag(testTag),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    Surface(
      shape = CircleShape,
      color = MaterialTheme.colorScheme.primaryContainer,
      modifier = Modifier.size(40.dp),
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onPrimaryContainer,
          modifier = Modifier.size(20.dp),
        )
      }
    }

    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    Icon(
      imageVector = Icons.AutoMirrored.Filled.OpenInNew,
      contentDescription = "打开链接",
      tint = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.size(18.dp),
    )
  }
}

private fun openExternalUrl(
  context: Context,
  url: String,
  onError: (String) -> Unit,
) {
  runCatching {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
  }.onFailure {
    onError("未找到可打开链接的应用")
  }
}
