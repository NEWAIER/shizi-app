package com.family.shizi.navigation

enum class ShiziRoute(val route: String, val pageName: String) {
    Home("home", "儿童首页"),
    Learn("learn", "单字学习页"),
    Practice("practice", "练习页"),
    Result("result", "学习结果页"),
    Parent("parent", "家长页"),
    StageTest("stage_test", "阶段测试"),
    Learned("learned", "已学习"),
    Profile("profile", "我的");

    companion object {
        val startDestination: String = Home.route

        /** 树洞测试关卡路由模板（Navigation Compose 参数名）。 */
        const val STAGE_TEST_ROUTE_PATTERN = "stage_test/{batch}"

        /** 生成带批次参数的具体路由，[batchIndex] 为 0 起的批次号（第 1 关 = 1-10 字）。 */
        fun stageTestRoute(batchIndex: Int): String = "stage_test/$batchIndex"

        /** 从导航参数中读取批次号。 */
        const val STAGE_TEST_ARG_BATCH = "batch"
    }
}
