# 识字 APP 内容战略重构 V1.0

## 目标

把内容生产从“每个汉字都人工重做一份完整资源”改成“基础数据复用、儿童内容加工、自有体验编排”。目标是支持 1000 字以上扩展，同时保持 4 岁儿童的听、看、理解、游戏、成长体验。

本版本只建立架构和生产契约，不扩充 30 字内容，也不把未经审核的开源数据直接展示给儿童。

## 三层内容模型

| 层 | 负责什么 | 不负责什么 | 典型来源 |
|---|---|---|---|
| Foundation 基础汉字库 | 字符、Unicode、拼音、声调、笔画数、部件、频率、笔顺引用 | 儿童释义、插画、游戏反馈 | Make Me a Hanzi、公开频率/笔顺数据 |
| Child Content 儿童加工层 | 4 岁儿童释义、生活词、例句、图片主题、音频文案、易混淆提醒 | 决定学习顺序和奖励 | 产品编辑、儿童教育审核、原创媒体 |
| Experience 自有体验层 | 学习顺序、每日节奏、题型、解锁、奖励、复习策略、角色反馈 | 重复维护基础字典字段 | APP 产品规则和体验设计 |

## 核心原则

1. 开源数据是机器输入，不是儿童成品。
2. 每个儿童可见文本都必须经过儿童语言审核；4 岁儿童优先听懂和看懂，不追求字典完整性。
3. 基础数据更新不得隐式改变学习体验；体验层通过稳定的 `characterId` 和版本锁定。
4. 资源可以缺失，但不能静默降级为错误内容。未完成条目状态必须是 `DRAFT` 或 `BLOCKED`，不得进入 active pack。
5. 新增汉字的边际成本应主要落在儿童内容和媒体审核，而不是重新录入基础字段。

## 稳定标识与版本

- 字符 ID：`char_u` + 四位或更多小写十六进制 Unicode，例如 `char_u6c34`。
- Foundation 版本：来源快照版本，例如 `mmh-v1-2025-01`。
- Child Content 版本：独立语义版本，例如 `child-v1.0`。
- Experience 版本：产品编排版本，例如 `exp-v1.0`。
- Runtime `contentVersion`：三层编译后的不可变包版本，例如 `2.0.0`。
- 已完成学习记录只绑定 runtime `contentVersion`；切换包时仍可通过稳定 `characterId` 识别同一个字。

## 编译边界

```text
开源快照 + 频率/笔顺适配
          ↓
character_base.json
          + 人工儿童加工
          ↓
child_content.json
          + 产品体验编排
          ↓
experience.json
          ↓  compile + validate + manifest
Android content pack / content.json
```

运行时仍读取当前 `ContentPackage`，因此现有 1.0.0 包无需迁移即可运行；后续包通过 `layers` 元数据声明三层来源和版本。

## 生产状态

`DRAFT` → `TEXT_REVIEWED` → `MEDIA_REVIEWED` → `PARENT_REVIEWED` → `ACTIVE`。

任一层被修改，都生成新的版本和 manifest，不覆盖已发布包。基础数据许可证、来源和原始快照指纹必须进入发布物料。

## 本轮交付

- 三层字段契约和 JSON Schema。
- 以“水”为例的最小样例，不接入 active pack。
- Android `ContentPackage.layers` 向后兼容字段。
- 30 字现有包保持原样，后续通过编译器接入三层源数据。
