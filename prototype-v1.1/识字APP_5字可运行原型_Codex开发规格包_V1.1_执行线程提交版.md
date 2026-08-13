# 《识字APP——5字可运行原型 Codex 开发规格包 V1.1（执行线程提交版）》

- 文档版本：V1.1（执行线程提交版）
- 日期：2026-07-25
- 文档用途：直接交给 Codex 开发，并作为产品审核和测试验收依据
- 唯一上位文档：《识字APP_阶段一_家庭自用简化版_V1.1.1审定版》
- 本阶段范围：5 个样板字的 Android 离线可运行原型
- 修订基线：《识字APP_5字可运行原型_Codex开发规格包_V1.md》
- 参考材料：《识字APP_5字可运行原型_Codex开发规格包_V1.1.md》（仅作修订参考，已逐项独立核验）
- 本阶段状态：执行线程已按总监 93/100 返工指令完成第二次定向修订，等待总监独立复审；**尚未开始编码**

---

## 0. 执行摘要

### 0.1 本规格包最终定义的成果

Codex 应按本文件交付一个可安装在华为 Mate 30、鸿蒙 4.2 上的 Android APK。断网后仍能完成：

```text
首次家长设置
→ 儿童首页
→ 学习“人、口、大、小、山”
→ 完成最小五类练习
→ 保存每题记录
→ 生成到期复习
→ 中断后继续
→ 查看结果
→ 家长查看报告
→ 家长完成或修改口头抽检
→ APP 延迟识别与家长独立认读共同决定稳定掌握
```

本原型不是静态页面，也不是无数据前端壳。学习状态、题目实例、作答、提示、复习日期、延迟验证、口头抽检和设置必须真实持久化。

### 0.2 已确认输入

| 项目 | 已确认值 | 开发含义 |
|---|---|---|
| 设备 | 华为 Mate 30 | 只对该机做首要适配；安装前核对“关于手机”确为标准版而非 Pro |
| 系统 | 鸿蒙 4.2 | 先做 APK 安装、启动、音频、数据库冒烟测试 |
| 屏幕 | 6.62 英寸、2340×1080 | 竖屏；设计基准约 360×780 dp；不得写死像素 |
| 使用方向 | 竖屏 | `screenOrientation="portrait"` |
| 当前识字量 | 0 | 所有指令必须可听，不依赖儿童读说明或拼音 |
| 已认识样板字 | 0 | 不把任一选项默认为已知 |
| 5 个样板字 | 人、口、大、小、山 | 固定顺序；内容配置化，页面代码不得写死 |
| 昵称 | 允许首次由家长设置 | 可选；留空时问候语不插入称呼，不猜测真实昵称 |
| 音频 | 合成女声 | 开发时预生成并随 APK 打包；运行时不联网合成 |
| 交付 | Android 安装包 | 提供 APK、源码、测试报告和 SHA-256 |
| 离线要求 | 完整离线 | 不申请网络权限；飞行模式完成全部验收 |

Mate 30 标准版屏幕规格依据公开硬件资料为 6.62 英寸、2340×1080；若真机“关于手机”显示为 Mate 30 Pro，则必须停止 UI 真机验收并重新记录 2400×1176 的目标设备，不得悄悄按标准版结项。

### 0.3 不得增加的功能

注册登录、多儿童、云同步、服务器、运营后台、购买订阅、排行榜、社交、开放式 AI、复杂积分、广告、推送、第三方分析 SDK、应用商店运营、儿童录音、人脸、姓名、位置采集均不在范围内。

### 0.4 强制停止门

| 停止门 | 通过条件 | 未通过处理 |
|---|---|---|
| G0 APK 兼容 | 空壳 APK 可在目标机安装、冷启动、播放本地 MP3、写读一条 Room 记录 | 停止完整开发，先解决目标机兼容 |
| G1 内容结构 | 5 字 JSON 通过 schema、ID/答案/题型/资源路径格式校验；每字首次三题和 D14 两题齐全，且至少 4 种独立证据题型 | 不得进入资源制作和 UI 联调 |
| G2 内容资源 | G1 通过后，所有真实文件存在、非空、大小与 SHA-256 匹配；每字的字形、发音、字义、图片、词语、短句、答案、干扰项经开发者和家长逐项核对 | 任一项失败，5 字均不得交给儿童 |
| G3 核心流程 | 自动测试及人工验收中的 P0 用例全部通过 | 不得儿童试用 |
| G4 真机离线 | 飞行模式下全流程运行，退出恢复和本地记录正确 | 不得儿童试用 |
| G5 儿童试用 | 3—7 天只验证会不会用、愿不愿用、反馈是否合适 | 不得宣称 14 天保持或稳定掌握 |

---

# 一、5 字样板内容

## 1.1 共用内容模板

每个字必须使用相同字段：`id`、`character`、`pinyin`、`toneNumber`、`meaningForChild`、`imageSpec`、`words`、`sentence`、`audio`、`teachingPrompt`、`questionSeeds`、`confusableRestrictions`、`misconceptions`、`reviewStatus`。内容只放在 JSON/资源目录，不写入 Composable。

统一教学顺序：**人 → 口 → 大 → 小 → 山**。顺序理由：先从具体的人和身体部位建立操作习惯，再引入成对概念“大/小”，最后用“大山、小山”组合已学字。顺序是本次个体原型的设计判断，不是普遍最佳顺序。

## 1.2 “人”

| 字段 | 开发定稿 |
|---|---|
| 汉字 | 人 |
| 标准读音 | rén，第二声 |
| 儿童可理解的字义 | “像你、爸爸妈妈、老师这样，都是人。” |
| 配图含义 | 一名完整站立、自然微笑的儿童；背景无文字、无人群、无动物；画面唯一主体是“一个人” |
| 生活化词语 | 大人、家人、好人 |
| 简短口语句 | “这里有一个人。” |
| 单字发音音频 | `audio/characters/char_ren_v1.mp3`，文本“人”，女声普通话 |
| 字义音频 | `audio/meanings/meaning_ren_v1.mp3`，文本同儿童字义 |
| 词语音频 | `word_daren_v1.mp3`、`word_jiaren_v1.mp3`、`word_haoren_v1.mp3` |
| 句子音频 | `sentence_ren_v1.mp3` |
| 教学提示 | 先展示人物图并问“这是谁呀？”，再显出大字“人”，播放“人”；不讲撇捺术语，不要求书写 |
| 正确答案 | 字形题为“人”；看图题为单人图；选音题为 `char_ren_v1.mp3` |
| 默认干扰项 | 字卡：口、山；图片：嘴、山；声音：口、山 |
| 形近字使用限制 | “入”不得在首次学习及前两次复习中出现；只有“人”已跨日无提示正确后，才能作为后续扩展的形近字；5 字原型儿童试用不启用“入” |
| 可能误解 | 误以为只有小朋友才是“人”；图片不得只画特定职业；教学音频明确“大人和小朋友都是人” |
| 内容审核结果 | 文本、拼音、唯一答案：规格级复核通过；最终图片与合成音频尚未生成，G2 前保持“禁止儿童试用” |

## 1.3 “口”

| 字段 | 开发定稿 |
|---|---|
| 汉字 | 口 |
| 标准读音 | kǒu，第三声 |
| 儿童可理解的字义 | “口就是嘴，用来吃东西，也用来说话。” |
| 配图含义 | 儿童脸部的自然局部，嘴轻轻张开；不要只画红唇、牙齿、口腔医学图，也不要把方框当嘴 |
| 生活化词语 | 开口、门口、一口水 |
| 简短口语句 | “我开口说：你好！” |
| 单字发音音频 | `audio/characters/char_kou_v1.mp3`，文本“口” |
| 字义音频 | `audio/meanings/meaning_kou_v1.mp3` |
| 词语音频 | `word_kaikou_v1.mp3`、`word_menkou_v1.mp3`、`word_yikoushui_v1.mp3` |
| 句子音频 | `sentence_kou_v1.mp3` |
| 教学提示 | 先看嘴部图，播放“口就是嘴”；再展示“口”；“门口”只作为生活词，不把“入口、出口、量词”等多义一次讲完 |
| 正确答案 | 字形题为“口”；看图题为嘴部图；选音题为 `char_kou_v1.mp3` |
| 默认干扰项 | 字卡：人、山；图片：单人图、山；声音：人、山 |
| 形近字使用限制 | “日、回、囗”不进入 5 字原型儿童选项；尤其不得把部件“囗”当成独立教学字 |
| 可能误解 | 误以为“口”只表示画出的红嘴；通过“开口、门口”告诉孩子这个字也会出现在生活词里，但本阶段主要义仍是嘴 |
| 内容审核结果 | 文本、拼音、主义项、唯一答案：规格级复核通过；最终资源待 G2 双人复核 |

## 1.4 “大”

| 字段 | 开发定稿 |
|---|---|
| 汉字 | 大 |
| 标准读音 | dà，第四声 |
| 儿童可理解的字义 | “两个同样的东西比一比，占地方更多的那个是大。” |
| 配图含义 | 两个同类西瓜并排，左侧明显大、右侧明显小；形状、颜色一致，只改变尺寸；目标图另存为大西瓜 |
| 生活化词语 | 大人、大山、大口 |
| 简短口语句 | “这个西瓜真大。” |
| 单字发音音频 | `audio/characters/char_da_v1.mp3` |
| 字义音频 | `audio/meanings/meaning_da_v1.mp3` |
| 词语音频 | `word_daren_v1.mp3`（复用）、`word_dashan_v1.mp3`、`word_dakou_v1.mp3` |
| 句子音频 | `sentence_da_v1.mp3` |
| 教学提示 | 先让孩子看同类大小对比，再显示“大”；不能只给一件物品却要求判断大小 |
| 正确答案 | 概念题为较大的同类物；字形题为“大”；选音题为 `char_da_v1.mp3` |
| 默认干扰项 | 字卡：小、人；图片：小西瓜、同尺寸苹果；声音：小、人 |
| 形近字使用限制 | “太、天、犬”不进入 5 字原型儿童选项；不得用缺一笔或多一笔的伪字做干扰 |
| 可能误解 | 把“大”理解为固定物体名称；每道大小题必须是同类物比较，并随机左右位置 |
| 内容审核结果 | 发音、相对概念、词句和唯一答案：规格级复核通过；最终尺寸差异需在 G2 检查，确保不含糊 |

## 1.5 “小”

| 字段 | 开发定稿 |
|---|---|
| 汉字 | 小 |
| 标准读音 | xiǎo，第三声 |
| 儿童可理解的字义 | “两个同样的东西比一比，占地方更少的那个是小。” |
| 配图含义 | 与“大”共用同一对西瓜，目标是小西瓜；不得更换品类或用远近透视制造假大小 |
| 生活化词语 | 小手、小山、小口 |
| 简短口语句 | “这只小猫很小。” |
| 单字发音音频 | `audio/characters/char_xiao_v1.mp3` |
| 字义音频 | `audio/meanings/meaning_xiao_v1.mp3` |
| 词语音频 | `word_xiaoshou_v1.mp3`、`word_xiaoshan_v1.mp3`、`word_xiaokou_v1.mp3` |
| 句子音频 | `sentence_xiao_v1.mp3` |
| 教学提示 | 与“大”成对呈现，但先各自建立意义，再混合辨认；左右位置每题随机 |
| 正确答案 | 概念题为较小的同类物；字形题为“小”；选音题为 `char_xiao_v1.mp3` |
| 默认干扰项 | 字卡：大、山；图片：大西瓜、同尺寸苹果；声音：大、山 |
| 形近字使用限制 | “少”不进入 5 字原型儿童选项；不得用残缺笔画伪字 |
| 可能误解 | 把近处物体看成大、远处物体看成小；所有大小对比使用同一平面、同一视角 |
| 内容审核结果 | 发音、相对概念、词句和唯一答案：规格级复核通过；最终插画待 G2 |

## 1.6 “山”

| 字段 | 开发定稿 |
|---|---|
| 汉字 | 山 |
| 标准读音 | shān，第一声 |
| 儿童可理解的字义 | “山是地面上高高隆起的地方，常常有石头、树和山坡。” |
| 配图含义 | 一座轮廓清晰的自然山峰，含山坡和少量树；无文字、无寺庙、无火山喷发、无过度卡通成三根竖线 |
| 生活化词语 | 大山、山上、爬山 |
| 简短口语句 | “远处有一座山。” |
| 单字发音音频 | `audio/characters/char_shan_v1.mp3` |
| 字义音频 | `audio/meanings/meaning_shan_v1.mp3` |
| 词语音频 | `word_dashan_v1.mp3`（复用）、`word_shanshang_v1.mp3`、`word_pashan_v1.mp3` |
| 句子音频 | `sentence_shan_v1.mp3` |
| 教学提示 | 先看自然山景，再出现“山”；不讲象形字演变，不把字形画成山的唯一依据 |
| 正确答案 | 字形题为“山”；看图题为山景；选音题为 `char_shan_v1.mp3` |
| 默认干扰项 | 字卡：口、人；图片：嘴、单人；声音：口、人 |
| 形近字使用限制 | “出”不进入 5 字原型儿童选项；后续只有“山”稳定形成表征后再做辨析 |
| 可能误解 | 把小土堆、屋顶或三角形都叫“山”；图片需呈现自然地形，字义音频提到山坡、石头或树 |
| 内容审核结果 | 发音、字义、词句和唯一答案：规格级复核通过；图片和音频待 G2 |

## 1.7 5 字内容停止门检查表

开发者生成资源后，开发者与家长必须逐字填写；任何一格“否”均阻断儿童试用。

| 检查项 | 人 | 口 | 大 | 小 | 山 |
|---|---:|---:|---:|---:|---:|
| 简体字形规范、清晰、字体一致 | □ | □ | □ | □ | □ |
| 女声普通话发音和声调准确 | □ | □ | □ | □ | □ |
| 字义适合 5 岁且无错误简化 | □ | □ | □ | □ | □ |
| 图片主旨唯一、无歧义 | □ | □ | □ | □ | □ |
| 词语真实、常用、适龄 | □ | □ | □ | □ | □ |
| 句子自然、口语化 | □ | □ | □ | □ | □ |
| 每题正确答案唯一 | □ | □ | □ | □ | □ |
| 干扰项不存在第二正确答案 | □ | □ | □ | □ | □ |
| 音频文件可离线播放、无截断 | □ | □ | □ | □ | □ |
| JSON 引用与实际资源文件一致 | □ | □ | □ | □ | □ |
| 开发者签名/日期 |  |  |  |  |  |
| 家长签名/日期 |  |  |  |  |  |

---

# 二、页面开发规格

## 2.0 全局页面规则

1. 只存在五类页面：儿童首页、单字学习、练习、学习结果、家长页。启动检查、弹层、标签页、首次设置均是五类页面的状态，不新增第六类业务页面。
2. 儿童页一屏只有一个主任务；主按钮最小 88dp 高，答题卡最小 112×96dp，任何可点击区域不小于 64×64dp。
3. 所有儿童指令同时有短语音和图形提示；儿童不需要阅读拼音、设置或长句。
4. 儿童页面锁定竖屏；进入沉浸模式但尊重刘海和系统手势安全区。
5. 系统返回键在学习中不直接退出，弹出“继续学习 / 先休息”双选确认；无操作 20 秒只重播一次指令，不自动代答。
6. 每次进入页面先检查内容资源与数据库。P0 资源缺失或数据库不可写时，不进入儿童任务。

### 五类页面状态完整性矩阵

| 页面 | 首次进入状态 | 学习中状态 | 已完成状态 | 无任务状态 | 数据异常状态 |
|---|---|---|---|---|---|
| 儿童首页 | 路由到家长页首次设置 | “继续”，恢复固化 session | “今天完成啦”，禁止加课 | “今天休息” | 阻断主按钮，显示“请叫大人来看看” |
| 单字学习 | A 情境步骤，状态写为初次学习 | 恢复 A/B/C 和音频标记 | 该字教学已完成时由练习或复习调度决定，不重复首教 | 非法路由，返回首页 | 主图/单字音/数据库异常均阻断 |
| 练习 | 打开固化题目实例并播放一次指令 | 保持尝试次数、选项顺序和提示级别 | 完成题直接导航下一题或结果，不允许重复提交 | 非法路由，返回首页 | 保存失败留在原题；资源错误阻断 |
| 学习结果 | 首次进入前先提交 session | 不适用；结果页不再答题 | 正常/提前/只复习三种完成文案 | 无 session 时返回首页 | 提交失败不显示完成，转家长处理 |
| 家长页 | 首次设置标签 | 报告随本地记录实时刷新 | 无“课程完成”概念；保存设置后提示“已保存” | 报告显示“还没有学习记录” | 显示错误码、最近保存时间、重试 |

### 五类页面反馈与退出补充

| 页面 | 正确/错误/连续答错 | 语音与动画 | 返回与保存 |
|---|---|---|---|
| 儿童首页 | 不适用 | 只播一次问候；按钮 240ms | 返回退出 APP；建课事务成功后跳转 |
| 单字学习 | 无答题正误 | 音频可重播；步骤淡入 240ms | 返回走暂停确认；每步 checkpoint |
| 练习 | 按第 2.3 节两级错误和直接教学 | 正确 ≤600ms、错误轻移 ≤220ms | 返回走暂停确认；每次提交事务保存 |
| 学习结果 | 不显示分数或错误 | 完成动画一次 ≤600ms | 返回首页；session 提交失败不得离开 |
| 家长页 | 算式答错关闭入口；业务页无连续答错 | 默认不自动播音、不使用奖励动画 | 返回儿童首页；设置/抽检每次操作即保存 |

## 2.1 儿童首页

### 页面目的

让孩子无需阅读即可开始、继续或结束当天任务；让家长能从隐藏但可发现的入口进入家长页。

### 页面元素与行为

| 元素 | 显示文字/视觉 | 点击或状态行为 | 数据 |
|---|---|---|---|
| 顶部问候 | 有昵称时“你好，{nickname}”；无昵称时“你好呀” | 不可点击；进入时播放一次问候 | 读 `settings.nickname` |
| 今日任务卡 | “今天有 {reviewCount} 个字要再看看”；无复习时“今天认识新朋友” | 不可点击 | 实时读取任务生成器 |
| 主按钮 | 未开始：“开始”；进行中：“继续”；已完成：“今天完成啦”；无任务：“今天休息” | 开始/继续进入当前课程；已完成不再出新任务 | 创建或读取 `learning_session` |
| 进度点 | 最多 5 个圆点；完成一个任务点亮一个 | 不可点击，不显示分数 | 当前 session 进度 |
| 音量按钮 | 扬声器图标 | 播放/暂停当前音频，不进入设置 | 写 `isMuted` |
| 家长入口 | 右上角小锁图标 | 长按 3 秒后显示成人算式门槛 | 不记录儿童行为 |

### 语音

- 首次进入：“你好呀，准备好就点中间的大按钮。”
- 有到期复习：“今天先和老朋友见个面。”
- 已完成：“今天的学习完成啦，我们下次再见。”

### 动画

- 主按钮进入时 240ms 轻微放大到 1.04 再回到 1.0；只播放一次。
- 已完成仅显示 600ms 星光散开，不循环。

### 状态

| 状态 | 显示与行为 |
|---|---|
| 首次进入 | 先进入家长页的首次设置状态；设置保存后回儿童首页 |
| 今日未开始 | 显示“开始”；任务只生成一次并固化 |
| 学习中/已暂停 | `ACTIVE` 或 `PAUSED` 均显示“继续”；恢复保存的题目、选项顺序、当前步骤和累计有效时长 |
| 已完成 | `COMPLETED` 显示“今天完成啦”；`ENDED_EARLY` 显示“今天先到这里”；两者主按钮当天均不可重新开课 |
| 无任务 | 显示“今天休息”；不生成随机练习 |
| 数据异常 | 显示大人图标和“请叫大人来看看”；播放同义语音；不得继续 |

