# T04合并Manifest与权限检查

结果：PASS。

| 检查项 | 实际值 |
|---|---|
| package | `com.family.shizi` |
| minSdk | 23 |
| targetSdk | 35 |
| allowBackup | false |
| fullBackupContent | false |
| usesCleartextTraffic | false |

仅声明`com.family.shizi.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`。未声明INTERNET、RECORD_AUDIO、CAMERA、位置、通讯录或广泛存储权限。原始权限输出与合并Manifest XML随包提交。
