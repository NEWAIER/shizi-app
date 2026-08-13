# T02 文件变更清单

## 工具链

- 新增 `gradle/libs.versions.toml`。
- 根 `build.gradle.kts` 改为版本目录插件别名。
- `settings.gradle.kts` 格式化并锁定仓库来源。
- `app/build.gradle.kts` 迁移 Kotlin＋Compose，加入锁定依赖和测试配置。

## 应用骨架

- 新增 `ShiziApplication.kt`、Kotlin `MainActivity.kt`。
- 新增 `navigation/ShiziRoute.kt`、`navigation/ShiziNavHost.kt`。
- 新增五个页面包和五个空页面 Composable。
- 新增 `ui/components/EmptyRouteScreen.kt` Debug 路由验证入口。
- 新增 `ui/theme/Color.kt`、`Theme.kt`、`Type.kt`。

## 测试与文档

- 新增 JUnit 路由测试和 Compose instrumentation 导航测试。
- 更新 README、DEVICE_CHECK、TEST_REPORT 和文件变更清单。
- 新增隐私权限、合并 Manifest、路由对应、依赖版本、T02 对照表。

## 删除

- 删除 T01 专用 Java Activity、Room Entity/DAO/Database 和测试 MP3。
- 没有添加内容 JSON、正式图片/音频或任何业务实现。