### 返回、退出与保存

- 首页按系统返回：允许退出 APP。
- 创建今日任务时，使用数据库事务保存 `session + session_items + question_instances` 后才显示学习页。
- 创建失败时停留首页异常态，提供“再试一次”和家长入口。

## 2.2 单字学习页

### 页面目的

建立一个字的字形—读音—基础意义—生活词语联系，不把观看页当作掌握证据。

### 固定三步

| 步骤 | 页面内容 | 显示文字 | 语音 | 前进条件 |
|---|---|---|---|---|
| A 情境 | 代表插画，占屏幕上半部 | 无需儿童阅读 | 播放引导问题和字义 | 音频结束后“下一步”可点 |
| B 字音义 | 148sp 大字、扬声器、简短字义 | 只显示目标汉字；字义可小字显示供家长 | 单字发音→0.5 秒→字义 | 至少完整播放一次单字发音 |
| C 词句 | 1—2 个图词卡、口语句 | 显示词语，目标字用主题色突出 | 播放两个词语和句子 | 点“我记住啦”进入练习 |

### 按钮

- `听一听`：重播当前音频；连续点击时先停止上一段再从头播放，300ms 内防抖。
- `下一步`：音频未结束前禁用；结束后启用。
- `我记住啦`：只表示看完教学，不产生正确证据。
- 左上角返回：进入暂停确认，不直接丢弃。

### 反馈与异常

- 单字页没有“答错”。
- 图片缺失：不显示占位猜图，阻断该字并进入资源异常。
- 单字音频缺失：阻断该字；不得临时调用联网 TTS。
- 词语某一音频缺失：跳过该词并记录资源错误，但至少一个词和句子完整才可继续。
- 中途退出：保存 `character_progress.initialTeachingStep`；重进后恢复当前步骤，已听过音频可重播但不重复计事件。

### 状态

| 状态 | 行为 |
|---|---|
| 首次进入 | 把该字从“未学习”事务性改为“初次学习”，记录 `firstStartedAt` |
| 学习中 | 恢复 A/B/C 步骤和已播放标记 |
| 已完成 | 不再进入单字教学，除非错误降级触发“重新认识一下” |
| 无任务 | 不应路由到此页，回首页并记录导航异常 |
| 数据异常 | 阻断前进，不更新学习状态 |

## 2.3 练习页

### 页面目的

每屏只呈现一道题，记录首次选择、是否提示、题型、反馈级别和最终结果。

### 共用元素

| 元素 | 规则 |
|---|---|
| 题目区 | 顶部 25%；目标字、场景图或扬声器三者之一 |
| 语音指令 | 进入 300ms 后自动播放一次；可手动重播 |
| 选项区 | 2—3 个大卡片；位置在实例创建时随机并持久化 |
| 进度 | 只显示圆点，不显示“1/10”压力文字 |
| 确认按钮 | 仅“看字选音”需要；显示对勾图标并配“选好了” |
| 暂停 | 左上角返回触发暂停确认 |

### 正确反馈

1. 选项边框变绿色，播放“找到了！”或“对啦！”；
2. 目标字发音再播放一次；
3. 450ms 轻微放大，不喷射大量奖励；
4. 800ms 后自动进入下一题；
5. 先事务性保存，再显示最终正确反馈；保存失败则不前进。

### 错误与连续答错

| 次数 | UI | 语音 | 记录 |
|---:|---|---|---|
| 第 1 次 | 所选项轻微左右移动 220ms；不出现红叉；其他选项保留 | “再听一听，再找一次。” | 保存 `isCorrect=false, hintLevel=NONE` |
| 第 2 次 | 进入降难：3 选项减为 2；目标与正确选项短暂高亮关联 | “我们一起看看。” | 保存第二次错误；随后 `hintLevel=STRONG` |
| 降难后 | 展示正确答案并播放字音义；不要求继续猜到对 | “这是‘{字}’，我们下次再见。” | 该题 `finalOutcome=TAUGHT_AFTER_ERROR` |

连续答错两次的定义是**同一题实例的两次选择均错**。不同题各错一次不触发本题强提示，但会增加本节疲劳计数。整节累计 3 个题实例发生首次错误时，设置 `endPendingReason=FATIGUE`；完成当前题并成功保存后提前进入结果页，不再生成或打开下一题。

### 看字选音特殊交互

1. 两个声音卡只有扬声器图标，不显示拼音；
2. 单击卡片只播放声音并选中，不立即判分；
3. 可反复试听两个声音；每次试听记录为非评分行为；
4. 孩子点对勾后才提交所选声音；
5. 未选择时对勾禁用；
6. 第一次提交才是 `attemptNumber=1`。

### 状态与保存

- 题目实例创建时保存选项 ID 和顺序，重进不重排。
- 每次提交答案使用单事务写入 `attempt`、更新 `question_instance`、重算 `character_progress` 和 session 进度。
- 练习页进入可交互状态时开始有效时长分段；提交、暂停、进入后台、页面切换和每 5 秒心跳均持久化累计值。
- 达到课程时长上限时设置 `endPendingReason=TIME_LIMIT`；若正在答题，允许完成并保存当前题后结束，绝不在题目中途切断。
- 页面关闭发生在提交之前：不产生作答记录。
- 页面关闭发生在提交成功之后、跳转之前：重进时直接显示下一题，不能重复记分。
- 数据保存失败：保持当前选项、显示“刚才没有保存好”，提供“再试一次”；失败 3 次进入家长处理态。

## 2.4 学习结果页

### 页面目的

明确结束，告诉孩子今天见过哪些字；告诉系统课程已经提交，避免无限继续。

### 元素与行为

| 元素 | 文字/行为 |
|---|---|
| 标题 | 正常：“今天完成啦”；提前结束：“今天先学到这里” |
| 字卡 | 只展示本节学习或复习的字；点击播放字音，不新增练习证据 |
| 温和提示 | 有困难字：“这个字以后再见一面”；不显示分数或红色失败 |
| 完成按钮 | “回到首页”；提交 session 后返回 |
| 家长提示 | 不在儿童主视觉中显示；首页家长入口可查看 |

### 完成规则

- 所有计划项目完成：`session.status=COMPLETED`。
- 主动点“先休息”：`session.status=PAUSED`、`pauseReason=USER_REST`，直接回首页；当天允许点“继续”，不进入结果页。恢复为 `ACTIVE` 时清空 `pauseReason`。
- 因疲劳或达到时间上限：`session.status=ENDED_EARLY`，`earlyEndReason` 分别为 `FATIGUE` 或 `TIME_LIMIT`；当天禁止重新开课。
- `ENDED_EARLY` 的已完成题目保留；未完成新字次日优先接续，未完成到期复习次日重新判定并生成新实例，具体见 3.7。
- 结果页进入前必须完成状态重算和下次复习日期写入。
- 结果页按系统返回等同“回到首页”，不重新打开最后一题。

### 状态

正常完成、提前结束、只复习完成、数据提交失败四态。提交失败时不显示“完成啦”，而显示家长处理态。

## 2.5 家长页

### 进入门槛

1. 首页长按右上角锁图标 3 秒；
2. 显示随机一位数加法，例如 `7 + 2 = ?`，三个数字按钮；
3. 答对进入，答错关闭弹层；无语音朗读；
4. 该门槛仅防误触，不宣称安全认证，不建立账号。

### 首次使用状态

首次启动直接显示家长页的设置标签，并先通过上述门槛。必须完成：

- 昵称：可选，0—8 个汉字/字母/数字；留空时使用无称呼问候；
- 每日新字数：默认 1，可选 1 或 2；
- 音量：默认 80%；
- 课程最长时间：默认 10 分钟，可选 8/10/12；
- 点击“完成设置”后写入 `onboardingCompleted=true`。

### 四个标签

#### A. 学习报告

逐字显示：

```text
山｜正在复习
首次学习：2026-07-25
有效日期：2 天　题型：3 种
APP 14天识别：待进行
家长直接认读：待进行
下次复习：2026-08-01
```

顶部只汇总五种状态的字数，不给总分。

#### B. 易错字

入选条件：最近 7 天首次无提示错误 ≥2，或家长结果为“提示后读出/未读出”。显示字、错误类别、最近错误日期、下次复习日。点击字可播放字音并查看记录，不可在家长页直接改 APP 作答。

#### C. 口头抽检

排序规则：

1. 首次学习已满 14 天且口头结果非“独立读出”；
2. APP 14 天识别已通过但口头未通过；
3. 口头结果为“未读出”；
4. 其他到期字。

每次列出最多 5 字（5 字原型即全部符合者）。家长点开一字后：

- 屏幕只显示 148sp 汉字，不自动发音、不显示图片；
- 家长让孩子直接读；
- 结果按钮：`独立读出`、`提示后读出`、`未读出`；
- 保存后显示时间和“修改本次结果”；
- 修改时不覆盖历史记录，而是新增 `revisionOf` 指向原记录；派生当前结果取最新有效记录。

只有首次学习满 14 天后记录的“独立读出”才能作为稳定掌握证据；更早的独立读出仅标记为练习记录。

#### D. 基本设置

- 修改昵称；
- 每日新字 1/2；
- 课程时长 8/10/12 分钟；
- 音量 0—100%；
- 导出诊断信息 JSON（仅含应用/内容版本、设置摘要、记录数量、最近成功保存时间和错误码，不含可恢复的完整学习记录）；
- 清空全部本地学习记录。

5 字原型**不提供备份导入或恢复**。清空需再次完成成人算式并二次确认，明确提示“清空后无法恢复”；诊断导出只用于排错，不能导回 APP。

### 数据异常

数据库只读或损坏时，家长页显示可读的错误说明、最近成功保存时间、`导出诊断信息`（不含身份数据）和`重试`。不得自动清空或静默重建覆盖原数据。

---

# 三、完整交互流程

## 3.1 第一次打开 APP

1. 启动页执行资源清单、JSON schema、数据库可写、应用版本检查；
2. 任一 P0 检查失败，进入儿童首页数据异常态；
3. `onboardingCompleted=false` 时进入家长页首次设置态；
4. 家长完成算式门槛，选择可选昵称、每日新字数、音量、时长；
5. 保存设置成功后回儿童首页；
6. 创建首日任务：默认新字“人”，无到期复习；
7. 仅在点击“开始”后创建正式 session。

## 3.2 开始当天学习

`DailyTaskGenerator` 只允许使用以下唯一完整顺序，3.7、6.7、8.7、AT-15 和 AT-39 均不得另行定义优先级：

1. 若当天已有 `CREATED/ACTIVE/PAUSED` session，直接返回原 session，不重新生成，主按钮显示“继续”；
2. 若存在历史日期未封账 session，先按 3.7 跨日封账；历史已完成记录保留；
3. 选取到期复习：`nextReviewDate <= today`，最多 3 字，按 `nextReviewDate` 升序→`isErrorProne=true` 优先→`learningOrder` 升序；超出 3 字的继续保持到期；
4. 选取历史未完成新字接续项：只含 `FIRST_LEARNING` 或 `initialLessonCompleted=false` 的字，按原 session 日期升序→原 `sequence` 升序；放在全部到期复习之后；
5. 再按 `learningOrder` 选尚未开始的新字。默认每日新字额度为 1；家长设置为 2 且本日到期复习不超过 1 字时额度为 2。历史未完成新字占用当日新字额度，不得因此额外多教新字；
6. session 顺序固定为“到期复习→未完成新字接续→全新字”；session 和全部题目实例事务保存成功后才进入第一项。

同一字若既因异常数据出现在到期集合又出现在未完成新字集合，只保留未完成新字身份，记录 `TASK_DATA_CONFLICT` 并阻断儿童进入；正常数据中两集合必须互斥。

## 3.3 学习一个新字

1. 进入单字学习页时将状态 `未学习→初次学习`；
2. 完成情境、字音义、词句三步；
3. 进入三道首次练习，优先题型：看字选图、听音选字、看字选音；
4. 三题实例均标记 `purpose=INITIAL`；三题都必须走完，错误题允许教学后结束；
5. 教学与三题完成后把状态 `初次学习→正在复习`；
6. 生成次日复习日期；
7. 当堂正确只计题型证据，不计跨日证据。
8. 若跨日接续，按该字历史上已完成的 `purpose=INITIAL` 题型集合，只生成固定顺序中尚未完成的首次题；已完成题无论正确或教学后结束都不重复，三种均完成后才置 `initialLessonCompleted=true`。

## 3.4 一次正确练习

1. 首次提交正确且未使用提示；
2. 事务保存 `attempt`；
3. `independentCorrect=true`；
4. 播放正确反馈；
5. 若这是到期复习检查中的合格题，参与本次里程碑判定；
6. 状态引擎重算，但同日重复不增加有效日期数。

## 3.5 一次错误练习

1. 首次提交错误，记录无提示错误；
2. 温和提示并允许第二次选择；
3. 第二次若正确，记录 `hintLevel=LIGHT`、`independentCorrect=false`；
4. 该题不能作为无提示正确，也不能推进复习里程碑；
5. 下次复习日设为次日。

## 3.6 连续答错两次

1. 同题第二次仍错；
2. 减少选项并直接建立目标字—正确答案联系；
3. 播放正确字音和简短解释；
4. 结束该题，`finalOutcome=TAUGHT_AFTER_ERROR`；
5. 不强迫第三次猜测；
6. 该字保持或回到“正在复习”，次日复习。

## 3.7 中途退出与重新进入

| 退出位置 | 保存内容 | 重进规则 |
|---|---|---|
| 单字页 | 当前 A/B/C 步骤、音频已播标记 | 恢复该步骤 |
| 未提交题目 | 题目实例、选项顺序、当前选择 | 恢复同题；未提交选择不算作答 |
| 已提交题目 | attempt 和下一题索引 | 从下一题继续 |
| 暂停确认选“先休息” | 累计有效时长、恢复游标，`session.status=PAUSED`、`pauseReason=USER_REST` | 当天首页显示“继续”，允许恢复；恢复后清空 `pauseReason` |
| APP 进入后台 | 立即结算本次有效时长分段并保存游标 | 回前台时继续，不累计后台时间 |
| APP 被系统杀死 | 最近事务性 checkpoint；5 秒心跳使未记时长最多损失 5 秒 | 冷启动后把旧活动分段视为已关闭，从 checkpoint 恢复，不把离线时间计入 |
| 疲劳/时间上限 | 完成并保存当前题或当前教学步骤后，`session.status=ENDED_EARLY` | 当天首页显示“今天先到这里”，禁止继续或新建日课 |
| 任一未完成/`ENDED_EARLY` session 到次日 | 先把历史日的 CREATED/ACTIVE/PAUSED 封账为 `ENDED_EARLY/DAY_ROLLOVER`；已完成记录保留；旧复习未提交实例作废并按到期规则重新生成；未完成新字保留接续游标 | 新日课严格按“到期复习→未完成新字接续→全新字”排序；不重复首教已完成步骤，不把旧未提交题算作答 |

学习中点返回时弹出：

- `继续学习`：关闭弹层；
- `先休息`：保存暂停并回首页。

不使用“确定/取消”抽象文字作为唯一提示，按钮配图标和语音。

### 3.7.1 暂停、提前结束与次日接续的唯一规则

| 事件 | session 状态 | 当天首页 | 当天能否继续 | 次日处理 |
|---|---|---|---:|---|
| 孩子/家长主动点“先休息” | `PAUSED` + `USER_REST` | “继续” | 是 | 若当天未继续，次日先封账为 `ENDED_EARLY/DAY_ROLLOVER` 再接续 |
| 一节累计 3 个题实例首次答错 | `ENDED_EARLY` + `FATIGUE` | “今天先到这里” | 否 | 已完成保留；次日先排到期复习，再接续未完成新字 |
| 累计有效时长达到设置上限 | `ENDED_EARLY` + `TIME_LIMIT` | “今天先到这里” | 否 | 同上 |
| 暂停/活动 session 跨到次日 | 旧 session `ENDED_EARLY` + `DAY_ROLLOVER` | 新日期按任务显示“开始” | 旧 session 否；新日课可以开始 | 到期复习→未完成新字接续→全新字 |
| 全部计划完成 | `COMPLETED` | “今天完成啦” | 否 | 按正常复习日期生成后续任务 |
| 数据写入连续失败 | `ERROR`，不是 `ENDED_EARLY` | “请叫大人来看看” | 修复前否 | 只从最后成功事务恢复 |

状态与原因字段必须遵循以下唯一约束：

1. `PAUSED` 是可恢复的非终态，只能由主动“先休息”产生；此时 `pauseReason=USER_REST`，`completedAt=null`、`earlyEndReason=null`，当天可以恢复为 `ACTIVE`。
2. `USER_REST` 是“主动休息原因”，不是提前结束原因，不得写入 `earlyEndReason`。从 `PAUSED` 恢复后必须在同一事务中清空 `pauseReason`。
3. `FATIGUE` 的唯一触发条件是一节课内累计 3 个不同题目实例发生首次错误；设置 `endPendingReason=FATIGUE`，完成并保存当前题后转为 `ENDED_EARLY`。
4. `TIME_LIMIT` 的唯一触发条件是累计有效学习时长达到本节固化的 `limitMinutesSnapshot`；设置 `endPendingReason=TIME_LIMIT`，完成并保存当前题或当前教学步骤后转为 `ENDED_EARLY`。
5. `ENDED_EARLY` 是当天不可恢复的终态，必须写 `completedAt`；由疲劳或时限产生时，`earlyEndReason` 只能是 `FATIGUE` 或 `TIME_LIMIT`，同一 `localDate` 内不能改回 `ACTIVE`。
6. 首次在更晚的 `LocalDate` 启动时，Repository 必须在创建新日课前，把旧的 `CREATED/ACTIVE/PAUSED` session 单事务封账为 `ENDED_EARLY`、`earlyEndReason=DAY_ROLLOVER` 并写 `completedAt`。原为 `PAUSED/USER_REST` 的记录保留 `pauseReason` 作为发生过主动休息的审计信息，但当前状态以 `ENDED_EARLY/DAY_ROLLOVER` 为准。

### 3.7.2 有效学习时长累计

1. 只累计儿童单字学习页和练习页处于前台且 session 为 `ACTIVE` 的时间；首页、结果页、家长页、暂停弹层、后台和音频因系统打断的等待不累计。
2. 进入有效页面时写 `activeSegmentStartedAt`；每 5 秒、页面切换、提交答案、暂停、`onStop` 时，把本段差值合并到 `activeElapsedMs` 并清空分段开始时间。
3. 冷启动发现非空的旧 `activeSegmentStartedAt` 时，不使用“当前时间减旧时间”补算，避免把被杀进程后的后台时间算入；只保留此前已提交的 `activeElapsedMs`。
4. 创建日课时把设置值固化为 `limitMinutesSnapshot`；UI 每秒检查 `activeElapsedMs + 当前前台分段`。达到该上限后置 `endPendingReason=TIME_LIMIT`，中途修改全局设置只影响下一节课。
5. 若正在答题，当前题可以完成两次错误支持并成功保存；若正在 A/B/C 教学步骤，只完成当前播放/步骤；之后不得打开下一题或下一步，提交 `ENDED_EARLY`。
6. 自动测试使用注入的 `Clock`，允许真机因 5 秒心跳产生不超过 5 秒的显示误差，但不得因此多开启一道新题。

## 3.8 当天学习完成

1. 全部计划项完成后重算所有相关字状态；
2. 生成下次复习日；
3. 提交 session；
4. 进入结果页；
5. 当天首页显示“今天完成啦”，不得继续生成新字；
6. 家长修改系统日期不应重复生成同一 `localDate` 的第二个日课。
7. `PAUSED` 不属于完成；只有 `COMPLETED` 或 `ENDED_EARLY` 会封闭当天再次开课。

