# T02 Gradle 依赖版本清单

所有版本集中在 `gradle/libs.versions.toml`，不存在 `latest`、动态版本或 `+`。

| 项目 | 锁定版本 |
|---|---|
| JDK | 17（本机构建为 17.0.19） |
| Gradle Wrapper | 8.9 |
| Android Gradle Plugin | 8.7.3 |
| Kotlin Android / Compose plugin | 2.0.21 |
| compileSdk / targetSdk / minSdk | 35 / 35 / 23 |
| Compose BOM | 2024.12.01 |
| Activity Compose | 1.10.0 |
| Navigation Compose | 2.8.5 |
| Room | 2.8.4 |
| DataStore | 1.2.1 |
| JUnit | 4.13.2 |
| AndroidX Test Ext JUnit | 1.2.1 |
| AndroidX Test Runner | 1.6.2 |
| Espresso | 3.6.1 |

说明：`gradlew --version` 中显示的 Kotlin 1.9.23 是 Gradle 8.9 自身内置 Kotlin DSL 版本；Android 应用插件明确锁定为 Kotlin 2.0.21。
