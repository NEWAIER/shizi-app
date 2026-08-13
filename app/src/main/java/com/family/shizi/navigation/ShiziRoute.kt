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
    }
}
