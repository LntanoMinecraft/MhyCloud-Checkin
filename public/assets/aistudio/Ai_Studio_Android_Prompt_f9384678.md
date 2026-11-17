你是一名精通 Kotlin、Jetpack Compose、Android 系统底层权限管理与 Material Design 3 Expressive（MD3 Expressive）设计规范的资深 Android 架构师。请为我编写一个完整的、可直接编译运行的 Android 原生应用项目：**《云游戏自动签到助手》（支持《云·原神》与《云·绝区零》）**。

请严格按照以下技术规格、安全红线、UI 规范与代码输出要求生成完整项目，**不要使用 `TODO`、省略号或伪代码占位符**。

---

## 1. 技术栈与项目基础配置
* **语言与 UI 框架**：Kotlin + Jetpack Compose（Material 3 最新 BOM，启用 Material 3 Expressive 视觉特性）
* **最低 / 目标 SDK**：`minSdk = 26`，`targetSdk = 35`（兼容 Android 13+ `POST_NOTIFICATIONS`、Android 14/15 精确闹钟、前台加急任务与 Edge-to-Edge 规范）
* **核心依赖**：
  * `androidx.compose.material3`（支持 Dynamic Color 与 Expressive 组件风格）
  * `androidx.navigation:navigation-compose`
  * `androidx.work:work-runtime-ktx`（后台调度与加急任务）
  * `androidx.security:security-crypto`（Android Keystore + `EncryptedSharedPreferences`）
  * `com.squareup.okhttp3:okhttp`（网络请求）
  * `com.github.topjohnwu.libsu:core`（单例 Root Shell 会话管理）
  * `org.jetbrains.kotlinx:kotlinx-serialization-json`（本地实体与设备头序列化）

---

## 2. UI 与视觉设计规范（Material Design 3 Expressive）
1. **MD3 Expressive 视觉语言**：
   * 采用大圆角容器（`RoundedCornerShape(24.dp)` / `28.dp` 卡片）、富有层次的排版对比（`displaySmall` / `titleLarge` 展示关键时长数字）、高对比度状态胶囊（Status Chip），以及流畅的弹簧物理动效（`spring(dampingRatio = Spring.DampingRatioMediumBouncy)`）。
   * 使用 MD3 Expressive 风格组件：分段选择按钮（`SingleChoiceSegmentedButtonRow`）、高表现力胶囊形操作按钮与模态底部弹窗（`ModalBottomSheet`）。
2. **底部悬浮式导航栏（MD3 Expressive Floating Navigation Bar）**：
   * **禁止使用传统的全宽贴底 `NavigationBar`**，必须采用 **Material Design 3 Expressive 悬浮式胶囊导航栏**（可使用 `HorizontalFloatingToolbar` 或基于 `Surface` 自定义全圆角胶囊悬浮容器：`shape = CircleShape`，`tonalElevation = 3.dp`，`shadowElevation = 6.dp`，背景使用 `surfaceContainerHigh`）。
   * **布局与留白**：悬浮居中置于屏幕底部，适配 `navigationBarsPadding()` 并额外保留底部 `16.dp` 悬浮外边距；选中项采用高对比度胶囊指示器（`secondaryContainer` 填充 + 弹性缩放/颜色过渡动效），支持图标与简短标签切换（首页 / 日志 / 设置）。
   * **主操作联动与防遮挡**：右下角（或悬浮导航栏右侧并排）可搭配一个同圆角体系的 Expressive FAB（用于“一键全部签到”）；所有页面的可滚动列表底部必须预留充足的 `contentPadding(bottom = 104.dp)`，确保最后一张卡片不会被悬浮导航栏遮挡。
