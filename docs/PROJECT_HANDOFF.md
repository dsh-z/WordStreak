# WordStreak 项目交接文档（PROJECT_HANDOFF）

> 本文件依据**当前仓库实际状态**与 **GitHub 实际状态**整理（非推测）。
> 生成于分支 `feature/stage-3.4-review`，提交 `99e1e75`。
> 仓库：`dsh-z/WordStreak`（当前为 public）。

---

## 1. 项目简介 / 技术栈 / 包名

- 项目名称：**WordStreak**（英语单词学习 App，离线优先，第一版无需登录）
- 语言：**Kotlin**
- UI：**Jetpack Compose + Material 3**（黑白灰 + 绿色强调，圆角卡片、极简浅色）
- 数据：**Room + SQLite**（当前数据库版本 **VERSION = 2**），Coroutines + Flow，**MVVM + Repository**
- 异步/偏好：Kotlin Coroutines、DataStore(Preferences)
- 架构：单 Activity + Navigation Compose；`data/`（local: entity/dao/converter; repository/model）、`domain/`（model/usecase）、`ui/`（home/vocabulary/study/review/calendar/settings/components/theme）、`navigation/`、`di/AppContainer`（轻量服务定位器）
- 包名（applicationId）：**`com.wordstreak`**
- 版本：versionCode **1**，versionName **0.1.0**；compileSdk **37**，targetSdk 37，minSdk **24**
- 构建工具链（实测）：AGP **9.5.1**、Gradle **9.8.2**（wrapper）、Kotlin **2.4.21**、Room **2.8.5**、KSP 2.3.13、DataStore 1.3.0、Coroutines 1.11.0

---

## 2. 已完成阶段及主要功能（Stage 1 → Stage 3.4）

- **Stage 1 — UI 框架**：五个主页面（首页 / 词库 / 学习 / 日历统计 / 设置），底部导航，`ScreenScaffold`、`EmptyState`，Material3 主题。
- **Stage 2 — 数据库**：8 个 Entity（`WordBook`、`Word`、`StudySession`、`StudyRecord`、`ReviewState`、`DailyStat`、`CheckIn`、`UserSetting`）、DAO、Repository、迁移框架（原始 DB v1）。
- **Stage 2.1 — 数据层稳定性/历史保护**：`StudyRecord.wordId` 改可空 + `ON DELETE SET NULL` + `wordSpelling/wordMeaning` 快照；**数据库 v1→v2 迁移（MIGRATION_1_2）**；标准 Linux CI（GitHub Actions，官方未改 Room 2.8.5 + 查询验证 + emulator connectedAndroidTest）。
- **Stage 3.1 — 词库管理**：词库创建/编辑/删除、词库列表（含单词数，`BookWithWordCount`）、词库详情单词列表、单词增删改查、搜索、编辑判重（排除自身、大小写不敏感）、删除确认（提示 ReviewState 清理、历史快照保留）。
- **Stage 3.2 — 学习流程**：选择词库开始学习、翻转卡片（`StudyCard`）、学习结果汇总、评分原子事务（`saveRatingAndSession` / `insertRecordAndUpdateSession`），学习记录与会话持久化。
- **Stage 5 — 学习日历与统计**：月度日历（有学习日期标记、月份切换、今日/选中）、每日统计（学习单词数/次数/正确错误，来自 `study_records` 实时计算）、连续打卡（当前/历史最长，来自 `check_ins`）、空状态。
- **Stage 3.3 — 基础复习**：待复习列表（新词/到期词，`ReviewDao` LEFT JOIN）、复习卡片（复用 `StudyCard`）、AGAIN/HARD/GOOD/EASY 评分、`ReviewState` 更新（到期日/间隔/次数）、待复习与已完成计数、空状态。
- **Stage 3.4 — 复习算法优化**：基于历史间隔 `ReviewState.intervalDays` 的动态间隔（基础 SM-2-lite）：`AGAIN=1；HARD=max(2,ceil(prev*1.2))；GOOD=max(4,prev*2)；EASY=max(7,prev*3)`，统一上限 **365 天**；旧固定间隔方法保留兼容。

---

## 3. 各阶段分支 / 提交 / PR（实测）

说明：Stage 1/2/2.1 的代码在**建 GitHub 仓库时合并进 main**（首个提交 `84f5c09`，随后 `8572772`），**无独立 PR**；PR #1 起为阶段开发。

