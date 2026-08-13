# T03测试与构建报告

- 执行日期：2026-07-26
- 范围：仅T03资源路径类型修复与G1复验
- APK：`shizi-t03-content-g1-v1.1-debug.apk`
- versionCode/versionName：`4` / `1.1-t03`
- APK SHA-256：`b80b5a3716a0dc7dfd1d10dabdd56da8d41b286e0e2c391bd315640add43a1a1`
- 正式content.json SHA-256：`ff0e64aefaa3685ca4181fe25fc0f5328c62da033ceba2f151bc42042b38df7e`
- 单元测试：31项，0失败，0错误，0跳过

## 实际命令与结果

| 命令 | 结果 | 摘要 |
|---|---|---|
| `gradlew.bat clean verifyContentG1 lintDebug assembleDebug --console=plain --no-daemon` | PASS | `BUILD SUCCESSFUL in 2m 28s`，55个任务全部执行 |
| `aapt dump permissions app-debug.apk` | PASS | 仅应用自身动态接收器signature权限，无敏感权限 |
| `tar -tf app-debug.apk` assets筛选 | PASS | 仅包含正式content.json和content.schema.json |

完整Gradle原始日志见 `T03_BUILD_RESULT.log`；权限原始输出见 `AAPT_PERMISSIONS.txt`；APK assets原始清单见 `T03_APK_ASSETS.txt`。

## 本轮新增反向测试

1. IMAGE引用MP3：返回`RESOURCE_PATH_INVALID`。
2. AUDIO引用WebP：返回`RESOURCE_PATH_INVALID`。
3. CONTEXT引用MP3：返回`RESOURCE_PATH_INVALID`。
4. promptAudio引用WebP：返回`RESOURCE_PATH_INVALID`。
5. imageAsset引用MP3：返回`RESOURCE_PATH_INVALID`。
6. Schema分别拒绝上述OptionKind错配、promptAudio错配及imageAsset错配。

正式`content.json`未修改；未制作图片、音频或资源manifest；未开始T04。
