package com.example.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
object HomeRoute

@Serializable
object LogsRoute

@Serializable
object SettingsRoute

@Serializable
object ImportHelpRoute

@Serializable
object AboutRoute

enum class AppTab(
  val label: String,
  val testTag: String,
) {
  HOME("首页", "tab_home"),
  LOGS("日志", "tab_logs"),
  SETTINGS("设置", "tab_settings"),
}