| 阶段 | 分支 | 关键提交 | PR（状态） |
|---|---|---|---|
| Stage 1/2/2.1 | `main` | `84f5c09` → `8572772` | 无（并入 main） |
| Stage 3.1 词库管理 | `feature/stage-3.1-vocabulary` | `4d124c8`,`9135fba`,`712f893`,`299b83c`(+`7d8c538` 源码ZIP) | **#1**（open, draft；base=main） |
| Stage 3.2 学习流程 | `feature/stage-3.2-study` | `0f9672c`…`b663dbf`（10 提交） | **#2**（open, draft；base=stage-3.1） |
| Stage 5 日历统计（早期） | `feature/stage-5-calendar-statistics` | `12a16bb` | **#3**（open, draft；base=stage-3.1） |
| Stage 5 整合（学习+日历） | `feature/stage-5-integrated` | `8f0fe5b`（cherry-pick 到 stage-3.2） | **#4**（open, draft；base=stage-3.2） |
| Stage 3.3 复习 | `feature/stage-3.3-review` | `8b76038` | **#5**（open, draft；base=stage-5-integrated） |
| Stage 3.4 复习算法优化 | `feature/stage-3.4-review` | `99e1e75` | **#6**（open, draft；base=stage-3.3-review） |

---

## 4. 当前 main 分支状态

- `main` = **`8572772`**（本地与远端一致）。
- 内容：Stage 1 + Stage 2 + Stage 2.1（Room 数据层、v1→v2 迁移、历史保护、标准 CI）。**不包含 Stage 3.x / Stage 5 的功能代码**（这些均在各自特性分支/Draft PR 上）。

---

## 5. 所有未合并 PR 及依赖关系（均为 open + draft）

- **#1** `feature/stage-3.1-vocabulary` → `main`（词库管理）
- **#2** `feature/stage-3.2-study` → `feature/stage-3.1-vocabulary`（学习流程）
- **#3** `feature/stage-5-calendar-statistics` → `feature/stage-3.1-vocabulary`（日历统计·早期独立分支）
- **#4** `feature/stage-5-integrated` → `feature/stage-3.2-study`（**学习+日历整合**；基于 stage-3.2 尖端）
- **#5** `feature/stage-3.3-review` → `feature/stage-5-integrated`（复习）
- **#6** `feature/stage-3.4-review` → `feature/stage-3.3-review`（复习算法优化）

依赖链（自底向上）：`main → #1 → #2 → #4 → #5 → #6`；另 `#3` 单独基于 `#1` 分支（早期日历版本，未进入整合链）。
**均未合并**；无破坏性操作。合并顺序建议：#1→#2→#4→#5→#6（#3 视需要处理/关闭）。

---

## 6. 最新 CI 结果与测试数量（实测）

- 标准环境：GitHub Actions `ubuntu-latest`（glibc）、JDK 21、Android SDK 预装、Emulator API 34 x86_64。
- **Stage 3.4 最新成功 CI**：run **43156661099**（`feature/stage-3.4-review` @ `99e1e75`）→ **success**
  - Build（官方 Room 2.8.5 + 查询验证）：`BUILD SUCCESSFUL`（约 2m8s）
  - connectedAndroidTest（emulator）：**tests = 59 / failures = 0 / errors = 0 / skipped = 0**
  - 测试分布：`WordStreakReviewScheduleTest 11`（Stage3.4 新增）、`WordStreakReviewTest 6`、`WordStreakDatabaseTest 21`、`WordStreakMigrationTest 1`、`WordStreakStatsTest 12`、`WordStreakStudyTest 4`、`WordStreakVocabularyTest 4`。
- 其他分支最近成功 run：stage-3.3 `43154700909`(48 测试) / stage-5-integrated `43149325512`(42) / stage-5-calendar `43138353540` / stage-3.2 多个 success / stage-3.1 多个 success / main `43100795816`(22)。
- 备注：PR 创建/推送时会额外触发一次 `pull_request` 事件运行（可能处于 in_progress）；以上以 dispatch 触发的成功运行为准。

---

## 7. 手机当前安装版本（实测）

- 已安装包：`com.wordstreak`，versionCode **1** / versionName **0.1.0**。
- 安装来源：**Stage 3.3 的 CI `app-debug.apk`**（`feature/stage-3.3-review` @ `8b76038`）。
- 已安装签名证书 SHA-256：**`bcdcd620f71728bb5444a3666934e9bd34ae45e8230f1739ca61387cc29af212`**（V2，debug 证书 `CN=Android Debug`）。
- 另存在测试包 `com.wordstreak.test`（instrumented 测试 APK，属正常，未卸载）。

---

## 8. Stage 3.4 当前状态

- 分支：`feature/stage-3.4-review`，提交 **`99e1e75`**（基于 `feature/stage-3.3-review` @ `8b76038`）。
- PR：**#6**（Draft，base=`feature/stage-3.3-review`）。
- CI：已通过（run 43156661099，tests=59）。
- **尚未真机验收**（未安装到手机）。
- 改动范围（3 文件）：`domain/usecase/ReviewScheduler.kt`（新增基于历史间隔的动态计算 + 兼容旧方法）、`data/repository/ReviewRepository.kt`（使用 `intervalDays` 作为历史间隔）、`androidTest/WordStreakReviewScheduleTest.kt`（11 项：增长/重置/上限/单调/边界/DB 持久化）。
- 未改 Entity/DAO/DB(VERSION=2)/UI；无迁移、无 destructive、无数据清除。

