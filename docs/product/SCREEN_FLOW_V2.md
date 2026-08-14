# 识字 APP V2.0 页面流程

## 1. 总流程

```mermaid
flowchart TD
    A[打开 APP] --> B{是否有未完成课程}
    B -->|是| C[继续今天学习]
    B -->|否| D[今天认识一个新朋友]
    C --> E[认识字]
    D --> E
    E --> F[了解意思]
    F --> G[组词与例句]
    G --> H[立即测试]
    H --> I{本字完成}
    I -->|是| J[星星与鼓励反馈]
    J --> K{还有今日内容}
    K -->|是| E
    K -->|否| L[学习结果]
```

## 2. 四个入口

```text
学习 ──> 今日学习会话 ──> Learn ──> Practice ──> Result
挑战 ──> 老朋友复习 / 阶段挑战 ──> Practice ──> Result
字卡 ──> 五列图鉴 ──> 单击播放 / 长按详情
我的星球 ──> 成长首页 ──> 头像、徽章、装饰预览
```

## 3. 学习状态机

```mermaid
stateDiagram-v2
    [*] --> ENTER_STEP
    ENTER_STEP --> PLAY_PROMPT
    PLAY_PROMPT --> PLAY_CONTENT
    PLAY_CONTENT --> HOLD
    HOLD --> SAVE_PROGRESS
    SAVE_PROGRESS --> ADVANCE
    ADVANCE --> ENTER_STEP: 还有步骤
    ADVANCE --> PRACTICE: 教学完成
    PLAY_CONTENT --> REPLAY: 儿童点击再听一次
    REPLAY --> PLAY_CONTENT
    ENTER_STEP --> PAUSED: 返回或休息
    PLAY_CONTENT --> PAUSED: 页面离开
    PAUSED --> ENTER_STEP: 继续学习
    SAVE_PROGRESS --> ERROR: 保存失败
    ERROR --> SAVE_PROGRESS: 重试
```

## 4. 状态约束

- 音频失败时停留在当前步骤，不自动跳过。
- 自动推进前先保存进度，保存失败不发奖励。
- 重听不会重复保存步骤或重复领取奖励。
- 快速点击不能产生重复播放、重复提交或重复奖励。
- 时间限制只在当前步骤/题目完成后生效。
- 退出后按会话状态恢复，不要求儿童重新开始。

## 5. 题型流程

### 听音选字

进入题目自动播放字音，儿童点击一个汉字后立即判断。

### 看字选图

展示大字和图片选项，儿童点击一张图片后立即判断。

### 找相同字

展示目标字和文字选项，儿童点击相同汉字后立即判断。

## 6. 反馈流程

```text
答对 → 温和鼓励 → 星星动画 → 约 800ms 后下一题
第一次答错 → “不着急，再看一眼” → 重播提示 → 再选一次
第二次答错 → 突出正确答案 → 播放字音 → 教学停留 → 下一题
```