3. **动态取色与深浅色自动切换**：
   * 启用 **Android 12+ 系统动态取色（Dynamic Color / Monet）**：在 `API >= 31` 时默认使用 `dynamicLightColorScheme(context)` / `dynamicDarkColorScheme(context)`，低版本回退至预设的精致紫色/靛蓝调色板；同时在设置页提供“动态取色开关”。
   * 启用 **设备深浅色自动切换**：默认 `isSystemInDarkTheme()` 跟随系统自动切换浅色/深色模式，并在设置页支持“跟随系统 / 浅色 / 深色”三态切换，配合 `enableEdgeToEdge()` 适配沉浸式状态栏与导航栏图标色彩。

---

## 3. 核心页面与交互设计
应用收敛为 **3 个主导航 Tab（首页 / 日志 / 设置）+ 1 个模态凭证导入 Sheet**：

1. **首页（账号概览）**
   * **顶部概览卡片**：展示今日签到总进度（如 `2/2 已完成`）、当前权限模式徽章（`基础模式` / `Root 模式`）与下次调度预计时间（若所有账号均因达上限而暂停，则显示“已全部达上限暂停，消耗时长后可恢复”）。
   * **账号卡片（`AccountCard`）**：每张卡片展示游戏徽标（云·原神 / 云·绝区零）、账号备注、状态胶囊（`DONE_TODAY` 绿色、`READY` 中性色、`CHECKING` 加载动画、`PAUSED_CAP_REACHED` 琥珀色“已达上限 · 自动暂停”、`NEED_REAUTH` 红色警示描边）、剩余免费时长进度条（如 `585 / 600 分钟`）、凭证来源标签，以及操作按钮（失效时高亮“更新凭证”；达上限暂停时提供“恢复签到”开关或按钮）。
   * **快捷操作**：通过底部悬浮区搭配的 Expressive FAB 执行“一键全部签到”，并可通过顶部或卡片入口触发添加账号。
2. **凭证导入 / 修复页（模态 `ModalBottomSheet`）**
   * 根据当前权限模式与游戏类型自动分流：
     * **Root 模式**：一键扫描本机 `com.miHoYo.cloudgames.ys` 与 `com.miHoYo.cloudgames.Nap` 的登录配置，展示识别到的账号 UID 与游戏类型，一键确认同步。
     * **基础模式 - 云·原神**：打开内置隔离 `WebView` 登录 `https://ys.mihoyo.com/cloud/#/`，顶部显示“正在监听登录凭证...”状态条，拦截到凭证后自动关闭并返回。
     * **基础模式 - 云·绝区零**：提供“从剪贴板识别（支持 `cURL` 与 HTTP Header）”按钮，解析后展示脱敏预览与字段完整性校验结果。
3. **运行日志页（Logs）**
   * 顶部筛选胶囊：`全部` / `仅异常`。
   * 折叠式列表：默认单行展示 `[时间] [游戏] [结果] [时长变化]`，点击可就地展开详细执行链路（如主方案失败原因、是否触发备用续期、是否因时长达上限自动暂停、HTTP 状态码），支持“复制脱敏诊断日志”。本地滚动保留最近 200 条。
4. **设置与调度页（Settings）**
   * **权限模式切换**：使用分段选择卡片切换 `基础模式` / `Root 模式`。点击切换 `Root 模式` 时，弹出实时探针检测 `su` 授权与 Doze 白名单写入结果，失败则自动回弹至 `基础模式` 并弹出 Snackbar 提示。
   * **调度、上限与通知配置**：每日基准时间选择器、随机抖动滑块（`0 ~ 30` 分钟）、自动备用续期开关、免费时长上限阈值配置（默认 `600` 分钟，达到即自动暂停）、每日签到结果通知开关、动态取色与深浅色模式切换。
   * **系统权限自检卡片**：一键检测并引导开启“通知推送权限（Android 13+ `POST_NOTIFICATIONS`）”、“电池优化无限制”与“精确闹钟权限（Android 14+）”。

---

## 4. 核心业务逻辑与技术细节