## 3.9 到期复习

1. 每次启动或回首页时查询 `nextReviewDate <= today`；
2. 先于历史未完成新字接续和全新字进入 session；唯一顺序以 3.2 为准；
3. 每字构成一个“复习检查”：两道不同题型；
4. 两题均为首次、无提示、独立正确才算本次里程碑通过；
5. 任一题错误或提示后正确，里程碑失败，次日再复习；
6. 错过计划日不惩罚，实际完成后按规则计算下一日期。

## 3.10 家长查看报告

进入家长门槛→报告标签→按字读取派生状态、有效日期、题型、下次复习日、APP 延迟结果、口头结果。报告数据由查询实时派生，不保存第二份可能失真的汇总。

## 3.11 家长口头抽检与修改

1. 进入“口头抽检”；
2. 选择到期字；
3. 屏幕只显示字，不播放音频；
4. 家长记录三选一结果；
5. 保存后状态引擎重算；
6. 若点“修改本次结果”，新增修订记录并引用原记录；
7. 最新有效值决定当前显示，历史均保留；
8. 从“独立读出”改为“未读出”时，若原为稳定掌握，立即写稳定回退并退到“正在复习”，次日复习；回退前的 APP/口头通过不得复用，重新达标按 4.6.3 第8—9条。

## 3.12 本地数据保存失败

1. 写事务失败时不前进、不播放“完成”；
2. 当前答案和题目实例留在内存；
3. 显示“刚才没有保存好，请再试一次”，提供重试；
4. 第 3 次失败进入家长处理态；
5. 家长可重启 APP；启动后只以最后成功事务为准；
6. 不自动清库、不伪造成功、不把未保存答案计入报告；
7. 记录不含儿童信息的本地错误码和时间，最多保留 50 条。

---

# 四、学习状态与复习规则

## 4.1 唯一允许的五种状态

```text
未学习 → 初次学习 → 正在复习 → 暂时掌握 → 稳定掌握
```

`易错`、`暂停`、`待口头验证`不是学习状态，只是标记或任务状态。

## 4.2 状态进入、退出和回退

| 状态 | 进入条件 | 正常退出 | 回退/保持 |
|---|---|---|---|
| 未学习 `UNLEARNED` | 初始、无 `firstStartedAt` | 开始单字页后进入初次学习 | 不存在更低状态 |
| 初次学习 `FIRST_LEARNING` | 单字页首次打开且事务保存成功 | 完成 A/B/C 教学和 3 道首次练习后进入正在复习 | 中断仍保持；清空数据才回未学习 |
| 正在复习 `REVIEWING` | 首次学习完成；或较高状态回退 | 满足暂时掌握硬条件 | 错误、提示后正确、证据不足均保持 |
| 暂时掌握 `TEMP_MASTERED` | ≥2 个不同日期；≥3 题型；覆盖至少 3 类证据；最近一条已完成到期检查为 PASS；不存在未清除的暂时回退 | 满足稳定掌握硬条件 | 暂时掌握后任一到期检查 FAIL，写入暂时回退并回正在复习；随后一条到期检查 PASS 才清除并可重新达标 |
| 稳定掌握 `STABLE_MASTERED` | 首学满 14 天；≥3 日期；≥4 题型；14 天 APP 检查通过；满 14 天后的家长独立读出通过；不存在未清除的稳定回退 | 低频维护，不设更高状态 | 家长最新有效结果改为非独立读出，立即回正在复习；单个日期一次到期 FAIL 保持稳定；自最近稳定资格时间后两个不同日期各有到期 FAIL，写入稳定回退并回正在复习 |

## 4.3 证据定义

- `不同日期`：设备本地时区下不同的 `LocalDate`；同一天练 20 次只算 1 个日期。
- `题型数`：在首次无提示作答中实际出现过的不同 `questionType`。
- `无提示正确`：`attemptNumber=1`、`isCorrect=true`、`hintLevel=NONE`。
- `提示后答对`：不是无提示正确；可显示鼓励，但不推进掌握里程碑。
- `疑似误触`：同一题进入后 300ms 内点击，或同时多点；记录 `isAccidental=true`，不参与状态证据，重新呈现同题。
- `跨题型`：不同题型 ID，不以同题换图冒充。

## 4.4 复习里程碑

1、3、7、14 天为本项目**待验证的初始参数**，不是这个孩子的科学最佳间隔。

| 当前里程碑 | 计划日期 | 通过条件 | 通过后 | 未通过 |
|---|---|---|---|---|
| 首学完成 | 首学日 | 教学和 3 题走完 | `firstLearnDate+1` | 未完成则保持初次学习 |
| D1 | 首学+1 | 两个不同题型均首次无提示正确 | 下一次 `max(首学+3, 实际日+1)` | 实际日+1 |
| D3 | 首学+3 | 同上 | `max(首学+7, 实际日+1)` | 实际日+1 |
| D7 | 首学+7 | 同上 | `max(首学+14, 实际日+1)` | 实际日+1 |
| D14 | 首学+14 或以后 | 两题均首次无提示正确，且包含“听音选字”和“看字选音” | APP 延迟识别 PASS；若口头也通过则稳定掌握，否则 7 天后再查 | APP 延迟识别 FAIL；实际日+1 |
| D30 | D14 通过后 30 天 | 两题首次无提示正确 | 60 天后低频维护 | 实际日+1，状态按回退规则重算 |

复习日期只按“日期”比较，不按小时；时区使用设备当前时区。设备日期倒退超过 24 小时时记录 `CLOCK_ROLLBACK`，不删除记录，今日任务沿用已创建 session，家长页提示核对系统日期。

## 4.5 APP 延迟识别和家长直接认读

| 证据 | 字段 | 通过条件 | 失败后 |
|---|---|---|---|
| APP 14 天延迟识别 | `appDelayedCheckStatus` | 首学满 14 天；同一 D14 检查中“听音选字”和“看字选音”均首次无提示正确 | 标记 FAIL，次日复习 |
| 家长直接认读 | `currentOralStatus` | 首学满 14 天后，家长记录“独立读出” | 提示后/未读出不通过，次日复习并进入抽检队列 |

稳定掌握布尔公式：

```text
stable =
  firstLearnDate <= today - 14 days
  AND validDateCount >= 3
  AND independentQuestionTypeCount >= 4
  AND appDelayedCheckStatus == PASS
  AND currentOralStatus == INDEPENDENT_PASS
  AND hasStableRollback == false
```

早于第 14 天的口头“独立读出”可以保留，但 `eligibleForStable=false`，不得替代第 14 天后的直接认读。

## 4.6 回退数据、窗口、清除和重新达标

### 4.6.1 唯一数据来源

状态引擎只读取以下已持久化事件，不允许 UI 或测试直接写掌握结论：

- 到期检查：已完成的 `session_item(kind=REVIEW)`，其 `completedLocalDate`、`completedAt`、`dueCheckPassed` 和两道 `isMilestoneQuestion=true` 的正式答题记录；
- APP 延迟检查：`character_progress.appDelayedCheckStatus/appDelayedCheckAt`，只能由 D14 两道正式题提交事务更新；
- 家长认读：`oral_check` 的最新未被替代记录；
- 回退缓存：`temporaryQualifiedAt`、`stableQualifiedAt`、`temporaryRollbackAt`、`stableRollbackAt`。它们由 `MasteryStateEngine.recalculate` 根据上述事件在同一事务中更新，均可由事件全量重建。

“到期检查 PASS”必须是同一个 REVIEW `session_item` 的两道里程碑题均首次、无提示、独立正确；否则该 item 完成时写 `dueCheckPassed=false`。未完成 item、未提交题、`isAccidental=true` 和普通证据补题均不是到期检查结果。

### 4.6.2 四个判定量的完整定义

```text
latestDueCheckPassed:
  查询该字 dueCheckPassed 非空的已完成 REVIEW item，
  按 completedLocalDate DESC、completedAt DESC、id DESC 取第一条；
  有记录且 dueCheckPassed=true 时为 true，无记录或最新为 false 时为 false。

hasTemporaryRollback:
  temporaryRollbackAt != null
  且不存在 completedAt > temporaryRollbackAt 的到期检查 PASS。

hasStableRollback:
  stableRollbackAt != null
  且 stableQualifiedAt == null 或 stableQualifiedAt <= stableRollbackAt。

stableFailureDates:
  只统计 completedLocalDate > LocalDate(stableQualifiedAt) 的到期检查 FAIL，
  对 completedLocalDate 去重；同一天无论失败几题或几个 item 只算一个失败日期。
```

### 4.6.3 触发、持久化与清除规则

1. 首次满足暂时掌握时写 `temporaryQualifiedAt=now`，并确保 `temporaryRollbackAt=null`。
2. 首次满足稳定掌握硬条件时，在同一重算事务写 `stableQualifiedAt=max(appDelayedCheckAt,currentOralCheckAt)`，并确保 `stableRollbackAt=null`；失败日期窗口从该锚点所在设备本地日期之后开始。
3. `TEMP_MASTERED` 后出现一条到期 FAIL：在同一事务写 `temporaryRollbackAt=该 item.completedAt`，立即回 `REVIEWING`，`nextReviewDate=失败实际日+1`。
4. 暂时回退后，后来一条到期 PASS 会清空 `temporaryRollbackAt`、更新 `temporaryQualifiedAt=该 item.completedAt`；若日期、题型和证据类别阈值仍满足，则重新进入 `TEMP_MASTERED`。
5. `STABLE_MASTERED` 后只有一个失败日期时，不写 `stableRollbackAt`，状态保持稳定，但 `nextReviewDate=失败实际日+1`。
6. 自 `stableQualifiedAt` 后累计到第二个不同失败日期时，在第二次失败事务写 `stableRollbackAt=该 item.completedAt`，同时写 `temporaryRollbackAt`，状态回 `REVIEWING`。
7. 家长把最新有效口头结果改为 `PROMPTED/FAIL` 时，不等待第二个 APP 失败日期，立即写 `stableRollbackAt=oral_check.checkedAt` 和 `temporaryRollbackAt`，状态回 `REVIEWING`。
8. 稳定回退后，先完成一条到期 PASS，可按第4条重新获得 `TEMP_MASTERED`；但不得直接恢复稳定。
9. 重新获得 `STABLE_MASTERED` 必须在 `stableRollbackAt` 之后重新完成：
   - 一次 D14 规格的 APP 检查 PASS（听音选字＋看字选音，均首次无提示正确），使 `appDelayedCheckAt > stableRollbackAt`；
   - 一次新的家长独立认读，且 `currentOralCheckAt > stableRollbackAt`。
   两项齐全后在同一重算事务更新 `stableQualifiedAt=max(appDelayedCheckAt,currentOralCheckAt)`，清空 `stableRollbackAt`，重新进入稳定掌握。回退前的 APP/口头通过不能复用。
10. 回退缓存只由正式答题、到期 item 完成或口头抽检事务驱动；清空学习记录时随学习表一并删除。

## 4.7 状态引擎伪代码

```kotlin
fun deriveState(c: ProgressEvidence, today: LocalDate): LearningState {
    if (c.firstStartedAt == null) return UNLEARNED
    if (!c.initialLessonCompleted) return FIRST_LEARNING

    val stableBase = c.firstLearnDate <= today.minusDays(14) &&
        c.validDateCount >= 3 &&
        c.independentQuestionTypes.size >= 4 &&
        c.appDelayedCheckStatus == PASS &&
        c.currentOralStatus == INDEPENDENT_PASS
    if (stableBase && !hasStableRollback(c)) return STABLE_MASTERED

    val temporaryBase = c.validDateCount >= 2 &&
        c.independentQuestionTypes.size >= 3 &&
        c.evidenceCategories.size >= 3
    if (
        temporaryBase &&
        latestDueCheckPassed(c) &&
        !hasTemporaryRollback(c)
    ) return TEMP_MASTERED

    return REVIEWING
}

fun onDueCheckCompleted(item: SessionItem, c: CharacterProgress) {
    require(item.kind == REVIEW && item.dueCheckPassed != null)
    if (item.dueCheckPassed == true) {
        c.temporaryRollbackAt = null
        if (temporaryBase(c)) c.temporaryQualifiedAt = item.completedAt
        return
    }

    c.nextReviewDate = item.completedLocalDate.plusDays(1)
    if (c.state == TEMP_MASTERED) c.temporaryRollbackAt = item.completedAt
    if (c.state == STABLE_MASTERED) {
        val failureDates = failedDueDatesAfter(c.stableQualifiedAt)
        if (failureDates.size >= 2) {
            c.stableRollbackAt = item.completedAt
            c.temporaryRollbackAt = item.completedAt
        }
    }
}

fun tryRequalifyStable(c: CharacterProgress) {
    val rollback = c.stableRollbackAt ?: return
    val freshApp = c.appDelayedCheckStatus == PASS && c.appDelayedCheckAt > rollback
    val freshOral = c.currentOralStatus == INDEPENDENT_PASS && c.currentOralCheckAt > rollback
    if (freshApp && freshOral) {
        c.stableQualifiedAt = max(c.appDelayedCheckAt, c.currentOralCheckAt)
        c.stableRollbackAt = null
    }
}
```

所有比较均使用非空检查后的 `Instant`；`failedDueDatesAfter` 通过 `session_item` 联结 `learning_session` 查询，不从当前状态反推历史。

## 4.8 P0 五字全空库稳定掌握可达性测试约束

AT-36 必须只执行一次“全空数据库→五字全部稳定掌握”的完整场景，不得把五个字拆成五个预置其他字条件的场景：

1. 初始化真实 5 字 JSON、空 Room 内存库、正式 `DailyTaskGenerator`、题型调度器、`SubmitAnswerUseCase`、`MasteryStateEngine`、`AppendOralCheckUseCase`，并注入固定 `Clock/ZoneId/Random`。
2. 第一次生成前断言五字全部 `UNLEARNED`，所有学习表行数为 0；不得插入任何“已学习”前置行。
3. 从基准日 D0 起逐日调用 `DailyTaskGenerator.getOrCreate(date)`。按唯一优先级完成当日到期复习，再按 `learningOrder` 正式首教“人→口→大→小→山”；不得直接构造 session、item 或题目实例。
4. NEW 模式首次三题必须由 JSON 真实种子生成，顺序为看字选图→听音选字→看字选音；`minLearnedCount` 资格必须真实满足。
5. D1、D3、D7、D14 只通过推进 Clock 触发；每次题目由正式调度器生成并通过正式答题事务提交。D3/D7 期间必须由正式调度补出第 4 种独立题型；D14 必须生成听音选字＋看字选音。
6. 五字 D14 APP 检查通过后，逐字调用正式家长抽检入口记录第14天后的独立读出，再由状态引擎重算。
7. 测试代码和机器模型均禁止：直接更新 `character_progress.state`，直接写 `reviewStage/nextReviewDate/appDelayedCheckStatus/currentOralStatus`，伪造 `validDateCount`/题型集合，或绕过 `DailyTaskGenerator` 手工插入任务。
8. 每完成一个 session item 立即输出：日期、字、项目类型/里程碑、实际题型、种子资格、状态、有效日期数、独立题型数、APP结果、口头结果。任一资格、题型、状态或写入断言失败时立即停止，不再继续后续字，并输出 `OVERALL=FAIL` 和首个失败步骤。
9. 最终必须同时断言五字都真实经过 `UNLEARNED→FIRST_LEARNING→REVIEWING→TEMP_MASTERED→STABLE_MASTERED`；任一字未达到稳定掌握则 AT-36 整项失败。

当前机器检查运行的是与本规格同字段、同 JSON、同过滤顺序的可执行参考模型，用于证明规格内在可达；开发后的 AT-36 必须对真实 Kotlin/Room 正式实现复跑，参考模型不能替代实现验收。

---

# 五、本地数据结构

## 5.1 存储边界

- 内容数据：APK 内 `assets/content/v1/` 的只读 JSON。
- 学习记录：Room/SQLite，数据库名 `shizi.db`。
- 设置：Preferences DataStore，文件名 `settings.preferences_pb`。
- 诊断导出：家长可通过系统文件选择器导出不可回导的最小诊断 JSON；APP 不自动上传。
- 5 字原型不做学习记录备份、导入或恢复；清空学习记录后不可恢复。
- 禁止使用：远程数据库、Web API、Firebase、第三方统计、SharedPreferences 作为主记录库。

## 5.2 内容数据字段

### `CharacterContent`

| 字段名 | 类型 | 必填 | 默认值 | 含义 |
|---|---|---:|---|---|
| `id` | string | 是 | 无 | 稳定 ID，如 `char_ren`；发布后不可改 |
| `character` | string | 是 | 无 | 一个规范简体汉字 |
| `pinyin` | string | 是 | 无 | 带声调拼音，只供内容核对和家长端 |
| `toneNumber` | int | 是 | 无 | 1—4 |
| `order` | int | 是 | 无 | 学习顺序，1—5，唯一 |
| `meaningForChild` | string | 是 | 无 | 幼儿化但准确的字义 |
| `imageAsset` | string | 是 | 无 | 主插画相对路径 |
| `imageAlt` | string | 是 | 无 | 家长/测试使用的图片含义 |
| `words` | array<WordContent> | 是 | `[]` | 至少 3 个词 |
| `sentence` | SentenceContent | 是 | 无 | 一句口语短句 |
| `audio` | AudioRefs | 是 | 无 | 单字、字义音频 |
| `teachingPrompt` | string | 是 | 无 | 开发和配音使用的教学提示 |
| `confusableRestrictions` | array<string> | 是 | `[]` | 5 字阶段禁用的形近字 |
| `misconceptions` | array<string> | 是 | `[]` | 可能误解 |
| `questionSeeds` | array<QuestionSeed> | 是 | `[]` | 该字可生成的题目 |
| `contentReview` | ContentReview | 是 | 无 | 文本与资源审核状态 |

### 子类型

| 类型 | 字段 |
|---|---|
| `WordContent` | `text:string`、`audioAsset:string` |
| `SentenceContent` | `text:string`、`audioAsset:string` |
| `AudioRefs` | `character:string`、`meaning:string` |
| `OptionContent` | `id:string`、`kind:TEXT/IMAGE/AUDIO/CONTEXT`、`characterId:string?`、`text:string?`、`asset:string?` |
| `QuestionSeed` | `id:string`、`type:enum`、`promptAudio:string`、`correctOptionId:string`、`optionIds:string[]`、`minLearnedCount:int`、`evidenceCategory:enum`。`minLearnedCount` 表示“生成该题实例前，`initialLessonCompleted=true` 的不同汉字数量”，包含已完成首学的目标字；NEW 固定三题在目标字尚未完成首学时必须为 0 |
| `ContentReview` | `textReviewed:boolean`、`assetReviewedByDeveloper:boolean`、`assetReviewedByParent:boolean`、`blockedReason:string?` |

## 5.3 Room 表字段

### A. `character_progress`

