请基于我们**当前已经编写好的 Android 项目（`MhyCloud-Checkin`）**进行增量功能升级与 UI 修复，**保留现有的架构、状态机、加密存储、后台调度、通知推送与底部悬浮式导航栏**。

本次需要完成以下 **3 项更新**：

---

## 一、 修复“贴心提醒”卡片在深色动态取色下的文字对比度异常（`ImportHelpScreen.kt`）

* **问题描述**：目前在开启系统动态取色的深色模式下，`ImportHelpScreen` 底部的“贴心提醒”卡片背景与标题/正文颜色未正确配对（标题偏暗、正文对比度不足）。
* **修复方案**：
  * 严格使用 Material Design 3 成对的色彩 Token：将“贴心提醒”卡片的 `containerColor` 设为 `MaterialTheme.colorScheme.secondaryContainer`（或 `surfaceContainerHigh`），并同步设置 `contentColor = MaterialTheme.colorScheme.onSecondaryContainer`（或 `onSurface`）。
  * 卡片内图标、主标题与条目标题统一使用 `MaterialTheme.colorScheme.onSecondaryContainer`，次级说明文字使用 `MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)`，禁止硬编码固定灰/黑颜色，确保任何动态取色和深浅色组合下都具备高可读性。

---

## 二、 新增“自定义主题调色盘”功能（遵循 MD3 动态取色规范）

1. **数据模型与持久化（`Models.kt` & `EncryptedStorageRepository.kt`）**：
   * 在 `AppGlobalConfig` 中新增 `themeSeedColorHex: String?`（默认 `null`，表示未指定自定义种子色）与 `useDynamicColor: Boolean`（默认 `true`）。
2. **基于种子色的 MD3 Tonal 色彩生成（`Theme.kt` / `Color.kt`）**：
   * 当 `useDynamicColor == true` 且系统为 Android 12+ 时，优先使用 `dynamicLightColorScheme` / `dynamicDarkColorScheme`。
   * 当用户关闭系统动态取色或选择了自定义调色盘种子色时，根据选中的 `Seed Color`（通过 HCT / HSL Tonal 映射算法生成符合 Material 3 规范的协调色阶：`primary`、`onPrimary`、`primaryContainer`、`onPrimaryContainer`、`secondary`、`secondaryContainer`、`tertiary`、`surfaceContainerLow/High` 等）构建完整的 `lightColorScheme` / `darkColorScheme`，确保深浅色切换依然自然协调。
3. **设置页调色盘 UI（`SettingsScreen.kt`）**：
   * 在外观设置卡片中新增**「主题调色盘」**横向色块选择器（提供 7 款精选 MD3 种子色圆点，如星辰紫、琉璃蓝、翡翠绿、琥珀金、绯樱红、青黛青、赤霞橙，选中带对勾与弹簧缩放动效），并支持切换开关决定“跟随系统壁纸动态取色”还是“使用自定义调色盘”。

---

## 三、 新增通用“令牌导出”功能（同时支持《云·原神》与《云·绝区零》）

1. **适用范围**：
   * 在首页每张 `AccountCard` 的操作栏（或卡片菜单/图标按钮）中，为**所有已保存的《云·原神》和《云·绝区零》账号**（无论其凭证来自 Root、WebView 还是剪贴板）统一提供**「导出令牌」**入口。
2. **导出弹窗与格式支持（`HomeScreen.kt` / `MainViewModel.kt`）**：
   * 点击“导出令牌”后弹出 MD3 风格确认/导出对话框（顶部明确提示：*“以下内容包含敏感登录凭证，仅用于迁移到您自己的其他设备或备份，请勿泄露给他人”*）。
   * 提供两个一键复制选项：
     1. **复制标准请求头（推荐，跨设备导入本软件）**：将当前账号完整的 `x-rpc-combo_token` 与 `deviceHeadersJson` 中的全部请求头按标准 `Key: Value` 多行文本拼接并写入剪贴板。
     2. **复制完整 `cURL` 命令**：根据该账号的 `gameType` 自动匹配真实 `walletEndpoint`（云·原神 `api-cloudgame.mihoyo.com` / 云·绝区零 `cg-nap-api.mihoyo.com`），拼装完整可执行的 `curl -X GET '...' -H '...'` 命令写入剪贴板。
   * 复制成功后弹出简短提示（“已复制到剪贴板”），且导出预览区默认对敏感部分做折叠或脱敏展示。

---

## 输出要求
为了方便直接合入工程，请只输出发生变更的完整文件（`ImportHelpScreen.kt`、`Theme.kt`、`Color.kt`、`SettingsScreen.kt`、`HomeScreen.kt`）以及 `Models.kt` / `MainViewModel.kt` 的具体修改片段。
