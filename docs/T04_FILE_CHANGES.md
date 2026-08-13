# T04文件变更清单

- 新增38个`assets/content/v1/audio/**/*.mp3`正式音频。
- 新增12个`assets/content/v1/images/**/*.webp`正式图片。
- 新增`assets/content/v1/manifest.json`。
- `content.json`仅更新五字contentReview三项。
- `content.schema.json`允许完成审核时blockedReason为null。
- 新增AssetManifest模型/加载器、G2校验器、结果和稳定错误码。
- 新增G2正向及反向单元测试，更新G1资源独立性测试。
- 新增`verifyContentG2`并接入`check`。
- versionCode/versionName更新为5/1.1-t04，应用标签更新为T04。
- 新增T04审核、技术检测、引用矩阵、构建、APK、权限及哈希材料。
- 未增加第三方运行时依赖；工具链和既有固定依赖版本不变。
