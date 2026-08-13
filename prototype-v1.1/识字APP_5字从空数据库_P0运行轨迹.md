# 《识字APP——5字从空数据库 P0 运行轨迹》

- 运行日期：2026-07-25
- 运行批次：90分复审版本链纠错后，针对最终规格重新运行
- 规格对象：《识字APP_5字可运行原型_Codex开发规格包_V1.1_执行线程提交版.md》
- 规格对象 SHA-256：`cafaa84100daf82cc4b9237e405dd0d339dcfae664c093793f60fb39dacec94a`
- 检查用例：AT-36/P0
- 初始条件：单一全空数据库；学习记录表0行；人、口、大、小、山全部为 `UNLEARNED`
- 数据来源：规格正文5.5节真实JSON
- 任务入口：规格定义的 `DailyTaskGenerator.getOrCreate(date)` 可执行参考模型
- 固定规则：到期复习→未完成新字接续→全新字；默认每日1个新字；到期复习最多3字
- 禁止项：不直接写状态、不预置其他字、不伪造证据、不手工插入任务

## 失败即停止规则

每一步在继续前检查种子资格、必需题型数量、状态和双验证结果。任何断言失败时立即输出：

```text
FAIL_STOP	<首个失败步骤>
OVERALL=FAIL	first_failure=<首个失败步骤>
```

失败后不再运行后续日期或后续汉字。本次未触发失败分支。

## 原始机器轨迹

