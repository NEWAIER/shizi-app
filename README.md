# 识字 APP

面向儿童的本地离线识字 Android 原型，当前内容包范围固定为 50 个字（child-pack-v1）。

## 当前功能

- 成长森林地图（V2.1）：按 10 字一章分为 5 个森林（第一~第四森林 + 第五星光森林），卡通曲线路径连接节点，每章有独立横幅与静态装饰（石头/云/叶子/星星/星光）；节点状态区分已完成（金色星星果）/当前/待成熟/未解锁。
- 毛毛虫主角：Canvas 矢量绘制，停在下一个待学果子旁，吃一个果子长一截；只有当前节点、毛毛虫与已解锁树洞使用无限动画，其余节点静态（性能约束）。
- 树洞测试关卡：每学满 10 个字解锁一个树洞，树洞测试最近一批（第 1-10 字、第 11-20 字……）。
- 徽章按类型判定（字数/学习天数/复习/挑战/收藏），只有真实计数源的字数与天数徽章会解锁，其余显示"等待点亮"，不再统一按已学字数错误解锁。
- 等级进度按成长星星计算（0/50/120/250/450/700），显示当前等级起点、下一级阈值与进度。
- 星星在 UI 统一称为"成长星星"，不暗示可消费货币。
- 练习反馈音频：答对播放"找到了！"+星星动画约 700ms 自动下一题；第一次错误"再试试看"；第二次错误"不着急，再看一眼"+正确卡片停留约 2 秒。已移除系统 TextToSpeech，全部使用打包的 audio/ui 音频（Edge TTS 生成，进入 G2 资源校验）。
- 字卡单击播放字音+第 1 个词语，长按播放字音+全部词语+例句。
- 本地 WebP 图片与 MP3 音频；学习、听音、看字选图/选音等练习。
- Room 本地学习记录、阶段测试记录和间隔复习基础。
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
