# PR-CONTENT-01 基础汉字库接口

`BaseCharacterRepository` 只负责机器基础字段：汉字、Unicode、拼音、声调、笔画、部首、字频和来源快照。它不提供儿童释义、词语、图片、音频或题目。

## 使用边界

- 基础库可以扩展到 1000+ 条记录。
- 通过稳定 `char_u` + Unicode ID 关联儿童内容层。
- 加载时拒绝重复 ID、Unicode 不匹配、声调/笔画非法和空来源快照。
- 基础库源数据位于 `content-source/`，未注册为 Android active pack。
- 只有编译器生成并通过儿童审核的运行包才可进入 `content/catalog.json`。
