# 星星能量与成长系统 V1

本文件为 PR-FE-00 冻结稿。奖励用于表达成长，不用于制造考试压力。

## 奖励事件

奖励事件必须使用唯一键 `rewardType + sourceId` 去重。保存失败不发奖，重复打开结果页不重复发奖，答错不扣分，中途退出不扣分。

建议数据结构：

```text
ChildProfile {
  avatarId
  avatarFrameId
  totalStars
  honorLevel
  learningStreak
  unlockedBadgeIds
  unlockedAvatarIds
  unlockedDecorationIds
}

RewardEvent {
  id
  rewardType
  sourceId
  amount
  createdAt
}
```

| 行为 | 星星能量 |
| --- | ---: |
| 完成一个新字 | 10 |
| 完成该字立即测试 | 5 |
| 首次答对一题 | 2 |
| 完成到期复习字 | 5 |
| 完成今日全部任务 | 10 |
| 连续学习 3 天 | 20 |
| 完成阶段挑战 | 20 |

## 等级

Lv.1 字宝宝（0）、Lv.2 识字小芽（50）、Lv.3 字宝宝朋友（120）、Lv.4 识字探险家（250）、Lv.5 星星收藏家（450）、Lv.6 汉字小达人（700）。升级事件同样必须幂等。

## 首版可配置内容

8 个头像、6 个头像边框、10 个装饰、4 套星球背景和至少 24 枚徽章。未解锁内容可预览但不可使用。

## 徽章分类

首版至少 24 枚，分为五类：学习（5 枚）、坚持（5 枚）、复习（5 枚）、挑战（5 枚）、收集（4 枚）。徽章定义必须配置化，解锁条件可追溯，不依赖页面重复打开。
