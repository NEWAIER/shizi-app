# T03 正向与反向测试结果

| 场景 | 期望错误码/结果 | 实际结果 |
|---|---|---|
| 正式 content.json | PASS | PASS |
| 删除正确选项 | `CORRECT_OPTION_INVALID` | PASS |
| 重复正确选项 | `CORRECT_OPTION_INVALID` | PASS |
| 重复 CharacterContent.id | `CONTENT_ID_DUPLICATE` | PASS |
| 重复 character 或 order | `CONTENT_ID_DUPLICATE` | PASS |
| 引用不存在 option ID | `OPTION_REFERENCE_MISSING` | PASS |
| 删除 CHARACTER_CHOOSE_AUDIO | `CONTENT_REACHABILITY_FAILED` | PASS |
| 缩减为三种题型 | `CONTENT_REACHABILITY_FAILED` | PASS |
| 首次题 minLearnedCount=1 | `CONTENT_REACHABILITY_FAILED` | PASS |
| `../x.mp3` | `RESOURCE_PATH_INVALID` | PASS |
| `/x.mp3` | `RESOURCE_PATH_INVALID` | PASS |
| `https://...` | `RESOURCE_PATH_INVALID` | PASS |
| 注入禁用形近字 | `CONFUSABLE_OPTION_FORBIDDEN` | PASS |
| textReviewed=false | `CONTENT_REVIEW_INVALID` | PASS |
| 复习日期重复或倒序 | `REVIEW_OFFSETS_INVALID` | PASS |
| 题型映射错误证据 | `EVIDENCE_MAPPING_INVALID` | PASS |
| 缺必填字段 | `CONTENT_STRUCTURE_INVALID` | PASS |
| 类型错误 | `CONTENT_STRUCTURE_INVALID` | PASS |
| 未知枚举 | `CONTENT_STRUCTURE_INVALID` | PASS |
| 未知字段 | Loader/Schema 失败 | PASS |
| 空必填字符串 | Schema 失败 | PASS |
| 删除全部真实资源文件 | G1 仍通过 | PASS |
