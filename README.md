# 今天吃什么

一个帮你决定今天吃什么的 App。四个餐段（早餐 / 午餐 / 晚餐 / 夜宵）各一个转盘，转一下就有了；也可以从菜库里按口味自己挑。

Android + Windows 桌面同一套代码（Kotlin Multiplatform + Compose Multiplatform），UI 和业务逻辑全在 `commonMain`，两个端只是薄壳。

## 功能

- **四个餐段的转盘**：按菜库权重、时段亲和、最近吃过衰减抽取，同盘内主料不重复（不会一盘子全是豆皮或羊肉）
- **两种取菜来源**：本地随机（只从自己的菜库抽）/ 网络推荐（从菜谱源实时拉）
- **自定义菜库**：新增、编辑、启停、按餐段筛选，支持导入导出；菜品带实拍图，搜不到时回落品类图
- **加菜只要写个名字**：菜系 / 时段 / 辣度 / 价格 / 食材 / 配图全部按菜名自动补全，存下来之前每个字段都能手动改；图片会自动去搜实拍图，也可以从相册自己挑一张（复制进应用私有目录，卸载重装也不丢）
- **在线灵感**：多源调度 + 永久去重 + 边推边补，源失效自动切换，国内源优先、海外兜底
- **食谱**：点任意菜进详情，看食材清单和做法步骤
- **价格估算**：按食材清单和用量算「自己做这道菜的买菜成本」，不按菜名猜
- **记录**：今日各餐段已定、最近吃过、7 天打卡、最常吃菜系
- **多主题**：几套配色，跟着餐段变换背景光晕

## 技术栈

| | |
|---|---|
| 语言 / 构建 | Kotlin 2.4.21、Gradle 9.8.1、AGP 9.4.1 |
| UI | Compose Multiplatform 1.12.2 |
| 模块 | `:shared`（业务 + UI）、`:androidApp`、`:desktopApp` |
| Android | minSdk 24 / targetSdk 37，applicationId `com.chisha.app` |
| 图片 | Coil 3 + OkHttp |

## 构建

需要 JDK 17+、Android SDK。

```bash
# Android 安装包
./gradlew :androidApp:assembleDebug
# 产物：androidApp/build/outputs/apk/debug/androidApp-debug.apk

# Windows 桌面端直接跑
./gradlew :desktopApp:run
```

桌面端带几个无窗口自查入口，方便验证逻辑：

```
--selfcheck              抽签/时段/权重/源切换断言
--feedtest N             连续取 N 道看会不会打转，打印每次耗时
--batchtest              首页「网络推荐」首屏耗时
--phototest 菜名,菜名      按菜名搜实拍图，打印命中的图
--pricetest / --priceaudit  价格档位与全量审计
--tab N --section M      直开指定页
```

## 安装包

两条路都通：

- 仓库根目录的 `chisha-1.2.apk`（已入库，跟着提交一起更新）
- [Releases](../../releases) 里的同名附件

两者的 sha256 应当一致（`1dcbf806…`）。机器上装了旧版的话，直接 `adb install -r chisha-1.2.apk` 覆盖即可。

## 数据

本地数据（菜库、记录、偏好、图片缓存）存在用户目录的 `~/.chisha/`，不进版本库。
