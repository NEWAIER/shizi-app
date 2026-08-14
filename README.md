# 识字 APP

面向儿童的本地离线识字 Android 原型，当前范围固定为 5 个字：人、口、大、小、山。

## 当前功能

- 底部四菜单：学习、测试、已学习、我的。
- 本地 WebP 图片与 MP3 音频；学习、听音、看字选图/选音等练习。
- Room 本地学习记录、阶段测试记录和间隔复习基础。
- 认识满 3 个字后开放阶段测试；成绩回顾区分首次答对与需巩固的字。
- 已学习页展示拼音、释义、词语和下次复习提示。
- 学习页内的家长入口使用长按与算式验证。

## 主要目录

- `app/`：Android 应用源码、测试和本地资源。
- `prototype-v1.1/`：冻结规格及历史审核材料。
- `docs/`：开发、测试和设备检查记录。

## 本地构建

要求：JDK 17、Android SDK（compileSdk 35）。配置 `JAVA_HOME`、`ANDROID_HOME` 和 `ANDROID_SDK_ROOT` 后执行：

```powershell
.\gradlew.bat assembleDebug --console=plain --no-daemon
```

APK 输出：`app/build/outputs/apk/debug/app-debug.apk`。

## 开发期音频试听

Edge TTS 仅在开发机生成本地候选 MP3，绝不进入 Android 运行时网络路径。依赖固定在 `tools/audio-generator/requirements.txt`；生成器会校验 CSV 的重复项和路径逃逸、保存输入指纹及 SHA-256，并通过 `ffmpeg` 解码检查音频。试听报告使用跨平台的 POSIX 相对路径。

```powershell
.venv-tts\Scripts\python.exe tools\audio-generator\generate_edge_tts.py --input tools\audio-generator\audition.csv --output-dir artifacts\tts-audition\zh-CN-XiaoyiNeural --voice zh-CN-XiaoyiNeural
```

已有文件只有在文本、声音、速率、音量、音高、Edge TTS 版本和 SHA-256 均与清单一致时才会复用；需要覆盖不匹配文件时必须显式传入 `--force`。

## 说明

本仓库不提交本机 SDK、Gradle 缓存、构建产物和 APK；这些均由 `.gitignore` 排除。应用仍处于调试原型阶段，不用于正式发布或儿童正式试用。