| 字段名 | SQLite/Kotlin 类型 | 必填 | 默认值 | 含义 |
|---|---|---:|---|---|
| `characterId` | TEXT/String，PK | 是 | 无 | 对应内容 ID |
| `state` | TEXT/LearningState | 是 | `UNLEARNED` | 五种状态之一 |
| `firstStartedAt` | INTEGER/Instant? | 否 | null | 首次打开教学页 |
| `firstLearnDate` | TEXT/LocalDate? | 否 | null | 首次教学完成日期 |
| `initialTeachingStep` | TEXT/InitialTeachingStep | 是 | `NOT_STARTED` | NOT_STARTED/A_CONTEXT/B_SOUND_MEANING/C_WORD_SENTENCE/PRACTICE/DONE；跨日接续首教 |
| `initialLessonCompleted` | INTEGER/Boolean | 是 | false | 首次教学与 3 题是否走完 |
| `reviewStage` | TEXT/ReviewStage | 是 | `NONE` | NONE/D1/D3/D7/D14/D30/D60 |
| `nextReviewDate` | TEXT/LocalDate? | 否 | null | 下次到期日期 |
| `appDelayedCheckStatus` | TEXT/DelayedStatus | 是 | `NOT_DUE` | NOT_DUE/PENDING/PASS/FAIL |
| `appDelayedCheckAt` | INTEGER/Instant? | 否 | null | 最近 D14 结果时间 |
| `currentOralStatus` | TEXT/OralStatus | 是 | `NOT_TESTED` | NOT_TESTED/INDEPENDENT_PASS/PROMPTED/FAIL |
| `currentOralCheckAt` | INTEGER/Instant? | 否 | null | 最新有效口头结果时间 |
| `temporaryQualifiedAt` | INTEGER/Instant? | 否 | null | 最近进入/重新进入暂时掌握的资格锚点；由状态引擎从事件更新 |
| `stableQualifiedAt` | INTEGER/Instant? | 否 | null | 最近进入/重新进入稳定掌握的资格锚点 |
| `temporaryRollbackAt` | INTEGER/Instant? | 否 | null | 暂时掌握后最新未清除的到期失败时间 |
| `stableRollbackAt` | INTEGER/Instant? | 否 | null | 稳定掌握的第二个不同日期失败或口头撤销触发时间；重新双验证后清空 |
| `isErrorProne` | INTEGER/Boolean | 是 | false | 派生易错标记的缓存；每次事务重算 |
| `updatedAt` | INTEGER/Instant | 是 | 当前时间 | 最后更新 |

`state` 与四个资格/回退时间均是正式事件的可重建缓存，只能由 `MasteryStateEngine` 在答题或抽检事务内更新；测试不得直接赋值。

### B. `learning_session`

| 字段名 | 类型 | 必填 | 默认值 | 含义 |
|---|---|---:|---|---|
| `id` | TEXT/UUID，PK | 是 | UUID | 一次日课 |
| `localDate` | TEXT/LocalDate | 是 | 今日 | 本地日期；同日最多一条未废弃主日课 |
| `status` | TEXT/SessionStatus | 是 | `CREATED` | CREATED/ACTIVE/PAUSED/COMPLETED/ENDED_EARLY/ERROR |
| `startedAt` | INTEGER/Instant? | 否 | null | 首次开始 |
| `completedAt` | INTEGER/Instant? | 否 | null | 正常/提前结束 |
| `currentItemIndex` | INTEGER/Int | 是 | 0 | 恢复游标 |
| `currentQuestionInstanceId` | TEXT/String? | 否 | null | 恢复到具体题 |
| `plannedNewCount` | INTEGER/Int | 是 | 0 | 固化的新字数 |
| `plannedReviewCount` | INTEGER/Int | 是 | 0 | 固化的复习数 |
| `limitMinutesSnapshot` | INTEGER/Int | 是 | 无 | 创建日课时从设置固化的 8/10/12；中途不变 |
| `contentVersion` | TEXT/String | 是 | 无 | 如 `1.0.0` |
| `activeElapsedMs` | INTEGER/Long | 是 | 0 | 已持久化的有效学习累计时长，不含暂停/后台 |
| `activeSegmentStartedAt` | INTEGER/Instant? | 否 | null | 当前前台有效计时分段起点；结算后置空 |
| `pauseReason` | TEXT/PauseReason? | 否 | null | 仅 `PAUSED` 使用；当前唯一值 `USER_REST`。恢复为 `ACTIVE` 时清空；跨日封账时可保留为审计信息 |
| `endPendingReason` | TEXT/EarlyEndReason? | 否 | null | 当前题/步骤完成后待提前结束：FATIGUE/TIME_LIMIT |
| `earlyEndReason` | TEXT/EarlyEndReason? | 否 | null | 仅 ENDED_EARLY 使用：FATIGUE/TIME_LIMIT/DAY_ROLLOVER |

唯一索引：`(localDate) WHERE status != 'ERROR'` 的业务约束由 Repository 事务保证；Room 可用普通索引并在创建事务中查询防重。

枚举定义：

- `PauseReason = USER_REST`；
- `EarlyEndReason = FATIGUE | TIME_LIMIT | DAY_ROLLOVER`；
- `USER_REST` 不得序列化到 `earlyEndReason`，`FATIGUE/TIME_LIMIT/DAY_ROLLOVER` 不得序列化到 `pauseReason`。

### C. `session_item`

| 字段名 | 类型 | 必填 | 默认值 | 含义 |
|---|---|---:|---|---|
| `id` | TEXT/UUID，PK | 是 | UUID | session 中一个字的任务 |
| `sessionId` | TEXT/FK | 是 | 无 | 所属 session |
| `characterId` | TEXT/String | 是 | 无 | 目标字 |
| `kind` | TEXT/ItemKind | 是 | 无 | NEW/REVIEW |
| `sequence` | INTEGER/Int | 是 | 无 | 固化顺序 |
| `status` | TEXT/ItemStatus | 是 | PENDING | PENDING/ACTIVE/COMPLETED/SKIPPED |
| `reviewStageAtStart` | TEXT/ReviewStage | 是 | NONE | 本次开始时里程碑 |
| `dueCheckPassed` | INTEGER/Boolean? | 否 | null | 两题检查是否全通过 |
| `completedAt` | INTEGER/Instant? | 否 | null | item 完成时间；写 `dueCheckPassed` 时必须同时写入 |
| `completedLocalDate` | TEXT/LocalDate? | 否 | null | item 完成的设备本地日期；回退失败窗口按此字段去重 |

到期结果查询索引：`(characterId, kind, completedLocalDate, completedAt)`；其中 `characterId` 已在本表。`latestDueCheckPassed` 和失败日期窗口必须从本表查询，不得从当前 `state` 反推。

### D. `question_instance`

| 字段名 | 类型 | 必填 | 默认值 | 含义 |
|---|---|---:|---|---|
| `id` | TEXT/UUID，PK | 是 | UUID | 一道固化题 |
| `sessionItemId` | TEXT/FK | 是 | 无 | 所属字任务 |
| `questionSeedId` | TEXT/String | 是 | 无 | 来源配置 |
| `questionType` | TEXT/QuestionType | 是 | 无 | 五类题型之一 |
| `evidenceCategory` | TEXT/EvidenceCategory | 是 | 无 | SOUND_TO_SHAPE/SHAPE_TO_MEANING/SHAPE_TO_SOUND/SHAPE/CONTEXT |
| `optionIdsJson` | TEXT/JSON array | 是 | `[]` | 固化后的选项顺序 |
| `correctOptionId` | TEXT/String | 是 | 无 | 唯一正确项 |
| `status` | TEXT/QuestionStatus | 是 | PENDING | PENDING/ACTIVE/COMPLETED |
| `selectedOptionId` | TEXT/String? | 否 | null | 当前未提交选择，仅看字选音使用 |
| `finalOutcome` | TEXT/FinalOutcome? | 否 | null | CORRECT/TAUGHT_AFTER_ERROR/ABANDONED |
| `purpose` | TEXT/QuestionPurpose | 是 | 无 | INITIAL/REVIEW/EVIDENCE；用于跨日判断首次三题是否已走完 |
| `isMilestoneQuestion` | INTEGER/Boolean | 是 | false | 是否参与 D1/D3...判定 |

### E. `practice_attempt`

| 字段名 | 类型 | 必填 | 默认值 | 含义 |
|---|---|---:|---|---|
| `id` | TEXT/UUID，PK | 是 | UUID | 一次提交 |
| `questionInstanceId` | TEXT/FK | 是 | 无 | 对应题 |
| `characterId` | TEXT/String | 是 | 无 | 冗余便于查询 |
| `attemptNumber` | INTEGER/Int | 是 | 1 | 同题第几次提交 |
| `selectedOptionId` | TEXT/String | 是 | 无 | 所选项 |
| `isCorrect` | INTEGER/Boolean | 是 | false | 是否正确 |
| `hintLevel` | TEXT/HintLevel | 是 | NONE | NONE/LIGHT/STRONG |
| `independentCorrect` | INTEGER/Boolean | 是 | false | 首次、无提示、正确时为 true |
| `isAccidental` | INTEGER/Boolean | 是 | false | 是否疑似误触；为 true 时不计证据 |
| `answeredAt` | INTEGER/Instant | 是 | 当前时间 | 提交时间 |
| `localDate` | TEXT/LocalDate | 是 | 今日 | 跨日统计 |
| `responseTimeMs` | INTEGER/Long | 是 | 0 | 从题可操作到提交 |

唯一约束：`(questionInstanceId, attemptNumber)`。

### F. `oral_check`

| 字段名 | 类型 | 必填 | 默认值 | 含义 |
|---|---|---:|---|---|
| `id` | TEXT/UUID，PK | 是 | UUID | 一次口头记录 |
| `characterId` | TEXT/String | 是 | 无 | 目标字 |
| `result` | TEXT/OralStatus | 是 | 无 | INDEPENDENT_PASS/PROMPTED/FAIL |
| `checkedAt` | INTEGER/Instant | 是 | 当前时间 | 记录时间 |
| `localDate` | TEXT/LocalDate | 是 | 今日 | 抽检日期 |
| `eligibleForStable` | INTEGER/Boolean | 是 | false | 是否首学满 14 天且独立读出 |
| `revisionOf` | TEXT/UUID? | 否 | null | 修改时指向被修订记录 |
| `isSuperseded` | INTEGER/Boolean | 是 | false | 是否已被后续修改替代 |

### G. `app_error_log`

| 字段名 | 类型 | 必填 | 默认值 | 含义 |
|---|---|---:|---|---|
| `id` | TEXT/UUID，PK | 是 | UUID | 本地错误 |
| `code` | TEXT/String | 是 | 无 | 稳定错误码 |
| `occurredAt` | INTEGER/Instant | 是 | 当前时间 | 时间 |
| `context` | TEXT/String | 是 | `{}` | 不含身份信息的简短 JSON |

只保留最近 50 条。

## 5.4 设置字段

| 字段名 | 类型 | 必填 | 默认值 | 含义 |
|---|---|---:|---|---|
| `schemaVersion` | int | 是 | 1 | 设置 schema |
| `onboardingCompleted` | boolean | 是 | false | 首次设置完成 |
| `nickname` | string | 是 | `""` | 可选，最多 8 字符 |
| `dailyNewCharacterCount` | int | 是 | 1 | 只允许 1 或 2 |
| `sessionLimitMinutes` | int | 是 | 10 | 只允许 8/10/12 |
| `volumePercent` | int | 是 | 80 | 0—100 |
| `isMuted` | boolean | 是 | false | 儿童首页静音 |
| `lastKnownLocalDate` | string? | 否 | null | 检测日期回拨 |
| `lastSuccessfulSaveAt` | long? | 否 | null | 家长诊断显示 |
| `contentVersion` | string | 是 | `1.0.0` | 当前内容版本 |

## 5.5 包含 5 字的真实 JSON 示例

下面是 `assets/content/v1/content.json` 的可实现种子数据。图片和音频路径必须在构建校验中真实存在。

