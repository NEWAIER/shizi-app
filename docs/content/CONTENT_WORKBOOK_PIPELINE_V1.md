# PR-CONTENT-03 Excel 内容生产流水线

## 工作表契约

工作簿必须包含三个工作表：`character_base`、`child_content`、`experience`。列名与三层 JSON 字段一致；列表字段使用 `|` 分隔。

## 构建

```powershell
python tools/content-builder/build_content_package.py `
  --workbook review/content.xlsx `
  --output tmp/content-package-candidate `
  --allow-draft
```

输出：`content.json` 和 `manifest.json`。输出状态固定为 `CANDIDATE`，不会写入 Android assets 或 catalog。

## 门禁

- 缺工作表、缺列、空表、重复 `characterId` 直接失败。
- 三层关联、Unicode、声调、笔画、学习顺序和审核状态继续由既有校验器负责。
- `--allow-draft` 只用于编辑联调；发布构建必须去掉该参数并要求 `ACTIVE`。
- 图片、音频和许可证仍需各自质量门，不能因为 Excel 构建成功而视为可发布。
