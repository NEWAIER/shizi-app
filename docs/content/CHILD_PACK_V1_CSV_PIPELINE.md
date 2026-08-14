# child-pack-v1 50 字 CSV 流水线

输入位于 `content-source/v1/`：

- `characters.csv`：50 字儿童文案和媒体需求。
- `questions.csv`：每字第一版听音找字题。
- `media.csv`：统一音频、图片生产工作单。
- `reviews.csv`：文本、媒体、题目和包级审核记录。

生成候选包：

```powershell
python tools/content-builder/build_child_pack_from_csv.py `
  --source content-source/v1 `
  --output tmp/child-pack-v1 `
  --allow-draft
```

输出包含 `pack.json`、`content.json` 和 `manifest.json`，状态为 `CANDIDATE`，不会自动复制到 Android assets，也不会修改 catalog。50 字内容全部为 `DRAFT`，完成真实图片、音频和家长审核后才允许激活。