```json
{
  "schemaVersion": 1,
  "contentVersion": "1.0.0",
  "learningOrder": ["char_ren", "char_kou", "char_da", "char_xiao", "char_shan"],
  "reviewOffsetsDays": [1, 3, 7, 14, 30, 60],
  "optionCatalog": [
    {"id": "text_char_ren", "kind": "TEXT", "characterId": "char_ren", "text": "人"},
    {"id": "text_char_kou", "kind": "TEXT", "characterId": "char_kou", "text": "口"},
    {"id": "text_char_da", "kind": "TEXT", "characterId": "char_da", "text": "大"},
    {"id": "text_char_xiao", "kind": "TEXT", "characterId": "char_xiao", "text": "小"},
    {"id": "text_char_shan", "kind": "TEXT", "characterId": "char_shan", "text": "山"},
    {"id": "image_person", "kind": "IMAGE", "characterId": "char_ren", "asset": "images/options/image_person_v1.webp"},
    {"id": "image_mouth", "kind": "IMAGE", "characterId": "char_kou", "asset": "images/options/image_mouth_v1.webp"},
    {"id": "image_mountain", "kind": "IMAGE", "characterId": "char_shan", "asset": "images/options/image_mountain_v1.webp"},
    {"id": "image_big_watermelon", "kind": "IMAGE", "characterId": "char_da", "asset": "images/options/image_big_watermelon_v1.webp"},
    {"id": "image_small_watermelon", "kind": "IMAGE", "characterId": "char_xiao", "asset": "images/options/image_small_watermelon_v1.webp"},
    {"id": "audio_char_ren", "kind": "AUDIO", "characterId": "char_ren", "asset": "audio/characters/char_ren_v1.mp3"},
    {"id": "audio_char_kou", "kind": "AUDIO", "characterId": "char_kou", "asset": "audio/characters/char_kou_v1.mp3"},
    {"id": "audio_char_da", "kind": "AUDIO", "characterId": "char_da", "asset": "audio/characters/char_da_v1.mp3"},
    {"id": "audio_char_xiao", "kind": "AUDIO", "characterId": "char_xiao", "asset": "audio/characters/char_xiao_v1.mp3"},
    {"id": "audio_char_shan", "kind": "AUDIO", "characterId": "char_shan", "asset": "audio/characters/char_shan_v1.mp3"},
    {"id": "context_dashan", "kind": "CONTEXT", "characterId": "char_da", "text": "大山", "asset": "images/options/context_dashan_v1.webp"},
    {"id": "context_xiaoshan", "kind": "CONTEXT", "characterId": "char_xiao", "text": "小山", "asset": "images/options/context_xiaoshan_v1.webp"}
  ],
  "characters": [
    {
      "id": "char_ren",
      "character": "人",
      "pinyin": "rén",
      "toneNumber": 2,
      "order": 1,
      "meaningForChild": "像你、爸爸妈妈、老师这样，都是人。",
      "imageAsset": "images/characters/char_ren_main_v1.webp",
      "imageAlt": "一名完整站立、自然微笑的儿童",
      "words": [
        {"text": "大人", "audioAsset": "audio/words/word_daren_v1.mp3"},
        {"text": "家人", "audioAsset": "audio/words/word_jiaren_v1.mp3"},
        {"text": "好人", "audioAsset": "audio/words/word_haoren_v1.mp3"}
      ],
      "sentence": {"text": "这里有一个人。", "audioAsset": "audio/sentences/sentence_ren_v1.mp3"},
      "audio": {
        "character": "audio/characters/char_ren_v1.mp3",
        "meaning": "audio/meanings/meaning_ren_v1.mp3"
      },
      "teachingPrompt": "这是谁呀？这是一个人。大人和小朋友都是人。",
      "confusableRestrictions": ["入"],
      "misconceptions": ["不能让图片暗示只有小朋友才是人"],
      "questionSeeds": [
        {
          "id": "q_ren_listen_char",
          "type": "LISTEN_CHOOSE_CHARACTER",
          "promptAudio": "audio/prompts/prompt_find_ren_v1.mp3",
          "correctOptionId": "text_char_ren",
          "optionIds": ["text_char_ren", "text_char_kou", "text_char_shan"],
          "minLearnedCount": 0,
          "evidenceCategory": "SOUND_TO_SHAPE"
        },
        {
          "id": "q_ren_char_image",
          "type": "CHARACTER_CHOOSE_IMAGE",
          "promptAudio": "audio/prompts/prompt_choose_picture_v1.mp3",
          "correctOptionId": "image_person",
          "optionIds": ["image_person", "image_mouth", "image_mountain"],
          "minLearnedCount": 0,
          "evidenceCategory": "SHAPE_TO_MEANING"
        },
        {
          "id": "q_ren_char_audio",
          "type": "CHARACTER_CHOOSE_AUDIO",
          "promptAudio": "audio/prompts/prompt_choose_sound_v1.mp3",
          "correctOptionId": "audio_char_ren",
          "optionIds": ["audio_char_ren", "audio_char_kou"],
          "minLearnedCount": 0,
          "evidenceCategory": "SHAPE_TO_SOUND"
        },
        {
          "id": "q_ren_shape",
          "type": "SHAPE_RECOGNITION",
          "promptAudio": "audio/prompts/prompt_find_same_v1.mp3",
          "correctOptionId": "text_char_ren",
          "optionIds": ["text_char_ren", "text_char_kou", "text_char_shan"],
          "minLearnedCount": 2,
          "evidenceCategory": "SHAPE"
        }
      ],
      "contentReview": {
        "textReviewed": true,
        "assetReviewedByDeveloper": false,
        "assetReviewedByParent": false,
        "blockedReason": "最终图片和合成音频尚未完成双人复核"
      }
    },
    {
      "id": "char_kou",
      "character": "口",
      "pinyin": "kǒu",
      "toneNumber": 3,
      "order": 2,
      "meaningForChild": "口就是嘴，用来吃东西，也用来说话。",
      "imageAsset": "images/characters/char_kou_main_v1.webp",
      "imageAlt": "儿童轻轻张开的嘴",
      "words": [
        {"text": "开口", "audioAsset": "audio/words/word_kaikou_v1.mp3"},
        {"text": "门口", "audioAsset": "audio/words/word_menkou_v1.mp3"},
        {"text": "一口水", "audioAsset": "audio/words/word_yikoushui_v1.mp3"}
      ],
      "sentence": {"text": "我开口说：你好！", "audioAsset": "audio/sentences/sentence_kou_v1.mp3"},
      "audio": {
        "character": "audio/characters/char_kou_v1.mp3",
        "meaning": "audio/meanings/meaning_kou_v1.mp3"
      },
      "teachingPrompt": "口就是嘴。我们用口吃东西，也用口说话。",
      "confusableRestrictions": ["日", "回", "囗"],
      "misconceptions": ["主要教学义是嘴，不一次讲完所有义项"],
      "questionSeeds": [
        {
          "id": "q_kou_listen_char",
          "type": "LISTEN_CHOOSE_CHARACTER",
          "promptAudio": "audio/prompts/prompt_find_kou_v1.mp3",
          "correctOptionId": "text_char_kou",
          "optionIds": ["text_char_kou", "text_char_ren", "text_char_shan"],
          "minLearnedCount": 0,
          "evidenceCategory": "SOUND_TO_SHAPE"
        },
        {
          "id": "q_kou_char_image",
          "type": "CHARACTER_CHOOSE_IMAGE",
          "promptAudio": "audio/prompts/prompt_choose_picture_v1.mp3",
          "correctOptionId": "image_mouth",
          "optionIds": ["image_mouth", "image_person", "image_mountain"],
          "minLearnedCount": 0,
          "evidenceCategory": "SHAPE_TO_MEANING"
        },
        {
          "id": "q_kou_char_audio",
          "type": "CHARACTER_CHOOSE_AUDIO",
          "promptAudio": "audio/prompts/prompt_choose_sound_v1.mp3",
          "correctOptionId": "audio_char_kou",
          "optionIds": ["audio_char_kou", "audio_char_ren"],
          "minLearnedCount": 0,
          "evidenceCategory": "SHAPE_TO_SOUND"
        },
        {
          "id": "q_kou_shape",
          "type": "SHAPE_RECOGNITION",
          "promptAudio": "audio/prompts/prompt_find_same_v1.mp3",
          "correctOptionId": "text_char_kou",
          "optionIds": ["text_char_kou", "text_char_ren", "text_char_shan"],
          "minLearnedCount": 2,
          "evidenceCategory": "SHAPE"
        }
      ],
      "contentReview": {
        "textReviewed": true,
        "assetReviewedByDeveloper": false,
        "assetReviewedByParent": false,
        "blockedReason": "最终图片和合成音频尚未完成双人复核"
      }
    },
    {
      "id": "char_da",
      "character": "大",
      "pinyin": "dà",
      "toneNumber": 4,
      "order": 3,
      "meaningForChild": "两个同样的东西比一比，占地方更多的那个是大。",
      "imageAsset": "images/characters/char_da_main_v1.webp",
      "imageAlt": "两个同类西瓜中较大的一个",
      "words": [
        {"text": "大人", "audioAsset": "audio/words/word_daren_v1.mp3"},
        {"text": "大山", "audioAsset": "audio/words/word_dashan_v1.mp3"},
        {"text": "大口", "audioAsset": "audio/words/word_dakou_v1.mp3"}
      ],
      "sentence": {"text": "这个西瓜真大。", "audioAsset": "audio/sentences/sentence_da_v1.mp3"},
      "audio": {
        "character": "audio/characters/char_da_v1.mp3",
        "meaning": "audio/meanings/meaning_da_v1.mp3"
      },
      "teachingPrompt": "两个西瓜比一比，这个占的地方更多，它很大。",
      "confusableRestrictions": ["太", "天", "犬"],
      "misconceptions": ["大小必须比较同类物，不把大当作物体名称"],
      "questionSeeds": [
        {
          "id": "q_da_listen_char",
          "type": "LISTEN_CHOOSE_CHARACTER",
          "promptAudio": "audio/prompts/prompt_find_da_v1.mp3",
          "correctOptionId": "text_char_da",
          "optionIds": ["text_char_da", "text_char_xiao", "text_char_ren"],
          "minLearnedCount": 0,
          "evidenceCategory": "SOUND_TO_SHAPE"
        },
        {
          "id": "q_da_char_image",
          "type": "CHARACTER_CHOOSE_IMAGE",
          "promptAudio": "audio/prompts/prompt_choose_picture_v1.mp3",
          "correctOptionId": "image_big_watermelon",
          "optionIds": ["image_big_watermelon", "image_small_watermelon"],
          "minLearnedCount": 0,
          "evidenceCategory": "SHAPE_TO_MEANING"
        },
        {
          "id": "q_da_char_audio",
          "type": "CHARACTER_CHOOSE_AUDIO",
          "promptAudio": "audio/prompts/prompt_choose_sound_v1.mp3",
          "correctOptionId": "audio_char_da",
          "optionIds": ["audio_char_da", "audio_char_xiao"],
          "minLearnedCount": 0,
          "evidenceCategory": "SHAPE_TO_SOUND"
        },
        {
          "id": "q_da_shape",
          "type": "SHAPE_RECOGNITION",
          "promptAudio": "audio/prompts/prompt_find_same_v1.mp3",
          "correctOptionId": "text_char_da",
          "optionIds": ["text_char_da", "text_char_xiao", "text_char_ren"],
          "minLearnedCount": 2,
          "evidenceCategory": "SHAPE"
        },
        {
          "id": "q_da_context",
          "type": "LIFE_WORD_CONTEXT",
          "promptAudio": "audio/prompts/prompt_find_dashan_v1.mp3",
          "correctOptionId": "context_dashan",
          "optionIds": ["context_dashan", "context_xiaoshan"],
          "minLearnedCount": 4,
          "evidenceCategory": "CONTEXT"
        }
      ],
      "contentReview": {
        "textReviewed": true,
        "assetReviewedByDeveloper": false,
        "assetReviewedByParent": false,
        "blockedReason": "最终图片和合成音频尚未完成双人复核"
      }
    },
    {
      "id": "char_xiao",
      "character": "小",
      "pinyin": "xiǎo",
      "toneNumber": 3,
      "order": 4,
      "meaningForChild": "两个同样的东西比一比，占地方更少的那个是小。",
      "imageAsset": "images/characters/char_xiao_main_v1.webp",
      "imageAlt": "两个同类西瓜中较小的一个",
      "words": [
        {"text": "小手", "audioAsset": "audio/words/word_xiaoshou_v1.mp3"},
        {"text": "小山", "audioAsset": "audio/words/word_xiaoshan_v1.mp3"},
        {"text": "小口", "audioAsset": "audio/words/word_xiaokou_v1.mp3"}
      ],
      "sentence": {"text": "这只小猫很小。", "audioAsset": "audio/sentences/sentence_xiao_v1.mp3"},
      "audio": {
        "character": "audio/characters/char_xiao_v1.mp3",
        "meaning": "audio/meanings/meaning_xiao_v1.mp3"
      },
      "teachingPrompt": "两个西瓜比一比，这个占的地方更少，它很小。",
      "confusableRestrictions": ["少"],
      "misconceptions": ["大小对比必须同一视角，不能用远近制造差异"],
      "questionSeeds": [
        {
          "id": "q_xiao_listen_char",
          "type": "LISTEN_CHOOSE_CHARACTER",
          "promptAudio": "audio/prompts/prompt_find_xiao_v1.mp3",
          "correctOptionId": "text_char_xiao",
          "optionIds": ["text_char_xiao", "text_char_da", "text_char_shan"],
          "minLearnedCount": 0,
          "evidenceCategory": "SOUND_TO_SHAPE"
        },
        {
          "id": "q_xiao_char_image",
          "type": "CHARACTER_CHOOSE_IMAGE",
          "promptAudio": "audio/prompts/prompt_choose_picture_v1.mp3",
          "correctOptionId": "image_small_watermelon",
          "optionIds": ["image_small_watermelon", "image_big_watermelon"],
          "minLearnedCount": 0,
          "evidenceCategory": "SHAPE_TO_MEANING"
        },
        {
          "id": "q_xiao_char_audio",
          "type": "CHARACTER_CHOOSE_AUDIO",
          "promptAudio": "audio/prompts/prompt_choose_sound_v1.mp3",
          "correctOptionId": "audio_char_xiao",
          "optionIds": ["audio_char_xiao", "audio_char_da"],
          "minLearnedCount": 0,
          "evidenceCategory": "SHAPE_TO_SOUND"
        },
        {
          "id": "q_xiao_shape",
          "type": "SHAPE_RECOGNITION",
          "promptAudio": "audio/prompts/prompt_find_same_v1.mp3",
          "correctOptionId": "text_char_xiao",
          "optionIds": ["text_char_xiao", "text_char_da", "text_char_shan"],
          "minLearnedCount": 2,
          "evidenceCategory": "SHAPE"
        },
        {
          "id": "q_xiao_context",
          "type": "LIFE_WORD_CONTEXT",
          "promptAudio": "audio/prompts/prompt_find_xiaoshan_v1.mp3",
          "correctOptionId": "context_xiaoshan",
          "optionIds": ["context_xiaoshan", "context_dashan"],
          "minLearnedCount": 4,
          "evidenceCategory": "CONTEXT"
        }
      ],
      "contentReview": {
        "textReviewed": true,
        "assetReviewedByDeveloper": false,
        "assetReviewedByParent": false,
        "blockedReason": "最终图片和合成音频尚未完成双人复核"
      }
    },
    {
      "id": "char_shan",
      "character": "山",
      "pinyin": "shān",
      "toneNumber": 1,
      "order": 5,
      "meaningForChild": "山是地面上高高隆起的地方，常常有石头、树和山坡。",
      "imageAsset": "images/characters/char_shan_main_v1.webp",
      "imageAlt": "轮廓清晰、带山坡和少量树的自然山峰",
      "words": [
        {"text": "大山", "audioAsset": "audio/words/word_dashan_v1.mp3"},
        {"text": "山上", "audioAsset": "audio/words/word_shanshang_v1.mp3"},
        {"text": "爬山", "audioAsset": "audio/words/word_pashan_v1.mp3"}
      ],
      "sentence": {"text": "远处有一座山。", "audioAsset": "audio/sentences/sentence_shan_v1.mp3"},
      "audio": {
        "character": "audio/characters/char_shan_v1.mp3",
        "meaning": "audio/meanings/meaning_shan_v1.mp3"
      },
      "teachingPrompt": "看，远处高高隆起的地方是一座山。",
      "confusableRestrictions": ["出"],
      "misconceptions": ["不能把屋顶、三角形或小土堆都解释成山"],
      "questionSeeds": [
        {
          "id": "q_shan_listen_char",
          "type": "LISTEN_CHOOSE_CHARACTER",
          "promptAudio": "audio/prompts/prompt_find_shan_v1.mp3",
          "correctOptionId": "text_char_shan",
          "optionIds": ["text_char_shan", "text_char_kou", "text_char_ren"],
          "minLearnedCount": 0,
          "evidenceCategory": "SOUND_TO_SHAPE"
        },
        {
          "id": "q_shan_char_image",
          "type": "CHARACTER_CHOOSE_IMAGE",
          "promptAudio": "audio/prompts/prompt_choose_picture_v1.mp3",
          "correctOptionId": "image_mountain",
          "optionIds": ["image_mountain", "image_person", "image_mouth"],
          "minLearnedCount": 0,
          "evidenceCategory": "SHAPE_TO_MEANING"
        },
        {
          "id": "q_shan_char_audio",
          "type": "CHARACTER_CHOOSE_AUDIO",
          "promptAudio": "audio/prompts/prompt_choose_sound_v1.mp3",
          "correctOptionId": "audio_char_shan",
          "optionIds": ["audio_char_shan", "audio_char_kou"],
          "minLearnedCount": 0,
          "evidenceCategory": "SHAPE_TO_SOUND"
        },
        {
          "id": "q_shan_shape",
          "type": "SHAPE_RECOGNITION",
          "promptAudio": "audio/prompts/prompt_find_same_v1.mp3",
          "correctOptionId": "text_char_shan",
          "optionIds": ["text_char_shan", "text_char_kou", "text_char_ren"],
          "minLearnedCount": 2,
          "evidenceCategory": "SHAPE"
        }
      ],
      "contentReview": {
        "textReviewed": true,
        "assetReviewedByDeveloper": false,
        "assetReviewedByParent": false,
        "blockedReason": "最终图片和合成音频尚未完成双人复核"
      }
    }
  ]
}
```

## 5.6 JSON 构建校验

校验分为两个互不循环的阶段。

### 5.6.1 G1 结构校验（T03 完成）

G1 不要求真实图片、音频已经制作，只校验结构和引用格式：

1. 5 个 `id`、`character`、`order` 唯一；
2. `learningOrder` 与 5 个 ID 完全一致；
3. 每个题目 `correctOptionId` 恰好出现在 `optionIds` 一次；
4. 所有 option ID 可解析；
5. 所有资源路径非空，使用相对路径、允许的目录和扩展名，不含 `..`、绝对路径或 URL；
6. 每字首次固定三题 `CHARACTER_CHOOSE_IMAGE`、`LISTEN_CHOOSE_CHARACTER`、`CHARACTER_CHOOSE_AUDIO` 各至少一个；
7. 每字至少有 4 种不同 `questionType`，且映射到至少 4 种独立 `evidenceCategory`；
8. 每字同时存在可用于 D14 的 `LISTEN_CHOOSE_CHARACTER` 与 `CHARACTER_CHOOSE_AUDIO`；
9. 每字固定首次三题种子的 `minLearnedCount` 必须全部为 0；非首次种子必须为 0—5 的整数；
10. 儿童选项中不含该字的 `confusableRestrictions`；
11. `textReviewed=true`，复习间隔严格递增，JSON 反序列化无未知必填字段和空字符串。

### 5.6.2 G2 真实资源校验（T04 完成）

资源生成后再执行：

1. manifest 中每个 `required=true` 的路径真实存在且文件非空；
2. 实际字节数与 `bytes` 一致，SHA-256 与 `sha256` 一致；
3. `content.json` 的全部图片/音频引用都在 manifest 中，manifest 不含未说明的必需孤儿资源；
4. 图片尺寸、格式、答案唯一性和音频格式/时长符合第 7 节；
5. `assetReviewedByDeveloper=true` 且 `assetReviewedByParent=true`；
6. 任一检查失败时 `childTrialEnabled=false`，不得用占位资源绕过。

T03 只负责 G1；T04 负责生成资源并通过 G2。T03 不得以“资源文件尚不存在”为失败理由，T04 也不得反向依赖 G2 已通过。

---

# 六、练习题型规格

## 6.1 共用出题规则

1. 5 字原型只实现以下五种 `QuestionType`，不得临时增加拼音题、书写题或录音题。
2. 新字首次练习 3 题；到期复习每字 2 题且题型不同。
3. 同一 session 中，同一字同一题型最多出现一次；错误后的降难不算新题。
4. 选项顺序用可注入的随机数生成器打乱，创建实例后持久化；正确项不得固定位置。
5. 首次学习可使用尚未学习的另外 5 字作为视觉/声音干扰，但不得解释或发音教学；实际掌握证据只归目标字。
6. 真实形近字“入、日、回、太、天、犬、少、出”在 5 字原型儿童题目中一律禁用。
7. 2 选项用于首次/降难；3 选项用于孩子已有至少 2 个学习中字且近期无连续错误。
8. 每题只有一个正确答案；图片题不得靠选项位置、边框色、尺寸或文件名泄露答案。
9. `learnedCount` 在每次生成题实例前实时查询，等于 `initialLessonCompleted=true` 的不同汉字数；目标字只有完成首次教学后才计入。未学习字可作为配置内干扰项，不计入 `learnedCount`。
10. 种子过滤顺序固定为：目标字种子→`minLearnedCount <= learnedCount`→模式/里程碑所需题型→排除本 session 已用题型→按历史最少使用排序→稳定 ID 排序打破并列。不得先选题型后绕过资格。
11. NEW 模式固定三题在 JSON 中全部 `minLearnedCount=0`，因此从全空数据库即可生成“人”的首次三题；NEW 不忽略该字段。D14 也不忽略资格，但两种所需种子均为 0，保证五字真实可生成。

## 6.2 听音选字 `LISTEN_CHOOSE_CHARACTER`

| 项目 | 规格 |
|---|---|
| 输入 | 目标 `characterId`、目标单字音频、2—3 个字卡 ID |
| 页面 | 顶部大扬声器；进入自动播目标音；下方 2—3 个 88sp 字卡 |
| 指令 | “听一听，请找到‘{发音}’。”；指令中可重复目标音，但屏幕不显示拼音 |
| 正确项 | 与目标 `characterId` 相同的字卡 |
| 干扰项 | 从另外 4 个样板字中选；优先未在上一题出现；不得用形近限制字 |
| 选项数量 | 默认 3；首次学习首题或降难为 2 |
| 重复 | 音频可重播；同 session 不重复同字该题型 |
| 正确反馈 | 高亮字卡，重播单字音，记录 SOUND_TO_SHAPE |
| 错误反馈 | 按共用两级错误处理 |
| 降难 | 3 选 2；若仍错，正确字卡与音频关联展示 |
| 记录 | 题型、选项顺序、目标、每次选择、提示、反应时间、是否独立正确 |

## 6.3 看字选图 `CHARACTER_CHOOSE_IMAGE`

| 项目 | 规格 |
|---|---|
| 输入 | 目标字、目标语义图片、1—2 张干扰图 |
| 页面 | 顶部 112sp 目标字；下方 2—3 张同尺寸图片卡 |
| 指令 | “看看这个字，选一张合适的图。” |
| 正确项 | 人→单人；口→嘴；大→同类中大者；小→同类中小者；山→自然山 |
| 干扰项 | 只使用定义明确的图片 ID；大/小必须是同类同视角 |
| 选项数量 | 人/口/山默认 3；大/小固定 2 |
| 重复 | 同 session 不重复；图片左右位置每实例随机 |
| 正确反馈 | 目标字与正确图用一条柔和线短暂连接，播放字音 |
| 错误反馈 | 不给红叉；重播字义关键句 |
| 降难 | 3 选 2；大/小第二次错直接并排讲解，不减为 1 |
| 记录 | SHAPE_TO_MEANING；提示后正确不算独立证据 |

## 6.4 看字选音 `CHARACTER_CHOOSE_AUDIO`

| 项目 | 规格 |
|---|---|
| 输入 | 目标字、2 个本地单字音频 |
| 页面 | 顶部 112sp 目标字；下方两个无拼音的扬声器卡；底部对勾 |
| 指令 | “点声音听一听，选好以后点对勾。” |
| 正确项 | 音频绑定的 `characterId` 等于目标 |
| 干扰项 | 另外 4 字的单字音；同一声调优先级不作硬要求 |
| 选项数量 | 固定 2 |
| 重复 | 两段声音可任意试听；试听不记作答；提交后按错误规则 |
| 正确反馈 | 选中卡出现柔和声波，重播正确音 |
| 错误反馈 | 第一次错后说“再听一听”；保留两项 |
| 降难 | 第二次错直接把正确音与目标字配对，不出现拼音 |
| 记录 | 仅点对勾提交时写 attempt；记录 SHAPE_TO_SOUND |

## 6.5 字形辨认 `SHAPE_RECOGNITION`

| 项目 | 规格 |
|---|---|
| 输入 | 顶部样本字、2—3 个样板字选项 |
| 页面 | 顶部目标字；指令“找一个和上面一样的字” |
| 正确项 | Unicode 字符和 `characterId` 均与样本相同 |
| 干扰项 | 只用其余 4 个样板字；不得用伪字、残字、旋转字或禁用形近字 |
| 选项数量 | 已学习字少于 2 时不出；默认 3，降难 2 |
| 重复 | 同 session 不重复 |
| 正确反馈 | 上下两个相同字短暂连线 |
| 错误反馈 | 样本字保持；所选项轻移 |
| 降难 | 减少到目标+一个差异明显字 |
| 记录 | SHAPE；不能单独证明知道读音或意义 |

## 6.6 生活词语情境 `LIFE_WORD_CONTEXT`

| 项目 | 规格 |
|---|---|
| 输入 | 场景图、目标词语音频、2 个图词卡 |
| 页面 | 上方场景；下方两张图词卡，词中目标字着主题色 |
| 指令 | 例如“哪一个是大山？”或“图里有一个人，请找到‘人’。” |
| 正确项 | 语音、图片与目标字三者一致的卡 |
| 干扰项 | 必须共享足够背景，避免纯靠无关图片猜；“大山/小山”使用同一山图不同尺寸语义 |
| 选项数量 | 固定 2 |
| 启用条件 | 至少已完成 4 个样板字首次学习；首次新字当天不用于掌握判定 |
| 重复 | 同 session 每字最多一题 |
| 正确反馈 | 播放完整词语，突出其中目标字 |
| 错误反馈 | 重播词语并突出场景关键处 |
| 降难 | 去掉词语文字干扰，先图义配对；该次不计 CONTEXT 独立证据 |
| 记录 | CONTEXT；只有未降难且首次无提示正确才计独立证据 |

## 6.7 题型调度

5 字种子必须满足以下可达性矩阵；“✓”表示可生成并可计独立证据：

| 字 | 看字选图 | 听音选字 | 看字选音 | 字形辨认 | 生活情境 | 首次三题齐全 | D14 两题齐全 | 独立题型总数 |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| 人 | ✓ | ✓ | ✓ | ✓ |  | ✓ | ✓ | 4 |
| 口 | ✓ | ✓ | ✓ | ✓ |  | ✓ | ✓ | 4 |
| 大 | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | 5 |
| 小 | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | 5 |
| 山 | ✓ | ✓ | ✓ | ✓ |  | ✓ | ✓ | 4 |

调度器启动时对矩阵执行运行时防御校验；任一字不满足首次三题、D14 两题或 4 种证据题型时，阻断创建该字任务并记录 `CONTENT_REACHABILITY_FAILED`。