```text
INIT	rows=0	人/口/大/小/山=UNLEARNED
DAY=00	人	NEW	learnedCount=0	seeds=q_ren_char_image,q_ren_listen_char,q_ren_char_audio	types=3	dates=1	APP=NOT_DUE	ORAL=NOT_TESTED	STATE=REVIEWING
DAY=01	人	REVIEW/D1	learnedCount=1	seeds=q_ren_char_audio,q_ren_char_image	types=3	dates=2	APP=NOT_DUE	ORAL=NOT_TESTED	STATE=TEMP_MASTERED
DAY=01	口	NEW	learnedCount=1	seeds=q_kou_char_image,q_kou_listen_char,q_kou_char_audio	types=3	dates=1	APP=NOT_DUE	ORAL=NOT_TESTED	STATE=REVIEWING
DAY=02	口	REVIEW/D1	learnedCount=2	seeds=q_kou_shape,q_kou_char_audio	types=4	dates=2	APP=NOT_DUE	ORAL=NOT_TESTED	STATE=TEMP_MASTERED
DAY=02	大	NEW	learnedCount=2	seeds=q_da_char_image,q_da_listen_char,q_da_char_audio	types=3	dates=1	APP=NOT_DUE	ORAL=NOT_TESTED	STATE=REVIEWING
DAY=03	人	REVIEW/D3	learnedCount=3	seeds=q_ren_shape,q_ren_listen_char	types=4	dates=3	APP=NOT_DUE	ORAL=NOT_TESTED	STATE=TEMP_MASTERED
DAY=03	大	REVIEW/D1	learnedCount=3	seeds=q_da_shape,q_da_char_audio	types=4	dates=2	APP=NOT_DUE	ORAL=NOT_TESTED	STATE=TEMP_MASTERED
DAY=03	小	NEW	learnedCount=3	seeds=q_xiao_char_image,q_xiao_listen_char,q_xiao_char_audio	types=3	dates=1	APP=NOT_DUE	ORAL=NOT_TESTED	STATE=REVIEWING
DAY=04	口	REVIEW/D3	learnedCount=4	seeds=q_kou_char_image,q_kou_listen_char	types=4	dates=3	APP=NOT_DUE	ORAL=NOT_TESTED	STATE=TEMP_MASTERED
DAY=04	小	REVIEW/D1	learnedCount=4	seeds=q_xiao_context,q_xiao_shape	types=5	dates=2	APP=NOT_DUE	ORAL=NOT_TESTED	STATE=TEMP_MASTERED
DAY=04	山	NEW	learnedCount=4	seeds=q_shan_char_image,q_shan_listen_char,q_shan_char_audio	types=3	dates=1	APP=NOT_DUE	ORAL=NOT_TESTED	STATE=REVIEWING
DAY=05	大	REVIEW/D3	learnedCount=5	seeds=q_da_context,q_da_char_image	types=5	dates=3	APP=NOT_DUE	ORAL=NOT_TESTED	STATE=TEMP_MASTERED
DAY=05	山	REVIEW/D1	learnedCount=5	seeds=q_shan_shape,q_shan_char_audio	types=4	dates=2	APP=NOT_DUE	ORAL=NOT_TESTED	STATE=TEMP_MASTERED
DAY=06	小	REVIEW/D3	learnedCount=5	seeds=q_xiao_char_audio,q_xiao_char_image	types=5	dates=3	APP=NOT_DUE	ORAL=NOT_TESTED	STATE=TEMP_MASTERED
DAY=07	人	REVIEW/D7	learnedCount=5	seeds=q_ren_shape,q_ren_char_audio	types=4	dates=4	APP=NOT_DUE	ORAL=NOT_TESTED	STATE=TEMP_MASTERED
DAY=07	山	REVIEW/D3	learnedCount=5	seeds=q_shan_char_image,q_shan_listen_char	types=4	dates=3	APP=NOT_DUE	ORAL=NOT_TESTED	STATE=TEMP_MASTERED
DAY=08	口	REVIEW/D7	learnedCount=5	seeds=q_kou_shape,q_kou_char_audio	types=4	dates=4	APP=NOT_DUE	ORAL=NOT_TESTED	STATE=TEMP_MASTERED
DAY=09	大	REVIEW/D7	learnedCount=5	seeds=q_da_context,q_da_listen_char	types=5	dates=4	APP=NOT_DUE	ORAL=NOT_TESTED	STATE=TEMP_MASTERED
DAY=10	小	REVIEW/D7	learnedCount=5	seeds=q_xiao_context,q_xiao_listen_char	types=5	dates=4	APP=NOT_DUE	ORAL=NOT_TESTED	STATE=TEMP_MASTERED
DAY=11	山	REVIEW/D7	learnedCount=5	seeds=q_shan_shape,q_shan_char_audio	types=4	dates=4	APP=NOT_DUE	ORAL=NOT_TESTED	STATE=TEMP_MASTERED
DAY=14	人	REVIEW/D14	learnedCount=5	seeds=q_ren_listen_char,q_ren_char_audio	types=4	dates=5	APP=PASS	ORAL=NOT_TESTED	STATE=TEMP_MASTERED
DAY=14	人	ORAL	APP=PASS	ORAL=INDEPENDENT_PASS	STATE=STABLE_MASTERED
DAY=15	口	REVIEW/D14	learnedCount=5	seeds=q_kou_listen_char,q_kou_char_audio	types=4	dates=5	APP=PASS	ORAL=NOT_TESTED	STATE=TEMP_MASTERED
DAY=15	口	ORAL	APP=PASS	ORAL=INDEPENDENT_PASS	STATE=STABLE_MASTERED
DAY=16	大	REVIEW/D14	learnedCount=5	seeds=q_da_listen_char,q_da_char_audio	types=5	dates=5	APP=PASS	ORAL=NOT_TESTED	STATE=TEMP_MASTERED
DAY=16	大	ORAL	APP=PASS	ORAL=INDEPENDENT_PASS	STATE=STABLE_MASTERED
DAY=17	小	REVIEW/D14	learnedCount=5	seeds=q_xiao_listen_char,q_xiao_char_audio	types=5	dates=5	APP=PASS	ORAL=NOT_TESTED	STATE=TEMP_MASTERED
DAY=17	小	ORAL	APP=PASS	ORAL=INDEPENDENT_PASS	STATE=STABLE_MASTERED
DAY=18	山	REVIEW/D14	learnedCount=5	seeds=q_shan_listen_char,q_shan_char_audio	types=4	dates=5	APP=PASS	ORAL=NOT_TESTED	STATE=TEMP_MASTERED
DAY=18	山	ORAL	APP=PASS	ORAL=INDEPENDENT_PASS	STATE=STABLE_MASTERED
OVERALL=PASS	五字均从全空库真实到达 STABLE_MASTERED
```

## 逐字最终轨迹

| 汉字 | 首学时数据库已完成首学字数 | 第4种题型 | D14两题 | APP延迟识别 | 家长独立认读 | 完整状态轨迹 |
|---|---:|---|---|---|---|---|
| 人 | 0 | D3 字形辨认 | 听音选字＋看字选音 | PASS | PASS | UNLEARNED→FIRST_LEARNING→REVIEWING→TEMP_MASTERED→STABLE_MASTERED |
| 口 | 1 | D1 字形辨认 | 听音选字＋看字选音 | PASS | PASS | 同上 |
| 大 | 2 | D1 字形辨认 | 听音选字＋看字选音 | PASS | PASS | 同上 |
| 小 | 3 | D1 生活情境、字形辨认 | 听音选字＋看字选音 | PASS | PASS | 同上 |
| 山 | 4 | D1 字形辨认 | 听音选字＋看字选音 | PASS | PASS | 同上 |

## 结论边界

本轨迹证明正式规格的真实JSON、资格过滤、任务优先级和状态规则在可执行参考模型中可达。它不等同于尚未开发的 Kotlin/Room 业务实现已经通过AT-36；总监放行并完成T01—T06后，必须对正式实现复跑同一轨迹。
