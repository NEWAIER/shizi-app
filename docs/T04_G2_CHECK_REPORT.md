# T04/G2真实资源校验报告

- 日期：2026-07-26
- 结论：**PASS**
- manifest：50个唯一required条目（38 MP3、12 WebP）
- G1：PASS
- G2：PASS
- `childTrialEnabled=true`（仅因最终G2无错误）

## G2检查结果

| 检查 | 结果 |
|---|---|
| required文件存在且非空 | PASS |
| bytes与manifest一致 | PASS |
| SHA-256与manifest一致 | PASS |
| content引用全部被manifest覆盖 | PASS |
| 无required孤儿、无重复path | PASS |
| 12图均为可解码WebP、1024×1024 | PASS |
| 38音频均为MP3、44.1kHz、单声道、96kbps | PASS |
| 分类时长限制 | PASS |
| 图片/音频类型未互换 | PASS |
| 五字开发者与家长审核状态完整 | PASS |

## 反向测试

缺失、空文件、字节/哈希伪造、引用缺失、孤儿、重复path、错误图片格式/尺寸、错误音频格式/时长、开发者或家长审核false均返回对应稳定错误码，且`childTrialEnabled=false`。资源缺失与篡改场景中G1仍PASS，G1/G2无循环。

AT-22/P0与AT-34/P0均PASS，详见原始结果文件。
