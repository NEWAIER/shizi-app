# PR-CS-01 三层内容编译器 V1

## 用法

候选源数据默认位于 `content-source/content-strategy-v1/`：

```powershell
python tools/content-builder/validate_layered_content.py --allow-draft
python tools/content-builder/compile_layered_content.py --allow-draft --output tmp/layered-content-v1/content.json
```

输出是运行时形状的候选 JSON，不会自动写入 `app/src/main/assets/content/`，也不会修改 `content/catalog.json`。

## 安全边界

- 未达到 `ACTIVE` 时，默认编译失败；示例只有在显式 `--allow-draft` 下可生成。
- 三层必须引用同一个稳定 `characterId`。
- Unicode、声调、笔画数、儿童文案和体验题型会在编译前检查。
- 资源路径只是确定性工作单，真实音频和图片仍需媒体质量门。
- 当前题型生成的是最小候选骨架；没有经过干扰项和家长审核的候选包不能激活。

## 下一步

将 `sample` 文件改为批量目录输入，接入实际 Foundation 快照，并为每个题型生成经过人工审核的 option catalog、媒体需求和 manifest。
