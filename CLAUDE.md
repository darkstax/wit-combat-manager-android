# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

WIT 战斗管理器 — Kotlin/Compose Android 原生移植版，替代 Python/Kivy 版本。
目标设备：Android 5.1+ (API 21+) 平板，2560x1600 横屏。

## 构建命令

```bash
# 编译 DEBUG APK
./gradlew assembleDebug

# 运行单元测试
./gradlew test

# 安装到已连接的模拟器/设备
adb -e install -r app/build/outputs/apk/debug/app-debug.apk

# 启动应用
adb -e shell am start -n com.witcombat.manager/.ui.MainActivity
```

**构建后必须立即执行**：将 APK 复制到 `release/wit-combat-manager-android/native-android/`，以时间戳命名，并清理旧版本只保留最新 3 个。

```powershell
$ts = Get-Date -Format 'yyyyMMdd-HHmmss'; Copy-Item app/build/outputs/apk/debug/app-debug.apk "release/wit-combat-manager-android/native-android/wit-combat-manager-$ts.apk"; $files = Get-ChildItem "release/wit-combat-manager-android/native-android" | Sort-Object LastWriteTime -Descending; if ($files.Count -gt 3) { $files | Select-Object -Skip 3 | Remove-Item -Force }
```

## 架构

三层结构，domain 层零 Android 依赖：

```
domain/         纯 Kotlin，可直接单测
├── GameUnit.kt          数据模型（注意：原名 Unit，与 kotlin.Unit 冲突已改名）
├── CombatState.kt       战斗会话状态（注意：非 data class 需要 updateCounter 驱动 UI）
├── StatusDefinitions.kt 游戏常量表（状态效果、元素损伤、韧性等）
└── CombatEngine.kt      纯函数：先攻/伤害/治疗/状态/回合管理

data/
└── UnitRepository.kt    JSON 文件持久化（Gson），存于 app 私有 filesDir

ui/
├── MainActivity.kt      Compose 入口，enableEdgeToEdge
├── MainViewModel.kt     StateFlow 状态管理，所有操作通过 updateState() helper
├── MainScreen.kt        主界面：左右分栏 + 底部日志/GM笔记 + 右下角 FAB 菜单
├── UnitEditDialog.kt    单位增删改弹窗（包含全部状态复选框）
├── QuickImportDialog.kt 骰娘文本解析导入
└── AppTextField.kt      原生 EditText 包装器（见下方"API 22 兼容"）
```

## 工作资料

`工作资料/` 目录存储测试数据和游戏规则参考文件，实现游戏逻辑时优先查阅：

| 文件 | 用途 |
|------|------|
| `buff.txt` | 全部 buff/debuff 规则表，定义哪些状态需要 X 参数、结束条件 |
| `元素损伤.txt` | 元素损伤类型规则 |
| `测试资料0.xlsx`、`测试资料1.xlsx` | 测试用数据表 |
| `快速导入.txt` | 快速导入文本格式参考 |

## 关键设计决策 & 坑

### StateFlow 更新强制触发

`CombatState` 的 `turn`/`nowIndex` 在 `CombatEngine` 中原地修改。如果直接 `_uiState.update { it.copy(combatState = sameRef) }`，`MutableStateFlow` 用 `equals` 判等会跳过 emit。

**解决方案**：`UiState` 含 `updateCounter: Long` 字段，`updateState()` helper 每次更新自动 +1。

### API 22 文本输入兼容

Material3 的 `OutlinedTextField` 在 Android 5.1 上完全不兼容——文字不可见、输入行为异常（拼接而非替换）。

**解决方案**：`AppTextField` 用 `AndroidView` 包装原生 `EditText`，`setSelectAllOnFocus(true)` 保证点击即全选替换。所有文本输入必须使用此组件，禁止直接使用 Compose 的 `OutlinedTextField`。

### GameUnit 命名

Kotlin 标准库有 `kotlin.Unit`（对应 Java `void`）。领域模型 `Unit` 会与其冲突，导致所有属性访问解析为 `kotlin.Unit`。**必须使用 `GameUnit`**，引入新文件时注意 import。

### 最小 SDK

`minSdk = 21` (Android 5.0)，Compose 最低要求。`compileSdk = 34`。

## 模拟器测试

**重要：所有模拟器命令必须在 PowerShell 中执行（不在 Bash），因为 `emulator` 不在 Git Bash 的 PATH 中。**

SDK 环境路径：
- `ANDROID_HOME` = `C:\Users\StarL\Android\Sdk`
- `JAVA_HOME` = `C:\Users\StarL\scoop\apps\corretto17-jdk\current`

已创建 AVD：`test_5.1`（API 22, Google APIs, x86_64, Nexus 10 tablet）。

```powershell
# 启动模拟器 (PowerShell)
& "$env:ANDROID_HOME\emulator\emulator.exe" -avd test_5.1 -no-snapshot -gpu swiftshader_indirect
```

## 工作空间级规则

参见 `../CLAUDE.md`：构建产物统一输出到 `release/wit-combat-manager-android/native-android/`，保留最新 3 个。