### 4.1 Root 模式直读官方客户端 XML
* **目标包名白名单（硬编码常量，严禁外部拼接）**：
  * 云·原神：`com.miHoYo.cloudgames.ys`
  * 云·绝区零：`com.miHoYo.cloudgames.Nap`
* **读取路径**：优先 `/data/user/0/<包名>/shared_prefs/`，回退 `/data/data/<包名>/shared_prefs/`。
* **解析三个明文 XML**：
  1. `mihoyo_login.xml`：提取 `sp_key_app_id` (`ai`)、`sp_key_channel_id` (`ci`)、`sp_key_open_id` (`oi`)、`sp_key_combo_token` (`ct`)。
  2. `mihoyo_sdk_preference.xml`：解析 `mdk_init_config` JSON（注意解码 XML 转义符 `&quot;`）获取 `game_key` (`bi`，如 `hk4e_cn` / `nap_cn`)。
  3. `pre_device.xml.xml`：提取标准 UUID 格式的 `device_id` 作为 `x-rpc-device_id`。
* **并发读取保护**：读取后校验 XML 文本包含完整 `</map>` 闭合标签，若解析异常则延迟 500ms 重试 1 次；`x-rpc-app_version` 通过 `PackageManager.getPackageInfo()` 动态获取本机安装版本（在 `Manifest` 添加 `<queries>`），其余设备型号/系统版本取自 `android.os.Build`。

### 4.2 基础模式凭证捕获
* **云·原神 `WebView`**：开启 JS 与 DOM Storage；在 `shouldInterceptRequest()` 中监听 `api-cloudgame.mihoyo.com` 的 `GET` 请求头，提取 `x-rpc-combo_token` 及全套 `x-rpc-*` 设备头；多账号登录前调用 `CookieManager.getInstance().removeAllCookies(null)` 清理会话。
* **云·绝区零剪贴板解析**：使用正则兼容解析 `cURL`（`-H 'key: val'`）与多行 HTTP Header（忽略大小写）；解析成功后立即调用 `ClipboardManager.clearPrimaryClip()`（低版本写入空内容）清理剪贴板隐私。

### 4.3 本地加密数据结构、状态机与上限自动暂停
* 使用 `EncryptedSharedPreferences`（`AES256_SIV` 键加密 + `AES256_GCM` 值加密）保存：
  * `AccountProfile`：`accountId`, `gameType`, `alias`, `region`, `isEnabled`, `credentialSource` (`ROOT_READ`/`WEBVIEW`/`CLIPBOARD`), `comboToken`, `deviceHeadersJson`, `hasWebviewSession`。
  * `CheckinState`：`accountId`, `status` (`PENDING_INIT` / `READY` / `CHECKING` / `DONE_TODAY` / `PAUSED_CAP_REACHED` / `NEED_REAUTH`), `lastCheckinDate`（**按北京时间 `ZoneId.of("Asia/Shanghai")` 判定**）, `lastAttemptAt`, `freeMinutes`, `totalMinutes`, `failCount`, `lastErrorMsg`。
  * `AppGlobalConfig`：权限模式、基准时间、抖动分钟数、免费时长上限阈值（默认 `600`）、通知开关、外观偏好。
* **主备切换与上限暂停决策链**：
  1. 跨天重置：北京时间零点后，检查到 `lastCheckinDate != 今日` 且状态为 `DONE_TODAY` 时重置为 `READY`（**`PAUSED_CAP_REACHED` 与 `NEED_REAUTH` 不自动重置**）。
  2. 主方案：用本地 `comboToken` 与绑定的 `deviceHeadersJson` 请求云游戏钱包/状态接口（如云·原神 `https://api-cloudgame.mihoyo.com/hk4e_cg_cn/wallet/wallet/get`）。
  3. **时长上限自动暂停**：每次请求成功后解析返回的 `freeMinutes`，若 `freeMinutes >= maxFreeMinutes`（默认 600 分钟），立即将该账号的 `isEnabled` 置为 `false`、状态转为 `PAUSED_CAP_REACHED`，并发送高优先级上限提示通知；若所有账号均已暂停，自动取消后续 `AlarmManager` 与 `PeriodicWork` 调度直到用户恢复任一账号。
  4. 瞬态网络错误：保持 `READY`，`WorkManager` 指数退避重试（最多 3 次）。
  5. 认证失效错误：若开启自动备用，Root 模式重读官方 XML 对比更新后补签 1 次；基础模式下云·原神通过无界面/后台 `WebView` 尝试刷新后补签 1 次；云·绝区零或补签仍失败则立即熔断置为 `NEED_REAUTH`，停止重试并发送高优先级失效通知。

