# T02 五路由与页面对应表

| 路由 | 页面名称 | Composable | 文件 |
|---|---|---|---|
| `home` | 儿童首页 | `HomeScreen` | `ui/home/HomeScreen.kt` |
| `learn` | 单字学习页 | `LearnScreen` | `ui/learn/LearnScreen.kt` |
| `practice` | 练习页 | `PracticeScreen` | `ui/practice/PracticeScreen.kt` |
| `result` | 学习结果页 | `ResultScreen` | `ui/result/ResultScreen.kt` |
| `parent` | 家长页 | `ParentScreen` | `ui/parent/ParentScreen.kt` |

启动路由为 `home`。Debug 构建由共用 `EmptyRouteScreen` 显示临时路由验证按钮；这不是正式儿童交互。系统返回由 Navigation Compose 返回栈处理。
