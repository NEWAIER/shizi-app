# 《识字APP——5字可运行原型 V1.1 机器检查报告》

- 检查日期：2026-07-25
- 生成批次：90分复审版本链纠错后重新运行
- 唯一检查对象：《识字APP_5字可运行原型_Codex开发规格包_V1.1_执行线程提交版.md》
- 对象 SHA-256：`cafaa84100daf82cc4b9237e405dd0d339dcfae664c093793f60fb39dacec94a`
- 检查器：`verify_spec_v1_1.py`
- 总结果：**15/15 PASS**

## 一、哈希绑定

检查前后均对最终正文复算 SHA-256，机器检查器输出与 `sha256sum` 输出相同：

```text
SPEC=识字APP_5字可运行原型_Codex开发规格包_V1.1_执行线程提交版.md
SHA256=cafaa84100daf82cc4b9237e405dd0d339dcfae664c093793f60fb39dacec94a
```

若附件字节发生任何变化，本报告立即失效，必须对新文件重新运行全部检查并生成新报告。

## 二、检查边界

本报告检查最终规格正文、其中的真实5字JSON、ID集合、题型与证据矩阵、种子资格、资源引用格式、语音指令、验收编号、任务依赖、次日任务优先级、状态回退闭环，以及单一全空数据库五字可执行参考模型。

本报告未检查尚未制作的真实图片/音频文件、文件大小与资源SHA-256、G2双人审核、APK、Mate 30真机或儿童试用。上述内容仍属于总监放行后的T04、T15和相应停止门。本次参考模型也不能替代开发后对正式Kotlin/Room实现复跑AT-36。

## 三、十五项机器检查结果

| 序号 | 检查项 | 结果 | 实际输出 |
|---:|---|---|---|
| 1 | 5字JSON真实解析 | PASS | JSON解析成功 |
| 2 | ID及`CharacterContent`字段字典唯一 | PASS | 汉字5个、题目22个、选项17个，分类内及跨分类均无重复；`CharacterContent`共16个字段，`imageAsset`恰好1个 |
| 3 | 五字首次固定三题 | PASS | 五字均具备看字选图、听音选字、看字选音 |
| 4 | 五字D14两题 | PASS | 五字均具备听音选字与看字选音 |
| 5 | 每字至少4种独立证据题型 | PASS | 人4/4、口4/4、大5/5、小5/5、山4/4（题型数/证据类别数） |
| 6 | 正确答案属于对应选项 | PASS | 22道题正确项均存在，并恰好出现一次 |
| 7 | 资源引用格式合法 | PASS | 69个引用均为允许目录下的相对路径 |
| 8 | “大、小”看字选图不泄露答案 | PASS | 均引用通用音频；精确文本为“看看这个字，选一张合适的图。” |
| 9 | 首字真实生成资格 | PASS | 五字固定首次三题 `minLearnedCount=0`；字段定义、过滤顺序和NEW规则一致 |
| 10 | 验收编号与AT-36口径 | PASS | 正文顺序AT-01—AT-43连续唯一；AT-36为单一全空库五字P0 |
| 11 | 备份导入恢复无功能残留 | PASS | 仅保留否定边界、只读诊断导出和不可逆清空 |
| 12 | 任务依赖无循环 | PASS | T01—T16拓扑无环；T04单向依赖T03 |
| 13 | 次日任务优先级唯一 | PASS | 3.2、3.7、AT-15、AT-39统一为到期复习→未完成新字接续→全新字 |
| 14 | 状态机回退闭环 | PASS | 数据源、失败日期窗口、资格/回退缓存、清除、重新达标和AT-40—43齐全 |
| 15 | 五字全空库正式规则可达 | PASS | 五字均按正式顺序经过五状态并最终到达稳定掌握 |

机器原始总结果：

```text
OVERALL=PASS
```

## 四、AT-36全空库关键输出

```text
INIT	rows=0	人/口/大/小/山=UNLEARNED
DAY=00	人	NEW	learnedCount=0	seeds=q_ren_char_image,q_ren_listen_char,q_ren_char_audio	STATE=REVIEWING
DAY=01	口	NEW	learnedCount=1	seeds=q_kou_char_image,q_kou_listen_char,q_kou_char_audio	STATE=REVIEWING
DAY=02	大	NEW	learnedCount=2	seeds=q_da_char_image,q_da_listen_char,q_da_char_audio	STATE=REVIEWING
DAY=03	小	NEW	learnedCount=3	seeds=q_xiao_char_image,q_xiao_listen_char,q_xiao_char_audio	STATE=REVIEWING
DAY=04	山	NEW	learnedCount=4	seeds=q_shan_char_image,q_shan_listen_char,q_shan_char_audio	STATE=REVIEWING
DAY=14	人	D14+ORAL	APP=PASS	ORAL=INDEPENDENT_PASS	STATE=STABLE_MASTERED
DAY=15	口	D14+ORAL	APP=PASS	ORAL=INDEPENDENT_PASS	STATE=STABLE_MASTERED
DAY=16	大	D14+ORAL	APP=PASS	ORAL=INDEPENDENT_PASS	STATE=STABLE_MASTERED
DAY=17	小	D14+ORAL	APP=PASS	ORAL=INDEPENDENT_PASS	STATE=STABLE_MASTERED
DAY=18	山	D14+ORAL	APP=PASS	ORAL=INDEPENDENT_PASS	STATE=STABLE_MASTERED
OVERALL=PASS	五字均从全空库真实到达 STABLE_MASTERED
```

完整逐日题型、种子资格和状态输出见《识字APP_5字从空数据库_P0运行轨迹.md》。

## 五、验收编号核对

正文实际顺序为：

```text
AT-01, AT-02, …, AT-35, AT-36, AT-37, …, AT-43
```

共43条，连续、递增、唯一。报告不再使用与正文不一致的“AT-01—AT-39”表述。

## 六、最终结论

最终规格通过本轮15项机器检查，且报告对象哈希与最终文件实际哈希一致。该结论仅支持“重新提交总监独立复审”，不构成T01编码放行，不证明资源、APK、真机或儿童学习效果已经通过。

## 七、唯一版本链复核

本批次四份正式材料均以同一规格对象为根：

```text
最终规格正文
SHA-256=cafaa84100daf82cc4b9237e405dd0d339dcfae664c093793f60fb39dacec94a
  ├─机器检查报告：引用同一规格SHA-256
  ├─P0运行轨迹：引用同一规格SHA-256
  └─总监审查问题关闭表：引用同一规格SHA-256
```

最终正文及三份关联材料中均不存在作为当前结论的`12/12`、`39条`、`3be926d3…`或`7524e295…`版本声明。历史问题描述不构成当前版本结论。