```kotlin
fun chooseQuestionTypes(
    character: CharacterId,
    mode: NEW or REVIEW,
    learnedCount: Int,
    history: EvidenceHistory,
    stage: ReviewStage
): List<QuestionType> {
    val eligibleSeeds = content.seeds(character)
        .filter { it.minLearnedCount <= learnedCount }
        .sortedWith(compareBy({ history.useCount(it.type) }, { it.id }))

    if (mode == NEW) {
        val requiredInitial = listOf(
            CHARACTER_CHOOSE_IMAGE,
            LISTEN_CHOOSE_CHARACTER,
            CHARACTER_CHOOSE_AUDIO
        )
        return requiredInitial
            .filterNot { it in history.completedInitialTypes }
            .map { requiredType -> eligibleSeeds.single { it.type == requiredType }.type }
    }
    if (stage == D14) {
        return listOf(LISTEN_CHOOSE_CHARACTER, CHARACTER_CHOOSE_AUDIO)
            .map { requiredType -> eligibleSeeds.single { it.type == requiredType }.type }
    }
    return pickTwoDifferentTypes(
        seeds = eligibleSeeds,
        prioritizeLeastUsed = true,
        exclude = history.typesUsedInCurrentSession,
        stableTieBreak = QuestionSeed::id
    )
}
```

`single` 找不到或找到多个同型必需种子时必须令 G1/运行时防御失败，不得静默退化为其他题型。`DailyTaskGenerator` 在创建每个 item 前调用此函数，并把当时的 `learnedCount` 与选中 `questionSeedId` 固化进诊断日志，以便 AT-36 验证真实资格。

---

# 七、视觉、声音和资源规格

## 7.1 页面尺寸与适配

| 项目 | 规格 |
|---|---|
| 目标物理屏幕 | 1080×2340，6.62 英寸，竖屏 |
| 设计基准 | 360×780 dp，仅作布局基准；运行时使用 `WindowInsets` |
| 最小验收宽度 | 320dp；低于时仍不得裁掉主按钮和选项 |
| 方向 | 全程竖屏 |
| 安全区 | 顶部刘海、状态栏、底部手势区均不得覆盖交互项 |
| 系统字体缩放 | 1.0 和 1.3 两档测试；儿童核心字卡不得截断 |

## 7.2 字体与字号

- UI 字体：系统 `sans-serif`，目标机对应鸿蒙系统无衬线字体。
- 汉字：同一 `FontFamily.SansSerif`、`FontWeight.Medium`；不使用手写、楷书、书法、空心或变形字。
- 单字学习主字：148sp；练习目标字：112sp；字卡：72—88sp；按钮文字：24sp；家长正文：16sp。
- 拼音不在儿童题目显示；只在家长报告的内容详情中以 16sp 显示。

## 7.3 色彩

| 用途 | 色值 | 规则 |
|---|---|---|
| 背景 | `#FFF8EC` | 暖白，不纯白刺眼 |
| 主色 | `#2F7772` | 主按钮、目标字强调 |
| 主文字 | `#243238` | 汉字和正文 |
| 辅助黄 | `#F4B942` | 少量进度与装饰 |
| 正确 | `#4A9D69` | 正确边框，不满屏绿 |
| 温和提醒 | `#E49A35` | 错误支持，不用鲜红大叉 |
| 卡片 | `#FFFFFF` | 与背景有边界 |

文字对比度按普通文本至少 4.5:1；大字至少 3:1。颜色不能作为唯一正误信息，必须配动画/语音/图标。

## 7.4 触控、图标、动画

- 儿童主按钮最小 88dp 高；答案卡最小 112×96dp；间距 ≥16dp。
- 成人控件不得低于 Android 建议的 48×48dp；本项目儿童控件使用更大的 64dp 下限。
- 图标统一圆角线性风格；扬声器、返回、暂停、对勾含义固定。
- 正确动画 ≤600ms；页面过渡 240ms；错误轻移 ≤220ms；不循环超过 2 秒。
- 禁止：闪屏、屏幕震动、爆炸、倒计时压迫、突然巨响、哭脸、红色大叉、失败音效、无限彩纸。

## 7.5 插画

| 项目 | 规格 |
|---|---|
| 格式 | WebP；需要透明背景时使用 lossless WebP |
| 源尺寸 | 1024×1024；APK 可按质量 80—85 压缩 |
| 画面 | 扁平、温和、轮廓清楚、主体唯一 |
| 文件名 | `img_{semantic_id}_{variant}_v{n}.webp` |
| 禁止 | 图片内文字、水印、商标、复杂背景、答案颜色暗示、儿童真实照片 |
| 无障碍描述 | 每图配置 `imageAlt`，儿童界面不朗读长描述 |

必需语义图片 ID：

```text
image_person
image_mouth
image_mountain
image_big_watermelon
image_small_watermelon
context_dashan
context_xiaoshan
```

## 7.6 音频

| 项目 | 规格 |
|---|---|
| 来源 | 合成普通话女声，开发期预生成 |
| 运行 | 本地文件播放；禁止联网 TTS |
| 格式 | MP3，44.1kHz，单声道，CBR 96kbps |
| 响度 | 全部文件标准化到约 -16 LUFS；峰值不超过 -1dBFS |
| 语速 | 单字自然清晰；句子约正常语速 0.9 倍 |
| 静音 | 头尾静音各 ≤100ms |
| 时长 | 单字 0.5—1.5s；词语 ≤2s；句子/指令 ≤4s |
| 文件名 | `char_{pinyin_no_tone}_v1.mp3`、`word_{id}_v1.mp3`、`prompt_{id}_v1.mp3` |
| 并发 | 新音频开始前停止旧音频；不叠音 |

不得合成“rén（二声）”这种带拼音术语的儿童音频。单字文件只读目标字；教学说明另文件。

必需题目指令音频：

| 文件 | 精确文本 |
|---|---|
| `prompt_find_ren_v1.mp3` | “请找到，人。” |
| `prompt_find_kou_v1.mp3` | “请找到，口。” |
| `prompt_find_da_v1.mp3` | “请找到，大。” |
| `prompt_find_xiao_v1.mp3` | “请找到，小。” |
| `prompt_find_shan_v1.mp3` | “请找到，山。” |
| `prompt_choose_picture_v1.mp3` | “看看这个字，选一张合适的图。” |
| `prompt_choose_sound_v1.mp3` | “点声音听一听，选好以后点对勾。” |
| `prompt_find_same_v1.mp3` | “找一个和上面一样的字。” |
| `prompt_find_dashan_v1.mp3` | “哪一个是大山？” |
| `prompt_find_xiaoshan_v1.mp3` | “哪一个是小山？” |

“大”“小”的看字选图均必须引用 `prompt_choose_picture_v1.mp3`。不得制作或引用“请选择大的/小的”作为该题指令；这类语音会泄露目标字含义，若出现则该题不能计 `SHAPE_TO_MEANING` 证据并阻断 G2。

## 7.7 资源清单与缺失处理

`assets/content/v1/manifest.json` 为每个文件记录：

```json
{
  "path": "audio/characters/char_ren_v1.mp3",
  "sha256": "<64位小写十六进制>",
  "bytes": 12345,
  "required": true
}
```

- 启动时校验必需资源存在和大小；开发测试构建可校验 SHA-256。
- 单字音频、主图、题目正确项资源缺失属于 P0：阻断对应任务和儿童试用。
- 装饰图缺失属于 P2：使用纯色占位，不影响题目。
- 资源异常写本地错误码，不尝试联网下载。

---

# 八、代码工程建议

## 8.1 推荐技术方案

**原生 Android：Kotlin + Jetpack Compose + Room + Preferences DataStore。**

选择理由：

1. 交付目标就是 Android APK，不需要跨平台层；
2. 鸿蒙 4.2 仍属于可运行 Android APK 的经典鸿蒙代际，但必须先过 G0 真机冒烟；
3. Compose 足以快速实现五类页面和统一状态 UI；
4. Room 提供 SQLite 事务、查询和编译期检查，适合每题可靠落库；
5. DataStore 只保存少量设置；
6. 内容与资源打包在 APK，可完全离线；
7. 从 5 字扩到 300 字只扩 JSON 和资源，不改页面架构。

不选择 PWA、Flutter、React Native 的原因不是它们不能实现，而是本项目只有一台 Android 兼容设备，引入浏览器容器或跨平台运行时没有实际收益。

## 8.2 工具链基线

为避免“追最新版”造成不可复现，开发第一提交必须生成并提交 Gradle Wrapper 和版本目录；以 G0 能在目标机运行的锁定组合为准：

- JDK 17；
- Gradle Wrapper 8.9；
- Android Gradle Plugin 8.7.3；
- Kotlin 2.0.21；
- compileSdk 35、targetSdk 35、minSdk 23；
- Compose BOM `2024.12.01`；
- Room `2.8.4`；
- DataStore `1.2.1`；
- JUnit 4、AndroidX Test、Compose UI Test。

如果某版本在实际构建环境不可解析，只允许做一次“依赖兼容调整提交”，并在 README 记录旧值、新值、原因和 G0 结果；不得在业务开发中反复浮动依赖。

## 8.3 Android 清单硬规则

```xml
<application
    android:allowBackup="false"
    android:fullBackupContent="false"
    android:usesCleartextTraffic="false"
    ... />
```

- 不声明 `INTERNET`、`RECORD_AUDIO`、`CAMERA`、`ACCESS_*_LOCATION`、`READ_CONTACTS`。
- Android 12 及部分厂商设备可能仍允许设备到设备迁移，故家长说明应写“APP 不主动上传，并已关闭系统云备份能力；设备厂商的迁移行为需以目标机实测为准”，不能绝对承诺任何系统层行为。
- 诊断导出使用 Storage Access Framework，不申请广泛存储权限；本版本没有学习记录导入入口。

## 8.4 项目目录

```text
shizi-app/
├── README.md
├── settings.gradle.kts
├── build.gradle.kts
├── gradle/
│   └── libs.versions.toml
├── app/
│   ├── build.gradle.kts
│   ├── schemas/                         # Room 导出 schema
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/family/shizi/
│       │   │   ├── ShiziApplication.kt
│       │   │   ├── MainActivity.kt
│       │   │   ├── navigation/
│       │   │   ├── ui/
│       │   │   │   ├── home/
│       │   │   │   ├── learn/
│       │   │   │   ├── practice/
│       │   │   │   ├── result/
│       │   │   │   ├── parent/
│       │   │   │   ├── components/
│       │   │   │   └── theme/
│       │   │   ├── domain/
│       │   │   │   ├── model/
│       │   │   │   ├── engine/
│       │   │   │   │   ├── ReviewScheduler.kt
│       │   │   │   │   ├── MasteryStateEngine.kt
│       │   │   │   │   └── DailyTaskGenerator.kt
│       │   │   │   └── usecase/
│       │   │   ├── data/
│       │   │   │   ├── content/
│       │   │   │   ├── db/
│       │   │   │   ├── repository/
│       │   │   │   ├── settings/
│       │   │   │   └── diagnostics/
│       │   │   └── audio/
│       │   └── assets/content/v1/
│       │       ├── content.json
│       │       ├── content.schema.json
│       │       ├── manifest.json
│       │       ├── audio/
│       │       └── images/
│       ├── test/
│       │   ├── content/
│       │   ├── engine/
│       │   ├── repository/
│       │   └── diagnostics/
│       └── androidTest/
│           ├── flow/
│           ├── ui/
│           ├── persistence/
│           └── targetdevice/
└── docs/
    ├── TEST_REPORT.md
    ├── CONTENT_REVIEW.md
    └── DEVICE_CHECK.md
```

## 8.5 页面与组件拆分

| 页面 | ViewModel | 关键组件 |
|---|---|---|
| 儿童首页 | `HomeViewModel` | `TodayTaskCard`、`PrimaryChildButton`、`ParentGate` |
| 单字学习 | `LearnViewModel` | `CharacterHero`、`MeaningCard`、`WordAudioCard` |
| 练习 | `PracticeViewModel` | `CharacterOptionCard`、`ImageOptionCard`、`AudioOptionCard`、`FeedbackOverlay` |
| 结果 | `ResultViewModel` | `CompletedCharacterRow`、`GentleSummary` |
| 家长 | `ParentViewModel` | `ReportTab`、`ErrorTab`、`OralCheckTab`、`SettingsTab` |

Composable 只渲染 `UiState` 和发出 `UiAction`；不得直接读写 Room/DataStore。

## 8.6 状态管理与事务

- 每个页面一个 `ViewModel + StateFlow<UiState>`。
- `Repository` 是内容、Room、DataStore 的唯一协调层。
- `SubmitAnswerUseCase` 在一个 Room `withTransaction` 内完成：插入 attempt → 更新 question → 更新 session → 重算字进度。
- 日期和随机数通过 `Clock`、`ZoneIdProvider`、`RandomProvider` 注入，保证测试可重复。
- 状态均由事件派生；UI 不直接把字设为“稳定掌握”。

## 8.7 复习任务模块接口

```kotlin
interface ReviewScheduler {
    fun nextDate(
        firstLearnDate: LocalDate,
        currentStage: ReviewStage,
        completedOn: LocalDate,
        milestonePassed: Boolean
    ): ReviewDecision
}

interface DailyTaskGenerator {
    suspend fun getOrCreate(date: LocalDate): LearningSession
}

interface MasteryStateEngine {
    suspend fun recalculate(characterId: String, today: LocalDate): CharacterProgress
}
```

`DailyTaskGenerator.getOrCreate` 是正式任务入口，必须在单事务中执行：

```text
若同日已有 CREATED/ACTIVE/PAUSED → 原样返回
→ 封账所有更早日期未完成 session
→ 查询并排序最多 3 个到期 REVIEW
→ 查询历史未完成 NEW 接续项
→ 计算当日剩余新字额度并按 learningOrder 选全新字
→ 固化 item 顺序：REVIEW、续学 NEW、全新 NEW
→ 对每个 item 计算 learnedCount
→ 按 6.7 资格过滤和题型调度生成 question_instance
→ 一次事务保存 session/items/questions
```

正式查询不得接受“预置已学字数”“目标字状态”或“强制题型”测试参数。测试可注入的只有 `Clock`、`ZoneIdProvider`、`RandomProvider` 和空/持久化 Repository；AT-36 不得调用 DAO 绕过此接口。

## 8.8 诊断导出与清空边界

5 字原型删除备份导入恢复。`diagnostics.json` 只允许包含：

- `diagnosticVersion`、`exportedAt`、应用版本、内容版本、设备型号/系统版本；
- 非身份设置摘要（每日新字数、课程时长，不导出昵称）；
- 各表记录数量、最近成功保存时间；
- 最近 50 条不含身份信息的错误码和发生时间。

诊断文件不得包含完整 `practice_attempt`、`oral_check` 或可用于恢复状态的逐条学习历史，也没有导入入口。清空操作在成人门槛和二次确认后，以单事务删除学习表并恢复设置默认值；清空失败则回滚，不能得到半清空数据库。

## 8.9 启动、构建、安装和检查命令

```bash
./gradlew --version
./gradlew clean testDebugUnitTest
./gradlew lintDebug
./gradlew connectedDebugAndroidTest
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am force-stop com.family.shizi
adb shell monkey -p com.family.shizi 1
adb shell pm list permissions -g | true
sha256sum app/build/outputs/apk/debug/app-debug.apk
```

权限检查使用：

```bash
aapt dump permissions app/build/outputs/apk/debug/app-debug.apk
```

预期不得出现网络、录音、相机、位置、通讯录权限。目标设备未连接时，`connectedDebugAndroidTest` 可在模拟器执行，但 G0/G4/G5 仍必须真机。

原型交付文件名：

```text
shizi-5char-prototype-v1.1-debug.apk
shizi-5char-prototype-v1.1-source.zip
TEST_REPORT.md
CONTENT_REVIEW.md
DEVICE_CHECK.md
SHA256SUMS.txt
```

---

# 九、Codex 开发任务拆分

Codex 必须按顺序工作。一个任务未达到完成标准，不得把后续界面截图当作该任务已完成。

## T01 目标机兼容冒烟

- **目标**：验证 Android APK 路线在 Mate 30 / 鸿蒙 4.2 可行。
- **涉及文件/模块**：最小 `MainActivity`、一段本地 MP3、最小 Room 表、`DEVICE_CHECK.md`。
- **输入**：目标设备、工具链基线。
- **输出**：可安装空壳 APK；启动/音频/数据库结果。
- **完成标准**：安装、冷启动、播放 MP3、写入并重启读回字符串全部成功；竖屏正常。
- **测试方法**：真机执行 G0；截取系统版本、APK 版本和结果，不截儿童个人信息。
- **依赖**：无；全项目第一门。

## T02 工程骨架与隐私清单

- **目标**：建立可复现工程、依赖锁、五页面导航空状态和无敏感权限清单。
- **涉及文件/模块**：Gradle、Manifest、Application、navigation、theme。
- **输入**：8.2、8.3。
- **输出**：可构建工程；五路由空页面；权限检查报告。
- **完成标准**：`clean test lint assembleDebug` 通过；无 INTERNET/录音/相机/位置权限；`allowBackup=false`。
- **测试方法**：执行 8.9 命令和 AT-28。
- **依赖**：T01。

## T03 内容模型、JSON 与构建校验

- **目标**：将本文件 5 字 JSON 落地为可解析内容包，只完成 G1 结构校验。
- **涉及文件/模块**：`assets/content/v1`、`ContentModels.kt`、`ContentLoader.kt`、schema、校验测试。
- **输入**：第 1、5、6 节及资源路径格式规则。
- **输出**：`content.json`、`content.schema.json`、校验器。
- **完成标准**：5.6.1 十一项全通过；每字首次三题、D14 两题及至少 4 种证据题型齐全；首次三题 `minLearnedCount=0`；内容 UI 不含硬编码字表；真实资源尚未生成不导致 G1 失败。
- **测试方法**：删除一个正确项、重复一个 ID、删掉某字“看字选音”、把某字缩减为 3 题型，结构测试均必须失败；仅资源文件不存在时结构测试仍通过。
- **依赖**：T02。

## T04 资源制作与双人审核门

- **目标**：制作 5 字插画、合成女声音频和完整 manifest。
- **涉及文件/模块**：`audio/`、`images/`、`manifest.json`、`CONTENT_REVIEW.md`。
- **输入**：1.2—1.7、7.5—7.7。
- **输出**：全部必需资源、SHA-256、双人核对表。
- **完成标准**：5.6.2 全通过；开发者与家长逐字全勾选；任一未勾选时 `childTrialEnabled=false`。
- **测试方法**：逐文件试听/查看；删除文件、篡改字节、伪造 SHA-256 均必须使 G2 失败；运行 AT-22。
- **依赖**：T03。

## T05 Room、DataStore 与 Repository

- **目标**：实现所有本地表、DAO、设置和事务边界。
- **涉及文件/模块**：`data/db`、`data/settings`、`data/repository`、Room schemas。
- **输入**：第 5 节。
- **输出**：数据库 v1、DAO、Repository、迁移基线、测试。
- **完成标准**：题目提交单事务；重启后读回；约束生效；设置默认值正确；有效时长、分段起点、`pauseReason`、待结束原因、终态原因、到期 item 完成时间及四个资格/回退时间均可持久化，且原因字段互斥约束生效。
- **测试方法**：内存 Room 单测、真机持久化 AT-15、故障注入 AT-19、计时持久化 AT-37。
- **依赖**：T02；可与 T04 先后独立，但合并前均完成。

## T06 状态与复习引擎

- **目标**：纯逻辑实现五状态、里程碑、回退、延迟与口头双证据。
- **涉及文件/模块**：`ReviewScheduler.kt`、`MasteryStateEngine.kt`、`DailyTaskGenerator.kt`。
- **输入**：第 4 节。
- **输出**：纯 Kotlin 引擎和参数化测试。
- **完成标准**：1/3/7/14、迟做、同日重复、提示后正确、暂时一次失败回退、稳定单日失败保持、稳定两日失败回退、回退后重新获得暂时/稳定、日期倒退均有测试；从同一个全空数据库按人→口→大→小→山全部可达稳定掌握；P0 不直接修改状态或派生证据字段。
- **测试方法**：固定 Clock/Zone/Random 运行 AT-12—AT-20、AT-36、AT-40—AT-43；对测试源码执行静态检查，禁止直接调用 `character_progress` DAO 写方法、直接插入任务或为状态/派生证据赋值。
- **依赖**：T05 的接口；逻辑可先用 fake repository。

