# T03 文件变更清单

## 新增

- `app/src/main/assets/content/v1/content.json`
- `app/src/main/assets/content/v1/content.schema.json`
- `data/content/ContentModels.kt`
- `data/content/ContentLoader.kt`
- `data/content/ContentValidator.kt`
- `data/content/ContentValidationError.kt`
- `data/content/ContentValidationResult.kt`
- 五类内容测试及共用测试加载器
- T03 G1、测试、变异、矩阵和本清单文档

## 修改

- 版本目录加入 Kotlin Serialization 1.7.3、NetworkNT JSON Schema Validator 1.5.9。
- Kotlin Serialization 插件锁定为 2.0.21。
- app 版本改为 `1.1-t03` / versionCode 4。
- 增加 `verifyContentG1` 并接入 `check`。
- 应用标签更新为 T03。

## 明确未做

未创建正式图片、音频、资源 manifest、业务 Room 表、DataStore 设置、状态机、复习调度、正式页面或学习流程；未开始 T04—T16。