### 4.4 通知栏消息推送规范
* 拆分两个独立 `NotificationChannel`：
  1. `checkin_summary`（`IMPORTANCE_DEFAULT`）：每次每日定时签到流程全部执行完毕后，合并推送一条**完成状况汇报通知**（标题如“今日云游戏签到完成 (2/2)”，正文采用 `InboxStyle` 或展开文本逐条列出各账号状态、新增时长及当前总剩余免费时长，点击通知跳转首页）。
  2. `checkin_alert`（`IMPORTANCE_HIGH`）：当任意账号因**免费时长达上限自动暂停**，或发生 **`NEED_REAUTH` 凭证彻底失效** 时推送高优先级提醒，点击直达首页或对应修复弹窗。

### 4.5 后台定时与保活组合
* **链式闹钟 + 加急任务**：`AlarmManager.setExactAndAllowWhileIdle()`（无精确闹钟权限时降级 `setAndAllowWhileIdle()`）预约“明日基准时间 + 0~`jitter` 分钟随机偏移”；触发 `AlarmReceiver` 后立即入队带 `NetworkType.CONNECTED` 约束的 `OneTimeWorkRequest`（调用 `.setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)` 并实现 `getForegroundInfo()` 托管短时静音通知），同时预约明日闹钟。
* **开机恢复与周期兜底**：监听 `BOOT_COMPLETED` 恢复闹钟（若存在已启用账号）；并行注册每 12 小时一次的 `PeriodicWorkRequest` 作为漏签巡检兜底。
* **Root 增强**：切入 Root 模式时执行 `dumpsys deviceidle whitelist +<当前应用包名>`。

---

## 5. 安全红线（必须严格遵守）
1. **防命令注入**：所有 `su` 命令仅允许读取硬编码白名单包名与固定文件名，严禁拼接任何用户输入。
2. **组件隔离**：所有非启动 `Activity`、`Service`、`BroadcastReceiver`（除系统 `BOOT_COMPLETED` 接收器）均设置 `android:exported="false"`。
3. **日志脱敏**：Release 模式下禁止打印完整 HTTP Header；UI 日志中对 `ct=`、`combo_token`、`open_id` 进行中间打码（仅保留前 4 后 4 位）。
4. **来源强绑定**：绝对禁止将安卓客户端凭证与网页端 `client_type` / UA 混搭。
5. **WebView 安全**：`shouldOverrideUrlLoading` 仅放行 `*.mihoyo.com` 与 `*.hoyoverse.com`，关闭 `allowFileAccess` 与 `allowContentAccess`，`AndroidManifest.xml` 设置 `android:usesCleartextTraffic="false"`。

---

## 6. 输出格式要求
请按以下顺序完整输出项目文件，确保包名统一（如 `com.example.cloudcheckin`）且代码可直接编译：
1. 项目结构目录树
2. `app/build.gradle.kts` 与 `AndroidManifest.xml`
3. 数据模型、加密存储与状态机管理类
4. Root 读取、WebView 拦截、剪贴板解析与网络签到客户端
5. 通知推送管理类与调度引擎（`AlarmReceiver`、`CheckinWorker`、`BootReceiver`）
6. MD3 Expressive 主题系统（含动态取色与深浅色切换）及包含悬浮式底部导航栏的 4 个完整 Compose UI 页面