## T07 儿童首页与首次设置

- **目标**：实现首次家长设置、首页四态、任务创建和成人门槛。
- **涉及文件/模块**：home、parent gate、onboarding、HomeViewModel。
- **输入**：2.1、2.5、3.1、3.2。
- **输出**：可生成首日 session 的首页。
- **完成标准**：昵称可空；默认 1 新字/10 分钟/80%；同日不重复建 session。
- **测试方法**：Compose UI 测试 AT-01、AT-02、AT-27。
- **依赖**：T05、T06。

## T08 单字学习页

- **目标**：实现 A/B/C 三步、音频控制、初次学习状态和恢复。
- **涉及文件/模块**：learn、audio、LearnViewModel。
- **输入**：2.2、3.3。
- **输出**：5 字均由配置渲染。
- **完成标准**：音频未播完不能前进；完成三步加三题后进入正在复习；中断恢复。
- **测试方法**：AT-03、AT-09、AT-23。
- **依赖**：T03—T07。

## T09 五种练习组件

- **目标**：实现五题型、随机固化、看字选音确认式交互。
- **涉及文件/模块**：practice/components、QuestionRenderer、PracticeViewModel。
- **输入**：第 6 节。
- **输出**：五题型统一事件模型。
- **完成标准**：无拼音；正确项位置随机但恢复不变；每题记录完整。
- **测试方法**：AT-04—AT-08、AT-11。
- **依赖**：T03、T05、T08。

## T10 错误支持、降难、暂停与提前结束

- **目标**：实现首次错、二次错、直接教学、有效时长、主动暂停、疲劳/时间上限提前结束。
- **涉及文件/模块**：PracticeViewModel、LearnViewModel、FeedbackOverlay、SessionTimer、session use case。
- **输入**：2.3、3.5、3.6、3.7。
- **输出**：可测试的三级反馈、计时和 session 状态转换。
- **完成标准**：不使用批评性红叉；提示后正确不算独立；主动休息只进入 `PAUSED/USER_REST` 且当天可继续；恢复时清空 `pauseReason`；疲劳或时限完成当前题/步骤后进入 `ENDED_EARLY/FATIGUE` 或 `ENDED_EARLY/TIME_LIMIT`；当天不能继续，次日按规则接续。
- **测试方法**：AT-06、AT-07、AT-10、AT-37—AT-39。
- **依赖**：T09。

## T11 结果页和完整课程提交

- **目标**：实现正常/提前/只复习/提交失败结果态。
- **涉及文件/模块**：result、SubmitSessionUseCase。
- **输入**：2.4、3.8。
- **输出**：明确结束的完整日课。
- **完成标准**：提交成功前不显示完成；当天不能无限继续；字卡点击只播放音频。
- **测试方法**：AT-08、AT-19。
- **依赖**：T06、T10。

## T12 家长报告、易错字与口头抽检

- **目标**：实现四标签中的前三个及口头结果修订。
- **涉及文件/模块**：parent/report、errors、oral。
- **输入**：2.5、3.10、3.11、4.5。
- **输出**：逐字可解释报告、抽检队列、不可变历史。
- **完成标准**：APP 延迟与口头结果分开；修改新增记录；双通过才稳定。
- **测试方法**：AT-16—AT-18、AT-20。
- **依赖**：T05、T06、T11。

## T13 设置、诊断导出与清空

- **目标**：完成基本设置、最小诊断导出和不可逆清空；不实现备份导入恢复。
- **涉及文件/模块**：parent/settings、diagnostics。
- **输入**：2.5、5.4、8.8。
- **输出**：诊断 JSON、清空事务和二次确认。
- **完成标准**：不存在导入入口；诊断文件不含逐条学习历史和昵称；清空成功后回首次设置，失败时完整回滚。
- **测试方法**：AT-21、AT-27、AT-35。
- **依赖**：T05、T12。

## T14 异常、资源缺失和日期异常

- **目标**：让失败可见、可恢复、不伪造成功。
- **涉及文件/模块**：error handler、resource validator、ClockRollbackDetector。
- **输入**：2.0、3.12、4.4、7.7。
- **输出**：错误码、家长处理态、最近保存时间。
- **完成标准**：P0 资源缺失阻断；数据库失败不前进；日期倒退不重复日课。
- **测试方法**：AT-19、AT-22、AT-24。
- **依赖**：T03、T04、T05、T13。

## T15 自动化与目标机验收

- **目标**：跑完单测、UI、持久化、无网、误触、分辨率验收。
- **涉及文件/模块**：全部 test/androidTest、`TEST_REPORT.md`、`DEVICE_CHECK.md`。
- **输入**：第 10 节。
- **输出**：逐条有证据的验收报告。
- **完成标准**：P0/P1 全通过；AT-36 单一全空数据库中的 5 字轨迹全部通过；无“未测但通过”。
- **测试方法**：8.9 全命令、真机飞行模式、字体 1.3、进程杀死恢复。
- **依赖**：T01—T14。

## T16 APK 打包与交付

- **目标**：形成可安装交付包，不扩展 30 字。
- **涉及文件/模块**：版本号、APK、源码压缩包、报告、校验和。
- **输入**：T15 通过结果。
- **输出**：8.9 所列六个文件。
- **完成标准**：干净环境可构建；APK 在目标机重新安装通过；SHA-256 匹配。
- **测试方法**：先用 `adb install -r` 覆盖安装并确认记录保留；如执行卸载后的全新安装，应明确本版本无恢复能力且经家长同意数据丢失，再跑首次使用最短流程；核对清单。
- **依赖**：T15。

---

# 十、验收测试

## 10.1 结果记录规则

- `通过`只能填“是/否”，未执行填“未测”，不得填“基本通过”。
- P0：阻断儿童试用；P1：阻断原型验收；P2：可记录后修但不能影响核心闭环。
- 每次执行记录：构建号、设备型号、系统版本、日期、执行人。

## 10.2 页面与流程测试

| ID/级别 | 前置条件 | 操作步骤 | 预期结果 | 是否通过 |
|---|---|---|---|---|
| AT-01/P0 首次打开 | 全新安装、无数据 | 启动→完成成人算式→昵称留空→保存 | 进入儿童首页；问候“你好呀”；设置默认 1/10/80 正确；无注册 | □ |
| AT-02/P0 首页任务 | AT-01 完成、首日 | 点“开始” | 只创建一个日课和“人”新字；事务成功后进入单字页 | □ |
| AT-03/P0 单字页 | “人”为未学习 | 依次完成 A/B/C，重播音频 | 字、图、音、义、词句正确；音频未结束前不能前进；状态变初次学习 | □ |
| AT-04/P0 听音选字 | 已进入该题 | 重播音频→点正确字 | 保存首次无提示正确；正确反馈后进入下一题 | □ |
| AT-05/P0 看字选音 | 已进入该题 | 分别试听两项→选择正确→点对勾 | 试听不判分；提交后只记一次；儿童界面无拼音 | □ |
| AT-06/P0 一次错误 | 题目 3 选项 | 首次点错→第二次点对 | 首次错误有温和反馈；第二次正确标记提示后正确，不计独立正确 | □ |
| AT-07/P0 连错两次 | 题目 3 选项 | 连续两次选错 | 降为 2 项/直接教学；无第三次强迫作答；次日复习 | □ |
| AT-08/P0 正常完成 | 完成计划全部题 | 进入结果→回首页 | 结果页显示本节字；session=COMPLETED；首页“今天完成啦”；不能再生成新字 | □ |
| AT-09/P0 中途退出 | 单字页 B 或未提交题 | 点返回→先休息→杀进程→重启→继续 | session=PAUSED；首页“继续”；恢复相同步骤/题目、选项顺序和累计时长；无虚假作答 | □ |
| AT-10/P0 疲劳结束 | 一节内三个不同题目实例发生首次错误 | 完成并保存第三个错误题 | 第三题保存前只置 `endPendingReason=FATIGUE`；保存后不再新增题，session=ENDED_EARLY/FATIGUE；首页“今天先到这里”；当天禁止继续；已完成记录保留 | □ |
| AT-11/P1 字形题限制 | 已学至少 2 字 | 多次生成字形题 | 只出现 5 个样板字；不出现入/日/回/太/天/犬/少/出或伪字 | □ |

## 10.3 状态与复习测试

| ID/级别 | 前置条件 | 操作步骤 | 预期结果 | 是否通过 |
|---|---|---|---|---|
| AT-12/P0 首学状态 | “人”未学习 | 开始教学→中断→完成教学和三题 | 状态依次未学习→初次学习→正在复习；nextReview=次日 | □ |
| AT-13/P0 同日不跨日 | 同日已有多次独立正确 | 同日再完成 10 次不同题 | `validDateCount` 仍只增加 1；不能因此暂时掌握 | □ |
| AT-14/P0 D1/D3/D7 | 注入固定 Clock | 各里程碑两题全独立正确 | 下一日期按 max 规则推进；状态满足硬条件时才暂时掌握 | □ |
| AT-15/P0 到期生成与优先级 | 某字 `nextReviewDate=today`，同时有历史未完成新字和可开全新字 | 冷启动→首页→开始→检查 session item 顺序 | 先到期复习，再历史未完成新字接续，最后全新字；到期字两种不同题型；实例只生成一次；未完成新字占新字额度 | □ |
| AT-16/P0 D14 分离 | 首学满 14 天且其他条件满足 | APP 两题通过，不做口头抽检 | APP=PASS、口头=NOT_TESTED；状态最多暂时掌握，不得稳定 | □ |
| AT-17/P0 双证据稳定 | AT-16 后 | 家长记录独立读出 | 口头=INDEPENDENT_PASS；满足全部硬条件后稳定掌握 | □ |
| AT-18/P0 修改回退 | 稳定掌握 | 把本次口头结果修改为“未读出” | 新增修订记录、不覆盖历史；状态立即回正在复习；次日到期 | □ |
| AT-19/P0 保存失败 | 注入 DAO 写失败 | 提交答案，连续重试 3 次 | 不跳题、不显示完成、不新增 attempt；进入家长处理态 | □ |
| AT-20/P1 早期口头 | 首学第 7 天 | 家长记录独立读出 | 历史保留但 `eligibleForStable=false`；第 14 天后仍待抽检 | □ |

## 10.4 数据、资源、设备与隐私测试

| ID/级别 | 前置条件 | 操作步骤 | 预期结果 | 是否通过 |
|---|---|---|---|---|
| AT-21/P0 清空原子性 | 至少 3 字有记录 | 进入家长设置→成人门槛→二次确认→清空；另注入一次清空事务失败 | 成功时学习表清空并回首次设置，明确不可恢复；失败时全部原记录保持；APP 不存在导入/恢复入口 | □ |
| AT-22/P0 资源完整性 | 测试构建 | 删除“山”单字音频再启动 | 构建校验或启动校验失败；不得让孩子进入“山”任务 | □ |
| AT-23/P0 音频离线 | 飞行模式、清应用网络缓存 | 完整学习一字、重播所有音频 | 全部本地播放，无网络提示、无 TTS 下载 | □ |
| AT-24/P1 日期回拨 | 已创建今日 session | 把系统日期倒退 2 天→启动 | 不重复建课、不删记录；家长页提示核对日期 | □ |
| AT-25/P0 目标机运行 | Mate 30/鸿蒙4.2 | 安装→冷启→学习→复习→报告 | 无崩溃、白屏、音频异常；数据库重启可读 | □ |
| AT-26/P0 无网络全流程 | 飞行模式、全新安装 | 从首次设置走到口头抽检 | 除 14 天需测试时钟外，所有功能可用；无联网权限 | □ |
| AT-27/P1 成人门槛 | 儿童首页 | 短点锁、长按不足 3 秒、答错算式、答对 | 前三者不进入家长页；答对进入；不建立账号 | □ |
| AT-28/P0 权限 | 已构建 APK | 运行 `aapt dump permissions` | 无 INTERNET、录音、相机、位置、通讯录、广泛存储权限 | □ |
| AT-29/P0 云备份配置 | 已构建 APK | 检查合并 Manifest | `allowBackup=false`、`fullBackupContent=false`；家长说明不作绝对系统承诺 | □ |
| AT-30/P1 分辨率 | 真机默认显示大小 | 检查五类页面 | 1080×2340 无裁切、重叠、刘海遮挡、底部误触 | □ |
| AT-31/P1 字体缩放 | 系统字体 1.3 | 检查五类页面 | 主字、按钮、选项不截断；家长页可滚动 | □ |
| AT-32/P1 多点误触 | 练习页 | 两指同时点不同选项、300ms 内乱点 | 标记疑似误触，不形成掌握证据；题目可继续 | □ |
| AT-33/P1 后台恢复 | 题目中 | 切后台 5 分钟→系统回收→重进 | 回到同题或下一已提交点；不重复记分 | □ |
| AT-34/P0 内容结构与资源分门 | 先无真实资源、后放入完整资源 | T03 运行 G1→T04 放入资源并运行 G2→分别删除文件和篡改 SHA | 无资源时 G1 可通过；每字首次三题及 `minLearnedCount=0`、D14 两题、≥4题型；资源完成后 G2 通过；缺文件/哈希不符只使 G2 失败并阻断儿童试用 | □ |
| AT-35/P1 诊断导出边界 | 至少 3 字有记录且有错误日志 | 导出 diagnostics.json→检查字段→尝试在 APP 中寻找导入入口 | 文件仅含版本、非身份设置摘要、数量、最近保存和错误码；不含昵称及逐条 attempt/oral；APP 无导入入口 | □ |
| AT-36/P0 五字全空库稳定可达 | 一个全新空 Room 内存库；真实 5 字 JSON；正式 `DailyTaskGenerator`、题型调度器、答题/状态/口头用例；固定 Clock/Zone/Random | 按 4.8 从 D0 运行；每天只调用正式 `getOrCreate`；按“到期复习→未完成新字→全新字”完成任务，真实首教人→口→大→小→山；推进各字 D1/D3/D7/D14并完成第4题型、APP延迟识别和家长独立认读；逐步扫描禁止直写 | 初始学习表全空；“人”首次三题在 `learnedCount=0` 下生成；五字所有种子资格真实满足；五字逐字输出完整状态轨迹并最终全部 `STABLE_MASTERED`；测试未直接写状态/任务/派生证据；任一步失败立即停止整项 | □ |
| AT-37/P0 暂停计时持久化 | session ACTIVE，注入 Clock | 学习 60 秒→先休息 120 秒→杀进程→重启→继续 30 秒 | 暂停时为 `PAUSED/USER_REST` 且 `completedAt/earlyEndReason` 均为空；后台/暂停 120 秒不累计；恢复为 ACTIVE 时 `pauseReason` 清空；`activeElapsedMs` 约 90 秒且误差≤5秒；当天仍可继续 | □ |
| AT-38/P0 时间上限结束 | 时长上限 8 分钟，当前题进行中 | 把有效时长推进到 7:59→开始当前题→跨过 8:00→完成并保存当前题 | 当前题不中断；不打开下一题；session=ENDED_EARLY/TIME_LIMIT；首页“今天先到这里”；当天禁止继续 | □ |
| AT-39/P0 次日接续与唯一优先级 | 前一日分别为 PAUSED/USER_REST、ENDED_EARLY/FATIGUE、ENDED_EARLY/TIME_LIMIT，且同时存在到期复习、未完成新字和可开全新字 | 次日启动→开始日课 | 旧 PAUSED/USER_REST 先封账为 ENDED_EARLY/DAY_ROLLOVER 并保留原暂停原因作审计；已完成记录保留；新 session 严格为“到期复习→未完成新字接续→全新字”；未完成新字从保存步骤/剩余首次题接续且占新字额度；旧复习题不复用未提交实例 | □ |

## 10.5 状态回退闭环测试

| ID/级别 | 前置条件 | 操作步骤 | 预期结果 | 是否通过 |
|---|---|---|---|---|
| AT-40/P0 暂时掌握后一次到期失败 | 某字已 `TEMP_MASTERED`，`temporaryQualifiedAt` 非空 | 由正式调度生成一条到期 REVIEW，并在其中一题首次答错后完成 item | `dueCheckPassed=false`；写 `temporaryRollbackAt`；状态回 `REVIEWING`；次日到期；历史资格不删除 | □ |
| AT-41/P0 稳定掌握单日一次失败不回退 | 某字已 `STABLE_MASTERED`，记录 `stableQualifiedAt` | 某日正式到期检查 FAIL；同日可重复失败但不得跨日期 | 失败日期集合大小为1；`stableRollbackAt=null`；仍为 `STABLE_MASTERED`；次日复查 | □ |
| AT-42/P0 稳定掌握两个不同日期失败后回退 | 延续 AT-41，次日由正式调度再次生成到期检查 | 第二个不同 `completedLocalDate` 的到期检查 FAIL | 第二次失败事务写 `stableRollbackAt` 和 `temporaryRollbackAt`；状态回 `REVIEWING`；同日多个失败不冒充两个日期 | □ |
| AT-43/P0 回退后重新获得暂时/稳定 | 延续 AT-42，保留全部历史 | 后续正式到期检查 PASS→检查状态；再完成 `stableRollbackAt` 后的D14规格 APP PASS和新的家长独立认读 | 到期 PASS 清除暂时回退并在阈值仍满足时恢复 `TEMP_MASTERED`，但不直接稳定；两项新的稳定证据均晚于回退后，更新 `stableQualifiedAt`、清除 `stableRollbackAt`，恢复 `STABLE_MASTERED` | □ |

## 10.6 儿童误触与可用性观察

在 G0—G4 全通过后，进行 3—7 天观察。家长只观察，不在孩子即将操作时提示。

| 观察项 | 通过线 | 记录方式 |
|---|---|---|
| 会不会开始 | 2 次使用后能独立点“开始/继续” | 成功/需口头提醒/需代点 |
| 听不听得懂 | 80% 题目无需家长解释操作 | 每题记录是否解释 |
| 误触 | 每节导致流程中断的误触 ≤1 次 | 记录位置和原因 |
| 错误反馈 | 连错后能继续或自然结束，无明显受挫 | 继续/求助/退出/情绪反应 |
| 时长 | 大多数课程 8—10 分钟内完成，绝不超过 12 分钟 | 自动时长+观察 |
| 愿不愿再用 | 次日被邀请时愿意开始，不用奖品强迫 | 愿意/犹豫/拒绝 |
| 家长负担 | 除首次设置外，普通日课不需持续代点 | 代点次数 |

本观察只验证可用性、意愿、反馈和数据记录。即使全部通过，也不能宣称 14 天记忆保持或稳定掌握。

---

# 十一、边界、风险和用户待确认项

## 11.1 当前无阻塞的待确认项

| 项目 | 当前处理 | 何时确认 |
|---|---|---|
| 实际昵称 | 未提供且不需要猜；首次设置允许留空 | 第一次运行由家长决定 |
| Mate 30 具体型号代码 | 按用户明确的“Mate 30 标准版”规格设计 | G0 在“关于手机”核对；若为 Pro 停止并更新设备基线 |
| 合成女声具体音色 | 规格只固定普通话女声及音频参数 | T04 生成 2 个候选音色，由家长选 1 个；选择只影响资源，不改逻辑 |
| 1/3/7/14 适配度 | 明确为初始参数 | 5 字真实使用后观察；本规格不宣称最佳 |

这些项目均不需要 Codex 猜测产品规则：昵称可空、型号有停止门、音色有资源选择步骤、复习参数已给初始实现值。