---

## 9. APK 签名差异问题及此前处理方式（重要）

**根因**：GitHub Actions 每次运行使用**临时生成的 debug keystore**，因此不同 CI 运行产出的 `app-debug.apk` 签名不同；本机本地 Gradle 构建使用本机默认 debug keystore，手机已安装版本又来自某次 CI（或本地）产物。多处证书因此互不一致。

已观测到的证书 SHA-256（实测）：
- 本机默认 debug keystore（Gradle 实际使用，Java `user.home=/data/data/com.termux/files/home`）：`~/.android/debug.keystore` → **`6a5db5778ac3e8dc00323f3a8bb9e726bb7a6a4815d76325f284525dce415e05`**
- 另一处 `$HOME(dsh-home)/.android/debug.keystore` → `959c68bd…`（**非 Gradle 实际使用**，勿混淆）
- Stage 5 CI APK → `8d24f0d2…`
- Stage 3.3 CI APK（= 手机当前安装）→ `bcdcd620…`
- Stage 3.4 CI APK → `59404133…`
- 曾为 Stage 5 生成“匹配现有安装签名”的重签副本 `app-debug-matched.apk`（用 `~/.android/debug.keystore` 重签为 `6a5…`）。

**此前处理方式**：
1. **不能直接覆盖**（签名不同会 `INSTALL_FAILED_UPDATE_INCOMPATIBLE`）。
2. 采取**先卸载旧版、再全新安装**（会清除应用数据；用户已明确接受）。
3. `/storage/emulated/0/...` 直接用 `pm install` 会被 **SELinux 拦截**（`system_server` 无法读 `fuse` 文件）；实际做法是复制到 `/data/local/tmp/xxx.apk` 再 `pm install -r -t`。
4. 也可用匹配安装版签名的 keystore **重签** APK（曾为 Stage 5 生成 `app-debug-matched.apk`）——但本轮要求不擅自重签。

> 结论：Stage 3.4 CI APK（`59404133…`）与手机现有安装（`bcdcd620…`，Stage 3.3）**签名不一致 → 不能直接覆盖安装**。

---

## 10. 下一步任务（Stage 3.4 真机验收）

1. **获取 Stage 3.4 APK**：来自 CI run `43156661099` 构件 `WordStreak-debug-apk`（对应提交 `99e1e75`）。已保存：`/storage/emulated/0/Download/WordStreak-Stage3.4/app-debug.apk`（APK SHA-256 `abf72ebd6bd1a6559b9f796496f16de080381d012b73c5d2a837c293bf392742`；签名 `59404133…`；versionName 0.1.0）。
2. **检查签名**：CI 证书 `59404133…` ≠ 手机现有 `bcdcd620…` → 需处理签名差异。
3. **准备真机安装验收**（二选一，实施时由用户确认）：
   - **(A) 卸载重装**：`pm uninstall com.wordstreak`（数据清除，需接受）→ 复制 APK 到 `/data/local/tmp/` → `pm install -r -t /data/local/tmp/app-debug.apk`；
   - **(B) 重签匹配**：用与现安装匹配的 keystore 重签（本轮禁止，需用户另行授权）。
4. **启动验证**：`am start -W -n com.wordstreak/.MainActivity`，确认 `Status: ok`、前台 activity、无崩溃、versionName=0.1.0；可进入「学习 → 单词复习」查看复习功能（真实数据来自 DB）。
5. 不合并 PR、不改 main、不清除无关数据。

---

## 附：开发 / 构建 / 测试环境要点（本机 bionic 实测）

- 本机为 **Android bionic（无 glibc）**：
  - **本地编译需加** `-Pwordstreak.localBionic=true`（使用 `localrepo/` 中打桩的 `room-compiler:2.8.5-patched`，仅规避 bionic 无法加载 glibc 原生 SQLite；`localrepo/` 已被 git 忽略，不进标准构建）。标准/CI 构建走**官方 Room 2.8.5**，切勿在标准配置中启用 localBionic。
  - 命令示例：`gradle -Pwordstreak.localBionic=true :app:assembleDebug :app:assembleDebugAndroidTest`。
- 真机测试（本机设备，经 adb）：
  - 开启 adb TCP：`setprop service.adb.tcp.port 5555; setprop ctl.restart adbd; adb connect 127.0.0.1:5555`。
  - **`am instrument` 需通过 `adb shell` 执行**（shell 域可用；root 直接 `app_process`/`am instrument` 会静默失败）。`uiautomator dump` 常被 OOM 杀掉；**截图用 `adb exec-out screencap -p` 正常**。
  - `/storage` 安装被 SELinux 拦截 → 复制到 `/data/local/tmp` 再 `pm install`。
- CI：`.github/workflows/android-ci.yml` + `gradlew`；`workflow_dispatch` 可对任意分支手动触发；`connectedAndroidTest` 在 API34 emulator 上运行。

---

_（如需更新，请在此基础上修改本文件，勿重复创建。）_
