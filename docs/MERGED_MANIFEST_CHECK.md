# T03合并Manifest检查

- 检查文件：`app/build/intermediates/merged_manifest/debug/processDebugMainManifest/AndroidManifest.xml`
- 归档副本：`T03_MERGED_AndroidManifest.xml`
- 结果：PASS

| 检查项 | 实际值 | 结果 |
|---|---|---|
| package | `com.family.shizi` | PASS |
| minSdkVersion | `23` | PASS |
| targetSdkVersion | `35` | PASS |
| allowBackup | `false` | PASS |
| fullBackupContent | `false` | PASS |
| usesCleartextTraffic | `false` | PASS |

合并Manifest只声明应用自身`com.family.shizi.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`，未声明网络、录音、相机、位置、通讯录或广泛存储权限。权限原始输出见`AAPT_PERMISSIONS.txt`。
