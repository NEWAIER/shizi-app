# PR-03A 候选资源生产准备

本目录记录 30 字候选包的人工审核入口。资源生产源位于 `content-source/starter-thirty-v1/`。

1. 运行 `python tools/content-builder/build_starter_thirty.py` 生成 TTS、图片和媒体需求清单。
2. 以 `generated/image_prompts.csv` 生成原创 WebP 插画，并导入候选包路径。
3. 审核一段 TTS 样音后，设置 Azure 环境变量并运行 `generate_tts.py`；不提供凭据不会产生任何假文件。
4. 运行 `validate_text.py` 和 `validate_media.py`。后者缺媒体时必须失败。
5. 由真实家长完成 `PARENT_REVIEW_CHECKLIST.md`。

候选包在以上条件满足及审核真实完成前不得注册为活动包。