## 11.2 主要风险

| 风险 | 等级 | 处理 |
|---|---|---|
| 鸿蒙 4.2 的 APK/音频/Room 兼容差异 | 高 | T01/G0 先行，未过不做完整 UI |
| 合成音频声调或断句不自然 | 高 | T04 双音色候选、逐字试听、家长复核 |
| “大/小”图片靠透视而非概念 | 高 | 同类同视角插画规则和唯一答案测试 |
| 只有 APP 答对、不能直接读出 | 高 | APP D14 与家长口头结果独立保存，双通过才稳定 |
| 同日刷题虚增掌握 | 高 | LocalDate 去重；测试 AT-13 |
| 本地写入失败造成假完成 | 高 | 事务、阻断前进、AT-19 |
| 系统备份/设备迁移超出 APP 控制 | 中 | 关闭备份能力并如实说明厂商行为需实测 |
| 5 字规则过拟合 | 中 | 内容模板和引擎按 300 字可扩展，但不建设服务器 |
| 家长误改口头结果 | 中 | 修订记录不可变、可追溯、状态立即重算 |

## 11.3 明确不做

- 不扩 30 字或 300 字；
- 不开始规范书写、拼音教学；
- 不录儿童声音判断发音；
- 不接入任何在线语音服务；
- 不做云端备份、账号或远程报告；
- 不为“以后可能用”先建 API、管理后台、推荐算法；
- 不用 3—7 天操作试用替代 14 天延迟验证。

---

# 十二、依据与设计判断

## 12.1 已核对事实

1. Mate 30 标准版公开屏幕规格为 6.62 英寸、2340×1080。
2. Android 官方建议交互目标至少 48×48dp；本项目因 5 岁儿童将儿童区下限提高到 64dp。
3. Room 是 SQLite 的 Android 抽象层并支持更可靠的本地数据访问；DataStore 适合少量异步、事务性设置。
4. `android:allowBackup="false"` 可关闭云备份，但 Android 12 及部分厂商设备的设备到设备迁移行为可能不同，因此必须实测并避免绝对承诺。

## 12.2 本项目设计判断

1. 默认每日 1 个新字，家长可改 2；
2. 首次练习 3 题，到期复习每字 2 题；
3. 一节累计 3 个题实例首次错误后提前结束；
4. 儿童点击目标下限 64dp，主任务使用更大的 88dp/96dp；
5. 原生 Android 优于本项目中的跨平台方案；
6. 早于 14 天的口头独立认读不作为稳定证据；
7. G0 兼容冒烟优先于完整界面开发。

这些判断均可在 5 字真实试用后调整，但当前已经明确，可直接编码和测试。

## 12.3 参考资料

- [Android：Compose 最小触控目标](https://developer.android.com/develop/ui/compose/accessibility/api-defaults)
- [Android：Room 本地持久化](https://developer.android.com/jetpack/androidx/releases/room)
- [Android：DataStore](https://developer.android.com/topic/libraries/architecture/datastore)
- [Android：Auto Backup 与 allowBackup](https://developer.android.com/identity/data/autobackup)
- [华为 Mate 30 系列公开规格（华为发布信息）](https://consumer.huawei.com/hk/press/news/2019/mate30_mate30prolocallaunch/)
- [Mate 30 标准版屏幕参数交叉核对](https://www.androidauthority.com/huawei-mate-30-pro-specs-1029252/)

---

# 十三、100 分制自评

## 13.1 本阶段评分标准

| 维度 | 满分 | 自评分 | 证据 |
|---|---:|---:|---|
| 教育有效性 | 30 | 29 | 五状态、跨日、跨题型、无提示、D14、口头双证据和回退均可编码；每字题库已满足首次三题、D14两题、≥4题型，并增加逐字稳定可达 P0 测试；扣 1 分因真实儿童结果尚未产生 |
| 儿童可用性 | 25 | 24 | 五页面逐状态、通用不泄题语音、两级错误支持、误触、PAUSED/ENDED_EARLY、时长持久化、次日接续和儿童观察标准完整；扣 1 分因尚未真机儿童验证 |
| 单儿童适配 | 15 | 15 | 已使用 Mate 30、鸿蒙 4.2、竖屏、0 识字、5 字、女声、昵称和离线 APK 的真实条件 |
| 开发简洁度 | 15 | 15 | 原生单模块、本地 Room/DataStore、配置内容、无网络/服务端/后台；5字原型删除备份恢复，T03/T04 依赖无循环 |
| 家长实用性 | 10 | 9 | 报告、易错、口头抽检、修订、设置、诊断与清空规则明确；扣 1 分因按原型范围暂不提供备份恢复 |
| 交付完整度 | 5 | 5 | 内容、页面、流程、状态、数据、真实 JSON、五题型、资源、工程、16 任务、43 测试和停止门齐全 |
| **总分** | **100** | **97** | **达到 95 分提交复审线，但不能替代总监评分** |

## 13.2 自评边界

97 分是“开发规格完整度”自评，不是软件质量评分，也不表示 APK 已开发。以下仍未完成：

- 最终图片和合成音频资源；
- 开发者与家长的 G2 双人内容审核；
- Mate 30 真机 G0/G4；
- 3—7 天儿童可用性观察；
- 14 天记忆保持和稳定掌握验证。

任何人不得引用本自评声称原型已经有效。

---

# 十四、V1.1 修订闭环与总监复审摘要

## 14.1 对 91/100 审查意见的闭环

| 审查阻断项 | V1.1 处理 | 可验证位置 |
|---|---|---|
| 首次三题与稳定掌握题型不可达 | 5 字均补齐看字选图、听音选字、看字选音；首次三题资格为0；每字≥4种独立题型；D14两题齐全 | 5.2、5.5、5.6、6.7、AT-36 |
| “大/小”语音泄露答案 | 两字看字选图统一使用“不读目标义”的通用语音 | 5.5、7.6、AT-34 |
| T03/T04 循环 | G1只校验结构和路径格式；G2在资源生成后校验存在、大小、SHA和双审 | 0.4、5.6、T03、T04 |
| 备份恢复外键不完整 | 5字原型删除备份导入恢复，仅保留不可回导诊断信息与原子清空 | 2.5、5.1、8.8、T13、AT-21/35 |
| PAUSED/ENDED_EARLY/时限未闭合 | 明确 `USER_REST` 与 `PAUSED`、`FATIGUE/TIME_LIMIT` 与 `ENDED_EARLY` 的唯一关系，以及首页表现、当天继续权限、次日接续、有效时长字段和5秒持久化 | 2.1、2.3、2.4、3.7、5.3、T10、AT-37—39 |
| 状态机回退闭环不完整 | 明确到期事件来源、失败日期窗口、四个资格/回退时间、清除条件及重新获得暂时/稳定掌握 | 4.2、4.6、4.7、5.3、T06、AT-40—43 |
| 次日任务优先级冲突 | 唯一顺序统一为到期复习→未完成新字接续→全新字 | 3.2、3.7、3.9、6.7、8.7、AT-15、AT-39 |

## 14.2 提交复审摘要

```text
本阶段名称：
《识字APP——5字可运行原型 Codex 开发规格包 V1.1》

已经完成：
围绕华为 Mate 30 / 鸿蒙 4.2 / 竖屏 / 0识字基线 / 人口大小山 /
可选昵称 / 本地合成女声 / 离线 Android APK，完成总监 91/100 审查后的定向修订。

核心决策：
1. 原生 Android Kotlin + Compose + Room + DataStore；
2. 先做 G0 目标机 APK 兼容冒烟；
3. 五种学习状态，1/3/7/14 为待验证初始参数；
4. APP 14天延迟识别与家长14天后独立读出双通过才稳定掌握；
5. 看字选音只显示可播放声音按钮，不显示拼音；
6. 每字首次三题、D14两题齐全，至少4种独立证据题型；
7. USER_REST只产生PAUSED并允许当天继续；FATIGUE/TIME_LIMIT导致ENDED_EARLY并封闭当天；
8. 每题事务保存，有效时长持久化，中断恢复，保存失败不前进；
9. G1结构与G2真实资源校验分离；
10. 5字版不做备份导入恢复，内容配置化，双人复核通过前禁止儿童试用。

交付物：
《识字APP_5字可运行原型_Codex开发规格包_V1.1_执行线程提交版.md》

仍存在的问题：
最终音频、图片、APK、真机结果和儿童观察尚未产生；这些属于审查通过后的开发与测试，
未被伪装成本阶段成果。

需要用户确认：
无阻塞项。昵称运行时可留空；G0核对具体设备型号；T04由家长在两个合成女声中选一个。

自评分：
97/100。

评分证据：
5字逐字模板、五页面全状态、13个完整流程、五状态与伪代码、字段字典、
真实5字JSON、五题型、资源规格、工程结构、16项开发任务、43条验收测试和六道停止门。

建议下一步：
总监只复审本 V1.1 开发规格。复审达到95分并放行后，才交给 Codex 从 T01 开始编码；
不得跳过目标机兼容冒烟，不得扩展30字。
```

---

## 文档终止声明

本文件完成后，本阶段停止。当前**没有编写代码、没有生成 APK、没有扩展 30 字或 300 字**。本次机器检查属于规格静态检查与规则模型检查，不等同于 T01—T16 已执行。只有总监对本执行线程提交版独立复审达到 95 分并明确放行，才允许 Codex 从 T01 开始。

---

# 十五、机器检查报告

## 15.1 检查范围与边界

- 检查对象：本执行线程提交版正文及其中 `content.json` 真实示例；
- 检查方式：JSON 解析、ID 集合检查、题型/证据矩阵检查、资源路径正则检查、指令文本检查、验收编号检查、任务依赖拓扑检查、五字状态规则模型检查；
- 未执行内容：真实图片/音频文件存在性、文件大小、SHA-256、G2 双人审核、APK、真机、儿童试用；这些必须在 T04、T15 及对应停止门执行；
- 可达性结论只表示本规格的真实 JSON、正式过滤顺序和任务/状态规则在可执行参考模型中存在合法路径。开发后的 AT-36 仍必须调用真实 Kotlin/Room 业务代码重新验证，禁止以本次参考模型代替。

## 15.2 十五项机器检查结果

| 序号 | 检查项 | 实际结果 | 结论 |
|---:|---|---|---|
| 1 | 5 字 JSON 能够真实解析 | 顶层对象、5 字内容、17 个选项、22 道题均成功反序列化 | PASS |
| 2 | 汉字 ID、题目 ID、选项 ID 无重复 | 汉字 ID 5 个、题目 ID 22 个、选项 ID 17 个；分类内和跨分类均无重复 | PASS |
| 3 | 五个字首次固定三题全部存在 | 人、口、大、小、山均具备看字选图、听音选字、看字选音 | PASS |
| 4 | 五个字 D14 两种题型全部可生成 | 每字均具备听音选字与看字选音种子，调度规则固定返回这两型 | PASS |
| 5 | 每字至少 4 种独立证据题型 | 人 4、口 4、大 5、小 5、山 4；证据类别数量对应为 4、4、5、5、4 | PASS |
| 6 | 正确答案存在于对应选项 | 22 道题的正确项均可解析，并在本题 `optionIds` 中恰好出现一次 | PASS |
| 7 | 资源引用格式合法 | 共检查 69 个引用；均为 `audio/` 或 `images/` 下允许目录的相对路径，无 URL、绝对路径或 `..` | PASS |
| 8 | “大、小”看字选图不泄露答案 | 两字均引用 `prompt_choose_picture_v1.mp3`；精确文本为“看看这个字，选一张合适的图。” | PASS |
| 9 | 首字真实生成资格 | 五字首次固定三题的 `minLearnedCount` 全为0；字段定义、过滤顺序和NEW不忽略规则一致 | PASS |
| 10 | 验收测试编号与AT-36口径 | 按正文顺序为 AT-01—AT-43，连续、递增、唯一；AT-36为单一全空库五字P0 | PASS |
| 11 | 已删除的备份导入恢复无功能残留 | 无入口、任务、输出、恢复验收；仅保留否定边界、系统云备份配置说明、只读诊断导出与不可逆清空 | PASS |
| 12 | 任务依赖不存在循环 | T01—T16 拓扑检查无环；T03→T02，T04→T03，无 T03/T04 反向依赖 | PASS |
| 13 | 次日任务优先级唯一 | 3.2、3.7、AT-15、AT-39均为到期复习→未完成新字接续→全新字 | PASS |
| 14 | 状态机回退闭环 | 数据源、失败日期窗口、资格/回退时间、清除、重新达标及AT-40—43齐全 | PASS |
| 15 | 五字全空库正式规则可达 | 单一空库从人→口→大→小→山运行；五字均经过全部五状态并最终稳定掌握 | PASS |

机器检查总结果：**15/15 PASS**。

## 15.3 五字全空库可达性明细

| 汉字 | 首学日/生成时learnedCount | 第4题型首次出现 | D14 APP | 家长独立认读 | 状态轨迹 |
|---|---:|---|---|---|---|
| 人 | D0 / 0 | D3 字形辨认 | D14 听音选字＋看字选音 | D14 PASS | UNLEARNED→FIRST_LEARNING→REVIEWING→TEMP_MASTERED→STABLE_MASTERED |
| 口 | D1 / 1 | D1 字形辨认 | D14（全局D15）两题 PASS | 同日 PASS | 同上 |
| 大 | D2 / 2 | D1 字形辨认 | D14（全局D16）两题 PASS | 同日 PASS | 同上 |
| 小 | D3 / 3 | D1 生活情境/字形辨认 | D14（全局D17）两题 PASS | 同日 PASS | 同上 |
| 山 | D4 / 4 | D1 字形辨认 | D14（全局D18）两题 PASS | 同日 PASS | 同上 |

逐日种子、资格、题型、状态和失败即停止输出见独立《五字从空数据库_P0运行轨迹》。

---

# 十六、《总监审查问题关闭表》

| 问题编号 | 原问题 | 修改章节 | 实际修改内容 | 验证方式 | 验证结果 | 是否关闭 |
|---|---|---|---|---|---|---|
| P01 | 每字缺少首次固定三题 | 3.3、5.2、5.5、5.6.1、6.1、6.7、AT-36 | 五字全部固定为看字选图→听音选字→看字选音；首次资格为0；跨日只补未完成首次题 | 解析 JSON 并按字求三题型集合和资格 | 五字均为 3/3；首次资格均为0 | 是 |
| P02 | 每字不足 4 种独立证据题型，D14 题型不可生成 | 4.2—4.5、5.5、5.6.1、6.7 | 人/口/山各 4 型，大/小各 5 型；D14 固定听音选字＋看字选音 | JSON 题型/证据类别计数＋调度规则检查 | 4、4、5、5、4；D14 五字均 2/2 | 是 |
| P03 | 缺少逐字从未学习到稳定掌握的 P0 自动测试 | 4.8、T06、T15、AT-36、15.3 | 新增单一全空数据库五字顺序测试；必须走正式任务生成、答题提交、状态引擎和口头抽检入口，禁止直写任务/状态/证据 | 可执行参考模型＋AT-36 测试源码静态禁写要求 | 人→口→大→小→山全部可达；真实代码仍须开发后复跑AT-36 | 是 |
| P04 | “大、小”看字选图指令泄露答案 | 5.5、6.3、7.6、AT-34 | 两字统一引用通用指令，精确文本不含目标读音或目标含义 | 检查两题资源引用及音频精确文本 | 均引用通用资源，无“大/小/大的/小的” | 是 |
| P05 | T03 与 T04 结构/资源校验混杂并形成循环 | 0.4、5.6、T03、T04、AT-34 | G1/T03 只验 JSON、ID、答案、题型和路径格式；G2/T04 在真实资源完成后验存在、大小、SHA-256和双审 | 解析 T01—T16 依赖并做拓扑检查 | 16 项任务无环；T04 单向依赖 T03 | 是 |
| P06 | 备份导入恢复无法保证完整外键 | 2.5、5.1、8.8、T13、AT-21、AT-35 | 删除学习记录备份、导入和恢复；只保留不可回导诊断导出和原子清空 | 搜索旧入口、任务、输出和验收措辞 | 无正向功能残留；系统云备份说明不属于数据恢复功能 | 是 |
| P07 | PAUSED、ENDED_EARLY、USER_REST、FATIGUE、时间上限规则不完整 | 2.1、2.3、2.4、3.7.1、5.3B、T10、AT-10/37—39 | USER_REST 只对应 PAUSED；FATIGUE/TIME_LIMIT 对应 ENDED_EARLY；定义首页文字、当天继续权限、终态与原因字段互斥 | 字段—状态—页面—验收四向交叉检查 | 规则一致，原因字段可持久化且互斥 | 是 |
| P08 | 杀进程后时长恢复及次日接续未闭合 | 3.7、3.7.2、5.3B、AT-33、AT-37—39 | 5 秒心跳结算；后台不计时；冷启动不补算离线时间；旧 PAUSED 跨日封账；首教按步骤与剩余首次题接续 | 状态表与三个 P0 用例交叉检查 | 杀进程误差上限≤5秒；次日不重复首教或旧未提交复习题 | 是 |
| P09 | 缺少可持久化累计有效学习时长 | 2.3、3.7.2、5.3B、T05、T10、AT-37/38 | 新增 `activeElapsedMs`、`activeSegmentStartedAt`、`limitMinutesSnapshot`、保存时机与时限待结束原因 | 字段存在性检查＋暂停/时限用例检查 | 字段、算法、保存时机和验收均齐全 | 是 |
| P10 | JSON、字段、状态机、页面、任务、停止门、测试和自评未同步 | 0.4、2—10、13—15 | 同步题库、矩阵、状态原因、首页状态、依赖、G1/G2、43 条测试和自评；新增机器检查报告 | 15 项机器检查＋全文交叉搜索 | 15/15 PASS；未发现新增章节矛盾 | 是 |
| P11 | 上次报告哈希与实际附件不一致 | 独立机器检查报告 | 最终正文完成后再计算 SHA-256；报告只引用最终文件实际哈希 | 对最终文件与报告声明分别复算并比较 | 必须相等，否则交付批次失败 | 是 |
| P12 | 首字首次三题资格歧义 | 5.2、5.5、5.6.1、6.1、6.7、AT-36 | 定义 learnedCount，固定过滤顺序；NEW不忽略字段；首次三题minLearnedCount=0 | 真实JSON解析＋空库D0生成 | 人在learnedCount=0时生成三题 | 是 |
| P13 | AT-36预置其他字且直写状态 | 4.8、8.7、T06、AT-36 | 改为单一全空库，所有字按正式顺序完成；禁止DAO直写和伪造证据 | 可执行参考模型逐日运行并失败即停止 | 五字全部沿正式路径稳定掌握 | 是 |
| P14 | 回退谓词和重新达标规则未定义 | 4.2、4.6、4.7、5.3、T06、AT-40—43 | 补齐数据源、窗口、持久化缓存、清除与重新双验证 | 字段/伪代码/验收交叉检查 | 四类回退/恢复场景均有P0验收 | 是 |
| P15 | 次日任务优先级互相冲突 | 3.2、3.7、3.9、6.7、8.7、AT-15、AT-39 | 统一为到期复习→未完成新字接续→全新字 | 全文冲突措辞扫描 | 无相反正向规则残留 | 是 |
| P16 | AT编号顺序与报告表述不符 | 第10节、15.2 | 正文按AT-01—AT-43顺序排列；报告如实声明43条 | 提取正文表格首列并比较连续序列 | 连续、唯一、递增 | 是 |
| P17 | 缺少正式问题关闭表和独立P0轨迹 | 第16节及两个独立交付文件 | 保留原91分10项并追加本轮7项；输出逐日P0轨迹 | 检查字段完整性与文件存在 | 17项均具备七个必填字段；轨迹含失败即停止标记 | 是 |
