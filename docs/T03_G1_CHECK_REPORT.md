# T03/G1结构校验报告

- 执行日期：2026-07-26
- 冻结规格SHA-256：`cafaa84100daf82cc4b9237e405dd0d339dcfae664c093793f60fb39dacec94a`
- 结论：**G1 11/11 PASS**

| # | G1校验项 | 结果 |
|---:|---|---|
| 1 | id、character、order唯一 | PASS |
| 2 | learningOrder与五字符ID及order一致 | PASS |
| 3 | correctOptionId恰好出现一次 | PASS |
| 4 | optionIds全部可解析 | PASS |
| 5 | 资源路径为安全相对路径且媒体类型严格匹配 | PASS |
| 6 | 每字首次固定三题齐全 | PASS |
| 7 | 每字至少四题型、四证据类别 | PASS |
| 8 | 每字D14两题齐全 | PASS |
| 9 | 首次三题minLearnedCount为0 | PASS |
| 10 | 儿童选项不含禁用形近字 | PASS |
| 11 | 文本审核、日期递增、严格解析、必填非空 | PASS |

## 资源路径类型规则

- `IMAGE`与`CONTEXT`的asset：仅允许`images/*.webp`。
- `AUDIO`的asset：仅允许`audio/*.mp3`。
- promptAudio、字符音频、字义音频、词语音频和短句音频：仅允许`audio/*.mp3`。
- imageAsset：仅允许`images/*.webp`。
- Kotlin校验器错配统一返回稳定错误码`RESOURCE_PATH_INVALID`。
- JSON Schema使用OptionKind分支严格绑定asset类型。

## 内容计数

`schemaVersion=1`；字符5个；选项17个；题目22个；复习日偏移为`1,3,7,14,30,60`。

G1不要求资源实体存在。本轮没有加入图片、音频、资源manifest或资源哈希，T04未开始。
