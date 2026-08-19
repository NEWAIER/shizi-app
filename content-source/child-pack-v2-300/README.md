# child-pack-v2-300 内容源

这是面向 300 字扩充的独立内容源目录，当前只用于生产和审核，不会自动替换正在使用的 `child-pack-v1`。

## 生产规则

- `characters.csv`：一字一行，必须包含儿童解释、两个词语、例句、图片提示和审核状态。
- `questions.csv`：一字一题，四个选项必须互不重复；题型由构建器生成四种体验变体。
- `media.csv`：记录图片、单字音、释义音、词语音和例句音的生产要求。
- `reviews.csv`：记录文字、图片、音频和家长审核状态。
- 未完成媒体或审核前，统一使用 `DRAFT`，禁止写入 active catalog。

## 目标分批

300 字分为 6 批，每批 50 字。每批完成后独立通过文字、选项、媒体清单和审核门禁，再合并到候选包。

## 构建示例

```powershell
python tools/content-builder/build_child_pack_from_csv.py `
  --source content-source/child-pack-v2-300 `
  --output tmp/child-pack-v2-300 `
  --pack-id child-pack-v2-300 `
  --expected-count 300 `
  --allow-draft
```

候选包通过全部媒体和家长审核后，才允许进入 Android 资源目录；本目录不修改现有 50 字包。
