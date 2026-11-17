package com.example.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun FloatingNavigationChrome(
  selectedTab: AppTab,
  onSelectTab: (AppTab) -> Unit,
  showBatchFab: Boolean,
  isBatchRunning: Boolean,
  onBatchCheckinClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .navigationBarsPadding()
      .padding(horizontal = 16.dp, vertical = 16.dp),
    contentAlignment = Alignment.BottomCenter,
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      // MD3 Expressive Floating Navigation Capsule
      Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 3.dp,
        shadowElevation = 6.dp,
        modifier = Modifier.testTag("floating_nav_capsule"),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
          horizontalArrangement = Arrangement.spacedBy(4.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          AppTab.entries.forEach { tab ->
            val selected = tab == selectedTab
            FloatingNavPillItem(
              tab = tab,
              selected = selected,
              onClick = { onSelectTab(tab) },
            )
          }
        }
      }

      // Expressive paired FAB for "一键全部签到"
      AnimatedVisibility(
        visible = showBatchFab,
        enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
        exit = scaleOut() + fadeOut(),
      ) {
        FloatingActionButton(
          onClick = { if (!isBatchRunning) onBatchCheckinClick() },
          shape = RoundedCornerShape(22.dp),
          containerColor = MaterialTheme.colorScheme.primaryContainer,
          contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
          elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = 6.dp,
            pressedElevation = 8.dp,
          ),
          modifier = Modifier
            .height(58.dp)
            .testTag("fab_batch_checkin"),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            if (isBatchRunning) {
              CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.5.dp,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
              )
              Text(
                text = "签到中",
                style = MaterialTheme.typography.labelLarge,
              )
            } else {
              Icon(
                imageVector = Icons.Filled.Bolt,
                contentDescription = "一键全部签到",
              )
              Text(
                text = "一键签到",
                style = MaterialTheme.typography.labelLarge,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun FloatingNavPillItem(
  tab: AppTab,
  selected: Boolean,
  onClick: () -> Unit,
) {
  val bgColor by animateColorAsState(
    targetValue = if (selected) {
      MaterialTheme.colorScheme.secondaryContainer
    } else {
      Color.Transparent
    },
    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
    label = "pill_bg",
  )
  val contentColor by animateColorAsState(
    targetValue = if (selected) {
      MaterialTheme.colorScheme.onSecondaryContainer
    } else {
      MaterialTheme.colorScheme.onSurfaceVariant
    },
    label = "pill_fg",
  )
  val scale by animateFloatAsState(
    targetValue = if (selected) 1.0f else 0.94f,
    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
    label = "pill_scale",
  )

  val icon = when (tab) {
    AppTab.HOME -> if (selected) Icons.Filled.Home else Icons.Outlined.Home
    AppTab.LOGS -> if (selected) Icons.Filled.ReceiptLong else Icons.Outlined.ReceiptLong
    AppTab.SETTINGS -> if (selected) Icons.Filled.Settings else Icons.Outlined.Settings
  }

  val interactionSource = remember { MutableInteractionSource() }

  Row(
    modifier = Modifier
      .scale(scale)
      .minimumInteractiveComponentSize()
      .clip(CircleShape)
      .background(bgColor)
      .clickable(
        interactionSource = interactionSource,
        indication = androidx.compose.material3.ripple(),
        role = Role.Tab,
        onClick = onClick,
      )
      .padding(horizontal = if (selected) 16.dp else 12.dp, vertical = 10.dp)
      .testTag(tab.testTag),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.Center,
  ) {
    Icon(
      imageVector = icon,
      contentDescription = tab.label,
      tint = contentColor,
      modifier = Modifier.size(20.dp),
    )
    AnimatedVisibility(visible = selected) {
      Row {
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = tab.label,
          style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
          color = contentColor,
          maxLines = 1,
        )
      }
    }
  }
}
