请基于我们**当前已经编写好的 Android 项目（`MhyCloud-Checkin`）**进行精准修复，**仅修改以下 4 个文件**，保留其余架构、UI 页面、状态机、加密存储与调度逻辑不变：

---

## 1. 修改 `app/src/main/java/com/example/data/Models.kt`

根据实机抓包结果，修正两款游戏的真实 `walletEndpoint`：
* **`GameType.GENSHIN`（云·原神）**：
  `"https://api-cloudgame.mihoyo.com/hk4e_cg_cn/wallet/wallet/get?cost_method=0&get_type=0"`
* **`GameType.ZZZ`（云·绝区零）**：
  `"https://cg-nap-api.mihoyo.com/nap_cn/cg/wallet/wallet/get?cost_method=0&get_type=0"`

---

## 2. 修改 `app/src/main/java/com/example/core/RootHelper.kt`

1. **修正 `si=`（Session ID）获取逻辑**：
   * 严禁再把 `si=` 硬填为 `hk4e_cn` 或 `nap_cn`。
   * 在包名与路径白名单中新增 `databases/kibana_db_cloud_game`，通过单例 `su` Shell 执行 `grep -aoE 'si=[0-9a-fA-F]{32,128}' /data/user/0/$pkg/databases/kibana_db_cloud_game 2>/dev/null | tail -n 1`（若目录不存在则回退 `/data/data/$pkg/databases/kibana_db_cloud_game`），提取官方客户端缓存的最新 64 位十六进制 `si` 值；若未匹配到则置为空字符串 `""`。
2. **对齐实机安卓端请求头**：
   * 公共头：`x-rpc-client_type = "2"`（不再是 `"4"`）、`x-rpc-vendor_id = "2"`、`x-rpc-language = "zh-cn"`、`User-Agent = "okhttp/4.10.0"`。
   * **云·原神（`com.miHoYo.cloudgames.ys`）专属头**：
     * `x-rpc-app_id = "1953439974"`
     * `x-rpc-channel = "miyoushe"`
     * `x-rpc-cps = "miyoushe"`
     * `x-rpc-op_biz = "clgm_cn"`
     * `x-rpc-cg_game_biz = "hk4e_cn"`
     * `x-rpc-cg_game_id = "9000055"`
   * **云·绝区零（`com.miHoYo.cloudgames.Nap`）专属头**：
     * `x-rpc-app_id = "1953458679"`
     * `x-rpc-channel = "mys_cloud"`
     * `x-rpc-cps = "mys_cloud"`
     * `x-rpc-op_biz = "clgm_nap-cn"`
     * `x-rpc-cg_game_biz = "nap_cn"`
     * `x-rpc-cg_game_id = "9000357"`

---

## 3. 修改 `app/src/main/java/com/example/core/CloudGameApiClient.kt`

根据实测响应体 `{"retcode":0,"data":{"free_time":{"send_freetime":"0","free_time":"300","free_time_limit":"600"},"total_time":"387",...}}` 调整 JSON 解析：
* 兼容 `data.free_time.free_time` 与 `data.total_time` 为**字符串**或整数的情况（统一安全转换为 `Int`）。
* 提取今日新增时长时，同时兼容 `send_freetime`（无下划线，实际接口返回字段）与 `send_free_time`。

---

## 4. 修改 `app/src/main/java/com/example/core/WebViewCredentialManager.kt`

修复 `shouldInterceptRequest` 过早截获未登录预检请求导致 `retcode=-100` 的问题：
* **收紧拦截条件**：仅当请求方法为 `GET`、URL 包含 `"wallet/wallet/get"`，且请求头 `x-rpc-combo_token` 同时匹配到**非空数字 `oi=`**（如 `oi=\d+`）和**非空 `ct=`**（如 `ct=[^;]+` 且非占位符）时，才执行 `captured.compareAndSet(false, true)` 并回调保存凭证。
* 完整保留该次请求中的所有 `x-rpc-*`、`User-Agent`、`Referer` 与 `Origin` 请求头，确保与网页端 `combo_token` 严格绑定。

---

## 输出要求
请只输出上述 4 个修改后的完整 Kotlin 文件源码，不要改动或重复输出其他无关文件。
