# T02 隐私与权限检查

## AT-28 APK 权限检查

执行命令：

```text
aapt dump permissions app/build/outputs/apk/debug/app-debug.apk
```

原始输出：

```text
package: com.family.shizi
permission: com.family.shizi.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION
uses-permission: name='com.family.shizi.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION'
```

该权限由 AndroidX 自动生成，为本应用自身 `signature` 保护级权限，不是系统敏感权限。APK 不包含 INTERNET、RECORD_AUDIO、CAMERA、位置、通讯录、外部存储或 MANAGE_EXTERNAL_STORAGE 权限。AT-28：PASS。

## AT-29 云备份与明文流量

最终合并 Manifest 原始片段：

```xml
<application
    android:name="com.family.shizi.ShiziApplication"
    android:allowBackup="false"
    android:fullBackupContent="false"
    android:usesCleartextTraffic="false" />
```

AT-29：PASS。

## SDK 与组件检查

- 无广告 SDK、分析 SDK、联网崩溃收集 SDK、云账号、在线 TTS。
- 无自定义后台服务。
- Room 依赖会默认合并 `MultiInstanceInvalidationService`；T02 Manifest 使用 `tools:node="remove"` 明确删除，最终合并 Manifest 中不存在 `<service>`。
- 没有业务数据库、正式内容或学习数据。
