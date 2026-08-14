# PR-CS-02 批量内容编译

## 目录格式

批量源目录包含三个 JSON 文件，每个文件都是数组，也可以使用 `{ "records": [] }` 包装：

```text
batch/
  character_base.json
  child_content.json
  experience.json
```

三个文件通过稳定 `characterId` 关联。每个基础字都必须同时拥有儿童加工层和体验层，且 `learningOrder` 从 1 连续编号。

## 用法

```powershell
python tools/content-builder/compile_layered_batch.py `
  --source content-source/content-strategy-v1/batch `
  --allow-draft `
  --output tmp/layered-content-v1/batch-content.json
```

默认不允许 `DRAFT`，只有审核完成的 `ACTIVE` 记录才能生成可发布候选。`--allow-draft` 只用于本地结构联调，输出不得注册到 `content/catalog.json`。

## 当前边界

批量示例只有 1 个记录，用于验证目录协议，不代表引入新的正式学习内容。接入真实开源快照时，先把来源、许可证、快照指纹补齐，再进入儿童文案与媒体审核。
