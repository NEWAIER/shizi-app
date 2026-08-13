# T04测试与构建报告

- APK：`shizi-t04-resources-g2-v1.1-debug.apk`
- versionCode/versionName：`5` / `1.1-t04`
- APK SHA-256：`9aae11781c80f42b9cdee315ff4dc64ca88a88fc6df2ba9cad40ead7911d5362`
- 单元测试：46项，0失败，0错误，0跳过

| 命令 | 结果 | 摘要 |
|---|---|---|
| `gradlew --version` | PASS | Gradle 8.9，JDK 17.0.19 |
| `gradlew clean testDebugUnitTest verifyContentG1 verifyContentG2 lintDebug assembleDebug --no-daemon` | PASS | `BUILD SUCCESSFUL in 2m 42s`；55个任务执行 |
| APK assets逐字节对比 | PASS | 53个content assets；38 MP3、12 WebP、JSON/Schema/manifest各1 |
| 候选音色排除 | PASS | APK中无A/B候选文件 |
| `aapt dump permissions` | PASS | 仅应用自身动态接收器signature权限 |

完整Gradle日志、ffprobe/FFmpeg原始检测、APK assets原始清单和逐字节对比结果均随包提交。

本轮未执行儿童试用，未进入T05。
