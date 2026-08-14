# PR-03A 候选资源生产准备

本目录记录 30 字候选包的人工审核入口。资源生产源位于 `content-source/starter-thirty-v1/`。

1. 运行 `python tools/content-builder/build_starter_thirty.py` 生成 TTS、图片和媒体需求清单。
2. 以 `generated/image_prompts.csv` 生成原创 WebP 插画，并导入候选包路径。
3. 使用项目根目录的 `.venv-tts` 与 `tools/audio-generator/generate_edge_tts.py` 生成候选 Edge TTS 音频。该开发工具只写本地 MP3，Android APP 不会在运行时调用在线 TTS；生成与校验均需要本机 `ffmpeg`/`ffprobe`。试听与真实家长审核完成前，不得复制音频进候选包。
4. 运行 `validate_text.py` 和 `validate_media.py`。后者缺媒体时必须失败。
5. 由真实家长完成 `PARENT_REVIEW_CHECKLIST.md`。

候选包在以上条件满足及审核真实完成前不得注册为活动包。
